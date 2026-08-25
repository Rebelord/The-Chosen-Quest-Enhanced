# The Chosen Quest Enhanced — Living Game Design Document

Last reconciled with the playable build: **2026-07-31**  
Current private-testing version: **0.8.0-beta.1**
Current save/world schema: **10**

## How to use this document

This document is the source of truth for intended player-facing rules and design
principles. The code and automated tests describe what the current build actually
does. A disagreement between them is either an implementation defect or documentation
drift and should be resolved deliberately.

- `GAME-DESIGN.md`: intended game rules and product direction
- `BACKLOG.md`: work status, priority, and acceptance criteria
- `ROADMAP.md`: implementation sequence
- `PLAYTEST-FINDINGS.md`: human evidence and reproducible observations
- `CHANGELOG.md`: completed changes grouped by application release
- Figma: visual hierarchy, components, states, and presentation specification

New proposals should be compared against this document before entering the backlog.
Major accepted changes should update this document alongside implementation.

## 1. Product vision

The Enhanced Edition is a polished, illustrated, portfolio-ready interpretation of
the original small turn-based RPG envisioned by its cofounders. It should retain the
clarity and compact quest structure of the source while giving each character build
a recognizable tactical identity.

The original experience remains a separate future Classic edition. Enhanced is not
intended to overwrite or silently redefine the preserved source.

### Design pillars

1. **Readable tactics:** Players should understand what happened, why it happened,
   and what their available responses will do.
2. **Distinct builds:** Race, class, Combat Path, equipment, and relic choices should
   produce different rhythms without creating obvious trap builds.
3. **Preparation matters:** Shops help against elites; elite relic progression helps
   against dragons; rushing the final boss should remain dangerous.
4. **Bounded progression:** The compact quest should not depend on endless grinding,
   renewable loot farms, or level-scaled enemies that erase progress.
5. **Authored presentation:** Important discoveries, attacks, statuses, rewards, and
   transitions should feel intentional rather than like system-dialog placeholders.
6. **Offline accessibility:** The primary game remains self-contained, keyboard
   accessible, and distributable without an online service dependency.

### Current non-goals

- Real-time or action combat
- An endless procedural campaign or live-service economy
- Unlimited enemy, XP, gold, or relic farming
- A large tabletop-style character-stat spreadsheet
- Automatic world scaling that makes every enemy match the hero
- Gender-dependent statistics or combat advantages
- Multiplayer or online account systems

## 2. Core game loop

1. Create a named hero by choosing race, class, and one of two Combat Paths.
2. Explore a generated 13×13 world through a scrolling, fog-aware map.
3. Learn terrain, landmarks, rumors, relic regions, and known threats.
4. Defeat standard enemies for experience, gold, and occasional field equipment.
5. Use specialized vendors to compare, buy, sell, rest, seek advice, and identify
   relics.
6. Defeat elite enemies to recover unidentified relics.
7. Bring each relic to its indicated specialist for identification and equipment.
8. Reach level 3 and identify all three elite relics to unseal the dragon lair.
9. Defeat the dragon to complete the quest.

Optional content, such as the Ashweb Nest, should offer bounded rewards and a
meaningful side objective without becoming mandatory or farmable.

## 3. Character creation

### Races

| Race | Current identity |
|---|---|
| Human | Balanced health and attack; adaptable baseline |
| Dwarf | Highest durability and defense; slower turn cadence |
| Elf | Faster, stronger magical affinity; Mages also receive additional mana |
| Halfling | Fastest racial cadence and additional starting gold |

Race modifies the class rather than replacing it. Every race/class combination must
remain viable, though strengths and difficulty may differ.

### Classes and Combat Paths

| Class | Path A | Path B |
|---|---|---|
| Fighter | Knight — longsword, iron shield, and chain armour | Berserker — greatsword, leather armour, high-risk power |
| Mage | Channeler — staff and cloth, reliable spellcasting | Arcanist — wand, tome, and leather, focused magic |
| Rogue | Assassin — single dagger and leather, Stealth burst | Skirmisher — dual daggers and leather, evasive multi-hit pressure |
| Hunter | Ranger — long bow and leather, mobile ranged combat | Marksman — crossbow and leather, heavier opening damage |

All starting equipment is Common quality. Character creation previews current
health, class resource, equipment, play style, racial traits, and the level-one
through level-three ability path. Origin Path records the creation choice; Current
Style derives from equipped weapon and offhand configuration and may change safely.
Each path card exposes its fantasy, equipment, resource loop, progression, and
tradeoff. A reversible style change preserves Origin Path and produces a short
non-modal notice naming the newly available techniques.

The offline fantasy-name generator follows the selected race and class style. Naming,
gender/presentation, and portrait choice are cosmetic unless a future design change
is explicitly approved.

## 4. Progression

