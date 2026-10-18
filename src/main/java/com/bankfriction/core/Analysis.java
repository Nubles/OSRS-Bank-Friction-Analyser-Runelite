package com.bankfriction.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the journal and works out the friction score, the loadouts and the advice. */
public final class Analysis
{
	/** Gear-ups averaged for the score, and again for the score before it. */
	static final int SCORE_WINDOW = 15;
	/** Fewer gear-ups than this and there is no score yet. */
	static final int MIN_TRIPS_FOR_SCORE = 3;
	/** A visit needs this many items taken out to count toward a loadout. */
	static final int MIN_LOADOUT_ITEMS = 3;
	/** A loadout must be seen this many times. */
	static final int MIN_LOADOUT_TRIPS = 3;
	/** How alike a visit must be to a loadout to join it. */
	static final double JOIN = 0.45;
	/** An item is core to a loadout when it is in this share of its trips. */
	static final double CORE = 0.6;
	/** Patterns must repeat this many times before they become advice. */
	static final int MIN_REPEATS = 3;
	/** An item counts as kept deep when this share of its withdrawals needed a scroll. */
	static final double DEEP_SHARE = 0.6;
	static final int MAX_ADVICE = 8;
	static final int MAX_LOADOUTS = 6;
	static final int RECENT = 40;
	private static final Pattern DOSE = Pattern.compile("^(.*)\\(([1-4])\\)$");

	private Analysis()
	{
	}

	public static Report of(Journal journal)
	{
		List<Visit> visits = journal.getVisits();
		List<Visit> trips = new ArrayList<>();
		double lost = 0;
		for (Visit visit : visits)
		{
			lost += visit.lostSeconds();
			if (visit.withdrawals() > 0)
			{
				trips.add(visit);
			}
		}

		int friction = -1;
		int previous = -1;
		if (trips.size() >= MIN_TRIPS_FOR_SCORE)
		{
			int end = trips.size();
			int from = Math.max(0, end - SCORE_WINDOW);
			friction = averageFriction(trips.subList(from, end));
			int prevFrom = Math.max(0, from - SCORE_WINDOW);
			if (from - prevFrom >= MIN_TRIPS_FOR_SCORE)
			{
				previous = averageFriction(trips.subList(prevFrom, from));
			}
		}

		List<Integer> gearUps = new ArrayList<>();
		for (Visit trip : trips)
		{
			gearUps.add(trip.gearUpMillis());
		}

		List<Loadout> loadouts = loadouts(journal, trips);
		List<Advice> advice = new ArrayList<>();
		gatherAdvice(journal, loadouts, trips, advice);
		raiseAdvice(journal, trips, advice);
		searchAdvice(journal, visits, advice);
		putBackAdvice(journal, visits, advice);
		decantAdvice(journal, trips, advice);
		paceAdvice(loadouts, advice);
		advice.sort((x, y) -> Double.compare(y.impact(), x.impact()));
		if (advice.size() > MAX_ADVICE)
		{
			advice = new ArrayList<>(advice.subList(0, MAX_ADVICE));
		}

		List<Visit> recent = new ArrayList<>();
		for (int i = visits.size() - 1; i >= 0 && recent.size() < RECENT; i--)
		{
			recent.add(visits.get(i));
		}
		return new Report(friction, previous, trips.size(), visits.size(), median(gearUps), lost,
			advice, loadouts, recent, journal.getNames());
	}

	private static int averageFriction(List<Visit> visits)
	{
		if (visits.isEmpty())
		{
			return -1;
		}
		double sum = 0;
		for (Visit visit : visits)
		{
			sum += visit.friction();
		}
		return (int) Math.round(sum / visits.size());
	}

	// ---- Loadouts -------------------------------------------------------------------------

	/** A loadout being built: the visits in it and how often each item appears. */
	private static final class Cluster
	{
		final List<Visit> members = new ArrayList<>();
		final Map<Integer, Integer> counts = new HashMap<>();
		/** Sum of the order each item was taken in, to put the first taken first. */
		final Map<Integer, Integer> order = new HashMap<>();

