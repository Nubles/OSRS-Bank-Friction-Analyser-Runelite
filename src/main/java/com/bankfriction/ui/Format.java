package com.bankfriction.ui;

/** How times and counts are written in the panel. */
public final class Format
{
	private Format()
	{
	}

	/** 9s, 47s, 1m 05s. */
	public static String seconds(int millis)
	{
		int s = (int) Math.round(millis / 1000.0);
		if (s < 60)
		{
			return s + "s";
		}
		return s / 60 + "m " + (s % 60 < 10 ? "0" : "") + s % 60 + "s";
	}

	/** Roughly, for totals: 40s, 12m, 3h 20m. */
	public static String duration(double seconds)
	{
		long s = Math.round(seconds);
		if (s < 60)
		{
			return s + "s";
		}
		long m = s / 60;
		if (m < 60)
		{
			return m + "m";
		}
		return m / 60 + "h " + m % 60 + "m";
	}

	/** How long ago: just now, 5m ago, 3h ago, 2d ago. */
	public static String ago(long then, long now)
	{
		long m = Math.max(0, now - then) / 60_000L;
		if (m < 1)
		{
			return "just now";
		}
		if (m < 60)
		{
			return m + "m ago";
		}
		long h = m / 60;
		if (h < 24)
		{
			return h + "h ago";
		}
		return h / 24 + "d ago";
	}

	public static String plural(int n, String word)
	{
		return n + " " + word + (n == 1 ? "" : "s");
	}
}