The authored quest curve is primarily levels 1–3. Additional numeric levels may be
possible in code, but content and ability complexity beyond level three are not yet
an approved progression layer.

| Level | Intended complexity |
|---|---|
| 1 | Core attack, class defense/setup action, consumable, and flee; Mage knows Magic Missile |
| 2 | First loadout-aware tactical ability; Mage gains Fireball |
| 3 | Mastered-weapon technique and class signature; Mage gains Ice Spike |

Non-Mage ability rules:

- Slot 1 unlocks at level 2.
- Slot 2 unlocks at level 3 and requires mastery of the equipped weapon family.
- Slot 3 unlocks at level 3.
- Relics never bypass level or proficiency requirements.

Weapon proficiency advances through meaningful level progression rather than
repeatable attack grinding: Trained, Proficient, then Mastered.

## 5. Combat model

Combat is turn-based with speed-adjusted action timing.

- Actions have Fast, Normal, or Slow tempo.
- Race, class, level difference, equipment weight, enemy type, and encounter tier
  influence effective speed.
- No combatant should run away with an unreadable number of consecutive actions.
  The engine limits queued enemy responses to two and forces a response after
  excessive consecutive hero actions.
- Multi-action enemy turns are presented as separate visual, audio, health, and log
  events.
- Enemy intent identifies the next named attack and its tempo.
- Fleeing returns the hero to a nearby safe tile; bosses recover fully and lesser
  enemies recover partially.

### Core combat actions

- **Attack:** Loadout-appropriate basic physical strike.
- **Class defense/setup:** Defend, Battle Cry, Channel Ward, Stealth, or Take Aim.
- **Abilities:** Numbered direct actions; locked or unavailable actions explain why.
- **Potion:** Restores 20 health and consumes one potion.
- **Flee:** Attempts a safe retreat.

### Damage and defense

Physical damage compares attack against enemy defense. Spell damage bypasses ordinary
physical defense. Bosses receive bounded relic-era tuning rather than blanket
immunity. Enemy armor can absorb damage, and traits or abilities may pierce, sunder,
bleed, stun, guard, pin, or mark.

Critical, spell, physical, incoming, healing, and status events use distinct combat-log
styles. Active effects appear as individual owner-aware status chips.

## 6. Class combat identities

### Fighter — Rage and commitment

Fighters are durable melee combatants who build Rage by engaging with danger.

- Maximum Rage: 100
- Basic attack: +14 Rage
- Damage received: +8 plus half of applied damage, capped at +24
- Two-handed Battle Cry: +25 Rage and consumes an action
- Tactical ability: 30 Rage
- Mastered-weapon technique: 50 Rage
- Rage resets between encounters.

Equipment-style behavior:

- A one-handed weapon and shield support Shield Counter and reliable defense.
- A two-handed weapon replaces ordinary Defend with Battle Cry and enables Heavy Strike.
- One-handed offense without a shield uses Power Strike.

**Second Wind** is a level-three passive, not healing. Once per encounter, an
otherwise fatal hit leaves the Fighter at 1 health, fills Rage, returns control, and
empowers the next basic attack. It then becomes Spent for that encounter.

Fighter healing should come only from universal consumables, rest, or an explicitly
approved equipment trait such as a future vampiric weapon.

### Mage — Mana and spell choice

Mages trade durability and weak basic melee attacks for direct magical damage.

- Magic Missile: 7 mana, level 1
- Fireball: 12 mana, level 2
- Ice Spike: 15 mana, level 3
- Channel Ward reduces incoming damage and restores a small amount of mana.
- Staff, wand, tome, grimoire, and totem concepts should reinforce spell power,
  mana control, defense, or spell identity rather than duplicate martial weapons.

The three spells are direct combat buttons. The game does not use a system-modal
spell-selection dialog.

### Rogue — Momentum and opportunism

Rogues are fast skirmishers who build Momentum through pressure and avoidance.

- Maximum Momentum: 100
- Basic attack: +18 Momentum
- Entering Stealth: +10 Momentum
- Successful Stealth evasion: +24 Momentum
- Twin Strike: 30 Momentum for Skirmishers
- Mastered-weapon technique: 55 Momentum
- Execute: 70 Momentum
- Momentum resets between encounters.

Assassin techniques are Ambush, Vanish, and Execute. Skirmisher techniques are Twin
Strike, Evasive Strike, and Blade Flurry. Stealth prepares a critical strike and an
evasion opportunity; Execute is strongest against a target at or below 35% health.

### Hunter — Focus, intent, and marked targets

Hunters are ranged planners who build and retain Focus through accurate preparation.

- Maximum Focus: 60 at level 1, 80 at level 2, 100 at level 3, then +10 per level
  to a cap of 120.
