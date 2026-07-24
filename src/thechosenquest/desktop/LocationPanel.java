package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;

/** Shared Figma shop component with merchant, blacksmith, alchemist, and inn variants. */
final class LocationPanel extends JPanel {
    interface Listener {
        void onBuy(Object selection);
        default void onSell(GameEngine.Item item) { }
        void onRest();
        void onMapService(GameEngine.MapService service);
        void onBack();
    }

    private static final long serialVersionUID = 1L;
    private static final Color RAIL = new Color(18, 19, 26);
    private static final Color CATALOG = new Color(10, 12, 17);
    private final Listener listener;
    private final AssetImagePanel artwork = new AssetImagePanel(null, true);
    private final JLabel npcName = new JLabel();
    private final JLabel npcRole = new JLabel();
    private final JLabel quote = new JLabel();
    private final JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final List<JButton> tabButtons = new ArrayList<JButton>();
    private final JLabel coffer = new JLabel();
    private final JPanel npcRail = new JPanel();
    private final JPanel catalog = new JPanel(new BorderLayout(0, 16));
    private final JPanel footer = new JPanel(new BorderLayout());
    private final JPanel wareCards = new JPanel();
    private JScrollBar catalogScrollBar;
    private Color accent = UiTheme.GOLD;
    private GameEngine.State currentState;
    private List<GameEngine.Item> currentEquipment = new ArrayList<GameEngine.Item>();
    private GameEngine.TileType currentHavenTile;
    private boolean currentBlacksmith;
    private String activeTab = "ALL";
    private int visibleWareCount;

