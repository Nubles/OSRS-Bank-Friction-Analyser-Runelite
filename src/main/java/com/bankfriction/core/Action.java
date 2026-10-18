package com.bankfriction.core;

/**
 * One thing done during a bank visit. Field names are kept short because every visit is saved
 * in the RuneLite profile.
 */
public final class Action
{
	public enum Kind
	{
		/** An item taken out of the bank. */
		WITHDRAW,
		/** An item put into the bank from the inventory. */
		DEPOSIT,
		/** A different bank tab opened. */
		TAB,
		/** A finished bank search; the text is in {@link #getQuery()}. */
		SEARCH,
		/** An item dragged to a new place in the bank. */
		REARRANGE
	}

	private Kind k;
	/** The item, or 0. */
	private int i;
	/** Milliseconds after the visit began. */
	private int t;
	/** The bank row the item sat on when taken, counted from the top of the view, or -1. */
	private int r = -1;
	/** The bank tab open at the time, 0 for the main tab, or -1. */
	private int b = -1;
	/** True when the item was below the bank's first screen, so it had to be scrolled to. */
	private boolean d;
	/** True when the item was taken while a search was showing. */
	private boolean s;
	/** The search text, for searches. */
	private String q;

	public Action()
	{
	}

	private Action(Kind kind, int itemId, int time)
	{
		this.k = kind;
		this.i = itemId;
		this.t = time;
	}

	public static Action withdraw(int itemId, int time, int row, int tab, boolean deep, boolean searched)
	{
		Action a = new Action(Kind.WITHDRAW, itemId, time);
		a.r = row;
		a.b = tab;
		a.d = deep;
		a.s = searched;
		return a;
	}

	public static Action deposit(int itemId, int time)
	{
		return new Action(Kind.DEPOSIT, itemId, time);
	}

	public static Action tab(int tab, int time)
	{
		Action a = new Action(Kind.TAB, 0, time);
		a.b = tab;
		return a;
	}

	public static Action search(String query, int time)
	{
		Action a = new Action(Kind.SEARCH, 0, time);
		a.q = query;
		return a;
	}

	public static Action rearrange(int itemId, int time)
	{
		return new Action(Kind.REARRANGE, itemId, time);
	}

	public Kind getKind()
	{
		return k;
	}

	public int getItemId()
	{
		return i;
	}

	public int getTime()
	{
		return t;
	}

	public int getRow()
	{
		return r;
	}

	public int getTab()
	{
		return b;
	}

	public boolean isDeep()
	{
		return d;
	}

	public boolean isSearched()
	{
		return s;
	}

	public String getQuery()
	{
		return q == null ? "" : q;
	}
}
