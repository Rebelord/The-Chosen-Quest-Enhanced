package thechosenquest.desktop;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

/** Cached access to the shared Figma icon library bundled with the game. */
final class IconAssets {
    static final String WEAPON_SWORD = "/assets/icons/weapon-sword.png";
    static final String ARMOUR_SHIELD = "/assets/icons/armour-shield.png";
    static final String POTION_HEALTH = "/assets/icons/potion-health.png";
    static final String INVENTORY_BACKPACK = "/assets/icons/inventory-backpack.png";
    static final String SERVICE_REST = "/assets/icons/service-rest.png";
    static final String SERVICE_MEAL = "/assets/icons/service-meal.png";
    static final String QUEST_RUMOR = "/assets/icons/quest-rumor.png";
    static final String CURRENCY_COINS = "/assets/icons/currency-coins.png";
    static final String MAGIC_ZAP = "/assets/icons/magic-zap.png";

    static final String HEALTH_POTION = "/assets/icons/items/consumable-health-potion.png";
    static final String REGIONAL_MAP = "/assets/icons/items/quest-regional-map.png";
    static final String UNIDENTIFIED_RELIC = "/assets/icons/items/quest-unidentified-relic.png";
    static final String IDENTIFIED_RELIC = "/assets/icons/items/quest-identified-relic.png";

    private static final Map<String, ImageIcon> CACHE = new HashMap<String, ImageIcon>();
    private static final Map<String, String> ITEM_RESOURCES = new HashMap<String, String>();

    static {
        register("/assets/icons/items/weapon-dagger.png",
            "Dagger", "Offhand Dagger", "Shadowsteel Dirk");
        register("/assets/icons/items/weapon-serrated-dagger.png",
            "Serrated Dagger", "Balanced Offhand Dagger", "Crest Dirk");
        register("/assets/icons/items/weapon-stiletto.png", "Moonlit Dirk");
        register("/assets/icons/items/weapon-arming-sword.png",
            "Short Sword", "Long Sword", "Steel Long Sword");
        register("/assets/icons/items/weapon-greatsword.png",
            "Greatsword", "Tempered Greatsword", "Moonsteel Blade", "Warlord Blade");
        register("/assets/icons/items/weapon-battle-axe.png", "Axe");
        register("/assets/icons/items/weapon-longbow.png",
            "Long Bow", "Ranger Bow", "Moonbow", "Warlord Recurve");
        register("/assets/icons/items/weapon-crossbow.png", "Crossbow", "Hunting Crossbow");
        register("/assets/icons/items/weapon-arcane-staff.png",
            "Oak Staff", "Ashwood Staff", "Runic Battlestaff");
        register("/assets/icons/items/weapon-wand.png", "Apprentice Wand", "Runed Wand", "Moonwand");
        register("/assets/icons/items/weapon-flanged-mace.png", "Iron Mace", "Flanged Mace");
        register("/assets/icons/items/weapon-warhammer.png", "Warhammer");
        register("/assets/icons/items/offhand-round-shield.png", "Iron Shield");
        register("/assets/icons/items/offhand-tome.png", "Apprentice Tome", "Runed Tome");
        register("/assets/icons/items/offhand-enchanted-quiver.png", "Hunter's Quiver");

        register("/assets/icons/items/armour-cloth-robe.png",
            "Cloth Armour", "Mystic Robes", "Oathweave Robes", "Soulglass Robes");
        register("/assets/icons/items/armour-leather.png",
            "Leather Armour", "Reinforced Leather", "Oathcloak", "Veilweave Leather",
            "Spirit Hide");
        register("/assets/icons/items/armour-chainmail.png",
            "Chain Armour", "Scale Armour", "Reinforced Scale", "Warded Chain", "Oathscale");
        register("/assets/icons/items/armour-plate.png",
            "Plate Armour", "Knight Plate", "Oathbound Plate");
    }

    private IconAssets() {}

    private static void register(String resource, String... names) {
        for (String name : names) ITEM_RESOURCES.put(name, resource);
    }

    /**
     * Resolves an item by its serialized display name. Keeping this mapping out
     * of Item preserves compatibility with saves created before artwork existed.
     */
    static String itemResource(GameEngine.Item item) {
        return item == null ? ARMOUR_SHIELD : itemResource(item.name, item.type);
    }

    static String itemResource(String name, String type) {
        String mapped = ITEM_RESOURCES.get(name);
        if (mapped != null) return mapped;
        if ("Weapon".equals(type)) return "/assets/icons/items/weapon-arming-sword.png";
        if ("Offhand".equals(type)) return "/assets/icons/items/offhand-round-shield.png";
        return "/assets/icons/items/armour-chainmail.png";
    }

    static ImageIcon itemIcon(GameEngine.Item item, int size) {
        return icon(itemResource(item), size);
    }

    static ImageIcon icon(String resource, int size) {
        if (resource == null || size <= 0) return null;
        String key = resource + "@" + size;
        if (CACHE.containsKey(key)) return CACHE.get(key);
        URL source = IconAssets.class.getResource(resource);
        if (source == null) return null;
        ImageIcon result = resource.startsWith("/assets/icons/items/")
            ? normalizedItemIcon(source, size)
            : scaledIcon(source, size);
        CACHE.put(key, result);
        return result;
    }

    private static ImageIcon scaledIcon(URL source, int size) {
        ImageIcon original = new ImageIcon(source);
        Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Normalizes artwork by its alpha bounds instead of the source canvas. The
     * generated icon sheets share a 320px canvas, but their painted bounds and
     * centers vary; using those canvases directly makes narrow items appear tiny
     * and off-center beside shields and armour.
     */
    private static ImageIcon normalizedItemIcon(URL source, int size) {
        try {
            BufferedImage image = ImageIO.read(source);
            if (image == null) return scaledIcon(source, size);
            int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) > 8) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) return scaledIcon(source, size);
            int contentWidth = maxX - minX + 1;
            int contentHeight = maxY - minY + 1;
            int padding = Math.max(2, Math.round(size * .08f));
            int available = Math.max(1, size - padding * 2);
            double scale = Math.min((double) available / contentWidth,
                (double) available / contentHeight);
            int width = Math.max(1, (int) Math.round(contentWidth * scale));
            int height = Math.max(1, (int) Math.round(contentHeight * scale));
            int x = (size - width) / 2;
            int y = (size - height) / 2;
            BufferedImage normalized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = normalized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, x, y, x + width, y + height,
                minX, minY, maxX + 1, maxY + 1, null);
            graphics.dispose();
            return new ImageIcon(normalized);
        } catch (IOException exception) {
            return scaledIcon(source, size);
        }
    }
}
