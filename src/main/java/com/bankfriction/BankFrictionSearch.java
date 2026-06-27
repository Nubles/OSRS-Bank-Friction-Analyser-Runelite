package com.bankfriction;

public class BankFrictionSearch
{
	private String query;
	private long timeMillis;

	public BankFrictionSearch()
	{
		this("", 0L);
	}

	BankFrictionSearch(String query, long timeMillis)
	{
		this.query = query == null ? "" : query;
		this.timeMillis = timeMillis;
	}

	public String getQuery()
	{
		return query;
	}

	public long getTimeMillis()
	{
		return timeMillis;
	}
}