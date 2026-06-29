package com.bankfriction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class BankFrictionAnalyserTest
{
	@Test
	public void buildsFrequentLoadoutRecommendationFromRepeatedWithdrawals()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		for (int i = 0; i < 3; i++)
		{
			long start = i * 10_000L;
			analyser.recordSessionStart(start);
			analyser.recordWithdraw(100, "Shark", start + 1_000L);
			analyser.recordWithdraw(101, "Prayer potion(4)", start + 2_000L);
			analyser.recordWithdraw(102, "Dragon dagger", start + 3_000L);
			analyser.recordSessionEnd(start + 4_000L);
		}

		BankFrictionRecommendation recommendation = findRecommendation(
			analyser.buildRecommendations(40_000L), BankFrictionRecommendationType.LOADOUT_GROUP);

		assertNotNull(recommendation);
		assertEquals(100, recommendation.getConfidencePercent());
		assertTrue(recommendation.getItemIds().contains(100));
		assertTrue(recommendation.getItemIds().contains(101));
		assertTrue(recommendation.getItemIds().contains(102));
	}

	@Test
	public void normalizesRepeatedRangeSearchToRanged()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSearch("range", 1_000L);
		analyser.recordSearch(" Range ", 2_000L);
		analyser.recordSearch("RANGE", 3_000L);

		BankFrictionRecommendation recommendation = findRecommendation(
			analyser.buildRecommendations(4_000L), BankFrictionRecommendationType.REPEATED_SEARCH);

		assertNotNull(recommendation);
		assertEquals("Repeated search: ranged", recommendation.getTitle());
		assertEquals(3, recommendation.getEvidenceCount());
	}

	@Test
	public void reportsSlowLoadoutDurationFromSessionBoundaries()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(1_000L);
		analyser.recordWithdraw(100, "Shark", 2_000L);
		analyser.recordWithdraw(101, "Prayer potion(4)", 3_000L);
		analyser.recordWithdraw(102, "Dragon dagger", 4_000L);
		analyser.recordSessionEnd(71_000L);

		BankFrictionRecommendation recommendation = findRecommendation(
			analyser.buildRecommendations(72_000L), BankFrictionRecommendationType.SLOW_LOADOUT);

		assertNotNull(recommendation);
		assertEquals(70, recommendation.getSeconds());
		assertTrue(recommendation.getDescription().contains("70 seconds"));
	}

	@Test
	public void groupsNearDuplicatePotionVariants()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(1_000L);
		analyser.recordWithdraw(200, "Prayer potion(4)", 2_000L);
		analyser.recordWithdraw(201, "Prayer potion(3)", 3_000L);
		analyser.recordWithdraw(202, "Prayer potion(2)", 4_000L);
		analyser.recordSessionEnd(5_000L);

		BankFrictionRecommendation recommendation = findRecommendation(
			analyser.buildRecommendations(6_000L), BankFrictionRecommendationType.NEAR_DUPLICATE);

		assertNotNull(recommendation);
		assertEquals(3, recommendation.getItemIds().size());
		assertTrue(recommendation.getDescription().contains("Prayer potion"));
	}

	@Test
	public void trimsHistoryToConfiguredSessionLimit()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser(new BankFrictionSnapshot(), 2);

		for (int i = 1; i <= 3; i++)
		{
			long start = i * 1_000L;
			analyser.recordSessionStart(start);
			analyser.recordSessionEnd(start + 100L);
		}

		assertEquals(2, analyser.snapshot().getSessions().size());
		assertEquals(2_000L, analyser.snapshot().getSessions().get(0).getStartMillis());
		assertEquals(3_000L, analyser.snapshot().getSessions().get(1).getStartMillis());
	}

	@Test
	public void preservesExplicitSessionStartAndEndTimes()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(1_000L);
		analyser.recordWithdraw(100, "Shark", 2_000L);
		analyser.recordSessionEnd(5_000L);
		analyser.recordSessionStart(10_000L);

		assertEquals(2, analyser.snapshot().getSessions().size());
		assertEquals(1_000L, analyser.snapshot().getSessions().get(0).getStartMillis());
		assertEquals(5_000L, analyser.snapshot().getSessions().get(0).getEndMillis());
		assertEquals(10_000L, analyser.snapshot().getSessions().get(1).getStartMillis());
	}

	private static BankFrictionRecommendation findRecommendation(
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
		return null;
	}
}
