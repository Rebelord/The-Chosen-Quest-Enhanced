# Character Creation Fidelity Audit

Reference:

- Approved mockup: `art-source/mockups/character-creation-fantasy-art-deco.png`
- Standard implementation: `build/character-creation-preview.png`
- Large implementation: `build/character-creation-large-preview.png`

The mockup is 1586×992 and the standard preview is 1440×900. Both are effectively
the same 16:10 composition, so most differences are genuine visual or layout gaps
rather than consequences of comparing different aspect ratios.

## Summary

The implementation has the correct information architecture and now uses the right
Art Deco structural vocabulary. The remaining fidelity gap comes from three areas:

1. The first authored lacquer and parchment tiles now replace key code-only surface
   approximations; brass wear, complex frame ornament, and transparent identity
   overlays still need authored modular assets.
2. The complete left-side dossier now fits the standard preview without a visible
   scrollbar, and bounded responsive spacing uses the additional 1080p height.
3. Item presentation needs another refinement pass to match the strength of the new
   frame, typography, button treatments, and responsive composition.

## Comparison matrix

| Area | Approved mockup | Current implementation | Recommended improvement | Priority |
|---|---|---|---|---|
| Background material | Painterly charcoal/green lacquer with mottled depth | **Improved:** an authored neutral lacquer tile is cached, repeated without stretch, and composited beneath class/race layers across both columns | Tune opacity after the brass/frame kit and validate tile seams | P2 |
| Outer shell | Multiple gold rails, corner fans, inner shadows, integrated geometry | **Improved:** crop-safe seven-pixel shell with connected inset shadow, outer metal rail, secondary highlight rail, and stepped corners | Add authored metal wear during the material pass without introducing detached rays | P2 |
| Header | Crest, balanced wordmark, long rail, central hanging diamond, composed corners | **Improved:** uses a dedicated transparent 30 px crest with brighter gold and a larger purple focal gem; redundant right-side copy and detached corner rays are removed | Refine the title lockup and rail transition without reintroducing decorative clutter | P2 |
| Main title | Serif display title with symmetric line/diamond ornaments | **Improved:** right diamond follows measured copy and its rail is capped responsively; compact left triple-rail remains aligned | Consider modest left-ornament scale refinement after material assets exist | P2 |
| Hero frame | Large multi-tier stepped frame with crown fan, deep shadow, and strong silhouette | **Improved:** a dedicated 3:4 painterly metal layer supplies the crown, rails, side tiers, sockets, and lower mount; independent native-size enamel and gemstone underlays follow the source art, underlap the brass to prevent gaps, and eliminate floating or surface-painted shapes | Validate alternate race themes and consider a quieter compact-height crown only if playtesting finds the 720p silhouette too dominant | P1 |
| Hero scale | Hero is the unmistakable focal point and nearly fills the authored frame | **Improved:** responsive 3:4 holder expands to 420×560 at 1080p and compresses safely at standard/compact heights | Refine frame-to-portrait spacing with the authored material pass | P2 |
| Right profile hierarchy | Name has flanking ornament; race/class, meters, stats, gear, and actions form one compact vertical rhythm | **Improved:** adaptive metal rails and class gems frame the player name, retreat for long copy, and share the profile width with meters, gear, and actions | Refine section-to-section material continuity during the texture pass | P2 |
| Typography | Display serif, serif body, and small engraved labels feel like one family | Display text is close; many labels/descriptions still use generic sans-serif | Use Cinzel for engraved labels and Cormorant Garamond for readable supporting copy; reserve sans-serif for dense utility text only | P0 |
| Buttons | Layered bevels, clipped corners, brass brackets, parchment primary face | **Improved:** authored parchment/lacquer faces now sit beneath a transparent three-slice brass frame; corner caps remain undistorted, secondary hardware is quieter, and every interaction state remains covered by the regression matrix | Tune source grading after playtest, then derive compatible compact-card and panel borders from the same hardware language | P1 |
| Selection states | Emerald enamel fill plus restrained gold corner brackets | **Improved:** active controls use class enamel and one accent rim; a second inner outline appears only for keyboard focus | Validate contrast for every class accent and disabled state | P2 |
| Race cards | Larger crests, more deliberate padding, quieter inactive frames | **Improved:** full horizontal presentation at standard widths; compact cards stack a 44 px crest over the complete centered label | Add subtle authored surface depth without increasing card height | P2 |
| Class cards | Icons, names, and descriptors are comfortably legible | **Improved:** horizontal weapon-and-copy layout remains intact at 1280×720 and scales as one unit below its narrow-width breakpoint | Increase standard-size icon presence slightly during the item-art pass | P2 |
| Starting loadout | Text has clear hierarchy and selected card feels dimensional | **Improved:** dedicated enamel face, single accent rim, raised shadow, and focus-only inner outline soften selected contrast | Validate the two longest loadout descriptions at 1280×720 | P2 |
| Build dossier | Rich emerald-black material, gold corner brackets, complete content at target size | **Improved:** the complete dossier fits at 1440×900 and now uses a supporting nine-slice brass frame with safe corner padding over the class-tinted lacquer | Validate unusually long localized copy and reduce hardware opacity only if dense translations compete with it | P2 |
| Center divider | Integrated gold architectural divider with top/bottom connections and multiple gems | **Improved:** divider and scrollbar are now one fixed-width component; its class-colored, race-metal gem moves only when scrolling is required and rests at center otherwise | Continue refining its top/bottom connection to the outer shell rails | P2 |
| Resource meters | Thin luminous fills with elegant engraved labels | **Improved:** inset tracks, class/health glow, highlight, gold baseline, and end caps preserve exact labels and values | Validate the shared meter treatment in combat, inventory, and compact layouts | P2 |
| Equipment strip | Larger item art with serif labels and comfortable vertical room | **Improved:** 40 px art, three centered cells, quality-colored item names/tooltips, and collision-free compact actions | Validate unusually long localized item names | P2 |
| Standard-height behavior | Entire composition reads without visual clipping | **Improved:** compact spacing and reclaimed outer padding fit the full decision flow and taller equipment strip at 1440×900 | Validate representative long names and every loadout description | P1 |
| Large-height behavior | Reference remains vertically composed with little dead space | **Improved:** bounded section gaps, a taller hero, and an expandable dossier consume useful 1080p height without globally stretching controls | Validate ultrawide and taller-than-1080p behavior; center the designed canvas once limits are reached | P2 |
| Texture modularity | Looks painterly but must remain adaptable | **In progress:** neutral lacquer/parchment tiles, button three-slice, panel nine-slice, and fixed-ratio hero overlay are independent assets; layout, masks, interaction, class gems, and content remain code-driven | Add transparent race/enamel identity masks without creating sixteen backgrounds | P1 |

