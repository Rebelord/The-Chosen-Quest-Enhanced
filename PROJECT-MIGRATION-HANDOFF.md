# The Chosen Quest Enhanced — Project Migration Handoff

Last updated: **2026-08-24**

This is the practical handoff for moving development to another computer or
starting a fresh Codex/ChatGPT thread. The durable rules remain in
`GAME-DESIGN.md`; task status lives in `BACKLOG.md`; external design discussions
should begin with `CHATGPT-CONTEXT.md`.

## Repository and external references

- GitHub: `git@github.com:Rebelord/The-Chosen-Quest-Enhanced.git`
- Branch: `main`
- Current release checkpoint: `v0.8.0-beta.1`
- Current application version: `0.8.0-beta.1`
- Published beta:
  https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases/tag/v0.8.0-beta.1
- Figma design:
  https://www.figma.com/design/eQ5UREjfLHuuyq19bjeilY/Untitled?node-id=0-1
- Feedback form: https://forms.gle/GJLCPtzP7LN9PeBeA
- Bug form: https://forms.gle/F8LtxPn5sKbwPAxX9

Dropbox is no longer the routine distribution path. GitHub Releases and the
in-game update checker are the intended tester workflow.

## Release checkpoint and generated files

The `v0.8.0-beta.1` tag is the portable source checkpoint for this release. A fresh
clone contains the source, release notes, generator scripts, and tracked tester PDFs.
Generated game archives, checksums, portfolio handoff files, and versioned invitation
kit ZIPs are intentionally not required for source restoration and may remain local.

Run these before migration:

```sh
git status --short
git diff --check
./test.sh
```

Do not use destructive Git cleanup commands when local generated artifacts or later
uncommitted work must be preserved.

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

## 0.8.0-beta.1 release scope

These changes are part of the tagged release and passed the complete automated suite:

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

## Implemented Combat Path direction

`BAL-004`, `BAL-005`, `CHAR-009`, and `DSG-003` are implemented in this release.

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

The class display name remains **Fighter**. Internal path identifiers migrated from
`VANGUARD`/`BREAKER` to `KNIGHT`/`BERSERKER` with compatibility for older saves.

Equipment rules:

- One-handed weapon + shield: Shield Bash and a combined Guard/counter technique.
- Two-handed weapon: Heavy Strike and weapon mastery.
- One-handed weapon without shield: Power Strike fallback.
- The combined shield counter is named **Shield Counter**.
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

### Mage and Hunter paths

Implemented decisions:

- Mage: **Channeler** and **Arcanist**, with different starter gear and technique loops.
- Hunter: **Ranger** and **Marksman**; Marksman receives its promised opening-shot
  bonus and both paths retain meaningful Mark/Volley relationships.

## Implemented ability and boss balance direction

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

1. Check out tag `v0.8.0-beta.1` or current `main` and run `./test.sh`.
2. Use the retained `0.7.0-beta.2` build to verify updater discovery and save migration.
3. Conduct human checks across the eight Combat Paths, relic decisions, and finite vendors.
4. Continue `DSG-001` and `DSG-002` Figma presentation synchronization.
5. Continue custom VFX/audio work and the planned Classic/browser preservation tracks.
