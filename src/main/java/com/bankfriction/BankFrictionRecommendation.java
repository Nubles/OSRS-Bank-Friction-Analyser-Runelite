package com.bankfriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BankFrictionRecommendation
{
	private final BankFrictionRecommendationType type;
	private final String title;
	private final String description;
	private final List<Integer> itemIds;
	private final int confidencePercent;
	private final int evidenceCount;
	private final int seconds;

	BankFrictionRecommendation(
		BankFrictionRecommendationType type,
		String title,
		String description,
		List<Integer> itemIds,
		int confidencePercent,
		int evidenceCount,
		int seconds)
	{
		this.type = type;
		this.title = title;
		this.description = description;
		this.itemIds = new ArrayList<>(itemIds);
		this.confidencePercent = confidencePercent;
		this.evidenceCount = evidenceCount;
		this.seconds = seconds;
	}

	public BankFrictionRecommendationType getType()
	{
		return type;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public List<Integer> getItemIds()
	{
		return Collections.unmodifiableList(itemIds);
	}

	public int getConfidencePercent()
	{
		return confidencePercent;
	}

	public int getEvidenceCount()
	{
		return evidenceCount;
	}

	public int getSeconds()
	{
		return seconds;
	}
}