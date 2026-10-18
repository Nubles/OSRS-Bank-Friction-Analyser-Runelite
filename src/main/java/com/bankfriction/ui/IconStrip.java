package com.bankfriction.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * Items in dark wells like bank slots, wrapped into rows. A strip with more items than it shows
 * ends in a well with the count of the rest.
 */
class IconStrip extends JComponent
{
	private static final int GAP = 4;

	private final List<Integer> items;
	private final ItemIcons icons;
	private final int well;
	private final int rows;
	private final int perRow;
	private final int shown;
	private final int more;

	/**
	 * @param well the size of each well
	 * @param width the width the strip may use
	 * @param maxRows rows to show before the rest are counted
	 * @param names gives each item's name for its tooltip, or null
	 */
	IconStrip(List<Integer> items, ItemIcons icons, int well, int width, int maxRows, IntFunction<String> names)
	{
		this.items = new ArrayList<>(items);
		this.icons = icons;
		this.well = well;
		this.perRow = Math.max(1, (width + GAP) / (well + GAP));
		int capacity = perRow * maxRows;
		if (items.size() > capacity)
		{
			shown = capacity - 1;
			more = items.size() - shown;
		}
		else
		{
			shown = items.size();
			more = 0;
		}
		int cells = shown + (more > 0 ? 1 : 0);
		this.rows = Math.max(1, (cells + perRow - 1) / perRow);
		setOpaque(false);
		Dimension d = new Dimension(width, rows * (well + GAP) - GAP);
		setPreferredSize(d);
		setMinimumSize(d);
		if (names != null)
		{
			StringBuilder tip = new StringBuilder("<html>");
			for (int i = 0; i < items.size() && i < 12; i++)
			{
				tip.append(i == 0 ? "" : "<br>").append(Ui.escape(names.apply(items.get(i))));
			}
			if (items.size() > 12)
			{
				tip.append("<br>and ").append(items.size() - 12).append(" more");
			}
			setToolTipText(tip.append("</html>").toString());
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int cells = shown + (more > 0 ? 1 : 0);
		for (int i = 0; i < cells; i++)
		{
			int x = (i % perRow) * (well + GAP);
			int y = (i / perRow) * (well + GAP);
			RoundRectangle2D shape = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, well - 1, well - 1, 7, 7);
			g.setPaint(new GradientPaint(0, y, Theme.INK_DEEP, 0, y + well, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
			g.fill(shape);
			g.setColor(Theme.STROKE_SOFT);
			g.draw(shape);
			if (i < shown)
			{
				BufferedImage icon = icons == null ? null : icons.icon(items.get(i), this::repaint);
				if (icon != null)
				{
					// Item pictures are 36 by 32; smaller wells scale them down to fit.
					double scale = Math.min(1, (well - 2) / 34.0);
					int iw = (int) Math.round(icon.getWidth() * scale);
					int ih = (int) Math.round(icon.getHeight() * scale);
					g.drawImage(icon, x + (well - iw) / 2, y + (well - ih) / 2, iw, ih, null);
				}
			}
			else
			{
				g.setFont(Theme.small());
				FontMetrics fm = g.getFontMetrics();
				String text = "+" + more;
				Ui.shadowed(g, text, x + (well - fm.stringWidth(text)) / 2, y + (well + fm.getAscent()) / 2 - 1, Theme.GOLD);
			}
		}
		g.dispose();
	}
}
