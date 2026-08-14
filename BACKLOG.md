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
| BAL-002 | Tune level-three bosses, elite relic power, and potion pressure | High | Medium | Completed | Test Shadow Dragon attempts and flag unfair turns |
| BAL-003 | Validate Rage, Momentum, Focus, and gated class abilities in human play | High | Medium | In Progress | Compare cautious and attack-spam runs across Fighter, Rogue, and Hunter |
| BAL-004 | Align starting paths, equipment requirements, and ability names | High | Medium | Ready | Verify that each creation-screen promise matches the first-turn combat options |
| BAL-005 | Convert ability scaling and rebuild three-relic dragon preparation | High | Medium | Ready | Compare level-two ability spikes and level-three dragon attempts after three elite routes |
| QA-001 | Create a repeatable playtest checklist and findings log | High | Small | Completed | Run the checklist with different player styles |
| DOC-001 | Create a living GDD and portable external-design context packet | High | Small | Completed | Compare new ideas against the documented current build |
| UX-001 | Add ability tooltips with cost, speed, effect, and estimated outcome | High | Medium | Completed | Review wording for clarity without revealing every formula |
| UX-002 | Improve multi-hit pacing, enemy turns, and combat status readability | High | Medium | Completed | Identify moments that feel instantaneous or confusing |
| UX-003 | Add in-game “What’s New” patch notes with first-launch version tracking | High | Medium | Completed | Review which changes and known issues matter most to testers |
| UX-004 | Add a styled bug-report entry point and automated report pipeline | High | Medium | In Progress | Submit a test report after the in-game link and GitHub automation are connected |
| FX-001 | Give every unlocked ability a distinctive VFX and sound identity | High | Large | Planned | Select visual and audio references for each class |
| AUDIO-001 | Replace rough cues with a cohesive old-school fantasy sound pass | High | Medium | In Progress | Playtest scene music and identify the roughest remaining UI/combat cues |
| FX-002 | Show persistent stun, bleed, mark, guard, and armour-break states | High | Medium | Completed | Review icon and color language for accessibility |
| LOOT-001 | Expand common/uncommon/rare pools and retune drops, prices, and sales | High | Medium | Completed | Propose item names and compare reward excitement |
| ART-001 | Add uniformly cropped art for weapons, offhands, armour, and relics | Medium | Large | Planned | Generate/select sprites and check crop consistency |
| ART-002 | Refine the provisional Male Dwarf Rogue toward a lighter, more agile rogue silhouette | Low | Small | Planned | Reduce shoulder/forearm armour while preserving the approved male dwarf anatomy, dual daggers, crop, and tunnel art direction |
| DSG-001 | Sync loadout, proficiency, ability, and progression UI to Figma | High | Medium | Ready | Review the Figma presentation against game captures |
| DSG-002 | Rebuild the primary Figma page as a curated screen presentation | High | Medium | Ready | Choose the strongest representative screens |
| CHAR-002 | Rebuild portrait frames with masked artwork viewports | High | Medium | Completed | Check every frame size for edge bleed and crop quality |
| CHAR-003 | Refocus character creation around one dominant live hero showcase | High | Medium | Completed | Verify the screen contains exactly one character image at every supported size |
| CHAR-004 | Replace thin selectors and the wide summary with class cards and a build dossier | High | Medium | Completed | Review resource, role, difficulty, strengths, and progression wording |
| CHAR-005 | Add cached character-selection transitions and confirmation polish | Medium–High | Medium | Planned | Suggest subtle class particles, transition cues, and reduced-motion behavior |
| CHAR-006 | Preserve advanced creation concepts for a later presentation expansion | Medium | Large | Later | Develop idle-animation, diorama, rotation, and origin concepts without changing current balance |
| CHAR-007 | Build the painterly Art Deco creation theme from modular race/class layers | High | Large | Completed | Review representative builds and keep race motifs quieter than class identity |
| CHAR-008 | Close the remaining character-creation fidelity gap against the approved Art Deco mockup | High | Medium | In Progress | Compare standard and large previews for clipping, dead space, readability, and material depth |
| CHAR-009 | Make Combat Path choice prominent in creation and persistent during play | High | Medium | Ready after BAL-004 | Review whether Origin Path and Current Style remain understandable after equipment changes |
| DSG-003 | Carry the shared hero visual theme through appropriate gameplay surfaces | High | Medium | Ready | Check that hero identity is visible without recoloring shops, maps, enemies, or global navigation |
| MKT-001 | Apply the shared Art Deco hero system to tester, community, release, and portfolio templates | High | Medium | In Progress | Review tester assets at actual Discord, Form, PDF, and GitHub crop sizes |
| CHAR-001 | Add a cosmetic character-presentation choice and counterpart art | Medium–High | Large | Completed | Approve counterpart art and presentation wording |
| TECH-001 | Establish public versioning, changelog, and in-game version label | High | Small | Completed | Review version naming at the next milestone |
| UPDATE-001 | Add safe in-game update checking and a one-click Update & Restart workflow | High | Large | In Progress | Test checksum-verified package download and assisted installation for Phase 2 |
| REL-001 | Commit, push, tag, and publish the stable beta checkpoint | High | Small | Completed | Test the published archive on a second computer |
| REL-002 | Refresh the shareable Dropbox/test build | Medium | Small | Completed | Test the unzipped `0.5.0-beta.1` Dropbox build on a second computer |
| REL-003 | Collect opt-in beta-tester credits and maintain a consent roster | Medium | Small | In Progress | Submit the optional credit choice in the live general feedback form |
| TECH-002 | Profile image loading, rendering, audio, and save hot paths | Medium | Medium | Completed | Report reproducible lag sequences and machine details |
| MAP-001 | Make enemy and landmark generation biome-aware | High | Medium | Completed | Review creature/terrain pairings during playtests |
| MAP-002 | Deepen rumors, purchasable maps, fog, and relic-region clues | High | Medium | Completed | Write rumor snippets and assess discovery pacing |
| MAP-003 | Expand the logical world, scrolling viewport, and later location layers | High | Very Large | In Progress | Sketch region and dungeon flow concepts |
| MAP-004 | Add fog-aware off-screen hints and map camera controls | High | Medium | Completed | Review marker priority and clarity during play |
| MAP-006 | Remove redundant terrain labels and emphasize known points of interest | High | Small | Completed | Review terrain and marker recognition at a glance |
| MAP-007 | Scale and clip hero, enemy, and warning markers by viewport density | High | Small | Completed | Confirm minis remain recognizable without crossing tiles |
| MAP-008 | Add authored 7x7 detail and 9x9 overview map zoom levels | High | Small | Completed | Compare route planning and marker recognition at both scales |
| MAP-009 | Add a complete fog-aware world map overlay on the M key | High | Small | Completed | Test tile tooltips and readability during a full playthrough |
| MAP-005 | Measure full-route exploration pace, leveling, and attrition | High | Medium | Completed | Compare human routes and resource use with the deterministic baseline |
| WORLD-001 | Prototype a limited, permanently clearable Spider Nest | High | Large | Completed | Test whether the nest feels avoidable but worth clearing |
| WORLD-002 | Prevent spawned enemies from becoming an XP, gold, or loot farm | High | Medium | Completed | Evaluate whether reduced rewards still feel fair |
| WORLD-003 | Expand validated sources into camps, dens, crypts, and shrines | Medium | Large | Later | Propose source themes, enemies, and authored rewards |
| CLASSIC-001 | Package an untouched standalone Classic edition | Medium | Large | Later | Verify fidelity against the original experience |
| PORT-001 | Produce and test a Chromebook-friendly package | Medium | Medium | Later | Test on representative Chromebook hardware |
| PORT-002 | Evaluate and prototype an eventual iOS architecture | Medium | Very Large | Later | Define target devices and interaction expectations |

