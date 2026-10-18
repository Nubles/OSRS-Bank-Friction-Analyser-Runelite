package com.bankfriction.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * The crest at the top of the panel: the medallion with its friction ring, the score in large
 * figures with a word for it, and the trips, usual gear-up and trend underneath.
 */
public class Crest extends JComponent
{
	private static final int HEIGHT = 104;

	private final Animator ring = new Animator(this);
	private String player;
	private String status = "Log in to read your bank";
	private int score = -1;
	private int previous = -1;
	private int trips;
	private int usualMillis;

	public Crest()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
	}

	/**
	 * @param status a line in place of the figures underneath, or null to show them
	 * @param score friction from 0 to 100, or -1 before there is one
	 */
	public void update(String player, String status, int score, int previous, int trips, int usualMillis)
	{
		this.player = player;
		this.status = status;
		this.previous = previous;
		this.trips = trips;
		this.usualMillis = usualMillis;
		boolean animate = this.score >= 0 && score >= 0;
		this.score = score;
		ring.set(Math.max(0, score), animate);
		repaint();
	}

	/** A word for a score, so the number means something at a glance. */
	public static String grade(int score)
	{
		if (score < 0)
		{
			return "Gathering";
		}
		if (score < 25)
		{
			return "Smooth";
		}
		if (score < 45)
		{
			return "Fair";
		}
		if (score < 65)
		{
			return "Clunky";
		}
		return "Tangled";
	}

	/**
	 * Draws text at twice the size of the game's bold font by doubling every pixel, the way the
	 * game itself draws large numbers, so the figures stay crisp instead of being re-rasterised.
	 *
	 * @return the width drawn
	 */
	private static int paintLarge(Graphics2D g, String text, int x, int y, Color top, Color bottom)
	{
		Font font = Theme.title();
		FontMetrics fm = g.getFontMetrics(font);
		int w = Math.max(1, fm.stringWidth(text) + 1);
		int h = fm.getHeight() + 1;
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = small.createGraphics();
		sg.setFont(font);
		sg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		int base = fm.getAscent();
		sg.setColor(Color.BLACK);
		sg.drawString(text, 1, base + 1);
		sg.setPaint(new GradientPaint(0, base - fm.getAscent() + 3, top, 0, base, bottom));
		sg.drawString(text, 0, base);
		sg.dispose();
		Graphics2D lg = (Graphics2D) g.create();
		lg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		lg.drawImage(small, x, y - 2 * fm.getAscent() + 22, w * 2, h * 2, null);
		lg.dispose();
		return w * 2;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.22), 0, h, Theme.INK));
		g.fillRect(0, 0, w, h);

		Color tone = Theme.friction(score);

		// The medallion: a gilt ring with the mark inside, and the friction ring around it.
		int mx = 10;
		int my = 14;
		int md = 54;
		g.setPaint(new RadialGradientPaint(mx + md / 2f, my + md / 3f, md * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(mx, my, md, md));
		g.setStroke(new BasicStroke(3f));
		g.setColor(Theme.INK_DEEP);
		g.draw(new Ellipse2D.Float(mx + 2, my + 2, md - 4, md - 4));
		double fill = ring.get() / 100.0;
		if (score >= 0 && fill > 0)
		{
			g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(Theme.alpha(tone, 70));
			g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Arc2D.Double(mx + 2, my + 2, md - 4, md - 4, 90, -360 * fill, Arc2D.OPEN));
			g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(tone);
			g.draw(new Arc2D.Double(mx + 2, my + 2, md - 4, md - 4, 90, -360 * fill, Arc2D.OPEN));
		}
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(mx, my, Theme.GOLD_BRIGHT, mx, my + md, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(mx + 5, my + 5, md - 10, md - 10));
		Sigil.paint(g, mx + 12, my + 11, md - 24);

		int tx = mx + md + 12;
		int room = w - tx - 8;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String heading = (player != null ? player + "  -  " : "") + "BANK FRICTION";
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(heading, small, room), tx, 22);

		int top = 27;
		if (score >= 0)
		{
			int figureW = paintLarge(g, String.valueOf(score), tx, top, Theme.mix(tone, Color.WHITE, 0.35), tone);
			g.setFont(Theme.body());
			Ui.shadowed(g, grade(score), tx + figureW + 5, top + 18, Theme.mix(tone, Theme.TEXT, 0.25));
		}
		else
		{
			paintLarge(g, "-", tx, top, Theme.GOLD_BRIGHT, Theme.GOLD);
		}

		int cy = top + 50;
		g.setFont(Theme.small());
		if (status != null)
		{
			g.setColor(Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3));
			g.drawString(Ui.clip(status, small, room), tx, cy);
		}
		else
		{
			String[] labels = {"Trips", "Gear-up", "Trend"};
			String trend = "-";
			Color trendColor = Theme.TEXT_FAINT;
			if (previous >= 0 && score >= 0)
			{
				int d = score - previous;
				trend = d == 0 ? "steady" : (d > 0 ? "+" : "") + d;
				trendColor = d < 0 ? Theme.GREEN : d > 0 ? Theme.LOSS : Theme.TEXT_MUTED;
			}
			String[] values = {String.valueOf(trips), usualMillis > 0 ? Format.seconds(usualMillis) : "-", trend};
			Color[] colors = {Theme.TEXT, Theme.TEXT, trendColor};
			int cw = room / 3;
			for (int i = 0; i < 3; i++)
			{
				int x = tx + i * cw;
				g.setColor(Theme.TEXT_FAINT);
				g.drawString(labels[i], x, cy - 11);
				Ui.shadowed(g, Ui.clip(values[i], small, cw - 4), x, cy + 1, colors[i]);
			}
		}

		// The gilt rule under the crest, with a diamond at its middle.
		int y = h - 2;
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.GOLD, 0), w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(0, y, w, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(w / 2f, y - 3);
		diamond.lineTo(w / 2f + 3, y);
		diamond.lineTo(w / 2f, y + 3);
		diamond.lineTo(w / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
		g.dispose();
	}
}
