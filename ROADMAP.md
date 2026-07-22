# The Chosen Quest Enhanced Roadmap

This file describes the intended order of work. Individual tasks, value, workload,
ownership fit, and acceptance criteria live in [BACKLOG.md](BACKLOG.md).

## Product direction

The Enhanced edition is a polished, portfolio-ready interpretation of the original
game envisioned by its cofounders. The original source remains a separate Classic
edition rather than becoming a mode inside the Enhanced interface.

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
- `BAL-002` level-three boss and relic tuning
- `UX-001` ability details and outcome previews
- `UX-002` multi-action combat timing and status clarity
- `QA-001` repeatable playtest protocol and findings log

## Milestone 2 — Presentation-quality combat

Goal: make every important action feel authored rather than provisional.

- `FX-001` distinctive ability VFX and sound cues
- `FX-002` persistent status-effect presentation
- `AUDIO-001` cohesive old-school fantasy sound and ambience pass
- `ART-001` expanded, uniformly cropped equipment art
- `LOOT-001` broader class-compatible loot and economy tuning

## Milestone 3 — Design and portfolio synchronization

Goal: keep the playable build and presentation file aligned and easy to review.

- `DSG-001` synchronize new loadout and progression components with Figma
- `DSG-002` restore a curated primary Figma presentation page
- `CHAR-002` make portrait cropping reliable through masked frame viewports
- `CHAR-001` add cosmetic character-presentation variants after frame stabilization
- `TECH-001` formal versioning, changelog, and in-game version placement — completed
- `REL-001` commit, tag, and publish the stable GitHub beta checkpoint — completed
- `REL-002` refresh the shareable test build

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
- `MAP-002` deepen rumors, maps, and fog-of-war discovery
- `WORLD-003` expand validated sources and later add dungeon/lair layers

## Later platform and preservation work

- `CLASSIC-001` package the original source as a standalone Classic edition
- `PORT-001` verify Chromebook-friendly distribution
- `PORT-002` evaluate an eventual iOS port after the desktop architecture settles

## Planning rule

New ideas receive a stable task ID and enter the backlog before implementation.
Completed tasks move to `CHANGELOG.md` once formal application versioning is in
place. Playtest observations should reference the applicable task ID.
