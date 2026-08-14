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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** Character sheet and inventory list adapted from the Figma inventory screen. */
final class InventoryPanel extends JPanel {
    interface Listener {
        void onEquip(GameEngine.Item item);
        void onBack();
    }

    private static final long serialVersionUID = 1L;
    private final JLabel slots = new JLabel();
    private final JLabel goldSummary = new JLabel();
    private final JLabel relics = new JLabel();
    private final DefaultListModel<GameEngine.Item> items = new DefaultListModel<GameEngine.Item>();
    private final JList<GameEngine.Item> itemList = new JList<GameEngine.Item>(items);
    private static final String[] SORT_MODES = {
        "BEST FIRST", "QUALITY", "NAME A-Z"
    };
    private final JButton sortButton = new JButton();
    private final JButton equipButton;
    private final JButton[] filterButtons = new JButton[4];
    private GameEngine.State currentState;
    private String itemFilter = "ALL";
    private int sortMode;

    InventoryPanel(final Listener listener) {
        setLayout(new BorderLayout(0, 14));
        setBackground(UiTheme.SURFACE_DEEP);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel top = new JPanel(new BorderLayout(0, 3));
        top.setOpaque(false);
        JLabel heading = new JLabel("Inventory");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.display(30));
        top.add(heading, BorderLayout.NORTH);
        JLabel guidance = new JLabel("Choose gear to compare it with the equipped slot shown at left.");
        guidance.setForeground(UiTheme.MUTED);
        guidance.setFont(UiTheme.body(Font.PLAIN, 12));
        top.add(guidance, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(buildInventoryList(), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton back = UiTheme.button("RETURN TO QUEST", false);
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBack(); }
        });
        equipButton = UiTheme.button("EQUIP SELECTED", true);
        equipButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                GameEngine.Item selected = itemList.getSelectedValue();
                if (selected != null) listener.onEquip(selected);
            }
        });
        equipButton.setEnabled(false);
        actions.add(back);
        actions.add(equipButton);
        add(actions, BorderLayout.SOUTH);

        itemList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) refreshEquipAction();
            }
        });
    }

    private JPanel buildInventoryList() {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(10, 8));
        heading.setOpaque(false);
        JLabel title = new JLabel("GEAR");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.body(Font.BOLD, 12));
        slots.setForeground(new Color(217, 204, 184));
        slots.setFont(UiTheme.body(Font.BOLD, 12));
        goldSummary.setForeground(UiTheme.GOLD_LIGHT);
        goldSummary.setFont(UiTheme.body(Font.BOLD, 12));
        goldSummary.setIcon(IconAssets.icon(IconAssets.CURRENCY_PURSE, 24));
        goldSummary.setIconTextGap(7);
        JPanel inventoryMeta = new JPanel(new java.awt.FlowLayout(
            java.awt.FlowLayout.RIGHT, 14, 0));
        inventoryMeta.setOpaque(false);
        inventoryMeta.add(slots);
        inventoryMeta.add(goldSummary);
        heading.add(title, BorderLayout.WEST);
        heading.add(inventoryMeta, BorderLayout.EAST);
        heading.add(buildInventoryControls(), BorderLayout.SOUTH);
        block.add(heading, BorderLayout.NORTH);

        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setBackground(UiTheme.SURFACE_DEEP);
        itemList.setCellRenderer(new ItemRenderer());
        itemList.setFixedCellHeight(68);
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
        relics.setPreferredSize(new Dimension(0, 64));
        block.add(relics, BorderLayout.SOUTH);
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
        sortButton.setText("SORT: " + SORT_MODES[sortMode]);
        sortButton.setFont(UiTheme.body(Font.BOLD, 10));
        UiTheme.applyButtonStyle(sortButton, UiTheme.ButtonStyle.SECONDARY, 6, 12);
        sortButton.setToolTipText("Cycle inventory sort order");
        sortButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                sortMode = (sortMode + 1) % SORT_MODES.length;
                sortButton.setText("SORT: " + SORT_MODES[sortMode]);
                rebuildItems();
            }
        });
        controls.add(sortButton, BorderLayout.EAST);
        return controls;
    }

    void setState(GameEngine.State state, int attack, int defense) {
        currentState = state;
        int relicCount = state.relics == null ? 0 : state.relics.size();
        slots.setText(state.inventory.size() + " equipment · " + relicCount + " relics");
        goldSummary.setText(state.gold + " GOLD");
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
        relics.setVisible(relicCount > 0);

        rebuildItems();
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
        final String sortName = SORT_MODES[sortMode];
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
        refreshEquipAction();
        itemList.repaint();
    }

    private void refreshEquipAction() {
        GameEngine.Item item = itemList.getSelectedValue();
        if (item == null || currentState == null) {
            equipButton.setText("SELECT AN ITEM");
            equipButton.setEnabled(false);
            return;
        }
        boolean equipped = item.name.equals(currentState.equippedWeapon) ||
            item.name.equals(currentState.equippedArmour) ||
            item.name.equals(currentState.equippedOffhand);
        String restriction = GameEngine.equipmentRestriction(currentState.heroClass, item);
        int delta = comparisonDelta(item);
        if (equipped) {
            equipButton.setText("CURRENTLY EQUIPPED");
            equipButton.setEnabled(false);
        } else if (restriction != null) {
            equipButton.setText("CLASS LOCKED");
            equipButton.setEnabled(false);
        } else {
            equipButton.setText(delta > 0 ? "EQUIP UPGRADE  +" + delta : "EQUIP SELECTED");
            equipButton.setEnabled(true);
        }
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

    String relicSummaryForTest() {
        return relics.getText();
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
                BorderFactory.createMatteBorder(0, 0, 6, 0, UiTheme.SURFACE_DEEP),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? UiTheme.GOLD : qualityColor,
                        selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12))));
            setIcon(IconAssets.itemIcon(item, 40));
            String bonus = item.attack > 0 && item.defense > 0
                ? "+" + item.attack + " ATK / +" + item.defense + " DEF"
                : (item.attack > 0 ? "+" + item.attack + " ATK" : "+" + item.defense + " DEF");
            String tag = equipped ? "EQUIPPED" : (restricted ? "CLASS LOCKED" : "EQUIP");
            String description = restricted
                ? GameEngine.equipmentRestriction(currentState.heroClass, item)
                : ("Weapon".equals(item.type) ? GameEngine.weaponTraitName(item) + " — " +
                    GameEngine.weaponTraitDescription(item) : "Adventure equipment");
            setToolTipText(description);
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
                "<font color='#d4af37'>" + bonus + "</font> &nbsp;&nbsp; " + comparison +
                "</html>");
            return this;
        }
    }
}