## Recommended implementation order

### Pass 1 — Composition and component states

1. Add responsive compact/expanded vertical layout tokens.
2. Prevent the standard preview from clipping the dossier.
3. Refine the completed unified scrollbar/divider and connect it to the shell rails.
4. Implement dedicated Art Deco primary, secondary, and selected-card states.
5. Establish the final typography hierarchy.

This pass produces the largest improvement without new painterly assets.

### Pass 2 — Hero and shell authorship

1. Build the dedicated scalable hero frame.
2. Upgrade the outer shell and header lockup.
3. Add player-name ornament.
4. Increase class-card, equipment-art, and descriptor legibility.

### Pass 3 — Painterly material layer

1. Create neutral lacquer, parchment, metallic wear, and inset-shadow textures.
2. Add optional transparent class enamel and race-heraldry overlays.
3. Validate every texture at standard and large sizes.
4. Use the same material tokens in the Figma library and brand templates.

## Guardrails

- Do not solve fidelity by using the mockup as one background image.
- Do not reduce essential text below the current readable size to make content fit.
- Do not let hero theme colors replace health, rarity, danger, disabled, error, or
  focus-state meanings.
- Do not make the large layout simply stretch every component.
- Keep all states keyboard accessible and visibly focused.
- Keep portrait, frame, texture, race motif, class wash, and content as independent
  layers.
