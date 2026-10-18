package com.bankfriction.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything the panel shows, worked out from the journal in one go. */
public final class Report
{
	public static final Report EMPTY = new Report(-1, -1, 0, 0, 0, 0,
		Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

	private final int friction;
	private final int previousFriction;
	private final int trips;
	private final int visits;
	private final int usualMillis;
	private final double lostSeconds;
	private final List<Advice> advice;
	private final List<Loadout> loadouts;
	private final List<Visit> recent;
	private final Map<Integer, String> names;

	Report(int friction, int previousFriction, int trips, int visits, int usualMillis, double lostSeconds,
		List<Advice> advice, List<Loadout> loadouts, List<Visit> recent, Map<Integer, String> names)
	{
		this.friction = friction;
		this.previousFriction = previousFriction;
		this.trips = trips;
		this.visits = visits;
		this.usualMillis = usualMillis;
		this.lostSeconds = lostSeconds;
		this.advice = new ArrayList<>(advice);
		this.loadouts = new ArrayList<>(loadouts);
		this.recent = new ArrayList<>(recent);
		this.names = new HashMap<>(names);
	}

	/** Average friction of the recent gear-ups, or -1 before there are enough. */
	public int getFriction()
	{
		return friction;
	}

	/** The same over the gear-ups before those, or -1. */
	public int getPreviousFriction()
	{
		return previousFriction;
	}

	/** Visits where something was taken out. */
	public int getTrips()
	{
		return trips;
	}

	/** Every remembered visit. */
	public int getVisits()
	{
		return visits;
	}

	/** Median gear-up time, or 0. */
	public int getUsualMillis()
	{
		return usualMillis;
	}

	/** Estimated seconds lost to hunting over every remembered visit. */
	public double getLostSeconds()
	{
		return lostSeconds;
	}

	public List<Advice> getAdvice()
	{
		return Collections.unmodifiableList(advice);
	}

	public List<Loadout> getLoadouts()
	{
		return Collections.unmodifiableList(loadouts);
	}

	/** The latest visits, newest first. */
	public List<Visit> getRecent()
	{
		return Collections.unmodifiableList(recent);
	}

	public String name(int itemId)
	{
		String name = names.get(itemId);
		return name == null || name.isEmpty() ? "Item " + itemId : name;
	}
}
