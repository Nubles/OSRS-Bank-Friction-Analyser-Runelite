package com.bankfriction;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class BankFrictionPanel extends PluginPanel
{
	private final JPanel recommendationContainer = new JPanel();

	public BankFrictionPanel()
	{
		super();
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		recommendationContainer.setLayout(new BoxLayout(recommendationContainer, BoxLayout.Y_AXIS));
		recommendationContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(header());
		add(recommendationContainer);
	}

	public void updateRecommendations(List<BankFrictionRecommendation> recommendations)
	{
		recommendationContainer.removeAll();

		if (recommendations.isEmpty())
		{
			recommendationContainer.add(textBlock(
				"No recommendations yet",
				"Use your bank normally. Recommendations appear after several repeated bank sessions."));
		}
		else
		{
			for (BankFrictionRecommendation recommendation : recommendations)
			{
				recommendationContainer.add(textBlock(recommendation.getTitle(), recommendation.getDescription()));
			}
		}

		revalidate();
		repaint();
	}

	private static JPanel header()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Bank Friction Analyser");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		panel.add(title, BorderLayout.NORTH);

		JTextArea body = new JTextArea(
			"Observes bank habits locally and suggests item groups, repeated searches, "
				+ "slow loadouts, and confusing duplicates.");
		body.setLineWrap(true);
		body.setWrapStyleWord(true);
		body.setEditable(false);
		body.setFocusable(false);
		body.setOpaque(false);
		body.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		panel.add(body, BorderLayout.CENTER);
		return panel;
	}

	private static JPanel textBlock(String title, String body)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		panel.add(titleLabel, BorderLayout.NORTH);

		JTextArea bodyText = new JTextArea(body);
		bodyText.setLineWrap(true);
		bodyText.setWrapStyleWord(true);
		bodyText.setEditable(false);
		bodyText.setFocusable(false);
		bodyText.setOpaque(false);
		bodyText.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		panel.add(bodyText, BorderLayout.CENTER);
		return panel;
	}
}