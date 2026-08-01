# Changelog

Application versions follow semantic versioning and are separate from the save-file
schema. Beta releases may still rebalance encounters and revise presentation based
on playtest feedback.

## Unreleased

## 0.7.0-beta.2 — 2026-07-31

Updater validation and presentation-fix prerelease for private testers.

- Completed the first published prerelease-to-prerelease updater test: beta.1
  discovered beta.2 automatically and manually, retained complete version text,
  opened View Changes in-game without crashing, and passed reminder/download
  handoff checks.

- Added a styled post-combat loot reveal for common and uncommon equipment with
  item artwork, quality color, slot/stat comparison, distinct discovery chimes,
  and direct Inventory access; reward cards queue cleanly after level and relic
  presentations while the combat log remains the permanent record.
- Added Settings directly to the title-screen footer, opening the same styled
  audio, display, accessibility, and update controls used during gameplay.
- Kept update “View Changes” navigation inside the game and deferred its panel
  transition until the button event completes, avoiding the native macOS Java 8
  crash reported during update testing.
- Made release names wrap within a bounded update card so long beta/version titles
  remain complete without displacing the release notes, and routed macOS external
  links through the operating system rather than the fragile legacy AWT bridge.
- Replaced the misleading shield fallback on empty equipment slots with a neutral
  recessed socket across character creation, inventory, and the persistent hero
  rail; slot labels now carry the context without repeating “No offhand” as an item
  name, while tooltips and the inventory EMPTY tag preserve explicit status.
- Added non-blocking application update checks against official GitHub Releases,
  prerelease-aware Stable/Beta version comparison, a manual Settings action, and a
  styled update overlay with Download, View Changes, and Remind Me Later actions;
  offline failures remain silent at launch and never affect saves or gameplay
- Replaced the character-creation buttons' thin procedural corner fittings with
  an authored painterly brass three-slice frame; corners retain their proportions
  at every button width while hover, active, focus, pressed, and disabled behavior
  remains code-controlled
- Added a quieter painterly brass nine-slice for character-creation panels, cards,
  equipment, and the shell, plus safe content padding around authored corners
- Replaced the selected hero's procedural portrait border with a dedicated ornate
  3:4 overlay; its crown, side tiers, and lower mount remain fixed while portrait
  masking stays independent and responsive
- Replaced coordinate-painted hero-frame gems and surface tinting with separate,
  native-size enamel and gemstone underlays beneath the metal; their regions are
  derived from the authored material boundaries, slightly underlap the opaque brass
  to prevent scaling gaps, and retain original inlay texture and depth

- Rebuilt character creation around the approved painterly fantasy Art Deco
  direction with responsive clipped-corner frames, metallic inset lines, branded
  heading/header ornament, themed cards and dossier surfaces, and a central divider.
- Added a shared modular `HeroVisualTheme` that separates class color from subtle
  race heraldry and can carry into hero-owned gameplay and marketing surfaces.
- Made the selected hero artwork use additional vertical room on large displays
  while preserving its 3:4 masked frame.
- Corrected runtime class-resource colors so Mana, Rage, Momentum, and Focus meters
  display their intended theme colors.
- Unified character creation's central ornament and scrollbar: a class-colored gem
  in a race-metal setting rests at center when content fits and becomes the
  draggable scroll indicator when content overflows, without shifting either column.
- Added dedicated Art Deco primary and secondary button painters with clipped faces,
  dimensional shadows, inset metal rims, class-enamel active states, and complete hover,
  pressed, focused, active, and disabled feedback.
- Added compact standard-height spacing and a taller equipment band so the complete
  creation dossier and gear labels fit at 1440×900 without colliding with actions.
- Applied the intended engraved/display font hierarchy to creation labels, cards,
  profile statistics, equipment, and build guidance.
- Removed the redundant top-right creation label and stray corner rays, allowing the
  header and outer shell to read as one intentional composition.
