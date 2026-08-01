# The Chosen Quest Enhanced Playtest Protocol

Use this protocol for repeatable beta sessions. It complements casual feedback by
recording enough context to reproduce defects and compare balance changes over time.
Copy the session header and the relevant route checklist into `PLAYTEST-FINDINGS.md`.

## Live tester forms

- [General beta playtest feedback](https://forms.gle/GJLCPtzP7LN9PeBeA)
- [Structured bug report](https://forms.gle/F8LtxPn5sKbwPAxX9)

Both forms use project-matched header art stored in `assets/forms/`. Each form writes
to its own private response Sheet. Tester identity, contact details, and public-credit
permission remain optional.

## Session header

- Date and tester:
- Game version/build:
- Operating system and Java version:
- World seed, if known:
- Race / class / starting loadout:
- Player familiarity: new / returning / developer:
- Input method: mouse / keyboard / mixed:

## Run A — cautious quest play

Play as though survival matters. Read the interface, use Defend or its class variant,
visit useful services, compare equipment, and prepare before approaching the dragon.

- [ ] Character creation explains the selected build and ability path.
- [ ] The first minute makes movement, map discovery, and the immediate objective clear.
- [ ] Shop filters, comparisons, buying, selling, and vendor specialization are understandable.
- [ ] Standard fights teach the core actions without requiring unlocked abilities.
- [ ] An elite reward is visually obvious and its identification hint is actionable.
- [ ] Ability hover and keyboard focus explain cost, tempo, effect, and estimated outcome.
- [ ] Multi-attack turns are perceptible and the combat log explains their order.
- [ ] Level, loadout, equipment, potions, and identified relics feel sufficient for a fair boss attempt.
- [ ] Victory or defeat explains what happened and offers a clear next action.

## Run B — exploratory play

Prioritize fog of war, rumors, optional landmarks, the Ashweb Nest, map zoom, panning,
and revisiting locations before advancing the main quest.

- [ ] Unknown, scouted, visited, rumored, and occupied tiles are distinguishable.
- [ ] Off-screen hints help without revealing threats the hero has not learned about.
- [ ] Map minis remain inside tiles at both zoom levels.
- [ ] Rumor/advice interactions cannot be repeatedly farmed.
- [ ] The Spider Nest communicates its finite state and cannot grant unlimited progression.
- [ ] Backtracking and roaming enemies do not trap the hero unfairly.
- [ ] Exploration remains purposeful rather than becoming repetitive travel.

## Run C — attack-spam stress test

Move quickly, skip optional preparation, and favor the basic attack. This is intended
to reveal unclear rules, stuck states, and whether reckless play is still too rewarding.

- [ ] Every combat ends in a valid victory, defeat, or flee state.
- [ ] A second encounter works after the first without stale buttons or artwork.
- [ ] Rapid character-creation switching remains responsive and shows the final choice.
- [ ] Rapid key presses do not duplicate rewards, turns, dialogue, or location actions.
- [ ] Standard enemies permit recovery from minor mistakes, while elites punish repetition.
- [ ] An unprepared boss attempt is dangerous without appearing broken or frozen.

## Run D — update-check baseline and handoff

Keep the first update-aware beta installed after the normal playtest. This route
requires two published prereleases and should be completed in order.

- [ ] In the baseline build, open Settings and choose **Check Updates**.
- [ ] Before the next beta exists, the baseline reports its own version as current.
- [ ] Publish the next beta with a higher prerelease number and a matching official ZIP.
- [ ] Relaunch the older baseline while online; startup remains responsive and a styled
      update notice appears after the game opens.
- [ ] **View Changes** opens the official release information.
- [ ] **Remind Me Later** closes the notice without modifying the installation or save.
- [ ] A later manual check can show the same available update again.
- [ ] **Download** opens the exact official GitHub release asset in the system browser.
- [ ] With networking unavailable, startup and play remain unaffected and no system
      error dialog appears.
- [ ] Record the installed version, discovered version, operating system, and result.

## End-of-run record

Record the final level, current/max health and class resource, equipped weapon/armour/offhand,
potions, identified and unidentified relics, gold, route summary, result, and approximate
play time. Then add the three best moments, three confusing moments, and one desired
change. Treat observations as evidence; do not rewrite rules from one isolated run.

## Optional beta-tester credit consent

Add the following to the beta feedback survey. Keep it optional and separate from
the gameplay questions.

**Would you like to be acknowledged in the public game credits as a beta tester?**

- Yes — use my real name
- Yes — use my gamer tag or display name
- Please thank me only as part of the anonymous beta-testing group
- No public credit

If either named option is selected, ask: **Exactly how should your credit appear?**
Collect only the requested public display text. Follow it with this note:

> This is optional and does not affect your ability to test the game or submit
> feedback. Your email address and survey answers will not be published. You may
> change or withdraw this permission before the applicable game release by contacting
> the project owner.

Internally record the response date and tested game version, but never place contact
information or unapproved names in the public roster.

## Reporting findings

- **Defect:** the game contradicts its own rule, becomes stuck, loses input, renders
  incorrectly, or behaves inconsistently under the same conditions.
- **Balance observation:** the rule works, but its difficulty, reward, pacing, or build
  viability feels wrong. Include the exact build and preparation state.
- **Usability observation:** the rule works, but the tester could not discover or
  understand it without help.
- **Preference:** an aesthetic or play-style opinion without a clear failure.

Use a stable backlog ID when one applies (`BAL-002`, `UX-002`, `MAP-002`, etc.).
Create a new ID in `BACKLOG.md` before implementing a genuinely new scope item.
