package com.bankfriction;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Bank Friction Analyser",
	description = "Analyses bank interaction habits locally and recommends lower-friction item organization.",
	tags = {"bank", "analytics", "organizer", "loadout"}
)
public class BankFrictionPlugin extends Plugin
{
	private static final String STORAGE_KEY = "snapshot";
	private static final long SESSION_IDLE_TIMEOUT_MILLIS = 90_000L;
	private static final long SAVE_INTERVAL_MILLIS = 60_000L;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BankFrictionConfig config;

	private BankFrictionAnalyser analyser;
	private BankFrictionPanel panel;
	private NavigationButton navigationButton;
	private long lastInteractionMillis;
	private long lastSaveMillis;
	private String pendingSearchText = "";

	@Provides
	BankFrictionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankFrictionConfig.class);
	}

	@Override
	protected void startUp()
	{
		BankFrictionSnapshot snapshot = BankFrictionStorage.deserialize(
			configManager.getConfiguration(BankFrictionConfig.GROUP, STORAGE_KEY));
		analyser = new BankFrictionAnalyser(snapshot);
		panel = new BankFrictionPanel();
		navigationButton = NavigationButton.builder()
			.tooltip("Bank Friction Analyser")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		recordPendingSearch(System.currentTimeMillis());
		persist();
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		panel = null;
		navigationButton = null;
		analyser = null;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!config.collectBehaviorData() || analyser == null || !isBankOpen())
		{
			return;
		}

		String option = clean(event.getMenuOption());
		String target = clean(event.getMenuTarget());
		long now = System.currentTimeMillis();

		if (isWithdrawOption(option))
		{
			analyser.recordWithdraw(event.getItemId(), target, now);
			recordInteraction(now);
		}
		else if (isDepositOption(option))
		{
			analyser.recordDeposit(event.getItemId(), target, now);
			recordInteraction(now);
		}
		else if (isManualRepositionOption(option))
		{
			analyser.recordManualReposition(event.getItemId(), target, now);
			recordInteraction(now);
		}
		else if (isTabSwitchOption(option, target))
		{
			analyser.recordTabSwitch(now);
			recordInteraction(now);
		}
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event)
	{
		if (!config.collectBehaviorData() || analyser == null || event.getIndex() != VarClientStr.INPUT_TEXT || !isBankOpen())
		{
			return;
		}

		String searchText = clean(client.getVarcStrValue(VarClientStr.INPUT_TEXT));
		if (!searchText.isEmpty())
		{
			pendingSearchText = searchText;
			recordInteraction(System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (analyser == null)
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (!isBankOpen())
		{
			recordPendingSearch(now);
		}

		if (lastInteractionMillis > 0L && now - lastInteractionMillis > SESSION_IDLE_TIMEOUT_MILLIS)
		{
			recordPendingSearch(lastInteractionMillis);
			analyser.recordSessionEnd(lastInteractionMillis);
			lastInteractionMillis = 0L;
			refreshPanel();
			persist();
		}
		else if (lastSaveMillis > 0L && now - lastSaveMillis > SAVE_INTERVAL_MILLIS)
		{
			persist();
		}
	}

	private void recordInteraction(long now)
	{
		lastInteractionMillis = now;
		if (lastSaveMillis == 0L)
		{
			lastSaveMillis = now;
		}
		refreshPanel();
	}

	private void recordPendingSearch(long now)
	{
		if (!pendingSearchText.isEmpty())
		{
			analyser.recordSearch(pendingSearchText, now);
			pendingSearchText = "";
		}
	}

	private void refreshPanel()
	{
		if (panel == null || analyser == null)
		{
			return;
		}

		List<BankFrictionRecommendation> recommendations = analyser.buildRecommendations(System.currentTimeMillis());
		panel.updateRecommendations(recommendations);
	}

	private void persist()
	{
		if (analyser != null)
		{
			configManager.setConfiguration(
				BankFrictionConfig.GROUP,
				STORAGE_KEY,
				BankFrictionStorage.serialize(analyser.snapshot()));
			lastSaveMillis = System.currentTimeMillis();
		}
	}

	private static boolean isWithdrawOption(String option)
	{
		return option.startsWith("Withdraw");
	}

	private static boolean isDepositOption(String option)
	{
		return option.startsWith("Deposit");
	}

	private boolean isBankOpen()
	{
		Widget bank = client.getWidget(InterfaceID.BANK, 1);
		return bank != null && !bank.isHidden();
	}

	private static boolean isManualRepositionOption(String option)
	{
		return option.equals("Swap") || option.equals("Insert");
	}

	private static boolean isTabSwitchOption(String option, String target)
	{
		String lowerOption = option.toLowerCase();
		String lowerTarget = target.toLowerCase();
		return (lowerOption.contains("tab") || lowerTarget.contains("tab")) && lowerTarget.contains("bank");
	}

	private static String clean(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim();
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(36, 32, 27));
		graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
		graphics.setColor(new Color(255, 152, 0));
		graphics.drawRoundRect(1, 1, 13, 13, 4, 4);
		graphics.fillOval(4, 4, 3, 3);
		graphics.fillOval(9, 4, 3, 3);
		graphics.fillRect(4, 10, 8, 2);
		graphics.dispose();
		return image;
	}
}