- Replaced the left column's opaque backing with the shared race/class lacquer
  treatment and added restrained procedural material depth without flattening the
  modular theme layers.
- Upgraded the selected-hero frame with a stepped Art Deco silhouette, crown detail,
  side tiers, and a class-colored lower gem.
- Centered all three profile equipment cells, corrected the vertical text baseline
  in primary, secondary, selected, and pressed button states, and removed detached
  corner strokes from adjoining controls.
- Added bounded responsive creation-screen spacing: compact windows compress safely,
  1080p expands the hero to a larger 3:4 showcase and gives the dossier more usable
  height, and remaining space is distributed between sections rather than collecting
  beneath the left column.
- Added responsive choice-card breakpoints: compact race cards stack smaller crests
  above complete labels instead of truncating them, while unusually narrow class
  cards scale their weapon icon, title, and descriptor together.
- Added a transparent, high-contrast UI variant of the app crest with simplified
  dragon geometry, brighter gold edges, and a larger purple gem for legibility in
  the creation header's 30 px navigation slot; the detailed original remains the
  operating-system, release, Discord, and marketing icon.
- Added an adaptive player-name lockup with race-metal rails and class-colored gems;
  the ornaments shorten or disappear independently so maximum-length names retain
  their full safe area.
- Upgraded shared status meters with inset tracks, restrained resource glow, upper
  highlights, gold baselines, and end caps while preserving exact values and
  health/resource color semantics.
- Enlarged creation-profile equipment artwork and applied the inventory quality
  palette to item names and tooltips without adding tier copy that wraps in compact
  three-column layouts.
- Upgraded the creation shell to a crop-safe seven-pixel frame with a connected dark
  inset, outer metal rail, secondary highlight rail, and stepped corners.
- Made the creation-title ornament measure the actual heading, then place a diamond
  and capped-length rail beside it instead of extending a fixed line toward the
  center divider at every window width.
- Simplified selected race, class, gender, and loadout presentation to enamel plus
  one accent rim; the additional inner outline is now reserved for keyboard focus.
- Added the first production painterly material kit: independent neutral lacquer and
  warm parchment sources, cached runtime tiles, responsive clipping, and material
  compositing beneath existing class/race theme layers.
- Refined Art Deco buttons against the approved mockup with parchment/lacquer faces,
  inset brass fittings, bevel highlights, heavier display-serif copy, and dedicated
  visual-regression rows for both Deco styles across five interaction states.

## 0.6.0-beta.1 — 2026-07-23

Private tester release for structured class-balance, exploration, interface, audio,
and stability feedback.

### Highlights

- Introduces complete Rage, Momentum, Focus, and Mana combat identities across the
  four classes, with stricter ability costs and level requirements.
- Expands the fog-aware world map, adds a closer exploration zoom, and concludes the
  finite Spider Nest with an elite Ashweb Matriarch encounter.
- Adds styled first-launch release notes, persistent access to What’s New, and direct
  Feedback and Bug Report entry points.
- Adds scene-aware fantasy music, improved combat and interface cues, in-game credits,
  full attribution manifests, and performance coverage.

### Changes

- Confirmed relic equipment cannot bypass class level requirements and added a
  level-two Fighter regression with an identified relic and mastered weapon
- Reworked Second Wind into a level-three passive: once per encounter an otherwise
  fatal hit leaves the Fighter at 1 health, returns control, and enrages the next
  basic attack; it no longer heals or occupies a combat-action button
- Debounced WASD and arrow-key auto-repeat so exploration advances one tile per
  deliberate physical keypress instead of sprinting while a key is held
- Replaced non-magical mana presentation with class resources: Fighters build
  Rage by attacking or taking damage, Rogues build Momentum through attacks,
  stealth, and evasion, and their advanced abilities now spend those resources
