package com.bankfriction.overlay;

import com.bankfriction.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Rings the bank items of whatever is chosen in the panel in soft gold, so they are easy to find
 * and gather. It only draws; nothing is clicked or moved.
 */
public class BankHighlight extends WidgetItemOverlay
{
	private volatile Set<Integer> items = Collections.emptySet();
	private volatile boolean enabled = true;

	public BankHighlight()
	{
		showOnBank();
	}

	/** The items to ring, already canonical; empty to ring nothing. */
	public void setItems(Collection<Integer> ids)
	{
		items = ids == null || ids.isEmpty() ? Collections.emptySet() : new HashSet<>(ids);
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem widgetItem)
	{
		Set<Integer> lit = items;
		if (!enabled || lit.isEmpty() || !lit.contains(itemId))
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		Theme.smooth(g2);
		RoundRectangle2D outer = new RoundRectangle2D.Float(bounds.x - 2, bounds.y - 2, bounds.width + 3, bounds.height + 3, 8, 8);
		RoundRectangle2D inner = new RoundRectangle2D.Float(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1, 7, 7);
		g2.setColor(Theme.alpha(Theme.GOLD, 46));
		g2.fill(inner);
		g2.setStroke(new BasicStroke(3f));
		g2.setColor(Theme.alpha(Theme.GOLD_BRIGHT, 70));
		g2.draw(outer);
		g2.setStroke(new BasicStroke(1.2f));
		g2.setColor(Theme.GOLD_BRIGHT);
		g2.draw(inner);
		g2.dispose();
	}
}
