package com.bankfriction.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AnalysisTest
{
	private static final int WHIP = 4151;
	private static final int DEFENDER = 12954;
	private static final int FIRE_CAPE = 6570;
	private static final int SHARK = 385;
	private static final int PRAYER_4 = 2434;
	private static final int PRAYER_3 = 139;
	private static final int RUNE_POUCH = 12791;

	private final Journal journal = new Journal();
	private final Recorder recorder = new Recorder(journal, 100);
	private long now = 1_700_000_000_000L;

	private void take(int item, String name, int tab, int row, boolean deep)
	{
		now += 1200;
		recorder.withdraw(item, name, now, row, deep);
	}

	/** A melee trip: three gear pieces from tab 1 and sharks from tab 2. */
	private void meleeTrip(boolean hop)
	{
		recorder.open(now, 1);
		take(WHIP, "Abyssal whip", 1, 0, false);
		take(DEFENDER, "Avernic defender", 1, 0, false);
		take(FIRE_CAPE, "Fire cape", 1, 1, false);
		if (hop)
		{
			now += 800;
			recorder.tab(2, now);
		}
		take(SHARK, "Shark", 2, 9, true);
		now += 3000;
		recorder.close(now);
		now += 600_000;
	}

	@Test
	public void recorderDropsVisitsWhereNothingHappened()
	{
		recorder.open(now, 0);
		assertFalse(recorder.close(now + 5000));
		assertTrue(journal.getVisits().isEmpty());
	}

	@Test
	public void recorderCountsTabHopsButNotTheOpeningTab()
	{
		meleeTrip(true);
		Visit visit = journal.getVisits().get(0);
		assertEquals(1, visit.tabSwitches());
		assertEquals(4, visit.withdrawals());
		assertEquals(1, visit.deepWithdrawals());
	}

	@Test
	public void searchIsCountedOnceAndMarksWhatItFound()
	{
		recorder.open(now, 0);
		recorder.searchText("p", now += 300);
		recorder.searchText("pr", now += 300);
		recorder.searchText("pray*", now += 300);
		take(PRAYER_4, "Prayer potion(4)", 0, 0, false);
		take(PRAYER_4, "Prayer potion(4)", 0, 0, false);
		recorder.endSearch(now += 500);
		take(SHARK, "Shark", 0, 2, false);
		recorder.close(now += 1000);

		Visit visit = journal.getVisits().get(0);
		assertEquals(1, visit.searches());
		List<Action> actions = visit.getActions();
		assertEquals(Action.Kind.SEARCH, actions.get(0).getKind());
		assertEquals("pray", actions.get(0).getQuery());
		assertTrue(actions.get(1).isSearched());
		assertTrue(actions.get(2).isSearched());
		assertFalse(actions.get(3).isSearched());
	}

	@Test
	public void quantityPromptsAreNotSearches()
	{
		recorder.open(now, 0);
		take(SHARK, "Shark", 0, 0, false);
		recorder.close(now += 1000);
		assertEquals(0, journal.getVisits().get(0).searches());
	}

	@Test
	public void idleBankSplitsTheVisit()
	{
		recorder.open(now, 0);
		take(SHARK, "Shark", 0, 0, false);
		now += Recorder.IDLE_MS + 1000;
		assertTrue(recorder.tick(now));
		take(WHIP, "Abyssal whip", 0, 0, false);
		recorder.close(now += 2000);
		assertEquals(2, journal.getVisits().size());
		assertTrue(journal.getVisits().get(1).gearUpMillis() < 5000);
	}

	@Test
	public void putBackOnlyCountsQuickReturns()
	{
		recorder.open(now, 0);
		take(SHARK, "Shark", 0, 0, false);
		recorder.deposit(SHARK, "Shark", now += 2000);
		take(WHIP, "Abyssal whip", 0, 0, false);
		recorder.deposit(WHIP, "Abyssal whip", now += 60_000);
		recorder.close(now += 1000);
		Visit visit = journal.getVisits().get(0);
		assertEquals(1, visit.putBackCount());
		assertEquals(Integer.valueOf(1), visit.putBacks().get(SHARK));
	}

	@Test
	public void frictionRisesWithHunting()
	{
		meleeTrip(false);
		meleeTrip(true);
		Visit calm = journal.getVisits().get(0);
		Visit hunted = journal.getVisits().get(1);
		assertTrue(hunted.friction() > calm.friction());
		assertTrue(calm.friction() >= 0 && hunted.friction() <= 100);
	}

	@Test
	public void sortsAdviceMostValuableFirstAndFindsTheLoadout()
	{
		for (int i = 0; i < 6; i++)
		{
			meleeTrip(true);
		}
		Report report = Analysis.of(journal);
		assertEquals(6, report.getTrips());
		assertTrue(report.getFriction() > 0);

		assertEquals(1, report.getLoadouts().size());
		Loadout loadout = report.getLoadouts().get(0);
		assertEquals(4, loadout.getItems().size());
		assertEquals("Abyssal whip & Avernic defender", loadout.getName());
		assertEquals(2, loadout.getTabs());

		List<Advice> advice = report.getAdvice();
		assertFalse(advice.isEmpty());
		for (int i = 1; i < advice.size(); i++)
		{
			assertTrue(advice.get(i - 1).impact() >= advice.get(i).impact());
		}
		assertTrue(has(advice, Advice.Kind.GATHER));
		assertTrue(has(advice, Advice.Kind.RAISE));
	}

	@Test
	public void twoLoadoutsStayApart()
	{
		for (int i = 0; i < 4; i++)
		{
			meleeTrip(false);
			recorder.open(now, 0);
			take(11785, "Armadyl crossbow", 0, 0, false);
			take(RUNE_POUCH, "Rune pouch", 0, 0, false);
			take(2444, "Ranging potion(4)", 0, 1, false);
			take(SHARK, "Shark", 0, 1, false);
			recorder.close(now += 2000);
			now += 600_000;
		}
		Report report = Analysis.of(journal);
		assertEquals(2, report.getLoadouts().size());
	}

	@Test
	public void repeatedSearchBecomesAdvice()
	{
		for (int i = 0; i < 3; i++)
		{
			recorder.open(now, 0);
			recorder.searchText("rune pouch", now += 500);
			take(RUNE_POUCH, "Rune pouch", 0, 0, false);
			recorder.close(now += 1000);
			now += 600_000;
		}
		Advice search = find(Analysis.of(journal).getAdvice(), Advice.Kind.SEARCH);
		assertNotNull(search);
		assertEquals(Arrays.asList(RUNE_POUCH), search.getItems());
	}

	@Test
	public void partialDosesSuggestDecanting()
	{
		for (int i = 0; i < 3; i++)
		{
			recorder.open(now, 0);
			take(PRAYER_4, "Prayer potion(4)", 0, 0, false);
			take(PRAYER_3, "Prayer potion(3)", 0, 0, false);
			recorder.close(now += 1000);
			now += 600_000;
		}
		Advice decant = find(Analysis.of(journal).getAdvice(), Advice.Kind.DECANT);
		assertNotNull(decant);
		assertEquals("Decant your Prayer potion", decant.getTitle());
	}

	@Test
	public void emptyJournalHasNoScore()
	{
		Report report = Analysis.of(journal);
		assertEquals(-1, report.getFriction());
		assertTrue(report.getAdvice().isEmpty());
	}

	@Test
	public void journalSurvivesASaveAndLoad()
	{
		meleeTrip(true);
		Gson gson = new Gson();
		Journal read = gson.fromJson(gson.toJson(journal), Journal.class).repair();
		assertEquals(1, read.getVisits().size());
		Visit visit = read.getVisits().get(0);
		assertEquals(4, visit.withdrawals());
		assertEquals(1, visit.tabSwitches());
		assertEquals("Shark", read.name(SHARK));
	}

	@Test
	public void trimForgetsOldVisitsAndTheirNames()
	{
		recorder.setKeep(1);
		recorder.open(now, 0);
		take(SHARK, "Shark", 0, 0, false);
		recorder.close(now += 1000);
		recorder.open(now, 0);
		take(WHIP, "Abyssal whip", 0, 0, false);
		recorder.close(now += 1000);
		assertEquals(1, journal.getVisits().size());
		assertFalse(journal.getNames().containsKey(SHARK));
	}

	@Test
	public void readsTheFirstVersionsHistory()
	{
		String old = "{\"sessions\":[{\"startMillis\":1000,\"endMillis\":9000,\"interactions\":["
			+ "{\"type\":\"WITHDRAW\",\"itemId\":4151,\"itemName\":\"Abyssal whip\",\"timeMillis\":2000},"
			+ "{\"type\":\"TAB_SWITCH\",\"itemId\":-1,\"itemName\":\"Bank tab\",\"timeMillis\":3000}]},"
			+ "{\"startMillis\":20000,\"endMillis\":21000,\"interactions\":[]}],"
			+ "\"searches\":[{\"query\":\"Whip\",\"timeMillis\":2500}],"
			+ "\"itemNames\":{\"4151\":\"Abyssal whip\"}}";
		Journal read = Legacy.read(old);
		assertNotNull(read);
		assertEquals(1, read.getVisits().size());
		Visit visit = read.getVisits().get(0);
		assertEquals(1, visit.withdrawals());
		assertEquals(1, visit.tabSwitches());
		assertEquals(1, visit.searches());
		assertEquals("Abyssal whip", read.name(WHIP));
		assertNull(Legacy.read("not json"));
		assertNull(Legacy.read(""));
	}

	private static boolean has(List<Advice> advice, Advice.Kind kind)
	{
		return find(advice, kind) != null;
	}

	private static Advice find(List<Advice> advice, Advice.Kind kind)
	{
		for (Advice a : advice)
		{
			if (a.getKind() == kind)
			{
				return a;
			}
		}
		return null;
	}
}
