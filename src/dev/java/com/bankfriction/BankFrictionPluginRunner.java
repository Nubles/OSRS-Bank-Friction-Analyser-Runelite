package com.bankfriction;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankFrictionPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankFrictionPlugin.class);
		RuneLite.main(args);
	}
}