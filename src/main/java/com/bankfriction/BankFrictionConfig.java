package com.bankfriction;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BankFrictionConfig.GROUP)
public interface BankFrictionConfig extends Config
{
	String GROUP = "bankfrictionanalyser";

	@ConfigItem(
		keyName = "collectBehaviorData",
		name = "Analyse bank behavior",
		description = "Collect local bank interaction data to build organization recommendations. No data is sent anywhere.",
		position = 0
	)
	default boolean collectBehaviorData()
	{
		return true;
	}
}