package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** Modal in-game settings layer that keeps the game visible beneath a dim scrim. */
final class GameSettingsOverlay extends JPanel {
    private static final long serialVersionUID = 1L;
    private final GameSettingsPanel settings;
    private final GamePreferences preferences;
    private final JPanel modalHost = new JPanel(new BorderLayout());
    private final JLabel transitionLabel = new JLabel("", SwingConstants.CENTER);
    private Runnable modalDismiss;
    private Timer transitionTimer;
    private Color transitionColor = Color.BLACK;
    private float transitionOpacity;

    GameSettingsOverlay(SoundManager soundManager, GamePreferences preferences,
                        final Runnable onApplied) {
        this.preferences = preferences;
        setLayout(new GridBagLayout());
        setOpaque(false);
        setFocusCycleRoot(true);
        settings = new GameSettingsPanel(soundManager, preferences,
            new GameSettingsPanel.Listener() {
            public void onCancel() { hideSettings(); }
            public void onApplied() {
                hideSettings();
                if (onApplied != null) onApplied.run();
            }
        });
        GridBagConstraints centered = new GridBagConstraints();
        centered.gridx = 0;
        centered.gridy = 0;
        add(settings, centered);
        modalHost.setOpaque(false);
        modalHost.setVisible(false);
        add(modalHost, centered);
        transitionLabel.setForeground(UiTheme.GOLD_LIGHT);
        transitionLabel.setFont(UiTheme.title(java.awt.Font.BOLD, 32));
        transitionLabel.setVisible(false);
        add(transitionLabel, centered);
        addMouseListener(new MouseAdapter() { });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ESCAPE"), "closeSettings");
        getActionMap().put("closeSettings", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                if (modalHost.isVisible()) closeModal(modalDismiss);
                else hideSettings();
            }
        });
        setVisible(false);
    }

    void showSettings() {
        modalHost.setVisible(false);
        transitionLabel.setVisible(false);
        settings.setVisible(true);
        settings.refreshFromManager();
        setVisible(true);
        settings.requestInitialFocus();
    }

    void hideSettings() {
        settings.setVisible(false);
        modalHost.setVisible(false);
        setVisible(false);
    }

    void showRelicDiscovery(GameEngine.Relic relic, Runnable inspectInventory,
                            Runnable continueQuest) {
        String copy = "<html><div style='text-align:center'>" +
            "<font color='#f1c85c' size='+1'><b>UNIDENTIFIED RELIC DISCOVERED</b></font><br><br>" +
            "<font color='#c795ff' size='+3'><b>" + relic.name + "</b></font><br>" +
            "<font color='#bdaed0'>Recovered from " + relic.source + "</font><br><br>" +
            "<font color='#fff9e5'>Bring this relic to the <b>" + relic.vendor +
            "</b> for free identification.<br>Its reward will attune itself to your class.</font>" +
            "</div></html>";
        showGameModal(IconAssets.UNIDENTIFIED_RELIC, copy, UiTheme.QUALITY_RELIC,
            "INSPECT IN INVENTORY", inspectInventory, "CONTINUE QUEST", continueQuest);
    }

    void showAutoEquipped(GameEngine.Item item, String previousName, int previousValue,
                          Runnable inspectInventory, Runnable continueQuest) {
        String stat = "Weapon".equals(item.type) ? "ATK" : "DEF";
        String previous = previousName == null ? "None" : previousName;
        String copy = "<html><div style='text-align:center'>" +
            "<font color='#f1c85c' size='+1'><b>RELIC ATTUNED</b></font><br><br>" +
            "<font color='#c795ff' size='+3'><b>" + item.name + "</b></font><br>" +
            "<font color='#c795ff'><b>[RELIC]</b></font> &nbsp; " +
            "<font color='#6fce78'><b>AUTO-EQUIPPED</b></font><br><br>" +
            "<font color='#bdaed0'>" + previous + " (" + previousValue + " " + stat + ")</font>" +
            " <font color='#f1c85c'>→</font> " +
            "<font color='#fff9e5'><b>" + item.name + " (" + GameEngine.itemStat(item) +
            " " + stat + ")</b></font><br>" +
            "<font color='#6fce78'><b>▲ +" + (GameEngine.itemStat(item) - previousValue) +
            " upgrade</b></font>" +
            "</div></html>";
        showGameModal(IconAssets.itemResource(item), copy, UiTheme.QUALITY_RELIC,
            "VIEW IN INVENTORY", inspectInventory, "CONTINUE QUEST", continueQuest);
    }

    private void showGameModal(String artworkResource, String copy, Color accent,
                               String primaryLabel, final Runnable primary,
                               String secondaryLabel, final Runnable secondary) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();

        JPanel card = new JPanel(new BorderLayout(18, 18));
        card.setPreferredSize(new Dimension(610, 370));
        card.setBackground(UiTheme.RELIC_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 3),
            BorderFactory.createEmptyBorder(28, 32, 26, 32)));
        // Keep important loot tied to the same artwork resolver used by
        // inventory and shops instead of falling back to a system glyph.
        JLabel icon = new JLabel("", SwingConstants.CENTER);
        icon.setIcon(IconAssets.icon(artworkResource, 96));
        icon.setForeground(UiTheme.GOLD_LIGHT);
        card.add(icon, BorderLayout.NORTH);
        JLabel message = new JLabel(copy, SwingConstants.CENTER);
        message.setFont(UiTheme.body(Font.PLAIN, 13));
        card.add(message, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton inspect = UiTheme.button(primaryLabel, true);
        inspect.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) { closeModal(primary); }
        });
        JButton continueButton = UiTheme.button(secondaryLabel, false);
        continueButton.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) { closeModal(secondary); }
        });
        actions.add(inspect);
        actions.add(continueButton);
        card.add(actions, BorderLayout.SOUTH);

        modalDismiss = secondary;
        modalHost.add(card, BorderLayout.CENTER);
        modalHost.setVisible(true);
        setVisible(true);
        revalidate();
        repaint();
        inspect.requestFocusInWindow();
    }

    private void closeModal(Runnable afterClose) {
        modalHost.setVisible(false);
        modalHost.removeAll();
        modalDismiss = null;
        setVisible(false);
        if (afterClose != null) afterClose.run();
    }

    void playTransition(String label, Color color, int duration, final Runnable midpoint) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        settings.setVisible(false);
        modalHost.setVisible(false);
        transitionColor = color == null ? Color.BLACK : color;
        transitionLabel.setText(label == null ? "" : label);
        transitionLabel.setVisible(label != null && label.length() > 0 &&
            !preferences.isReducedMotion());
        transitionOpacity = 0.0f;
        setVisible(true);

        final int actualDuration = preferences.isReducedMotion() ? 120 : Math.max(180, duration);
        final long started = System.currentTimeMillis();
        final boolean[] switched = new boolean[] { false };
        transitionTimer = new Timer(16, null);
        transitionTimer.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                float progress = Math.min(1.0f,
                    (System.currentTimeMillis() - started) / (float) actualDuration);
                if (!switched[0] && progress >= 0.5f) {
                    switched[0] = true;
                    if (midpoint != null) midpoint.run();
                }
                float triangle = progress <= 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
                transitionOpacity = triangle * (preferences.isReducedMotion() ? 0.72f : 0.92f);
                repaint();
                if (progress >= 1.0f) {
                    transitionTimer.stop();
                    transitionOpacity = 0.0f;
                    transitionLabel.setVisible(false);
                    setVisible(false);
                }
            }
        });
        transitionTimer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (settings.isVisible() || modalHost.isVisible()) {
            graphics.setColor(new Color(0, 0, 0, 176));
        } else {
            int alpha = Math.max(0, Math.min(255, Math.round(255 * transitionOpacity)));
            graphics.setColor(new Color(transitionColor.getRed(), transitionColor.getGreen(),
                transitionColor.getBlue(), alpha));
        }
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }
}
