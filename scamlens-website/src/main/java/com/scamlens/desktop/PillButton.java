package com.scamlens.desktop;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A rounded "pill" button in one of four styles.
 * INHERITANCE: extends JButton, so it still fires ActionEvents like any button.
 * POLYMORPHISM: overrides paintComponent to draw its own shape.
 */
public class PillButton extends JButton {

    public enum Style { PRIMARY, GHOST, DANGER, NAV }

    private final Style style;
    private boolean active;

    public PillButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(style == Style.NAV ? Theme.sans(14.5f) : Theme.sans(14).deriveFont(java.awt.Font.BOLD));
        setForeground(switch (style) {
            case PRIMARY -> Color.WHITE;
            case DANGER -> Theme.ERROR_TEXT;
            case NAV -> Theme.MUTED;
            case GHOST -> Theme.TEXT;
        });
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(style == Style.NAV ? new EmptyBorder(7, 16, 7, 16) : new EmptyBorder(10, 20, 10, 20));
        if (style == Style.NAV) {
            setFocusable(false);
        }
    }

    /** For navigation buttons: highlights the page you're on. */
    public void setActive(boolean active) {
        this.active = active;
        setForeground(active ? Theme.TEXT : Theme.MUTED);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        ButtonModel m = getModel();
        boolean hover = m.isRollover() && isEnabled();
        int w = getWidth() - 1;
        int h = getHeight() - 1;

        Color fill = null;
        Color line = null;
        switch (style) {
            case PRIMARY -> fill = m.isPressed() ? Theme.BLUE.darker() : hover ? Theme.BLUE_HOVER : Theme.BLUE;
            case GHOST -> { fill = hover ? Theme.PANEL_2 : Theme.BG; line = hover ? Theme.BLUE_LIGHT : Theme.LINE_STRONG; }
            case DANGER -> { fill = hover ? new Color(60, 18, 22) : Theme.BG; line = new Color(239, 68, 68, hover ? 180 : 90); }
            case NAV -> fill = active ? new Color(110, 150, 255, 34) : hover ? new Color(110, 150, 255, 16) : null;
        }
        if (!isEnabled()) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
        }
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, h, h);
        }
        if (line != null) {
            g2.setColor(line);
            g2.drawRoundRect(0, 0, w, h, h, h);
        }
        if (isFocusOwner()) {   // visible keyboard focus
            g2.setColor(Theme.BLUE_LIGHT);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, h - 2, h - 2);
        }
        g2.dispose();
        super.paintComponent(g);   // draws the text
    }
}
