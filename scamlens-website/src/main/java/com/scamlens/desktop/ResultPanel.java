package com.scamlens.desktop;

import com.scamlens.model.AnalysisResult;
import com.scamlens.model.DetectionResult;
import com.scamlens.util.TextHighlighter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Screen 2: the result of one check. show(...) fills it from a real AnalysisResult;
 * nothing on this screen is made up.
 */
public class ResultPanel extends JPanel {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final Runnable onNewCheck;
    private final Page page = new Page();
    private final JScrollPane scroll;

    public ResultPanel(Runnable onNewCheck) {
        super(new BorderLayout());
        this.onNewCheck = onNewCheck;
        scroll = Theme.scroll(page);
        add(scroll, BorderLayout.CENTER);
    }

    /** Rebuilds the screen for a new result. */
    public void show(AnalysisResult result, String savedStatus) {
        page.clear();
        Color risk = Theme.riskColor(result.riskLevel());

        page.row(new Chip("Analysis complete · " + result.analysedAt().format(DATE), Theme.BLUE_LIGHT));
        page.gap(18);
        page.row(verdict(result, risk));
        page.gap(12);
        page.row(Theme.paragraph("The score is a warning level worked out from fixed rules, not proof that a "
                + "message is a scam. " + savedStatus, Theme.sans(13.5f), Theme.MUTED));
        page.gap(24);

        JPanel twoColumns = new JPanel(new GridLayout(1, 2, 16, 0));
        twoColumns.setOpaque(false);
        twoColumns.add(messageCard(result, risk));
        twoColumns.add(tipsCard(result));
        page.row(twoColumns);
        page.gap(36);

        page.row(Theme.label(result.hasIndicators() ? "Why it was flagged" : "Nothing suspicious found",
                Theme.light(26), Theme.TEXT));
        page.gap(16);
        page.row(reasons(result));
        page.gap(32);

        PillButton again = new PillButton("Check another message", PillButton.Style.PRIMARY);
        again.addActionListener(e -> onNewCheck.run());
        page.row(Theme.flow(FlowLayout.LEFT, 0, again));

        page.refresh();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    // ---------- parts of the screen ----------

    /** The big coloured box: level, summary, scale and score. */
    private JComponent verdict(AnalysisResult result, Color risk) {
        RoundedPanel box = new RoundedPanel(new BorderLayout(32, 0),
                Theme.blend(Theme.PANEL, risk, 0.10), Theme.blend(Theme.LINE, risk, 0.55), 20, 28);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(result.riskLevel().label(), Theme.light(50), risk));
        text.add(Theme.gap(10));
        text.add(Theme.paragraph(result.riskLevel().summary(), Theme.sans(16.5f), Theme.TEXT));
        text.add(Theme.gap(22));
        text.add(new ScaleBar(result.score()));
        box.add(text, BorderLayout.CENTER);

        JPanel score = new JPanel();
        score.setOpaque(false);
        score.setLayout(new BoxLayout(score, BoxLayout.Y_AXIS));
        score.setBorder(new CompoundBorder(new MatteBorder(0, 1, 0, 0, Theme.LINE), new EmptyBorder(0, 32, 0, 8)));
        JLabel number = Theme.label(String.valueOf(result.score()), Theme.light(84), risk);
        JLabel caption = Theme.label("Risk score / 100", Theme.mono(12), Theme.MUTED);
        number.setAlignmentX(Component.CENTER_ALIGNMENT);
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        score.add(Box.createVerticalGlue());
        score.add(number);
        score.add(caption);
        score.add(Box.createVerticalGlue());
        box.add(score, BorderLayout.EAST);
        return box;
    }