- Hunters begin with at least one-third of maximum Focus.
- Take Aim: +25 Focus and prepares an Aimed Shot.
- Basic ranged hit: +15 Focus.
- Aimed Shot: additional +10 Focus.
- Guided hit against a marked target: additional +5 Focus.
- Enemy miss: +15 Focus.
- Taking damage disrupts prepared Aim.
- Earned Focus carries between encounters.

Abilities:

- **Pinning Shot:** 25 Focus. Deals damage and prevents the target's next melee
  action. It does not stop magic, ranged attacks, or dragon breath.
- **Mastered-weapon technique:** 45 Focus unless the technique is Volley.
- **Hunter's Mark:** A Fast setup action with no Focus cost. Only one current target
  may be marked.
- **Volley:** 50 Focus, requires Hunter's Mark, gains a marked-target burst bonus,
  and consumes the mark.

Volley should remain powerful but cannot be repeated without rebuilding Focus and
reapplying Hunter's Mark.

Marksman begins each encounter with one opening-shot bonus, making the path promise
mechanically distinct from Ranger while preserving Hunter's Mark as deliberate setup.

## 7. Weapons, equipment, and traits

Heroes have Weapon, Armour, and Offhand slots.

- Two-handed weapons clear the offhand slot.
- Fighters use martial weapons, heavy armour, and shields.
- Mages use magical weapons, cloth/leather options, and magical offhands.
- Rogues use daggers or short swords, light armour, and dual-wield offhands.
- Hunters use bows or crossbows, light/medium armour, and quivers.

Current weapon-trait language:

| Family tendency | Trait identity |
|---|---|
| Hammers and maces | Concussive stun |
| Axes and greatswords | Sundering armor break |
| Daggers | Bleeding Edge |
| Bows | Precise weak-point attacks; Volley at mastery |
| Crossbows | Armor Piercing; Piercing Bolt at mastery |
| Swords | Balanced Guard; Guarded Riposte at mastery |
| Magical focus weapons | Arcane Focus spell amplification |

Traits must be visible in equipment comparison, combat status, and the combat log.

## 8. Loot, relics, vendors, and economy

### Item quality

Common, Uncommon, Rare, and Relic quality are communicated through text and color.
Inventory sorting favors stronger and higher-quality items. Equipment comparison
shows improvements and regressions relative to the currently equipped item.

### Field loot

Standard enemies may drop class-compatible Common or Uncommon weapons, armour, or
offhands. Duplicate avoidance favors a different useful item before repeating one
already owned.

### Vendors

- Blacksmith: martial weapons, shields, and heavier armour
- General Merchant: lighter equipment, ranged/magical stock, supplies, and maps
- Alchemist: magical guidance, identification tradition, and recovery services
- Innkeeper/Tavern: rest, rumors, and identification tradition

Vendor tabs and sorting are functional. Common starter, equipped, and relic-bound
items are protected from accidental sale. Specialist resale bonuses may apply.

### Elite relic loop

Normal elite enemies carry unidentified relics. The discovery uses a prominent
custom popup, chime, inventory entry, and specialist hint. Each relic must be taken
to its named vendor for free identification. Identified relic equipment prepares
the hero while all three identified relics break the dragon seal. Relics no longer
grant large flat boss damage or guard stacks.

Shops should provide minor upgrades sufficient for standard and elite content, but
should not replace relic preparation for bosses.

## 9. World and exploration

The current world is a finite 13×13 generated grid.

- Terrain: fields, lakes, and crypts
- Safe/special locations: shops, tavern, alchemist encampment, Ashweb Nest
- Threat structure: five standard enemies, three elites, and one dragon boss
- Enemies roam only through compatible terrain and cannot enter safe landmarks,
  sources, or occupied cells.
- Terrain and landmarks vary by seed; the compact quest remains finite.

### Fog of war and map knowledge

Tiles can be Unknown, Scouted, or Visited. Travel reveals nearby terrain; Hunters
scout farther. Rumors and specialist advice can reveal uncertain threats or relic
search regions. A purchased regional map scouts terrain and landmarks but not the
identity of roaming threats.

Map presentation:

- 5×5 close view
- 7×7 default detail view
- 9×9 route-planning overview
- Full fog-aware world overlay on `M`
- Camera panning, recentering, edge hints, and density-aware hero/enemy minis
- Ordinary terrain uses artwork rather than repeated text labels

### Ashweb Nest

The nest is an optional, permanently clearable source with three finite charges.

1. Giant Forest Spider — reduced reward
2. Giant Forest Spider — reduced reward
3. Ashweb Matriarch — elite presentation, level-two difficulty, reduced source reward

The two lesser broods grant 3 XP and 3 gold each. The Matriarch grants 6 XP and
6 gold. Clearing the source grants one additional 18-gold salvage reward. Total
source rewards are capped at 12 XP and 30 gold. Source enemies cannot drop equipment
or relics.

