# The Chosen Quest Enhanced — 0.8.0-beta.1

This private prerelease introduces Combat Paths, carries hero identity throughout the
game, and packages the latest interface, reward, balance, and stability work for human
playtesting.

## Combat Paths and hero identity

- Replaced legacy starting-loadout labels with eight named Combat Paths:
  Knight/Berserker, Channeler/Arcanist, Assassin/Skirmisher, and Ranger/Marksman.
- Added detailed path cards covering equipment, resource loop, techniques, strengths,
  and tradeoffs.
- Preserved an Origin Path while equipped gear determines the hero's Current Style.
- Added a non-modal style-change notice when equipment unlocks a different technique set.
- Extended class and race identity through the hero rail, resources, Inventory actions,
  combat actions, and outcome framing without overriding danger or item-quality colors.

## Gameplay and equipment

- Aligned starter equipment and techniques with each Combat Path's stated play style.
- Converted martial bonuses to bounded percentage scaling and rebuilt dragon preparation
  around level 3 plus three identified relics.
- Added an explicit Equip Now decision after relic identification; declined relic gear
  remains safely in Inventory.
- Added upgrade-aware purchase and loot cards with direct Equip Now and Inventory actions.
- Made vendor equipment finite per shop and protected repeat purchase events from charging
  the player twice; consumable potions remain restockable.

## Interface and stability

- Rebuilt the persistent hero rail on one alignment grid with a larger 3:4 portrait,
  clearer equipment artwork, and consistent meter and statistic edges.
- Simplified Inventory around the item list, added a themed cycling sort control, and
  retained hero identity without duplicating the persistent rail.
- Audited interface text bounds and all 32 character portraits at representative display
  sizes, correcting portrait-bottom spacing and expanding visual smoke coverage.
- Added cached hero-art crossfades and a short Begin Journey confirmation, both skipped
  immediately when reduced motion is enabled.
- Made development JAR replacement atomic to prevent Java 8 libzip crashes when building
  while another development copy is running.

## Updating from 0.7.0-beta.2

1. Keep an extracted `0.7.0-beta.2` folder and launch it while online.
2. Open **Settings > Check Updates** and confirm `0.8.0-beta.1` is offered.
3. Verify **View Changes**, **Remind Me Later**, and the official download handoff.
4. Extract the new release to a separate folder; the updater does not replace files.
5. Load a beta.2 save and confirm it opens with equipment intact, a migrated Origin Path,
   and an equipment-derived Current Style.
6. Launch the new release offline and confirm normal play remains available.

## Playtest focus

- Compare both Combat Paths for any class that interests you.
- Equip gear associated with the alternate path and verify Current Style changes while
  Origin Path remains unchanged, including after save/load.
- Accept and decline relic Equip Now, test finite vendor gear, and watch for duplicate
  charges or missing Inventory items.
- Report clipped text, portrait gaps, missing artwork, stale controls, confusing technique
  availability, balance spikes, and unexpected save-migration behavior.
- If possible, test at 1280×720 or 1440×900 and note whether reduced motion is enabled.

## Reporting

- General feedback: https://forms.gle/GJLCPtzP7LN9PeBeA
- Bug reports: https://forms.gle/F8LtxPn5sKbwPAxX9

Please include the game version, operating system, Java version, hero race/class,
Origin Path, Current Style, level, important equipment, and clear reproduction steps.