    /** The message as a chat bubble, with suspicious phrases highlighted. */
    private JComponent messageCard(AnalysisResult result, Color risk) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.label("The message", Theme.sansBold(16), Theme.TEXT));
        card.add(Theme.gap(12));

        RoundedPanel bubble = new RoundedPanel(new BorderLayout(), Theme.PANEL_2, Theme.LINE_STRONG, 22, 0) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(new HighlightedText(result, risk), BorderLayout.CENTER);
        card.add(bubble);
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** "What to do": the safety tips, without repeating the summary sentence. */
    private JComponent tipsCard(AnalysisResult result) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.label("What to do", Theme.sansBold(16), Theme.TEXT));
        card.add(Theme.gap(12));

        List<String> tips = result.recommendations().stream()
                .filter(tip -> !tip.equals(result.riskLevel().summary()))
                .toList();
        if (tips.isEmpty()) {
            tips = List.of("If a message later asks for money, a code or a password, "
                    + "check with the sender using a number you already trust.");
        }
        for (String tip : tips) {
            JPanel row = new JPanel(new BorderLayout(12, 0)) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                }
            };
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel bulletHolder = new JPanel(new BorderLayout());
            bulletHolder.setOpaque(false);
            bulletHolder.add(new Bullet(), BorderLayout.NORTH);
            row.add(bulletHolder, BorderLayout.WEST);
            JPanel textHolder = new JPanel();
            textHolder.setOpaque(false);
            textHolder.setLayout(new BoxLayout(textHolder, BoxLayout.Y_AXIS));
            textHolder.add(Theme.paragraph(tip, Theme.sans(14.5f), Theme.TEXT));
            row.add(textHolder, BorderLayout.CENTER);
            card.add(row);
            card.add(Theme.gap(12));
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** One card per sign found, two per row. */
    private JComponent reasons(AnalysisResult result) {
        if (!result.hasIndicators()) {
            RoundedPanel none = new RoundedPanel(new BorderLayout(), Theme.PANEL, Theme.LINE, 18, 20);
            none.add(Theme.label("None of the six warning signs appear in this message.", Theme.sans(15), Theme.MUTED));
            return none;
        }
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);
        result.detections().stream().map(ResultPanel::reasonCard).forEach(grid::add);
        return grid;
    }

    private static JComponent reasonCard(DetectionResult detection) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.spread(Theme.label(detection.category().label(), Theme.sansBold(16), Theme.TEXT),
                new Chip("+" + detection.category().points() + " pts", Theme.BLUE_LIGHT)));
        card.add(Theme.gap(10));
        card.add(Theme.paragraph(detection.explanation(), Theme.sans(14), Theme.MUTED));
        card.add(Theme.gap(14));
        card.add(Theme.divider());
        card.add(Theme.gap(10));
        card.add(Theme.label("Found", Theme.mono(12), Theme.MUTED));
        card.add(Theme.gap(4));
        String found = detection.evidence().stream()
                .map(text -> "\"" + text + "\"")
                .collect(Collectors.joining(", "));
        card.add(Theme.paragraph(found, Theme.mono(12.5f), Theme.BLUE_LIGHT));
        card.add(Box.createVerticalGlue());
        return card;
    }

    // ---------- small custom-drawn pieces ----------

    /** The message text with each suspicious phrase in bold, underlined and tinted. */
    private static final class HighlightedText extends JTextPane {
        HighlightedText(AnalysisResult result, Color risk) {
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));

            SimpleAttributeSet normal = new SimpleAttributeSet();
            StyleConstants.setFontFamily(normal, Theme.SANS);
            StyleConstants.setFontSize(normal, 16);
            StyleConstants.setForeground(normal, Theme.TEXT);
            StyleConstants.setLineSpacing(normal, 0.2f);

            SimpleAttributeSet flagged = new SimpleAttributeSet(normal);
            StyleConstants.setBold(flagged, true);
            StyleConstants.setUnderline(flagged, true);
            StyleConstants.setBackground(flagged, Theme.blend(Theme.PANEL_2, risk, 0.32));

            StyledDocument doc = getStyledDocument();
            doc.setParagraphAttributes(0, 0, normal, true);
            try {
                for (TextHighlighter.Segment part : TextHighlighter.split(result.message(), result.allEvidence())) {
                    doc.insertString(doc.getLength(), part.text(), part.highlighted() ? flagged : normal);
                }
            } catch (BadLocationException impossible) {
                throw new IllegalStateException(impossible);
            }
        }

        /**
         * Wraps to the width it's given. Swing measures wrapped text at the pane's current width,
         * so once the pane has a width its own preferred height is correct.
         */
        @Override
        public Dimension getPreferredSize() {
            if (getWidth() <= 0 && getParent() != null && getParent().getWidth() > 0) {
                setSize(getParent().getWidth(), Short.MAX_VALUE);   // first measure: use the bubble's width
            }
            Dimension natural = super.getPreferredSize();
            return new Dimension(60, natural.height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** The 0–100 scale: green, amber and red zones with a white marker at the score. */
    private static final class ScaleBar extends JComponent {
        private final int score;

        ScaleBar(int score) {
            this.score = score;
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(300, 44));
            setMaximumSize(new Dimension(620, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            int[] starts = {0, (int) (w * 0.31), (int) (w * 0.61)};
            int[] ends = {(int) (w * 0.31) - 3, (int) (w * 0.61) - 3, w};
            Color[] colours = {Theme.LOW, Theme.SUSPICIOUS, Theme.HIGH};
            String[] names = {"Low 0–30", "Suspicious 31–60", "High 61–100"};
            g2.setFont(Theme.mono(11));
            for (int i = 0; i < 3; i++) {
                Color c = colours[i];
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
                g2.fillRoundRect(starts[i], 8, ends[i] - starts[i], 6, 6, 6);
                g2.setColor(Theme.MUTED);
                g2.drawString(names[i], starts[i], 36);
            }
            int x = (int) Math.round((w - 4) * score / 100.0);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(x - 3, 0, 10, 22, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, 2, 4, 18, 3, 3);
            g2.dispose();
        }
    }

    /** A small glowing blue square in front of each tip. */
    private static final class Bullet extends JComponent {
        Bullet() {
            setPreferredSize(new Dimension(10, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(new Color(59, 130, 246, 70));
            g2.fillRoundRect(0, 6, 10, 10, 4, 4);
            g2.setColor(Theme.BLUE_HOVER);
            g2.fillRoundRect(1, 7, 8, 8, 3, 3);
            g2.dispose();
        }
    }
}
