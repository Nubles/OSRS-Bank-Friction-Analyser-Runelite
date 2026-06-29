package com.bankfriction;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class BankFrictionStorage
{
	private BankFrictionStorage()
	{
	}

	public static String serialize(Gson gson, BankFrictionSnapshot snapshot)
	{
		return gson.toJson(snapshot == null ? new BankFrictionSnapshot() : snapshot);
	}

	public static BankFrictionSnapshot deserialize(Gson gson, String encoded)
	{
		if (encoded == null || encoded.trim().isEmpty())
		{
			return new BankFrictionSnapshot();
		}

		try
		{
			BankFrictionSnapshot snapshot = gson.fromJson(encoded, BankFrictionSnapshot.class);
			return snapshot == null ? new BankFrictionSnapshot() : snapshot;
		}
		catch (JsonSyntaxException ex)
		{
			return new BankFrictionSnapshot();
		}
	}
}