		void add(Visit visit, Set<Integer> items)
		{
			members.add(visit);
			int index = 0;
			for (int item : items)
			{
				counts.merge(item, 1, Integer::sum);
				order.merge(item, index++, Integer::sum);
			}
		}

		Set<Integer> core()
		{
			Set<Integer> core = new HashSet<>();
			int need = (int) Math.ceil(members.size() * CORE);
			for (Map.Entry<Integer, Integer> e : counts.entrySet())
			{
				if (e.getValue() >= need)
				{
					core.add(e.getKey());
				}
			}
			return core.isEmpty() ? counts.keySet() : core;
		}

		List<Integer> sortedCore()
		{
			List<Integer> core = new ArrayList<>(core());
			core.sort((a, b) ->
			{
				int byCount = Integer.compare(counts.get(b), counts.get(a));
				if (byCount != 0)
				{
					return byCount;
				}
				return Double.compare(order.get(a) / (double) counts.get(a), order.get(b) / (double) counts.get(b));
			});
			return core;
		}
	}

	static double similarity(Set<Integer> a, Set<Integer> b)
	{
		if (a.isEmpty() && b.isEmpty())
		{
			return 0;
		}
		int both = 0;
		for (int x : a)
		{
			if (b.contains(x))
			{
				both++;
			}
		}
		return both / (double) (a.size() + b.size() - both);
	}

	private static List<Loadout> loadouts(Journal journal, List<Visit> trips)
	{
		List<Cluster> clusters = new ArrayList<>();
		for (Visit trip : trips)
		{
			Set<Integer> items = trip.withdrawn();
			if (items.size() < MIN_LOADOUT_ITEMS)
			{
				continue;
			}
			Cluster best = null;
			double bestScore = JOIN;
			for (Cluster cluster : clusters)
			{
				double score = similarity(items, cluster.core());
				if (score >= bestScore)
				{
					best = cluster;
					bestScore = score;
				}
			}
			if (best == null)
			{
				best = new Cluster();
				clusters.add(best);
			}
			best.add(trip, items);
		}

		List<Loadout> loadouts = new ArrayList<>();
		for (Cluster cluster : clusters)
		{
			if (cluster.members.size() < MIN_LOADOUT_TRIPS)
			{
				continue;
			}
			List<Integer> core = cluster.sortedCore();
			if (core.size() < MIN_LOADOUT_ITEMS)
			{
				continue;
			}
			Set<Integer> coreSet = new HashSet<>(core);
			List<Integer> gearUps = new ArrayList<>();
			int best = Integer.MAX_VALUE;
			double tabSwitches = 0;
			double friction = 0;
			Set<Integer> tabs = new HashSet<>();
			Map<Integer, List<Integer>> rows = new HashMap<>();
			long lastSeen = 0;
			for (Visit visit : cluster.members)
			{
				gearUps.add(visit.gearUpMillis());
				best = Math.min(best, visit.gearUpMillis());
				tabSwitches += visit.tabSwitches();
				friction += visit.friction();
				lastSeen = Math.max(lastSeen, visit.getStart());
				for (Action action : visit.getActions())
				{
					if (action.getKind() == Action.Kind.WITHDRAW && coreSet.contains(action.getItemId()) && !action.isSearched())
					{
						if (action.getTab() >= 0)
						{
							tabs.add(action.getTab());
						}
						if (action.getRow() >= 0)
						{
							rows.computeIfAbsent(action.getItemId(), k -> new ArrayList<>()).add(action.getRow());
						}
					}
				}
			}
			int n = cluster.members.size();
			loadouts.add(new Loadout(name(journal, core), core, n, median(gearUps), best,
				tabSwitches / n, realTabs(tabs), rowSpan(rows), (int) Math.round(friction / n), lastSeen));
		}
		loadouts.sort((a, b) -> Integer.compare(b.getTrips(), a.getTrips()));
		return loadouts.size() > MAX_LOADOUTS ? new ArrayList<>(loadouts.subList(0, MAX_LOADOUTS)) : loadouts;
	}

