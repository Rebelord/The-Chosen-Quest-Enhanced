# The Chosen Quest Enhanced — Project Migration Handoff

Last updated: **2026-08-13**

This is the practical handoff for moving development to another computer or
starting a fresh Codex/ChatGPT thread. The durable rules remain in
`GAME-DESIGN.md`; task status lives in `BACKLOG.md`; external design discussions
should begin with `CHATGPT-CONTEXT.md`.

## Repository and external references

- GitHub: `git@github.com:Rebelord/The-Chosen-Quest-Enhanced.git`
- Branch: `main`
- Previous published base before this migration checkpoint:
  `c1181f9 Record successful updater validation`
- Current application version: `0.7.0-beta.2`
- Published beta:
  https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases/tag/v0.7.0-beta.2
- Figma design:
  https://www.figma.com/design/eQ5UREjfLHuuyq19bjeilY/Untitled?node-id=0-1
- Feedback form: https://forms.gle/GJLCPtzP7LN9PeBeA
- Bug form: https://forms.gle/F8LtxPn5sKbwPAxX9

Dropbox is no longer the routine distribution path. GitHub Releases and the
in-game update checker are the intended tester workflow.

## Important migration warning

At the time of this handoff, the working tree contains substantial **uncommitted
changes**. A fresh clone alone will not contain them. Before retiring the current
computer, either:

1. review, commit, and push the current changes to GitHub; or
2. copy the complete `TheChosenQuest-Desktop` directory, including untracked files,
   to the new computer and verify it before deleting the original.

Run these before migration:

```sh
git status --short
git diff --check
./test.sh
```

Do not use destructive Git cleanup commands while this working tree is dirty.

## Setup on the new computer

1. Install Git and a JDK. Java 8 or newer builds/runs the JAR; JDK 14 or newer is
   needed only for `jpackage` native app images.
2. Clone the repository:

   ```sh
   git clone git@github.com:Rebelord/The-Chosen-Quest-Enhanced.git
   cd The-Chosen-Quest-Enhanced
   ```

3. Build and test:

   ```sh
   ./build.sh
   ./test.sh
   ```

4. Run:

   ```sh
   java -jar build/TheChosenQuest-Desktop.jar
   ```

5. Confirm GitHub authentication before the first release operation.

The build script creates a temporary JAR and atomically swaps it into place. Keep
this behavior: Java 8 can crash in native `libzip` with `SIGBUS` if a running JAR is
rewritten in place while later music, artwork, or classes are being loaded.

## Implemented locally after the last committed checkpoint

These changes were implemented and passed the complete automated suite locally,
but must be confirmed in `git status` because they may not yet be committed:

- Finite per-shop equipment stock; purchased equipment disappears and cannot charge
  the player twice. Repeatable consumables remain available.
- Upgrade-aware purchase and loot cards with optional **Equip Now**.
- Relic identification no longer auto-equips its reward. Relic gear has a distinct
  equip flourish whether equipped from the reward card or Inventory.
- Inventory removes duplicated hero statistics/equipped summaries, uses a themed
  sort control, tighter item rows, and clearer comparison actions.
- Persistent hero rail uses a wider shared alignment grid, a correctly proportioned
  3:4 portrait, larger equipment art, and icon counters for gold and potions.
- Development JAR production is atomic to prevent the native Java 8 `libzip` crash.
- Regression and UI coverage was extended for stock, optional equip, portrait ratio,
  and acquisition actions.

The last full local suite reported passing engine, regression, updater, 32-build
balance, exploration, performance, and enhanced UI smoke tests.

## Approved next combat direction

The next mechanical wave is `BAL-004`, followed by `BAL-005`.

### Combat Paths instead of hidden loadouts

- Do not add a traditional branching skill tree yet.
- Character creation should present two prominent **Combat Path** cards per class.
- **Origin Path** records the creation choice.
- **Current Style** is derived from currently equipped weapons/offhand.
- Changing equipment may change Current Style without rewriting Origin Path.
- Show a custom `COMBAT STYLE CHANGED` notice when a gear swap changes techniques.
- Show a compact linear three-node ability progression rather than skill points.

Approved visual reference:

`assets/design-references/combat-path-cards-rogue-concept-v1.png`

This is a flattened mood/hierarchy reference only. Functional cards must be built
from modular UI components and support all interaction states and target sizes.

