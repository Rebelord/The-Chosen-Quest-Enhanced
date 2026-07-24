# Authored RPG sound effects

- Pack: **RPG Sound Pack**
- Artist: **artisticdude**
- Source: https://opengameart.org/content/rpg-sound-pack
- License: **CC0 1.0 Universal / Public Domain Dedication**
- License URL: https://creativecommons.org/publicdomain/zero/1.0/
- Downloaded: 2026-07-22

Attribution is not required under CC0. The Chosen Quest credits artisticdude as a
project policy and in thanks for making the work available.

## Imported mapping

| Game file | Original pack file | Use |
|---|---|---|
| `ui-confirm.wav` | `interface/interface1.wav` | Confirm and primary UI actions |
| `ui-cancel.wav` | `interface/interface2.wav` | Cancel and back actions |
| `error.wav` | `interface/interface6.wav` | Invalid or unavailable action |
| `equip.wav` | `inventory/armor-light.wav` | Equipment changes |
| `purchase.wav` | `inventory/coin3.wav` | Buying, selling, and services |
| `attack.wav` | `battle/swing.wav` | Player weapon swing |
| `enemy-hit.wav` | `battle/sword-unsheathe2.wav` | Enemy impact feedback |
| `player-hit.wav` | `battle/sword-unsheathe4.wav` | Incoming physical impact |
| `defend.wav` | `inventory/metal-ringing.wav` | Guard and defensive actions |
| `spell.wav` | `battle/magic1.wav` | Spellcasting |
| `heal.wav` | `inventory/bubble2.wav` | Potion and healing feedback |
| `rest.wav` | `inventory/cloth-heavy.wav` | Resting and recovery |
| `dragon-roar.wav` | `NPC/giant/giant2.wav` | Temporary dragon vocal cue |

All files were downmixed to mono PCM16 at 22,050 Hz and peak-normalized using
`tools/prepare_audio.py`. The dragon vocal remains an interim cue until a dedicated
dragon recording is selected or created.
