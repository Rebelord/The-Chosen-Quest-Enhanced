package thechosenquest.desktop;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

final class BackgroundPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BufferedImage background;
    private final float veil;
    private final boolean gradientVeil;

    BackgroundPanel(float veil) {
        this("/assets/quest-background.png", veil);
    }

    BackgroundPanel(String resource, float veil) {
        this(resource, veil, false);
    }

    BackgroundPanel(String resource, float veil, boolean gradientVeil) {
        this.veil = veil;
        this.gradientVeil = gradientVeil;
        BufferedImage loaded = null;
        try {
            loaded = ImageIO.read(BackgroundPanel.class.getResource(resource));
        } catch (Exception ignored) {
            // A solid background remains usable if the image cannot be read.
        }
        background = loaded;
        setBackground(new Color(22, 24, 28));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (background == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.max((double) getWidth() / background.getWidth(),
            (double) getHeight() / background.getHeight());
        int width = (int) Math.ceil(background.getWidth() * scale);
        int height = (int) Math.ceil(background.getHeight() * scale);
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        g.drawImage(background, x, y, width, height, null);
        if (gradientVeil) {
            g.setComposite(AlphaComposite.SrcOver);
            g.setPaint(new LinearGradientPaint(0, 0, 0, Math.max(1, getHeight()),
                new float[] {0.0f, 0.55f, 1.0f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 102),
                    new Color(0, 0, 0, 204)}));
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g.setComposite(AlphaComposite.SrcOver.derive(veil));
            g.setColor(new Color(10, 12, 16));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.dispose();
    }
}
