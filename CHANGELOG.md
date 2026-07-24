# Changelog

Application versions follow semantic versioning and are separate from the save-file
schema. Beta releases may still rebalance encounters and revise presentation based
on playtest feedback.

## Unreleased

No unreleased changes yet.

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
