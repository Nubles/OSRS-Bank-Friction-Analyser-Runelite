package com.bankfriction;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class BankFrictionStorage
{
	private static final Gson GSON = new Gson();

	private BankFrictionStorage()
	{
	}

	public static String serialize(BankFrictionSnapshot snapshot)
	{
		return GSON.toJson(snapshot == null ? new BankFrictionSnapshot() : snapshot);
	}

	public static BankFrictionSnapshot deserialize(String encoded)
	{
		if (encoded == null || encoded.trim().isEmpty())
		{
			return new BankFrictionSnapshot();
		}

		try
		{
			BankFrictionSnapshot snapshot = GSON.fromJson(encoded, BankFrictionSnapshot.class);
			return snapshot == null ? new BankFrictionSnapshot() : snapshot;
		}
		catch (JsonSyntaxException ex)
		{
			return new BankFrictionSnapshot();
		}
	}
}