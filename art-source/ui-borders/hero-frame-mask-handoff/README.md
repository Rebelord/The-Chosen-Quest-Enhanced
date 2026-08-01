# Hero-frame mask handoff

All editable files use the frame's native `932 × 1240` canvas. Preserve that canvas,
origin, and transparent background when saving.

## Files

- `hero-frame-reference.png` — approved transparent frame artwork.
- `EDIT-hero-frame-enamel-visible-mask.png` — starter selection for every visible
  enamel region. This automatic selection is intentionally provided as a starting
  point and currently contains holes in painted highlights, grime, reflections, and
  very dark green texture.
- `EDIT-hero-frame-gem-visible-mask.png` — starter selection for the five visible
  gemstone interiors.
- `four-class-preview.png` — current in-game comparison for reference.

## Photoshop editing contract

1. Keep the document exactly `932 × 1240`; do not crop, resize, or reposition it.
2. Use solid opaque white for the complete visible enamel or gemstone interior.
3. Keep everything outside that material region transparent.
4. Follow the inside edge of the original brass boundary.
5. Fill interior texture holes. The mask describes the complete material surface,
   not only pixels that happen to be strongly green or dark.
6. Do not manually add the hidden bleed. The build step will dilate the corrected
   visible masks four pixels beneath the brass.
7. Export as RGBA PNG with the filenames unchanged.

Once the two corrected masks are returned, the build step should generate:

- separate four-pixel-overscanned color underlays;
- a matching partially transparent metal/textural overlay;
- the four-class verification preview.

This keeps the human-authored selection as the source of truth while deriving all
dependent runtime assets consistently.
