## 5. Data dictionary

### 5.1 Frame flags in the binary (informational)
The app stores which fields a frame has in a flag byte; in text this is implicit by which `$KEYS` you write: `0x01 $MID`, `0x02 $WACTFLAG`, `0x04 $MOVE`, `0x08 $SE`, `0x10 $HITS`, `0x20 $TIME`, `0x40 $OPT`, `0x80 $RAZ`.

### 5.2 `$HITS` flags (`$FLAG`)

| Flag | Meaning |
|---|---|
| 0 | **Projectile spawn point** at (x, y) — creates the move's projectile ANO (HDAT 4 `PROJETIL_ANIM_*`). Inside a projectile ANO it spawns a **sub-projectile**. Max 8 projectiles alive in the game. |
| 1 | **Hurtbox** (body, where the character takes damage), `[x,y,w,h]`. |
| 2 | **Hitbox** (attack area), `[x,y,w,h]`. Uses the frame's `$OPT` (damage, reaction). |
| 3 / 4 / 5 | Screen shake light / medium / strong. |
| 10–17 | Dust/smoke effect from the shared `effect` pack (effect ANO = 50 + flag − 10) at offset (x, y). |
| 20 | **Cutscene** trigger (x, y); the event is selected by data in the character's HDAT 13. Complex — copy from an existing character; the app shows a panel explaining the event. |
| 26 | Opponent cannot defend. |
| 27 | Prevent cancelling. |
| 28 | Damage ignores attack/defense stats. |
| 30 / 31 | Camera zoom. |
| 32 | Damage to self (amount = `$OPT` d0). |
| 34 | Heal. |
| 37 | Hit does not count for combo. |
| 38 | **Block the sequence** (the move's SEQ does not trigger when this hit connects). |
| 39 | Teleport to the opponent. |
| 40 | Pull the opponent. |
| 41–70 | Aura/bubble from the `charf` pack (ANO = flag − 41 + 3). |
| 71–125 | **Character's own effect/aura ANOs**: flag f spawns ANO `201 + (f − 71)` of the character's own DAC (so ANOs 201…255 are effect/aura ANOs). They follow the character while the flag is present. |

Other numbers *(unknown / unused)*. Boxes with the same flag in consecutive frames are shown as one object by the timeline.

### 5.3 `$OPT` (extra frame data)
`$OPT: { $FLAG: F, $DATA: B[d0, d1, d2, d3] }`
- `d0` = **damage** of the hitbox in this frame (also the amount for flag 32).
- `d1` = **reaction code** `OPT1 = group*50 + index` (see 5.4). Maximum 249. **Only meaningful on frames with a hitbox (flag 2).**
- `d2` ≠ 0 = **interpolate** rotation/alpha/zoom toward the next frame (needs `$RAZ` in the next frame; mismatching box counts between the frames gives wrong values).
- `d3` *(unknown)* — keep the value of an existing similar frame.
- `F` (16-bit) = "window" bits of the frame: 512 = **window for "try grab" / repeat projectile** (same bit, so a move cannot use both), 1024 = hide the scenery, 2048 = wall impact effect, bits 12–15 (value `4096*t`, t = 1..15) = dust/smoke type. Other bits *(unknown)*: copy from an existing frame.

### 5.4 Hit reactions
`OPT1 = group*50 + index`.

- `index` 0..29 = a **game-wide** reaction (read-only, in the game data). `index` 30..49 = **this character's own** reaction slot `index − 30` (0..19), stored in HDAT 14..33 of `000.bin.txt` (editable; may be empty).
- `group` 0..4 chooses effect/sound/knockback:

{{OPT1_GROUPS}}

A **reaction table** is a list of **steps** (rows of 14 bytes). The defender plays them in order; ground values are used while the defender is on the ground and air values while in the air (air columns are used when the defender's height is above the floor):

| Col | Key | Meaning |
|---|---|---|
| 0 | `DUR_C` | ticks of this step on the ground (0..255) |
| 1 | `DUR_A` | ticks of this step in the air; **255 = until landing** |
| 2 | `ANO` | the **defender's** animation (signed: negative = mirrored) |
| 3,4 | `XSPEED, YSPEED` | ground speed. Unit **1 = 0.1 px per tick**. **−1 = keep the previous step's value, −2 = half** |
| 5,6 | `XACCEL, YACCEL` | ground acceleration (same units); when acceleration makes the speed cross zero the speed stops (friction) |
| 7..10 | `XSPEED_A…YACCEL_A` | the same four for the air |
| 11 | `COMP` | next reaction code after this step (0 = none; 1..29 game reaction; 30..49 character slot) — "SEQ" lane |
| 12 | `WALL` | wall behaviour: with 2 or 3, if the defender touches the wall during the step, the reaction restarts at `COMP`; 0 = nothing |
| 13 | `VOZ` | voice sound id (1..127; 0 = none) played at the start of the step |

Each step **restarts the defender's animation** (ANO). The empty slots of the character (no rows) can be filled with the app (copy from the game data, copy from another slot, import from another character, or blank).

### 5.5 HDAT 4 — fields of a move (`$GOLPE_i`)

{{HDAT4_TABLE}}

**`ATK_FLAGS` bits** (sum the values you want):

| Bit | Name | Effect |
|---|---|---|
| 1 | GROUND_ONLY | the projectile explodes only on the floor (does not explode on the enemy) |
| 2 | NO_TRADE | the move's hitbox destroys the opponent's hitboxes (the opponent enters the reaction stored in the character's HDAT 3 byte 13) |
| 4 | TRIPLE | throws 3 projectiles (ANO, ANO+1, ANO+2) |
| 8 | GRAB | grab at the end of `ANIM_INICIAL` or during a frame with the window bit 512; continues to `ANIM_INICIAL + 1`; works even if it did not hit |
| 16 | GHOST | characters pass through each other |
| 32 | PROJ_OPP | the projectile spawns at the opponent (turns off 64) |
| 64 | PROJ_FLOOR | the projectile spawns under the opponent, stuck to the floor (turns off 32) |
| 128 | GRAB_HIT | grab only while the opponent is in this move's hurt reaction (moves 0..12 only; if the move has a sequence the sequence takes over and the grab does not happen) |

Move indexes: moves **0, 3 and 6** are special slots; when transplanting a move to another character, 0/3/6 can only replace 0/3/6 and the others only the non-special ones. Grabs only work in moves 0..12. Move text fields are 16-bit: **ANO numbers above 255 are normal** (up to 32767).

### 5.6 Sounds
Sound id `SE`: 0..79 = common game sounds (the app's sound list plays them); **80, 81, … = the character's own `*.wav` files in sorted file-name order** (first wav = 80). Voice in reactions uses the same ids (1..127). Adding/removing wavs shifts ids — warn the user before suggesting that.

### 5.7 Sprites and the CNV
Frame `$IMG: n` → `$SPR_n` in `000.cnv.txt` → rectangle in sheet `NNN.png` (`$src`). The sprite list screen (`SPRITES`) adds/replaces/removes sprites, edits cuts and imports PNG/JPG/GIF; new sprites go to the first free space of a sheet. Sheets are never rewritten destructively.

### 5.8 Linked animations (links) and effects
A link spawns another ANO together with its parent (effects, body parts, auras) and follows it. Only 1 level, max 13 per ANO. Effects can also be triggered by hit flags 10–17 / 41–125 (see 5.2).
