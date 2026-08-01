# The Chosen Quest Enhanced Brand Visual System

The product interface, tester materials, community spaces, release graphics, and
marketing use one cohesive painterly fantasy Art Deco language.

The brand is not a single flattened background. It is a reusable composition:

1. Neutral charcoal, parchment, and antique-gold Art Deco foundation
2. Painterly fantasy environment or character artwork
3. Optional featured-hero class wash
4. Optional featured-hero race heraldry
5. Shared title treatment, stepped geometry, typography, and restrained lighting
6. Placement-specific copy, calls to action, and platform-safe margins

## Core visual signature

- Painterly high-fantasy illustration with tactile materials
- Charcoal lacquer and dark umber foundations
- Warm parchment content surfaces
- Antique gold and aged bronze framing
- Stepped Art Deco geometry, fans, rays, chevrons, and symmetrical accents
- Cinzel for compact labels and title-adjacent structure
- Cormorant Garamond for expressive display copy
- Strong cinematic contrast without crushing readable shadow detail
- Dimensional, authored buttons instead of modern flat application controls

This signature remains recognizable even when no particular hero is featured.

## Featured-hero rule

When a marketing asset features a playable hero, it uses the same modular identity
as the game:

- Fighter: crimson enamel and iron
- Mage: sapphire and arcane light
- Rogue: amethyst and smoked metal
- Hunter: emerald and aged bronze
- Human: crown and sunburst heraldry
- Dwarf: mountain angles, runes, and hammered metal
- Elf: leaves, crescents, and silver curves
- Halfling: wheat, round arches, and warm copper

Class color is the primary variable. Race treatment stays quieter. The hero art,
frame, accents, and background lighting may inherit those inputs, but the logo,
body copy, legal copy, and essential calls to action must retain stable contrast.

Do not make sixteen independent marketing systems. Use one template with race,
class, hero artwork, environment, copy, and aspect ratio as modular inputs.

## Marketing template anatomy

Every reusable template should expose these replaceable slots:

- `Format`: Discord banner, tester invitation, release card, social post, store art,
  form header, documentation cover, or screenshot frame
- `Hero`: none, one featured build, or balanced four-class ensemble
- `Class Wash`: neutral, Fighter, Mage, Rogue, or Hunter
- `Race Motif`: none, Human, Dwarf, Elf, or Halfling
- `Environment`: world vista, shop, tavern, map, combat, or neutral texture
- `Headline`
- `Supporting Copy`
- `Call to Action`
- `Version / Beta Label`
- `Required Links / Attribution`

Logo, content-safe region, margins, and typography rules remain locked.

## Tester and community materials

Private tester invitations, tester guides, quick-reference cards, feedback-form
headers, bug-report headers, Discord announcements, and release posts should feel
like extensions of the game interface.

- Default tester materials use the neutral four-class ensemble treatment.
- A class-focused test wave may feature that class color and representative hero.
- A race-focused test wave may add the race motif, but should not discard the class
  or product hierarchy.
- Always show the build version and whether it is private beta, public beta, or
  stable.
- Feedback and bug-report actions remain visually distinct and use their exact
  published destinations.
- Tester materials should never imply that a featured build is the only playable
  character.

## Transparency and source-file rules

- Keep logo, title treatment, hero, frame ornament, race motif, class lighting,
  environment, copy, and platform crop guides on separate layers.
- Export transparent PNG overlays for painterly glows, heraldry, ornament, and
  character cutouts.
- Keep editable source compositions for every master template.
- Do not bake version numbers, URLs, or release-channel labels into background art.
- Preserve a clean master without a logo or text for future crops and localization.
- Generate at the largest required size, then crop per placement from the master.
- Never stretch a completed image to a new aspect ratio.

## Placement defaults

- **Application/Discord icon:** retain the detailed square master for operating
  systems, releases, community identity, and large-format marketing.
- **In-game UI mark:** use the transparent simplified crest with brighter gold,
  broader dragon geometry, and a larger purple gem; validate at 24–32 px and never
  place the dark square master directly in a small navigation slot.
- **Discord banner:** centered logo/figures with generous lateral crop safety.
- **Tester invitation:** strong title, beta label, one short purpose statement,
  version, and one clear next action.
- **Tester guide/PDF:** branded cover and section dividers; neutral reading pages.
- **Google Form header:** shallow crop, readable at small size, no essential copy
  embedded in the artwork.
- **GitHub release:** wide hero image plus concise version card; release notes remain
  live text.
- **Portfolio/store art:** strongest representative hero or four-class ensemble,
  readable product title, and sufficient room for platform overlays.

## Review checklist

- Does this immediately look like The Chosen Quest Enhanced?
- Does it use the shared Art Deco shell rather than unrelated fantasy ornament?
- If a hero is featured, do class and race treatments match that build?
- Is class identity stronger than race decoration?
- Can every changeable input be replaced without repainting the entire composition?
- Are logo, version, copy, links, and attribution still editable?
- Does the asset survive its intended crop and small-size preview?
- Are readability and action hierarchy stronger than decoration?
- Does it avoid conflicting with health, danger, rarity, error, or disabled colors?

The implementation details and exact race/class palette live in
`HERO-VISUAL-THEME-SYSTEM.md`.