    LocationPanel(final Listener listener) {
        this.listener = listener;
        setLayout(new BorderLayout());
        setBackground(CATALOG);
        setPreferredSize(new Dimension(UiTheme.CENTER_WIDTH, UiTheme.BODY_HEIGHT));
        add(buildNpcRail(), BorderLayout.WEST);
        add(buildCatalog(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildNpcRail() {
        npcRail.setLayout(new BoxLayout(npcRail, BoxLayout.Y_AXIS));
        npcRail.setBackground(RAIL);
        npcRail.setPreferredSize(new Dimension(200, 0));
        npcRail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(70, 56, 40)),
            BorderFactory.createEmptyBorder(20, 16, 20, 16)));

        artwork.setPreferredSize(new Dimension(168, 210));
        artwork.setMinimumSize(new Dimension(168, 210));
        artwork.setMaximumSize(new Dimension(168, 210));
        artwork.setAlignmentX(CENTER_ALIGNMENT);
        npcRail.add(artwork);
        npcRail.add(Box.createVerticalStrut(15));
        npcName.setForeground(UiTheme.TEXT);
        npcName.setFont(UiTheme.display(18));
        npcName.setAlignmentX(CENTER_ALIGNMENT);
        npcRail.add(npcName);
        npcRole.setForeground(new Color(202, 167, 110));
        npcRole.setFont(UiTheme.body(Font.PLAIN, 9));
        npcRole.setAlignmentX(CENTER_ALIGNMENT);
        npcRail.add(npcRole);
        npcRail.add(Box.createVerticalStrut(15));

        quote.setForeground(UiTheme.MUTED);
        quote.setFont(UiTheme.display(11));
        quote.setAlignmentX(CENTER_ALIGNMENT);
        quote.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(69, 57, 45)),
            BorderFactory.createEmptyBorder(13, 8, 10, 8)));
        quote.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        npcRail.add(quote);
        npcRail.add(Box.createVerticalGlue());
        return npcRail;
    }

    private JPanel buildCatalog() {
        catalog.setBackground(CATALOG);
        catalog.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        tabBar.setOpaque(false);
        tabBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(65, 53, 40)),
            BorderFactory.createEmptyBorder(4, 0, 12, 0)));
        catalog.add(tabBar, BorderLayout.NORTH);

        wareCards.setLayout(new BoxLayout(wareCards, BoxLayout.Y_AXIS));
        wareCards.setBackground(CATALOG);
        JScrollPane scroll = new JScrollPane(wareCards);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CATALOG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        catalogScrollBar = scroll.getVerticalScrollBar();
        FantasyScrollBarUI.install(catalogScrollBar, UiTheme.GOLD);
        catalog.add(scroll, BorderLayout.CENTER);
        return catalog;
    }

    private JPanel buildFooter() {
        footer.setPreferredSize(new Dimension(0, 80));
        footer.setBackground(RAIL);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 56, 40)),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        coffer.setForeground(new Color(218, 186, 136));
        coffer.setFont(UiTheme.displayBold(16));
        footer.add(coffer, BorderLayout.WEST);
        JButton back = compactButton("RETURN TO QUEST", false);
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBack(); }
        });
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    void showShop(GameEngine.State state, List<GameEngine.Item> equipment) {
        boolean blacksmith = state.blacksmithShops != null &&
            state.blacksmithShops[state.row][state.col];
        boolean sameShop = currentState == state && currentHavenTile == null &&
            currentBlacksmith == blacksmith;
        currentState = state;
        currentEquipment = new ArrayList<GameEngine.Item>(equipment);
        currentHavenTile = null;
        currentBlacksmith = blacksmith;
        if (!sameShop) activeTab = "ALL";
        applyVariant(blacksmith ? new Color(190, 100, 42) : UiTheme.GOLD);
        npcName.setText(blacksmith ? "Gareth" : "Master Barnaby");
        npcRole.setText(blacksmith ? "BLACKSMITH" : "TRADER");
        quote.setText(blacksmith
            ? "<html>“My hammer shapes destiny.<br>What steel do you need?”</html>"
            : "<html>“Welcome, traveler! Fair<br>prices for fair folk.”</html>");
        artwork.setResourceAsync(blacksmith ? "/assets/encounters/npcs/blacksmith.png" :
            "/assets/encounters/npcs/general-merchant.png");
        coffer.setText("PLAYER COFFER:   " + state.gold + " GOLD");
        configureTabs(blacksmith
            ? new String[] {"ALL", "WEAPONS", "ARMOUR", "OFFHAND", "SELL", "SERVICES"}
            : new String[] {"ALL", "WEAPONS", "ARMOUR", "OFFHAND", "SELL", "SUPPLIES"});
        renderShopWares();
    }

    void showHaven(GameEngine.State state, GameEngine.TileType tile) {
        boolean tavern = tile == GameEngine.TileType.TAVERN;
        boolean sameHaven = currentState == state && currentHavenTile == tile;
        currentState = state;
        currentHavenTile = tile;
        if (!sameHaven) activeTab = "ALL";
        applyVariant(tavern ? new Color(173, 115, 65) : new Color(76, 139, 96));
        npcName.setText(tavern ? "Bram" : "Sylara");
        npcRole.setText(tavern ? "INNKEEPER" : "ALCHEMIST");
        quote.setText(tavern
            ? "<html>“Rest your weary bones.<br>A warm meal awaits.”</html>"
            : "<html>“Careful with these vials...<br>some bite back.”</html>");
        artwork.setResourceAsync(tavern ? "/assets/encounters/npcs/innkeeper.png" :
            "/assets/encounters/npcs/alchemist.png");
        String resource = "Mage".equals(state.heroClass)
            ? "     MANA " + state.mana + "/" + state.maxMana
            : ("Fighter".equals(state.heroClass)
                ? "     RAGE " + state.rage + "/" + state.maxRage
                : ("Rogue".equals(state.heroClass)
                    ? "     MOMENTUM " + state.momentum + "/" + state.maxMomentum
                    : ("Hunter".equals(state.heroClass)
                        ? "     FOCUS " + state.focus + "/" + state.maxFocus : "")));
        coffer.setText("HEALTH " + state.health + "/" + state.maxHealth + resource);
        configureTabs(tavern
            ? new String[] {"ALL", "ROOMS", "MEALS", "RUMORS"}
            : new String[] {"ALL", "POTIONS", "INGREDIENTS", "ADVICE"});
        renderHavenWares();
    }

    private void configureTabs(String[] labels) {
        tabBar.removeAll();
        tabButtons.clear();
        boolean valid = false;
        for (String label : labels) if (label.equals(activeTab)) valid = true;
        if (!valid) activeTab = labels[0];
        for (final String label : labels) {
            JButton tab = UiTheme.button(label, false);
            tab.setFont(UiTheme.body(Font.BOLD, 10));
            tab.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            tab.getAccessibleContext().setAccessibleName(label + " shop category");
            UiTheme.setButtonActive(tab, label.equals(activeTab));
            tab.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { selectTab(label); }
            });
            tabButtons.add(tab);
            tabBar.add(tab);
        }
        tabBar.revalidate();
        tabBar.repaint();
    }

    void selectTab(String label) {
        activeTab = label;
        for (JButton tab : tabButtons) UiTheme.setButtonActive(tab, label.equals(tab.getText()));
        if (currentHavenTile == null) renderShopWares(); else renderHavenWares();
    }

    private void renderShopWares() {
        clearWares();
        if ("SELL".equals(activeTab)) {
            renderSellInventory();
            finishWares();
            return;
        }
        if (!currentBlacksmith && ("ALL".equals(activeTab) ||
                "SUPPLIES".equals(activeTab))) {
            addShopRow("Health Potion", "Restores 20 HP", "10 GOLD",
                IconAssets.HEALTH_POTION, "Potion", currentState.gold >= 10, false);
        }
        if ("SERVICES".equals(activeTab)) {
            addServiceRow("Equipment Appraisal", "Compare every item against your equipped gear",
                "INCLUDED", IconAssets.ARMOUR_SHIELD, false);
        }
        if (!currentBlacksmith && ("ALL".equals(activeTab) ||
                "SERVICES".equals(activeTab))) {
            addMapServiceRow("Regional Map",
                "Scouts every road, terrain type, and landmark — threats remain uncertain",
                currentState.regionalMapOwned ? "OWNED" :
                    GameEngine.REGIONAL_MAP_COST + " GOLD",
                IconAssets.REGIONAL_MAP, GameEngine.MapService.REGIONAL_MAP,
                "BUY MAP", !currentState.regionalMapOwned &&
                    currentState.gold >= GameEngine.REGIONAL_MAP_COST);
        }
        if ("ALL".equals(activeTab) || "SERVICES".equals(activeTab)) {
            addRelicRows();
        }
        for (GameEngine.Item item : currentEquipment) {
            if (!GameEngine.vendorOffers(currentBlacksmith, item)) continue;
            if ("WEAPONS".equals(activeTab) && !"Weapon".equals(item.type)) continue;
            if ("ARMOUR".equals(activeTab) && !"Armour".equals(item.type)) continue;
            if ("OFFHAND".equals(activeTab) && !"Offhand".equals(item.type)) continue;
            if ("SUPPLIES".equals(activeTab) || "SERVICES".equals(activeTab)) continue;
            String icon = IconAssets.itemResource(item);
            String restriction = GameEngine.equipmentRestriction(currentState.heroClass, item);
            boolean equipped = isEquipped(item);
            String description = comparisonFor(item, restriction != null);
            if ("Weapon".equals(item.type)) {
                description += "<br><font color='#f1c85c'><b>" +
                    GameEngine.weaponTraitName(item) + "</b></font> · " +
                    GameEngine.weaponTraitDescription(item);
            }
            if (restriction != null) description +=
                "<br><font color='#d76a62'>" + restriction + "</font>";
            addShopRow(item.name, description, item.cost + " GOLD", icon, item,
                currentState.gold >= item.cost && restriction == null && !equipped, equipped);
        }
        finishWares();
    }

    private void renderSellInventory() {
        if (currentState.inventory == null || currentState.inventory.isEmpty()) return;
        for (final GameEngine.Item item : currentState.inventory) {
            boolean equipped = isEquipped(item);
            boolean locked = item.starterItem || item.relicReward || equipped;
            String reason = item.starterItem ? "Starter gear is protected" :
                (item.relicReward ? "Relic-bound equipment cannot be sold" :
                    (equipped ? "Unequip this item before selling" :
                        "Sell unwanted equipment to fund upgrades"));
            JPanel row = wareRow(item.name, reason,
                locked ? GameEngine.itemQuality(item) : "SELL VALUE " + salePricePreview(item) + " GOLD",
                IconAssets.itemResource(item));
            JButton sell = compactButton(locked ? "LOCKED" : "SELL", true);
            sell.setEnabled(!locked);
            sell.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { listener.onSell(item); }
            });
            row.add(sell, BorderLayout.EAST);
            wareCards.add(row);
            wareCards.add(Box.createVerticalStrut(10));
            visibleWareCount++;
        }
    }

    private int salePricePreview(GameEngine.Item item) {
        return GameEngine.salePrice(item, currentBlacksmith);
    }

    private void renderHavenWares() {
        boolean tavern = currentHavenTile == GameEngine.TileType.TAVERN;
        clearWares();
        if ("ALL".equals(activeTab) || "ROOMS".equals(activeTab) ||
                "POTIONS".equals(activeTab)) {
            addServiceRow(tavern ? "Night's Rest" : "Restorative Draught",
                "Mage".equals(currentState.heroClass)
                    ? "Restore health and mana to full"
                    : "Restore health to full",
                "RECOVER",
                tavern ? IconAssets.SERVICE_REST : IconAssets.POTION_HEALTH, true);
        }
        if ("ALL".equals(activeTab) || "MEALS".equals(activeTab) ||
                "INGREDIENTS".equals(activeTab)) {
            addServiceRow(tavern ? "Hot Meal" : "Healing Herbs",
                tavern ? "Warm food before the road" : "Prepared beneath the old forest",
                "INCLUDED", tavern ? IconAssets.SERVICE_MEAL : IconAssets.POTION_HEALTH, false);
        }
        if ("ALL".equals(activeTab) || "RUMORS".equals(activeTab) ||
                "ADVICE".equals(activeTab)) {
            boolean available = currentState.rumorServicesUsed == null ||
                !currentState.rumorServicesUsed[currentState.row][currentState.col];
            addMapServiceRow(tavern ? "Local Rumors" : "Arcane Advice",
                available ? (tavern ? "Scout a rumored danger or relic-bearing elite" :
                    "Reveal an arcane disturbance or relic search region") :
                    (tavern ? "You have heard every useful rumor here" :
                        "This alchemist has shared all available advice"),
                available ? "AVAILABLE ONCE" : "CONSULTED",
                tavern ? IconAssets.QUEST_RUMOR : IconAssets.MAGIC_ZAP,
                GameEngine.MapService.RUMOR, available ? "ASK" : "ASKED", available);
            addRelicRows();
        }
        finishWares();
    }

    private void addRelicRows() {
        if (currentState == null || currentState.relics == null) return;
        for (final GameEngine.Relic relic : currentState.relics) {
            if (relic.identified) continue;
            boolean correctVendor = relic.vendor.equals(currentVendorLabel());
            JPanel row = wareRow(relic.name,
                correctVendor ? "I recognize its markings. Identification is free." :
                    "This requires the " + relic.vendor + ". Keep it safe.",
                "UNIDENTIFIED RELIC", IconAssets.UNIDENTIFIED_RELIC);
            JButton identify = compactButton(correctVendor ? "IDENTIFY" : "SEEK " +
                relic.vendor.toUpperCase(), true);
            identify.setEnabled(correctVendor);
            identify.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { listener.onBuy(relic); }
            });
            row.add(identify, BorderLayout.EAST);
            wareCards.add(row);
            wareCards.add(Box.createVerticalStrut(10));
            visibleWareCount++;
        }
    }

    private String currentVendorLabel() {
        if (currentHavenTile == GameEngine.TileType.TAVERN) return "Innkeeper";
        if (currentHavenTile == GameEngine.TileType.ENCAMPMENT) return "Alchemist";
        return currentBlacksmith ? "Blacksmith" : "General Merchant";
    }

    private boolean isEquipped(GameEngine.Item item) {
        String equippedName = "Weapon".equals(item.type) ? currentState.equippedWeapon :
            ("Offhand".equals(item.type) ? currentState.equippedOffhand : currentState.equippedArmour);
        return item.name.equals(equippedName);
    }

    private String comparisonFor(GameEngine.Item item, boolean restricted) {
        boolean weapon = "Weapon".equals(item.type);
        boolean offhand = "Offhand".equals(item.type);
        int currentBonus = 0;
        String equippedName = weapon ? currentState.equippedWeapon :
            (offhand ? currentState.equippedOffhand : currentState.equippedArmour);
        for (GameEngine.Item owned : currentState.inventory) {
            if (owned.name.equals(equippedName)) {
                currentBonus = GameEngine.itemStat(owned);
                break;
            }
        }
        int candidateBonus = GameEngine.itemStat(item);
        int base = offhand ? 0 : (weapon ? currentState.baseAttack + currentState.level - 1 :
            currentState.baseDefense);
        int currentTotal = base + currentBonus;
        int candidateTotal = base + candidateBonus;
        int delta = candidateTotal - currentTotal;
        String stat = offhand ? "POWER" : (weapon ? "ATK" : "DEF");
        String comparison;
        if (restricted) {
            comparison = "<font color='#d76a62'>LOCKED FOR YOUR CLASS</font>";
        } else if (isEquipped(item)) {
            comparison = "<font color='#d4af37'>CURRENTLY EQUIPPED</font>";
        } else if (delta > 0) {
            comparison = "<font color='#6fce78'>▲ +" + delta + " upgrade</font>";
        } else if (delta < 0) {
            comparison = "<font color='#d76a62'>▼ " + delta + " downgrade</font>";
        } else {
            comparison = "<font color='#a6998c'>— no stat change</font>";
        }
        return "Current " + currentTotal + " " + stat + " → " + candidateTotal +
            " " + stat + " &nbsp; " + comparison;
    }

    private void clearWares() {
        wareCards.removeAll();
        visibleWareCount = 0;
    }

    private void finishWares() {
        wareCards.add(Box.createVerticalGlue());
        wareCards.revalidate();
        wareCards.repaint();
    }

    private void addShopRow(String name, String description, String price, String icon,
                            final Object selection, boolean affordable, boolean equipped) {
        JPanel row = wareRow(name, description, price, icon);
        JButton buy = compactButton(equipped ? "EQUIPPED" : "BUY", true);
        buy.setEnabled(affordable);
        buy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBuy(selection); }
        });
        row.add(buy, BorderLayout.EAST);
        wareCards.add(row);
        wareCards.add(Box.createVerticalStrut(10));
        visibleWareCount++;
    }

    private void addServiceRow(String name, String description, String tag,
                               String iconResource, boolean actionable) {
        JPanel row = wareRow(name, description, tag, iconResource);
        if (actionable) {
            JButton use = compactButton("REST", true);
            use.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { listener.onRest(); }
            });
            row.add(use, BorderLayout.EAST);
        }
        wareCards.add(row);
        wareCards.add(Box.createVerticalStrut(10));
        visibleWareCount++;
    }

    private void addMapServiceRow(String name, String description, String tag,
                                  String iconResource,
                                  final GameEngine.MapService service,
                                  String actionLabel, boolean enabled) {
        JPanel row = wareRow(name, description, tag, iconResource);
        JButton use = compactButton(actionLabel, true);
        use.setEnabled(enabled);
        use.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onMapService(service); }
        });
        row.add(use, BorderLayout.EAST);
        wareCards.add(row);
        wareCards.add(Box.createVerticalStrut(10));
        visibleWareCount++;
    }

    int visibleWareCount() {
        return visibleWareCount;
    }

    private JPanel wareRow(String name, String description, String meta, String iconResource) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(new Color(30, 32, 43));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        row.setPreferredSize(new Dimension(600, 86));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(54, 48, 43)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel symbol = new JLabel("", SwingConstants.CENTER);
        symbol.setIcon(IconAssets.icon(iconResource, 44));
        symbol.setForeground(accent);
        symbol.setFont(UiTheme.display(20));
        symbol.setPreferredSize(new Dimension(38, 38));
        symbol.setOpaque(false);
        row.add(symbol, BorderLayout.WEST);
        JLabel copy = new JLabel("<html><font color='#fff9e5'><b>" + name +
            "</b></font><br><font color='#a6998c'>" + description +
            "</font></html>");
        copy.setFont(UiTheme.body(Font.PLAIN, 11));
        row.add(copy, BorderLayout.CENTER);
        JLabel value = new JLabel(meta);
        value.setForeground(accent);
        value.setFont(UiTheme.body(Font.BOLD, 10));
        value.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        row.add(value, BorderLayout.SOUTH);
        return row;
    }

    private JButton compactButton(String text, boolean primary) {
        JButton button = UiTheme.button(text, primary);
        button.setFont(UiTheme.body(Font.BOLD, 10));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        return button;
    }

    private void applyVariant(Color value) {
        accent = value;
        if (catalogScrollBar != null) FantasyScrollBarUI.install(catalogScrollBar, accent);
        artwork.setBorder(BorderFactory.createLineBorder(accent, 2));
        npcRole.setForeground(accent);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, accent),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        coffer.setForeground(accent);
    }
}
