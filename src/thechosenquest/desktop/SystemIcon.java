package thechosenquest.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Small code-native system icons that remain stable across installed fonts. */
final class SystemIcon implements Icon {
    enum Type { SPEAKER_ON, SPEAKER_MUTED, SETTINGS, CLOSE, STAR, BOOKMARK, DICE }

    private final Type type;
    private final int size;
    private final Color color;

    SystemIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    public int getIconWidth() { return size; }
    public int getIconHeight() { return size; }

    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1.4f, size / 12.0f),
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (type == Type.SETTINGS) {
            paintGear(g);
        } else if (type == Type.CLOSE) {
            double inset = size * 0.24;
            g.draw(new Line2D.Double(inset, inset, size - inset, size - inset));
            g.draw(new Line2D.Double(size - inset, inset, inset, size - inset));
        } else if (type == Type.STAR) {
            paintStar(g);
        } else if (type == Type.BOOKMARK) {
            paintBookmark(g);
        } else if (type == Type.DICE) {
            paintDice(g);
        } else {
            paintSpeaker(g, type == Type.SPEAKER_MUTED);
        }
        g.dispose();
    }

    private void paintSpeaker(Graphics2D g, boolean muted) {
        Path2D speaker = new Path2D.Double();
        speaker.moveTo(size * 0.14, size * 0.40);
        speaker.lineTo(size * 0.34, size * 0.40);
        speaker.lineTo(size * 0.56, size * 0.20);
        speaker.lineTo(size * 0.56, size * 0.80);
        speaker.lineTo(size * 0.34, size * 0.60);
        speaker.lineTo(size * 0.14, size * 0.60);
        speaker.closePath();
        g.draw(speaker);
        if (muted) {
            g.draw(new Line2D.Double(size * 0.68, size * 0.38, size * 0.90, size * 0.62));
            g.draw(new Line2D.Double(size * 0.90, size * 0.38, size * 0.68, size * 0.62));
        } else {
            g.drawArc((int) (size * 0.52), (int) (size * 0.32),
                (int) (size * 0.25), (int) (size * 0.36), -55, 110);
            g.drawArc((int) (size * 0.52), (int) (size * 0.20),
                (int) (size * 0.42), (int) (size * 0.60), -50, 100);
        }
    }

    private void paintGear(Graphics2D g) {
        double center = size / 2.0;
        double outer = size * 0.34;
        double inner = size * 0.15;
        Path2D gear = new Path2D.Double();
        for (int i = 0; i < 16; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 8.0;
            double radius = (i % 2 == 0) ? outer : outer * 0.78;
            double px = center + Math.cos(angle) * radius;
            double py = center + Math.sin(angle) * radius;
            if (i == 0) gear.moveTo(px, py); else gear.lineTo(px, py);
        }
        gear.closePath();
        g.draw(gear);
        g.draw(new Ellipse2D.Double(center - inner, center - inner, inner * 2, inner * 2));
    }

    private void paintStar(Graphics2D g) {
        double center = size / 2.0;
        g.draw(new Ellipse2D.Double(0.8, 0.8, size - 1.6, size - 1.6));
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
            double radius = (i % 2 == 0) ? size * 0.27 : size * 0.12;
            double px = center + Math.cos(angle) * radius;
            double py = center + Math.sin(angle) * radius;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.closePath();
        g.draw(star);
    }

    private void paintBookmark(Graphics2D g) {
        Path2D mark = new Path2D.Double();
        mark.moveTo(size * 0.28, size * 0.18);
        mark.lineTo(size * 0.72, size * 0.18);
        mark.lineTo(size * 0.72, size * 0.82);
        mark.lineTo(size * 0.50, size * 0.66);
        mark.lineTo(size * 0.28, size * 0.82);
        mark.closePath();
        g.draw(mark);
    }

    private void paintDice(Graphics2D g) {
        double inset = size * .12;
        double side = size - inset * 2;
        g.draw(new RoundRectangle2D.Double(inset, inset, side, side,
            size * .20, size * .20));
        double pip = Math.max(2d, size * .105);
        paintPip(g, size * .31, size * .31, pip);
        paintPip(g, size * .69, size * .31, pip);
        paintPip(g, size * .50, size * .50, pip);
        paintPip(g, size * .31, size * .69, pip);
        paintPip(g, size * .69, size * .69, pip);
    }

    private void paintPip(Graphics2D g, double centerX, double centerY, double diameter) {
        g.fill(new Ellipse2D.Double(centerX - diameter / 2d,
            centerY - diameter / 2d, diameter, diameter));
    }
}
