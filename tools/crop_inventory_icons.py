#!/usr/bin/env python3
"""Export the three 4x4 inventory sheets into stable individual game assets.

The generated manifest is the source of truth shared by the desktop UI and the
Figma asset-library workflow. Re-running this script is safe and deterministic.
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SHEET_DIR = ROOT / "assets" / "icons" / "inventory-sheets"
OUTPUT_DIR = ROOT / "assets" / "icons" / "items"
CELL_SIZE = 320


SHEETS = {
    "inventory-weapons-4x4.png": [
        ("weapon/arming-sword", "Arming Sword"),
        ("weapon/battle-axe", "Battle Axe"),
        ("weapon/flanged-mace", "Flanged Mace"),
        ("weapon/warhammer", "Warhammer"),
        ("weapon/greatsword", "Greatsword"),
        ("weapon/greataxe", "Greataxe"),
        ("weapon/maul", "Maul"),
        ("weapon/spear", "Spear"),
        ("weapon/arcane-staff", "Arcane Staff"),
        ("weapon/wand", "Wand"),
        ("offhand/grimoire", "Grimoire"),
        ("weapon/dagger", "Dagger"),
        ("weapon/stiletto", "Stiletto"),
        ("weapon/serrated-dagger", "Serrated Dagger"),
        ("weapon/parrying-dagger", "Parrying Dagger"),
        ("weapon/shortbow", "Shortbow"),
    ],
    "inventory-offhand-armour-4x4.png": [
        ("weapon/longbow", "Longbow"),
        ("weapon/crossbow", "Crossbow"),
        ("offhand/round-shield", "Round Shield"),
        ("offhand/kite-shield", "Kite Shield"),
        ("offhand/tower-shield", "Tower Shield"),
        ("offhand/tome", "Tome"),
        ("offhand/elemental-totem", "Elemental Totem"),
        ("offhand/warding-totem", "Warding Totem"),
        ("offhand/light-quiver", "Light Quiver"),
        ("offhand/war-quiver", "War Quiver"),
        ("offhand/enchanted-quiver", "Enchanted Quiver"),
        ("armour/cloth-robe", "Cloth Robe"),
        ("armour/leather", "Leather Armour"),
        ("armour/chainmail", "Chainmail"),
        ("armour/plate", "Plate Armour"),
        ("offhand/rogue-buckler", "Rogue Buckler"),
    ],
    "inventory-consumables-quest-4x4.png": [
        ("consumable/health-potion", "Health Potion"),
        ("consumable/mana-potion", "Mana Potion"),
        ("consumable/antidote", "Antidote"),
        ("consumable/combat-tonic", "Combat Tonic"),
        ("quest/unidentified-relic", "Unidentified Relic"),
        ("quest/identified-relic", "Identified Relic"),
        ("quest/regional-map", "Regional Map"),
        ("quest/rumor-note", "Rumor Note"),
        ("currency/coin-purse", "Coin Purse"),
        ("tool/lockpicks", "Lockpicks"),
        ("consumable/spell-scroll", "Spell Scroll"),
        ("consumable/travel-ration", "Travel Ration"),
        ("material/mana-crystal", "Mana Crystal"),
        ("material/enchantment-rune", "Enchantment Rune"),
        ("tool/repair-kit", "Repair Toolkit"),
        ("material/dragon-scale", "Dragon Scale"),
    ],
}


def export() -> list[dict[str, object]]:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    manifest: list[dict[str, object]] = []
    for sheet_name, entries in SHEETS.items():
        sheet = Image.open(SHEET_DIR / sheet_name).convert("RGBA")
        expected = (CELL_SIZE * 4, CELL_SIZE * 4)
        if sheet.size != expected:
            raise ValueError(f"{sheet_name} is {sheet.size}; expected {expected}")
        for index, (asset_key, display_name) in enumerate(entries):
            row, column = divmod(index, 4)
            crop = sheet.crop((
                column * CELL_SIZE,
                row * CELL_SIZE,
                (column + 1) * CELL_SIZE,
                (row + 1) * CELL_SIZE,
            ))
            filename = asset_key.replace("/", "-") + ".png"
            crop.save(OUTPUT_DIR / filename, optimize=True)
            manifest.append({
                "key": asset_key,
                "name": display_name,
                "category": asset_key.split("/", 1)[0],
                "resource": "/assets/icons/items/" + filename,
                "sourceSheet": sheet_name,
                "cell": {"row": row, "column": column},
                "size": {"width": CELL_SIZE, "height": CELL_SIZE},
            })
    return manifest


if __name__ == "__main__":
    records = export()
    manifest_path = OUTPUT_DIR / "inventory-icons.json"
    manifest_path.write_text(json.dumps(records, indent=2) + "\n", encoding="utf-8")
    print(f"Exported {len(records)} icons to {OUTPUT_DIR}")
    print(f"Wrote {manifest_path}")
