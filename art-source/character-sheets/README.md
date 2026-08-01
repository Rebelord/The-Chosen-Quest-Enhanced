# Counterpart character sheets

The two 4×4 source sheets in this folder were generated for the cosmetic gender
choice. Their fixed order matches the original character sheets:

- columns: Fighter, Mage, Rogue, Hunter
- rows: Human, Dwarf, Elf, Halfling

`tools/CropCharacterSheets.java` creates deterministic production crops under
`assets/avatars/counterpart/`. Existing character art remains untouched.

The counterpart assignment is:

- Human: male Fighter, female Mage, male Rogue, female Hunter
- Dwarf: female Fighter, female Mage, male Rogue, female Hunter
- Elf: male Fighter, male Mage, female Rogue, male Hunter
- Halfling: female Fighter, male Mage, female Rogue, male Hunter
