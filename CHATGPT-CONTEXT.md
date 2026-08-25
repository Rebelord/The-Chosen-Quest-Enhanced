# The Chosen Quest Enhanced — ChatGPT Design Context Packet

Copy this document into a ChatGPT conversation before proposing or evaluating new
game-design ideas. It is a compact companion to `GAME-DESIGN.md`, not a replacement.

---

## Instructions for the design assistant

You are helping design **The Chosen Quest Enhanced**, a compact, illustrated,
offline turn-based RPG. Treat the facts below as constraints from the current
playable build.

When evaluating an idea:

1. Separate **current implemented behavior**, **the proposed change**, and **your
   recommendation**.
2. Identify direct conflicts with existing class identities, resources, progression,
   relics, equipment, world pacing, interface, or balance principles.
3. Prefer extending an existing rule over adding a parallel subsystem.
4. Avoid unlimited farming, repeatable healing, unrestricted ability spam, automatic
   world scaling, and complexity that does not create a meaningful player decision.
5. State dependencies: code systems, interface components, new statuses, art, audio,
   content, save migration, documentation, and tests.
6. Explain how the player discovers and understands the rule.
7. Include likely exploits and at least one acceptance/playtest scenario.
8. Mark numerical tuning as a starting hypothesis, not established fact.
9. Do not assume access to the repository, Figma, local files, or Codex conversation.
10. If information is missing, ask for the relevant GDD section or current mechanic
    instead of inventing a conflicting rule.

Use this response format:

- **Compatibility:** What fits the current game
- **Conflicts:** What would need to change or be removed
- **Recommended design:** The smallest coherent rule set
- **Player-facing explanation:** How the game communicates it
- **Balance risks and exploits**
- **Implementation dependencies**
- **Playtest acceptance criteria**

---

## Current game identity

- Small turn-based RPG with a finite quest and generated 13×13 world
- Primary authored progression curve: levels 1–3
- Four races: Human, Dwarf, Elf, Halfling
- Four classes: Fighter, Mage, Rogue, Hunter
- Two starting Combat Paths per class
- Standard enemies, elites with unidentified relics, and one dragon boss
- Fog of war, rumors, maps, roaming biome-compatible enemies, shops, tavern,
  alchemist encampment, and a finite Spider Nest
- Offline Java desktop application with keyboard and mouse support
- Portfolio-quality fantasy UI synchronized with Figma

## Design pillars

- Readable tactics and combat feedback
- Distinct race/class/path identities
- Preparation and relic progression matter
- Finite progression without grinding or farming
- Authored visual/audio presentation
- Offline accessibility and straightforward controls

## Current progression

- Level 1: core actions; Mage knows Magic Missile
- Level 2: first tactical class ability; Mage gains Fireball
- Level 3: mastered-weapon technique and signature ability; Mage gains Ice Spike
- Relics never bypass level or weapon-mastery requirements
- Shops provide minor upgrades; identified elite relics bridge into boss combat

## Current classes

### Fighter

- Resource: Rage, maximum 100, resets each encounter
- Builds from basic attacks, incoming damage, and two-handed Battle Cry
- Tactical ability costs 30; mastered technique costs 50
- Knight-style equipment supports Shield Counter and defense
- Berserker-style equipment supports Battle Cry and Heavy Strike
- Second Wind is a level-three passive: once per encounter, a fatal hit leaves
  1 health, fills Rage, and empowers the next basic attack
- Second Wind is not healing and is not a clickable ability

### Mage

- Resource: Mana
- Magic Missile 7, Fireball 12, Ice Spike 15
- Channel Ward reduces damage and restores some mana
- Weak basic melee attacks; spells provide direct defense-bypassing damage
- Spells are direct action buttons, not a system-modal selector

### Rogue

- Resource: Momentum, maximum 100, resets each encounter
- Builds from basic attacks, entering Stealth, and successful Stealth evasion
- Assassin uses Ambush, Vanish, and Execute; Skirmisher uses Twin Strike,
  Evasive Strike, and Blade Flurry
- Fastest class cadence; Stealth burst and dual-wield pressure identities
- Execute is strongest against targets at or below 35% health

### Hunter

- Resource: Focus; maximum 60/80/100 at levels 1/2/3, later capped at 120
- Starts with at least one-third maximum Focus and retains earned Focus between fights
- Builds Focus through Take Aim, accurate shots, aimed shots, guided marked shots,
  and enemy misses
- Taking damage disrupts Aim
- Pinning Shot costs 25 and prevents only the next melee action
- Hunter's Mark is a Fast setup action and marks one target
- Volley costs 50 Focus, requires Hunter's Mark, and consumes the mark
- Volley may be powerful but cannot be chained without rebuilding and remarking

## Current combat

- Fast, Normal, and Slow action tempo
- Speed uses class, race, level difference, equipment weight, enemy, and tier
- At most two queued enemy actions are presented individually
- Enemy intent displays its next named action and tempo
- Statuses include Guard, Aim, Stealth, Rage, Momentum, Focus, Mark, Pin, Stun,
  Bleed, and Armor Break
- Combat actions are mapped to number keys 1–7
- Fleeing returns the hero to a safe nearby tile

## Equipment and loot