## Ready-task acceptance criteria

### UX-003 — In-game patch notes

- Status: **Completed**
- Show the current release notes automatically once per installed application
  version, then retain them under Settings or the title-screen version label.
- Use the game’s styled overlay rather than a system dialog.
- Keep the in-game notes, packaged `CHANGELOG.md`, and GitHub release notes sourced
  from the same versioned content.
- `0.6.0-beta.1` shows a styled summary once per installed version and retains
  What’s New access from both the title footer and Settings.

### UX-004 — Bug-report workflow

- Status: **In Progress**
- A published Google Form titled `The Chosen Quest Enhanced — Beta Bug Report`
  accepts structured reports and stores them in a linked private response Sheet.
- Essential reproduction, impact, version, and operating-system fields are required;
  tester identity and contact fields are optional and must remain private.
- Add a styled in-game entry point that prefills safe game context and also offers
  copy/save diagnostics for offline reporting.
- The first release slice adds direct title, Settings, and What’s New links to the
  live short-form URLs. Context prefilling and offline diagnostics remain planned.
- Add duplicate-safe Google Apps Script automation that creates labeled, sanitized
  GitHub issues and records the resulting issue URL or failure status in the Sheet.
- Never embed Google or GitHub credentials in the game, repository, Form, or Sheet.

