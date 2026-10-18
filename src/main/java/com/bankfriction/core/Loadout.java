package com.bankfriction.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A set of items you keep taking out together, found by grouping visits that look alike. */
public final class Loadout
{
	private final String name;
	private final List<Integer> items;
	private final int trips;
	private final int usualMillis;
	private final int bestMillis;
	private final double tabSwitches;
	private final int tabs;
	private final int rowSpan;
	private final int friction;
	private final long lastSeen;

	Loadout(String name, List<Integer> items, int trips, int usualMillis, int bestMillis,
		double tabSwitches, int tabs, int rowSpan, int friction, long lastSeen)
	{
		this.name = name;
		this.items = new ArrayList<>(items);
		this.trips = trips;
		this.usualMillis = usualMillis;
		this.bestMillis = bestMillis;
		this.tabSwitches = tabSwitches;
		this.tabs = tabs;
		this.rowSpan = rowSpan;
		this.friction = friction;
		this.lastSeen = lastSeen;
	}

	public String getName()
	{
		return name;
	}

	/** The core items, the most constant first. */
	public List<Integer> getItems()
	{
		return Collections.unmodifiableList(items);
	}

	public int getTrips()
	{
		return trips;
	}

	/** The median time from opening the bank to taking the last item out. */
	public int getUsualMillis()
	{
		return usualMillis;
	}

	public int getBestMillis()
	{
		return bestMillis;
	}

	/** Tab hops per trip, on average. */
	public double getTabSwitches()
	{
		return tabSwitches;
	}

	/** How many tabs the core items are taken from. */
	public int getTabs()
	{
		return tabs;
	}

	/** Rows between the highest and lowest core item, or -1 when unknown. */
	public int getRowSpan()
	{
		return rowSpan;
	}

	/** Average friction of its trips. */
	public int getFriction()
	{
		return friction;
	}

	public long getLastSeen()
	{
		return lastSeen;
	}

	public String key()
	{
		return "loadout:" + items;
	}
}
