# Painterly UI Material Asset Pipeline

The approved character-creation mockup is the visual target, not a background image
to place behind Swing controls. Production uses a hybrid skin: authored raster
materials provide painterly depth while code retains responsive geometry, masks,
theme tinting, accessibility, interaction, and layout.

## Honest current state

- Layout, clipping, portrait masks, spacing, theme colors, meters, focus, button
  states, and hit targets are genuinely modular.
- Race crests and the small navigation mark are independent transparent assets.
- Neutral lacquer and warm parchment are now independent material tiles loaded once
  by `UiMaterials` and clipped inside responsive panels and buttons.
- The approved mockup remains a flattened visual reference and is never shipped as
  the screen background.
- Compact buttons now use an authored transparent brass frame rendered as a
  horizontal three-slice: corner hardware keeps its shape while only the quiet
  middle rails stretch. Source and runtime files remain separate under
  `art-source/ui-borders/` and `assets/ui/borders/`.
- Dossiers, cards, equipment bands, profile panels, and the shell use a quieter
  authored brass frame rendered as a true nine-slice.
- The selected 3:4 hero uses a dedicated ornate overlay with fixed crown, side
  tiers and lower mount. Portrait art is rendered into the measured opening; class
  color sits in separate native-size enamel and gemstone underlays behind the metal.
  Their regions come from the authored material boundaries, not coordinates or
  runtime shapes. No colored geometry is drawn over the frame.
- Race motif overlays still rely primarily on code. Fidelity work is not complete
  until those transparent identity layers exist and survive the target-size review.

## Required production kit

| Layer | Asset strategy | Runtime responsibility |
|---|---|---|
| Charcoal lacquer | Seamless neutral tile | Clip, scale, class wash, race tint |
| Parchment/vellum | Seamless warm tile | Clip inside primary faces; state tint |
| Brass rail and wear | **Started:** transparent three-slice button frame; panel nine-slices remain | Responsive geometry, theme tint, and focus state |
| Panel inset/shadow | **Implemented:** transparent brass nine-slice | Responsive panel bounds and opacity hierarchy |
| Selected-card hardware | Transparent corner fittings | Class enamel, hover, press, focus |
| Hero-frame ornament | **Implemented:** fixed-ratio metal overlay plus independent enamel/gem underlays | 3:4 portrait opening and class-color composition |
| Race heraldry | Four transparent motif overlays | Low-opacity placement only |
| Class enamel | Prefer one neutral mask | Runtime Fighter/Mage/Rogue/Hunter tint |

Do not generate sixteen complete screens or sixteen complete frames. New races and
classes add only their authored identity input.

## Techniques

1. **Seamless texture painting:** repeatable lacquer and parchment tiles use
   `TexturePaint`, avoiding stretched raster surfaces.
2. **Nine-slice scaling:** fixed corners keep their authored shape; repeatable edges
   and centers expand independently.
3. **Alpha-mask tinting:** neutral enamel, gem, and metal masks accept runtime theme
   colors without recoloring neighboring content.
4. **Layered compositing:** base material, class wash, race motif, frame, and content
   remain separate in both Java and Figma.
5. **Code masks and hit targets:** clipping, focus, pressed offsets, keyboard flow,
   and accessibility never depend on decorative pixels.
6. **Multi-resolution exports:** retain large editable sources; ship optimized
   runtime assets and validate at 1280×720, 1440×900, and 1920×1080.
7. **Visual regression captures:** compare the same representative build and maximum
   player name after every material or frame change.

## Frame hierarchy

- Buttons use the brightest, most compact hardware because they must communicate
  interaction.
- Cards and panels use quieter rails and smaller corner weight so decoration never
  competes with copy.
- The selected hero frame is intentionally the only highly ornate frame on the
  screen. Its crown and lower mount establish the focal point.
- Gem sockets and enamel channels are derived from their painted material boundaries
  rather than recreated as polygons or located with stored coordinates. Each mask
  intentionally extends four source pixels beneath the surrounding opaque brass.
  This hidden underlap prevents scaling gaps while the metal overlay remains the
  authoritative visible edge. Partial original darkness preserves inlay depth.
- Portrait opening, mask, frame overlay, and character artwork remain separate.

## Fidelity threshold

The skin is not complete merely because it uses gold lines and dark green panels.
At target size it must preserve:

- visible painterly depth without compromising text contrast;
- crisp, authored corners rather than procedural-looking decoration;
- consistent materials across buttons, cards, dossier, profile, and hero frame;
- primary, secondary, selected, focused, pressed, hover, and disabled states;
- no seams, stretched corners, detached ornaments, clipped copy, or image bleed;
- no layout shift when race, class, loadout, name length, or window size changes.

If a capture still reads as flat Swing controls decorated with fantasy lines, the
material pass is incomplete.
