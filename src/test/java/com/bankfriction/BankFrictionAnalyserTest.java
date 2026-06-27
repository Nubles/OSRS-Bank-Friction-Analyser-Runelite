package com.bankfriction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class BankFrictionAnalyserTest
{
	private static final long START = 1_000L;

	@Test
	public void recommendsItemsWithdrawnTogetherAsLoadout()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		withdrawSession(analyser, 10, 20, 30, 40);
		withdrawSession(analyser, 10, 20, 30, 40);
		withdrawSession(analyser, 10, 20, 30, 40);
		withdrawSession(analyser, 10, 20, 30, 40);
		withdrawSession(analyser, 10, 20, 30, 40);
		withdrawSession(analyser, 10, 20, 30, 99);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 100_000L);

		assertFalse(recommendations.isEmpty());
		BankFrictionRecommendation recommendation = recommendations.get(0);
		assertEquals(BankFrictionRecommendationType.LOADOUT_GROUP, recommendation.getType());
		assertTrue(recommendation.getItemIds().contains(10));
		assertTrue(recommendation.getItemIds().contains(20));
		assertTrue(recommendation.getItemIds().contains(30));
		assertEquals(83, recommendation.getConfidencePercent());
	}

	@Test
	public void startingANewSessionFinishesThePreviousSession()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(START);
		analyser.recordWithdraw(1, "Item 1", START + 1_000L);
		analyser.recordSessionStart(START + 10_000L);
		analyser.recordWithdraw(2, "Item 2", START + 11_000L);

		assertEquals(2, analyser.snapshot().getSessions().size());
		assertEquals(START + 1_000L, analyser.snapshot().getSessions().get(0).getEndMillis());
	}

	@Test
	public void recommendsRepeatedSearches()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSearch("ranged", START);
		analyser.recordSearch("Ranged", START + 10_000L);
		analyser.recordSearch(" range ", START + 20_000L);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 30_000L);

		BankFrictionRecommendation recommendation = find(recommendations, BankFrictionRecommendationType.REPEATED_SEARCH);
		assertEquals("Repeated search: ranged", recommendation.getTitle());
		assertEquals(3, recommendation.getEvidenceCount());
	}

	@Test
	public void recommendsSlowLoadoutAssembly()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(START);
		analyser.recordWithdraw(100, "Amulet of glory", START + 1_000L);
		analyser.recordWithdraw(200, "Dragon dagger", START + 45_000L);
		analyser.recordWithdraw(300, "Shark", START + 92_000L);
		analyser.recordSessionEnd(START + 120_000L);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 130_000L);

		BankFrictionRecommendation recommendation = find(recommendations, BankFrictionRecommendationType.SLOW_LOADOUT);
		assertEquals(120, recommendation.getSeconds());
		assertTrue(recommendation.getDescription().contains("took about 120 seconds"));
	}

	@Test
	public void doesNotTreatOrdinaryNumberedItemNamesAsNearDuplicates()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(START);
		analyser.recordWithdraw(10, "Item 10", START + 1_000L);
		analyser.recordWithdraw(20, "Item 20", START + 2_000L);
		analyser.recordWithdraw(30, "Item 30", START + 3_000L);
		analyser.recordSessionEnd(START + 4_000L);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 5_000L);

		for (BankFrictionRecommendation recommendation : recommendations)
		{
			assertFalse(recommendation.getType() == BankFrictionRecommendationType.NEAR_DUPLICATE);
		}
	}

	@Test
	public void recommendsNearDuplicateNamesUsedCloseTogether()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(START);
		analyser.recordWithdraw(1000, "Super restore(4)", START + 1_000L);
		analyser.recordWithdraw(1001, "Super restore(3)", START + 5_000L);
		analyser.recordWithdraw(1002, "Super restore(2)", START + 9_000L);
		analyser.recordSessionEnd(START + 20_000L);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 30_000L);

		BankFrictionRecommendation recommendation = find(recommendations, BankFrictionRecommendationType.NEAR_DUPLICATE);
		assertTrue(recommendation.getDescription().contains("Super restore"));
		assertEquals(3, recommendation.getEvidenceCount());
	}

	@Test
	public void ignoresSingleOneOffWithdrawals()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		withdrawSession(analyser, 1, 2);

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(START + 10_000L);

		assertTrue(recommendations.isEmpty());
	}

	private static void withdrawSession(BankFrictionAnalyser analyser, int... itemIds)
	{
		analyser.recordSessionStart(START);
		long time = START;
		for (int itemId : itemIds)
		{
			analyser.recordWithdraw(itemId, "Item " + itemId, time);
			time += 1_000L;
		}
		analyser.recordSessionEnd(time);
	}

	private static BankFrictionRecommendation find(
		List<BankFrictionRecommendation> recommendations,
		BankFrictionRecommendationType type)
	{
		for (BankFrictionRecommendation recommendation : recommendations)
		{
			if (recommendation.getType() == type)
			{
				return recommendation;
			}
		}

		throw new AssertionError("No recommendation with type " + type);
	}
}