### Fighter/Warrior paths

Approved build names:

- **Knight**: one-handed sword, starter shield, chain armour; shield techniques.
- **Berserker**: two-handed weapon, leather armour, empty offhand; heavy techniques.

The display name **Fighter** versus **Warrior** remains a final naming decision.
Internal path identifiers should move from `VANGUARD`/`BREAKER` to
`KNIGHT`/`BERSERKER`, with a trivial loader migration for old saves.

Equipment rules:

- One-handed weapon + shield: Shield Bash and a combined Guard/counter technique.
- Two-handed weapon: Heavy Strike and weapon mastery.
- One-handed weapon without shield: Power Strike fallback.
- Do not call the combined shield counter `Riposte`; the final name remains open.
- Reserve `Cleave` for future multi-target combat rather than a single-target hit.
- Rage should come from aggression, damage, or a successful counter—not merely
  selecting a passive defensive action.

Keep armour as straightforward Defense during this wave. Do not add armour-weight,
speed, Mana-efficiency, or other cross-system modifiers until testing proves they
are needed. A simple initial budget is Knight 7 Attack + 4 protection versus
Berserker 9 Attack + 2 protection.

### Rogue paths

Approved replacement for Duelist/Quick Knives:

- **Assassin**: single dagger, leather armour, empty offhand; Stealth, Vanish,
  Ambush, and Execute. Patient burst from hidden states.
- **Skirmisher**: dual weapons and leather armour; Twin Strike, Evasive Strike,
  and Blade Flurry. Fast Momentum-building hit-and-run combat.

Both paths retain Momentum but build/spend it through different loops. No ability
may require equipment absent from its starting path.

### Mage and Hunter review

Recommended but not fully approved:

- Mage: **Channeler** and **Battlemage**. Current `Spellblade` carries a wand and
  tome and therefore contradicts its name. The two paths also need real mechanical
  differentiation rather than one being a stronger equipment package.
- Hunter: **Ranger** and **Marksman** names remain good. Marksman still needs its
  promised heavy opening shot and a meaningful Hunter's Mark relationship;
  Piercing Bolt should likely require/consume the mark as Volley does.

## Approved ability and boss balance direction

- Use hybrid ability scaling: a bounded percentage of relevant combat power plus
  small flat utility modifiers, then defense. Do not use ordinary enemy-health
  percentage damage.
- Basic attack is approximately 100% Attack. Starting hypotheses include Power
  Strike around 135%, with cost/tempo doing part of the balancing.
- Heavy two-handed attacks should be Slow and meaningfully committed.
- Require level 3 and **three identified elite relics** to unseal the dragon.
- Objectives should show explicit recovery/identification progress such as `0/3`.
- Remove the current large additive per-relic dragon damage and post-armour flat
  damage reduction. Relic equipment and breaking the ward become the reward.
- Retune dragon offense after removing relic guard.
- Extend simulations across zero, one, two, and three identified relics.

This direction came from human playtests: level-two Fighter Power Strike felt too
strong, two relics made dragon attacks feel trivial compared with player abilities,
and reaching the boss did not require enough elite preparation.

## Documentation map

- `README.md` — build/run/release overview
- `GAME-DESIGN.md` — authoritative implemented game rules
- `CHATGPT-CONTEXT.md` — portable external design context
- `BACKLOG.md` — durable task source of truth and acceptance criteria
- `ROADMAP.md` — milestone sequence
- `CHANGELOG.md` — implemented user-facing changes
- `PLAYTESTING.md` — tester procedure
- `PLAYTEST-FINDINGS.md` — recorded human observations
- `BRAND-VISUAL-SYSTEM.md` — shared visual/marketing rules
- `UI-MATERIAL-ASSET-PIPELINE.md` — modular UI asset workflow

## Recommended first steps after migration

1. Confirm the dirty work was committed and appears on the new computer.
2. Run `./test.sh` before changing mechanics.
3. Implement `BAL-004` engine rules and save migration before redesigning cards.
4. Re-run simulations and conduct human checks for Knight, Berserker, Assassin, and
   Skirmisher.
5. Implement `CHAR-009` Combat Path cards and persistent Origin/Current Style UI.
6. Implement `BAL-005` scaling and three-relic dragon gate.
7. Sync the settled components to Figma through `DSG-001`.
