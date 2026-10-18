package com.bankfriction.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One suggestion for a bank that takes less hunting, with the evidence behind it. */
public final class Advice
{
	public enum Kind
	{
		/** A loadout spread over several tabs or far apart rows. */
		GATHER,
		/** Items you keep scrolling down for. */
		RAISE,
		/** A search you keep typing. */
		SEARCH,
		/** Items you take out and put straight back. */
		PUT_BACK,
		/** Partial potion doses taken out one by one. */
		DECANT,
		/** A loadout whose usual gear-up is far slower than your best. */
		PACE
	}

	private final Kind kind;
	private final String title;
	private final String body;
	private final List<Integer> items;
	private final double secondsPerTrip;
	private final int trips;
	private final String evidence;

	Advice(Kind kind, String title, String body, List<Integer> items, double secondsPerTrip, int trips, String evidence)
	{
		this.kind = kind;
		this.title = title;
		this.body = body;
		this.items = new ArrayList<>(items);
		this.secondsPerTrip = secondsPerTrip;
		this.trips = trips;
		this.evidence = evidence;
	}

	public Kind getKind()
	{
		return kind;
	}

	public String getTitle()
	{
		return title;
	}

	public String getBody()
	{
		return body;
	}

	/** The items to light up in the bank, most important first. */
	public List<Integer> getItems()
	{
		return Collections.unmodifiableList(items);
	}

	/** About how many seconds a trip this would save. */
	public double getSecondsPerTrip()
	{
		return secondsPerTrip;
	}

	/** How many remembered trips it applies to. */
	public int getTrips()
	{
		return trips;
	}

	/** A short line of what was seen, such as "9 of 12 trips". */
	public String getEvidence()
	{
		return evidence;
	}

	/** What it is worth over the remembered trips; advice is shown most valuable first. */
	public double impact()
	{
		return secondsPerTrip * trips;
	}

	/** A stable key, so a chosen suggestion stays chosen when the list is rebuilt. */
	public String key()
	{
		return kind + ":" + items;
	}
}