### UPDATE-001 — Streamlined application updates

- Status: **In Progress — Phase 1 implemented and human-verified**
- Value: **High**
- Workload: **Large**, delivered in independently testable stages.
- Host a small versioned update manifest through the project repository or GitHub
  Releases. It records the latest version, release and download URLs, SHA-256,
  update channel, required/optional status, and concise patch notes.
- Phase 1 adds a quiet startup check, a manual **Check for Updates** action in
  Settings, and a game-styled update overlay with **Download**, **Remind Me Later**,
  and **View Changes**. Offline play and network failures must never block launch.
- Phase 1 now checks the public GitHub Releases API asynchronously, compares Stable
  and Beta releases with prerelease-aware semantic ordering, ignores drafts, keeps
  dismissed-version state outside the game directory, and opens only official
  release/download URLs. It does not modify or execute application files.
- End-to-end verification completed on 2026-07-31: retained `0.7.0-beta.1`
  discovered published `0.7.0-beta.2` automatically at launch and through the
  manual Settings action; the version remained unclipped, View Changes stayed
  in-game without crashing, and reminder/download handoff actions worked.
- Phase 2 downloads the correct platform package with visible progress, verifies its
  checksum, and offers an assisted installation without executing unverified files.
- Phase 3 introduces a separate updater process and a single **Update & Restart**
  action. The updater closes the game, swaps application files, preserves saves and
  settings, relaunches the game, and restores the previous build if replacement
  fails.
- Support distinct **Stable** and **Beta** channels; private testers may opt into
  Beta while ordinary players remain on Stable.
- Store saves, settings, diagnostics, and update preferences outside the replaceable
  application directory before enabling automatic replacement.
- Ordinary updates remain optional. Reserve required updates for severe defects or
  explicitly documented save/protocol incompatibility.
- Do not enable Phase 3 until platform packaging, install locations, signing
  expectations, and a private prerelease-to-prerelease update test are verified.
- Acceptance criteria:
  - **Verified:** semantic version comparison handles beta/prerelease versions,
    numeric prerelease identifiers, stable-over-prerelease precedence, and leading
    `v` tags correctly.
  - **Verified:** a published prerelease is detected from the previous beta through
    both automatic and manual checks without delaying or destabilizing the game.
  - A tampered or mismatched package is rejected before installation.
  - Canceling, losing connectivity, or running offline leaves the installed game
    playable.
  - Updating preserves existing saves and preferences.
  - A failed replacement restores the prior working version.
  - Update UI follows the existing overlay, button-state, typography, and
    accessibility conventions.

### CHAR-003 — Selected-hero character-creation showcase

- Status: **Completed**
- Preserve the current name generator, race/class choices, two starting loadouts,
  build preview rules, class sounds, confirmation chime, and all sixteen authored
  race/class combinations.
- Make the selected build the only full-size character illustration on the screen;
  replace competing race portraits with compact illustrated crests or emblems.
- Character creation contains exactly one character illustration: the live full-body
  hero. Race and class controls use heraldry or symbolic icons, never miniature
  character portraits.
- Enlarge the selected hero artwork by roughly 20–25% where the responsive layout
  permits, without allowing artwork to bleed past the masked fantasy frame.
- Rebalance the desktop layout so unused lower-right space supports the selected
  character while the selection controls remain readable at 1280 × 720.
- Keep every race, class, loadout, name, Back, and Begin Journey action keyboard
  reachable with a clear selected, focused, disabled, and hover state.
- Do not change combat formulas, racial bonuses, class resources, starting items,
  or progression as part of this presentation task.
