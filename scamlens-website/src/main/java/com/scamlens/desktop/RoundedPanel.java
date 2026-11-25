package com.scamlens.desktop;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/**
 * A panel with rounded corners, a fill colour and a thin border: the "glass card"
 * used all over the app. INHERITANCE: extends JPanel. POLYMORPHISM: overrides paintComponent.
 */
public class RoundedPanel extends JPanel {

    private Color fill;
    private Color border;
    private final int radius;

    public RoundedPanel(LayoutManager layout, Color fill, Color border, int radius, int padding) {
        super(layout);
        this.fill = fill;
        this.border = border;
        this.radius = radius;
        setOpaque(false);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    public void setColours(Color fill, Color border) {
        this.fill = fill;
        this.border = border;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
    }
}
