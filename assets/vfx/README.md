# Prototype combat VFX

These combat animations are intentionally temporary. They validate the cached
PNG-frame animation pipeline while custom, painterly effects are created for The
Chosen Quest.

## Source and license

- Artist: **Cethiel**
- License: **Creative Commons Zero (CC0 1.0 / public-domain dedication)**
- License text: https://creativecommons.org/publicdomain/zero/1.0/

Imported collections:

- **Weapon Slash - Effect** — physical attacks, enemy strikes, and criticals
  - https://opengameart.org/content/weapon-slash-effect
- **Arcane Magic Effect** — Magic Missile
  - https://opengameart.org/content/arcane-magic-effect
- **Fireball Effect** — Fireball
  - https://opengameart.org/content/fireball-effect
- **Water Magic Effect** — temporary Ice Spike treatment
  - https://opengameart.org/content/water-magic-effect
- **Angel Shield Effect** — defend ward
  - https://opengameart.org/content/angel-shield-effect
- **Fire Wrath - Magic Effect** — boss strike
  - https://opengameart.org/content/fire-wrath-magic-effect

The files under `prototype/` retain their original filenames and are grouped by
their temporary gameplay role. Attribution is not required by CC0, but provenance
is kept here for maintainers and the eventual credits screen.

## Replacement contract

Replace these assets before the final portfolio release. New sequences should:

- keep the directory names and frame ranges currently consumed by `CombatVfx`,
  or update that single mapping class;
- use transparent RGBA PNG files with no background, labels, borders, or UI;
- keep a stable visual origin across every frame so effects do not wobble;
- leave generous transparent padding and avoid clipping bloom or debris;
- use the game's painted fantasy palette: restrained steel/gold for physical
  attacks, arcane blue/violet for magic, and ember red for hostile attacks;
- remain readable over both bright snow scenes and dark crypt scenes.

The procedural lines in `EncounterPanel` remain as resilience fallbacks only and
should not normally be visible while every sequence is present.
