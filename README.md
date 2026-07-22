# The Chosen Quest — Enhanced Edition

A standalone enhanced desktop adaptation of the original CIT 260 console RPG.
This repository contains only the Enhanced Edition; the original source project
and frozen Classic desktop snapshot remain separate and are not modified by
Enhanced development.

The Enhanced edition follows the current Figma direction and includes an illustrated
title screen, visual character creation with race/class artwork, a three-column
exploration shell with a painted tile map and consistent location information,
player and enemy markers, health/mana/experience meters, and illustrated encounter
cards with enemy health, threat details, rewards, a live battle log, and complete
combat controls. Inventory, merchants, blacksmiths, alchemists, and inns use the
matching card-based layouts from the latest Figma screens.
Equipment, consumables, quest relics, tools, currency, and crafting materials
share a 48-piece painted inventory-art library. Run
`python3 tools/crop_inventory_icons.py` after replacing a master sheet to
regenerate the individual PNGs and `assets/icons/items/inventory-icons.json`.
Inventory, merchants, taverns, and encampments use illustrated in-game panels so
equipment, purchases, and recovery remain part of the main adventure view.
Combat controls enable only when their actions are available, damage produces a
brief visual flash, and victory or defeat opens a dedicated illustrated ending.
The project-bound fantasy background is stored at `assets/quest-background.png`
and all project assets under `assets/` are bundled into the runnable JAR by `build.sh`.
Cinzel and Cormorant Garamond are bundled under `assets/fonts/` with their SIL Open
Font License files so display typography remains consistent on every platform.
If a transparent `assets/title-logo.png` is supplied, the title screen uses it
automatically; otherwise it renders the title with the bundled Cinzel font.
The title screen loops the CC0 track “The Old Tower Inn” by RandomMind. Music has
its own volume control under Game Settings → Audio and automatically pauses when
leaving the title screen, muting the game, or moving focus away from the game.
Source and license provenance are retained in `assets/audio/README.txt`.
Combat attacks use cached, transparent prototype sprite sequences documented in
`assets/vfx/README.md`. Those CC0 effects deliberately preserve the rendering
contract while a custom painterly VFX set is produced for the final portfolio build.

## Requirements

- Java Development Kit (JDK) 8 or newer to build and run the JAR
- JDK 14 or newer only when creating a native application with `jpackage`

## Run

```sh
./build.sh
java -jar build/TheChosenQuest-Desktop.jar
```

Run the engine smoke tests, deterministic regression suite, and visual render tests
with `./test.sh`.

## Play

Use the arrow keys or compass buttons to explore. Travel fully reveals the current
tile and scouts nearby terrain; Hunters scout one tile farther. Tavern rumors and
alchemist advice mark uncertain threats and possible relic search regions. The
rumor and advice consultations are each available once per location per quest,
and their shop cards change to `CONSULTED` after use. The
General Merchant sells a regional map that scouts every terrain tile and landmark
without revealing the creatures that roam there. Hover a tile for its current
discovery and clue details. An enemy blocks movement until it is defeated or you
flee.

Create a Human, Dwarf, Elf, or Halfling hero and choose the Fighter, Mage, Rogue,
or Hunter class. Each class now offers two starting loadouts that establish a
play style while keeping all starting equipment at Common quality. Each combination
has different health, combat bonuses, gold, and magical ability. Standard enemies
can occasionally drop class-compatible Common or Uncommon equipment. Shops focus
on useful upgrades and include a Sell category; equipped, relic-bound, and starter
items are protected from accidental sale, while blacksmiths pay a specialist bonus
for weapons and heavier armour.

Heroes now have Weapon, Armour, and Offhand equipment slots. Fighters can pair a
one-handed weapon with a shield or wield a two-handed weapon that changes Defend
into Rage. Mages can use tomes to amplify spell damage, Rogues can dual-wield
daggers, and Hunters can equip quivers with mixed offensive and defensive bonuses.
Two-handed weapons automatically clear the offhand slot, and the inventory and
shop interfaces explain and filter the new equipment category.

The map uses the selected race/class portrait as the hero miniature and cropped
encounter art for identified enemies. A red `!` is a rumored but unidentified
threat; violet borders mark a rumored relic search region. Save files persist all
map knowledge, and older Enhanced saves migrate with their previously visible map
preserved as scouted terrain.

## Native application

With JDK 14 or newer installed:

```sh
./package-app.sh
```

This creates a self-contained application image in `dist/`. Native packages are
platform-specific and must be built separately on macOS, Windows, and Linux.