	/** Tab 0 is the main tab, which shows every item; it only counts when nothing else was used. */
	private static int realTabs(Set<Integer> tabs)
	{
		Set<Integer> real = new HashSet<>(tabs);
		real.remove(0);
		return real.isEmpty() ? (tabs.isEmpty() ? 0 : 1) : real.size() + (tabs.contains(0) ? 1 : 0);
	}

	/** Rows between the highest and lowest item, using each item's usual row. */
	private static int rowSpan(Map<Integer, List<Integer>> rows)
	{
		if (rows.size() < 2)
		{
			return -1;
		}
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (List<Integer> r : rows.values())
		{
			int m = median(r);
			low = Math.min(low, m);
			high = Math.max(high, m);
		}
		return high - low;
	}

	static String name(Journal journal, List<Integer> core)
	{
		String first = shortName(journal.name(core.get(0)));
		if (core.size() == 1)
		{
			return first;
		}
		return first + " & " + shortName(journal.name(core.get(1)));
	}

	private static String shortName(String name)
	{
		Matcher m = DOSE.matcher(name);
		return m.matches() ? m.group(1).trim() : name;
	}

	// ---- Advice ---------------------------------------------------------------------------

	private static void gatherAdvice(Journal journal, List<Loadout> loadouts, List<Visit> trips, List<Advice> out)
	{
		for (Loadout loadout : loadouts)
		{
			boolean spread = loadout.getTabs() >= 2 || loadout.getTabSwitches() >= 1.5;
			boolean far = loadout.getRowSpan() >= 6;
			if (!spread && !far)
			{
				continue;
			}
			String body;
			double seconds;
			if (spread)
			{
				seconds = Math.max(1, loadout.getTabSwitches()) * Visit.TAB_SECONDS;
				body = "You take these from " + plural(Math.max(2, loadout.getTabs()), "tab")
					+ " and hop tabs about " + oneDecimal(loadout.getTabSwitches()) + " times a trip."
					+ " Keeping them in one tab (a Bank Tags layout works well) lets you gear up in one view.";
			}
			else
			{
				seconds = Visit.DEEP_SECONDS;
				body = "These sit " + loadout.getRowSpan() + " rows apart. Gathering them into neighbouring rows"
					+ " means one glance instead of a scroll.";
			}
			out.add(new Advice(Advice.Kind.GATHER, "Gather the " + loadout.getName() + " loadout", body,
				loadout.getItems(), seconds, loadout.getTrips(), plural(loadout.getTrips(), "trip")));
		}
	}

	private static void raiseAdvice(Journal journal, List<Visit> visits, List<Advice> out)
	{
		Map<Integer, int[]> stats = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rows = new HashMap<>();
		for (Visit trip : visits)
		{
			Set<Integer> seen = new HashSet<>();
			for (Action action : trip.getActions())
			{
				if (action.getKind() != Action.Kind.WITHDRAW || action.isSearched() || !seen.add(action.getItemId()))
				{
					continue;
				}
				int[] s = stats.computeIfAbsent(action.getItemId(), k -> new int[2]);
				s[0]++;
				if (action.isDeep())
				{
					s[1]++;
					rows.computeIfAbsent(action.getItemId(), k -> new ArrayList<>()).add(action.getRow());
				}
			}
		}
		List<Integer> deep = new ArrayList<>();
		int trips = 0;
		for (Map.Entry<Integer, int[]> e : stats.entrySet())
		{
			int[] s = e.getValue();
			if (s[0] >= MIN_REPEATS && s[1] >= s[0] * DEEP_SHARE)
			{
				deep.add(e.getKey());
				trips = Math.max(trips, s[0]);
			}
		}
		if (deep.isEmpty())
		{
			return;
		}
		deep.sort((a, b) -> Integer.compare(stats.get(b)[0], stats.get(a)[0]));
		if (deep.size() > 8)
		{
			deep = new ArrayList<>(deep.subList(0, 8));
		}
		int first = deep.get(0);
		String title;
		String body;
		if (deep.size() == 1)
		{
			int row = median(rows.get(first)) + 1;
			title = "Bring " + journal.name(first) + " up";
			body = "You have taken it out on " + plural(stats.get(first)[0], "trip") + ", and it usually sits on row "
				+ row + ", below the bank's first screen. A spot in the top rows saves the scroll every time.";
		}
		else
		{
			title = "Bring " + deep.size() + " items up";
			body = "You keep scrolling down for " + journal.name(first) + " and " + (deep.size() - 1)
				+ (deep.size() == 2 ? " other item" : " others")
				+ ". They are among your most used, yet they live below the first screen. Moving them into the top rows saves a scroll each.";
		}
		double perTrip = 0;
		for (int item : deep)
		{
			perTrip += stats.get(item)[1] / (double) Math.max(1, trips) * Visit.DEEP_SECONDS;
		}
		out.add(new Advice(Advice.Kind.RAISE, title, body, deep, perTrip, trips,
			deep.size() == 1 ? stats.get(first)[1] + " scrolls" : plural(deep.size(), "item") + " below the fold"));
	}

