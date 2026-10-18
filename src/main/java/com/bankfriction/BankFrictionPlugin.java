package com.bankfriction;

import com.bankfriction.core.Analysis;
import com.bankfriction.core.Journal;
import com.bankfriction.core.Legacy;
import com.bankfriction.core.Recorder;
import com.bankfriction.core.Report;
import com.bankfriction.overlay.BankHighlight;
import com.bankfriction.ui.FrictionPanel;
import com.bankfriction.ui.ItemIcons;
import com.bankfriction.ui.Sigil;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Bank Friction Analyser",
	description = "Finds what slows your banking down (tab hopping, searching, scrolling, misclicks) and shows what to move, gather or tag",
	tags = {"bank", "analytics", "organizer", "organiser", "loadout", "gear", "layout", "tags"}
)
public class BankFrictionPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(BankFrictionPlugin.class);
	private static final String JOURNAL_KEY = "journal";
	/** Where the first version kept every account's history together. */
	private static final String LEGACY_KEY = "snapshot";
	/** Ticks between saves while something has changed, about half a minute. */
	private static final int SAVE_TICKS = 50;
	/** Bank items are laid out on a 36 pixel grid. */
	private static final int ROW_HEIGHT = 36;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Gson gson;

	@Inject
	private BankFrictionConfig config;

	private final BankHighlight highlight = new BankHighlight();

	private FrictionPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Recorder recorder;
	private boolean bankOpen;
	private boolean searching;
	private boolean unsaved;
	private int ticks;
	/** Ticks until the panel is refreshed after logging in, once the player's name is known. */
	private int refreshIn = -1;

	/** The bank item being dragged, and the last place it was over. */
	private int dragItem = -1;
	private int dragIndex = -1;
	private Widget dragOver;

	@Provides
	BankFrictionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankFrictionConfig.class);
	}

	@Override
	protected void startUp()
	{
		ItemIcons icons = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(onLoaded);
			return image;
		};
		panel = new FrictionPanel(icons, this::light, () -> clientThread.invokeLater(this::clearHistory));
		navButton = NavigationButton.builder()
			.tooltip("Bank Friction Analyser")
			.icon(Sigil.icon(16))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		highlight.setEnabled(config.showPassiveHighlights());
		overlayManager.add(highlight);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(highlight);
		highlight.setItems(null);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			closeVisit();
			save();
			recorder = null;
			profile = null;
			bankOpen = false;
			searching = false;
		});
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			closeVisit();
			save();
			publish();
		}
		else if (state == GameState.LOGGED_IN)
		{
			refreshIn = 2;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		closeVisit();
		save();
		loadProfile();
		publish();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BankFrictionConfig.GROUP.equals(event.getGroup()) || event.getKey().equals(JOURNAL_KEY))
		{
			return;
		}
		highlight.setEnabled(config.showPassiveHighlights());
		clientThread.invokeLater(() ->
		{
			if (recorder != null)
			{
				recorder.setKeep(config.maximumRetainedSessions());
				if (!config.collectBehaviorData())
				{
					closeVisit();
				}
			}
			publish();
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (recorder == null)
		{
			return;
		}
		ticks++;
		if (refreshIn >= 0 && refreshIn-- == 0)
		{
			publish();
		}
		long now = System.currentTimeMillis();
		boolean open = isBankOpen();
		if (open && !bankOpen)
		{
			bankOpen = true;
			if (config.collectBehaviorData())
			{
				recorder.open(now, client.getVarbitValue(VarbitID.BANK_CURRENTTAB));
			}
			publish();
		}
		else if (!open && bankOpen)
		{
			closeVisit();
			publish();
		}
		else if (open && recorder.tick(now))
		{
			unsaved = true;
			publish();
		}

		if (searching && !isSearching())
		{
			searching = false;
			recorder.endSearch(now);
		}
		if (unsaved && ticks % SAVE_TICKS == 0)
		{
			save();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (recorder != null && bankOpen && event.getVarbitId() == VarbitID.BANK_CURRENTTAB)
		{
			recorder.tab(event.getValue(), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event)
	{
		if (recorder == null || !bankOpen || event.getIndex() != VarClientID.MESLAYERINPUT || !isSearching())
		{
			return;
		}
		// The chat box input is shared with quantity prompts, so it only counts as a search while the bank's search is open.
		searching = true;
		recorder.searchText(client.getVarcStrValue(VarClientID.MESLAYERINPUT), System.currentTimeMillis());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (recorder == null || !recorder.isOpen())
		{
			return;
		}
		String option = event.getMenuOption();
		if (option == null)
		{
			return;
		}
		int widgetId = event.getParam1();
		long now = System.currentTimeMillis();
		if (widgetId == InterfaceID.Bankmain.ITEMS && option.startsWith("Withdraw"))
		{
			int itemId = event.getItemId();
			if (itemId <= 0)
			{
				return;
			}
			int row = -1;
			boolean deep = false;
			Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
			Widget slot = container == null ? null : container.getChild(event.getParam0());
			if (slot != null)
			{
				row = slot.getRelativeY() / ROW_HEIGHT;
				// Below the first screen of the bank, so it had to be scrolled to.
				deep = slot.getRelativeY() + slot.getHeight() > container.getHeight();
			}
			recorder.withdraw(itemId, name(itemId), now, row, deep);
		}
		else if (widgetId == InterfaceID.Bankside.ITEMS && option.startsWith("Deposit"))
		{
			int itemId = event.getItemId();
			if (itemId > 0)
			{
				int canonical = itemManager.canonicalize(itemId);
				recorder.deposit(canonical, name(canonical), now);
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (recorder == null || !recorder.isOpen())
		{
			return;
		}
		if (client.isDraggingWidget())
		{
			Widget dragged = client.getDraggedWidget();
			if (dragged != null && dragged.getId() == InterfaceID.Bankmain.ITEMS && dragged.getItemId() > 0)
			{
				dragItem = dragged.getItemId();
				dragIndex = dragged.getIndex();
				Widget over = client.getDraggedOnWidget();
				if (over != null)
				{
					dragOver = over;
				}
			}
			return;
		}
		if (dragItem > 0)
		{
			Widget over = dragOver;
			boolean moved = over != null
				&& (over.getId() == InterfaceID.Bankmain.ITEMS && over.getIndex() != dragIndex
				|| over.getId() == InterfaceID.Bankmain.TABS);
			if (moved)
			{
				recorder.rearrange(dragItem, name(dragItem), System.currentTimeMillis());
			}
			dragItem = -1;
			dragIndex = -1;
			dragOver = null;
		}
	}

	private boolean isBankOpen()
	{
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return items != null && !items.isHidden();
	}

	private boolean isSearching()
	{
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.SEARCH.getType();
	}

	private String name(int itemId)
	{
		ItemComposition composition = itemManager.getItemComposition(itemId);
		return composition == null ? null : composition.getName();
	}

	private void closeVisit()
	{
		bankOpen = false;
		searching = false;
		dragItem = -1;
		dragOver = null;
		if (recorder != null && recorder.close(System.currentTimeMillis()))
		{
			unsaved = true;
		}
	}

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		recorder = null;
		if (profile == null)
		{
			return;
		}
		Journal journal = null;
		try
		{
			String saved = configManager.getRSProfileConfiguration(BankFrictionConfig.GROUP, JOURNAL_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Journal read = gson.fromJson(saved, Journal.class);
				journal = read == null ? null : read.repair();
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved bank visits, starting afresh", e);
		}
		unsaved = false;
		if (journal == null)
		{
			journal = Legacy.read(configManager.getConfiguration(BankFrictionConfig.GROUP, LEGACY_KEY));
			if (journal != null)
			{
				// The first version kept one history for every account; it moves to the first account that logs in.
				unsaved = true;
			}
		}
		if (journal == null)
		{
			journal = new Journal();
		}
		recorder = new Recorder(journal, config.maximumRetainedSessions());
		journal.trim(config.maximumRetainedSessions());
		if (unsaved)
		{
			save();
			configManager.unsetConfiguration(BankFrictionConfig.GROUP, LEGACY_KEY);
		}
	}

	private void save()
	{
		if (profile == null || recorder == null || !unsaved)
		{
			return;
		}
		unsaved = false;
		configManager.setConfiguration(BankFrictionConfig.GROUP, profile, JOURNAL_KEY, gson.toJson(recorder.getJournal()));
	}

	private void clearHistory()
	{
		if (recorder == null)
		{
			return;
		}
		Journal journal = recorder.getJournal();
		journal.getVisits().clear();
		journal.getNames().clear();
		boolean open = recorder.isOpen();
		recorder = new Recorder(journal, config.maximumRetainedSessions());
		if (open && config.collectBehaviorData())
		{
			recorder.open(System.currentTimeMillis(), client.getVarbitValue(VarbitID.BANK_CURRENTTAB));
		}
		unsaved = true;
		save();
		publish();
	}

	/** Called from the panel on the Swing thread with what to ring in the bank. */
	private void light(String title, List<Integer> items)
	{
		List<Integer> copy = new ArrayList<>(items);
		clientThread.invokeLater(() ->
		{
			// Ring an item's placeholder too, so it can be found when every one is in the inventory.
			Set<Integer> ids = new HashSet<>();
			for (int id : copy)
			{
				ids.add(id);
				ItemComposition composition = itemManager.getItemComposition(id);
				if (composition != null && composition.getPlaceholderId() > 0)
				{
					ids.add(composition.getPlaceholderId());
				}
			}
			highlight.setItems(ids);
		});
	}

	/** Works the report out on the client thread and hands it to the panel. */
	private void publish()
	{
		FrictionPanel target = panel;
		if (target == null)
		{
			return;
		}
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		Report report = recorder == null ? Report.EMPTY : Analysis.of(recorder.getJournal());
		Player player = client.getLocalPlayer();
		String name = loggedIn && player != null ? player.getName() : null;
		FrictionPanel.State state = !loggedIn ? FrictionPanel.State.LOGGED_OUT
			: config.collectBehaviorData() ? FrictionPanel.State.READY : FrictionPanel.State.PAUSED;
		boolean open = bankOpen;
		long now = System.currentTimeMillis();
		SwingUtilities.invokeLater(() -> target.update(report, name, state, open, now));
	}
}
