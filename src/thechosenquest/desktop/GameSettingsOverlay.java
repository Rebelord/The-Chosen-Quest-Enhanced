package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.RenderingHints;
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
import javax.swing.JTextArea;

/** Modal in-game settings layer that keeps the game visible beneath a dim scrim. */
final class GameSettingsOverlay extends JPanel {
    private static final long serialVersionUID = 1L;
    private final GameSettingsPanel settings;
    private final GamePreferences preferences;
    private final JPanel modalHost = new JPanel(new BorderLayout());
    private final JLabel transitionLabel = new JLabel("", SwingConstants.CENTER);
    private Runnable modalDismiss;
    private Timer transitionTimer;
    private Timer dialogueTimer;
    private JTextArea dialogueCopy;
    private String fullDialogueText;
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
        stopDialogueTimer();
        settings.setVisible(false);
        modalHost.setVisible(false);
        setVisible(false);
    }

    /**
     * Presents story dialogue inside the game shell. The portrait, speaker
     * identity, speech bubble, and typewriter pacing are shared by friendly
     * NPC consultations and enemy encounter threats.
     */
    void showDialogue(String artworkResource, String speaker, String role,
                      String message, Color accent, final Runnable afterClose) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        stopDialogueTimer();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();

        JPanel card = new JPanel(new BorderLayout(22, 0));
        card.setPreferredSize(new Dimension(760, 390));
        card.setBackground(new Color(22, 20, 24));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 3),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)));

        AssetImagePanel portrait = new AssetImagePanel(artworkResource, true);
        portrait.setPreferredSize(new Dimension(250, 330));
        portrait.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 2),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        card.add(portrait, BorderLayout.WEST);

        JPanel conversation = new JPanel(new BorderLayout(0, 14));
        conversation.setOpaque(false);
        JLabel identity = new JLabel("<html><font color='#fff9e5' size='+2'><b>" +
            speaker + "</b></font><br><font color='#d4af37'>" + role + "</font></html>");
        identity.setFont(UiTheme.display(15));
        conversation.add(identity, BorderLayout.NORTH);

        SpeechBubble bubble = new SpeechBubble(accent);
        bubble.setLayout(new BorderLayout());
        dialogueCopy = new JTextArea();
        dialogueCopy.setEditable(false);
        dialogueCopy.setLineWrap(true);
        dialogueCopy.setWrapStyleWord(true);
        dialogueCopy.setOpaque(false);
        dialogueCopy.setForeground(UiTheme.TEXT);
        dialogueCopy.setFont(UiTheme.body(Font.PLAIN, 18));
        dialogueCopy.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 24));
        bubble.add(dialogueCopy, BorderLayout.CENTER);
        conversation.add(bubble, BorderLayout.CENTER);

        JButton confirm = UiTheme.button("CONTINUE", true);
        confirm.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                if (dialogueTimer != null && dialogueTimer.isRunning()) {
                    completeDialogue();
                } else {
                    closeModal(afterClose);
                }
            }
        });
        confirm.setToolTipText("Finish the message, then close the conversation");
        conversation.add(confirm, BorderLayout.SOUTH);
        card.add(conversation, BorderLayout.CENTER);

        fullDialogueText = message == null ? "" : message;
        dialogueCopy.setText("");
        final int[] character = {0};
        dialogueTimer = new Timer(preferences.isReducedMotion() ? 4 : 22,
            new AbstractAction() {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent event) {
                    int step = preferences.isReducedMotion() ? 5 : 1;
                    character[0] = Math.min(fullDialogueText.length(), character[0] + step);
                    dialogueCopy.setText(fullDialogueText.substring(0, character[0]));
                    if (character[0] >= fullDialogueText.length()) {
                        ((Timer) event.getSource()).stop();
                    }
                }
            });
        modalDismiss = afterClose;
        modalHost.add(card, BorderLayout.CENTER);
        modalHost.setVisible(true);
        setVisible(true);
        revalidate();
        repaint();
        dialogueTimer.start();
        confirm.requestFocusInWindow();
    }

    private void completeDialogue() {
        if (dialogueCopy != null) dialogueCopy.setText(fullDialogueText == null ? "" : fullDialogueText);
        stopDialogueTimer();
    }

    private void stopDialogueTimer() {
        if (dialogueTimer != null && dialogueTimer.isRunning()) dialogueTimer.stop();
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

    void showProgression(GameEngine.ProgressionNotice notice, Runnable inspectCharacter,
                         Runnable continueQuest) {
        String family = notice.weaponFamily == null ? "Current weapon" : notice.weaponFamily;
        String copy = "<html><div style='text-align:center'>" +
            "<font color='#f1c85c' size='+1'><b>LEVEL " + notice.level + " REACHED</b></font><br><br>" +
            "<font color='#fff9e5' size='+2'><b>NEW ABILITY</b></font><br>" +
            "<font color='#c795ff' size='+2'><b>" + notice.abilitySummary + "</b></font><br><br>" +
            "<font color='#d4af37'><b>" + family + " · " +
            GameEngine.proficiencyLabel(notice.proficiencyRank) + "</b></font><br>" +
            "<font color='#bdaed0'>New actions now appear automatically in numbered combat slots.</font>" +
            "</div></html>";
        showGameModal(IconAssets.WEAPON_SWORD, copy, UiTheme.GOLD,
            "VIEW CHARACTER", inspectCharacter, "CONTINUE QUEST", continueQuest);
    }

    private void showGameModal(String artworkResource, String copy, Color accent,
                               String primaryLabel, final Runnable primary,
                               String secondaryLabel, final Runnable secondary) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();

        JPanel card = new JPanel(new BorderLayout(18, 18));
        // Leave enough vertical room for multi-line progression and relic copy
        // so the message never competes with the fixed action row.
        card.setPreferredSize(new Dimension(610, 410));
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
        stopDialogueTimer();
        modalHost.setVisible(false);
        modalHost.removeAll();
        modalDismiss = null;
        setVisible(false);
        if (afterClose != null) afterClose.run();
    }

    boolean dialogueVisibleForTest() { return modalHost.isVisible() && dialogueCopy != null; }
    String dialogueTextForTest() { return dialogueCopy == null ? "" : dialogueCopy.getText(); }
    void completeDialogueForTest() { completeDialogue(); }

    /** Painterly rounded speech bubble with a small portrait-facing tail. */
    private static final class SpeechBubble extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color accent;

        SpeechBubble(Color accent) {
            this.accent = accent;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 13;
            int width = Math.max(0, getWidth() - x - 1);
            g.setColor(new Color(46, 39, 37));
            g.fillRoundRect(x, 1, width, Math.max(0, getHeight() - 3), 24, 24);
            int middle = Math.max(28, getHeight() / 3);
            int[] xs = {x, 0, x};
            int[] ys = {middle - 12, middle, middle + 12};
            g.fillPolygon(xs, ys, 3);
            g.setColor(accent);
            g.drawRoundRect(x, 1, Math.max(0, width - 1),
                Math.max(0, getHeight() - 4), 24, 24);
            g.dispose();
            super.paintComponent(graphics);
        }
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
