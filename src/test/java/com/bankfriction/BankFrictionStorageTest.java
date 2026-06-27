package com.bankfriction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BankFrictionStorageTest
{
	@Test
	public void roundTripsAnalyserState()
	{
		BankFrictionAnalyser analyser = new BankFrictionAnalyser();

		analyser.recordSessionStart(1_000L);
		analyser.recordWithdraw(4151, "Abyssal whip", 2_000L);
		analyser.recordSearch("whip", 3_000L);
		analyser.recordSessionEnd(4_000L);

		String encoded = BankFrictionStorage.serialize(analyser.snapshot());
		BankFrictionSnapshot decoded = BankFrictionStorage.deserialize(encoded);

		assertEquals(analyser.snapshot().getSessions().size(), decoded.getSessions().size());
		assertEquals(analyser.snapshot().getSearches().size(), decoded.getSearches().size());
		assertEquals("Abyssal whip", decoded.getItemNames().get(4151));
	}

	@Test
	public void emptyOrBrokenStorageReturnsEmptySnapshot()
	{
		assertEquals(0, BankFrictionStorage.deserialize("").getSessions().size());
		assertEquals(0, BankFrictionStorage.deserialize("{broken").getSearches().size());
	}
}