- Added level-scaled Hunter Focus from Take Aim, accurate shots, and enemy misses;
  Volley now requires and consumes Hunter's Mark, while Pinning Shot interrupts
  only the target's next melee action
- Replaced the Ashweb Nest's identical third brood with an elite-presented,
  midgame-scaled Ashweb Matriarch finale that retains finite source rewards and
  cannot produce a normal elite relic
- Added the missing `M` shortcut and custom full-world map overlay; `M` or Escape
  closes it, and movement/combat shortcuts pause while any modal overlay is open
- Replaced unused full-map rail space with larger legend entries and dedicated
  Active Quest, Objectives, and Rumors & Clues modules
- Made exploration artwork follow each landscape asset's authored aspect ratio
  on larger displays instead of cropping it inside a fixed-height cover frame
- Kept character-creation race portraits square and centered on wide displays
  so responsive cards no longer crop faces inside stretched fantasy frames
- Changed the map's default viewport from 9x9 to 7x7 and added a closer 5x5
  zoom step while retaining the 9x9 route-planning overview
- Rebalanced all dragon variants and made identified relic bonuses consistent across
  basic attacks, spells, class abilities, and boss protection
- Improved Hunter marking and Fighter recovery so level-three tactical boss play is
  viable across a wider range of race and loadout combinations
- Added loadout- and target-aware ability help with cost, tempo, effect, unlock state,
  and estimated outcome for mouse, keyboard, and assistive-technology users
- Added a repeatable three-style playtest protocol and structured findings log
- Added persistent, color-coded Guard, Mark, Stun, Bleed, and Armor Break indicators;
  Bleed and Armor Break now have visible enemy-turn durations
- Made stunned and multi-attack enemy turns play as individually numbered beats, with
  shorter but complete sequencing under reduced-motion settings
- Pre-rendered character portraits in the background and added repeatable profiling
  for build switching, image rendering, audio initialization, and save/load paths
- Expanded standard-enemy loot across weapons, armour, and offhands for every class;
  retiered shop upgrades and improved protected-item resale progression
- Added a persistent map-intelligence journal with directional rumors, terrain context,
  regional-map limitations, and specialist hints for relic search regions
- Masked portrait artwork beneath the fantasy moulding so resized and square images
  retain frame overlap without bleeding through rounded corners
- Added an in-game Credits & Licenses panel from the title screen and Settings,
  packaged attribution manifests, and an opt-in beta-tester acknowledgment roster
- Added attributed, scene-aware fantasy music for exploration, safe locations,
  standard/elite combat, and boss encounters with resilient fallback routing
- Replaced thirteen high-frequency placeholder cues with cached CC0 RPG sounds for
  UI, equipment, commerce, weapons, defending, magic, healing, rest, and dragons

## 0.5.0-beta.1 — 2026-07-22

First public Enhanced Edition beta.

### Highlights

- Illustrated title, character creation, exploration, shop, inventory, combat,
  dialogue, settings, victory, and defeat interfaces
- Sixteen race/class builds with two starting loadouts per class, weapon traits,
  offhand rules, proficiency, and level-based abilities
- A 13×13 generated world with fog of war, biome-aware encounters, scrolling map
  camera, two zoom levels, rumors, relic clues, and safe fleeing
- Standard, elite, and boss combat presentations with quick-action keybinds,
  readable turn sequencing, combat VFX, sound, and status feedback
- Specialized vendors, equipment comparison, item quality, selling, enemy drops,
  elite relic identification, and communicated auto-equipping
- The finite Ashweb Nest world source with dedicated panoramic environment artwork
- Bundled fonts, music, ambience, effects, accessibility preferences, and reduced
  motion support

### Playtest notes

- The Shadow Dragon and other level-three boss variants remain active balance targets.
- Combat effects and several sound cues are beta assets scheduled for a cohesive
  custom-art and audio pass.
- Saves are stored outside the application folder. Keep a backup before replacing
  a beta with a newer build.