- Preload or reuse the existing artwork cache so rapid selection changes do not
  reintroduce the character-creation lag addressed by `TECH-002`.

### CHAR-004 — Class cards and selected-build dossier

- Status: **Completed**
- Replace the thin class tabs with equal-size cards showing class emblem, name,
  resource, and one concise playstyle label.
- Consolidate Selected Build, Combat Style, Build Traits, equipment, and ability
  progression into a scannable character-sheet presentation beside the hero.
- Reuse the existing inventory icon library for starting weapon, armour, and offhand
  presentation instead of plain bullet text.
- Prefer accurate resource, role, strength, weakness, equipment, and progression
  language over speculative statistics or mechanics the engine does not calculate.
- Retain exact health, resource, attack, and defense information where it helps
  players compare builds; do not introduce decorative attribute bars that imply
  unsupported formulas.

### CHAR-005 — Lightweight selection and confirmation transitions

- Status: **Planned** after `CHAR-003` and `CHAR-004`
- Add a short cached crossfade or class-colored overlay when the selected hero art
  changes; avoid frame-by-frame character animation that requires new viewpoints.
- Give each class a restrained particle or lighting accent that does not obscure the
  portrait or compete with class-color accessibility.
- Preserve the existing race/class selection sounds and give Begin Journey a brief,
  interruptible confirmation flourish before entering the game.
- Reduced-motion mode must replace transitions with an immediate state change while
  retaining sound, focus, and selection feedback.
- Rapidly alternating selections must not queue stale images, sounds, or animations.
- Preserve the single-hero rule: transitions update the one live hero showcase and
  never add thumbnail portraits to race or class controls.

### CHAR-007 — Modular painterly Art Deco creation theme

- Status: **Completed**
- Preserve one fixed layout and component hierarchy across all sixteen builds.
- Compose the visual treatment from a neutral Art Deco shell, transparent
  class-color wash, subtle race motif, modular frame ornament, and ordinary content.
- Keep race decoration at roughly 5–10% intensity; class color may carry active
  states, resource identity, and restrained lighting.
- Export painterly overlays with transparency and no embedded text, panels, portrait,
  or neighboring-component shadows. Prefer programmatic geometry where practical.
- `HeroVisualTheme.forBuild(race, heroClass)` is the shared implementation contract.
- `HERO-VISUAL-THEME-SYSTEM.md` records asset and Figma component rules before the
  full visual reskin proceeds.
- Every build must preserve spacing, hit targets, keyboard flow, focus indication,
  readability, portrait masking, and rapid-selection performance.
- The production screen now uses responsive clipped-corner frames, metallic inset
  lines, themed selection cards, a branded header, ornamental section heading,
  central diamond divider, themed dossier/equipment frames, and a large-display
  hero treatment without flattening those elements into background art.
- Resource meters now honor their runtime `HeroVisualTheme` color rather than the
  construction-time default.

### DSG-003 — Shared hero identity across gameplay

- Status: **Ready**, after the `CHAR-007` creation-screen foundation.
- Resolve one `HeroVisualTheme` from the saved race/class selection and reuse it
  across hero-owned surfaces.
- Apply class identity to resource meters, selected quick actions, compatible item
  actions, player status, and restrained hero VFX.
- Apply race identity only as subtle heraldry or material on personal profile,
  progression, reward, and outcome surfaces.
- Keep global navigation, maps, settings, shops, NPCs, enemies, quality tiers,
  danger tiers, health, validation, and disabled states neutral or context-owned.
- No gameplay panel may fork into sixteen race/class-specific layouts.
- Verify all sixteen builds in character creation, exploration, inventory, combat,
  dialogue, and outcome previews.

### CHAR-008 — Art Deco fidelity refinement

- Status: **In Progress**
- `CHARACTER-CREATION-FIDELITY-AUDIT.md` compares the approved mockup with standard
  and large production previews and records the ranked gaps.
- First correct responsive vertical rhythm, dossier clipping, large-screen dead
  space, scrollbar/divider competition, bespoke button states, and typography.
- Then build the richer scalable hero frame, shell/header treatment, name ornament,
  and more legible item/card hierarchy.