	private static void searchAdvice(Journal journal, List<Visit> visits, List<Advice> out)
	{
		Map<String, Integer> counts = new HashMap<>();
		Map<String, Map<Integer, Integer>> found = new HashMap<>();
		for (Visit visit : visits)
		{
			String query = null;
			for (Action action : visit.getActions())
			{
				if (action.getKind() == Action.Kind.SEARCH)
				{
					query = action.getQuery();
					counts.merge(query, 1, Integer::sum);
				}
				else if (action.getKind() == Action.Kind.WITHDRAW && action.isSearched() && query != null)
				{
					found.computeIfAbsent(query, k -> new HashMap<>()).merge(action.getItemId(), 1, Integer::sum);
				}
				else if (action.getKind() == Action.Kind.TAB)
				{
					query = null;
				}
			}
		}
		List<Map.Entry<String, Integer>> repeated = new ArrayList<>();
		for (Map.Entry<String, Integer> e : counts.entrySet())
		{
			if (e.getValue() >= MIN_REPEATS)
			{
				repeated.add(e);
			}
		}
		repeated.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		for (int i = 0; i < repeated.size() && i < 3; i++)
		{
			String query = repeated.get(i).getKey();
			int times = repeated.get(i).getValue();
			List<Integer> items = new ArrayList<>();
			Map<Integer, Integer> hits = found.getOrDefault(query, Collections.emptyMap());
			hits.entrySet().stream()
				.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
				.limit(6)
				.forEach(e -> items.add(e.getKey()));
			String body = "You have searched \"" + query + "\" " + times + " times";
			if (!items.isEmpty())
			{
				body += ", usually for " + journal.name(items.get(0));
			}
			body += ". A Bank Tags tab for it, or a home near the top, saves typing it each trip.";
			out.add(new Advice(Advice.Kind.SEARCH, "Stop searching for \"" + query + "\"", body, items,
				Visit.SEARCH_SECONDS, times, times + " searches"));
		}
	}

