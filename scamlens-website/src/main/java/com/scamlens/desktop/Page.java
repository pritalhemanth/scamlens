package com.scamlens.desktop;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * The background of every screen: near-black with a blue glow at the top.
 * Content is stacked top to bottom, and the page scrolls when it's taller than the window.
 */
public class Page extends JPanel implements Scrollable {

    private int lastWidth = -1;

    public Page() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(32, 48, 56, 48));
        setBackground(Theme.BG);

        // EVENT: when the window is resized, lay out again so wrapped text gets the right height
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() != lastWidth) {
                    lastWidth = getWidth();
                    SwingUtilities.invokeLater(() -> { revalidate(); repaint(); });
                }
            }
        });
    }

    /** Adds a block to the page, lined up on the left. */
    public void row(JComponent block) {
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(block);
    }

    /** Adds empty space. */
    public void gap(int height) {
        add(Theme.gap(height));
    }

    /** Removes everything, ready for new content. */
    public void clear() {
        removeAll();
    }

    /** Re-measures everything after new content is added (twice, so wrapped text settles). */
    public void refresh() {
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> { revalidate(); repaint(); scrollToTop(); });
    }

    /** Scrolls back to the top of the page. */
    public void scrollToTop() {
        SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(Theme.BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // A soft blue glow, wider than it is tall, centred above the top edge
        float radius = Math.max(400, getWidth() * 0.55f);
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(getWidth() * 0.55f, -40), radius,
                new Point2D.Float(getWidth() * 0.55f, -40),
                new float[]{0f, 1f},
                new Color[]{new Color(37, 99, 235, 95), new Color(37, 99, 235, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                AffineTransform.getScaleInstance(1, 0.55));
        g2.setPaint(glow);
        g2.fillRect(0, 0, getWidth(), (int) (radius * 0.6));
        g2.dispose();
    }

    // ----- Scrollable: the page always matches the window's width and scrolls up and down -----

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return o == SwingConstants.VERTICAL ? r.height - 40 : r.width;
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
