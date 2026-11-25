package com.scamlens.desktop;

import com.scamlens.model.RiskLevel;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Colours, fonts and small helper methods shared by every screen of the Swing app.
 *
 * Keeping them in one place (ENCAPSULATION) means the whole look can be changed here.
 * The colours match the website: near-black, electric blue, and green / amber / red for risk.
 */
public final class Theme {

    private Theme() { }

    // ---------- colours ----------
    public static final Color BG = new Color(0x03060D);
    public static final Color PANEL = new Color(0x0B1428);
    public static final Color PANEL_2 = new Color(0x111E38);
    public static final Color LINE = new Color(0x1B2944);
    public static final Color LINE_STRONG = new Color(0x2A3D66);
    public static final Color TEXT = new Color(0xEEF3FF);
    public static final Color MUTED = new Color(0x8A97B4);
    public static final Color BLUE = new Color(0x2563EB);
    public static final Color BLUE_HOVER = new Color(0x3B82F6);
    public static final Color BLUE_LIGHT = new Color(0x93BBFF);
    public static final Color LOW = new Color(0x22C55E);
    public static final Color SUSPICIOUS = new Color(0xF59E0B);
    public static final Color HIGH = new Color(0xEF4444);
    public static final Color ERROR_TEXT = new Color(0xFCA5A5);

    // ---------- fonts: the first one installed on the computer is used ----------
    private static final Set<String> INSTALLED = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    public static final String SANS = pick("Segoe UI", "Inter", "Helvetica Neue", "Arial");
    public static final String LIGHT = pick("Segoe UI Light", "Segoe UI Semilight", "Inter Light", SANS);
    public static final String MONO = pick("Cascadia Mono", "Consolas", "DejaVu Sans Mono", Font.MONOSPACED);

    /** FUNCTIONAL style: the first font name that is installed, or a safe default. */
    private static String pick(String... names) {
        return Arrays.stream(names)
                .filter(INSTALLED::contains)
                .findFirst()
                .orElse(Font.SANS_SERIF);
    }

    public static Font sans(float size)     { return new Font(SANS, Font.PLAIN, 12).deriveFont(size); }
    public static Font sansBold(float size) { return new Font(SANS, Font.BOLD, 12).deriveFont(size); }
    public static Font light(float size)    { return new Font(LIGHT, Font.PLAIN, 12).deriveFont(size); }
    public static Font mono(float size)     { return new Font(MONO, Font.PLAIN, 12).deriveFont(size); }

    // ---------- colour helpers ----------
    public static Color riskColor(RiskLevel level) {
        return switch (level) {
            case LOW -> LOW;
            case SUSPICIOUS -> SUSPICIOUS;
            case HIGH -> HIGH;
        };
    }

    /** Mixes two colours: amount 0 = all of a, 1 = all of b. */
    public static Color blend(Color a, Color b, double amount) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    /** Turns on smooth edges for custom drawing. */
    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ---------- small building blocks ----------
    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** A paragraph of text that wraps onto several lines. */
    public static WrapText paragraph(String text, Font font, Color color) {
        return new WrapText(text, font, color);
    }

    /** Empty vertical space of the given height. */
    public static JComponent gap(int height) {
        JComponent gap = (JComponent) Box.createRigidArea(new Dimension(0, height));
        gap.setAlignmentX(Component.LEFT_ALIGNMENT);
        return gap;
    }

    /** A see-through row with components side by side. */
    public static JPanel flow(int align, int hgap, Component... parts) {
        JPanel row = new JPanel(new FlowLayout(align, hgap, 0));
        row.setOpaque(false);
        for (Component part : parts) {
            row.add(part);
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** A row with one thing on the left and one on the right; it never grows taller than it needs. */
    public static JPanel spread(Component west, Component east) {
        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        if (west != null) row.add(west, BorderLayout.WEST);
        if (east != null) row.add(east, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** A thin horizontal line. */
    public static JComponent divider() {
        JComponent line = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(LINE);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        line.setPreferredSize(new Dimension(10, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    /** "High risk" in red with a coloured dot in front of it. */
    public static JLabel levelLabel(RiskLevel level, float size) {
        JLabel label = label(level.label(), sansBold(size), riskColor(level));
        label.setIcon(new DotIcon(riskColor(level)));
        label.setIconTextGap(8);
        return label;
    }

    /** A dark checkbox that matches the theme. */
    public static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setOpaque(false);
        box.setForeground(MUTED);
        box.setFont(sans(13.5f));
        box.setFocusPainted(false);
        box.setIcon(new CheckIcon(false));
        box.setSelectedIcon(new CheckIcon(true));
        box.setIconTextGap(8);
        return box;
    }

    /** Puts a page in a dark scroll pane with a slim scroll bar. */
    public static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(null);
        pane.getViewport().setBackground(BG);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pane.getVerticalScrollBar().setBackground(BG);
        return pane;
    }

    /** Colours for pop-up dialogs (JOptionPane), set once when the app starts. */
    public static void applyDialogColours() {
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", sans(14));
        UIManager.put("OptionPane.buttonFont", sans(13));
        UIManager.put("Button.background", PANEL_2);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", BLUE);
        UIManager.put("Button.focus", BLUE_LIGHT);
        UIManager.put("Button.gradient", null);
        UIManager.put("Label.foreground", TEXT);
    }

    // ---------- small custom-drawn pieces ----------

    /** A small coloured circle, used in front of risk levels. */
    static final class DotIcon implements Icon {
        private final Color color;
        DotIcon(Color color) { this.color = color; }
        @Override public int getIconWidth()  { return 9; }
        @Override public int getIconHeight() { return 9; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g2.fillOval(x - 1, y - 1, 11, 11);
            g2.setColor(color);
            g2.fillOval(x + 1, y + 1, 7, 7);
            g2.dispose();
        }
    }

    /** The checkbox square: dark when empty, blue with a tick when ticked. */
    static final class CheckIcon implements Icon {
        private final boolean ticked;
        CheckIcon(boolean ticked) { this.ticked = ticked; }
        @Override public int getIconWidth()  { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.setColor(ticked ? BLUE : PANEL_2);
            g2.fillRoundRect(x, y, 15, 15, 5, 5);
            g2.setColor(ticked ? BLUE_HOVER : LINE_STRONG);
            g2.drawRoundRect(x, y, 15, 15, 5, 5);
            if (ticked) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 4, x + 7, x + 12}, new int[]{y + 8, y + 11, y + 5}, 3);
            }
            g2.dispose();
        }
    }

    /** A slim, rounded, dark scroll bar. */
    static final class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = LINE_STRONG;
            trackColor = BG;
        }

        @Override protected JButton createDecreaseButton(int orientation) { return invisibleButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return invisibleButton(); }

        private static JButton invisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
            g.setColor(BG);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() ? BLUE : thumbColor);
            g2.fillRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 8, 8);
            g2.dispose();
        }
    }
}
