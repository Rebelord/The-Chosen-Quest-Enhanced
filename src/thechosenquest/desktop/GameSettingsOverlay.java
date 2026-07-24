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
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.util.List;

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
    private GameMapPanel worldMap;
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
            public void onCredits() { showCredits(true); }
            public void onReleaseNotes() { showReleaseNotes(true, null); }
            public void onFeedback() { ProjectLinks.open(ProjectLinks.FEEDBACK); }
            public void onBugReport() { ProjectLinks.open(ProjectLinks.BUG_REPORT); }
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
        worldMap = null;
        settings.setVisible(false);
        modalHost.setVisible(false);
        setVisible(false);
    }

    /** Opens the release-owned credits without falling back to a system dialog. */
    void showCredits(final boolean returnToSettings) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        stopDialogueTimer();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();
        final Runnable close = new Runnable() {
            public void run() {
                if (returnToSettings) {
                    modalHost.setVisible(false);
                    modalHost.removeAll();
                    showSettings();
                } else {
                    closeModal(null);
                }
            }
        };
        modalDismiss = close;
        modalHost.add(new CreditsPanel(close), BorderLayout.CENTER);
        modalHost.setVisible(true);
        setVisible(true);
        revalidate();
        repaint();
    }

    /** Opens the current tester-release summary as a reusable in-game reference. */
    void showReleaseNotes(final boolean returnToSettings, final Runnable afterClose) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        stopDialogueTimer();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();
        final Runnable close = new Runnable() {
            public void run() {
                if (afterClose != null) afterClose.run();
                if (returnToSettings) {
                    modalHost.setVisible(false);
                    modalHost.removeAll();
                    showSettings();
                } else {
                    closeModal(null);
                }
            }
        };
        modalDismiss = close;
        modalHost.add(new ReleaseNotesPanel(close,
            new Runnable() {
                public void run() { ProjectLinks.open(ProjectLinks.FEEDBACK); }
            },
            new Runnable() {
                public void run() { ProjectLinks.open(ProjectLinks.BUG_REPORT); }
            }), BorderLayout.CENTER);
        modalHost.setVisible(true);
        setVisible(true);
        revalidate();
        repaint();
    }

    /** Opens the complete 13x13 world without exposing undiscovered information. */
    void showWorldMap(GameEngine engine) {
        if (transitionTimer != null && transitionTimer.isRunning()) transitionTimer.stop();
        stopDialogueTimer();
        settings.setVisible(false);
        transitionLabel.setVisible(false);
        modalHost.removeAll();

        int availableWidth = getWidth() > 0 ? getWidth() : UiTheme.SHELL_WIDTH;
        int availableHeight = getHeight() > 0 ? getHeight() : UiTheme.SHELL_HEIGHT;
        int cardWidth = Math.max(760, availableWidth - 100);
        int cardHeight = Math.max(600, availableHeight - 80);
        int mapSide = Math.max(480, Math.min(cardHeight - 130, cardWidth - 330));

        JPanel card = new JPanel(new BorderLayout(22, 16));
        card.setPreferredSize(new Dimension(cardWidth, cardHeight));
        card.setBackground(UiTheme.SURFACE_DEEP);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.GOLD, 2),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("<html><font color='#d4af37' size='+2'>" +
            "<b>WORLD MAP</b></font><br><font color='#a69987'>" +
            "Discovered terrain, known threats, and recorded clues</font></html>");
        title.setFont(UiTheme.body(Font.PLAIN, 13));
        heading.add(title, BorderLayout.WEST);
        JButton close = UiTheme.button("CLOSE  [M]", false);
        close.setToolTipText("Close the world map · M or Escape");
        close.getAccessibleContext().setAccessibleName("Close world map");
        close.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) { closeWorldMap(); }
        });
        heading.add(close, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel mapHolder = new JPanel(new GridBagLayout());
        mapHolder.setOpaque(false);
        worldMap = new GameMapPanel(engine);
        worldMap.showFullWorld();
        worldMap.setPreferredSize(new Dimension(mapSide, mapSide));
        worldMap.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        mapHolder.add(worldMap);
        card.add(mapHolder, BorderLayout.CENTER);

        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setOpaque(false);
        legend.setPreferredSize(new Dimension(310, 0));
        legend.add(mapInfoCard("ACTIVE QUEST", activeQuestCopy(engine)));
        legend.add(Box.createVerticalStrut(10));
        legend.add(mapInfoCard("OBJECTIVES", objectiveCopy(engine)));
        legend.add(Box.createVerticalStrut(10));
        legend.add(mapInfoCard("RUMORS & CLUES", rumorCopy(engine)));
        legend.add(Box.createVerticalStrut(10));
        legend.add(mapInfoCard("JOURNEY STATUS", journeyStatusCopy(engine)));
        legend.add(Box.createVerticalStrut(10));
        legend.add(buildMapLegend());
        legend.add(Box.createVerticalGlue());
        JLabel help = new JLabel("<html><font color='#a69987'>" +
            "Hover tiles for details · <b>M</b> or <b>Esc</b> closes</font></html>");
        help.setFont(UiTheme.body(Font.PLAIN, 12));
        help.setAlignmentX(LEFT_ALIGNMENT);
        legend.add(help);
        card.add(legend, BorderLayout.EAST);

        modalDismiss = null;
        modalHost.add(card, BorderLayout.CENTER);
        modalHost.setVisible(true);
        setVisible(true);
        revalidate();
        repaint();
        close.requestFocusInWindow();
    }

    private JPanel buildMapLegend() {
        JPanel panel = mapInfoPanel();
        panel.add(mapLegendTitle("MAP LEGEND"));
        panel.add(Box.createVerticalStrut(8));
        JPanel items = new JPanel(new java.awt.GridLayout(4, 2, 8, 8));
        items.setOpaque(false);
        items.setAlignmentX(LEFT_ALIGNMENT);
        items.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        items.add(mapLegendItem("●", UiTheme.GOLD, "Hero"));
        items.add(mapLegendItem("◆", UiTheme.MAP_WARNING, "Enemy"));
        items.add(mapLegendItem("!", UiTheme.DANGER, "Rumor"));
        items.add(mapLegendItem("◆", UiTheme.QUALITY_RELIC, "Relic"));
        items.add(mapLegendItem("▦", UiTheme.MUTED, "Fog"));
        items.add(mapLegendItem("◐", new Color(175, 185, 196), "Scouted"));
        items.add(mapLegendItem("⌖", new Color(177, 136, 71), "Landmark"));
        items.add(mapLegendItem("◎", UiTheme.GOLD_LIGHT, "Visited"));
        panel.add(items);
        return panel;
    }

    private JPanel mapInfoCard(String title, String htmlCopy) {
        JPanel panel = mapInfoPanel();
        panel.add(mapLegendTitle(title));
        panel.add(Box.createVerticalStrut(7));
        JLabel copy = new JLabel("<html>" + htmlCopy + "</html>");
        copy.setForeground(UiTheme.TEXT);
        copy.setFont(UiTheme.body(Font.PLAIN, 12));
        copy.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(copy);
        return panel;
    }

    private JPanel mapInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTheme.SURFACE);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return panel;
    }

    private String activeQuestCopy(GameEngine engine) {
        GameEngine.State state = engine.getState();
        if (state.won) {
            return "<font color='#6fce78'><b>QUEST COMPLETE</b></font><br>" +
                "The dragon has fallen and the frontier is safe.";
        }
        boolean lairKnown = engine.discoveryAt(state.dragonLairRow, state.dragonLairCol) !=
            GameEngine.DiscoveryState.UNKNOWN ||
            engine.threatKnowledgeAt(state.dragonLairRow, state.dragonLairCol) > 0;
        String destination = lairKnown
            ? coordinate(state.dragonLairRow, state.dragonLairCol)
            : "location unknown";
        return "<font color='#f1e5ac'><b>Defeat the Shadow Dragon</b></font><br>" +
            "Prepare with elite relics and survive the final battle.<br>" +
            "<font color='#a69987'>Lair: " + destination + "</font>";
    }

    private String objectiveCopy(GameEngine engine) {
        GameEngine.State state = engine.getState();
        int recovered = state.relics == null ? 0 : state.relics.size();
        int identified = engine.identifiedRelicCount();
        boolean nestKnown = engine.discoveryAt(state.spiderNestRow, state.spiderNestCol) !=
            GameEngine.DiscoveryState.UNKNOWN || state.spiderNestCleared;
        StringBuilder copy = new StringBuilder();
        copy.append(objectiveLine(state.level >= 3,
            "Reach level 3", "Level " + state.level));
        copy.append(objectiveLine(recovered > 0,
            "Recover elite relics", recovered + " recovered"));
        copy.append(objectiveLine(recovered > 0 && identified == recovered,
            "Identify recovered relics", identified + " identified"));
        if (nestKnown) {
            copy.append(objectiveLine(state.spiderNestCleared,
                "Clear the Ashweb Nest", state.spiderNestCleared
                    ? "Route secured" : state.spiderNestRemaining + " brood remain"));
        }
        return copy.toString();
    }

    private String objectiveLine(boolean complete, String objective, String status) {
        return "<font color='" + (complete ? "#6fce78" : "#d4af37") + "'><b>" +
            (complete ? "✓" : "◆") + "</b></font> " + objective +
            " <font color='#a69987'>· " + status + "</font><br>";
    }

    private String rumorCopy(GameEngine engine) {
        List<String> journal = engine.mapJournal();
        if (journal.isEmpty()) {
            return "<font color='#a69987'>No rumors recorded yet.<br>" +
                "Ask an innkeeper or alchemist for guidance.</font>";
        }
        StringBuilder copy = new StringBuilder();
        int first = Math.max(0, journal.size() - 3);
        for (int index = journal.size() - 1; index >= first; index--) {
            copy.append("<font color='#c795ff'>◆</font> ")
                .append(journal.get(index)).append("<br>");
        }
        return copy.toString();
    }

    private String journeyStatusCopy(GameEngine engine) {
        GameEngine.State state = engine.getState();
        int charted = 0;
        int visited = 0;
        int knownThreats = 0;
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
                if (discovery != GameEngine.DiscoveryState.UNKNOWN) charted++;
                if (discovery == GameEngine.DiscoveryState.VISITED) visited++;
                if (state.enemies[row][col] != null &&
                        engine.threatKnowledgeAt(row, col) > 0) knownThreats++;
            }
        }
        int percent = (int) Math.round(charted * 100d /
            (GameEngine.SIZE * GameEngine.SIZE));
        return "<font color='#f1e5ac'><b>" + state.playerName + "</b></font> · Level " +
            state.level + "<br>Position: " + coordinate(state.row, state.col) +
            " · " + engine.currentTile().label + "<br>Charted: " + percent +
            "% · " + visited + " visited<br>Known threats: " + knownThreats +
            " · Relics: " + engine.identifiedRelicCount() +
            (state.regionalMapOwned
                ? "<br><font color='#6fce78'>Regional map acquired</font>"
                : "<br><font color='#a69987'>Regional map not acquired</font>");
    }

    private static String coordinate(int row, int col) {
        return Character.toString((char) ('A' + row)) + (col + 1);
    }

    private JLabel mapLegendTitle(String copy) {
        JLabel label = new JLabel(copy);
        label.setForeground(UiTheme.GOLD_LIGHT);
        label.setFont(UiTheme.body(Font.BOLD, 12));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel mapLegendItem(String symbol, Color color, String copy) {
        JLabel label = new JLabel("<html><font color='" + colorHex(color) +
            "'><b>" + symbol + "</b></font>&nbsp;&nbsp;" + copy + "</html>");
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.body(Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String colorHex(Color color) {
        return String.format("#%02x%02x%02x",
            color.getRed(), color.getGreen(), color.getBlue());
    }

    void closeWorldMap() {
        if (worldMap == null) return;
        worldMap = null;
        closeModal(null);
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
        worldMap = null;
        modalHost.setVisible(false);
        modalHost.removeAll();
        modalDismiss = null;
        setVisible(false);
        if (afterClose != null) afterClose.run();
    }

    boolean dialogueVisibleForTest() { return modalHost.isVisible() && dialogueCopy != null; }
    String dialogueTextForTest() { return dialogueCopy == null ? "" : dialogueCopy.getText(); }
    void completeDialogueForTest() { completeDialogue(); }
    boolean creditsVisibleForTest() {
        return modalHost.isVisible() && modalHost.getComponentCount() == 1 &&
            modalHost.getComponent(0) instanceof CreditsPanel;
    }
    boolean releaseNotesVisibleForTest() {
        return modalHost.isVisible() && modalHost.getComponentCount() == 1 &&
            modalHost.getComponent(0) instanceof ReleaseNotesPanel;
    }
    boolean worldMapVisibleForTest() {
        return worldMap != null && modalHost.isVisible() && isVisible();
    }
    int worldMapViewSizeForTest() {
        return worldMap == null ? 0 : worldMap.viewSizeForTest();
    }

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
