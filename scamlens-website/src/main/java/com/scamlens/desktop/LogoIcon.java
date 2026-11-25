package com.scamlens.desktop;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The ScamLens logo drawn with Java 2D: a magnifying lens over a chat bubble,
 * on a blue tile. Same design as public/logo.svg. Drawn in a 64 x 64 grid and scaled.
 */
public class LogoIcon implements Icon {

    private final int size;

    public LogoIcon(int size) {
        this.size = size;
    }

    @Override public int getIconWidth()  { return size; }
    @Override public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Theme.smooth(g);
        g2.translate(x, y);
        g2.scale(size / 64.0, size / 64.0);

        // blue tile
        g2.setPaint(new GradientPaint(0, 0, new Color(0x4C8DFF), 64, 64, new Color(0x1D3FBF)));
        g2.fill(new RoundRectangle2D.Double(0, 0, 64, 64, 32, 32));
        g2.setColor(new Color(255, 255, 255, 56));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Double(1, 1, 62, 62, 30, 30));

        // chat bubble with a tail
        Path2D bubble = new Path2D.Double();
        bubble.append(new RoundRectangle2D.Double(15.5, 22.5, 25, 16, 8, 8), false);
        bubble.moveTo(22, 38);
        bubble.lineTo(22, 43);
        bubble.lineTo(27.5, 38);
        bubble.closePath();
        g2.setColor(new Color(255, 255, 255, 46));
        g2.fill(bubble);

        // three "typing" dots
        g2.setColor(Color.WHITE);
        for (double cx : new double[]{23, 28.5, 34}) {
            g2.fill(new Ellipse2D.Double(cx - 2.1, 30.5 - 2.1, 4.2, 4.2));
        }

        // lens ring and handle
        g2.setStroke(new BasicStroke(4f));
        g2.draw(new Ellipse2D.Double(28.5 - 15, 30.5 - 15, 30, 30));
        g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(39.5, 41.5, 50, 52));
        g2.dispose();
    }

    /** The logo at several sizes, for the window and taskbar icon. */
    public static List<Image> windowIcons() {
        return IntStream.of(16, 24, 32, 48, 64, 128)
                .<Image>mapToObj(LogoIcon::render)
                .toList();
    }

    static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        new LogoIcon(size).paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }
}