- Finish with reusable painterly lacquer, parchment, metal-wear, shadow, class-enamel,
  and race-heraldry layers rather than a flattened background.
- Verify 1440×900 and 1920×1080 previews across representative and edge-case names,
  all four classes, all four races, both cosmetic choices, and both loadouts.
- First completed slice unifies the divider and scrollbar in one permanently reserved
  28 px component. Its themed gem is centered when content fits and becomes the
  draggable thumb when content overflows, with a larger invisible drag target.
- The responsive-composition slice now uses bounded gaps and dossier growth at
  1080p, expands the masked hero showcase to 420×560 where space permits, preserves
  the complete 1440×900 flow, and retains themed scrolling at 1280×720.
- The profile-hierarchy slice adds a maximum-name-safe ornamental lockup, shared
  Art Deco meters, and larger quality-aware equipment presentation without
  sacrificing the compact 1280×720 layout.
- The shell-and-selection slice adds layered crop-safe rails, a measured title
  ornament, and single-rim enamel selection states with an independent keyboard
  focus outline.
- The first authored-material slice adds cached lacquer/parchment tiles and records
  the required nine-slice, mask, multi-resolution, and regression workflow in
  `UI-MATERIAL-ASSET-PIPELINE.md`.
- The authored-frame slice adds a button three-slice, supporting panel nine-slice,
  and dedicated 3:4 hero overlay. A class-color backplate now shows only through
  independent native-size gem and enamel underlays derived from the source artwork.
  Their hidden four-pixel underlap is clipped by the opaque brass overlay, so the
  frame remains aligned at every scale without coordinate-defined shapes. Portrait,
  masks, metal, and artwork remain independent runtime layers. Transparent race
  overlays and broader gameplay-surface rollout remain in scope.

### MKT-001 — Cohesive branded communication templates

- Status: **In Progress**
- `BRAND-VISUAL-SYSTEM.md` defines the shared marketing composition, modular slots,
  transparency rules, placement defaults, and review checklist.
- Treat tester invitations, tester guides, feedback/bug Form headers, Discord art,
  GitHub release cards, portfolio images, and eventual store graphics as extensions
  of the product visual system.
- General materials use a neutral four-class ensemble; focused campaigns may use
  the featured build's class wash and quieter race heraldry.
- Keep hero, environment, logo, ornament, class wash, race motif, copy, version,
  links, attribution, and crop guides editable as independent layers.
- Create and verify reusable master templates at the actual target aspect ratios.
- Do not mark complete until the existing tester kit and community assets have been
  reconciled against the new system.

### CHAR-006 — Deferred expanded creation concepts

- Status: **Later**
- Preserve, but do not yet implement, a miniature/diorama presentation, handcrafted
  idle animations, alternate character viewpoints, and optional origin/background
  choices.
- If implemented, the diorama replaces the live hero canvas; it does not add a
  second character image or restore portrait grids.
- Treat origins as a gameplay-and-balance feature requiring separate GDD approval,
  save migration, item/relic rules, simulations, and Figma work rather than bundling
  them into a visual character-creation revision.
- Reassess this item after counterpart character art, any new classes, and the
  eventual platform direction are settled.

### AUDIO-001 — Cohesive old-school fantasy sound pass

- Status: **In Progress**
- The first authored-music slice is complete: attributed Soundimage themes now cover
  exploration/creation, safe locations, standard/elite combat, and boss encounters;
  all are compact Java-compatible PCM16 files with normalized peaks and softened loop
  seams, while the existing title music remains the resilient fallback.
- The second authored-effects slice replaces thirteen high-frequency placeholder
  sounds using artisticdude's CC0 RPG Sound Pack: UI confirm/cancel/error, equipment,
  commerce, weapon swings and impacts, defense, spellcasting, healing, rest, and an
  interim dragon vocal. Every file is decoded into the cache at startup, with the
  original procedural sample retained automatically if an asset is unavailable.
- Human playtesting still needs to evaluate perceived loudness, repetition, cue fit,
  music-loop transitions, and whether the temporary dragon vocal should be replaced.
- Curate a small, consistent palette instead of mixing unrelated sounds simply
  because they are free: Soundimage/Incompetech/OpenGameArt for music, Kenney for
  core UI and RPG cues, and carefully filtered Freesound or Sonniss material for
  ambience, creatures, and environmental detail.
