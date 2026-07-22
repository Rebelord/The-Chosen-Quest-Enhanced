package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Persistent three-column game shell: hero rail, active center view, and map.
 * Auxiliary encounter, inventory, and location panels are swapped only in the
 * center so global navigation and player status remain stable.
 */
final class EnhancedExplorationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color PARCHMENT = new Color(244, 228, 188);
    private static final Color PARCHMENT_INK = new Color(42, 27, 14);

    interface Listener {
        void onInventory();
        void onSpellbook();
        void onShop();
        void onHaven();
        void onSave();
        void onLoad();
        void onNewQuest();
        void onSettings();
        boolean onToggleMute();
        void onSound(SoundManager.Cue cue);
        void onStateChanged();
    }

    private final GameEngine engine;
    private final Listener listener;
    private final GameMapPanel map;
    private final AssetImagePanel heroPortrait = new AssetImagePanel(null, true);
    private final JLabel heroName = new JLabel();
    private final JLabel heroIdentity = new JLabel();
    private final JLabel heroStatus = new JLabel();
    private final JLabel heroStats = new JLabel();
    private final JLabel weapon = new JLabel();
    private final JLabel armour = new JLabel();
    private final JLabel offhand = new JLabel();
    private EquipmentRow weaponEquipment;
    private EquipmentRow armourEquipment;
    private EquipmentRow offhandEquipment;
    private final JProgressBar health = meter(UiTheme.HEALTH);
    private final JProgressBar mana = meter(UiTheme.BLUE);
    private final JProgressBar experience = meter(new Color(255, 191, 0));
    private final JLabel headerLocation = new JLabel();
    private final AssetImagePanel sceneImage =
        new AssetImagePanel("/assets/scenes/alshira-ruins.png", true);
    private final JLabel sceneTitle = new JLabel();
    private final JTextArea sceneDescription = new JTextArea();
    private final JTextArea eventLog = new JTextArea();
    private final JLabel mapLocation = new JLabel();
    private final JLabel mapTerritory = new JLabel();
    private final JTextArea mapDescription = new JTextArea();
    private final JButton primaryAction = UiTheme.button("INVESTIGATE", true);
    private final JButton secondaryAction = UiTheme.button("CONTINUE JOURNEY", false);
    private final JButton tertiaryAction = UiTheme.button("REST", false);
    private final JButton spellbookAction = UiTheme.button("SPELLBOOK", false);
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerStage = new JPanel(centerLayout);
    private JPanel headerPanel;
    private JPanel heroRail;
    private JPanel mapRail;
    private JButton soundButton;
    private JButton northTravelButton;
    private JButton southTravelButton;
    private JButton westTravelButton;
    private JButton eastTravelButton;

    private static final String STORY = "story";
    private static final String ENCOUNTER = "encounter";
    private static final String INVENTORY = "inventory";
    private static final String LOCATION = "location";
    private static final String OUTCOME = "outcome";

    EnhancedExplorationPanel(GameEngine engine, Listener listener) {
        this.engine = engine;
        this.listener = listener;
        this.map = new GameMapPanel(engine);
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        headerPanel = buildHeader();
        add(headerPanel, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(UiTheme.SURFACE_DEEP);
        header.setPreferredSize(new Dimension(0, UiTheme.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(166, 124, 0)),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)));

        JLabel title = new JLabel("The Chosen Quest",
            new SystemIcon(SystemIcon.Type.STAR, 20, UiTheme.GOLD), SwingConstants.LEFT);
        title.setIconTextGap(12);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.title(Font.PLAIN, 18));
        header.add(title, BorderLayout.WEST);

        JPanel journey = new JPanel(new GridLayout(1, 2, 24, 0));
        journey.setOpaque(false);
        journey.add(headerDatum("CURRENT QUEST", "Investigate the strange ruins"));
        headerLocation.setForeground(UiTheme.TEXT);
        headerLocation.setFont(UiTheme.display(14));
        JPanel locationDatum = new JPanel(new BorderLayout(0, 2));
        locationDatum.setOpaque(false);
        JLabel locationLabel = new JLabel("LOCATION");
        locationLabel.setForeground(UiTheme.GOLD);
        locationLabel.setFont(UiTheme.body(Font.BOLD, 10));
        locationDatum.add(locationLabel, BorderLayout.NORTH);
        locationDatum.add(headerLocation, BorderLayout.CENTER);
        journey.add(locationDatum);
        header.add(journey, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton save = compactButton("SAVE");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CONFIRM);
                listener.onSave();
            }
        });
        JButton load = compactButton("LOAD");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CONFIRM);
                listener.onLoad();
            }
        });
        JButton menu = compactButton("MENU");
        menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CANCEL);
                listener.onNewQuest();
            }
        });
        soundButton = systemButton(SystemIcon.Type.SPEAKER_ON, "Mute sound");
        soundButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setMuted(listener.onToggleMute());
            }
        });
        JButton settings = systemButton(SystemIcon.Type.SETTINGS, "Game settings");
        settings.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CONFIRM);
                listener.onSettings();
            }
        });
        actions.add(save);
        actions.add(load);
        actions.add(menu);
        actions.add(soundButton);
        actions.add(settings);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel headerDatum(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        JLabel heading = new JLabel(label);
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.body(Font.BOLD, 10));
        JLabel copy = new JLabel(value);
        copy.setForeground(UiTheme.TEXT);
        copy.setFont(UiTheme.display(14));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(copy, BorderLayout.CENTER);
        return panel;
    }

    private JButton compactButton(String text) {
        JButton button = UiTheme.button(text, false);
        button.setFont(UiTheme.body(Font.BOLD, 10));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        return button;
    }

    private JButton systemButton(SystemIcon.Type type, String tooltip) {
        JButton button = new JButton(new SystemIcon(type, 18, UiTheme.GOLD));
        button.setPreferredSize(new Dimension(40, 40));
        button.setToolTipText(tooltip);
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.ICON, 7, 9);
        return button;
    }

    void setMuted(boolean muted) {
        if (soundButton == null) return;
        soundButton.setIcon(new SystemIcon(muted ? SystemIcon.Type.SPEAKER_MUTED :
            SystemIcon.Type.SPEAKER_ON, 18, muted ? UiTheme.MUTED : UiTheme.GOLD));
        soundButton.setToolTipText(muted ? "Unmute sound" : "Mute sound");
        soundButton.getAccessibleContext().setAccessibleName(
            muted ? "Unmute sound" : "Mute sound");
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UiTheme.BACKGROUND);
        heroRail = buildHeroRail();
        body.add(heroRail, BorderLayout.WEST);
        centerStage.setBackground(PARCHMENT);
        centerStage.setPreferredSize(new Dimension(UiTheme.CENTER_WIDTH, UiTheme.BODY_HEIGHT));
        centerStage.add(buildScene(), STORY);
        body.add(centerStage, BorderLayout.CENTER);
        mapRail = buildMapRail();
        body.add(mapRail, BorderLayout.EAST);
        return body;
    }

    void setAuxiliaryPanels(EncounterPanel encounter, InventoryPanel inventory,
                            LocationPanel location, OutcomePanel outcome) {
        centerStage.add(encounter, ENCOUNTER);
        centerStage.add(inventory, INVENTORY);
        centerStage.add(location, LOCATION);
        centerStage.add(outcome, OUTCOME);
    }

    void showStory() { showCenter(STORY, "Exploring", UiTheme.GREEN); }
    void showEncounter() { showCenter(ENCOUNTER, "In Combat", new Color(178, 58, 48)); }
    void showInventory() { showCenter(INVENTORY, "Managing Equipment", UiTheme.GOLD); }
    void showLocation() { showCenter(LOCATION, "Trading & Resting", UiTheme.BLUE); }
    void showOutcome() { showCenter(OUTCOME, "Quest Resolved", UiTheme.GOLD); }

    private void showCenter(String card, String status, Color color) {
        heroStatus.setText(status);
        heroStatus.setForeground(color);
        headerPanel.setVisible(!OUTCOME.equals(card));
        heroRail.setVisible(!OUTCOME.equals(card));
        mapRail.setVisible(!(INVENTORY.equals(card) || OUTCOME.equals(card)));
        centerLayout.show(centerStage, card);
    }

    private JPanel buildHeroRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(UiTheme.SURFACE_DEEP);
        rail.setPreferredSize(new Dimension(UiTheme.HERO_RAIL_WIDTH, 0));
        rail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(166, 124, 0)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        heroPortrait.setPreferredSize(new Dimension(240, 240));
        heroPortrait.setMinimumSize(new Dimension(240, 210));
        heroPortrait.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        heroPortrait.setAlignmentX(LEFT_ALIGNMENT);
        heroPortrait.setBorder(new FantasyPortraitBorder("Human", "Fighter"));
        rail.add(heroPortrait);
        rail.add(Box.createVerticalStrut(14));

        heroName.setForeground(UiTheme.GOLD);
        heroName.setFont(UiTheme.display(24));
        heroName.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(heroName);
        heroIdentity.setForeground(UiTheme.TEXT);
        heroIdentity.setFont(UiTheme.body(Font.BOLD, 12));
        heroIdentity.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(heroIdentity);
        heroStatus.setText("Exploring");
        heroStatus.setForeground(UiTheme.GREEN);
        heroStatus.setFont(UiTheme.display(12));
        heroStatus.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(heroStatus);
        rail.add(Box.createVerticalStrut(18));

        rail.add(health);
        rail.add(Box.createVerticalStrut(8));
        rail.add(mana);
        rail.add(Box.createVerticalStrut(8));
        rail.add(experience);
        rail.add(Box.createVerticalStrut(18));

        rail.add(sectionLabel("EQUIPMENT"));
        weapon.setAlignmentX(LEFT_ALIGNMENT);
        armour.setAlignmentX(LEFT_ALIGNMENT);
        offhand.setAlignmentX(LEFT_ALIGNMENT);
        weaponEquipment = equipmentRow(weapon, IconAssets.WEAPON_SWORD);
        rail.add(weaponEquipment);
        rail.add(Box.createVerticalStrut(8));
        armourEquipment = equipmentRow(armour, IconAssets.ARMOUR_SHIELD);
        rail.add(armourEquipment);
        rail.add(Box.createVerticalStrut(8));
        offhandEquipment = equipmentRow(offhand, IconAssets.ARMOUR_SHIELD);
        rail.add(offhandEquipment);
        rail.add(Box.createVerticalStrut(18));

        rail.add(sectionLabel("STATISTICS"));
        heroStats.setForeground(UiTheme.TEXT);
        heroStats.setFont(UiTheme.body(Font.BOLD, 12));
        heroStats.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(heroStats);
        rail.add(Box.createVerticalGlue());

        JButton inventory = UiTheme.button("INVENTORY", false);
        inventory.setAlignmentX(LEFT_ALIGNMENT);
        inventory.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        inventory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CONFIRM);
                listener.onInventory();
            }
        });
        rail.add(inventory);
        return rail;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.GOLD);
        label.setFont(UiTheme.body(Font.BOLD, 10));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return label;
    }

    private EquipmentRow equipmentRow(JLabel value, String iconResource) {
        return new EquipmentRow(value, iconResource);
    }

    private JPanel buildScene() {
        JPanel scene = new JPanel(new BorderLayout(0, 18));
        scene.setBackground(PARCHMENT);
        scene.setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));

        JPanel narrative = new JPanel();
        narrative.setLayout(new BoxLayout(narrative, BoxLayout.Y_AXIS));
        narrative.setOpaque(false);
        sceneImage.setPreferredSize(new Dimension(700, 300));
        sceneImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        sceneImage.setAlignmentX(LEFT_ALIGNMENT);
        sceneImage.setBorder(BorderFactory.createLineBorder(new Color(42, 27, 14, 80)));
        narrative.add(sceneImage);
        narrative.add(Box.createVerticalStrut(14));

        sceneTitle.setForeground(PARCHMENT_INK);
        sceneTitle.setFont(UiTheme.display(30));
        sceneTitle.setAlignmentX(LEFT_ALIGNMENT);
        narrative.add(sceneTitle);
        narrative.add(Box.createVerticalStrut(6));

        sceneDescription.setEditable(false);
        sceneDescription.setLineWrap(true);
        sceneDescription.setWrapStyleWord(true);
        sceneDescription.setOpaque(false);
        sceneDescription.setForeground(PARCHMENT_INK);
        sceneDescription.setFont(UiTheme.display(17));
        sceneDescription.setRows(3);
        sceneDescription.setAlignmentX(LEFT_ALIGNMENT);
        narrative.add(sceneDescription);
        scene.add(narrative, BorderLayout.NORTH);

        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setBackground(new Color(239, 222, 181));
        eventLog.setForeground(PARCHMENT_INK);
        eventLog.setFont(UiTheme.display(15));
        eventLog.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(42, 27, 14, 100)));
        logScroll.getViewport().setBackground(new Color(239, 222, 181));
        scene.add(logScroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        primaryAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { performPrimaryAction(); }
        });
        secondaryAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { performSecondaryAction(); }
        });
        tertiaryAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { performTertiaryAction(); }
        });
        spellbookAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onSound(SoundManager.Cue.UI_CONFIRM);
                listener.onSpellbook();
            }
        });
        actions.add(primaryAction);
        actions.add(tertiaryAction);
        actions.add(secondaryAction);
        actions.add(spellbookAction);
        controls.add(actions, BorderLayout.CENTER);
        scene.add(controls, BorderLayout.SOUTH);
        return scene;
    }

    private JPanel buildTravelControls() {
        JPanel travel = new JPanel();
        travel.setLayout(new BoxLayout(travel, BoxLayout.Y_AXIS));
        travel.setOpaque(false);
        // The wrapper spans the rail; only its inner keyboard is centered. This
        // prevents a centered narrow child from constraining sibling widths.
        travel.setAlignmentX(LEFT_ALIGNMENT);
        travel.setPreferredSize(new Dimension(UiTheme.MAP_RAIL_WIDTH - 32, 112));
        travel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));

        JLabel title = new JLabel("TRAVEL");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.body(Font.BOLD, 11));
        title.setAlignmentX(CENTER_ALIGNMENT);
        travel.add(title);
        travel.add(Box.createVerticalStrut(8));

        // Arrow keys mirror a physical keyboard so the on-screen affordance
        // teaches the global movement shortcuts without additional copy.
        JPanel directions = new JPanel(new GridLayout(2, 3, 6, 6));
        directions.setOpaque(false);
        directions.setAlignmentX(CENTER_ALIGNMENT);
        directions.setPreferredSize(new Dimension(180, 82));
        directions.setMaximumSize(new Dimension(180, 82));
        directions.add(Box.createGlue());
        northTravelButton = addDirection(directions, "↑", "Move north", -1, 0);
        directions.add(Box.createGlue());
        westTravelButton = addDirection(directions, "←", "Move west", 0, -1);
        southTravelButton = addDirection(directions, "↓", "Move south", 1, 0);
        eastTravelButton = addDirection(directions, "→", "Move east", 0, 1);
        travel.add(directions);
        return travel;
    }

    private JButton addDirection(JPanel panel, String text, String accessibleName,
                                 final int rowDelta, final int colDelta) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.body(Font.BOLD, 16));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.KEY, 0, 0);
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                GameEngine.State before = engine.getState();
                int row = before.row;
                int col = before.col;
                engine.move(rowDelta, colDelta);
                GameEngine.State after = engine.getState();
                listener.onSound(after.row != row || after.col != col ?
                    SoundManager.Cue.MOVE : SoundManager.Cue.ERROR);
                listener.onStateChanged();
            }
        });
        panel.add(button);
        return button;
    }

    /** Gives physical arrow-key input the same pressed feedback as a mouse click. */
    void triggerMovementShortcut(int rowDelta, int colDelta) {
        final JButton button = rowDelta < 0 ? northTravelButton :
            (rowDelta > 0 ? southTravelButton :
                (colDelta < 0 ? westTravelButton : eastTravelButton));
        if (button == null || !button.isEnabled()) return;
        button.getModel().setArmed(true);
        button.getModel().setPressed(true);
        Timer release = new Timer(90, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                button.getModel().setPressed(false);
                button.getModel().setArmed(false);
            }
        });
        release.setRepeats(false);
        release.start();
    }

    private JPanel buildMapRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(UiTheme.SURFACE_DEEP);
        rail.setPreferredSize(new Dimension(UiTheme.MAP_RAIL_WIDTH, 0));
        rail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, UiTheme.BORDER),
            BorderFactory.createEmptyBorder(18, 16, 18, 16)));

        JLabel heading = new JLabel("◉  WORLD MAP");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.body(Font.BOLD, 11));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(heading);
        rail.add(Box.createVerticalStrut(14));

        map.setPreferredSize(new Dimension(UiTheme.MAP_RAIL_WIDTH - 32, 300));
        map.setMaximumSize(new Dimension(UiTheme.MAP_RAIL_WIDTH - 32, 300));
        map.setAlignmentX(LEFT_ALIGNMENT);
        map.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        rail.add(map);
        rail.add(Box.createVerticalStrut(14));

        mapLocation.setForeground(new Color(227, 213, 191));
        mapLocation.setFont(UiTheme.body(Font.BOLD, 12));
        mapLocation.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(mapLocation);
        rail.add(Box.createVerticalStrut(4));
        mapTerritory.setForeground(UiTheme.GOLD);
        mapTerritory.setFont(UiTheme.body(Font.PLAIN, 11));
        mapTerritory.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(mapTerritory);
        rail.add(Box.createVerticalStrut(6));

        mapDescription.setEditable(false);
        mapDescription.setLineWrap(true);
        mapDescription.setWrapStyleWord(true);
        mapDescription.setOpaque(false);
        mapDescription.setForeground(UiTheme.MUTED);
        mapDescription.setFont(UiTheme.body(Font.PLAIN, 11));
        mapDescription.setRows(4);
        mapDescription.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        mapDescription.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(mapDescription);
        rail.add(Box.createVerticalStrut(16));
        rail.add(newLegend());
        rail.add(Box.createVerticalGlue());
        rail.add(Box.createVerticalStrut(10));
        rail.add(buildTravelControls());
        return rail;
    }

    private JPanel newLegend() {
        JPanel legend = new JPanel(new BorderLayout(0, 9));
        legend.setOpaque(false);
        legend.setAlignmentX(LEFT_ALIGNMENT);
        legend.setPreferredSize(new Dimension(UiTheme.MAP_RAIL_WIDTH - 32, 196));
        legend.setMaximumSize(new Dimension(Integer.MAX_VALUE, 196));
        legend.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(77, 63, 51, 100)),
            BorderFactory.createEmptyBorder(12, 0, 0, 0)));

        JLabel title = new JLabel("MAP LEGEND");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.body(Font.BOLD, 12));
        legend.add(title, BorderLayout.NORTH);

        JPanel entries = new JPanel(new GridLayout(4, 2, 8, 7));
        entries.setOpaque(false);
        entries.add(legendRow("●", UiTheme.GOLD, "Hero mini"));
        entries.add(legendRow("●", new Color(190, 85, 60), "Known enemy"));
        entries.add(legendRow("!", new Color(224, 79, 60), "Rumored threat"));
        entries.add(legendRow("◆", UiTheme.QUALITY_RELIC, "Relic region"));
        entries.add(legendRow("?", new Color(107, 111, 123), "Unknown"));
        entries.add(legendRow("◐", new Color(141, 151, 159), "Scouted"));
        entries.add(legendRow("◉", UiTheme.GOLD_LIGHT, "Visited"));
        entries.add(legendRow("⌖", new Color(177, 136, 71), "Landmark"));
        legend.add(entries, BorderLayout.CENTER);
        return legend;
    }

    private JPanel legendRow(String symbol, Color color, String text) {
        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setOpaque(true);
        row.setBackground(new Color(31, 24, 21));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(77, 63, 51, 110)),
            BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        JLabel dot = new JLabel(symbol);
        dot.setForeground(color);
        dot.setFont(UiTheme.body(Font.BOLD, 17));
        dot.setPreferredSize(new Dimension(19, 22));
        dot.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel label = new JLabel(text);
        label.setForeground(new Color(222, 208, 184));
        label.setFont(UiTheme.body(Font.PLAIN, 12));
        row.add(dot, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private void performPrimaryAction() {
        if (engine.currentEnemy() != null) {
            listener.onSound(SoundManager.Cue.ATTACK);
            engine.attack();
        } else if (engine.currentTile() == GameEngine.TileType.SHOP) {
            listener.onSound(SoundManager.Cue.UI_CONFIRM);
            listener.onShop();
            return;
        } else if (engine.currentTile() == GameEngine.TileType.TAVERN ||
                   engine.currentTile() == GameEngine.TileType.ENCAMPMENT) {
            listener.onSound(SoundManager.Cue.UI_CONFIRM);
            listener.onHaven();
            return;
        } else {
            listener.onSound(SoundManager.Cue.UI_CONFIRM);
            engine.locationAction();
        }
        listener.onStateChanged();
    }

    private void performSecondaryAction() {
        if (engine.currentEnemy() != null) {
            listener.onSound(SoundManager.Cue.FLEE);
            engine.flee();
        } else {
            GameEngine.State before = engine.getState();
            engine.move(0, 1);
            GameEngine.State after = engine.getState();
            listener.onSound(after.col != before.col ? SoundManager.Cue.MOVE :
                SoundManager.Cue.ERROR);
        }
        listener.onStateChanged();
    }

    private void performTertiaryAction() {
        if (engine.currentEnemy() != null) {
            listener.onSound(SoundManager.Cue.DEFEND);
            engine.defend();
        } else if (engine.currentTile() == GameEngine.TileType.TAVERN ||
                   engine.currentTile() == GameEngine.TileType.ENCAMPMENT) {
            listener.onSound(SoundManager.Cue.REST);
            engine.locationAction();
        } else {
            listener.onSound(SoundManager.Cue.HEAL);
            engine.usePotion();
        }
        listener.onStateChanged();
    }

    void refresh() {
        GameEngine.State state = engine.getState();
        String coordinate = Character.toString((char) ('A' + state.row)) + (state.col + 1);
        String identity = "Level " + state.level + " " + state.race + " " + state.heroClass;
        heroPortrait.setResourceAsync("/assets/avatars/" + state.race.toLowerCase() + "-" +
            state.heroClass.toLowerCase() + ".png");
        heroPortrait.setBorder(new FantasyPortraitBorder(state.race, state.heroClass));
        heroName.setText(state.playerName);
        heroIdentity.setText(identity);
        setMeter(health, state.health, state.maxHealth, "HEALTH");
        setMeter(mana, state.mana, Math.max(1, state.maxMana), "MANA");
        mana.setVisible(state.maxMana > 0);
        setMeter(experience, state.experience, state.level * 30, "EXPERIENCE");
        refreshEquipment(weaponEquipment, equippedItem(state, state.equippedWeapon),
            state.equippedWeapon == null ? "No weapon" : state.equippedWeapon);
        refreshEquipment(armourEquipment, equippedItem(state, state.equippedArmour),
            state.equippedArmour == null ? "No armour" : state.equippedArmour);
        refreshEquipment(offhandEquipment, equippedItem(state, state.equippedOffhand),
            state.equippedOffhand == null ? "Empty offhand" : state.equippedOffhand);
        heroStats.setText("<html>ATK " + engine.getAttack() + " &nbsp;&nbsp; DEF " +
            engine.getDefense() + "<br>GOLD " + state.gold + " &nbsp;&nbsp; POTIONS " +
            state.potions + "</html>");

        headerLocation.setText(engine.currentTile().label + " — " + coordinate);
        sceneImage.setResourceAsync(sceneResourceFor(engine.currentTile(), engine.currentVendor()));
        sceneTitle.setText(titleFor(engine.currentTile()));
        sceneDescription.setText(descriptionFor(engine.currentTile()));
        eventLog.setText(engine.getHistory());
        eventLog.setCaretPosition(eventLog.getDocument().getLength());

        mapLocation.setText(titleFor(engine.currentTile()) + " — " + coordinate);
        mapTerritory.setText(territoryFor(engine.currentTile()));
        mapDescription.setText(descriptionFor(engine.currentTile()));
        map.repaint();

        GameEngine.Enemy enemy = engine.currentEnemy();
        if (enemy != null) {
            primaryAction.setText("ATTACK " + enemy.name.toUpperCase());
            secondaryAction.setText("FLEE");
            tertiaryAction.setText("DEFEND");
        } else if (engine.currentTile() == GameEngine.TileType.SHOP) {
            primaryAction.setText("ENTER SHOP");
            secondaryAction.setText("CONTINUE JOURNEY");
            tertiaryAction.setText("USE POTION");
        } else if (engine.currentTile() == GameEngine.TileType.TAVERN ||
                   engine.currentTile() == GameEngine.TileType.ENCAMPMENT) {
            primaryAction.setText(engine.currentTile() == GameEngine.TileType.TAVERN ?
                "ENTER TAVERN" : "VISIT WAYCAMP");
            secondaryAction.setText("CONTINUE JOURNEY");
            tertiaryAction.setText("REST");
        } else {
            primaryAction.setText("INVESTIGATE");
            secondaryAction.setText("CONTINUE JOURNEY");
            tertiaryAction.setText("USE POTION");
        }
        normalizeActionButton(primaryAction);
        normalizeActionButton(tertiaryAction);
        normalizeActionButton(secondaryAction);
        normalizeActionButton(spellbookAction);
        revalidate();
        repaint();
    }

    void showCombatHealth(int value, int maximum) {
        setMeter(health, value, maximum, "HEALTH");
        health.repaint();
    }

    /** Waits for asynchronous art only when tests/exporters need a stable frame. */
    void awaitArtworkForSnapshot() {
        heroPortrait.awaitResource();
        sceneImage.awaitResource();
    }

    void animateCombatHealth(final int from, final int to, final int maximum,
                             boolean reducedMotion) {
        if (reducedMotion || from == to) {
            showCombatHealth(to, maximum);
            return;
        }
        final int safeMaximum = Math.max(1, maximum);
        health.setMaximum(safeMaximum);
        health.setValue(Math.max(0, from));
        health.setString("HEALTH   " + from + " / " + maximum);
        final long started = System.currentTimeMillis();
        Timer timer = new Timer(16, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                double progress = Math.min(1d,
                    (System.currentTimeMillis() - started) / 240d);
                double eased = 1d - Math.pow(1d - progress, 3d);
                int value = (int) Math.round(from + (to - from) * eased);
                // Only the bar value and label change during the tween. The
                // maximum is set once above so Swing does not repeatedly fire
                // model-change events for an invariant value.
                health.setValue(Math.max(0, value));
                health.setString("HEALTH   " + value + " / " + maximum);
                health.repaint();
                if (progress >= 1d) ((Timer) event.getSource()).stop();
            }
        });
        timer.start();
    }

    private void normalizeActionButton(JButton button) {
        button.setPreferredSize(null);
        Dimension natural = button.getPreferredSize();
        button.setPreferredSize(new Dimension(natural.width, 44));
    }

    String primaryActionLabelForTest() {
        return primaryAction.getText();
    }

    void triggerPrimaryActionForTest() {
        primaryAction.doClick();
    }

    private String titleFor(GameEngine.TileType tile) {
        switch (tile) {
            case CRYPT: return "Forgotten Crypt";
            case LAKE: return "Moonwater Crossing";
            case SHOP: return "Frontier Market";
            case TAVERN: return "Wanderer's Rest";
            case ENCAMPMENT: return "Traveler's Encampment";
            default: return "Ancient Ruins — Alshira Forest";
        }
    }

    /**
     * Maps the current tile to its exterior/location artwork. NPC portraits are
     * intentionally reserved for LocationPanel after the player chooses Enter.
     */
    static String sceneResourceFor(GameEngine.TileType tile, String vendor) {
        if (tile == GameEngine.TileType.LAKE) return "/assets/scenes/moonwater-crossing.png";
        if (tile == GameEngine.TileType.CRYPT) return "/assets/scenes/forgotten-crypt.png";
        if (tile == GameEngine.TileType.TAVERN)
            return "/assets/scenes/wanderers-rest-exterior.png";
        if (tile == GameEngine.TileType.ENCAMPMENT)
            return "/assets/scenes/alchemist-waycamp-exterior.png";
        if (tile == GameEngine.TileType.SHOP) {
            return "Blacksmith".equals(vendor)
                ? "/assets/scenes/blacksmith-forge-exterior.png"
                : "/assets/scenes/frontier-market-exterior.png";
        }
        return "/assets/scenes/alshira-ruins.png";
    }

    private String territoryFor(GameEngine.TileType tile) {
        switch (tile) {
            case CRYPT: return "The Ashen Burial Grounds";
            case LAKE: return "The Moonwater Basin";
            case SHOP: return "The Golden Road";
            case TAVERN: return "Stonecrest Frontier";
            case ENCAMPMENT: return "Alshira Waycamp";
            default: return "Alshira Forest";
        }
    }

    private String descriptionFor(GameEngine.TileType tile) {
        switch (tile) {
            case CRYPT:
                return "Weathered stones descend into a silent crypt where old magic lingers beneath the dust.";
            case LAKE:
                return "Cold blue water cuts across the trail. Ripples move against the wind near the far shore.";
            case SHOP:
                return "Lanterns mark a frontier market where merchants trade weapons, provisions, and rumors.";
            case TAVERN:
                return "Firelight and the smell of a hot meal offer a welcome pause from the dangerous road.";
            case ENCAMPMENT:
                return "A guarded camp provides shelter, supplies, and a safe place to recover before traveling on.";
            default:
                return "Crumbling stone pillars rise from the mossy ground. The air is thick with damp earth and old magic.";
        }
    }

    private static JProgressBar meter(Color color) {
        JProgressBar bar = new StatusBar(color);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        return bar;
    }

    private void setMeter(JProgressBar bar, int value, int maximum, String label) {
        bar.setMaximum(Math.max(1, maximum));
        bar.setValue(Math.max(0, value));
        bar.setString(label + "   " + value + " / " + maximum);
    }

    private GameEngine.Item equippedItem(GameEngine.State state, String itemName) {
        if (state == null || state.inventory == null || itemName == null) return null;
        for (GameEngine.Item item : state.inventory) {
            if (itemName.equals(item.name)) return item;
        }
        return null;
    }

    private void refreshEquipment(EquipmentRow row, GameEngine.Item item, String fallbackName) {
        if (row != null) row.showItem(item, fallbackName);
    }

    /** Compact inventory-quality treatment for the persistent equipped items. */
    private static final class EquipmentRow extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel value;

        EquipmentRow(JLabel value, String iconResource) {
            super(new BorderLayout(8, 0));
            this.value = value;
            setBackground(new Color(39, 29, 24));
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(220, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            JLabel icon = new JLabel("", SwingConstants.CENTER);
            icon.setIcon(IconAssets.icon(iconResource, 19));
            icon.setPreferredSize(new Dimension(22, 20));
            value.setFont(UiTheme.body(Font.BOLD, 12));
            add(icon, BorderLayout.WEST);
            add(value, BorderLayout.CENTER);
            showItem(null, "No equipment");
        }

        void showItem(GameEngine.Item item, String fallbackName) {
            String quality = item == null ? "EMPTY" : GameEngine.itemQuality(item);
            Color qualityColor = item == null ? UiTheme.MUTED : UiTheme.qualityColor(quality);
            String name = fallbackName == null ? "No equipment" : fallbackName;
            value.setForeground(qualityColor);
            value.setText("<html><b>" + name + "</b><br><font size='2'>[" +
                quality + "]</font></html>");
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 3, 1, 1, qualityColor),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        }
    }
}
