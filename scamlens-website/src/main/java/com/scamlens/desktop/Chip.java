package com.scamlens.desktop;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** A small rounded tag with monospace text, like "+25 pts" or "Rule-based". */
public class Chip extends JLabel {

    public Chip(String text) {
        this(text, Theme.MUTED);
    }

    public Chip(String text, Color textColour) {
        super(text);
        setFont(Theme.mono(11.5f));
        setForeground(textColour);
        setBorder(new EmptyBorder(3, 10, 3, 10));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int h = getHeight() - 1;
        g2.setColor(new Color(37, 99, 235, 26));
        g2.fillRoundRect(0, 0, getWidth() - 1, h, h, h);
        g2.setColor(Theme.LINE_STRONG);
        g2.drawRoundRect(0, 0, getWidth() - 1, h, h, h);
        g2.dispose();
        super.paintComponent(g);
    }
}
