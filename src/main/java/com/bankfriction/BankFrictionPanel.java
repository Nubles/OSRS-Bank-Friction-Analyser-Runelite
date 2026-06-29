package com.bankfriction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class BankFrictionPanel extends PluginPanel
{
	private final JPanel recommendationContainer = new JPanel();
	private final Consumer<BankFrictionRecommendation> selectionConsumer;
	private final Runnable clearHistoryAction;
	private List<BankFrictionRecommendation> recommendations = Collections.emptyList();
	private BankFrictionRecommendation selectedRecommendation;
	private String highlightStatus = "";

	public BankFrictionPanel()
	{
		this(ignored -> { }, () -> { });
	}

	public BankFrictionPanel(
		Consumer<BankFrictionRecommendation> selectionConsumer,
		Runnable clearHistoryAction)
	{
		super();
		if (selectionConsumer == null)
		{
			this.selectionConsumer = ignored -> { };
		}
		else
		{
			this.selectionConsumer = selectionConsumer;
		}

		if (clearHistoryAction == null)
		{
			this.clearHistoryAction = () -> { };
		}
		else
		{
			this.clearHistoryAction = clearHistoryAction;
		}

		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		recommendationContainer.setLayout(new BoxLayout(recommendationContainer, BoxLayout.Y_AXIS));
		recommendationContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(header());
		add(recommendationContainer);
	}

	public void updateRecommendations(List<BankFrictionRecommendation> recommendations)
	{
		updateRecommendations(recommendations, selectedRecommendation, "");
	}

	public void updateRecommendations(
		List<BankFrictionRecommendation> recommendations,
		BankFrictionRecommendation selectedRecommendation)
	{
		updateRecommendations(recommendations, selectedRecommendation, "");
	}

	public void updateRecommendations(
		List<BankFrictionRecommendation> recommendations,
		BankFrictionRecommendation selectedRecommendation,
		String highlightStatus)
	{
		this.recommendations = recommendations == null
			? Collections.emptyList()
			: new ArrayList<>(recommendations);
		this.selectedRecommendation = selectedRecommendation;
		this.highlightStatus = highlightStatus == null ? "" : highlightStatus;
		renderRecommendations();
	}

	private void renderRecommendations()
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
				recommendationContainer.add(recommendationCard(recommendation));
			}
		}

		revalidate();
		repaint();
	}

	private JPanel header()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 8));
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

		JButton clearButton = new JButton("Clear local history");
		clearButton.setFocusable(false);
		clearButton.addActionListener(event -> clearHistoryAction.run());
		panel.add(clearButton, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel recommendationCard(BankFrictionRecommendation recommendation)
	{
		boolean selected = recommendation == selectedRecommendation;
		JPanel panel = new JPanel(new BorderLayout(0, 6));
		panel.setBackground(selected ? new Color(75, 55, 31) : ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)));

		JLabel titleLabel = new JLabel(recommendation.getTitle());
		titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		panel.add(titleLabel, BorderLayout.NORTH);

		JPanel bodyPanel = new JPanel();
		bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
		bodyPanel.setOpaque(false);
		bodyPanel.add(wrappedText(recommendation.getDescription()));
		bodyPanel.add(detailLabel(details(recommendation)));
		if (selected && !highlightStatus.isEmpty())
		{
			bodyPanel.add(detailLabel(highlightStatus));
		}
		panel.add(bodyPanel, BorderLayout.CENTER);

		if (!recommendation.getItemIds().isEmpty())
		{
			JButton highlightButton = new JButton(selected ? "Highlighted" : "Highlight items");
			highlightButton.setFocusable(false);
			highlightButton.addActionListener(event -> {
				selectedRecommendation = recommendation;
				selectionConsumer.accept(recommendation);
				renderRecommendations();
			});
			panel.add(highlightButton, BorderLayout.SOUTH);
		}

		return panel;
	}

	private static JLabel detailLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(label.getFont().deriveFont(11f));
		return label;
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
		panel.add(wrappedText(body), BorderLayout.CENTER);
		return panel;
	}

	private static JTextArea wrappedText(String body)
	{
		JTextArea bodyText = new JTextArea(body);
		bodyText.setLineWrap(true);
		bodyText.setWrapStyleWord(true);
		bodyText.setEditable(false);
		bodyText.setFocusable(false);
		bodyText.setOpaque(false);
		bodyText.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return bodyText;
	}

	private static String details(BankFrictionRecommendation recommendation)
	{
		String details = "Confidence " + recommendation.getConfidencePercent()
			+ "% | Evidence " + recommendation.getEvidenceCount()
			+ " | Items " + recommendation.getItemIds().size();
		if (recommendation.getSeconds() > 0)
		{
			details += " | Time " + recommendation.getSeconds() + "s";
		}
		return details;
	}
}
