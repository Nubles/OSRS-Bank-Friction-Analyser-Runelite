package com.bankfriction;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class BankFrictionOverlay extends WidgetItemOverlay
{
	private static final Color FILL_COLOR = new Color(255, 152, 0, 45);
	private static final Color BORDER_COLOR = new Color(255, 152, 0, 210);

	private final BankFrictionConfig config;
	private Set<Integer> highlightedItemIds = Collections.emptySet();

	BankFrictionOverlay(BankFrictionConfig config)
	{
		this.config = config;
		showOnBank();
	}

	void setHighlightedItemIds(Collection<Integer> itemIds)
	{
		if (itemIds == null || itemIds.isEmpty())
		{
			highlightedItemIds = Collections.emptySet();
			return;
		}

		highlightedItemIds = new HashSet<>(itemIds);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.showPassiveHighlights() || !highlightedItemIds.contains(itemId))
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}

		graphics.setStroke(new BasicStroke(2f));
		graphics.setColor(FILL_COLOR);
		graphics.fill(bounds);
		graphics.setColor(BORDER_COLOR);
		graphics.draw(bounds);
	}
}
