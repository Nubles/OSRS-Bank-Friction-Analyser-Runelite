package com.bankfriction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankFrictionSnapshot
{
	private final List<BankFrictionSession> sessions = new ArrayList<>();
	private final List<BankFrictionSearch> searches = new ArrayList<>();
	private final Map<Integer, String> itemNames = new HashMap<>();

	public List<BankFrictionSession> getSessions()
	{
		return sessions;
	}

	public List<BankFrictionSearch> getSearches()
	{
		return searches;
	}

	public Map<Integer, String> getItemNames()
	{
		return itemNames;
	}
}