- Prefer CC0, CC BY 4.0, OGA-BY, or an explicitly game-safe royalty-free license;
  exclude NonCommercial and NoDerivatives material from distributable builds.
- Record the author, asset title, source URL, license, download date, required credit,
  and any edits in `assets/audio/ATTRIBUTION.md`, retaining a local license copy when
  practical.
- Credit every identifiable artist as a project policy even when attribution is
  optional, including CC0 and no-attribution sources, as thanks for their work.
- Add a clearly labeled `Credits & Licenses` entry to the title-screen footer and
  Settings/About; it opens a styled in-game credits view containing the full credits,
  licenses, source links, and modification notes rather than relying on an external
  link alone.
- Include an optional `Beta Testers` thank-you section populated only from the
  consent roster maintained by `REL-003`.
- Include the same attribution record in every downloadable package so credits remain
  available if the game cannot launch or the source website later changes.
- Select candidates for title, exploration, tavern/shop, standard combat, elite/boss
  combat, victory/relic discovery, defeat, UI, movement, weapons, spells, creatures,
  and biome ambience before replacing the current cues.
- Normalize loudness, trim silence, test seamless loops and transitions, and verify
  separate music, ambience, and effects controls before accepting the pass.
- Preserve the current audio as a fallback until the replacement set passes a full
  playthrough and a second-computer package test.

### WORLD-001 — Finite Ashweb Spider Nest

- Status: **Completed**
- Every new world contains one mid-route nest with three persistent brood charges.
- The first two charges spawn reduced-reward Giant Forest Spiders; the final charge
  reveals an elite-presented, level-two Ashweb Matriarch.
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
- The two lesser broods grant 3 XP and 3 gold each; the Matriarch grants 6 XP and
  6 gold. None roll equipment or relics, and each consumes one finite source charge.
- Clearing grants one authored 18-gold reward; further searches cannot spawn enemies
  or repeat rewards. The entire source is therefore capped at 12 XP and 30 gold.
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

- Status: **Completed**
- Identified relic bonuses now apply consistently to basic attacks, spells, class
  abilities, and bounded boss damage mitigation.
- Dragon health/attack and Hunter/Fighter survival tools were tuned against all 32
  race/class/loadout checkpoints rather than a single favored build.
- The generated tactical baseline now wins 84.6% of 384 boss trials; vulnerable
  Human Mage and Breaker results preserve meaningful build and preparation risk.

- A tactically played level-three hero with an appropriate identified relic has a
  credible path to victory against the Shadow Dragon.
- Shops help defeat elites but do not replace elite relic progression.

### BAL-004 — Starting-path and equipment alignment

- Status: **Ready**
- Rename the displayed and internal Fighter starting paths from Vanguard and Breaker
  to Knight and Berserker, while safely migrating legacy save values.
- Knight starts with a one-handed sword, modest shield, and chain armour; Berserker
  starts with a two-handed weapon, leather armour, and no offhand.
- Equipment configuration—not the original path selection—controls available
  techniques: shield techniques require a one-handed weapon and shield, heavy
  techniques require a two-handed weapon, and Power Strike remains the fallback
  for a one-handed weapon without a shield.
- Combine Guard and the intended counterattack behavior into a clearly named shield
  technique without calling it Riposte. Rage comes from the resulting counterattack,
  not merely from selecting a defensive action.
- Rename the current single-target two-handed Cleave to Heavy Strike and reserve
  Cleave for a future encounter system with multiple simultaneous targets.
- Replace the Rogue paths with the approved Assassin and Skirmisher identities.
  Assassin uses a single dagger, Stealth, Vanish, Ambush, and Execute; Skirmisher
  uses dual weapons, Twin Strike, Evasive Strike, and Blade Flurry. Both use
  Momentum differently, and no technique may require equipment the path does not
  actually provide.
- Rename or mechanically redesign Spellblade so its name matches its wand, tome,
  armour, and combat loop; differentiate it from Channeler without adding a broad
  armour-weight system in this wave.
- Make the Marksman opening-shot promise and Hunter's Mark relationship mechanically
  true rather than relying only on creation-screen copy.
- Keep armour as straightforward Defense during this task. Add weight, speed, Mana,
  or efficiency tradeoffs only after human testing demonstrates a universal heavy-
  armour problem.
