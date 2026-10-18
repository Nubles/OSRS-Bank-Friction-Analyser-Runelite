package com.bankfriction.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** The plugin's mark: a gilt bank chest with a teal line sweeping cleanly over its lid. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float stroke = Math.max(0.7f, 0.6f * s);

		// The chest's body.
		RoundRectangle2D body = new RoundRectangle2D.Float(x + 1.6f * s, y + 7.4f * s, 12.8f * s, 7.4f * s, 2f * s, 2f * s);
		g.setPaint(new GradientPaint(x, y + 7.4f * s, Theme.GOLD, x, y + 14.8f * s, Theme.GOLD_DIM));
		g.fill(body);

		// Its arched lid.
		Path2D lid = new Path2D.Float();
		lid.moveTo(x + 1.6f * s, y + 7.4f * s);
		lid.lineTo(x + 1.6f * s, y + 6.2f * s);
		lid.curveTo(x + 1.6f * s, y + 3.2f * s, x + 14.4f * s, y + 3.2f * s, x + 14.4f * s, y + 6.2f * s);
		lid.lineTo(x + 14.4f * s, y + 7.4f * s);
		lid.closePath();
		g.setPaint(new GradientPaint(x, y + 3.6f * s, Theme.GOLD_BRIGHT, x, y + 7.4f * s, Theme.GOLD));
		g.fill(lid);

		g.setStroke(new BasicStroke(stroke));
		g.setColor(Theme.INK_DEEP);
		g.draw(body);
		g.draw(lid);

		// Iron bands and the lock plate.
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.fill(new RoundRectangle2D.Float(x + 4.2f * s, y + 4.4f * s, 1.3f * s, 10.4f * s, s, s));
		g.fill(new RoundRectangle2D.Float(x + 10.5f * s, y + 4.4f * s, 1.3f * s, 10.4f * s, s, s));
		RoundRectangle2D plate = new RoundRectangle2D.Float(x + 6.4f * s, y + 6.6f * s, 3.2f * s, 3.6f * s, 1.2f * s, 1.2f * s);
		g.setColor(Theme.INK_DEEP);
		g.fill(plate);
		g.setColor(Theme.TEAL);
		g.fill(new Ellipse2D.Float(x + 7.4f * s, y + 7.4f * s, 1.2f * s, 1.2f * s));
		g.fillRect(Math.round(x + 7.75f * s), Math.round(y + 8.2f * s), Math.max(1, Math.round(0.5f * s)), Math.max(1, Math.round(1.2f * s)));

		// The sweep: a smooth line lifting off the lid to a bright point.
		Path2D sweep = new Path2D.Float();
		sweep.moveTo(x + 0.6f * s, y + 5.2f * s);
		sweep.curveTo(x + 4.5f * s, y + 0.6f * s, x + 10f * s, y + 3.4f * s, x + 14.4f * s, y + 0.9f * s);
		g.setStroke(new BasicStroke(Math.max(2.2f, 2.4f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.INK_DEEP);
		g.draw(sweep);
		g.setStroke(new BasicStroke(Math.max(1.1f, 1.3f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.TEAL);
		g.draw(sweep);
		float r = 1.6f * s;
		g.setColor(Theme.alpha(Theme.TEAL, 90));
		g.fill(new Ellipse2D.Float(x + 14.4f * s - r * 1.7f, y + 0.9f * s - r * 1.7f, r * 3.4f, r * 3.4f));
		g.setColor(Theme.mix(Theme.TEAL, Theme.TEXT, 0.5));
		g.fill(new Ellipse2D.Float(x + 14.4f * s - r, y + 0.9f * s - r, r * 2, r * 2));
	}
}
