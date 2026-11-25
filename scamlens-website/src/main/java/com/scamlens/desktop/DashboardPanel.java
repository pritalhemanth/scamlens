package com.scamlens.desktop;

import com.scamlens.model.DashboardStats;
import com.scamlens.model.RiskLevel;
import com.scamlens.model.ScanRecord;
import com.scamlens.service.HistoryService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.List;

/**
 * Screen 3: totals and recent checks, always counted from the real history file.
 * refresh() is called every time the screen is opened.
 */
public class DashboardPanel extends JPanel {

    private final HistoryService history;
    private final Runnable onCheckMessage;
    private final Page page = new Page();

    public DashboardPanel(HistoryService history, Runnable onCheckMessage) {
        super(new BorderLayout());
        this.history = history;
        this.onCheckMessage = onCheckMessage;
        add(Theme.scroll(page), BorderLayout.CENTER);
    }

    /** Reloads the numbers from the history file. */
    public void refresh() {
        page.clear();
        page.row(new Chip("Dashboard", Theme.BLUE_LIGHT));
        page.gap(14);
        page.row(ScannerPanel.twoTone("Your scan ", "activity", 46));
        page.gap(10);

        try {
            DashboardStats stats = history.getStats();
            List<ScanRecord> recent = history.findRecent(5);
            if (stats.isEmpty()) {
                page.row(Theme.label("Your checks, counted from the saved history.", Theme.sans(16.5f), Theme.MUTED));
                page.gap(28);
                page.row(emptyState());
            } else {
                page.row(Theme.label("Based on " + stats.total() + " saved " + (stats.total() == 1 ? "check" : "checks") + ".",
                        Theme.sans(16.5f), Theme.MUTED));
                page.gap(28);
                page.row(totals(stats));
                page.gap(20);
                JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
                columns.setOpaque(false);
                columns.add(mixCard(stats));
                columns.add(recentCard(recent));
                page.row(columns);
            }
        } catch (IOException e) {
            page.row(Theme.label("Could not read the history file: " + e.getMessage(), Theme.sans(15), Theme.ERROR_TEXT));
        }
        page.refresh();
    }

    private JComponent emptyState() {
        RoundedPanel box = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 24);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Theme.label("No messages checked yet. Numbers appear here after your first check.",
                Theme.sans(15), Theme.MUTED));
        box.add(Theme.gap(16));
        PillButton go = new PillButton("Check a message", PillButton.Style.PRIMARY);
        go.addActionListener(e -> onCheckMessage.run());
        box.add(Theme.flow(FlowLayout.LEFT, 0, go));
        return box;
    }

    /** Four numbers: all checks, then each risk level in its colour. */
    private JComponent totals(DashboardStats stats) {
        RoundedPanel strip = new RoundedPanel(new GridLayout(1, 4), Theme.PANEL, Theme.LINE, 18, 0);
        strip.add(total(String.valueOf(stats.total()), "Checks", Theme.TEXT, false));
        strip.add(total(String.valueOf(stats.high()), "High risk", Theme.HIGH, true));
        strip.add(total(String.valueOf(stats.suspicious()), "Suspicious", Theme.SUSPICIOUS, true));
        strip.add(total(String.valueOf(stats.low()), "Low risk", Theme.LOW, true));
        return strip;
    }

    private static JComponent total(String number, String caption, Color colour, boolean lineOnLeft) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(new CompoundBorder(
                new MatteBorder(0, lineOnLeft ? 1 : 0, 0, 0, Theme.LINE), new EmptyBorder(22, 12, 22, 12)));
        JLabel big = Theme.label(number, Theme.light(40), colour);
        JLabel small = Theme.label(caption, Theme.mono(12), Theme.MUTED);
        big.setAlignmentX(Component.CENTER_ALIGNMENT);
        small.setAlignmentX(Component.CENTER_ALIGNMENT);
        cell.add(big);
        cell.add(Theme.gap(4));
        cell.add(small);
        return cell;
    }

    /** "Risk mix": one bar split by level, with a legend under it. */
    private static JComponent mixCard(DashboardStats stats) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.label("Risk mix", Theme.sansBold(16), Theme.TEXT));
        card.add(Theme.gap(16));
        card.add(new MixBar(stats));
        card.add(Theme.gap(16));

        int[] counts = {stats.high(), stats.suspicious(), stats.low()};
        RiskLevel[] levels = {RiskLevel.HIGH, RiskLevel.SUSPICIOUS, RiskLevel.LOW};
        for (int i = 0; i < levels.length; i++) {
            long percent = Math.round(100.0 * counts[i] / stats.total());
            JPanel row = Theme.spread(Theme.levelLabel(levels[i], 14.5f),
                    Theme.label(counts[i] + " · " + percent + "%", Theme.mono(12.5f), Theme.MUTED));
            row.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.LINE), new EmptyBorder(10, 0, 10, 0)));
            card.add(row);
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** "Recent checks": the five newest. */
    private static JComponent recentCard(List<ScanRecord> recent) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.label("Recent checks", Theme.sansBold(16), Theme.TEXT));
        card.add(Theme.gap(14));
        recent.stream().map(DashboardPanel::recentRow).forEach(row -> {
            card.add(row);
            card.add(Theme.gap(8));
        });
        card.add(Box.createVerticalGlue());
        return card;
    }

    private static JComponent recentRow(ScanRecord record) {
        RoundedPanel row = new RoundedPanel(new BorderLayout(16, 0), Theme.BG, Theme.LINE, 14, 0) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Theme.levelLabel(record.riskLevel(), 14));
        left.add(Theme.gap(2));
        String signs = record.categories().isEmpty() ? "No warning signs" : record.categoryLabels();
        left.add(Theme.label(signs, Theme.sans(13), Theme.MUTED));
        row.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel score = Theme.label(String.valueOf(record.score()), Theme.mono(15), Theme.TEXT);
        JLabel date = Theme.label(record.scannedAt().format(ResultPanel.DATE), Theme.mono(11.5f), Theme.MUTED);
        score.setAlignmentX(Component.RIGHT_ALIGNMENT);
        date.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(score);
        right.add(date);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /** One rounded bar: red, amber and green parts as wide as their share of all checks. */
    private static final class MixBar extends JComponent {
        private final DashboardStats stats;

        MixBar(DashboardStats stats) {
            this.stats = stats;
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(200, 14));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            int[] counts = {stats.high(), stats.suspicious(), stats.low()};
            Color[] colours = {Theme.HIGH, Theme.SUSPICIOUS, Theme.LOW};
            double x = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                double width = (double) getWidth() * counts[i] / stats.total();
                g2.setColor(colours[i]);
                g2.fillRect((int) Math.round(x), 0, (int) Math.round(width) - 2, getHeight());
                x += width;
            }
            g2.dispose();
        }
    }
}
