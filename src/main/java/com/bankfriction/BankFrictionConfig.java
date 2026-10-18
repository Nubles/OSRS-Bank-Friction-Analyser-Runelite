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
		name = "Watch bank visits",
		description = "Remember what you do at the bank to find what slows you down. It is kept in your RuneLite profile and never sent anywhere.",
		position = 0
	)
	default boolean collectBehaviorData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPassiveHighlights",
		name = "Light up items",
		description = "Ring the bank items of the advice, loadout or visit you click in the panel. Nothing is clicked or moved for you.",
		position = 1
	)
	default boolean showPassiveHighlights()
	{
		return true;
	}

	@Range(
		min = 20,
		max = 300
	)
	@ConfigItem(
		keyName = "maximumRetainedSessions",
		name = "Visits remembered",
		description = "How many bank visits to remember per account. More gives steadier advice; fewer reacts faster to a new layout.",
		position = 2
	)
	default int maximumRetainedSessions()
	{
		return 150;
	}
}
