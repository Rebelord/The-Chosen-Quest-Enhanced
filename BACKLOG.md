# The Chosen Quest Enhanced Backlog

This is the durable source of truth for planned work. `ROADMAP.md` defines sequence;
this file records why an item matters and how difficult it is expected to be.

## Ratings

- **Value:** High materially improves playability, clarity, portfolio quality, or
  release safety; Medium is worthwhile polish or reach; Low is optional refinement.
- **Workload:** Small is usually less than a focused day; Medium is a cohesive feature
  wave; Large spans multiple systems or requires substantial asset/testing work;
  Very Large is a future project or architectural transition.
- **Status:** Ready can be started now; In Progress has a verified working slice;
  Planned needs its preceding milestone work;
  Later is deliberately deferred; Blocked requires an external decision or asset;
  Completed is implemented, documented, and verified.

## Prioritized value/workload matrix

| ID | Work item | Value | Workload | Status | Good collaborator contribution |
|---|---|---:|---:|---|---|
| BAL-001 | Simulate all 16 race/class builds and both class loadouts | High | Large | Completed | Play runs and compare human outcomes with the generated baseline |
| BAL-002 | Tune level-three bosses, elite relic power, and potion pressure | High | Medium | Ready | Test Shadow Dragon attempts and flag unfair turns |
| QA-001 | Create a repeatable playtest checklist and findings log | High | Small | Ready | Run the checklist with different player styles |
| UX-001 | Add ability tooltips with cost, speed, effect, and estimated outcome | High | Medium | Ready | Review wording for clarity without revealing every formula |
| UX-002 | Improve multi-hit pacing, enemy turns, and combat status readability | High | Medium | Ready | Identify moments that feel instantaneous or confusing |
| FX-001 | Give every unlocked ability a distinctive VFX and sound identity | High | Large | Planned | Select visual and audio references for each class |
| AUDIO-001 | Replace rough cues with a cohesive old-school fantasy sound pass | High | Medium | Planned | Curate reference sounds and evaluate volume/character |
| FX-002 | Show persistent stun, bleed, mark, guard, and armour-break states | High | Medium | Planned | Review icon and color language for accessibility |
| LOOT-001 | Expand common/uncommon/rare pools and retune drops, prices, and sales | High | Medium | Planned | Propose item names and compare reward excitement |
| ART-001 | Add uniformly cropped art for weapons, offhands, armour, and relics | Medium | Large | Planned | Generate/select sprites and check crop consistency |
| DSG-001 | Sync loadout, proficiency, ability, and progression UI to Figma | High | Medium | Ready | Review the Figma presentation against game captures |
| DSG-002 | Rebuild the primary Figma page as a curated screen presentation | High | Medium | Ready | Choose the strongest representative screens |
| CHAR-002 | Rebuild portrait frames with masked artwork viewports | High | Medium | Ready | Check every frame size for edge bleed and crop quality |
| CHAR-001 | Add a cosmetic character-presentation choice and counterpart art | Medium–High | Large | Planned | Approve counterpart portraits and presentation wording |
| TECH-001 | Establish public versioning, changelog, and in-game version label | High | Small | Completed | Review version naming at the next milestone |
| REL-001 | Commit, push, and tag the current stable checkpoint | High | Small | In Progress | Test the published archive on a second computer |
| REL-002 | Refresh the shareable Dropbox/test build | Medium | Small | Planned | Test installation on a second computer |
| TECH-002 | Profile image loading, rendering, audio, and save hot paths | Medium | Medium | Planned | Report reproducible lag sequences and machine details |
| MAP-001 | Make enemy and landmark generation biome-aware | High | Medium | Completed | Review creature/terrain pairings during playtests |
| MAP-002 | Deepen rumors, purchasable maps, fog, and relic-region clues | High | Medium | Planned | Write rumor snippets and assess discovery pacing |
| MAP-003 | Expand the logical world, scrolling viewport, and later location layers | High | Very Large | In Progress | Sketch region and dungeon flow concepts |
| MAP-004 | Add fog-aware off-screen hints and map camera controls | High | Medium | Completed | Review marker priority and clarity during play |
| MAP-006 | Remove redundant terrain labels and emphasize known points of interest | High | Small | Completed | Review terrain and marker recognition at a glance |
| MAP-007 | Scale and clip hero, enemy, and warning markers by viewport density | High | Small | Completed | Confirm minis remain recognizable without crossing tiles |
| MAP-008 | Add authored 7x7 detail and 9x9 overview map zoom levels | High | Small | Completed | Compare route planning and marker recognition at both scales |
| MAP-005 | Measure full-route exploration pace, leveling, and attrition | High | Medium | Completed | Compare human routes and resource use with the deterministic baseline |
| WORLD-001 | Prototype a limited, permanently clearable Spider Nest | High | Large | Completed | Test whether the nest feels avoidable but worth clearing |
| WORLD-002 | Prevent spawned enemies from becoming an XP, gold, or loot farm | High | Medium | Completed | Evaluate whether reduced rewards still feel fair |
| WORLD-003 | Expand validated sources into camps, dens, crypts, and shrines | Medium | Large | Later | Propose source themes, enemies, and authored rewards |
| CLASSIC-001 | Package an untouched standalone Classic edition | Medium | Large | Later | Verify fidelity against the original experience |
| PORT-001 | Produce and test a Chromebook-friendly package | Medium | Medium | Later | Test on representative Chromebook hardware |
| PORT-002 | Evaluate and prototype an eventual iOS architecture | Medium | Very Large | Later | Define target devices and interaction expectations |