	private static void putBackAdvice(Journal journal, List<Visit> visits, List<Advice> out)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (Visit visit : visits)
		{
			visit.putBacks().forEach((item, n) -> counts.merge(item, n, Integer::sum));
		}
		List<Integer> items = new ArrayList<>();
		int total = 0;
		for (Map.Entry<Integer, Integer> e : counts.entrySet())
		{
			if (e.getValue() >= MIN_REPEATS)
			{
				items.add(e.getKey());
				total += e.getValue();
			}
		}
		if (items.isEmpty())
		{
			return;
		}
		items.sort((a, b) -> Integer.compare(counts.get(b), counts.get(a)));
		int first = items.get(0);
		String body = "You took " + journal.name(first) + " out and put it straight back " + counts.get(first) + " times"
			+ (items.size() > 1 ? ", and " + (items.size() - 1) + " more item" + (items.size() > 2 ? "s" : "") + " the same way" : "")
			+ ". Something beside it probably looks alike; a gap or a different neighbour stops the misclick.";
		out.add(new Advice(Advice.Kind.PUT_BACK, items.size() == 1 ? "Stop misclicking " + journal.name(first) : "Stop the misclicks",
			body, items, Visit.PUT_BACK_SECONDS, total, total + " put back"));
	}

	private static void decantAdvice(Journal journal, List<Visit> trips, List<Advice> out)
	{
		Map<String, Integer> partial = new HashMap<>();
		Map<String, Set<Integer>> items = new HashMap<>();
		for (Visit trip : trips)
		{
			Set<String> seen = new HashSet<>();
			for (int item : trip.withdrawn())
			{
				Matcher m = DOSE.matcher(journal.name(item));
				if (!m.matches())
				{
					continue;
				}
				String base = m.group(1).trim();
				items.computeIfAbsent(base, k -> new HashSet<>()).add(item);
				if (!m.group(2).equals("4") && seen.add(base))
				{
					partial.merge(base, 1, Integer::sum);
				}
			}
		}
		List<String> bases = new ArrayList<>();
		for (Map.Entry<String, Integer> e : partial.entrySet())
		{
			if (e.getValue() >= MIN_REPEATS)
			{
				bases.add(e.getKey());
			}
		}
		if (bases.isEmpty())
		{
			return;
		}
		bases.sort((a, b) -> Integer.compare(partial.get(b), partial.get(a)));
		String base = bases.get(0);
		List<Integer> shown = new ArrayList<>(items.get(base));
		shown.sort((a, b) -> journal.name(b).compareTo(journal.name(a)));
		String body = "On " + plural(partial.get(base), "trip") + " you took out partial doses of " + base
			+ (bases.size() > 1 ? " (and " + bases.get(1) + ")" : "")
			+ ". Decanting them into full doses with Bob Barter or Zahur means one click per potion.";
		out.add(new Advice(Advice.Kind.DECANT, "Decant your " + base, body, shown,
			Visit.IDEAL_SECONDS, partial.get(base), plural(partial.get(base), "trip")));
	}

	private static void paceAdvice(List<Loadout> loadouts, List<Advice> out)
	{
		for (Loadout loadout : loadouts)
		{
			int gap = loadout.getUsualMillis() - loadout.getBestMillis();
			if (loadout.getTrips() < 4 || gap < 8_000 || loadout.getUsualMillis() < loadout.getBestMillis() * 1.6)
			{
				continue;
			}
			String body = "Your best " + loadout.getName() + " gear-up took " + seconds(loadout.getBestMillis())
				+ ", but it usually takes " + seconds(loadout.getUsualMillis())
				+ ". Light the items up from the Loadouts page and take them in the same order each time.";
			// Only part of the gap is laying out the bank; the rest is the player, so it is weighed lightly.
			out.add(new Advice(Advice.Kind.PACE, "Speed up " + loadout.getName(), body, loadout.getItems(),
				gap / 1000.0 / 3, loadout.getTrips(), "best " + seconds(loadout.getBestMillis())));
		}
	}

	// ---- Helpers --------------------------------------------------------------------------

	static int median(List<Integer> values)
	{
		if (values == null || values.isEmpty())
		{
			return 0;
		}
		List<Integer> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		int n = sorted.size();
		return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
	}

	static String plural(int n, String word)
	{
		return n + " " + word + (n == 1 ? "" : "s");
	}

	private static String oneDecimal(double value)
	{
		return String.format(Locale.ROOT, "%.1f", value);
	}

	public static String seconds(int millis)
	{
		int s = (int) Math.round(millis / 1000.0);
		if (s < 60)
		{
			return s + "s";
		}
		return s / 60 + "m " + (s % 60 < 10 ? "0" : "") + s % 60 + "s";
	}
}
