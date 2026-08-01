# The Chosen Quest Enhanced — 0.7.0-beta.2

This private prerelease completes the first end-to-end test of the in-game updater
and packages the latest presentation and reward-feedback refinements.

## Updater validation

- Settings is now accessible from both the title screen and the active game.
- Long release names and version numbers wrap safely inside the update card.
- **View Changes** opens the styled in-game release notes instead of leaving the
  game or triggering the macOS Java crash seen during the beta.1 test.
- Download links use the operating system's supported browser handoff on macOS.
- Startup and manual update checks remain non-blocking and offline-safe.

## Player-facing refinements

- Ordinary combat equipment now receives a styled loot reveal with item artwork,
  quality color, equipped-item comparison, discovery audio, and direct Inventory
  access; the combat log remains the permanent record.
- Empty equipment slots use a neutral recessed socket instead of a misleading
  shield icon, and compact views no longer repeat “No offhand” as an item name.
- Character creation retains the expanded painterly Art Deco component treatment,
  responsive hero presentation, corrected Dwarf Rogue artwork assignment, and
  clearer equipment/profile alignment.

## How to test the updater

1. Keep your extracted `0.7.0-beta.1` folder.
2. Launch beta.1 while online and open **Settings > Check Updates**.
3. Confirm that `0.7.0-beta.2` is offered as the newer beta release.
4. Select **View Changes** and confirm these notes open inside the game without a
   crash; close them and verify the title screen or adventure remains responsive.
5. Reopen the notice and test **Remind Me Later** and the official download handoff.
6. Launch either build without internet access and confirm gameplay remains usable.

The updater does not replace files or saves automatically in this phase. It only
checks the official GitHub release channel and hands downloads to the system browser.

## Reporting

- General feedback: https://forms.gle/GJLCPtzP7LN9PeBeA
- Bug reports: https://forms.gle/F8LtxPn5sKbwPAxX9

Please include the game version, operating system, Java version, hero build, and
clear reproduction steps with defect reports.
