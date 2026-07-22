# The Chosen Quest — Enhanced Edition

Current public build: **0.5.0-beta.1**. See [CHANGELOG.md](CHANGELOG.md) for
tester-facing release notes.

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
with `./test.sh`. The same command runs all 1,152 deterministic build/loadout combat
trials and writes their telemetry to `build/reports/balance-report.md`.

Project direction and prioritized work are tracked in [ROADMAP.md](ROADMAP.md) and
[BACKLOG.md](BACKLOG.md). Backlog entries include stable IDs, value, expected
workload, status, collaborator opportunities, and acceptance criteria.

## Play

Use WASD or the keyboard-style map controls to explore; arrow keys remain available
as an accessibility fallback. Combat actions use the nearby number keys 1–7, and
Mage spells cast directly from their numbered buttons without a selector dialog.
Travel fully reveals the current
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

Weapon families now carry visible combat traits: hammers and maces can stun,
crossbows pierce armour, axes and greatswords sunder defense, daggers can open
wounds, bows can find weak points, swords can flow into guard, and magical
focus weapons amplify spells. Trait activations appear in the combat log and
the live readiness line. NPC rumors, alchemist advice, and authored enemy battle
cries use an illustrated in-game typewriter conversation rather than system dialogs.
The blacksmith specializes in martial progression: Common maces and greatswords
appear immediately, level two reveals improved concussive and two-handed options,
and level three unlocks a Rare warhammer. These items never appear in the General
Merchant's lighter ranged, magical, and supply-focused catalog.

Combat complexity now grows across the intended three-level quest curve. Level
one exposes only core actions and a Mage's Magic Missile. Level two reveals a
loadout-aware tactical ability (or Fireball), and level three adds weapon mastery,
a class signature action, or Ice Spike. The equipped weapon family advances from
Trained to Proficient to Mastered at meaningful level-ups rather than through
repeatable attack grinding. Newly unlocked actions automatically join the numbered
combat bar and appear in a custom level-up presentation; locked actions do not
clutter early encounters. Older saves migrate their spellbook and proficiency to
the abilities appropriate for their current level. Character creation previews the
loadout-specific level-one through level-three ability path, and the character sheet
keeps unlocked and upcoming actions visible without opening combat.

The world now uses a 13x13 logical grid behind a scrolling map viewport. Players
can switch between a 9x9 exploration overview and a 7x7 detail view with the
sidebar `-` / `+` controls or matching keyboard keys. Zoom preserves the inspected
camera center, while hero-tracking mode recenters on the hero at either density.
The camera also supports independent compass-button panning and can be recentered
at any time. Knowledge-aware edge hints point toward known
off-screen threats, relic regions, and landmarks without exposing unknown terrain;
clicking a hint pans toward it. Ordinary terrain is communicated by artwork rather
than repeated labels, while compact glyphs are reserved for known points of interest;
exact terrain names remain available through tile tooltips. The map uses the selected race/class portrait as
the hero miniature and cropped encounter art for identified enemies. Minis scale
with tile density, remain clipped inside safe tile insets, and simplify into readable
tokens at overview scale. A red `!` is
a rumored but unidentified threat; violet borders mark a rumored relic search
region. Save files persist all map knowledge, and older 5x5 Enhanced saves expand
to schema 10 with known tiles preserved and new territory left uncharted.

World generation now places creatures in compatible terrain: serpents stay near
lakes, crypt threats remain in burial terrain, and forest or humanoid encounters
roam fields. Each world also contains the optional Ashweb Nest, a finite source with
three reduced-reward spider broods. Clearing it is permanent, grants one bounded
completion reward, and changes its map and location presentation. Source enemies
cannot drop equipment or become an XP/gold farm. The nest uses dedicated panoramic
environment artwork derived from the established Alshira and forest-spider art
direction.

`./test.sh` also generates `build/reports/exploration-report.md`. Its deterministic
quest-rush, prepared-route, and prepared-plus-nest simulations measure movement,
encounters, rests, potions, discovery, relic errands, and boss readiness across
every hero build.
This report is the world-pacing baseline for future nests, camps, and larger maps.

## Native application

With JDK 14 or newer installed:

```sh
./package-app.sh
```

This creates a self-contained application image in `dist/`. Native packages are
platform-specific and must be built separately on macOS, Windows, and Linux.

## Tester release

Run `./package-release.sh` to execute the complete verification suite and create a
cross-platform tester ZIP in `dist/`. The archive includes the runnable JAR,
double-click launchers for macOS and Windows, a Linux/Chromebook launcher, setup
instructions, release notes, and a SHA-256 checksum. Testers need Java 8 or newer.