## Ready-task acceptance criteria

### WORLD-001 — Finite Ashweb Spider Nest

- Status: **Completed**
- Every new world contains one mid-route nest with three persistent brood charges.
- Each defeated brood requires an explicit search or later return before the next
  encounter; the third permanently changes the map and location copy to cleared.
- The source, remaining brood budget, and cleared state persist through schema 10.
- Active legacy quests receive the source only on empty uncharted terrain; completed
  or fully explored legacy worlds do not gain a surprise encounter.
- The map glyph, tooltip, story view, action label, ambience, and encounter scene all
  communicate source state through the standard game shell.

### WORLD-002 — Source reward protection

- Status: **Completed**
- Nest enemies are explicitly tagged as source spawns and cannot roam away.
- Each brood grants only 3 XP and 3 gold, never rolls standard equipment, and consumes
  one of the three permanent source charges.
- Clearing grants one authored 18-gold reward; further searches cannot spawn enemies
  or repeat rewards. The entire source is therefore capped at 9 XP and 27 gold.
- The prepared-plus-nest route adds 12.0 average movement steps and three encounters;
  it remains an optional safety objective rather than an unlimited progression loop.

### MAP-001 — Biome-aware placement

- Status: **Completed**
- Safe road landmarks and the nest select field terrain; the dragon lair selects a
  distant crypt before the landmark replaces its underlying tile presentation.
- Serpents inhabit lakes; undead, cultists, trolls, necromancers, and fallen knights
  inhabit crypts; forest beasts and humanoid threats inhabit fields.
- Roaming enemies remain inside compatible terrain and cannot enter safe landmarks,
  sources, or another occupied cell.
- Sixty-four deterministic generation seeds verify required landmark and encounter
  counts, terrain compatibility, all dragon variants, and a finite nest source.

### MAP-005 — Exploration pacing baseline

- Status: **Completed**
- `ExplorationSimulationTest` covers all 32 race/class/loadout builds across six
  deterministic worlds and quest-rush, prepared, and prepared-plus-nest policies.
- The generated `build/reports/exploration-report.md` records travel, fights,
  rests, potions, map knowledge, boss readiness, and completion rates.
