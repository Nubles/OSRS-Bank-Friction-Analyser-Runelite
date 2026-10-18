package com.bankfriction.core;

import java.util.Locale;

/**
 * Turns what happens at the bank into visits in the journal. The plugin calls it on the client
 * thread; it holds no RuneLite types so it can be tested on its own.
 */
public final class Recorder
{
	/** A bank left open and untouched this long ends the visit; the next action begins another. */
	public static final long IDLE_MS = 90_000L;
	/** A visit picked up after a pause is taken to have begun this long before its first action. */
	private static final int RESUME_LEAD_MS = 600;

	private final Journal journal;
	private int keep;
	private Visit current;
	private boolean open;
	private long lastAction;
	private int tab = -1;
	private String pendingSearch;
	private String countedSearch;

	public Recorder(Journal journal, int keep)
	{
		this.journal = journal;
		this.keep = keep;
	}

	public Journal getJournal()
	{
		return journal;
	}

	public void setKeep(int keep)
	{
		this.keep = keep;
		journal.trim(keep);
	}

	public boolean isOpen()
	{
		return open;
	}

	/** The visit in progress, or null. */
	public Visit current()
	{
		return current;
	}

	/** The bank opened on the given tab. */
	public void open(long now, int tab)
	{
		if (open)
		{
			return;
		}
		open = true;
		current = new Visit(now);
		lastAction = now;
		this.tab = tab;
		pendingSearch = null;
		countedSearch = null;
	}

	/**
	 * The bank closed.
	 *
	 * @return true when a visit was added to the journal
	 */
	public boolean close(long now)
	{
		if (!open)
		{
			return false;
		}
		endSearch(now);
		open = false;
		tab = -1;
		return finish(now);
	}

	/**
	 * Called every game tick while the bank is open; ends a visit that has sat idle.
	 *
	 * @return true when a visit was added to the journal
	 */
	public boolean tick(long now)
	{
		if (!open || current == null || now - lastAction < IDLE_MS)
		{
			return false;
		}
		endSearch(lastAction);
		return finish(lastAction);
	}

	public void withdraw(int itemId, String name, long now, int row, boolean deep)
	{
		if (!ready(itemId, name, now))
		{
			return;
		}
		boolean searched = pendingSearch != null || countedSearch != null;
		countSearch(now);
		current.add(Action.withdraw(itemId, time(now), row, tab, deep, searched));
	}

	public void deposit(int itemId, String name, long now)
	{
		if (ready(itemId, name, now))
		{
			current.add(Action.deposit(itemId, time(now)));
		}
	}

	public void rearrange(int itemId, String name, long now)
	{
		if (ready(itemId, name, now))
		{
			current.add(Action.rearrange(itemId, time(now)));
		}
	}

	/** The open bank tab is now this one. */
	public void tab(int newTab, long now)
	{
		if (!open || newTab == tab)
		{
			return;
		}
		boolean first = tab < 0;
		tab = newTab;
		if (!first)
		{
			resume(now);
			current.add(Action.tab(newTab, time(now)));
		}
	}

	/** The bank search box shows this text. */
	public void searchText(String text, long now)
	{
		if (!open)
		{
			return;
		}
		String query = normalize(text);
		if (query.isEmpty() || query.equals(countedSearch))
		{
			return;
		}
		pendingSearch = query;
		lastAction = now;
	}

	/** The bank search was closed. */
	public void endSearch(long now)
	{
		if (open && pendingSearch != null)
		{
			countSearch(now);
		}
		pendingSearch = null;
		countedSearch = null;
	}

	public static String normalize(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("*", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	private void countSearch(long now)
	{
		if (pendingSearch == null)
		{
			return;
		}
		resume(now);
		current.add(Action.search(pendingSearch, time(now)));
		countedSearch = pendingSearch;
		pendingSearch = null;
	}

	private boolean ready(int itemId, String name, long now)
	{
		if (!open || itemId <= 0)
		{
			return false;
		}
		resume(now);
		if (name != null && !name.isEmpty())
		{
			journal.getNames().put(itemId, name);
		}
		return true;
	}

	/** Starts a fresh visit when the last one ended for being idle. */
	private void resume(long now)
	{
		if (current == null)
		{
			current = new Visit(now - RESUME_LEAD_MS);
		}
		lastAction = now;
	}

	private int time(long now)
	{
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, now - current.getStart()));
	}

	private boolean finish(long end)
	{
		Visit visit = current;
		current = null;
		if (visit == null || visit.isEmpty())
		{
			return false;
		}
		visit.finish(end);
		journal.add(visit, keep);
		return true;
	}
}
