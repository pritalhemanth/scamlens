package com.scamlens.desktop;

import com.scamlens.analyzer.MessageAnalyzer;
import com.scamlens.model.AnalysisResult;
import com.scamlens.model.Category;
import com.scamlens.service.HistoryService;
import com.scamlens.util.InputValidator;
import com.scamlens.util.SampleMessages;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Screen 1: where the user pastes a message and clicks Check message.
 *
 * EVENT-DRIVEN: every button has an ActionListener, the text box has a DocumentListener
 * (runs on every change) and a Ctrl + Enter key binding.
 * It does no detection itself: it hands the message to MessageAnalyzer.
 */
public class ScannerPanel extends JPanel {

    private final MessageAnalyzer analyzer;
    private final HistoryService history;
    private final BiConsumer<AnalysisResult, String> onResult;   // what to do with a finished result

    private final JTextArea messageBox = new PlaceholderTextArea("Paste the message here…");
    private final Chip charCount = new Chip("0 / 5,000");
    private final JLabel note = Theme.label(" ", Theme.sans(13.5f), Theme.BLUE_LIGHT);
    private final JCheckBox keepPreview = Theme.checkBox("Keep a masked preview in history");
    private final PillButton checkButton = new PillButton("Check message", PillButton.Style.PRIMARY);
    private final PillButton exampleButton = new PillButton("Try an example", PillButton.Style.GHOST);
    private final PillButton clearButton = new PillButton("Clear", PillButton.Style.GHOST);

    private final Page page = new Page();
    private int nextExample = 0;
    private String loadedExample;   // the example text currently in the box, if any

    public ScannerPanel(MessageAnalyzer analyzer, HistoryService history,
                        BiConsumer<AnalysisResult, String> onResult) {
        super(new BorderLayout());
        this.analyzer = analyzer;
        this.history = history;
        this.onResult = onResult;

        page.row(buildHero());
        page.gap(56);
        page.row(buildStrip());
        page.gap(64);
        page.row(new Chip("Detection rules", Theme.BLUE_LIGHT));
        page.gap(14);
        page.row(twoTone("Six signs scammers ", "rely on", 36));
        page.gap(10);
        page.row(Theme.paragraph("Each sign adds its points once, however often it appears. Asking for an OTP "
                + "together with a link or a payment always counts as high risk.", Theme.sans(16), Theme.MUTED));
        page.gap(28);
        page.row(buildSignGrid());
        add(Theme.scroll(page), BorderLayout.CENTER);

        registerEvents();
        updateCount();
    }

    // =====================================================================
    // EVENTS
    // =====================================================================

