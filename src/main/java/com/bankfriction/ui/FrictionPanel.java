package com.bankfriction.ui;

import com.bankfriction.core.Advice;
import com.bankfriction.core.Loadout;
import com.bankfriction.core.Report;
import com.bankfriction.core.Visit;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel: the crest with the friction score, then three pages. Advice is what to change
 * in the bank, most valuable first; Loadouts are the sets of items you take out together; Visits
 * is the log of recent trips to the bank. Clicking any of them lights its items up in the bank.
 */
public class FrictionPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"advice", "loadouts", "visits"};

	/** What the panel has to show. */
	public enum State
	{
		LOGGED_OUT,
		PAUSED,
		READY
	}

	private final ItemIcons icons;
	private final BiConsumer<String, List<Integer>> onLight;
	private final Runnable onClear;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Advice", "Loadouts", "Visits");
	private final JPanel litBar = new JPanel(new BorderLayout());
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane advice = scroller();
	private final JScrollPane loadouts = scroller();
	private final JScrollPane visits = scroller();

	private Report report = Report.EMPTY;
	private State state = State.LOGGED_OUT;
	private boolean bankOpen;
	private long now;
	private String litKey;
	private String litTitle;

	/**
	 * @param onLight told the title and items to light up in the bank, or null and nothing to clear them
	 * @param onClear asked to forget every remembered visit
	 */
	public FrictionPanel(ItemIcons icons, BiConsumer<String, List<Integer>> onLight, Runnable onClear)
	{
		super(false);
		this.icons = icons;
		this.onLight = onLight;
		this.onClear = onClear;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel under = new JPanel(new BorderLayout(0, 6));
		under.setOpaque(false);
		under.setBorder(new EmptyBorder(8, 8, 2, 8));
		under.add(tabs, BorderLayout.NORTH);
		litBar.setOpaque(false);
		under.add(litBar, BorderLayout.SOUTH);
		north.add(under, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(advice, TABS[0]);
		content.add(loadouts, TABS[1]);
		content.add(visits, TABS[2]);
		add(content, BorderLayout.CENTER);

		tabs.onChange(this::showTab);
		rebuild();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever the report changes. */
	public void update(Report report, String player, State state, boolean bankOpen, long now)
	{
		this.report = report == null ? Report.EMPTY : report;
		this.state = state;
		this.bankOpen = bankOpen;
		this.now = now;
		String status = null;
		if (state == State.LOGGED_OUT)
		{
			status = this.report.getVisits() > 0 ? "Logged out, as last seen" : "Log in to read your bank";
		}
		else if (state == State.PAUSED)
		{
			status = "Paused in the plugin's settings";
		}
		else if (this.report.getFriction() < 0)
		{
			status = "Bank a few times to begin";
		}
		crest.update(player, status, this.report.getFriction(), this.report.getPreviousFriction(),
			this.report.getTrips(), this.report.getUsualMillis());
		rebuild();
	}

	/** Opens one tab, for previews and tests. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
	}

	/** Lights something up as if it were clicked, for previews and tests. */
	public void light(String key, String title, List<Integer> items)
	{
		litKey = key;
		litTitle = title;
		onLight.accept(title, items);
		rebuild();
	}

	private void toggle(String key, String title, List<Integer> items)
	{
		if (key.equals(litKey))
		{
			clearLight();
		}
		else
		{
			light(key, title, items);
		}
	}

	private void clearLight()
	{
		litKey = null;
		litTitle = null;
		onLight.accept(null, Collections.emptyList());
		rebuild();
	}

	private void rebuild()
	{
		rebuildLitBar();
		rebuildAdvice();
		rebuildLoadouts();
		rebuildVisits();
	}

	private void rebuildLitBar()
	{
		litBar.removeAll();
		if (litKey != null)
		{
			Card bar = new Card(new BorderLayout(6, 0));
			bar.accent(Theme.GOLD);
			bar.highlighted(true);
			bar.setBorder(new EmptyBorder(5, 10, 5, 6));
			JPanel text = new JPanel(new GridLayout(2, 1));
			text.setOpaque(false);
			text.add(Ui.label(bankOpen ? "LIT IN YOUR BANK" : "OPEN YOUR BANK TO SEE IT", Theme.small(), Theme.TEXT_MUTED));
			JComponent title = Ui.label(Ui.clip(litTitle, getFontMetrics(Theme.body()), WIDTH - 70), Theme.body(), Theme.GOLD_BRIGHT);
			text.add(title);
			bar.add(text, BorderLayout.CENTER);
			JPanel button = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
			button.setOpaque(false);
			button.add(new CodexButton("Clear", false, this::clearLight));
			bar.add(button, BorderLayout.EAST);
			litBar.add(bar, BorderLayout.CENTER);
		}
		litBar.revalidate();
		litBar.repaint();
	}

	private boolean waiting(StackPanel stack)
	{
		if (report.getVisits() == 0)
		{
			if (state == State.LOGGED_OUT)
			{
				stack.add(note("The bank is closed", "Log in and bank as you normally do. Bank Friction Analyser watches each trip to the bank"
					+ " and shows where your layout slows you down: tab hopping, searching, scrolling and misclicks."));
			}
			else
			{
				stack.add(note("Watching your bank", "Bank as you normally do. After a few trips this page shows what to move,"
					+ " gather or tag, with the time each change saves. Nothing is clicked or moved for you."));
			}
			return true;
		}
		return false;
	}

	private void rebuildAdvice()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(advice, stack);
			return;
		}
		List<Advice> list = report.getAdvice();
		if (list.isEmpty())
		{
			stack.add(note(report.getTrips() < 6 ? "Still learning your habits" : "Nothing to fix",
				report.getTrips() < 6
					? "Advice appears once a habit repeats a few times. " + Format.plural(report.getTrips(), "gear-up") + " seen so far."
					: "Your bank is laid out well for how you use it. New advice appears here if that changes."));
			replace(advice, stack);
			return;
		}
		double total = 0;
		for (Advice a : list)
		{
			total += a.impact();
		}
		Card head = new Card(new BorderLayout(0, 4));
		head.highlighted(true);
		head.add(Ui.label(Format.plural(list.size(), "change") + " worth making", Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
		head.add(Ui.wrapped("Together they would have saved about " + Format.duration(total) + " over your last "
			+ Format.plural(report.getTrips(), "trip") + ". Click one to light its items up in your bank.",
			Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		stack.add(head);
		stack.add(new SectionHeader("Most time saved first"));
		for (Advice a : list)
		{
			stack.add(adviceCard(a));
		}
		replace(advice, stack);
	}

	static Color accent(Advice.Kind kind)
	{
		switch (kind)
		{
			case GATHER:
				return Theme.TEAL;
			case RAISE:
				return Theme.GOLD;
			case SEARCH:
				return Theme.VIOLET;
			case PUT_BACK:
				return Theme.EMBER;
			case DECANT:
				return Theme.GREEN;
			default:
				return new Color(150, 170, 230);
		}
	}

	static String label(Advice.Kind kind)
	{
		switch (kind)
		{
			case GATHER:
				return "GATHER";
			case RAISE:
				return "MOVE UP";
			case SEARCH:
				return "TAG IT";
			case PUT_BACK:
				return "MISCLICKS";
			case DECANT:
				return "DECANT";
			default:
				return "PACE";
		}
	}

	private Card adviceCard(Advice a)
	{
		Color accent = accent(a.getKind());
		boolean lit = a.key().equals(litKey);
		Card card = new Card(new BorderLayout(0, 5));
		card.accent(accent);
		card.highlighted(lit);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(Ui.label(label(a.getKind()), Theme.small(), accent), BorderLayout.WEST);
		top.add(Ui.label("~" + Format.duration(a.getSecondsPerTrip()) + " a trip", Theme.small(), Theme.TEXT_MUTED), BorderLayout.EAST);

		JPanel middle = new JPanel(new BorderLayout(0, 4));
		middle.setOpaque(false);
		middle.add(Ui.wrapped(a.getTitle(), Theme.body(), lit ? Theme.GOLD_BRIGHT : Theme.TEXT, WIDTH - 30), BorderLayout.NORTH);
		middle.add(Ui.wrapped(a.getBody(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		if (!a.getItems().isEmpty())
		{
			middle.add(new IconStrip(a.getItems(), icons, 32, WIDTH - 22, 2, report::name), BorderLayout.SOUTH);
		}

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(Ui.label(a.getEvidence(), Theme.small(), Theme.TEXT_FAINT), BorderLayout.WEST);
		if (!a.getItems().isEmpty())
		{
			bottom.add(Ui.label(lit ? "lit" : "light up", Theme.small(), lit ? Theme.GOLD_BRIGHT : Theme.GOLD_DIM), BorderLayout.EAST);
		}

		card.add(top, BorderLayout.NORTH);
		card.add(middle, BorderLayout.CENTER);
		card.add(bottom, BorderLayout.SOUTH);
		if (!a.getItems().isEmpty())
		{
			card.onClick(() -> toggle(a.key(), a.getTitle(), a.getItems()));
		}
		return card;
	}

	private void rebuildLoadouts()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(loadouts, stack);
			return;
		}
		List<Loadout> list = report.getLoadouts();
		if (list.isEmpty())
		{
			stack.add(note("No loadouts yet", "When you take out the same set of three or more items on a few trips, it shows up here"
				+ " with how long it takes you to gear up for it."));
			replace(loadouts, stack);
			return;
		}
		stack.add(new SectionHeader("Your loadouts", Format.plural(list.size(), "set")));
		for (Loadout loadout : list)
		{
			stack.add(loadoutCard(loadout));
		}
		replace(loadouts, stack);
	}

	private Card loadoutCard(Loadout loadout)
	{
		boolean lit = loadout.key().equals(litKey);
		Color tone = Theme.friction(loadout.getFriction());
		Card card = new Card(new BorderLayout(0, 6));
		card.accent(tone);
		card.highlighted(lit);

		JPanel top = new JPanel(new BorderLayout(0, 1));
		top.setOpaque(false);
		top.add(Ui.wrapped(loadout.getName(), Theme.body(), lit ? Theme.GOLD_BRIGHT : Theme.TEXT, WIDTH - 30), BorderLayout.NORTH);
		top.add(Ui.label(Format.plural(loadout.getTrips(), "trip") + "  -  " + Format.ago(loadout.getLastSeen(), now)
			+ "  -  friction " + loadout.getFriction(), Theme.small(), Theme.TEXT_FAINT), BorderLayout.CENTER);
		card.add(top, BorderLayout.NORTH);
		card.add(new IconStrip(loadout.getItems(), icons, 32, WIDTH - 22, 3, report::name), BorderLayout.CENTER);

		JPanel stats = new JPanel(new GridLayout(1, 3, 4, 0));
		stats.setOpaque(false);
		stats.add(new StatTile(Format.seconds(loadout.getUsualMillis()), "Usual", "gear-up", Theme.TEXT));
		stats.add(new StatTile(Format.seconds(loadout.getBestMillis()), "Best", "so far", Theme.GREEN));
		String tabsValue = loadout.getTabs() > 0 ? String.valueOf(loadout.getTabs()) : "-";
		stats.add(new StatTile(tabsValue, "Tabs", oneDecimal(loadout.getTabSwitches()) + " hops", loadout.getTabs() >= 2 ? Theme.EMBER : Theme.TEXT));
		card.add(stats, BorderLayout.SOUTH);
		card.onClick(() -> toggle(loadout.key(), loadout.getName(), loadout.getItems()));
		return card;
	}

	private void rebuildVisits()
	{
		StackPanel stack = new StackPanel(4, 8);
		if (waiting(stack))
		{
			replace(visits, stack);
			return;
		}
		List<Visit> list = report.getRecent();
		JPanel totals = new JPanel(new GridLayout(1, 2, 6, 0));
		totals.setOpaque(false);
		totals.add(new StatTile(String.valueOf(report.getVisits()), "Visits", Format.plural(report.getTrips(), "gear-up"), Theme.GOLD));
		totals.add(new StatTile(Format.duration(report.getLostSeconds()), "Hunting", "time lost, about", Theme.friction(report.getFriction())));
		stack.add(totals);
		stack.space(2);
		stack.add(new SectionHeader("Latest trips", "friction"));
		for (Visit visit : list)
		{
			String key = "visit:" + visit.getStart();
			List<Integer> items = new ArrayList<>(visit.withdrawn());
			String title = "Trip " + Format.ago(visit.getStart(), now);
			stack.add(new VisitRow(visit, icons, now, key.equals(litKey), () -> toggle(key, title, items)));
		}
		stack.space(6);
		JPanel clear = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		clear.setOpaque(false);
		clear.add(new CodexButton("Forget every visit", false, this::confirmClear));
		stack.add(clear);
		replace(visits, stack);
	}

	private void confirmClear()
	{
		int answer = JOptionPane.showConfirmDialog(this,
			"Forget every remembered bank visit for this account?\nThe advice starts again from nothing.",
			"Bank Friction Analyser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer == JOptionPane.OK_OPTION)
		{
			clearLight();
			onClear.run();
		}
	}

	private static String oneDecimal(double value)
	{
		return String.format("%.1f", value);
	}

	static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(title, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
