package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

final class CharacterCreationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    interface Listener {
        void onBegin(String name, String race, String heroClass);
        default void onBegin(String name, String race, String heroClass, String starterKit) {
            onBegin(name, race, heroClass);
        }
        void onBack();
        default void onRandomizeName() { }
        default void onRaceSelected(String race) { }
        default void onClassSelected(String heroClass) { }
    }

    private final Listener listener;
    private final GameEngine previewEngine = new GameEngine();
    private final JTextField name = new JTextField("Aelindra");
    private final ChoiceCard[] raceCards = new ChoiceCard[GameEngine.RACES.length];
    private final ChoiceCard[] classCards = new ChoiceCard[GameEngine.CLASSES.length];
    private final JButton[] starterKitButtons = new JButton[2];
    private final AssetImagePanel previewImage = new AssetImagePanel(null, false);
    private final JLabel previewTitle = new JLabel();
    private final JLabel attackValue = new JLabel();
    private final JLabel defenseValue = new JLabel();
    private final JLabel equipment = new JLabel();
    private final JLabel classDescription = new JLabel();
    private final JLabel buildEyebrow = new JLabel();
    private final JLabel buildSummary = new JLabel();
    private final JLabel combatStyle = new JLabel();
    private final JLabel buildTraits = new JLabel();
    private JPanel buildGuidePanel;
    private JPanel previewPanel;
    private final JProgressBar health = meter(UiTheme.HEALTH);
    private final JProgressBar mana = meter(UiTheme.BLUE);
    private String selectedRace = "Elf";
    private String selectedClass = "Mage";
    private String selectedStarterKit = GameEngine.defaultStarterKit("Mage");
    private boolean initialSelection = true;
    private final Timer artworkTimer;
    private String artworkClass;

    CharacterCreationPanel(Listener listener) {
        this.listener = listener;
        artworkTimer = new Timer(90, new ActionListener() {
            public void actionPerformed(ActionEvent event) { applySelectedArtwork(false); }
        });
        artworkTimer.setRepeats(false);
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshSelection();
        initialSelection = false;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.SURFACE_DEEP);
        header.setPreferredSize(new Dimension(0, UiTheme.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        JLabel title = new JLabel("THE CHOSEN QUEST",
            new SystemIcon(SystemIcon.Type.STAR, 20, UiTheme.GOLD), SwingConstants.LEFT);
        title.setIconTextGap(12);
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 21));
        header.add(title, BorderLayout.WEST);
        JLabel step = new JLabel("CREATE YOUR HERO");
        step.setForeground(UiTheme.GOLD_LIGHT);
        step.setFont(UiTheme.body(Font.BOLD, 12));
        header.add(step, BorderLayout.EAST);
        return header;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setBackground(UiTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        content.add(buildSelectionArea(), BorderLayout.CENTER);
        content.add(buildPreview(), BorderLayout.EAST);
        return content;
    }

    private JScrollPane buildSelectionArea() {
        JPanel selection = new JPanel();
        selection.setLayout(new BoxLayout(selection, BoxLayout.Y_AXIS));
        selection.setBackground(UiTheme.BACKGROUND);

        JLabel heading = new JLabel("Create Your Hero");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.display(38));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        selection.add(heading);
        selection.add(Box.createVerticalStrut(18));

        selection.add(sectionLabel("HERO NAME"));
        name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        name.setPreferredSize(new Dimension(700, 48));
        name.setBackground(UiTheme.SURFACE);
        name.setForeground(UiTheme.TEXT);
        name.setCaretColor(UiTheme.TEXT);
        name.setFont(UiTheme.body(Font.PLAIN, 16));
        name.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        name.setAlignmentX(LEFT_ALIGNMENT);
        JPanel nameRow = new JPanel(new BorderLayout(10, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        nameRow.add(name, BorderLayout.CENTER);
        JButton randomize = UiTheme.button("", false);
        randomize.setIcon(new SystemIcon(SystemIcon.Type.DICE, 23, UiTheme.GOLD_LIGHT));
        randomize.setPreferredSize(new Dimension(48, 48));
        randomize.setMinimumSize(new Dimension(48, 48));
        randomize.setMaximumSize(new Dimension(48, 48));
        randomize.setToolTipText("Roll a fantasy hero name");
        randomize.getAccessibleContext().setAccessibleName("Randomize hero name");
        randomize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (listener != null) listener.onRandomizeName();
                name.setText(FantasyNameGenerator.generate(
                    selectedRace, selectedClass, name.getText()));
                name.requestFocusInWindow();
                name.selectAll();
            }
        });
        nameRow.add(randomize, BorderLayout.EAST);
        selection.add(nameRow);
        selection.add(Box.createVerticalStrut(22));

        selection.add(sectionLabel("CHOOSE RACE"));
        JPanel races = new JPanel(new GridLayout(1, 4, 14, 0));
        races.setOpaque(false);
        races.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        races.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < GameEngine.RACES.length; i++) {
            final String race = GameEngine.RACES[i];
            raceCards[i] = new ChoiceCard(race, descriptionForRace(race), asset(race, selectedClass, false));
            raceCards[i].addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    selectedRace = race;
                    refreshSelection();
                    if (listener != null) listener.onRaceSelected(race);
                }
            });
            races.add(raceCards[i]);
        }
        selection.add(races);
        selection.add(Box.createVerticalStrut(22));

        selection.add(sectionLabel("CHOOSE CLASS"));
        JPanel classes = new JPanel(new GridLayout(1, 4, 14, 0));
        classes.setOpaque(false);
        classes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        classes.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < GameEngine.CLASSES.length; i++) {
            final String heroClass = GameEngine.CLASSES[i];
            classCards[i] = new ChoiceCard(heroClass, descriptionForClass(heroClass),
                asset(selectedRace, heroClass, false), true);
            classCards[i].addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    selectedClass = heroClass;
                    selectedStarterKit = GameEngine.defaultStarterKit(heroClass);
                    refreshSelection();
                    if (listener != null) listener.onClassSelected(heroClass);
                }
            });
            classes.add(classCards[i]);
        }
        selection.add(classes);
        selection.add(Box.createVerticalStrut(10));
        classDescription.setForeground(UiTheme.MUTED);
        classDescription.setFont(UiTheme.body(Font.PLAIN, 12));
        classDescription.setAlignmentX(LEFT_ALIGNMENT);
        selection.add(classDescription);
        selection.add(Box.createVerticalStrut(18));
        selection.add(sectionLabel("CHOOSE STARTING LOADOUT"));
        JPanel kits = new JPanel(new GridLayout(1, 2, 12, 0));
        kits.setOpaque(false);
        kits.setAlignmentX(LEFT_ALIGNMENT);
        kits.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        for (int i = 0; i < starterKitButtons.length; i++) {
            final int index = i;
            starterKitButtons[i] = UiTheme.button("", false);
            starterKitButtons[i].setFont(UiTheme.body(Font.BOLD, 11));
            starterKitButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    selectedStarterKit = GameEngine.starterKitsFor(selectedClass)[index];
                    refreshSelection();
                }
            });
            kits.add(starterKitButtons[i]);
        }
        selection.add(kits);
        selection.add(Box.createVerticalStrut(18));
        selection.add(buildBuildGuide());
        selection.add(Box.createVerticalStrut(4));

        JScrollPane scroll = new JScrollPane(selection);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Explains the practical identity of the selected race/class pairing. Keeping this
     * beside the selectors lets players compare builds before committing to a quest.
     */
    private JPanel buildBuildGuide() {
        JPanel guide = new JPanel();
        buildGuidePanel = guide;
        guide.setLayout(new BoxLayout(guide, BoxLayout.Y_AXIS));
        guide.setBackground(UiTheme.SURFACE);
        guide.setAlignmentX(LEFT_ALIGNMENT);
        guide.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        buildEyebrow.setForeground(UiTheme.GOLD_LIGHT);
        buildEyebrow.setFont(UiTheme.body(Font.BOLD, 11));
        buildEyebrow.setAlignmentX(LEFT_ALIGNMENT);
        guide.add(buildEyebrow);
        guide.add(Box.createVerticalStrut(5));

        buildSummary.setForeground(UiTheme.TEXT);
        buildSummary.setFont(UiTheme.body(Font.PLAIN, 14));
        buildSummary.setAlignmentX(LEFT_ALIGNMENT);
        guide.add(buildSummary);
        guide.add(Box.createVerticalStrut(14));

        JPanel details = new JPanel(new GridLayout(1, 2, 18, 0));
        details.setOpaque(false);
        details.setAlignmentX(LEFT_ALIGNMENT);
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        details.add(buildGuideColumn("COMBAT STYLE", combatStyle));
        details.add(buildGuideColumn("BUILD TRAITS", buildTraits));
        guide.add(details);
        return guide;
    }

    private JPanel buildGuideColumn(String title, JLabel copy) {
        JPanel column = new JPanel(new BorderLayout(0, 4));
        column.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setForeground(UiTheme.MUTED);
        heading.setFont(UiTheme.body(Font.BOLD, 10));
        copy.setForeground(UiTheme.TEXT);
        copy.setFont(UiTheme.body(Font.PLAIN, 12));
        column.add(heading, BorderLayout.NORTH);
        column.add(copy, BorderLayout.CENTER);
        return column;
    }

    private JPanel buildPreview() {
        JPanel preview = new JPanel();
        previewPanel = preview;
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        preview.setBackground(UiTheme.SURFACE);
        preview.setPreferredSize(new Dimension(520, 700));
        UiTheme.panelBorder(preview, 20);

        // Full-body portraits are 3:4. Keep the framed component at that same
        // ratio so the border follows the artwork instead of framing side bars.
        previewImage.setPreferredSize(new Dimension(270, 360));
        previewImage.setMinimumSize(new Dimension(270, 360));
        previewImage.setMaximumSize(new Dimension(270, 360));
        previewImage.setBorder(new FantasyPortraitBorder(selectedRace, selectedClass));
        JPanel portraitRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        portraitRow.setOpaque(false);
        portraitRow.setAlignmentX(LEFT_ALIGNMENT);
        portraitRow.setPreferredSize(new Dimension(398, 360));
        portraitRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        portraitRow.add(previewImage);
        preview.add(portraitRow);
        preview.add(Box.createVerticalStrut(14));

        previewTitle.setForeground(UiTheme.GOLD);
        previewTitle.setFont(UiTheme.display(30));
        previewTitle.setAlignmentX(LEFT_ALIGNMENT);
        preview.add(previewTitle);
        preview.add(Box.createVerticalStrut(12));
        preview.add(health);
        preview.add(Box.createVerticalStrut(8));
        preview.add(mana);
        preview.add(Box.createVerticalStrut(12));

        JPanel combatStats = new JPanel(new GridLayout(1, 2, 12, 0));
        combatStats.setOpaque(false);
        combatStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        combatStats.setAlignmentX(LEFT_ALIGNMENT);
        combatStats.add(stat("ATTACK", attackValue));
        combatStats.add(stat("DEFENSE", defenseValue));
        preview.add(combatStats);
        preview.add(Box.createVerticalStrut(12));

        equipment.setOpaque(true);
        equipment.setBackground(UiTheme.SURFACE_DEEP);
        equipment.setForeground(UiTheme.TEXT);
        equipment.setFont(UiTheme.body(Font.PLAIN, 13));
        equipment.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        equipment.setAlignmentX(LEFT_ALIGNMENT);
        equipment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        preview.add(equipment);
        preview.add(Box.createVerticalGlue());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        JButton back = UiTheme.button("BACK", false);
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBack(); }
        });
        JButton begin = UiTheme.button("BEGIN JOURNEY", true);
        begin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                listener.onBegin(name.getText(), selectedRace, selectedClass, selectedStarterKit);
            }
        });
        actions.add(back);
        actions.add(begin);
        preview.add(actions);
        return preview;
    }

    private JPanel stat(String label, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel heading = new JLabel(label);
        heading.setForeground(UiTheme.MUTED);
        heading.setFont(UiTheme.body(Font.BOLD, 10));
        value.setForeground(UiTheme.TEXT);
        value.setFont(UiTheme.body(Font.BOLD, 18));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JLabel sectionLabel(String value) {
        JLabel label = new JLabel(value);
        label.setForeground(UiTheme.GOLD_LIGHT);
        label.setFont(UiTheme.body(Font.BOLD, 12));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void refreshSelection() {
        Color classAccent = FantasyPortraitBorder.accentForClass(selectedClass);
        if (initialSelection) {
            applySelectedArtwork(true);
        } else {
            // Keep click feedback immediate while coalescing expensive image
            // decoding/scaling when the player rapidly explores several builds.
            artworkTimer.restart();
        }
        for (int i = 0; i < raceCards.length; i++) {
            String race = GameEngine.RACES[i];
            raceCards[i].setSelected(race.equals(selectedRace), classAccent);
        }
        for (int i = 0; i < classCards.length; i++) {
            String heroClass = GameEngine.CLASSES[i];
            Color choiceAccent = FantasyPortraitBorder.accentForClass(heroClass);
            classCards[i].setAccent(choiceAccent);
            classCards[i].setSelected(heroClass.equals(selectedClass), choiceAccent);
        }

        String[] kits = GameEngine.starterKitsFor(selectedClass);
        for (int i = 0; i < starterKitButtons.length; i++) {
            String kit = kits[i];
            starterKitButtons[i].setText("<html><center>" + kit + "<br><font size='2'>" +
                GameEngine.starterKitDescription(selectedClass, kit) + "</font></center></html>");
            UiTheme.setButtonActive(starterKitButtons[i], kit.equals(selectedStarterKit));
        }
        previewEngine.configureHeroPreview(selectedRace, selectedClass, selectedStarterKit);
        GameEngine.State state = previewEngine.getState();
        previewTitle.setText(selectedRace + " " + selectedClass);
        previewTitle.setForeground(classAccent);
        classDescription.setForeground(classAccent);
        buildEyebrow.setForeground(classAccent);
        buildGuidePanel.setBackground(blend(UiTheme.SURFACE, classAccent, .10));
        buildGuidePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(classAccent),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        previewPanel.setBackground(blend(UiTheme.SURFACE, classAccent, .07));
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(classAccent),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        equipment.setBackground(blend(UiTheme.SURFACE_DEEP, classAccent, .15));
        previewImage.setBorder(new FantasyPortraitBorder(selectedRace, selectedClass));
        setMeter(health, state.health, state.maxHealth, "HEALTH");
        setMeter(mana, state.mana, Math.max(1, state.maxMana), "MANA");
        mana.setVisible(state.maxMana > 0);
        attackValue.setText(Integer.toString(previewEngine.getAttack()));
        defenseValue.setText(Integer.toString(previewEngine.getDefense()));
        equipment.setText("<html><b><font color='" + htmlColor(classAccent) +
            "'>STARTING EQUIPMENT</font></b><br>• " +
            state.equippedWeapon + (state.equippedOffhand == null ? "" :
                "<br>• " + state.equippedOffhand) +
            "<br>• " + state.equippedArmour + "</html>");
        classDescription.setText(descriptionForClass(selectedClass));
        refreshBuildGuide();
        repaint();
    }

    /** Applies only the final artwork requested during a burst of clicks. */
    private void applySelectedArtwork(boolean synchronous) {
        String race = selectedRace;
        String heroClass = selectedClass;
        String fullBody = asset(race, heroClass, true);
        if (synchronous) previewImage.setResource(fullBody);
        else previewImage.setResourceAsync(fullBody);

        // Race portraits change with class, but not when only the race choice
        // changes. Avoid resubmitting four identical image requests.
        if (synchronous || !heroClass.equals(artworkClass)) {
            for (int i = 0; i < raceCards.length; i++) {
                String cardRace = GameEngine.RACES[i];
                raceCards[i].setImage(asset(cardRace, heroClass, false));
                raceCards[i].setPortraitFrame(cardRace, heroClass);
            }
        }
        artworkClass = heroClass;
    }

    private void refreshBuildGuide() {
        buildEyebrow.setText("SELECTED BUILD  ·  " +
            selectedRace.toUpperCase() + " " + selectedClass.toUpperCase());
        buildSummary.setText("<html>" + buildSummaryFor(selectedRace, selectedClass) + "</html>");
        combatStyle.setText("<html>" + combatStyleFor(selectedClass) + "</html>");
        buildTraits.setText("<html>" + raceTraitFor(selectedRace) + "<br>" +
            classTraitFor(selectedClass) + "</html>");
    }

    private String buildSummaryFor(String race, String heroClass) {
        String raceLead;
        if ("Dwarf".equals(race)) raceLead = "A durable but deliberate hero";
        else if ("Elf".equals(race)) raceLead = "A swift, magically gifted hero";
        else if ("Halfling".equals(race)) raceLead = "A nimble, resourceful hero";
        else raceLead = "A reliable, adaptable hero";

        if ("Mage".equals(heroClass))
            return raceLead + " who wins through spell choice and careful mana control.";
        if ("Rogue".equals(heroClass))
            return raceLead + " built around stealth, fast turns, and decisive critical strikes.";
        if ("Hunter".equals(heroClass))
            return raceLead + " who creates openings with Aim and controls fights at range.";
        return raceLead + " who survives pressure with armour, defense, and steady melee damage.";
    }

    private String combatStyleFor(String heroClass) {
        if ("Mage".equals(heroClass))
            return "Spellcaster · tactical<br>Cast for burst damage; defend to recover mana.";
        if ("Rogue".equals(heroClass))
            return "Skirmisher · very fast<br>Enter stealth, then convert setup into critical damage.";
        if ("Hunter".equals(heroClass))
            return "Ranged striker · fast<br>Take aim, then land powerful precision shots.";
        return "Front-line defender · steady<br>Trade blows safely and use Defend against heavy attacks.";
    }

    private String raceTraitFor(String race) {
        if ("Dwarf".equals(race)) return "Dwarf: high health and defense; slower turns.";
        if ("Elf".equals(race)) return "Elf: bonus mana, attack, and speed.";
        if ("Halfling".equals(race)) return "Halfling: fastest movement and bonus starting gold.";
        return "Human: balanced health and attack.";
    }

    private String classTraitFor(String heroClass) {
        if ("Mage".equals(heroClass)) return "Mage: high burst; weak basic melee attacks.";
        if ("Rogue".equals(heroClass)) return "Rogue: quickest turns and stealth criticals.";
        if ("Hunter".equals(heroClass)) return "Hunter: ranged gear and precision bonuses.";
        return "Fighter: highest durability and dependable damage.";
    }

    private String asset(String race, String heroClass, boolean fullBody) {
        return "/assets/avatars/" + (fullBody ? "full-body/" : "") +
            race.toLowerCase() + "-" + heroClass.toLowerCase() + ".png";
    }

    private String descriptionForRace(String race) {
        if ("Dwarf".equals(race)) return "Sturdy masters of stone.";
        if ("Elf".equals(race)) return "Agile, mystical explorers.";
        if ("Halfling".equals(race)) return "Nimble and clever.";
        return "Adaptable and resilient.";
    }

    private String descriptionForClass(String heroClass) {
        if ("Mage".equals(heroClass)) return "Wielder of arcane energy.";
        if ("Rogue".equals(heroClass)) return "Expert in stealth and shadow.";
        if ("Hunter".equals(heroClass)) return "Guardian of the wild.";
        return "Master of steel and shield.";
    }

    private static JProgressBar meter(Color color) {
        JProgressBar bar = new StatusBar(color);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        return bar;
    }

    private static Color blend(Color base, Color accent, double amount) {
        double weight = Math.max(0d, Math.min(1d, amount));
        return new Color(
            (int) Math.round(base.getRed() * (1d - weight) + accent.getRed() * weight),
            (int) Math.round(base.getGreen() * (1d - weight) + accent.getGreen() * weight),
            (int) Math.round(base.getBlue() * (1d - weight) + accent.getBlue() * weight));
    }

    private static String htmlColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void setMeter(JProgressBar bar, int value, int maximum, String label) {
        bar.setMaximum(Math.max(1, maximum));
        bar.setValue(Math.max(0, value));
        bar.setString(label + "   " + value + " / " + maximum);
    }

    private static final class ChoiceCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final AssetImagePanel image;
        private final boolean compact;
        private final JLabel titleLabel;

        ChoiceCard(String title, String description, String resource) {
            this(title, description, resource, false);
        }

        ChoiceCard(String title, String description, String resource, boolean compact) {
            this.compact = compact;
            setLayout(new BorderLayout(0, 8));
            setBackground(UiTheme.SURFACE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (compact) {
                image = null;
                titleLabel = new JLabel(title, SwingConstants.CENTER);
                titleLabel.setForeground(UiTheme.GOLD_LIGHT);
                titleLabel.setFont(UiTheme.body(Font.BOLD, 12));
                add(titleLabel, BorderLayout.CENTER);
                setSelected(false, UiTheme.GOLD);
                return;
            }
            image = new AssetImagePanel(resource, true);
            image.setPreferredSize(new Dimension(150, 115));
            add(image, BorderLayout.CENTER);

            JPanel copy = new JPanel(new GridLayout(2, 1, 0, 2));
            copy.setOpaque(false);
            titleLabel = new JLabel(title);
            titleLabel.setForeground(UiTheme.GOLD_LIGHT);
            titleLabel.setFont(UiTheme.display(18));
            JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
            descriptionLabel.setForeground(UiTheme.TEXT);
            descriptionLabel.setFont(UiTheme.body(Font.PLAIN, 11));
            copy.add(titleLabel);
            copy.add(descriptionLabel);
            add(copy, BorderLayout.SOUTH);
            setSelected(false, UiTheme.GOLD);
        }

        void setImage(String resource) {
            if (image != null) image.setResourceAsync(resource);
        }

        void setPortraitFrame(String race, String heroClass) {
            if (image != null) image.setBorder(new FantasyPortraitBorder(race, heroClass, true));
        }

        void setAccent(Color accent) {
            titleLabel.setForeground(accent);
        }

        void setSelected(boolean selected, Color accent) {
            int padding = compact ? 7 : (selected ? 9 : 10);
            setBackground(selected ? blend(UiTheme.SURFACE, accent, .16) : UiTheme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? accent : UiTheme.BORDER,
                    selected ? 2 : 1),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
        }
    }
}