- Quest rushes average 20.3 moves and win 0% of boss attempts.
- Prepared routes average 87.9 moves, 8.5 encounters, 2.0 rests, and 1.9 potions;
  80.2% reach the boss and 73.4% complete the quest.
- Prepared Ranger Hunters reach the boss only 58.3% of the time, identifying
  pre-boss route attrition as a tuning concern rather than only dragon difficulty.
- The prepared route reaches the boss at level 3 with three identified relics and
  full health when it survives. Clearing the nest adds 12.0 moves and three fights,
  giving future source designs a measured upper bound for optional detours.

### BAL-001 — Build and loadout balance harness

- Status: **Completed**
- Every race/class/loadout combination can be run with deterministic seeds.
- Results capture survival, turns, damage, resource use, and encounter tier.
- The report identifies outliers without assuming every build plays identically.
- Existing deterministic regression tests remain green.

Baseline findings from 1,152 trials:

- Standard checkpoint: 100% wins, 4.1 average turns, 84.6% average health left.
- Elite checkpoint: 100% wins, 7.7 average turns, 58.3% average health left.
- Shadow Dragon checkpoint: 2.1% wins, 10.8 average turns, 0.3% average health left.
- The few boss wins disproportionately favor Quick Knives Rogues; `BAL-002` should
  improve cross-class viability without erasing their speed identity.
- Re-run `./test.sh` to regenerate `build/reports/balance-report.md` after tuning.

### BAL-002 — Boss and relic tuning

- A tactically played level-three hero with an appropriate identified relic has a
  credible path to victory against the Shadow Dragon.
- Shops help defeat elites but do not replace elite relic progression.
- Victory does not require repeated critical hits or a single exact build.
- Extra enemy turns remain dangerous but are clearly communicated.

### QA-001 — Playtest protocol

- A playtest records build, seed, route, level, equipment, relics, result, and notes.
- Separate checks cover cautious, exploratory, and attack-spamming behavior.
- Findings link to backlog IDs and distinguish defects from balance opinions.

### UX-001 — Ability information

- Hover or focus explains cost, tempo, damage/effect, prerequisites, and status impact.
- Locked abilities explain their level or proficiency requirement.
- Keyboard-only players can access the same information.
- Descriptions use the same names and rules as the combat engine.

### UX-002 — Combat timing and feedback

- Each attack in a multi-attack turn is perceptible in sequence.
- Critical, spell, physical, incoming, healing, and status events remain distinct.
- Active statuses show source and remaining duration where applicable.
- Reduced-motion mode remains functional.

### DSG-001 — Figma synchronization

- Character creation includes the two loadout choices and ability path.
- Character/Inventory includes proficiency and ready/locked ability cards.
- Components use the established fonts, colors, spacing, and interaction states.
- The implementation and Figma screen use the same content hierarchy.

### TECH-001 — Versioning

- Status: **Completed**

- One documented semantic application version exists independently of save schema.
- The version appears unobtrusively on the title screen or settings overlay.
- `CHANGELOG.md` records user-facing additions and fixes by version.
- Release tags and downloadable filenames use the same version.

## Recently completed foundation

These are recorded here temporarily so the remaining plan has context. They should
move into the first formal changelog entry under `TECH-001`.

- Two Common starting loadouts per class with build-aware character creation
- Standard enemy Common/Uncommon drops, vendor specialization, and item selling
- Weapon traits, offhands, two-handed Fighter rules, and class equipment limits
- Level-based abilities, weapon proficiency, dynamic quick actions, and progression UI
- Inventory comparisons, quality tiers, sorting, relic identification, and auto-equip
- Procedural landmarks/enemies, safe fleeing, fog of war, rumors, and map markers
- Custom dialogue, settings, relic, progression, victory, and defeat overlays
- Standard, elite, and boss combat layouts with encounter art and turn sequencing
- A 13x13 finite world behind a 9x9 scrolling viewport, map camera controls,
  7x7 detail zoom, fog-aware edge hints, and safe migration from legacy 5x5 saves
