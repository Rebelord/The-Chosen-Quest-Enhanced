# Build Map Markers

Transparent player miniatures used by `GameMapPanel`. Each race/class build has
one 256 x 256 PNG named `{race}-{class}.png`.

The source sheet is ordered as follows:

- Rows: Human, Dwarf, Elf, Halfling
- Columns: Fighter, Mage, Rogue, Hunter

`build-markers-source.png` preserves the generated chroma-key source.
`build-markers.png` is the transparent master sheet used to export the sixteen
individual files.

The map renderer prefers these miniatures and falls back to the corresponding
portrait under `assets/avatars/` if a future build does not yet have a marker.

## Generation notes

The miniatures were generated as a strict 4 x 4 sheet using the existing
full-body character art as a style reference. The prompt required compact,
three-quarter top-down figures, oversized readable heads, contained equipment,
class-color accents, and a flat magenta background. The background was removed
locally with a soft matte and despill before the sheet was cropped.
