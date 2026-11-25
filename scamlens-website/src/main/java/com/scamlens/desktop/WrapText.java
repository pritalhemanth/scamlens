package com.scamlens.desktop;

import javax.swing.JTextArea;
import javax.swing.text.View;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/**
 * A read-only paragraph that wraps onto several lines (INHERITANCE: it extends JTextArea).
 *
 * Swing labels don't wrap, so this works out its height from the width it is given.
 */
public class WrapText extends JTextArea {

    public WrapText(String text, Font font, Color color) {
        super(text);
        setFont(font);
        setForeground(color);
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setHighlighter(null);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** Height = what the text needs at the current width (POLYMORPHISM: overrides JTextArea). */
    @Override
    public Dimension getPreferredSize() {
        int width = getWidth();
        if (width <= 0 && getParent() != null) {
            Container parent = getParent();
            Insets in = parent.getInsets();
            width = parent.getWidth() - in.left - in.right;
        }
        if (width <= 0) {
            return super.getPreferredSize();
        }
        View root = getUI().getRootView(this);
        Insets in = getInsets();
        root.setSize(Math.max(1, width - in.left - in.right), Integer.MAX_VALUE);
        int height = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + in.top + in.bottom;
        return new Dimension(60, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
