# The Chosen Quest Enhanced Roadmap

This file describes the intended order of work. Individual tasks, value, workload,
ownership fit, and acceptance criteria live in [BACKLOG.md](BACKLOG.md).

## Product direction

The Enhanced edition is a polished, portfolio-ready interpretation of the original
game envisioned by its cofounders. The original source remains a separate Classic
edition rather than becoming a mode inside the Enhanced interface.

The intended rules are maintained in `GAME-DESIGN.md`; `CHATGPT-CONTEXT.md` is the
portable current-build summary for external design conversations.

## Current foundation

The current playable build includes the redesigned desktop shell, sixteen hero
builds, two starting loadouts per class, weapon and offhand rules, level-based
abilities, weapon proficiency, procedural encounters and landmarks, fog-of-war
discovery, rumors, specialized vendors, selling, equipment comparison, elite
relics, custom overlays, combat timing, encounter artwork, sound controls, and
save migration through world-generation schema 10.

Schema numbers protect save compatibility; they are not public release versions.
The public application version is now tracked independently in `AppVersion`, the
title screen, changelog, release tag, and tester archive.

## Milestone 1 — Stable combat progression

Goal: prove that every intended build is understandable, viable, and tactically
different across standard, elite, and boss encounters.

- `BAL-001` deterministic balance harness for all builds and loadouts — completed
- `BAL-002` level-three boss and relic tuning — completed
- `BAL-003` human validation of Rage, Momentum, Focus, and gated abilities — in progress
- `BAL-004` align starting Combat Paths, equipment requirements, and ability names
- `BAL-005` percentage-scale abilities and rebuild dragon preparation around three
  identified elite relics — after `BAL-004`
- `UX-001` ability details and outcome previews — completed
- `UX-002` multi-action combat timing and status clarity — completed
- `QA-001` repeatable playtest protocol and findings log — completed

## Milestone 2 — Presentation-quality combat

Goal: make every important action feel authored rather than provisional.

- `FX-001` distinctive ability VFX and sound cues
- `FX-002` persistent status-effect presentation — completed
- `AUDIO-001` cohesive old-school fantasy sound and ambience pass
- `ART-001` expanded, uniformly cropped equipment art
- `LOOT-001` broader class-compatible loot and economy tuning — completed

## Milestone 3 — Design and portfolio synchronization

Goal: keep the playable build and presentation file aligned and easy to review.

- `DSG-001` synchronize new loadout and progression components with Figma
- `DSG-002` restore a curated primary Figma presentation page
- `CHAR-002` make portrait cropping reliable through masked frame viewports — completed
- `CHAR-003` refocus character creation around one dominant selected-hero showcase
  with symbolic race crests and no miniature portraits — completed
- `CHAR-004` replace thin selectors and the wide summary with class cards and a
  selected-build dossier — completed
- `CHAR-005` add cached selection transitions and confirmation polish after the
  static hierarchy is validated
- `CHAR-007` establish a modular painterly Art Deco shell with transparent class
  washes and race motifs before the full creation-screen visual reskin — completed
- `CHAR-008` close the measured fidelity gap in responsive rhythm, component states,
  hero framing, typography, and material depth before propagating the system
- `CHAR-009` present two prominent Combat Path cards during creation and preserve
  Origin Path versus equipment-derived Current Style throughout play — after `BAL-004`
- `DSG-003` carry that same hero theme into the hero rail, inventory, combat,
  dialogue, progression, and outcomes without recoloring context-owned surfaces
- `MKT-001` use the same modular system for tester kits, Discord/community art,
  release graphics, Form headers, portfolio imagery, and future store assets
- `CHAR-006` preserve diorama, idle-animation, rotation, and origin concepts for a
  later presentation expansion
- `CHAR-001` add cosmetic character-presentation variants after frame stabilization
  — completed
- `TECH-001` formal versioning, changelog, and in-game version placement — completed
- `REL-001` commit, tag, and publish the stable GitHub beta checkpoint — completed
- `REL-002` refresh the shareable test build — completed
- `REL-003` collect opt-in beta-tester credits and maintain a consent roster
- `TECH-002` profile and optimize UI/audio/save hot paths — completed
- `UPDATE-001` Phase 1 update checking and official release-page download handoff
  completed; checksum-verified downloads and the separately tested one-click
  Update & Restart workflow remain

## Milestone 4 — A more variable world

Goal: increase replay value without losing the compact quest structure.

- `MAP-003` expand the logical world and scrolling viewport — first slice complete
- `MAP-004` add knowledge-aware off-screen hints — completed
- `MAP-006` simplify terrain labels and emphasize points of interest — completed
- `MAP-007` scale and clip map minis for every viewport density — completed
- `MAP-008` add discrete 7x7 detail and 9x9 overview zoom — completed
- `MAP-005` measure route-scale exploration, leveling, and attrition — completed
- `WORLD-001` prototype a finite, clearable Spider Nest — completed
- `WORLD-002` apply anti-farming rewards to spawned enemies — completed
- `MAP-001` make enemy and landmark placement biome-aware — completed
- `MAP-002` deepen rumors, maps, and fog-of-war discovery — completed
- `WORLD-003` expand validated sources and later add dungeon/lair layers

## Later platform and preservation work

- `CLASSIC-001` package the original source as a standalone Classic edition
- `PORT-001` verify Chromebook-friendly distribution
- `PORT-002` evaluate an eventual iOS port after the desktop architecture settles

## Planning rule

New ideas receive a stable task ID and enter the backlog before implementation.
Completed tasks move to `CHANGELOG.md` once formal application versioning is in
place. Playtest observations should reference the applicable task ID.
