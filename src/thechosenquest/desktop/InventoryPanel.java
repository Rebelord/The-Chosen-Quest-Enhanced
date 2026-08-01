package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

/** Character sheet and inventory list adapted from the Figma inventory screen. */
final class InventoryPanel extends JPanel {
    interface Listener {
        void onEquip(GameEngine.Item item);
        void onBack();
    }

    private static final long serialVersionUID = 1L;
    private final JLabel identity = new JLabel();
    private final JLabel experienceCopy = new JLabel();
    private final JProgressBar experience = new JProgressBar();
    private final JLabel weapon = new JLabel();
    private final JLabel armour = new JLabel();
    private final JLabel offhand = new JLabel();
    private JLabel weaponArtwork;
    private JLabel armourArtwork;
    private JLabel offhandArtwork;
    private JLabel offhandTag;
    private final JLabel slots = new JLabel();
    private final JLabel relics = new JLabel();
    private final JLabel[] abilityNames = new JLabel[4];
    private final JLabel[] abilityStates = new JLabel[4];
    private final JLabel[] statValues = new JLabel[6];
    private final JLabel[] statLabels = new JLabel[6];
    private final DefaultListModel<GameEngine.Item> items = new DefaultListModel<GameEngine.Item>();
    private final JList<GameEngine.Item> itemList = new JList<GameEngine.Item>(items);
    private final JComboBox<String> sort = new JComboBox<String>(new String[] {
        "BEST FIRST", "QUALITY", "NAME A-Z"
    });
    private final JButton[] filterButtons = new JButton[4];
    private GameEngine.State currentState;
    private String itemFilter = "ALL";

