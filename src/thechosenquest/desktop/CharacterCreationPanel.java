package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

final class CharacterCreationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static boolean avatarWarmupStarted;

    interface Listener {
        void onBegin(String name, String race, String heroClass);
        default void onBegin(String name, String race, String heroClass, String starterKit) {
            onBegin(name, race, heroClass);
        }
        default void onBegin(String name, String race, String heroClass, String starterKit,
                             String gender) {
            onBegin(name, race, heroClass, starterKit);
        }
        void onBack();
        default void onRandomizeName() { }
        default void onRaceSelected(String race) { }
        default void onClassSelected(String heroClass) { }
        default void onCombatPathSelected(String path) { }
        default void onGenderSelected(String gender) { }
    }

    private final Listener listener;
    private final boolean reducedMotion = new GamePreferences().isReducedMotion();
    private final GameEngine previewEngine = new GameEngine();
    private final JTextField name = new JTextField("Aelindra");
    private final ChoiceCard[] raceCards = new ChoiceCard[GameEngine.RACES.length];
    private final ChoiceCard[] classCards = new ChoiceCard[GameEngine.CLASSES.length];
    private final JButton[] starterKitButtons = new JButton[2];
    private final JButton[] genderButtons = new JButton[2];
    private final AssetImagePanel previewImage = new AssetImagePanel(null, true);
    private final JLabel playerNameTitle = new JLabel();
    private final ArtDecoProfileNamePanel playerNamePlate =
        new ArtDecoProfileNamePanel(playerNameTitle);
    private final JLabel previewTitle = new JLabel();
    private final JLabel nameCount = new JLabel();
    private final JLabel attackValue = new JLabel();
    private final JLabel defenseValue = new JLabel();
    private final JPanel equipment = new JPanel(new GridLayout(1, 3, 8, 0));
    private final JLabel equipmentWeapon = new JLabel();
    private final JLabel equipmentOffhand = new JLabel();
    private final JLabel equipmentArmour = new JLabel();
    private final JLabel classDescription = new JLabel();
    private final JLabel buildEyebrow = new JLabel();
    private final JTextArea buildSummary = wrappingCopy();
    private final JTextArea combatStyle = wrappingCopy();
    private final JTextArea buildTraits = wrappingCopy();
    private final JTextArea abilityPath = wrappingCopy();
    private final JTextArea startingProfile = wrappingCopy();
    private JPanel buildGuidePanel;
    private JPanel previewPanel;
    private CreationThemePanel creationShell;
    private ArtDecoDividerScrollBarUI dividerScrollUi;
    private final JProgressBar health = meter(UiTheme.HEALTH);
    private final JProgressBar mana = meter(UiTheme.BLUE);
    private String selectedRace = "Elf";
    private String selectedClass = "Mage";
    private String selectedStarterKit = GameEngine.defaultStarterKit("Mage");
    private String selectedGender = CharacterArt.FEMALE;
    private boolean initialSelection = true;
    private final Timer artworkTimer;
    private String artworkClass;
    private String artworkGender;

    CharacterCreationPanel(Listener listener) {
        this.listener = listener;
        ((AbstractDocument) name.getDocument()).setDocumentFilter(
            new MaxLengthFilter(GameEngine.MAX_PLAYER_NAME_LENGTH));
        name.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refreshProfileName(); }
            public void removeUpdate(DocumentEvent event) { refreshProfileName(); }
            public void changedUpdate(DocumentEvent event) { refreshProfileName(); }
        });
        artworkTimer = new Timer(90, new ActionListener() {
            public void actionPerformed(ActionEvent event) { applySelectedArtwork(false); }
        });
        artworkTimer.setRepeats(false);
        setLayout(new BorderLayout());
        previewImage.setAnimatedTransitions(!reducedMotion);
        setBackground(UiTheme.BACKGROUND);
        setBorder(ArtDecoBorder.shell(UiTheme.GOLD, 7));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshSelection();
        initialSelection = false;
        warmAvatarRenders();
    }

    /** Pre-renders all build portraits while the player is still on the title screen. */
    private void warmAvatarRenders() {
        synchronized (CharacterCreationPanel.class) {
            if (avatarWarmupStarted) return;
            avatarWarmupStarted = true;
        }
        String[] portraits = new String[GameEngine.RACES.length *
            GameEngine.CLASSES.length * 2];
        String[] fullBody = new String[portraits.length];
        int index = 0;
        for (String heroClass : GameEngine.CLASSES) {
            for (String race : GameEngine.RACES) {
                for (String gender : new String[] {
                        CharacterArt.MALE, CharacterArt.FEMALE}) {
                    portraits[index] = asset(race, heroClass, gender, false);
                    fullBody[index] = asset(race, heroClass, gender, true);
                    index++;
                }
            }
        }
        AssetImagePanel.preloadRenderedAsync(portraits, 150, 150, true);
        AssetImagePanel.preloadRenderedAsync(fullBody, 270, 360, false);
    }

    private JPanel buildHeader() {
        JPanel header = new ArtDecoHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(UiTheme.SURFACE_DEEP);
        header.setPreferredSize(new Dimension(0, UiTheme.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        // The full application artwork is intentionally retained for the OS,
        // releases, and community surfaces. This brighter simplified mark is
        // authored specifically to remain legible in a 24–32 px navigation slot.
        Icon brandIcon = IconAssets.icon("/assets/app-icon-ui.png", 30);
        JLabel title = new JLabel("THE CHOSEN QUEST",
            brandIcon == null
                ? new SystemIcon(SystemIcon.Type.STAR, 20, UiTheme.GOLD)
                : brandIcon,
            SwingConstants.LEFT);
        title.setIconTextGap(12);
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 21));
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel buildContent() {
        CreationThemePanel content = new CreationThemePanel(CreationThemePanel.Role.SHELL);
        creationShell = content;
        content.setLayout(new BorderLayout(18, 0));
        content.setBorder(BorderFactory.createEmptyBorder(12, 32, 12, 32));
        content.add(buildSelectionArea(), BorderLayout.CENTER);
        content.add(buildPreview(), BorderLayout.EAST);
        return content;
    }

    private JScrollPane buildSelectionArea() {
        JPanel selection = new VerticalScrollablePanel();
        selection.setLayout(new BoxLayout(selection, BoxLayout.Y_AXIS));
        selection.setOpaque(false);

        JPanel heading = new ArtDecoHeading("CREATE YOUR HERO");
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        selection.add(heading);
        selection.add(verticalGap(4, 10, 18));

        JPanel nameHeading = new JPanel();
        nameHeading.setLayout(new BoxLayout(nameHeading, BoxLayout.X_AXIS));
        nameHeading.setOpaque(false);
        nameHeading.setAlignmentX(LEFT_ALIGNMENT);
        nameHeading.setPreferredSize(new Dimension(560, 26));
        nameHeading.setMinimumSize(new Dimension(560, 26));
        nameHeading.setMaximumSize(new Dimension(560, 26));
        JLabel nameLabel = sectionLabel("HERO NAME");
        nameLabel.setAlignmentY(CENTER_ALIGNMENT);
        nameHeading.add(nameLabel);
        nameHeading.add(Box.createHorizontalStrut(8));
        nameCount.setForeground(UiTheme.MUTED);
        nameCount.setFont(UiTheme.body(Font.BOLD, 10));
        nameCount.setHorizontalAlignment(SwingConstants.CENTER);
        nameCount.setVerticalAlignment(SwingConstants.CENTER);
        nameCount.setAlignmentY(CENTER_ALIGNMENT);
        nameCount.setPreferredSize(new Dimension(46, 26));
        nameCount.setMinimumSize(new Dimension(46, 26));
        nameCount.setMaximumSize(new Dimension(46, 26));
        nameHeading.add(nameCount);
        nameHeading.add(Box.createHorizontalStrut(8));

        JButton randomize = new JButton();
        final RollingDiceIcon diceIcon = new RollingDiceIcon(IconAssets.icon(
            "/assets/icons/items/utility-name-die.png", 26));
        randomize.setIcon(diceIcon);
        randomize.setPreferredSize(new Dimension(30, 26));
        randomize.setMinimumSize(new Dimension(30, 26));
        randomize.setMaximumSize(new Dimension(30, 26));
        randomize.setAlignmentY(CENTER_ALIGNMENT);
        randomize.setBorder(BorderFactory.createEmptyBorder());
        randomize.setBorderPainted(false);
        randomize.setContentAreaFilled(false);
        randomize.setFocusPainted(false);
        randomize.setOpaque(false);
        randomize.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        randomize.setToolTipText("Roll a fantasy hero name");
        randomize.getAccessibleContext().setAccessibleName("Randomize hero name");
        final int[] rollFrame = {0};
        final Timer diceRoll = new Timer(55, null);
        diceRoll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                rollFrame[0]++;
                diceIcon.setFrame(rollFrame[0]);
                randomize.repaint();
                if (rollFrame[0] >= 7) {
                    diceRoll.stop();
                    rollFrame[0] = 0;
                    diceIcon.setFrame(0);
                    randomize.repaint();
                }
            }
        });
        randomize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                rollFrame[0] = 0;
                diceRoll.restart();
                if (listener != null) listener.onRandomizeName();
                name.setText(FantasyNameGenerator.generate(
                    selectedRace, selectedClass, name.getText()));
                name.requestFocusInWindow();
                name.selectAll();
            }
        });
        nameHeading.add(randomize);
        selection.add(nameHeading);
        selection.add(verticalGap(2, 4, 8));
        name.setMaximumSize(new Dimension(560, 44));
        name.setPreferredSize(new Dimension(560, 44));
        name.setBackground(UiTheme.SURFACE);
        name.setForeground(UiTheme.TEXT);
        name.setCaretColor(UiTheme.TEXT);
        name.setFont(UiTheme.display(20));
        name.setBorder(BorderFactory.createCompoundBorder(
            new ArtDecoBorder(UiTheme.GOLD, 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        name.setAlignmentX(LEFT_ALIGNMENT);
        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameRow.setPreferredSize(new Dimension(560, 44));
        nameRow.setMaximumSize(new Dimension(560, 44));
        nameRow.add(name, BorderLayout.CENTER);
        selection.add(nameRow);
        selection.add(verticalGap(5, 10, 18));

        selection.add(sectionLabel("GENDER"));
        JPanel genders = new JPanel(new GridLayout(1, 2, 10, 0));
        genders.setOpaque(false);
        genders.setAlignmentX(LEFT_ALIGNMENT);
        genders.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        String[] genderChoices = {CharacterArt.FEMALE, CharacterArt.MALE};
        for (int i = 0; i < genderChoices.length; i++) {
            final String gender = genderChoices[i];
            genderButtons[i] = UiTheme.button(gender.toUpperCase(), false);
            decorateButton(genderButtons[i], UiTheme.GOLD, false);
            genderButtons[i].setFont(UiTheme.displayBold(13));
            genderButtons[i].setToolTipText(
                "Changes character artwork only; gameplay remains identical");
            genderButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    selectedGender = gender;
                    refreshSelection();
                    if (listener != null) listener.onGenderSelected(gender);
                }
            });
            genders.add(genderButtons[i]);
        }
        selection.add(genders);
        selection.add(verticalGap(6, 12, 22));

        selection.add(sectionLabel("RACE"));
        JPanel races = new JPanel(new GridLayout(1, 4, 14, 0));
        races.setOpaque(false);
        races.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        races.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < GameEngine.RACES.length; i++) {
            final String race = GameEngine.RACES[i];
            raceCards[i] = new ChoiceCard(race, descriptionForRace(race),
                raceCrestResource(race), false, true);
            raceCards[i].setSelectionAction(new Runnable() {
                public void run() {
                    selectedRace = race;
                    refreshSelection();
                    if (listener != null) listener.onRaceSelected(race);
                }
            });
            races.add(raceCards[i]);
        }
        selection.add(races);
        selection.add(verticalGap(8, 14, 26));

        selection.add(sectionLabel("CLASS"));
        JPanel classes = new JPanel(new GridLayout(2, 2, 14, 10));
        classes.setOpaque(false);
        classes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));
        classes.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < GameEngine.CLASSES.length; i++) {
            final String heroClass = GameEngine.CLASSES[i];
            classCards[i] = new ChoiceCard(heroClass, classCardDescription(heroClass),
                classIconResource(heroClass), true, false);
            classCards[i].setSelectionAction(new Runnable() {
                public void run() {
                    selectedClass = heroClass;
                    selectedStarterKit = GameEngine.defaultStarterKit(heroClass);
                    refreshSelection();
                    if (listener != null) listener.onClassSelected(heroClass);
                }
            });
            classes.add(classCards[i]);
        }
        selection.add(classes);
        selection.add(verticalGap(6, 12, 22));
        selection.add(sectionLabel("COMBAT PATH"));
        JPanel kits = new JPanel(new GridLayout(1, 2, 12, 0));
        kits.setOpaque(false);
        kits.setAlignmentX(LEFT_ALIGNMENT);
        kits.setPreferredSize(new Dimension(600, 118));
        kits.setMinimumSize(new Dimension(240, 108));
        kits.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        for (int i = 0; i < starterKitButtons.length; i++) {
            final int index = i;
            starterKitButtons[i] = UiTheme.button("", false);
            decorateButton(starterKitButtons[i], UiTheme.GOLD, false);
            starterKitButtons[i].setFont(UiTheme.displayBold(12));
            starterKitButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    selectedStarterKit = GameEngine.starterKitsFor(selectedClass)[index];
                    refreshSelection();
                    if (listener != null) listener.onCombatPathSelected(selectedStarterKit);
                }
            });
            kits.add(starterKitButtons[i]);
        }
        selection.add(kits);
        selection.add(verticalGap(6, 12, 22));
        selection.add(buildBuildGuide());
        selection.add(verticalGap(2, 4, 8));
        selection.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 8));

        JScrollPane scroll = new JScrollPane(selection);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setViewportBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Always reserve the divider width. When no scrolling is needed its gem
        // rests at center; with overflow the same gem becomes the draggable thumb.
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        dividerScrollUi =
            ArtDecoDividerScrollBarUI.install(scroll.getVerticalScrollBar());
        scroll.getVerticalScrollBar().getAccessibleContext().setAccessibleName(
            "Character creation section divider and scroll control");
        return scroll;
    }

    /**
     * Explains the practical identity of the selected race/class pairing. Keeping this
     * beside the selectors lets players compare builds before committing to a quest.
     */
    private JPanel buildBuildGuide() {
        CreationThemePanel guide =
            new CreationThemePanel(CreationThemePanel.Role.DOSSIER);
        buildGuidePanel = guide;
        guide.setLayout(new BoxLayout(guide, BoxLayout.Y_AXIS));
        guide.setAlignmentX(LEFT_ALIGNMENT);
        guide.setBorder(new ArtDecoBorder(UiTheme.GOLD, 22));

        buildEyebrow.setForeground(UiTheme.GOLD_LIGHT);
        buildEyebrow.setFont(UiTheme.title(Font.BOLD, 10));
        buildEyebrow.setAlignmentX(LEFT_ALIGNMENT);
        buildEyebrow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        guide.add(buildEyebrow);
        guide.add(verticalGap(3, 5, 10));

        buildSummary.setForeground(UiTheme.TEXT);
        buildSummary.setFont(UiTheme.displayBold(15));
        buildSummary.setAlignmentX(LEFT_ALIGNMENT);
        buildSummary.setRows(2);
        buildSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        guide.add(buildSummary);
        guide.add(verticalGap(6, 10, 18));

        JPanel details = new JPanel(new GridLayout(2, 2, 18, 12));
        details.setOpaque(false);
        details.setAlignmentX(LEFT_ALIGNMENT);
        details.setPreferredSize(new Dimension(600, 184));
        details.setMinimumSize(new Dimension(240, 184));
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        details.add(buildGuideColumn("COMBAT LOOP", combatStyle));
        details.add(buildGuideColumn("RACE & CLASS TRAITS", buildTraits));
        details.add(buildGuideColumn("ABILITY PROGRESSION", abilityPath));
        details.add(buildGuideColumn("STARTING PROFILE", startingProfile));
        guide.add(details);
        return guide;
    }

    private JPanel buildGuideColumn(String title, JTextArea copy) {
        JPanel column = new JPanel(new BorderLayout(0, 4));
        column.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setForeground(UiTheme.MUTED);
        heading.setFont(UiTheme.title(Font.BOLD, 9));
        copy.setForeground(UiTheme.TEXT);
        copy.setFont(UiTheme.displayBold(14));
        column.add(heading, BorderLayout.NORTH);
        column.add(copy, BorderLayout.CENTER);
        return column;
    }

    private JPanel buildPreview() {
        CreationThemePanel preview =
            new CreationThemePanel(CreationThemePanel.Role.HERO_PROFILE);
        previewPanel = preview;
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        preview.setPreferredSize(new Dimension(540, 760));
        preview.setBorder(new ArtDecoBorder(UiTheme.GOLD, 12));

        // Full-body portraits are 3:4. Keep the framed component at that same
        // ratio so the border follows the artwork instead of framing side bars.
        previewImage.setPreferredSize(new Dimension(420, 560));
        previewImage.setMinimumSize(new Dimension(150, 200));
        previewImage.setMaximumSize(new Dimension(420, 560));
        // The authored frame has a stepped silhouette. Let the themed profile
        // surface show through outside that silhouette instead of painting the
        // image panel's rectangular fallback behind it.
        previewImage.setOpaque(false);
        previewImage.setBorder(new FantasyPortraitBorder(selectedRace, selectedClass));
        JPanel portraitRow = new ResponsiveHeroPortraitHolder(previewImage);
        portraitRow.setOpaque(false);
        portraitRow.setAlignmentX(CENTER_ALIGNMENT);
        portraitRow.setPreferredSize(new Dimension(440, 560));
        portraitRow.setMinimumSize(new Dimension(200, 200));
        portraitRow.setMaximumSize(new Dimension(440, 560));
        preview.add(portraitRow);
        preview.add(verticalGap(8, 14, 22));

        JPanel profile = new JPanel();
        profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
        profile.setOpaque(false);
        profile.setAlignmentX(CENTER_ALIGNMENT);
        profile.setPreferredSize(new Dimension(440, 326));
        profile.setMinimumSize(new Dimension(240, 298));
        profile.setMaximumSize(new Dimension(440, 340));

        playerNameTitle.setForeground(UiTheme.GOLD_LIGHT);
        playerNameTitle.setFont(UiTheme.display(34));
        playerNameTitle.setHorizontalAlignment(SwingConstants.CENTER);
        playerNameTitle.setAlignmentX(CENTER_ALIGNMENT);
        playerNameTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        playerNamePlate.setOpaque(false);
        playerNamePlate.setAlignmentX(CENTER_ALIGNMENT);
        playerNamePlate.setPreferredSize(new Dimension(440, 42));
        playerNamePlate.setMinimumSize(new Dimension(240, 42));
        playerNamePlate.setMaximumSize(new Dimension(440, 42));
        profile.add(playerNamePlate);
        profile.add(verticalGap(1, 2, 5));

        previewTitle.setForeground(UiTheme.MUTED);
        previewTitle.setFont(UiTheme.display(20));
        previewTitle.setHorizontalAlignment(SwingConstants.CENTER);
        previewTitle.setAlignmentX(CENTER_ALIGNMENT);
        previewTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        profile.add(previewTitle);
        classDescription.setFont(UiTheme.body(Font.BOLD, 10));
        classDescription.setHorizontalAlignment(SwingConstants.CENTER);
        classDescription.setAlignmentX(CENTER_ALIGNMENT);
        classDescription.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        profile.add(classDescription);
        profile.add(verticalGap(6, 10, 16));
        health.setAlignmentX(CENTER_ALIGNMENT);
        health.setPreferredSize(new Dimension(440, 26));
        health.setMaximumSize(new Dimension(440, 26));
        profile.add(health);
        profile.add(verticalGap(5, 8, 13));
        mana.setAlignmentX(CENTER_ALIGNMENT);
        mana.setPreferredSize(new Dimension(440, 26));
        mana.setMaximumSize(new Dimension(440, 26));
        profile.add(mana);
        profile.add(verticalGap(7, 12, 18));

        JPanel combatStats = new JPanel(new GridLayout(1, 2, 12, 0));
        combatStats.setOpaque(false);
        combatStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        combatStats.setAlignmentX(CENTER_ALIGNMENT);
        combatStats.add(stat("ATTACK", attackValue));
        combatStats.add(stat("DEFENSE", defenseValue));
        profile.add(combatStats);
        profile.add(verticalGap(7, 12, 20));

        equipment.setOpaque(true);
        equipment.setBackground(UiTheme.SURFACE_DEEP);
        equipment.setBorder(new ArtDecoBorder(UiTheme.GOLD, 12));
        equipment.setAlignmentX(CENTER_ALIGNMENT);
        // The Art Deco border consumes 24 vertical pixels. A 102 px strip leaves
        // all three 40 px icons and two text lines fully visible inside it.
        equipment.setPreferredSize(new Dimension(440, 102));
        equipment.setMinimumSize(new Dimension(240, 102));
        equipment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
        configureEquipmentLabel(equipmentWeapon);
        configureEquipmentLabel(equipmentOffhand);
        configureEquipmentLabel(equipmentArmour);
        equipment.add(equipmentWeapon);
        equipment.add(equipmentOffhand);
        equipment.add(equipmentArmour);
        profile.add(equipment);
        preview.add(profile);
        preview.add(verticalGap(4, 8, 14));
        preview.add(Box.createVerticalGlue());

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(CENTER_ALIGNMENT);
        actions.setPreferredSize(new Dimension(392, 44));
        actions.setMinimumSize(new Dimension(240, 44));
        actions.setMaximumSize(new Dimension(392, 44));
        JButton back = UiTheme.button("BACK", false);
        decorateButton(back, UiTheme.GOLD, false);
        back.setFont(UiTheme.displayBold(15));
        back.setPreferredSize(new Dimension(190, 44));
        back.setMinimumSize(new Dimension(100, 44));
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBack(); }
        });
        JButton begin = UiTheme.button("BEGIN JOURNEY", true);
        decoratePrimaryButton(begin, UiTheme.GOLD_LIGHT);
        begin.setFont(UiTheme.displayBold(15));
        begin.setPreferredSize(new Dimension(190, 44));
        begin.setMinimumSize(new Dimension(100, 44));
        begin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (reducedMotion) {
                    listener.onBegin(name.getText(), selectedRace, selectedClass,
                        selectedStarterKit, selectedGender);
                    return;
                }
                begin.setEnabled(false);
                Timer confirmation = new Timer(180, new ActionListener() {
                    public void actionPerformed(ActionEvent ignored) {
                        listener.onBegin(name.getText(), selectedRace, selectedClass,
                            selectedStarterKit, selectedGender);
                    }
                });
                confirmation.setRepeats(false);
                confirmation.start();
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
        heading.setFont(UiTheme.title(Font.BOLD, 10));
        value.setForeground(UiTheme.TEXT);
        value.setFont(UiTheme.displayBold(20));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private void configureEquipmentLabel(JLabel label) {
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.displayBold(12));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setIconTextGap(3);
    }

    /**
     * A bounded gap keeps the 720p layout compact while letting 1080p distribute
     * breathing room between authored sections instead of collecting dead space
     * beneath the dossier.
     */
    private static java.awt.Component verticalGap(int minimum, int preferred,
                                                  int maximum) {
        return new Box.Filler(
            new Dimension(0, minimum),
            new Dimension(0, preferred),
            new Dimension(Integer.MAX_VALUE, maximum));
    }

    private JLabel sectionLabel(String value) {
        JLabel label = new JLabel(value);
        label.setForeground(UiTheme.GOLD_LIGHT);
        label.setFont(UiTheme.title(Font.BOLD, 11));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static void decorateButton(JButton button, Color accent,
                                       boolean selected) {
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.DECO_SECONDARY, 7, 12);
        button.putClientProperty("thechosenquest.button.accent", accent);
        UiTheme.setButtonActive(button, selected);
    }

    private static void decoratePrimaryButton(JButton button, Color accent) {
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.DECO_PRIMARY, 7, 12);
        button.putClientProperty("thechosenquest.button.accent", accent);
    }

    private void refreshSelection() {
        HeroVisualTheme heroTheme =
            HeroVisualTheme.forBuild(selectedRace, selectedClass);
        Color classAccent = heroTheme.classAccent();
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
            boolean active = kit.equals(selectedStarterKit);
            String cardCopy = "<html><center><b>" + kit + "</b><br>" +
                "<font size='3'>" + GameEngine.combatPathFantasy(kit) + "</font><br>" +
                "<font size='2'>" + GameEngine.starterKitDescription(selectedClass, kit) +
                "<br>" + GameEngine.combatPathResourceLoop(selectedClass, kit) +
                "<br>" + GameEngine.combatPathTrack(kit) +
                "<br><i>Tradeoff: " + GameEngine.combatPathTradeoff(kit) +
                "</i></font></center></html>";
            starterKitButtons[i].setText(cardCopy);
            starterKitButtons[i].setToolTipText("<html><b>" + kit + "</b><br>" +
                GameEngine.combatPathFantasy(kit) + "<br>" +
                GameEngine.starterKitDescription(selectedClass, kit) + "<br>" +
                GameEngine.combatPathResourceLoop(selectedClass, kit) + "<br>" +
                GameEngine.combatPathTrack(kit) + "<br>Tradeoff: " +
                GameEngine.combatPathTradeoff(kit) + "</html>");
            starterKitButtons[i].getAccessibleContext().setAccessibleDescription(
                kit + ". " + GameEngine.combatPathFantasy(kit) + ". " +
                GameEngine.starterKitDescription(selectedClass, kit) + ". " +
                GameEngine.combatPathResourceLoop(selectedClass, kit) + ". " +
                GameEngine.combatPathTrack(kit) + ". Tradeoff: " +
                GameEngine.combatPathTradeoff(kit));
            UiTheme.setButtonActive(starterKitButtons[i], active);
            decorateButton(starterKitButtons[i],
                active ? classAccent : heroTheme.metalAccent(), active);
        }
        for (JButton genderButton : genderButtons) {
            if (genderButton != null) {
                boolean active =
                    selectedGender.equalsIgnoreCase(genderButton.getText());
                UiTheme.setButtonActive(genderButton, active);
                decorateButton(genderButton,
                    active ? classAccent : heroTheme.metalAccent(), active);
            }
        }
        previewEngine.configureHeroPreview(selectedRace, selectedClass, selectedStarterKit,
            selectedGender);
        GameEngine.State state = previewEngine.getState();
        previewTitle.setText(selectedRace + " " + selectedClass);
        previewTitle.setForeground(blend(UiTheme.TEXT, classAccent, .65));
        playerNamePlate.setAccent(heroTheme.metalAccent(), classAccent);
        refreshProfileName();
        classDescription.setForeground(classAccent);
        buildEyebrow.setForeground(classAccent);
        ((CreationThemePanel) buildGuidePanel).setHeroVisualTheme(heroTheme);
        buildGuidePanel.setBorder(
            new ArtDecoBorder(heroTheme.metalAccent(), 22, true));
        ((CreationThemePanel) previewPanel).setHeroVisualTheme(heroTheme);
        previewPanel.setBorder(
            new ArtDecoBorder(heroTheme.metalAccent(), 12, true));
        creationShell.setHeroVisualTheme(heroTheme);
        if (dividerScrollUi != null) dividerScrollUi.setHeroVisualTheme(heroTheme);
        equipment.setBackground(blend(UiTheme.SURFACE_DEEP, classAccent, .15));
        equipment.setBorder(new ArtDecoBorder(heroTheme.metalAccent(), 12));
        previewImage.setBorder(new FantasyPortraitBorder(selectedRace, selectedClass));
        setMeter(health, state.health, state.maxHealth, "HEALTH");
        if ("Mage".equals(state.heroClass)) {
            mana.setForeground(heroTheme.resourceColor());
            setMeter(mana, state.mana, Math.max(1, state.maxMana), "MANA");
            mana.setVisible(true);
        } else if ("Fighter".equals(state.heroClass)) {
            mana.setForeground(heroTheme.resourceColor());
            setMeter(mana, state.rage, Math.max(1, state.maxRage), "RAGE");
            mana.setVisible(true);
        } else if ("Rogue".equals(state.heroClass)) {
            mana.setForeground(heroTheme.resourceColor());
            setMeter(mana, state.momentum, Math.max(1, state.maxMomentum), "MOMENTUM");
            mana.setVisible(true);
        } else if ("Hunter".equals(state.heroClass)) {
            mana.setForeground(heroTheme.resourceColor());
            setMeter(mana, state.focus, Math.max(1, state.maxFocus), "FOCUS");
            mana.setVisible(true);
        } else {
            mana.setVisible(false);
        }
        attackValue.setText(Integer.toString(previewEngine.getAttack()));
        defenseValue.setText(Integer.toString(previewEngine.getDefense()));
        setEquipmentLabel(equipmentWeapon, state.equippedWeapon, "Weapon",
            itemFor(state, state.equippedWeapon));
        setEquipmentLabel(equipmentOffhand, state.equippedOffhand,
            "Offhand", itemFor(state, state.equippedOffhand));
        setEquipmentLabel(equipmentArmour, state.equippedArmour, "Armour",
            itemFor(state, state.equippedArmour));
        classDescription.setText("ORIGIN  " + GameEngine.originPath(state) +
            "   ·   CURRENT STYLE  " + GameEngine.currentCombatStyle(state));
        refreshBuildGuide();
        revalidate();
        repaint();
    }

    /** Applies only the final artwork requested during a burst of clicks. */
    private void applySelectedArtwork(boolean synchronous) {
        String race = selectedRace;
        String heroClass = selectedClass;
        String fullBody = asset(race, heroClass, selectedGender, true);
        if (synchronous) previewImage.setResource(fullBody);
        else previewImage.setResourceAsync(fullBody);

        // Race portraits change with class, but not when only the race choice
        // changes. Avoid resubmitting four identical image requests.
        if (synchronous || !heroClass.equals(artworkClass) ||
                !selectedGender.equals(artworkGender)) {
            for (int i = 0; i < raceCards.length; i++) {
                String cardRace = GameEngine.RACES[i];
                raceCards[i].setImage(asset(cardRace, heroClass, selectedGender, false));
                raceCards[i].setPortraitFrame(cardRace, heroClass);
            }
        }
        artworkClass = heroClass;
        artworkGender = selectedGender;
    }

    private void refreshBuildGuide() {
        buildEyebrow.setText("SELECTED BUILD  ·  " +
            selectedRace.toUpperCase() + " " + selectedClass.toUpperCase());
        buildSummary.setText(buildSummaryFor(selectedRace, selectedClass));
        combatStyle.setText(plainLines(combatStyleFor(selectedClass)));
        buildTraits.setText(raceTraitFor(selectedRace, selectedClass) + "\n" +
            classTraitFor(selectedClass));
        GameEngine.State preview = previewEngine.getState();
        abilityPath.setText(plainLines(GameEngine.abilityProgressionSummary(preview)));
        startingProfile.setText(classResource(selectedClass) + " resource · " +
            difficultyFor(selectedClass) + "\n" +
            GameEngine.starterKitDescription(selectedClass, selectedStarterKit));
    }

    private static JTextArea wrappingCopy() {
        JTextArea copy = new JTextArea();
        copy.setEditable(false);
        copy.setFocusable(false);
        copy.setOpaque(false);
        copy.setBorder(null);
        copy.setLineWrap(true);
        copy.setWrapStyleWord(true);
        copy.setRows(4);
        return copy;
    }

    private static String plainLines(String value) {
        return value == null ? "" : value.replace("<br>", "\n");
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
            return raceLead + " who builds Momentum through pressure and spends it on decisive strikes.";
        if ("Hunter".equals(heroClass))
            return raceLead + " who builds Focus, marks priority targets, and controls fights at range.";
        return raceLead + " who turns attacks and incoming damage into Rage-powered techniques.";
    }

    private String combatStyleFor(String heroClass) {
        if ("Mage".equals(heroClass))
            return "Spellcaster · tactical<br>Cast for burst damage; defend to recover mana.";
        if ("Rogue".equals(heroClass))
            return "Skirmisher · very fast<br>Build Momentum with attacks and evasion; spend it on advanced abilities.";
        if ("Hunter".equals(heroClass))
            return "Ranged striker · fast<br>Build Focus with Aim and accuracy; mark targets before Volley.";
        return "Front-line warrior · steady<br>Build Rage by trading blows; spend it on powerful techniques.";
    }

    private String raceTraitFor(String race, String heroClass) {
        if ("Dwarf".equals(race)) return "Dwarf: high health and defense; slower turns.";
        if ("Elf".equals(race)) return "Mage".equals(heroClass)
            ? "Elf: bonus mana, attack, and speed."
            : "Elf: bonus attack and speed.";
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
        return asset(race, heroClass, selectedGender, fullBody);
    }

    private String asset(String race, String heroClass, String gender, boolean fullBody) {
        return fullBody ? CharacterArt.fullBody(race, heroClass, gender)
            : CharacterArt.portrait(race, heroClass, gender);
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

    private String classCardDescription(String heroClass) {
        if ("Mage".equals(heroClass)) return "MANA · TACTICAL CASTER";
        if ("Rogue".equals(heroClass)) return "MOMENTUM · FAST FINISHER";
        if ("Hunter".equals(heroClass)) return "FOCUS · RANGED CONTROL";
        return "RAGE · FRONT-LINE PRESSURE";
    }

    private static String pathIdentityFor(String heroClass, String path) {
        if ("KNIGHT".equals(path)) return "Guard · Shield Counter · Second Wind";
        if ("BERSERKER".equals(path)) return "Heavy Strike · armour break · Rage";
        if ("CHANNELER".equals(path)) return "Reliable casting · mana control";
        if ("ARCANIST".equals(path)) return "Wand + tome · focused spell power";
        if ("ASSASSIN".equals(path)) return "Ambush · Vanish · Execute";
        if ("SKIRMISHER".equals(path)) return "Twin Strike · Evasive Strike · Flurry";
        if ("MARKSMAN".equals(path)) return "Opening shot · mark · precision";
        return "Aim · mobile fire · ranged control";
    }

    private String classResource(String heroClass) {
        if ("Mage".equals(heroClass)) return "Mana";
        if ("Rogue".equals(heroClass)) return "Momentum";
        if ("Hunter".equals(heroClass)) return "Focus";
        return "Rage";
    }

    private String difficultyFor(String heroClass) {
        if ("Mage".equals(heroClass)) return "Demanding";
        if ("Rogue".equals(heroClass)) return "Tactical";
        if ("Hunter".equals(heroClass)) return "Tactical";
        return "Approachable";
    }

    private static String classIconResource(String heroClass) {
        if ("Mage".equals(heroClass)) return "/assets/icons/items/weapon-arcane-staff.png";
        if ("Rogue".equals(heroClass)) return "/assets/icons/items/weapon-dagger.png";
        if ("Hunter".equals(heroClass)) return "/assets/icons/items/weapon-longbow.png";
        return "/assets/icons/items/weapon-greatsword.png";
    }

    private GameEngine.Item itemFor(GameEngine.State state, String itemName) {
        if (state == null || itemName == null) return null;
        for (GameEngine.Item item : state.inventory) {
            if (itemName.equals(item.name)) return item;
        }
        return null;
    }

    private void setEquipmentLabel(JLabel label, String name, String type,
                                   GameEngine.Item item) {
        if (name == null || name.trim().isEmpty()) {
            label.setText("<html><center><font color='" +
                htmlColor(UiTheme.MUTED) + "'>" + type.toUpperCase() +
                "</font><br>&nbsp;</center></html>");
            label.setIcon(IconAssets.emptySlotIcon(40));
            label.setToolTipText(type + " slot is empty");
            return;
        }
        String quality = GameEngine.itemQuality(item);
        Color qualityColor = UiTheme.qualityColor(quality);
        label.setText("<html><center><font color='" + htmlColor(UiTheme.MUTED) +
            "'>" + type.toUpperCase() +
            "</font><br><font color='" + htmlColor(qualityColor) + "'>" +
            name + "</font></center></html>");
        label.setIcon(item == null
            ? IconAssets.icon(IconAssets.itemResource(name, type), 40)
            : IconAssets.itemIcon(item, 40));
        label.setToolTipText(type + ": " + name + " [" + quality + "]");
    }

    void selectBuildForTest(String race, String heroClass, String starterKit) {
        selectedRace = race;
        selectedClass = heroClass;
        selectedStarterKit = starterKit;
        refreshSelection();
        artworkTimer.stop();
        applySelectedArtwork(false);
    }

    void selectGenderForTest(String gender) {
        selectedGender = CharacterArt.normalizeGender(gender, selectedRace, selectedClass);
        refreshSelection();
        artworkTimer.stop();
        applySelectedArtwork(false);
    }

    void awaitArtworkForTest() { previewImage.awaitResource(); }

    String displayedArtworkForTest() { return previewImage.displayedResourceForTest(); }

    void setPlayerNameForTest(String value) { name.setText(value); }

    String playerNameForTest() { return name.getText(); }

    String displayedProfileNameForTest() { return playerNameTitle.getText(); }

    String combatPathCardCopyForTest(int index) {
        return starterKitButtons[index].getText();
    }

    String combatPathCardAccessibleCopyForTest(int index) {
        return starterKitButtons[index].getAccessibleContext().getAccessibleDescription();
    }

    void activateCombatPathForTest(int index) { starterKitButtons[index].doClick(); }

    boolean compactProfileFitsForTest() {
        return equipment.getHeight() >= 102 &&
            findButtonHeight(this, "BACK") >= 44 &&
            findButtonHeight(this, "BEGIN JOURNEY") >= 44 &&
            copyFits(buildSummary) && copyFits(combatStyle) &&
            copyFits(buildTraits) && copyFits(abilityPath) &&
            copyFits(startingProfile);
    }

    private static boolean copyFits(JTextArea copy) {
        if (copy.getWidth() <= 0 || copy.getHeight() <= 0) return false;
        return copy.getPreferredSize().height <= copy.getHeight() + 2;
    }

    private static int findButtonHeight(java.awt.Container container, String text) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JButton &&
                    text.equals(((JButton) component).getText())) {
                return component.getHeight();
            }
            if (component instanceof java.awt.Container) {
                int nested = findButtonHeight((java.awt.Container) component, text);
                if (nested >= 0) return nested;
            }
        }
        return -1;
    }

    boolean racePortraitsSquareForTest() {
        for (ChoiceCard card : raceCards) {
            if (card != null && !card.portraitIsSquare()) return false;
        }
        return true;
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

    private static String raceCrestResource(String race) {
        return "/assets/race-crests/" + race.toLowerCase() + ".png";
    }

    private void refreshProfileName() {
        String value = name.getText() == null ? "" : name.getText().trim();
        playerNameTitle.setText(value.isEmpty() ? "The Chosen One" : value);
        int length = value.length();
        playerNameTitle.setFont(UiTheme.display(
            length > 20 ? 25 : length > 15 ? 29 : 34));
        nameCount.setText(length + " / " + GameEngine.MAX_PLAYER_NAME_LENGTH);
    }

    private void setMeter(JProgressBar bar, int value, int maximum, String label) {
        bar.setMaximum(Math.max(1, maximum));
        bar.setValue(Math.max(0, value));
        bar.setString(label + "   " + value + " / " + maximum);
    }

    /** Keeps authored and player-entered names within the profile title contract. */
    private static final class MaxLengthFilter extends DocumentFilter {
        private final int limit;

        MaxLengthFilter(int limit) {
            this.limit = limit;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String value,
                                 AttributeSet attributes)
                throws BadLocationException {
            replace(bypass, offset, 0, value, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length,
                            String value, AttributeSet attributes)
                throws BadLocationException {
            String addition = value == null ? "" : value;
            int available = limit - (bypass.getDocument().getLength() - length);
            if (available <= 0) return;
            if (addition.length() > available) {
                addition = addition.substring(0, available);
            }
            super.replace(bypass, offset, length, addition, attributes);
        }
    }

    /**
     * Animates inside a fixed icon box, so rolling never changes the name-row
     * baseline, its height, or the gap above the text field.
     */
    private static final class RollingDiceIcon implements Icon {
        private static final int[] LIFT = {0, 1, 3, 1, 2, 0, 1, 0};
        private final Icon source;
        private int frame;

        RollingDiceIcon(Icon source) {
            this.source = source == null
                ? new SystemIcon(SystemIcon.Type.DICE, 24, UiTheme.GOLD_LIGHT)
                : source;
        }

        void setFrame(int frame) {
            this.frame = Math.max(0, Math.min(frame, LIFT.length - 1));
        }

        public int getIconWidth() { return 30; }

        public int getIconHeight() { return 28; }

        public void paintIcon(java.awt.Component component, Graphics graphics,
                              int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            int centerX = x + getIconWidth() / 2;
            int centerY = y + getIconHeight() / 2 - LIFT[frame];
            g.translate(centerX, centerY);
            g.rotate(Math.toRadians(frame * 90d));
            source.paintIcon(component, g,
                -source.getIconWidth() / 2, -source.getIconHeight() / 2);
            g.dispose();
        }
    }

    private static final class ChoiceCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final AssetImagePanel image;
        private final boolean compact;
        private final boolean crest;
        private final String iconResource;
        private final JLabel titleLabel;
        private JLabel emblemLabel;
        private JLabel descriptionLabel;
        private JPanel copyPanel;
        private boolean condensed;

        ChoiceCard(String title, String description, String resource) {
            this(title, description, resource, false);
        }

        ChoiceCard(String title, String description, String resource, boolean compact) {
            this(title, description, resource, compact, false);
        }

        ChoiceCard(String title, String description, String resource,
                   boolean compact, boolean crest) {
            this.compact = compact;
            this.crest = crest;
            this.iconResource = resource;
            setLayout(new BorderLayout(14, 8));
            setBackground(UiTheme.SURFACE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            getAccessibleContext().setAccessibleName(title);
            getAccessibleContext().setAccessibleDescription(description);
            if (compact || crest) {
                image = null;
                ImageIcon crestImage = crest ? IconAssets.icon(resource, 60) : null;
                emblemLabel = new JLabel(crest
                    ? (crestImage != null ? crestImage : new RaceCrestIcon(title, 52))
                    : IconAssets.icon(resource, 36));
                emblemLabel.setHorizontalAlignment(SwingConstants.CENTER);
                emblemLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
                add(emblemLabel, BorderLayout.WEST);
                copyPanel = new JPanel(new GridLayout(crest ? 1 : 2, 1, 0, 2));
                copyPanel.setOpaque(false);
                titleLabel = new JLabel(title, SwingConstants.LEFT);
                titleLabel.setForeground(crest ? UiTheme.TEXT : UiTheme.GOLD_LIGHT);
                titleLabel.setFont(crest
                    ? UiTheme.displayBold(19) : UiTheme.displayBold(14));
                descriptionLabel = new JLabel(description);
                descriptionLabel.setForeground(UiTheme.MUTED);
                descriptionLabel.setFont(UiTheme.body(Font.BOLD, 8));
                copyPanel.add(titleLabel);
                if (!crest) copyPanel.add(descriptionLabel);
                add(copyPanel, BorderLayout.CENTER);
                setSelected(false, UiTheme.GOLD);
                return;
            }
            image = new AssetImagePanel(resource, true);
            // The source portraits and decorative frames are square. A centered
            // square viewport prevents wide desktop layouts from stretching the
            // card while cover-scaling and cropping away the character's face.
            SquarePortraitHolder holder = new SquarePortraitHolder(image);
            if (crest) {
                holder.setPreferredSize(new Dimension(68, 68));
                holder.setMinimumSize(new Dimension(56, 56));
                add(holder, BorderLayout.WEST);
            } else {
                add(holder, BorderLayout.CENTER);
            }

            JPanel copy = new JPanel(new GridLayout(crest ? 1 : 2, 1, 0, 2));
            copy.setOpaque(false);
            titleLabel = new JLabel(title);
            titleLabel.setForeground(UiTheme.GOLD_LIGHT);
            titleLabel.setFont(UiTheme.display(crest ? 16 : 18));
            descriptionLabel = new JLabel("<html>" + description + "</html>");
            descriptionLabel.setForeground(UiTheme.TEXT);
            descriptionLabel.setFont(UiTheme.body(Font.PLAIN, 11));
            copy.add(titleLabel);
            if (!crest) copy.add(descriptionLabel);
            add(copy, crest ? BorderLayout.CENTER : BorderLayout.SOUTH);
            setSelected(false, UiTheme.GOLD);
        }

        @Override
        public void doLayout() {
            super.doLayout();
            if (emblemLabel == null || copyPanel == null) return;

            if (crest) {
                boolean stack = getWidth() < 176;
                if (stack != condensed) {
                    condensed = stack;
                    Icon icon = IconAssets.icon(iconResource, stack ? 44 : 60);
                    if (icon != null) emblemLabel.setIcon(icon);
                    titleLabel.setFont(UiTheme.displayBold(stack ? 15 : 19));
                    titleLabel.setHorizontalAlignment(
                        stack ? SwingConstants.CENTER : SwingConstants.LEFT);
                    emblemLabel.setBorder(stack
                        ? BorderFactory.createEmptyBorder()
                        : BorderFactory.createEmptyBorder(0, 0, 0, 4));
                }
                if (stack) {
                    java.awt.Insets insets = getInsets();
                    int innerWidth = Math.max(1,
                        getWidth() - insets.left - insets.right);
                    int innerHeight = Math.max(1,
                        getHeight() - insets.top - insets.bottom);
                    int iconHeight = Math.min(46, Math.max(34, innerHeight - 24));
                    emblemLabel.setBounds(insets.left, insets.top,
                        innerWidth, iconHeight);
                    copyPanel.setBounds(insets.left,
                        insets.top + iconHeight - 1,
                        innerWidth, Math.max(20, innerHeight - iconHeight + 1));
                }
                return;
            }

            // Class cards remain horizontal, but their symbol and typography scale
            // down together if a narrower host window reaches this breakpoint.
            boolean shrink = compact && getWidth() < 270;
            if (shrink != condensed) {
                condensed = shrink;
                Icon icon = IconAssets.icon(iconResource, shrink ? 28 : 36);
                if (icon != null) emblemLabel.setIcon(icon);
                titleLabel.setFont(UiTheme.displayBold(shrink ? 12 : 14));
                descriptionLabel.setFont(UiTheme.body(Font.BOLD, shrink ? 7 : 8));
            }
        }

        void setSelectionAction(final Runnable action) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if (isEnabled() && action != null) action.run();
                }
            });
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "select");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "select");
            getActionMap().put("select", new AbstractAction() {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent event) {
                    if (isEnabled() && action != null) action.run();
                }
            });
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
            // Enamel plus one accent rim is enough; the previous second bright
            // outline made adjacent selected cards appear misaligned.
            setBorder(new ArtDecoBorder(selected ? accent : UiTheme.BORDER,
                padding, false));
        }

        boolean portraitIsSquare() {
            return image == null || image.getWidth() == image.getHeight();
        }
    }

    /** Vector heraldry keeps race choices identifiable without adding competing portraits. */
    private static final class RaceCrestIcon implements Icon {
        private final String race;
        private final int size;

        RaceCrestIcon(String race, int size) {
            this.race = race;
            this.size = size;
        }

        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }

        public void paintIcon(java.awt.Component component, Graphics graphics,
                              int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            Color accent = raceAccent(race);
            Polygon shield = new Polygon(
                new int[] {5, size - 5, size - 8, size / 2, 8},
                new int[] {5, 5, size - 18, size - 4, size - 18}, 5);
            g.setColor(new Color(25, 20, 15));
            g.fillPolygon(shield);
            g.setStroke(new BasicStroke(2f));
            g.setColor(accent);
            g.drawPolygon(shield);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
            drawSigil(g, race, accent, size);
            g.dispose();
        }

        private static Color raceAccent(String race) {
            if ("Dwarf".equals(race)) return new Color(184, 135, 70);
            if ("Elf".equals(race)) return new Color(105, 176, 112);
            if ("Halfling".equals(race)) return new Color(205, 164, 74);
            return new Color(194, 205, 218);
        }

        private static void drawSigil(Graphics2D g, String race, Color accent,
                                      int size) {
            g.setColor(accent);
            int mid = size / 2;
            if ("Dwarf".equals(race)) {
                g.drawLine(mid - 10, 15, mid + 10, 36);
                g.drawLine(mid + 8, 14, mid - 8, 38);
                g.drawLine(mid - 13, 15, mid - 5, 11);
                g.drawLine(mid + 5, 11, mid + 13, 15);
            } else if ("Elf".equals(race)) {
                g.drawOval(mid - 10, 11, 20, 29);
                g.drawLine(mid - 8, 38, mid + 8, 14);
                g.drawLine(mid - 4, 27, mid + 6, 25);
            } else if ("Halfling".equals(race)) {
                g.drawLine(mid, 11, mid, 40);
                for (int i = 0; i < 3; i++) {
                    int yy = 17 + i * 7;
                    g.drawOval(mid - 9, yy, 8, 5);
                    g.drawOval(mid + 1, yy - 3, 8, 5);
                }
            } else {
                Polygon crown = new Polygon(
                    new int[] {mid - 12, mid - 9, mid - 3, mid, mid + 5,
                        mid + 11, mid + 9, mid - 9},
                    new int[] {34, 18, 26, 13, 26, 18, 34, 34}, 8);
                g.drawPolygon(crown);
                g.drawLine(mid - 10, 38, mid + 10, 38);
            }
        }
    }

    /** Neutral branded header; hero color is intentionally reserved for personal UI. */
    private static final class ArtDecoHeaderPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(UiTheme.GOLD.getRed(), UiTheme.GOLD.getGreen(),
                UiTheme.GOLD.getBlue(), 145));
            g.drawLine(0, height - 5, width, height - 5);
            g.setColor(new Color(UiTheme.GOLD_LIGHT.getRed(),
                UiTheme.GOLD_LIGHT.getGreen(), UiTheme.GOLD_LIGHT.getBlue(), 65));
            g.drawLine(48, height - 8, Math.max(48, width - 48), height - 8);
            g.dispose();
        }
    }

    /** Section title with the compact line-and-diamond language from the mockup. */
    private static final class ArtDecoHeading extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel title;

        ArtDecoHeading(String text) {
            setLayout(new BorderLayout());
            setOpaque(false);
            title = new JLabel(text);
            title.setForeground(UiTheme.GOLD);
            title.setFont(UiTheme.display(38));
            title.setBorder(BorderFactory.createEmptyBorder(0, 42, 0, 26));
            add(title, BorderLayout.WEST);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int middle = Math.max(10, getHeight() / 2);
            g.setColor(new Color(UiTheme.GOLD.getRed(), UiTheme.GOLD.getGreen(),
                UiTheme.GOLD.getBlue(), 155));
            g.drawLine(4, middle, 30, middle);
            g.drawLine(10, middle - 4, 25, middle - 4);
            g.drawLine(10, middle + 4, 25, middle + 4);
            int textEnd = 42 + title.getFontMetrics(title.getFont())
                .stringWidth(title.getText());
            int diamondCenter = Math.min(getWidth() - 16, textEnd + 16);
            Polygon diamond = new Polygon(
                new int[] {diamondCenter - 4, diamondCenter,
                    diamondCenter + 4, diamondCenter},
                new int[] {middle, middle - 4, middle, middle + 4}, 4);
            g.drawPolygon(diamond);
            int lineStart = diamondCenter + 11;
            int available = Math.max(0, getWidth() - 12 - lineStart);
            int lineLength = Math.min(220, available);
            if (lineLength > 10) {
                g.drawLine(lineStart, middle, lineStart + lineLength, middle);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /**
     * Frames the player-authored name without placing ornament behind the copy.
     * The rails shorten around long names and disappear independently when the
     * full 24-character contract needs the available width.
     */
    private static final class ArtDecoProfileNamePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel title;
        private Color metal = UiTheme.GOLD;
        private Color gem = UiTheme.GOLD_LIGHT;

        ArtDecoProfileNamePanel(JLabel title) {
            this.title = title;
            setLayout(new BorderLayout());
            add(title, BorderLayout.CENTER);
        }

        void setAccent(Color metal, Color gem) {
            if (metal != null) this.metal = metal;
            if (gem != null) this.gem = gem;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int middle = Math.max(8, getHeight() / 2 + 2);
            int textWidth = title.getFontMetrics(title.getFont())
                .stringWidth(title.getText() == null ? "" : title.getText());
            int innerLeft = Math.max(0, (width - textWidth) / 2 - 13);
            int innerRight = Math.min(width, (width + textWidth) / 2 + 13);
            int outerInset = 14;

            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(metal.getRed(), metal.getGreen(),
                metal.getBlue(), 145));
            if (innerLeft - outerInset >= 18) {
                paintNameRail(g, outerInset, innerLeft, middle, true);
            }
            if (width - outerInset - innerRight >= 18) {
                paintNameRail(g, innerRight, width - outerInset,
                    middle, false);
            }
            g.dispose();
        }

        private void paintNameRail(Graphics2D g, int start, int end,
                                   int y, boolean leftSide) {
            g.drawLine(start, y, end, y);
            int inner = leftSide ? end : start;
            Polygon diamond = new Polygon(
                new int[] {inner - 4, inner, inner + 4, inner},
                new int[] {y, y - 4, y, y + 4}, 4);
            g.setColor(new Color(gem.getRed(), gem.getGreen(),
                gem.getBlue(), 175));
            g.fillPolygon(diamond);
            g.setColor(new Color(metal.getRed(), metal.getGreen(),
                metal.getBlue(), 205));
            g.drawPolygon(diamond);
        }
    }

    /**
     * Uses additional vertical room for the selected hero on larger displays while
     * preserving the authored 3:4 image ratio and the same masked frame.
     */
    private static final class ResponsiveHeroPortraitHolder extends JPanel {
        private static final long serialVersionUID = 1L;
        private final AssetImagePanel portrait;

        ResponsiveHeroPortraitHolder(AssetImagePanel portrait) {
            this.portrait = portrait;
            setLayout(null);
            add(portrait);
        }

        @Override
        public void doLayout() {
            int availableWidth = Math.max(1, getWidth());
            int availableHeight = Math.max(1, getHeight());
            int height = Math.min(560, availableHeight);
            int width = Math.min(420, (height * 3) / 4);
            if (width > availableWidth) {
                width = availableWidth;
                height = Math.min(560, (width * 4) / 3);
            }
            portrait.setBounds((availableWidth - width) / 2,
                (availableHeight - height) / 2, width, height);
        }
    }

    /** Centers the largest possible square portrait inside a responsive choice card. */
    private static final class SquarePortraitHolder extends JPanel {
        private static final long serialVersionUID = 1L;
        private final AssetImagePanel portrait;

        SquarePortraitHolder(AssetImagePanel portrait) {
            this.portrait = portrait;
            setOpaque(false);
            setLayout(null);
            setPreferredSize(new Dimension(150, 150));
            setMinimumSize(new Dimension(96, 96));
            add(portrait);
        }

        @Override
        public void doLayout() {
            int size = Math.max(1, Math.min(getWidth(), getHeight()));
            portrait.setBounds((getWidth() - size) / 2, (getHeight() - size) / 2,
                size, size);
        }
    }

    /**
     * The selection column always follows the viewport width. A plain JPanel
     * retains its wider preferred width inside JScrollPane, and with horizontal
     * scrolling disabled that silently clips the final race card.
     */
    private static final class VerticalScrollablePanel extends JPanel
            implements Scrollable {
        private static final long serialVersionUID = 1L;

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect,
                                              int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect,
                                               int orientation, int direction) {
            return Math.max(16, visibleRect.height - 32);
        }

        public boolean getScrollableTracksViewportWidth() { return true; }

        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null &&
                getParent().getHeight() > getPreferredSize().height;
        }
    }
}
