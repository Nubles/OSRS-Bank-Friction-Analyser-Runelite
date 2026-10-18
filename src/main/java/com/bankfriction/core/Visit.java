package com.bankfriction.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One bank visit, from opening the bank to closing it, and what it cost.
 *
 * <p>Friction is an estimate of the time spent hunting rather than taking: every tab hop,
 * search, scroll below the first screen and item put straight back costs a little, and that is
 * set against the second or so a well laid out bank needs per item.
 */
public final class Visit
{
	/** Seconds a tab hop costs: the click and finding your place in the new tab. */
	public static final double TAB_SECONDS = 1.2;
	/** Seconds a search costs: opening it, typing and reading the results. */
	public static final double SEARCH_SECONDS = 4.0;
	/** Seconds a scroll below the bank's first screen costs. */
	public static final double DEEP_SECONDS = 1.8;
	/** Seconds an item taken out and put straight back costs. */
	public static final double PUT_BACK_SECONDS = 2.4;
	/** Seconds a well placed item needs: one click and a glance. */
	public static final double IDEAL_SECONDS = 1.2;
	/** An item put back within this long of taking it was a mistake, not a change of plan. */
	static final int PUT_BACK_WINDOW_MS = 15_000;
	/** No visit records more actions than this, so one long session cannot fill the profile. */
	static final int MAX_ACTIONS = 120;

	private long start;
	private int dur;
	private final List<Action> a = new ArrayList<>();

	public Visit()
	{
	}

	Visit(long start)
	{
		this.start = start;
	}

	void add(Action action)
	{
		if (a.size() < MAX_ACTIONS)
		{
			a.add(action);
		}
		dur = Math.max(dur, action.getTime());
	}

	void finish(long end)
	{
		dur = (int) Math.max(dur, Math.min(Integer.MAX_VALUE, end - start));
	}

	public long getStart()
	{
		return start;
	}

	public int getDuration()
	{
		return dur;
	}

	public List<Action> getActions()
	{
		return Collections.unmodifiableList(a);
	}

	/** True when the visit did anything worth remembering. */
	public boolean isEmpty()
	{
		return a.isEmpty();
	}

	/** Every item taken out, in the order first taken. */
	public Set<Integer> withdrawn()
	{
		Set<Integer> items = new LinkedHashSet<>();
		for (Action action : a)
		{
			if (action.getKind() == Action.Kind.WITHDRAW && action.getItemId() > 0)
			{
				items.add(action.getItemId());
			}
		}
		return items;
	}

	public int withdrawals()
	{
		return count(Action.Kind.WITHDRAW);
	}

	public int tabSwitches()
	{
		return count(Action.Kind.TAB);
	}

	public int searches()
	{
		return count(Action.Kind.SEARCH);
	}

	public int rearranges()
	{
		return count(Action.Kind.REARRANGE);
	}

	/** Items that had to be scrolled to. */
	public int deepWithdrawals()
	{
		int n = 0;
		for (Action action : a)
		{
			if (action.getKind() == Action.Kind.WITHDRAW && action.isDeep())
			{
				n++;
			}
		}
		return n;
	}

	/** The tabs items were taken from, the main tab included. */
	public Set<Integer> tabsUsed()
	{
		Set<Integer> tabs = new HashSet<>();
		for (Action action : a)
		{
			if (action.getKind() == Action.Kind.WITHDRAW && action.getTab() >= 0)
			{
				tabs.add(action.getTab());
			}
		}
		return tabs;
	}

	/** How many times each item was taken out and put straight back in. */
	public Map<Integer, Integer> putBacks()
	{
		Map<Integer, Integer> taken = new HashMap<>();
		Map<Integer, Integer> result = new HashMap<>();
		for (Action action : a)
		{
			int item = action.getItemId();
			if (item <= 0)
			{
				continue;
			}
			if (action.getKind() == Action.Kind.WITHDRAW)
			{
				taken.put(item, action.getTime());
			}
			else if (action.getKind() == Action.Kind.DEPOSIT)
			{
				Integer at = taken.remove(item);
				if (at != null && action.getTime() - at <= PUT_BACK_WINDOW_MS)
				{
					result.merge(item, 1, Integer::sum);
				}
			}
		}
		return result;
	}

	public int putBackCount()
	{
		int n = 0;
		for (int c : putBacks().values())
		{
			n += c;
		}
		return n;
	}

	/** From opening the bank to taking the last item out; the whole visit when nothing was taken. */
	public int gearUpMillis()
	{
		int last = -1;
		for (Action action : a)
		{
			if (action.getKind() == Action.Kind.WITHDRAW)
			{
				last = Math.max(last, action.getTime());
			}
		}
		return last < 0 ? dur : last;
	}

	/** Estimated seconds lost to hunting for items. */
	public double lostSeconds()
	{
		return tabSwitches() * TAB_SECONDS
			+ searches() * SEARCH_SECONDS
			+ deepWithdrawals() * DEEP_SECONDS
			+ putBackCount() * PUT_BACK_SECONDS;
	}

	/** 0 for a bank where everything was at hand, rising toward 100 as hunting outweighs taking. */
	public int friction()
	{
		double lost = lostSeconds();
		double ideal = Math.max(1, withdrawals()) * IDEAL_SECONDS;
		return (int) Math.round(100 * lost / (lost + ideal));
	}

	private int count(Action.Kind kind)
	{
		int n = 0;
		for (Action action : a)
		{
			if (action.getKind() == kind)
			{
				n++;
			}
		}
		return n;
	}
}
