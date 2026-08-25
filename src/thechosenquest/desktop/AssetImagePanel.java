package thechosenquest.desktop;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class AssetImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Map<String, SoftReference<BufferedImage>> CACHE = boundedCache(64);
    private static final Map<String, SoftReference<BufferedImage>> RENDER_CACHE = boundedCache(160);
    private static final ThreadPoolExecutor LOADER = new ThreadPoolExecutor(
        2, 2, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        private int number;
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "chosen-quest-image-loader-" + (++number));
            thread.setDaemon(true);
            return thread;
        }
        });
    static {
        // The game reads bundled resources and does not benefit from ImageIO's
        // temporary-disk cache. Keeping decoding in memory avoids intermittent
        // filesystem stalls during rapid portrait changes.
        ImageIO.setUseCache(false);
    }
    private BufferedImage image;
    private BufferedImage rendered;
    private BufferedImage renderedSource;
    private int renderedWidth;
    private int renderedHeight;
    private boolean renderedCover;
    private double renderedCropAnchorY;
    private boolean renderedFlipHorizontal;
    private boolean cover;
    // 0 keeps the source's top edge visible, .5 is a conventional centered crop.
    private double cropAnchorY = .5d;
    private boolean flipHorizontal;
    private boolean animatedTransitions;
    private float transitionOpacity = 1f;
    private final Timer transitionTimer = new Timer(24, event -> {
        transitionOpacity = Math.min(1f, transitionOpacity + .12f);
        repaint();
        if (transitionOpacity >= 1f) ((Timer) event.getSource()).stop();
    });
    private String requestedResource;
    private String displayedResource;
    private Future<?> pendingLoad;

    AssetImagePanel(String resource, boolean cover) {
        this.cover = cover;
        setOpaque(true);
        setBackground(UiTheme.SURFACE_DEEP);
        setResource(resource);
    }

    void setResource(String resource) {
        if (pendingLoad != null) {
            pendingLoad.cancel(true);
            LOADER.purge();
            pendingLoad = null;
        }
        requestedResource = resource;
        image = load(resource);
        displayedResource = image == null ? null : resource;
        clearRendered();
        repaint();
    }

    void setResourceAsync(final String resource) {
        if (same(resource, requestedResource) &&
                (image != null || (pendingLoad != null && !pendingLoad.isDone()))) return;
        boolean resourceChanged = !same(resource, requestedResource);
        if (pendingLoad != null) {
            pendingLoad.cancel(true);
            // Rapid character/build switching used to leave canceled work queued,
            // which made later selections progressively slower.
            LOADER.purge();
        }
        requestedResource = resource;
        Insets imageInsets = imageInsets();
        final int targetWidth = Math.max(1,
            (getWidth() > 0 ? getWidth() : getPreferredSize().width) -
                imageInsets.left - imageInsets.right);
        final int targetHeight = Math.max(1,
            (getHeight() > 0 ? getHeight() : getPreferredSize().height) -
                imageInsets.top - imageInsets.bottom);
        final boolean targetCover = cover;
        final double targetCropAnchorY = cropAnchorY;
        final boolean targetFlipHorizontal = flipHorizontal;
        // Revisiting a build should be instantaneous even if newer image work
        // is queued. Promote an already rendered frame directly on the EDT.
        BufferedImage cachedSource = cached(resource);
        BufferedImage cachedRender = renderedCached(resource, targetWidth,
            targetHeight, targetCover, targetCropAnchorY, targetFlipHorizontal);
        if (cachedSource != null && cachedRender != null) {
            image = cachedSource;
            displayedResource = resource;
            rendered = cachedRender;
            renderedSource = cachedSource;
            renderedWidth = targetWidth;
            renderedHeight = targetHeight;
            renderedCover = targetCover;
            renderedCropAnchorY = targetCropAnchorY;
            renderedFlipHorizontal = targetFlipHorizontal;
            pendingLoad = null;
            startTransition();
            repaint();
            return;
        }
        // Never leave the previous encounter's portrait on screen while a new
        // resource is loading. That made a freshly encountered creature appear
        // to use whichever enemy graphic happened to be rendered immediately
        // before it (for example, Swamp Serpent showing Armored Boar artwork).
        if (resourceChanged) {
            image = null;
            displayedResource = null;
            clearRendered();
            repaint();
        }
        pendingLoad = LOADER.submit(new Runnable() {
            public void run() {
                if (Thread.currentThread().isInterrupted()) return;
                final BufferedImage loaded = load(resource);
                if (Thread.currentThread().isInterrupted()) return;
                final BufferedImage scaled = renderCached(resource, loaded, targetWidth,
                    targetHeight, targetCover, targetCropAnchorY, targetFlipHorizontal);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (same(resource, requestedResource)) {
                            image = loaded;
                            displayedResource = loaded == null ? null : resource;
                            rendered = scaled;
                            renderedSource = loaded;
                            renderedWidth = targetWidth;
                            renderedHeight = targetHeight;
                            renderedCover = targetCover;
                            renderedCropAnchorY = targetCropAnchorY;
                            renderedFlipHorizontal = targetFlipHorizontal;
                            pendingLoad = null;
                            startTransition();
                            repaint();
                        }
                    }
                });
            }
        });
    }

    /**
     * Warms a scaled frame on a low-priority thread. Character creation calls
     * this while the title screen is visible, so later class/race exploration
     * uses the small render cache instead of decoding multi-megabyte originals.
     */
    static void preloadRenderedAsync(final String[] resources, final int width,
                                     final int height, final boolean cover) {
        if (resources == null || resources.length == 0) return;
        Thread preloader = new Thread(new Runnable() {
            public void run() {
                for (String resource : resources) {
                    if (Thread.currentThread().isInterrupted()) return;
                    BufferedImage source = load(resource);
                    renderCached(resource, source, width, height, cover, .5d, false);
                }
            }
        }, "chosen-quest-avatar-preloader");
        preloader.setDaemon(true);
        preloader.setPriority(Thread.MIN_PRIORITY);
        preloader.start();
    }

    static int renderedCacheSizeForTest() {
        synchronized (RENDER_CACHE) { return RENDER_CACHE.size(); }
    }

    /**
     * Waits for the current request when a deterministic snapshot/export is needed.
     * Normal game rendering never calls this and therefore remains non-blocking.
     */
    void awaitResource() {
        Future<?> load = pendingLoad;
        if (load != null) {
            try {
                load.get(5L, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // Missing optional art is represented by the panel's dark fallback.
            }
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { }
                });
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // A failed snapshot flush should not affect the live interface.
            }
        }
    }

    String displayedResourceForTest() {
        return displayedResource;
    }

    void setCover(boolean value) {
        cover = value;
        clearRendered();
        repaint();
    }

    /**
     * Selects the vertical focal point used when a cover image must be cropped.
     * This lets artwork with important detail near its upper edge retain that
     * detail without changing the dimensions of every image panel.
     */
    void setCropAnchorY(double value) {
        cropAnchorY = Math.max(0d, Math.min(1d, value));
        clearRendered();
        repaint();
    }

    /** Mirrors artwork at render time without altering or recompressing its source file. */
    void setFlipHorizontal(boolean value) {
        flipHorizontal = value;
        clearRendered();
        repaint();
    }

    void setAnimatedTransitions(boolean value) {
        animatedTransitions = value;
        if (!value) {
            transitionTimer.stop();
            transitionOpacity = 1f;
        }
    }

    private void startTransition() {
        if (!animatedTransitions) {
            transitionOpacity = 1f;
            return;
        }
        transitionOpacity = .4f;
        transitionTimer.restart();
    }

    private static BufferedImage load(String resource) {
        if (resource == null) return null;
        BufferedImage cached = cached(resource);
        if (cached != null) return cached;
        BufferedImage loaded = null;
        try {
            loaded = ImageIO.read(AssetImagePanel.class.getResource(resource));
        } catch (Exception ignored) {
            // The dark fallback remains usable when an optional asset is missing.
        }
        if (loaded != null) {
            synchronized (CACHE) {
                SoftReference<BufferedImage> reference = CACHE.get(resource);
                BufferedImage existing = reference == null ? null : reference.get();
                if (existing != null) return existing;
                CACHE.put(resource, new SoftReference<BufferedImage>(loaded));
            }
        }
        return loaded;
    }

    private static BufferedImage cached(String resource) {
        if (resource == null) return null;
        synchronized (CACHE) {
            SoftReference<BufferedImage> reference = CACHE.get(resource);
            BufferedImage image = reference == null ? null : reference.get();
            if (reference != null && image == null) CACHE.remove(resource);
            return image;
        }
    }

    private static BufferedImage renderCached(String resource, BufferedImage source,
                                               int width, int height, boolean cover,
                                               double cropAnchorY, boolean flipHorizontal) {
        if (source == null) return null;
        String key = resource + "|" + width + "x" + height + "|" + cover +
            "|" + cropAnchorY;
        key += "|" + flipHorizontal;
        synchronized (RENDER_CACHE) {
            SoftReference<BufferedImage> reference = RENDER_CACHE.get(key);
            BufferedImage cached = reference == null ? null : reference.get();
            if (cached != null) return cached;
        }
        BufferedImage rendered = render(source, width, height, cover, cropAnchorY,
            flipHorizontal);
        synchronized (RENDER_CACHE) {
            RENDER_CACHE.put(key, new SoftReference<BufferedImage>(rendered));
        }
        return rendered;
    }

    private static BufferedImage renderedCached(String resource, int width, int height,
                                                 boolean cover, double cropAnchorY,
                                                 boolean flipHorizontal) {
        String key = resource + "|" + width + "x" + height + "|" + cover +
            "|" + cropAnchorY + "|" + flipHorizontal;
        synchronized (RENDER_CACHE) {
            SoftReference<BufferedImage> reference = RENDER_CACHE.get(key);
            BufferedImage cached = reference == null ? null : reference.get();
            if (reference != null && cached == null) RENDER_CACHE.remove(key);
            return cached;
        }
    }

    private static boolean same(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static Map<String, SoftReference<BufferedImage>> boundedCache(final int limit) {
        return new LinkedHashMap<String, SoftReference<BufferedImage>>(limit, .75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, SoftReference<BufferedImage>> eldest) {
                return size() > limit;
            }
        };
    }

    private void clearRendered() {
        rendered = null;
        renderedSource = null;
        renderedWidth = 0;
        renderedHeight = 0;
    }

    /** Portrait artwork extends beneath decorative borders like a physical frame. */
    private Insets imageInsets() {
        if (getBorder() instanceof FantasyPortraitBorder) {
            FantasyPortraitBorder frame = (FantasyPortraitBorder) getBorder();
            int width = getWidth() > 0 ? getWidth() : getPreferredSize().width;
            int height = getHeight() > 0 ? getHeight() : getPreferredSize().height;
            return frame.viewportInsets(Math.max(1, width), Math.max(1, height));
        }
        return new Insets(0, 0, 0, 0);
    }

    private static BufferedImage render(BufferedImage source, int panelWidth,
                                        int panelHeight, boolean cover,
                                        double cropAnchorY, boolean flipHorizontal) {
        if (source == null || panelWidth < 1 || panelHeight < 1) return null;
        BufferedImage result = new BufferedImage(panelWidth, panelHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = cover
            ? Math.max((double) panelWidth / source.getWidth(),
                (double) panelHeight / source.getHeight())
            : Math.min((double) panelWidth / source.getWidth(),
                (double) panelHeight / source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (panelWidth - width) / 2;
        int y = cover
            ? (int) Math.round((panelHeight - height) * cropAnchorY)
            : (panelHeight - height) / 2;
        if (flipHorizontal) {
            g.drawImage(source, x + width, y, -width, height, null);
        } else {
            g.drawImage(source, x, y, width, height, null);
        }
        g.dispose();
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) return;
        Insets imageInsets = imageInsets();
        int targetWidth = Math.max(1, getWidth() - imageInsets.left - imageInsets.right);
        int targetHeight = Math.max(1, getHeight() - imageInsets.top - imageInsets.bottom);
        if (rendered == null || renderedSource != image ||
                renderedWidth != targetWidth || renderedHeight != targetHeight ||
                renderedCover != cover || renderedCropAnchorY != cropAnchorY ||
                renderedFlipHorizontal != flipHorizontal) {
            rendered = renderCached(requestedResource, image, targetWidth,
                targetHeight, cover, cropAnchorY, flipHorizontal);
            renderedSource = image;
            renderedWidth = targetWidth;
            renderedHeight = targetHeight;
            renderedCover = cover;
            renderedCropAnchorY = cropAnchorY;
            renderedFlipHorizontal = flipHorizontal;
        }
        Graphics2D imageGraphics = (Graphics2D) graphics.create();
        imageGraphics.setComposite(AlphaComposite.SrcOver.derive(transitionOpacity));
        if (getBorder() instanceof FantasyPortraitBorder) {
            FantasyPortraitBorder frame = (FantasyPortraitBorder) getBorder();
            int left = imageInsets.left;
            int top = imageInsets.top;
            int width = Math.max(1, getWidth() - imageInsets.left - imageInsets.right);
            int height = Math.max(1, getHeight() - imageInsets.top - imageInsets.bottom);
            int arc = frame.viewportArc();
            imageGraphics.clip(new RoundRectangle2D.Double(left, top, width, height,
                arc, arc));
        }
        imageGraphics.drawImage(rendered, imageInsets.left, imageInsets.top, null);
        imageGraphics.dispose();
    }

    boolean usesMaskedFantasyViewportForTest() {
        return getBorder() instanceof FantasyPortraitBorder;
    }
}