- Weapon, Armour, and Offhand slots
- Two-handed weapons clear the offhand
- Weapon traits include Concussive, Sundering, Bleeding Edge, Precise, Armor
  Piercing, Balanced Guard, and Arcane Focus
- Common, Uncommon, Rare, and Relic quality
- Standard enemies may drop class-compatible Common/Uncommon equipment
- Inventory and shops show comparison indicators and sorting
- Vendors are specialized; selling protects starter, equipped, and relic items

## Relics and bosses

- Normal elites carry unidentified relics
- Each relic identifies a required specialist vendor
- Discovery uses a custom popup, chime, inventory entry, and hint
- Identified relic equipment is expected preparation for dragons
- Level 3 and all three identified relics are required to unseal the dragon lair
- Relics do not add large flat boss damage or post-armour guard bonuses
- Bosses should be dangerous without requiring a single exact build
- Quest rushing should remain a poor boss strategy

## World and optional sources

- Finite 13×13 generated grid with field, lake, crypt, shops, tavern, encampment,
  Ashweb Nest, and dragon lair
- Fog states: Unknown, Scouted, Visited
- Full map on M; 5×5, 7×7, and 9×9 viewport options
- Enemies roam within biome restrictions; safe landmarks remain protected
- Ashweb Nest has two standard reduced-reward spiders followed by an elite-presented
  level-two Ashweb Matriarch
- Nest rewards are permanently capped at 12 XP and 30 gold
- Nest enemies never drop equipment or elite relics

## Current balance guardrails

- Standard encounters teach and are reliably survivable
- Elites punish basic-attack repetition
- Prepared level-three builds should have credible boss paths
- No active ability should be infinitely repeatable without resource or setup
- Strong burst is acceptable when preparation is visible and costly
- Human playtests override simulation-only conclusions about fun and clarity
- Current validation target: Fighter Rage/Second Wind, Rogue Momentum, Hunter
  Focus/Mark/Volley/Pinning, and Ashweb Matriarch difficulty

## Current development priorities

1. Human validation of the revised Knight, Berserker, Assassin, and Skirmisher loops
2. Synchronize settled Combat Path and progression components with Figma (`DSG-001`)
3. Complete the remaining character-creation material and race-heraldry fidelity work
4. Continue cohesive old-school fantasy audio, distinctive ability effects, and
   uniform equipment/relic artwork

## Implemented Combat Path and balance wave

The following rules are current playable behavior. Human playtesting remains the
next authority for feel and tuning; acceptance criteria live in `BACKLOG.md`.

### Combat Path presentation

- Use two prominent **Combat Path** cards per class instead of a branching skill tree.
- Each card communicates fantasy, starting equipment, resource loop, a compact
  three-node progression, strength, and tradeoff.
- **Origin Path** preserves the character-creation decision.
- **Current Style** derives from equipped weapon/offhand requirements and may change
  without rewriting Origin Path.
- A custom `COMBAT STYLE CHANGED` notice explains technique changes caused by gear.
- The notice is non-modal, remains readable with reduced motion, and explicitly
  confirms that Origin Path was preserved. Creation cards include accessible
  fantasy, equipment, resource-loop, progression, and tradeoff copy.
- The flattened Rogue visual reference is stored at
  `assets/design-references/combat-path-cards-rogue-concept-v1.png`.

### Fighter/Warrior path correction

- Fighter paths are **Knight** and **Berserker**; legacy values migrate safely.
- Knight starts with a one-handed sword, shield, and chain armour.
- Berserker starts with a two-handed weapon, leather armour, and an empty offhand.
- One-handed + shield supports the combined **Shield Counter** guard/counter technique.
- Two-handed weapons support Heavy Strike and mastery. One-handed weapons without a
  shield retain Power Strike as a fallback.
- Reserve `Cleave` for future multi-target combat.
- Rage should reward attacks, taking damage, and successful counters rather than a
  passive Defend action.
- Keep armour as simple Defense for now; do not add weight-based subsystems solely
  to justify different starting armour.
- The player-facing class name remains **Fighter**.

### Rogue path correction

- **Assassin**: single dagger and empty offhand; patient Stealth/Vanish/Ambush/Execute
  burst play.
- **Skirmisher**: dual weapons; Twin Strike/Evasive Strike/Blade Flurry hit-and-run
  play.
- Both use Momentum through distinct build/spend loops.
- No path may promise an action that its starting equipment cannot perform.

### Ability and boss balance

- Scale damaging abilities from a bounded percentage of relevant combat power plus
  small flat utility modifiers; do not scale ordinary attacks from enemy maximum HP.
- Treat 100% Attack as the basic-attack baseline. Any numerical multiplier remains a
  test hypothesis until simulation and human play confirm it.
- Require level 3 and three identified elite relics before the dragon can be unsealed.
- Show explicit relic progress such as `0/3` in objectives.
- Remove large additive per-relic dragon damage and post-armour flat protection;
  identified relic equipment and ward removal should provide the preparation value.
- Re-test boss offense and outcomes at zero, one, two, and three identified relics.

## Proposal to evaluate

Paste the new idea below this line. Include the intended player benefit and any
specific numbers only if they are important to the concept.

**Idea:**

**Player problem or opportunity:**

**Desired feeling:**

**Rules or numbers already being considered:**