- Update character creation, combat help, Inventory, documentation, simulations,
  and later Figma synchronization from the same authoritative equipment rules.

### CHAR-009 — Combat Path selection and persistent identity

- Status: **Ready after BAL-004**
- Present each class's two starting builds as large **Combat Path** cards rather
  than narrow loadout buttons or a traditional branching skill tree.
- Each card shows path name, concise fantasy, starting weapon/offhand/armour,
  resource loop, three-node progression, strength, and tradeoff. Selection uses
  the class theme, a restrained authored highlight, and a distinct sound without
  allowing decoration to obscure comparison text.
- Character creation prominently displays the chosen Origin Path beneath the hero's
  race/class identity. The approved Rogue paths are **Assassin** and **Skirmisher**.
- Preserve two separate concepts during play: **Origin Path** records the creation
  choice, while **Current Style** is calculated from equipped weapon configuration.
  Equipment may unlock another style without rewriting the hero's origin.
- Show a short custom `COMBAT STYLE CHANGED` notification when equipment changes
  Current Style, listing newly available techniques without using a system modal.
- Add a compact linear three-node progression track to the character profile and
  relevant Inventory presentation. Do not introduce skill points or permanent
  branching until the game supports mutually exclusive ability choices.
- Use `assets/design-references/combat-path-cards-rogue-concept-v1.png` as the
  approved hierarchy and mood reference. Rebuild cards from modular UI components;
  do not embed the flattened concept image as the functional interface.
- Acceptance checks cover 1280×720, 1440×900, and larger displays; keyboard focus,
  hover, selected, pressed, and disabled states; long translated path copy; and a
  gear swap that changes Current Style and can be reversed safely.

### BAL-005 — Percentage ability scaling and dragon preparation

- Status: **Ready after BAL-004**
- Replace oversized flat martial ability bonuses with bounded percentages of the
  relevant combat power plus small utility modifiers; keep enemy-health percentage
  damage out of ordinary player abilities.
- Rebalance Power Strike and Heavy Strike through percentage, resource cost, and
  action tempo so level-two progression feels meaningful without trivializing elites.
- Require level 3 and three identified elite relics to unseal the dragon encounter.
  Objectives and lair feedback show explicit `0/3` recovery and identification
  progress and explain what remains before the seal can be broken.
- Remove the current large per-relic dragon damage bonus and post-armour flat damage
  reduction. Relic equipment and breaking the dragon's ward become the preparation
  reward instead of runaway stacking combat modifiers.
- Retune dragon offense after removing relic guard so its attacks remain legible,
  threatening, and comparable to player ability turns without relying on surprise
  one-shots.
- Extend deterministic coverage across zero, one, two, and three identified relics,
  each class path, and representative ordinary/elite/boss checkpoints. Human runs
  remain authoritative for satisfaction, perceived danger, and tactical clarity.
- Victory does not require repeated critical hits or a single exact build.
- Extra enemy turns remain dangerous but are clearly communicated.

### QA-001 — Playtest protocol

- Status: **Completed**
- `PLAYTESTING.md` defines cautious, exploratory, and attack-spam sessions.
- `PLAYTEST-FINDINGS.md` records builds, seeds, routes, resources, outcomes,
  reproduction evidence, and linked backlog IDs.

- A playtest records build, seed, route, level, equipment, relics, result, and notes.
- Separate checks cover cautious, exploratory, and attack-spamming behavior.
- Findings link to backlog IDs and distinguish defects from balance opinions.

### DOC-001 — Living design source

- Status: **Completed**
- `GAME-DESIGN.md` records intended player-facing rules, design pillars, class
  resources, progression, combat, equipment, economy, world, UI, and balance policy.
- `CHATGPT-CONTEXT.md` provides a portable summary and a required comparison format
  for external design conversations that cannot inspect the repository.
- New accepted mechanics should update the GDD alongside code, tests, backlog, and
  relevant Figma components.

### UX-001 — Ability information

- Status: **Completed**
- Combat-engine-owned descriptions expose cost, tempo, loadout-aware effect,
  prerequisites, status impact, and a current-target outcome estimate.
- The same content is available through mouse tooltips, keyboard focus feedback,
  and accessible descriptions.

