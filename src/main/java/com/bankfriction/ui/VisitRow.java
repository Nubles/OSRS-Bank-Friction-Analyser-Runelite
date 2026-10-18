package com.bankfriction.ui;

import com.bankfriction.core.Visit;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * One bank visit in the log: when it was and how long the gear-up took, the first items taken,
 * what slowed it down, and its friction as a coloured pill.
 */
class VisitRow extends JComponent
{
	private static final int HEIGHT = 46;
	private static final int ICON = 20;

	private final Visit visit;
	private final List<Integer> items;
	private final ItemIcons icons;
	private final long now;
	private boolean hover;
	private boolean selected;

	VisitRow(Visit visit, ItemIcons icons, long now, boolean selected, Runnable onClick)
	{
		this.visit = visit;
		this.items = new ArrayList<>(visit.withdrawn());
		this.icons = icons;
		this.now = now;
		this.selected = selected;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(tip());
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hover = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = false;
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()))
				{
					onClick.run();
				}
			}
		});
	}

	private String tip()
	{
		return "<html>Friction " + visit.friction() + ", about " + Format.duration(visit.lostSeconds()) + " spent hunting"
			+ "<br>" + Format.plural(visit.withdrawals(), "item") + " taken, " + Format.plural(visit.tabSwitches(), "tab hop")
			+ ", " + visit.searches() + (visit.searches() == 1 ? " search" : " searches")
			+ "<br>" + Format.plural(visit.deepWithdrawals(), "scroll") + ", " + visit.putBackCount() + " put back"
			+ "<br>Click to light its items up in your bank</html>";
	}

	/** What slowed the visit down, shortest first. */
	private String notes()
	{
		List<String> parts = new ArrayList<>();
		if (visit.tabSwitches() > 0)
		{
			parts.add(Format.plural(visit.tabSwitches(), "hop"));
		}
		if (visit.searches() > 0)
		{
			parts.add(visit.searches() + (visit.searches() == 1 ? " search" : " searches"));
		}
		if (visit.deepWithdrawals() > 0)
		{
			parts.add(Format.plural(visit.deepWithdrawals(), "scroll"));
		}
		if (visit.putBackCount() > 0)
		{
			parts.add(visit.putBackCount() + " put back");
		}
		if (parts.isEmpty())
		{
			return visit.withdrawals() == 0 ? "only deposits" : "clean";
		}
		return String.join("  ", parts);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight() - 2;
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		Color top = hover ? Theme.CARD_HOVER : Theme.CARD_TOP;
		g.setPaint(new GradientPaint(0, 0, top, 0, h, Theme.CARD));
		g.fill(shape);
		g.setColor(selected ? Theme.alpha(Theme.GOLD, 150) : hover ? Theme.alpha(Theme.GOLD, 90) : Theme.STROKE_SOFT);
		g.draw(shape);

		int friction = visit.friction();
		Color tone = visit.withdrawals() == 0 ? Theme.TEXT_FAINT : Theme.friction(friction);

		// The friction pill on the right.
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		String score = visit.withdrawals() == 0 ? "-" : String.valueOf(friction);
		int pw = Math.max(26, body.stringWidth(score) + 12);
		int px = w - pw - 8;
		int py = 8;
		RoundRectangle2D pill = new RoundRectangle2D.Float(px, py, pw, 18, 18, 18);
		g.setColor(Theme.alpha(tone, 40));
		g.fill(pill);
		g.setColor(Theme.alpha(tone, 160));
		g.draw(pill);
		Ui.shadowed(g, score, px + (pw - body.stringWidth(score)) / 2, py + 14, Theme.mix(tone, Theme.TEXT, 0.2));

		// When, and how long the gear-up took.
		int x = 10;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String when = Format.ago(visit.getStart(), now);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(when, x, 15);
		g.setFont(Theme.body());
		String took = Format.seconds(visit.gearUpMillis());
		int tw = Math.max(small.stringWidth("00m ago"), body.stringWidth(took));
		Ui.shadowed(g, took, x, 32, Theme.TEXT);

		// The first items taken, small.
		int ix = x + tw + 10;
		int room = px - ix - 6;
		int fit = Math.max(0, (room + 2) / (ICON + 2));
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		for (int i = 0; i < items.size() && i < fit; i++)
		{
			BufferedImage icon = icons == null ? null : icons.icon(items.get(i), this::repaint);
			if (icon != null)
			{
				int iw = icon.getWidth() * ICON / 34;
				int ih = icon.getHeight() * ICON / 34;
				g.drawImage(icon, ix + i * (ICON + 2) + (ICON - iw) / 2, 5 + (ICON - ih) / 2, iw, ih, null);
			}
		}
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(notes(), small, w - ix - 8), ix, 37);
		g.dispose();
	}
}