Future sources such as camps, dens, crypts, or shrines should follow the same
principles: finite budget, authored escalation, clear completion state, bounded
rewards, and no farming exploit.

## 10. Enemies and difficulty

Encounter tiers communicate expected danger:

- Standard: teaches core combat and provides modest progression
- Elite: punishes repetition, provides unidentified relic progression
- Boss: expects level-three tactics, appropriate equipment, consumables, and relics

Enemy attacks have named intent and tempo. Attack types include melee, ranged,
magic, and breath when rules need to distinguish them. Speed may grant extra actions,
but each action must be individually perceptible.

The world does not automatically scale every creature to the hero. Standard regions,
midgame sources, elites, and dragon encounters occupy authored difficulty bands so
character growth remains meaningful.

## 11. Interface and controls

### Core layout

- Top navigation: game identity, sound, settings, and major views
- Left hero rail: portrait, health, class resource, experience, equipment, core stats
- Main stage: location, shop, inventory, dialogue, encounter, or map content
- Right map rail: map viewport, legend, movement controls, clues/objectives

Class-resource presentation:

- Mage: blue Mana
- Fighter: red-orange Rage
- Rogue: violet Momentum
- Hunter: green Focus

### Shared hero visual identity

The selected race and class resolve one modular `HeroVisualTheme` used throughout
the game. Class provides the stronger enamel/resource accent; race provides a much
quieter heraldic motif and material treatment.

Hero identity may appear on the hero rail, inventory profile, player combat
resources and actions, dialogue responses, progression/reward moments, and outcome
summaries. Global navigation, maps, shops, NPC panels, enemy presentation, settings,
and system surfaces remain neutral or context-themed. Quality, danger, health,
validation, disabled, and accessibility colors always take priority.

This is a shared component system—not sixteen versions of each screen. Race/class
selection must never change layout geometry, control placement, keyboard order, or
content density.

The same system is a branding rule. When tester, community, release, portfolio, or
marketing material features a hero, its class wash and subtle race heraldry must
match the selected build. General product material uses the neutral painterly Art
Deco shell or a balanced four-class ensemble. Brand templates remain layered so
hero art, theme inputs, environment, version, copy, and crop can change independently.

Unavailable abilities remain understandable through tooltip, keyboard focus,
accessible description, resource requirement, and prerequisite status. Second Wind
appears as Armed or Spent status rather than a button.

### Controls

- WASD: movement
- Arrow keys: accessibility movement alternative
- M: full world map
- Number keys 1–7: combat actions
- Escape: close overlays where appropriate

Movement accepts one tile per deliberate physical keypress; operating-system key
repeat must not become map sprinting.

### Overlays

Settings, dialogue, relic discovery, level progression, spell information, victory,
and defeat use custom in-game presentation rather than native system dialogs.

## 12. Audio, visual effects, and accessibility

- Music changes between title, exploration, safe locations, combat, and bosses.
- Ambience supports terrain and location identity.
- Important rewards, attacks, selection confirmations, and errors receive distinct
  cues.
- Current prototype combat VFX preserve a replaceable rendering contract while
  custom painterly assets are developed.
- Credits and license information remain available in-game and in release packages.
- Reduced motion shortens effects without skipping combat information.
- Separate controls exist for master, music, ambience, and effects volume.
- Fonts and core icons are bundled for consistent offline rendering.

## 13. Balance principles

- Standard encounters should be reliably survivable and teach the build.
- Elites should require class mechanics rather than reward basic-attack repetition.
- A prepared level-three build with appropriate relic support should have a credible
  boss path.
- A quest-rush boss attempt should remain highly dangerous.
- No active ability should be infinitely repeatable without a meaningful resource,
  state, or timing requirement.
- Strong burst actions may remain dramatic when their setup is visible and costly.
- Race/class combinations may differ in difficulty, but no intended build should be
  insurmountable solely because of its identity.
- Deterministic simulations identify outliers; human playtests decide clarity, fun,
  perceived fairness, and whether a strategy is discoverable.

Current human-validation focus is `BAL-003`: Rage, Momentum, Focus, Second Wind,
Hunter's Mark/Volley, Pinning Shot, and the Ashweb Matriarch.

## 14. Approved future directions

These are compatible directions, not current implementation promises:

- Additional finite sources: camps, dens, crypts, and shrines
- Larger scrolling regions and authored cave/dungeon/lair layers
- More distinctive ability VFX and cohesive old-school fantasy audio
- Uniformly cropped equipment, armour, offhand, and relic art
- Cosmetic character-presentation choices with counterpart art
- Standalone Classic preservation package
- Chromebook distribution validation
- Eventual iOS architecture exploration after desktop systems stabilize

Every future proposal should state its player value, affected systems, balance risk,
UI requirements, asset needs, testing plan, and whether it replaces or extends an
existing rule.