    InventoryPanel(final Listener listener) {
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.SURFACE_DEEP);
        setBorder(BorderFactory.createEmptyBorder(26, 32, 24, 32));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        JLabel heading = new JLabel("Character & Inventory");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.display(30));
        top.add(heading, BorderLayout.NORTH);
        top.add(buildStatistics(), BorderLayout.CENTER);

        JPanel level = new JPanel(new BorderLayout(10, 5));
        level.setOpaque(false);
        identity.setForeground(UiTheme.TEXT);
        identity.setFont(UiTheme.body(Font.BOLD, 12));
        experienceCopy.setForeground(new Color(217, 204, 184));
        experienceCopy.setFont(UiTheme.body(Font.BOLD, 12));
        experienceCopy.setHorizontalAlignment(SwingConstants.RIGHT);
        level.add(identity, BorderLayout.WEST);
        level.add(experienceCopy, BorderLayout.EAST);
        experience.setForeground(new Color(255, 191, 0));
        experience.setBackground(new Color(25, 20, 17));
        experience.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        experience.setPreferredSize(new Dimension(0, 11));
        level.add(experience, BorderLayout.SOUTH);
        top.add(level, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        JPanel characterOverview = new JPanel(new BorderLayout(0, 12));
        characterOverview.setOpaque(false);
        characterOverview.add(buildEquipped(), BorderLayout.NORTH);
        characterOverview.add(buildAbilities(), BorderLayout.CENTER);
        body.add(characterOverview, BorderLayout.NORTH);
        body.add(buildInventoryList(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton back = UiTheme.button("RETURN TO QUEST", false);
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBack(); }
        });
        JButton equip = UiTheme.button("EQUIP SELECTED", true);
        equip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                GameEngine.Item selected = itemList.getSelectedValue();
                if (selected != null) listener.onEquip(selected);
            }
        });
        actions.add(back);
        actions.add(equip);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel buildStatistics() {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        JLabel title = new JLabel("FULL STATISTICS");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.body(Font.BOLD, 12));
        block.add(title, BorderLayout.NORTH);
        JPanel stats = new JPanel(new GridLayout(1, 6, 10, 0));
        stats.setOpaque(false);
        String[] labels = {"ATK", "DEF", "HEALTH", "MANA", "GOLD", "POTIONS"};
        for (int i = 0; i < labels.length; i++) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(38, 28, 23));
            card.setBorder(BorderFactory.createLineBorder(new Color(212, 175, 55)));
            JLabel label = new JLabel(labels[i], SwingConstants.CENTER);
            statLabels[i] = label;
            label.setForeground(UiTheme.MUTED);
            label.setFont(UiTheme.body(Font.BOLD, 9));
            label.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
            statValues[i] = new JLabel("0", SwingConstants.CENTER);
            statValues[i].setForeground(UiTheme.TEXT);
            statValues[i].setFont(UiTheme.display(18));
            statValues[i].setBorder(BorderFactory.createEmptyBorder(0, 2, 7, 2));
            card.add(label, BorderLayout.NORTH);
            card.add(statValues[i], BorderLayout.CENTER);
            stats.add(card);
        }
        block.add(stats, BorderLayout.CENTER);
        return block;
    }

    private JPanel buildEquipped() {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        JLabel heading = new JLabel("Equipped");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.display(22));
        block.add(heading, BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.add(equippedCard("W", "WEAPON", weapon, true));
        cards.add(equippedCard("A", "ARMOUR", armour, false));
        cards.add(equippedCard("O", "OFFHAND", offhand, false, true));
        block.add(cards, BorderLayout.CENTER);
        return block;
    }

    private JPanel equippedCard(String fallback, String slot, JLabel value,
                                boolean weaponSlot) {
        return equippedCard(fallback, slot, value, weaponSlot, false);
    }

    private JPanel equippedCard(String fallback, String slot, JLabel value,
                                boolean weaponSlot, boolean offhandSlot) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(new Color(42, 30, 24));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.GOLD, 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel icon = new JLabel(fallback, SwingConstants.CENTER);
        if (weaponSlot) weaponArtwork = icon;
        else if (offhandSlot) offhandArtwork = icon;
        else armourArtwork = icon;
        icon.setForeground(UiTheme.TEXT);
        icon.setFont(UiTheme.display(22));
        icon.setPreferredSize(new Dimension(42, 42));
        icon.setOpaque(false);
        value.setForeground(UiTheme.TEXT);
        value.setFont(UiTheme.display(14));
        value.putClientProperty("thechosenquest.equipment.slot", slot);
        card.add(icon, BorderLayout.WEST);
        card.add(value, BorderLayout.CENTER);
        JLabel tag = new JLabel("EQUIPPED");
        if (offhandSlot) offhandTag = tag;
        tag.setForeground(new Color(31, 25, 14));
        tag.setBackground(UiTheme.GOLD);
        tag.setOpaque(true);
        tag.setFont(UiTheme.body(Font.BOLD, 8));
        tag.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        card.add(tag, BorderLayout.EAST);
        return card;
    }

    private JPanel buildInventoryList() {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(10, 8));
        heading.setOpaque(false);
        JLabel title = new JLabel("Inventory");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.display(22));
        slots.setForeground(new Color(217, 204, 184));
        slots.setFont(UiTheme.body(Font.BOLD, 12));
        heading.add(title, BorderLayout.WEST);
        heading.add(slots, BorderLayout.EAST);
        heading.add(buildInventoryControls(), BorderLayout.SOUTH);
        block.add(heading, BorderLayout.NORTH);

        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setBackground(UiTheme.SURFACE_DEEP);
        itemList.setCellRenderer(new ItemRenderer());
        itemList.setFixedCellHeight(82);
        JScrollPane scroll = new JScrollPane(itemList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.SURFACE_DEEP);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        block.add(scroll, BorderLayout.CENTER);
        relics.setOpaque(true);
        relics.setBackground(new Color(31, 24, 40));
        relics.setForeground(UiTheme.TEXT);
        relics.setFont(UiTheme.body(Font.PLAIN, 11));
        relics.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(139, 96, 190)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        relics.setPreferredSize(new Dimension(0, 78));
        block.add(relics, BorderLayout.SOUTH);
        return block;
    }

    private JPanel buildAbilities() {
        JPanel block = new JPanel(new BorderLayout(0, 7));
        block.setOpaque(false);
        JLabel heading = new JLabel("Ability Progression");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.display(18));
        block.add(heading, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.setOpaque(false);
        for (int i = 0; i < abilityNames.length; i++) {
            JPanel card = new JPanel(new BorderLayout(0, 3));
            card.setBackground(new Color(34, 26, 31));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            abilityNames[i] = new JLabel("—");
            abilityNames[i].setForeground(UiTheme.TEXT);
            abilityNames[i].setFont(UiTheme.body(Font.BOLD, 11));
            abilityStates[i] = new JLabel("LOCKED");
            abilityStates[i].setForeground(UiTheme.MUTED);
            abilityStates[i].setFont(UiTheme.body(Font.BOLD, 9));
            card.add(abilityNames[i], BorderLayout.CENTER);
            card.add(abilityStates[i], BorderLayout.SOUTH);
            cards.add(card);
        }
        block.add(cards, BorderLayout.CENTER);
        return block;
    }

    private JPanel buildInventoryControls() {
        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        JPanel filters = new JPanel(new GridLayout(1, 4, 6, 0));
        filters.setOpaque(false);
        String[] names = {"ALL", "WEAPONS", "ARMOUR", "OFFHAND"};
        for (int i = 0; i < names.length; i++) {
            final String filter = names[i];
            final JButton button = new JButton(filter);
            button.setFont(UiTheme.body(Font.BOLD, 10));
            UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.SECONDARY, 6, 12);
            filterButtons[i] = button;
            UiTheme.setButtonActive(button, i == 0);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    itemFilter = filter;
                    for (int j = 0; j < filterButtons.length; j++) {
                        UiTheme.setButtonActive(filterButtons[j], filterButtons[j] == button);
                    }
                    rebuildItems();
                }
            });
            filters.add(button);
        }
        controls.add(filters, BorderLayout.WEST);
        sort.setFont(UiTheme.body(Font.BOLD, 10));
        sort.setForeground(UiTheme.TEXT);
        sort.setBackground(new Color(43, 31, 24));
        sort.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { rebuildItems(); }
        });
        controls.add(sort, BorderLayout.EAST);
        return controls;
    }

    void setState(GameEngine.State state, int attack, int defense) {
        currentState = state;
        identity.setText("Level " + state.level + " " + state.race + " " + state.heroClass);
        experienceCopy.setText(state.experience + "/" + (state.level * 30) + " XP");
        experience.setMaximum(Math.max(1, state.level * 30));
        experience.setValue(state.experience);
        String resourceLabel = "Mage".equals(state.heroClass) ? "MANA" :
            ("Fighter".equals(state.heroClass) ? "RAGE" :
            ("Rogue".equals(state.heroClass) ? "MOMENTUM" :
            ("Hunter".equals(state.heroClass) ? "FOCUS" : "RESOURCE")));
        String resourceValue = "Mage".equals(state.heroClass)
            ? state.mana + "/" + state.maxMana :
            ("Fighter".equals(state.heroClass)
                ? state.rage + "/" + state.maxRage :
            ("Rogue".equals(state.heroClass)
                ? state.momentum + "/" + state.maxMomentum :
            ("Hunter".equals(state.heroClass)
                ? state.focus + "/" + state.maxFocus : "—")));
        statLabels[3].setText(resourceLabel);
        String[] values = {String.valueOf(attack), String.valueOf(defense),
            state.health + "/" + state.maxHealth, resourceValue,
            String.valueOf(state.gold), String.valueOf(state.potions)};
        for (int i = 0; i < values.length; i++) statValues[i].setText(values[i]);
        GameEngine.Item equippedWeapon = GameEngine.equippedWeapon(state);
        String proficiency = equippedWeapon == null ? "UNARMED" :
            GameEngine.proficiencyLabel(GameEngine.equippedWeaponProficiency(state));
        String trait = equippedWeapon == null ? "No weapon trait" :
            GameEngine.weaponTraitName(equippedWeapon);
        weapon.setText(equippedSlotText("WEAPON", state.equippedWeapon,
            state.equippedWeapon == null ? null : trait + " · " + proficiency));
        armour.setText(equippedSlotText("ARMOUR", state.equippedArmour,
            state.equippedArmour == null ? null : "Currently equipped"));
        offhand.setText(equippedSlotText("OFFHAND", state.equippedOffhand,
            state.equippedOffhand == null ? null : "Currently equipped"));
        if (offhandTag != null) offhandTag.setText(state.equippedOffhand == null ? "EMPTY" : "EQUIPPED");
        refreshAbilities(state);
        updateEquippedArtwork(weaponArtwork, state.equippedWeapon, "Weapon", "W");
        updateEquippedArtwork(armourArtwork, state.equippedArmour, "Armour", "A");
        updateEquippedArtwork(offhandArtwork, state.equippedOffhand, "Offhand", "O");
        int relicCount = state.relics == null ? 0 : state.relics.size();
        slots.setText(state.inventory.size() + " equipment · " + relicCount + " relics");
        StringBuilder relicCopy = new StringBuilder(
            "<html><font color='#d4af37'><b>QUEST RELICS</b></font> &nbsp; ");
        if (relicCount == 0) {
            relicCopy.append("<font color='#a6998c'>No elite relics discovered.</font>");
        } else {
            for (int i = 0; i < state.relics.size(); i++) {
                GameEngine.Relic relic = state.relics.get(i);
                if (i > 0) relicCopy.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                relicCopy.append("<font color='")
                    .append(relic.identified ? "#6fce78" : "#c795ff")
                    .append("'><b>").append(relic.name).append("</b></font> — ")
                    .append(relic.identified ? "Identified: " + relic.rewardName :
                        "Unidentified · Bring to " + relic.vendor);
            }
        }
        relicCopy.append("</html>");
        relics.setText(relicCopy.toString());

        rebuildItems();
    }

    private void refreshAbilities(GameEngine.State state) {
        abilityNames[0].setText("Core Training");
        abilityStates[0].setText("LEVEL 1 · READY");
        abilityStates[0].setForeground(new Color(111, 206, 120));
        for (int slot = 1; slot <= 3; slot++) {
            boolean unlocked = GameEngine.abilityUnlocked(state, slot);
            abilityNames[slot].setText(GameEngine.abilityName(state, slot));
            abilityStates[slot].setText(unlocked ? "READY · " +
                GameEngine.abilityTempoLabel(state, slot) :
                GameEngine.abilityRequirement(state, slot));
            abilityStates[slot].setForeground(unlocked
                ? new Color(111, 206, 120) : UiTheme.MUTED);
        }
    }

    private void updateEquippedArtwork(JLabel target, String name, String type,
                                       String fallback) {
        if (target == null) return;
        javax.swing.ImageIcon artwork = name == null ? IconAssets.emptySlotIcon(42) :
            IconAssets.icon(IconAssets.itemResource(name, type), 42);
        target.setIcon(artwork);
        target.setText(artwork == null ? fallback : "");
        target.setToolTipText(name == null ? type + " slot is empty" : name);
    }

    private String equippedSlotText(String slot, String name, String detail) {
        StringBuilder text = new StringBuilder("<html><font color='#a6998c'>")
            .append(slot).append("</font>");
        if (name != null && !name.trim().isEmpty()) {
            text.append("<br><b>").append(name).append("</b>");
            if (detail != null && !detail.isEmpty()) {
                text.append("<br><font color='#d4af37'>")
                    .append(detail).append("</font>");
            }
        } else {
            text.append("<br>&nbsp;");
        }
        return text.append("</html>").toString();
    }

    private void rebuildItems() {
        if (currentState == null) return;
        GameEngine.Item selected = itemList.getSelectedValue();
        ArrayList<GameEngine.Item> visible = new ArrayList<GameEngine.Item>();
        for (GameEngine.Item item : currentState.inventory) {
            if ("WEAPONS".equals(itemFilter) && !"Weapon".equals(item.type)) continue;
            if ("ARMOUR".equals(itemFilter) && !"Armour".equals(item.type)) continue;
            if ("OFFHAND".equals(itemFilter) && !"Offhand".equals(item.type)) continue;
            visible.add(item);
        }
        final String sortName = String.valueOf(sort.getSelectedItem());
        Collections.sort(visible, new Comparator<GameEngine.Item>() {
            public int compare(GameEngine.Item left, GameEngine.Item right) {
                if ("NAME A-Z".equals(sortName)) return left.name.compareToIgnoreCase(right.name);
                if ("QUALITY".equals(sortName)) {
                    int tier = GameEngine.itemQualityRank(right) - GameEngine.itemQualityRank(left);
                    return tier != 0 ? tier : left.name.compareToIgnoreCase(right.name);
                }
                boolean leftLocked = GameEngine.equipmentRestriction(currentState.heroClass, left) != null;
                boolean rightLocked = GameEngine.equipmentRestriction(currentState.heroClass, right) != null;
                if (leftLocked != rightLocked) return leftLocked ? 1 : -1;
                int delta = comparisonDelta(right) - comparisonDelta(left);
                if (delta != 0) return delta;
                int tier = GameEngine.itemQualityRank(right) - GameEngine.itemQualityRank(left);
                if (tier != 0) return tier;
                int value = GameEngine.itemStat(right) - GameEngine.itemStat(left);
                return value != 0 ? value : left.name.compareToIgnoreCase(right.name);
            }
        });
        items.clear();
        for (GameEngine.Item item : visible) items.addElement(item);
        if (selected != null && visible.contains(selected)) itemList.setSelectedValue(selected, true);
        if (itemList.getSelectedIndex() < 0 && !items.isEmpty()) itemList.setSelectedIndex(0);
        itemList.repaint();
    }

    private int comparisonDelta(GameEngine.Item item) {
        if (currentState == null || item == null) return 0;
        String equippedName = "Weapon".equals(item.type) ? currentState.equippedWeapon :
            ("Offhand".equals(item.type) ? currentState.equippedOffhand : currentState.equippedArmour);
        int equippedValue = 0;
        for (GameEngine.Item owned : currentState.inventory) {
            if (owned.name.equals(equippedName)) {
                equippedValue = GameEngine.itemStat(owned);
                break;
            }
        }
        return GameEngine.itemStat(item) - equippedValue;
    }

    private String colorHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }

    String relicSummaryForTest() {
        return relics.getText();
    }

    String abilitySummaryForTest() {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < abilityNames.length; i++) {
            if (i > 0) summary.append(" | ");
            summary.append(abilityNames[i].getText()).append(":")
                .append(abilityStates[i].getText());
        }
        return summary.toString();
    }

    static String heroAsset(String race, String heroClass) {
        return CharacterArt.fullBody(race, heroClass,
            CharacterArt.defaultGender(race, heroClass));
    }

    private final class ItemRenderer extends JLabel implements ListCellRenderer<GameEngine.Item> {
        private static final long serialVersionUID = 1L;

        ItemRenderer() {
            setOpaque(true);
            setFont(UiTheme.display(14));
            setIconTextGap(12);
        }

        public Component getListCellRendererComponent(JList<? extends GameEngine.Item> list,
                GameEngine.Item item, int index, boolean selected, boolean focused) {
            boolean equipped = currentState != null &&
                (item.name.equals(currentState.equippedWeapon) ||
                 item.name.equals(currentState.equippedArmour) ||
                 item.name.equals(currentState.equippedOffhand));
            boolean restricted = currentState != null &&
                GameEngine.equipmentRestriction(currentState.heroClass, item) != null;
            String quality = GameEngine.itemQuality(item);
            Color qualityColor = UiTheme.qualityColor(quality);
            setBackground(selected ? new Color(72, 52, 34) : new Color(39, 29, 24));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 10, 0, UiTheme.SURFACE_DEEP),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? UiTheme.GOLD : qualityColor,
                        selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(7, 14, 7, 14))));
            setIcon(IconAssets.itemIcon(item, 46));
            String bonus = item.attack > 0 && item.defense > 0
                ? "+" + item.attack + " ATK / +" + item.defense + " DEF"
                : (item.attack > 0 ? "+" + item.attack + " ATK" : "+" + item.defense + " DEF");
            String tag = equipped ? "EQUIPPED" : (restricted ? "CLASS LOCKED" : "EQUIP");
            String description = restricted
                ? GameEngine.equipmentRestriction(currentState.heroClass, item)
                : ("Weapon".equals(item.type)
                    ? "<font color='#f1c85c'><b>" + GameEngine.weaponTraitName(item) +
                        "</b></font> · " + GameEngine.weaponTraitDescription(item)
                    : "Adventure equipment");
            int delta = comparisonDelta(item);
            String comparison = equipped
                ? "<font color='#d4af37'><b>CURRENTLY EQUIPPED</b></font>"
                : restricted
                    ? "<font color='#d76a62'><b>LOCKED FOR YOUR CLASS</b></font>"
                    : delta > 0
                        ? "<font color='#6fce78'><b>▲ +" + delta + " upgrade</b></font>"
                        : delta < 0
                            ? "<font color='#d76a62'><b>▼ " + delta + " downgrade</b></font>"
                            : "<font color='#a6998c'>— no stat change</font>";
            String qualityHex = colorHex(qualityColor);
            setText("<html><font color='" + qualityHex + "'><b>" + item.name + "</b></font>" +
                "&nbsp;&nbsp;<font color='" + qualityHex + "'>[" + quality + "]</font>" +
                "&nbsp;&nbsp;<font color='#d4af37'>[" + tag + "]</font><br>" +
                "<font color='#a6998c'>" + description + "</font><br>" +
                "<font color='#d4af37'>" + bonus + "</font> &nbsp;&nbsp; " + comparison +
                " &nbsp;&nbsp;<font color='#a6998c'>" + item.cost + " gold</font></html>");
            return this;
        }
    }
}
