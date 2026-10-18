package com.bankfriction.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every bank visit remembered for one account, oldest first, and the names of the items they
 * mention. Saved as one Gson blob in the account's RuneLite profile.
 */
public final class Journal
{
	public static final int VERSION = 2;

	private int version = VERSION;
	private final List<Visit> visits = new ArrayList<>();
	private final Map<Integer, String> names = new HashMap<>();

	public List<Visit> getVisits()
	{
		return visits;
	}

	public Map<Integer, String> getNames()
	{
		return names;
	}

	public int getVersion()
	{
		return version;
	}

	public String name(int itemId)
	{
		String name = names.get(itemId);
		return name == null || name.isEmpty() ? "Item " + itemId : name;
	}

	void add(Visit visit, int keep)
	{
		visits.add(visit);
		trim(keep);
	}

	/** Forgets the oldest visits beyond the limit, and the names nothing mentions any more. */
	public void trim(int keep)
	{
		int limit = Math.max(1, keep);
		if (visits.size() > limit)
		{
			visits.subList(0, visits.size() - limit).clear();
			Map<Integer, Boolean> used = new HashMap<>();
			for (Visit visit : visits)
			{
				for (Action action : visit.getActions())
				{
					used.put(action.getItemId(), true);
				}
			}
			names.keySet().removeIf(id -> !used.containsKey(id));
		}
	}

	/** Mends what an older or damaged save may lack. */
	public Journal repair()
	{
		visits.removeIf(v -> v == null || v.getActions().isEmpty());
		List<Visit> sorted = new ArrayList<>(visits);
		sorted.sort((x, y) -> Long.compare(x.getStart(), y.getStart()));
		visits.clear();
		visits.addAll(sorted);
		names.values().removeIf(n -> n == null);
		version = VERSION;
		return this;
	}
}
