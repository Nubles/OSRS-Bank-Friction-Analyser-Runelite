package com.bankfriction;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

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

	@ConfigItem(
		keyName = "showPassiveHighlights",
		name = "Show passive highlights",
		description = "Highlight visible bank items related to the selected recommendation. This never clicks or moves items.",
		position = 1
	)
	default boolean showPassiveHighlights()
	{
		return true;
	}

	@Range(
		min = 10,
		max = 250
	)
	@ConfigItem(
		keyName = "maximumRetainedSessions",
		name = "Retained sessions",
		description = "Maximum number of local bank sessions to retain for recommendations.",
		position = 2
	)
	default int maximumRetainedSessions()
	{
		return 75;
	}
}