    private void registerEvents() {
        // Button clicks (ActionEvent) — lambdas
        checkButton.addActionListener(e -> analyse());
        clearButton.addActionListener(e -> clear());
        exampleButton.addActionListener(e -> loadExample());

        // Runs every time the text changes (typing, pasting, deleting)
        messageBox.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onTextChanged(); }
        });

        // Ctrl + Enter inside the text box also checks the message
        messageBox.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "check");
        messageBox.getActionMap().put("check", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                analyse();
            }
        });
    }

    /** The main action: text box -> MessageAnalyzer -> history -> result screen. */
    private void analyse() {
        String message = messageBox.getText();

        AnalysisResult result;
        try {
            result = analyzer.analyze(message);               // all detection happens in the engine
        } catch (IllegalArgumentException invalidInput) {
            showNote(invalidInput.getMessage(), true);        // e.g. "Please enter or paste a message"
            messageBox.requestFocusInWindow();
            return;
        }

        String savedStatus;
        try {
            history.save(result, keepPreview.isSelected());
            savedStatus = keepPreview.isSelected()
                    ? "Saved to history with a short masked preview."
                    : "Saved to history. The message itself was not stored.";
        } catch (IOException e) {
            savedStatus = "Not saved to history: " + e.getMessage();   // still show the result
        }

        onResult.accept(result, savedStatus);                 // DesktopApp opens the result screen
    }

    private void clear() {
        loadedExample = null;
        messageBox.setText("");
        showNote(" ", false);
        messageBox.requestFocusInWindow();
    }

    private void loadExample() {
        SampleMessages.Sample sample = SampleMessages.ALL.get(nextExample);
        int number = nextExample + 1;
        nextExample = (nextExample + 1) % SampleMessages.ALL.size();

        loadedExample = sample.text();
        messageBox.setText(sample.text());
        messageBox.setCaretPosition(0);
        showNote("Example " + number + " of " + SampleMessages.ALL.size() + ": " + sample.title(), false);
    }

    private void onTextChanged() {
        updateCount();
        String text = messageBox.getText();
        if (loadedExample != null && !loadedExample.equals(text)) {
            loadedExample = null;                              // edited, so it's no longer the example
            showNote(" ", false);
        } else if (loadedExample == null && note.getForeground() == Theme.ERROR_TEXT) {
            showNote(" ", false);                              // clear an old error once they type
        }
    }

    private void updateCount() {
        int length = messageBox.getText().length();
        charCount.setText(String.format("%,d / %,d", length, InputValidator.MAX_LENGTH));
        charCount.setForeground(length > InputValidator.MAX_LENGTH ? Theme.ERROR_TEXT : Theme.MUTED);
    }

    private void showNote(String text, boolean isError) {
        note.setText(text);
        note.setForeground(isError ? Theme.ERROR_TEXT : Theme.BLUE_LIGHT);
    }

    // ----- used by the automated tests -----
    public void setMessage(String text) { messageBox.setText(text); }
    public void clickCheck()            { checkButton.doClick(); }
    public String noteText()            { return note.getText(); }

    /** Shows the top of the page (used when the window first opens). */
    public void scrollToTop()           { page.scrollToTop(); }

    // =====================================================================
    // LAYOUT
    // =====================================================================

    /** Heading on the left, the message window on the right. */
    private JComponent buildHero() {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Theme.label("Is this message", Theme.light(52), Theme.TEXT));
        left.add(Theme.label("a scam?", Theme.light(52), Theme.BLUE_LIGHT));
        left.add(Theme.gap(18));
        left.add(Theme.paragraph("Paste a text, WhatsApp message or email. ScamLens checks it against six "
                + "warning signs scammers rely on and explains every one it finds.", Theme.sans(16.5f), Theme.MUTED));

        JPanel hero = new JPanel(new GridBagLayout());
        hero.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.42;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 40);
        hero.add(left, c);
        c.gridx = 1;
        c.weightx = 0.58;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        hero.add(buildMessageWindow(), c);
        return hero;
    }

    /** The message box, styled like an app window. */
    private JComponent buildMessageWindow() {
        RoundedPanel window = new RoundedPanel(new BorderLayout(), Theme.PANEL, Theme.LINE_STRONG, 20, 0);

        JPanel bar = Theme.spread(
                Theme.flow(FlowLayout.LEFT, 10, new WindowLights(),
                        Theme.label("ScamLens · Scanner", Theme.mono(12.5f), Theme.MUTED)),
                charCount);
        bar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.LINE), new EmptyBorder(12, 16, 12, 16)));
        window.add(bar, BorderLayout.NORTH);

        messageBox.setFont(Theme.sans(16));
        messageBox.setForeground(Theme.TEXT);
        messageBox.setCaretColor(Theme.TEXT);
        messageBox.setBackground(Theme.PANEL);
        messageBox.setSelectionColor(new Color(37, 99, 235, 120));
        messageBox.setSelectedTextColor(Theme.TEXT);
        messageBox.setLineWrap(true);
        messageBox.setWrapStyleWord(true);
        messageBox.setBorder(new EmptyBorder(16, 18, 16, 18));
        JScrollPane textScroll = Theme.scroll(messageBox);
        textScroll.getViewport().setBackground(Theme.PANEL);
        textScroll.setPreferredSize(new Dimension(420, 250));
        window.add(textScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JPanel noteRow = Theme.spread(note, null);
        noteRow.setBorder(new EmptyBorder(0, 18, 8, 18));
        bottom.add(noteRow);
        JPanel actions = Theme.spread(keepPreview,
                Theme.flow(FlowLayout.RIGHT, 8, exampleButton, clearButton, checkButton));
        actions.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.LINE), new EmptyBorder(12, 16, 14, 16)));
        bottom.add(actions);
        window.add(bottom, BorderLayout.SOUTH);
        return window;
    }

    /** Four facts about how ScamLens works. */
    private JComponent buildStrip() {
        RoundedPanel strip = new RoundedPanel(new GridLayout(1, 4), Theme.PANEL, Theme.LINE, 18, 0);
        strip.add(stripCell("6", "Warning signs checked", "Rule-based", false));
        strip.add(stripCell("8", "Checks on every link", "Shorteners, IPs, fake sites", true));
        strip.add(stripCell("0", "Messages stored in full", "Masked previews", true));
        strip.add(stripCell("100", "Highest risk score", "Capped", true));
        return strip;
    }

    private JComponent stripCell(String number, String caption, String chip, boolean lineOnLeft) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(new CompoundBorder(
                new MatteBorder(0, lineOnLeft ? 1 : 0, 0, 0, Theme.LINE), new EmptyBorder(22, 12, 22, 12)));
        JLabel big = Theme.label(number, Theme.light(38), Theme.TEXT);
        JLabel small = Theme.label(caption, Theme.mono(12), Theme.MUTED);
        Chip tag = new Chip(chip);
        for (JComponent part : new JComponent[]{big, small, tag}) {
            part.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        cell.add(big);
        cell.add(Theme.gap(6));
        cell.add(small);
        cell.add(Theme.gap(10));
        cell.add(tag);
        for (Component part : cell.getComponents()) {
            ((JComponent) part).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        return cell;
    }

    /** One card per warning sign, built from the Category enum (FUNCTIONAL: a stream). */
    private JComponent buildSignGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));
        grid.setOpaque(false);
        Arrays.stream(Category.values())
                .map(this::signCard)
                .forEach(grid::add);
        return grid;
    }

    private JComponent signCard(Category category) {
        RoundedPanel card = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.spread(Theme.label(category.label(), Theme.sansBold(15.5f), Theme.TEXT),
                new Chip("+" + category.points() + " pts", Theme.BLUE_LIGHT)));
        card.add(Theme.gap(10));
        card.add(Theme.paragraph(category.explanation(), Theme.sans(13.5f), Theme.MUTED));
        card.add(Theme.gap(14));
        card.add(Theme.divider());
        card.add(Theme.gap(10));
        card.add(Theme.spread(Theme.label("Example", Theme.mono(12), Theme.MUTED),
                Theme.label(example(category), Theme.mono(12), Theme.BLUE_LIGHT)));
        card.add(javax.swing.Box.createVerticalGlue());
        return card;
    }

    private static String example(Category category) {
        return switch (category) {
            case URGENCY -> "\"act now\"";
            case THREAT -> "\"account will be blocked\"";
            case REWARD -> "\"you have won\"";
            case CREDENTIAL -> "\"share your OTP\"";
            case FINANCIAL -> "\"pay a processing fee\"";
            case URL -> "bit.ly, http://, fake sites";
        };
    }

    /** A heading in two colours: "Six signs scammers" white, "rely on" light blue. */
    static JComponent twoTone(String first, String second, float size) {
        return Theme.flow(FlowLayout.LEFT, 0,
                Theme.label(first, Theme.light(size), Theme.TEXT),
                Theme.label(second, Theme.light(size), Theme.BLUE_LIGHT));
    }

    // =====================================================================
    // SMALL CUSTOM PIECES
    // =====================================================================

    /** A text box that shows grey hint text while it's empty. */
    private static final class PlaceholderTextArea extends JTextArea {
        private final String hint;

        PlaceholderTextArea(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(new Color(0x5D6A88));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(hint, in.left, in.top + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }

    /** The three coloured dots in the corner of the message window. */
    private static final class WindowLights extends JComponent {
        WindowLights() {
            setPreferredSize(new Dimension(42, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            Color[] colours = {new Color(0xF87171), new Color(0xFBBF24), new Color(0x4ADE80)};
            for (int i = 0; i < colours.length; i++) {
                g2.setColor(colours[i]);
                g2.fillOval(i * 15, 1, 10, 10);
            }
            g2.dispose();
        }
    }
}
