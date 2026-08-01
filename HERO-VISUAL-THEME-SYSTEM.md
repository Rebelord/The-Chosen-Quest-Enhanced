# Shared Hero Visual Theme System

The game uses one shared painterly fantasy Art Deco visual language. Race and class
customize hero-owned surfaces through modular inputs rather than separate screen
designs. Character creation is the first implementation and establishes the contract
consumed throughout the playable game.

## Layer contract

From back to front:

1. **Fixed shell:** charcoal lacquer, warm parchment, stepped gold framing, layout,
   typography, component dimensions, focus treatment, and interaction states.
2. **Class overlay:** a translucent enamel wash and stronger selection accent.
3. **Race overlay:** low-opacity heraldic pattern used only on generous background
   areas, the dossier, and the hero profile.
4. **Frame ornament:** independent class-color enamel and gemstone underlays beneath
   an authored brass overlay. Their native-size masks follow the painted material
   boundaries and safely underlap the metal; race identity remains quieter.
5. **Content:** text, controls, meters, item art, and the live hero image.

Only layers 2–4 vary. Content geometry must not move when the build changes.

## Modular inputs

`HeroVisualTheme.forBuild(race, heroClass)` supplies:

- `classAccent`: selection outlines, active class labels, and restrained light.
- `classDeep`: optional enamel-shadow tone.
- `resourceColor`: Rage, Mana, Momentum, or Focus meter color.
- `raceAccent`: quiet heraldic linework and race-associated material.
- `metalAccent`: Art Deco corner and frame highlight.
- `raceMotif`: Crown/Sunburst, Mountain/Runes, Leaf/Crescent, or Wheat/Arch.

The initial class palette is crimson Fighter, sapphire Mage, amethyst Rogue, and
emerald Hunter. Race treatments remain intentionally quieter than class treatments.

## Asset transparency rules

- Race motifs, corner ornaments, glows, particles, and frame overlays should be
  transparent PNGs when raster art is required.
- Do not include panels, text, portrait art, shadows for adjacent components, or
  fixed background colors inside overlay assets.
- Author raster overlays at the largest intended display size with at least 24 px
  clear padding on every edge.
- Prefer code/vector drawing for masks, hit targets, layout, and simple heraldry.
  Use transparent three-slice/nine-slice raster assets for painterly metalwork that
  code cannot reproduce convincingly.
- Portraits remain independent 3:4 artwork rendered into a programmatic opening
  beneath the authored frame overlay.
- Selection backgrounds are composited at runtime; do not export sixteen flattened
  race/class screen backgrounds.

## Intensity hierarchy

- Class accent: approximately 12–22% overlay opacity on surfaces.
- Race motif: approximately 7–10% perceived intensity.
- Selected border and resource meter: full accessible accent color.
- Persistent game shell remains neutral; do not recolor global navigation for every
  build. Theme only hero-owned or hero-reactive surfaces.

## Gameplay surface mapping

- **Hero rail:** portrait frame material, class resource, selected equipment accent,
  and a very quiet race motif behind the personal profile.
- **Inventory:** class accent on equipped/compatible actions; race motif may appear
  only in the profile header, never beneath dense item rows.
- **Combat:** resource meter, hero-status chips, selected quick action, and player
  action VFX use class identity. Enemy health, tier, and status retain enemy/tier
  colors so threat communication is never confused with player identity.
- **Dialogue:** player response/confirmation accent may use class color. NPC panels
  retain their location or profession theme.
- **Progression and rewards:** level-up and hero-equipment confirmations may combine
  the class glow with restrained race heraldry.
- **Victory/defeat:** portrait frame and personal summary inherit the hero theme;
  outcome color and readability still take priority.
- **Maps, shops, NPC cards, global navigation, settings, credits, and system
  overlays:** remain neutral or context-themed.

## Figma component mapping

Create the following component properties rather than sixteen detached screens:

- `Creation/Shell` — fixed
- `Creation/Class Wash` — Fighter, Mage, Rogue, Hunter variants
- `Creation/Race Motif` — Human, Dwarf, Elf, Halfling variants
- `Creation/Hero Frame` — race material × class gem properties
- `Creation/Choice Card` — default, hover, pressed, selected, focused, disabled
- `Creation/Resource Meter` — Rage, Mana, Momentum, Focus variants

Overlay components must use transparent fills outside their artwork. A composed
example for each of the sixteen builds can be placed on an exploration page, while
the primary presentation page should show only representative builds.

## Branding and marketing carryover

The same identity inputs apply whenever a playable hero is featured in tester,
community, release, portfolio, or marketing artwork. Marketing uses a reusable
neutral Art Deco master template, then swaps hero art, class wash, race motif,
environment, copy, and placement crop as modular inputs.

Hero theming must not alter the product logo, essential copy contrast, release
version, legal/attribution information, or calls to action. Ensemble marketing may
use the neutral shell with balanced four-class accents instead of selecting one
featured build. See `BRAND-VISUAL-SYSTEM.md` for the complete rule.

## Implementation guardrails

- Race/class changes may repaint theme layers but must not reconstruct the complete
  screen or re-decode unchanged artwork.
- Reduced-motion mode changes transitions, not the selected visual identity.
- Contrast and focus indicators are evaluated against the fixed shell, not against
  the darkest or lightest possible portrait.
- New races and classes add theme inputs and authored art; they do not fork the
  character-creation layout.
- The selected race and class are already persistent game state; surfaces resolve
  the theme from that state rather than storing duplicate theme preferences.
- Class/race presentation never overrides quality-tier, danger-tier, health,
  disabled-state, validation, or accessibility colors.
