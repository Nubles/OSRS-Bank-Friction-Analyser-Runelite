package com.bankfriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BankFrictionSession
{
	private long startMillis;
	private long endMillis;
	private final List<BankFrictionInteraction> interactions = new ArrayList<>();

	public BankFrictionSession()
	{
		this(0L);
	}

	BankFrictionSession(long startMillis)
	{
		this.startMillis = startMillis;
		this.endMillis = startMillis;
	}

	void addInteraction(BankFrictionInteraction interaction)
	{
		interactions.add(interaction);
		endMillis = Math.max(endMillis, interaction.getTimeMillis());
	}

	void finish(long timeMillis)
	{
		endMillis = Math.max(endMillis, timeMillis);
	}

	public long getStartMillis()
	{
		return startMillis;
	}

	public long getEndMillis()
	{
		return endMillis;
	}

	public List<BankFrictionInteraction> getInteractions()
	{
		return Collections.unmodifiableList(interactions);
	}
}