- Hover or focus explains cost, tempo, damage/effect, prerequisites, and status impact.
- Locked abilities explain their level or proficiency requirement.
- Keyboard-only players can access the same information.
- Descriptions use the same names and rules as the combat engine.

### UX-002 — Combat timing and feedback

- Status: **Completed**
- Multi-attack and stunned turns are exposed as individual presentation events with
  numbered enemy-turn beats, sequential health changes, log updates, and sound.
- Reduced-motion mode retains every event while shortening animation, initial,
  interval, and settlement waits.
- Existing log styling keeps critical, spell, physical, incoming, healing, and
  status messages visually distinct.

- Each attack in a multi-attack turn is perceptible in sequence.
- Critical, spell, physical, incoming, healing, and status events remain distinct.
- Active statuses show source and remaining duration where applicable.
- Reduced-motion mode remains functional.

### FX-002 — Persistent combat status presentation

- Status: **Completed**
- Guard, Aim, Rage, Stealth, Mark, Stun, Bleed, and Armor Break render as separate,
  color-coded status chips with their owner, magnitude, and remaining duration.
- Bleed and Armor Break now persist and age on enemy turns; the displayed defense
  value reflects active armor reduction.
- Engine regression and visual smoke tests cover effect aging and chip presentation.

### TECH-002 — Performance hot paths

- Status: **Completed**
- `PerformanceSmokeTest` measures hero-preview rules, first/cached image rendering,
  audio initialization, save serialization, and save loading on every full test run.
- Character portraits are pre-rendered on low-priority background threads while the
  title is visible, canceled requests are purged, and bundled ImageIO avoids disk cache.
- Current measurements are written to `build/reports/performance-report.md` so later
  changes can be evaluated as trends rather than anecdotal lag reports.

### LOOT-001 — Loot progression and economy

- Status: **Completed**
- Each class can receive Common or Uncommon weapons, armour, and offhands from
  standard enemies rather than repeatedly rolling one weapon.
- Duplicate avoidance favors a different field item before repeating a known name.
- Shops carry explicitly tiered, class-compatible minor upgrades, with additional
  ranged and magical stock at level two; relic equipment remains the boss bridge.
- Common, Uncommon, and Rare resale rates are 35%, 50%, and 60%, with the existing
  specialist blacksmith bonus and starter/relic/equipped-item protections intact.
- Prepared-route completion moved from 73.4% to 81.8% while quest rushing remains
  a 0% boss strategy in the deterministic exploration baseline.

### MAP-002 — Map intelligence and relic clues

- Status: **Completed**
- Purchased maps explicitly record that terrain and landmarks—not roaming threats—
  were revealed.
- One-use rumors now record distance, direction, terrain context, and relic tradition
  in a bounded persistent map journal without exposing the enemy identity.
- Violet relic-region tooltips explain which specialist may understand a future find.
- The latest clue appears in the map rail and is also available to assistive technology.

### CHAR-002 — Masked portrait frames

- Status: **Completed**
- Portraits render through a rounded opening derived from the fantasy frame instead
  of relying on image aspect ratio or a thicker decorative border.
- The artwork remains underneath the moulding while square corners and resized crops
  cannot bleed outside it.
- Character creation, hero rail, race cards, and compact class identifiers all share
  the same scalable masking contract.

### DSG-001 — Figma synchronization

- Character creation includes the two loadout choices and ability path.
- Character/Inventory includes proficiency and ready/locked ability cards.
- Components use the established fonts, colors, spacing, and interaction states.
- The implementation and Figma screen use the same content hierarchy.

### REL-003 — Opt-in beta-tester credits

- Status: **Ready**
- The feedback survey asks separately whether a tester wants a public credit and how
  that credit should appear: real name, gamer tag/display name, anonymous group
  acknowledgment, or no listing.
- Public-credit permission is optional and never required to submit feedback or
  participate in testing.
- Store only the chosen public display text in the release roster; do not expose
  email addresses, legal names, survey responses, or other identifying information.
- Let testers revise or withdraw permission before the applicable release is
  published, and record the consent date and game version internally.
- Preview the final spelling and presentation of the `Beta Testers` section before
  release, then include it in the in-game credits and packaged attribution file.

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
