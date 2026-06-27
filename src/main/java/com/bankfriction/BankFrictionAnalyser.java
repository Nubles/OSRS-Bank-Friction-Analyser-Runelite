package com.bankfriction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BankFrictionAnalyser
{
	private static final int MIN_LOADOUT_ITEMS = 3;
	private static final int MIN_LOADOUT_SESSIONS = 3;
	private static final int MIN_REPEATED_SEARCHES = 3;
	private static final int SLOW_LOADOUT_SECONDS = 60;
	private static final int MIN_NEAR_DUPLICATES = 3;
	private static final int MIN_MANUAL_REPOSITIONS = 3;
	private static final int MIN_TAB_SWITCHES = 5;
	private static final int MAX_SESSIONS = 75;
	private static final int MAX_SEARCHES = 150;

	private final BankFrictionSnapshot snapshot;
	private BankFrictionSession currentSession;

	public BankFrictionAnalyser()
	{
		this(new BankFrictionSnapshot());
	}

	public BankFrictionAnalyser(BankFrictionSnapshot snapshot)
	{
		this.snapshot = snapshot == null ? new BankFrictionSnapshot() : snapshot;
	}

	public void recordSessionStart(long timeMillis)
	{
		if (currentSession != null)
		{
			currentSession.finish(lastInteractionTime(currentSession));
		}

		currentSession = new BankFrictionSession(timeMillis);
		snapshot.getSessions().add(currentSession);
		trimHistory();
	}

	public void recordSessionEnd(long timeMillis)
	{
		if (currentSession != null)
		{
			currentSession.finish(timeMillis);
			currentSession = null;
		}
	}

	public void recordWithdraw(int itemId, String itemName, long timeMillis)
	{
		recordItemInteraction(BankFrictionInteractionType.WITHDRAW, itemId, itemName, timeMillis);
	}

	public void recordDeposit(int itemId, String itemName, long timeMillis)
	{
		recordItemInteraction(BankFrictionInteractionType.DEPOSIT, itemId, itemName, timeMillis);
	}

	public void recordManualReposition(int itemId, String itemName, long timeMillis)
	{
		recordItemInteraction(BankFrictionInteractionType.DRAG, itemId, itemName, timeMillis);
	}

	public void recordTabSwitch(long timeMillis)
	{
		ensureSessionStarted(timeMillis);
		currentSession.addInteraction(new BankFrictionInteraction(
			BankFrictionInteractionType.TAB_SWITCH, -1, "Bank tab", timeMillis));
	}

	public void recordSearch(String query, long timeMillis)
	{
		String normalized = normalizeSearch(query);
		if (!normalized.isEmpty())
		{
			snapshot.getSearches().add(new BankFrictionSearch(normalized, timeMillis));
			trimHistory();
		}
	}

	public List<BankFrictionRecommendation> buildRecommendations(long nowMillis)
	{
		List<BankFrictionRecommendation> recommendations = new ArrayList<>();
		addLoadoutRecommendation(recommendations);
		addRepeatedSearchRecommendation(recommendations);
		addSlowLoadoutRecommendation(recommendations);
		addNearDuplicateRecommendation(recommendations);
		addManualRepositionRecommendation(recommendations);
		addTabSwitchRecommendation(recommendations);

		recommendations.sort(Comparator
			.comparingInt(BankFrictionRecommendation::getConfidencePercent).reversed()
			.thenComparingInt(BankFrictionRecommendation::getEvidenceCount).reversed()
			.thenComparing(BankFrictionRecommendation::getTitle));
		return recommendations;
	}

	public BankFrictionSnapshot snapshot()
	{
		return snapshot;
	}

	private void ensureSessionStarted(long timeMillis)
	{
		if (currentSession == null)
		{
			currentSession = new BankFrictionSession(timeMillis);
			snapshot.getSessions().add(currentSession);
			trimHistory();
		}
	}

	private static long lastInteractionTime(BankFrictionSession session)
	{
		long timeMillis = session.getStartMillis();
		for (BankFrictionInteraction interaction : session.getInteractions())
		{
			timeMillis = Math.max(timeMillis, interaction.getTimeMillis());
		}
		return timeMillis;
	}

	private void trimHistory()
	{
		while (snapshot.getSessions().size() > MAX_SESSIONS)
		{
			snapshot.getSessions().remove(0);
		}

		while (snapshot.getSearches().size() > MAX_SEARCHES)
		{
			snapshot.getSearches().remove(0);
		}
	}

	private void recordItemInteraction(BankFrictionInteractionType type, int itemId, String itemName, long timeMillis)
	{
		if (itemId <= 0)
		{
			return;
		}

		ensureSessionStarted(timeMillis);
		String safeName = itemName == null || itemName.trim().isEmpty()
			? "Item " + itemId
			: itemName.trim();
		snapshot.getItemNames().put(itemId, safeName);
		currentSession.addInteraction(new BankFrictionInteraction(type, itemId, safeName, timeMillis));
	}

	private void addLoadoutRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		List<Set<Integer>> sessionWithdrawals = new ArrayList<>();
		Map<Integer, Integer> itemCounts = new HashMap<>();
		for (BankFrictionSession session : snapshot.getSessions())
		{
			Set<Integer> withdrawals = itemIdsFor(session, BankFrictionInteractionType.WITHDRAW);
			if (withdrawals.size() >= MIN_LOADOUT_ITEMS)
			{
				sessionWithdrawals.add(withdrawals);
				for (int itemId : withdrawals)
				{
					itemCounts.put(itemId, itemCounts.getOrDefault(itemId, 0) + 1);
				}
			}
		}

		if (sessionWithdrawals.size() < MIN_LOADOUT_SESSIONS)
		{
			return;
		}

		List<Integer> candidate = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : itemCounts.entrySet())
		{
			if (entry.getValue() >= MIN_LOADOUT_SESSIONS)
			{
				candidate.add(entry.getKey());
			}
		}
		candidate.sort(Comparator.comparing(this::itemName));

		if (candidate.size() < MIN_LOADOUT_ITEMS)
		{
			return;
		}

		int togetherCount = 0;
		for (Set<Integer> withdrawals : sessionWithdrawals)
		{
			if (withdrawals.containsAll(candidate))
			{
				togetherCount++;
			}
		}

		if (togetherCount < MIN_LOADOUT_SESSIONS)
		{
			return;
		}

		int confidence = percent(togetherCount, sessionWithdrawals.size());
		String itemList = joinItemNames(candidate);
		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.LOADOUT_GROUP,
			"Move frequent loadout together",
			"Move these items together; you withdraw them as one loadout " + confidence + "% of the time: " + itemList + ".",
			candidate,
			confidence,
			togetherCount,
			0));
	}

	private void addRepeatedSearchRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		Map<String, Integer> counts = new HashMap<>();
		for (BankFrictionSearch search : snapshot.getSearches())
		{
			counts.put(search.getQuery(), counts.getOrDefault(search.getQuery(), 0) + 1);
		}

		Map.Entry<String, Integer> best = null;
		for (Map.Entry<String, Integer> entry : counts.entrySet())
		{
			if (entry.getValue() >= MIN_REPEATED_SEARCHES && (best == null || entry.getValue() > best.getValue()))
			{
				best = entry;
			}
		}

		if (best == null)
		{
			return;
		}

		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.REPEATED_SEARCH,
			"Repeated search: " + best.getKey(),
			"You repeatedly search for \"" + best.getKey() + "\". A bank tag or nearby item cluster may save time.",
			Collections.emptyList(),
			75,
			best.getValue(),
			0));
	}

	private void addSlowLoadoutRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		BankFrictionSession slowest = null;
		long slowestDuration = 0L;
		for (BankFrictionSession session : snapshot.getSessions())
		{
			Set<Integer> withdrawals = itemIdsFor(session, BankFrictionInteractionType.WITHDRAW);
			long duration = session.getEndMillis() - session.getStartMillis();
			if (withdrawals.size() >= MIN_LOADOUT_ITEMS
				&& duration >= SLOW_LOADOUT_SECONDS * 1_000L
				&& duration > slowestDuration)
			{
				slowest = session;
				slowestDuration = duration;
			}
		}

		if (slowest == null)
		{
			return;
		}

		int seconds = (int) (slowestDuration / 1_000L);
		List<Integer> itemIds = new ArrayList<>(itemIdsFor(slowest, BankFrictionInteractionType.WITHDRAW));
		itemIds.sort(Comparator.comparing(this::itemName));
		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.SLOW_LOADOUT,
			"Slow loadout assembly",
			"This loadout took about " + seconds + " seconds to assemble. Grouping "
				+ joinItemNames(itemIds) + " may reduce bank time.",
			itemIds,
			65,
			itemIds.size(),
			seconds));
	}

	private void addNearDuplicateRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		Map<String, Set<Integer>> byBaseName = new HashMap<>();
		for (BankFrictionSession session : snapshot.getSessions())
		{
			for (BankFrictionInteraction interaction : session.getInteractions())
			{
				if (interaction.getType() == BankFrictionInteractionType.WITHDRAW)
				{
					String baseName = baseItemName(interaction.getItemName());
					byBaseName.computeIfAbsent(baseName, ignored -> new LinkedHashSet<>()).add(interaction.getItemId());
				}
			}
		}

		String bestName = null;
		Set<Integer> bestItems = Collections.emptySet();
		for (Map.Entry<String, Set<Integer>> entry : byBaseName.entrySet())
		{
			if (entry.getValue().size() >= MIN_NEAR_DUPLICATES && entry.getValue().size() > bestItems.size())
			{
				bestName = entry.getKey();
				bestItems = entry.getValue();
			}
		}

		if (bestName == null)
		{
			return;
		}

		List<Integer> itemIds = new ArrayList<>(bestItems);
		itemIds.sort(Comparator.comparing(this::itemName));
		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.NEAR_DUPLICATE,
			"Near-duplicate items",
			"You use several versions of " + bestName + ". Put the doses or variants in a consistent order to reduce misclicks.",
			itemIds,
			70,
			itemIds.size(),
			0));
	}

	private void addManualRepositionRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		Map<Integer, Integer> counts = interactionCounts(BankFrictionInteractionType.DRAG);
		Map.Entry<Integer, Integer> best = bestItemCount(counts, MIN_MANUAL_REPOSITIONS);
		if (best == null)
		{
			return;
		}

		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.MANUAL_REPOSITION,
			"Item keeps being repositioned",
			"You manually reposition " + itemName(best.getKey()) + " often. It may belong beside the items you use around it.",
			Collections.singletonList(best.getKey()),
			60,
			best.getValue(),
			0));
	}

	private void addTabSwitchRecommendation(List<BankFrictionRecommendation> recommendations)
	{
		int switches = 0;
		for (BankFrictionSession session : snapshot.getSessions())
		{
			for (BankFrictionInteraction interaction : session.getInteractions())
			{
				if (interaction.getType() == BankFrictionInteractionType.TAB_SWITCH)
				{
					switches++;
				}
			}
		}

		if (switches < MIN_TAB_SWITCHES)
		{
			return;
		}

		recommendations.add(new BankFrictionRecommendation(
			BankFrictionRecommendationType.TAB_SWITCHING,
			"Frequent bank tab switching",
			"You switch bank tabs repeatedly while preparing. Items from those tabs may belong in the same bank tag layout.",
			Collections.emptyList(),
			55,
			switches,
			0));
	}

	private Map<Integer, Integer> interactionCounts(BankFrictionInteractionType type)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (BankFrictionSession session : snapshot.getSessions())
		{
			for (BankFrictionInteraction interaction : session.getInteractions())
			{
				if (interaction.getType() == type && interaction.getItemId() > 0)
				{
					counts.put(interaction.getItemId(), counts.getOrDefault(interaction.getItemId(), 0) + 1);
				}
			}
		}
		return counts;
	}

	private static Map.Entry<Integer, Integer> bestItemCount(Map<Integer, Integer> counts, int minimum)
	{
		Map.Entry<Integer, Integer> best = null;
		for (Map.Entry<Integer, Integer> entry : counts.entrySet())
		{
			if (entry.getValue() >= minimum && (best == null || entry.getValue() > best.getValue()))
			{
				best = entry;
			}
		}
		return best;
	}

	private static Set<Integer> itemIdsFor(BankFrictionSession session, BankFrictionInteractionType type)
	{
		Set<Integer> itemIds = new HashSet<>();
		for (BankFrictionInteraction interaction : session.getInteractions())
		{
			if (interaction.getType() == type && interaction.getItemId() > 0)
			{
				itemIds.add(interaction.getItemId());
			}
		}
		return itemIds;
	}

	private String itemName(int itemId)
	{
		return snapshot.getItemNames().getOrDefault(itemId, "Item " + itemId);
	}

	private String joinItemNames(Collection<Integer> itemIds)
	{
		List<String> names = new ArrayList<>();
		for (int itemId : itemIds)
		{
			names.add(itemName(itemId));
		}
		return String.join(", ", names);
	}

	private static int percent(int count, int total)
	{
		if (total <= 0)
		{
			return 0;
		}
		return (int) Math.round((count * 100.0d) / total);
	}

	private static String normalizeSearch(String query)
	{
		if (query == null)
		{
			return "";
		}
		String normalized = query.trim().toLowerCase(Locale.ROOT);
		if (normalized.equals("range"))
		{
			return "ranged";
		}
		return normalized;
	}

	private static String baseItemName(String itemName)
	{
		String base = itemName == null ? "" : itemName;
		base = base.replaceAll("\\([0-9]+\\)", "");
		base = base.replaceAll("\\+[0-9]+$", "");
		base = base.replaceAll("\\s+", " ").trim();
		return base.isEmpty() ? "item" : base;
	}
}