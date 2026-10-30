### 15.10 Flags and special behaviours (questions 77–81 and 10–12)

There are **four different flag spaces**; never mix them.

**(a) `ATK_FLAGS` = HDAT 4 byte 0 = `SpecialKind[p]` [CODE].** `1` projectile survives hitting the opponent (keeps flying; jogo:24569) · `2` the move's hitbox **destroys/beats the opponent's attack boxes** and forces the opponent into the reaction stored in the *attacker's* `HDAT3[0][13]` (clash handling, jogo:23286, 23339, 23486-23516) · `4` throw 3 projectiles (jogo:29812) · `8` simple grab · `128` conditional grab (only while the opponent is in this move's hurt reaction; grabs work in moves 0–12) (jogo:21929) · `16` stop the camera push — characters pass through each other (jogo:15176) · `32` projectile spawns at the opponent's X/Y · `64` at the opponent's X on the ground (jogo:29822-29827). Bits 256+ unused **[UNKNOWN]**. Flags 8/128 and `MAX_REPETICOES` share the frame window 512.

**(b) Hit-box `type` numbers inside a frame (`$HT_i: {$FLAG: n}`) [CODE: `CheckOptFlag` jogo:2112-2350, `_ActReqMain`, `PlayerTail`].** Processed markers are set to −1 after use, so they fire **once per pattern**; the per-frame state bits are cleared each tick (`PlayerState &= -50069508`) and re-applied from the current pattern.

| type | Effect / parameters |
|---|---|
| 0 | projectile spawn (x, y offset) — spawns once per pattern |
| 1 | hurtbox |
| 2 | hitbox |
| 3, 4, 5 | screen shake light/medium/strong (`iBackShakeType`, `iBackShake = 12`) |
| 10–17 | effect-pack dust, ANO 50 + n − 10, offset (x, y) |
| 20 | cutscene: md 996, row `hit[3]` (the width field) of HDAT 13 |
| 26 | `PlayerState |= 2`: opponent cannot guard |
| 27 | `|= 524288`: cancel blocked while `CancelTime > 0` |
| 28 | `|= 262144`: raw damage (no attack/defense/rage/min-2) |
| 30, 31 | action zoom variables |
| 32 | `|= 1048576`: self-damage frame (`SelfDamage(p, d0, d1)`; cannot kill) |
| 33 | `|= 2097152`: set but never read — **no effect** |
| 34 | heal `LIFE_ADD(OptData[0])` (once; needs `iTimer != 0` and the opponent alive) |
| 37 | `|= 4194304`: no combo count, no score |
| 38 | `|= 8388608`: the SEQ/END chain uses `DamageHitActNo[opp]` instead of HDAT4[32]; also blocks the SEQ in the app's model ("block sequence") |
| 39 | teleport self onto the opponent (jogo:2314-2337) |
| 40 | teleport the opponent to self + (x, y) mirrored by facing |
| 41–70 | charf-pack bubble, ANO n − 41 + 3, never mirrored |
| 71–125 | character's own effect/aura ANO 201 + n − 71, follows the owner while present |
| other | ignored **[CODE: no handler found]**; hit numbers 6–9, 18–19, 21–25, 29, 35–36 have no processing in `CheckOptFlag` |

**(c) `wOptFlag` bits (the `$OPT` `$FLAG` word) [CODE].** `256` survives the mask, **no reader found** [UNKNOWN] · `512` chain/cancel/repeat/grab window (read by md 882/883: advancing to `ano+1` on grab, repeat-fire, cancel timing) · `1024` `PlayerState |= 1` (hide background, suppress wall detection; blackout/super marker) · `2048` wall-impact effect (needs `DamageWall`) · `4096·t` (t 1..15) dust/smoke selector. After each pass `wOptFlag &= 1792` so 2048 and the dust bits fire once, 256/512/1024 persist for the pattern.

**(d) `PlayerState` bits (internal) [CODE].** 1 = background/blackout, 2 = unguardable, 4…131072 = always-set per-slot gates, 262144 raw damage, 524288 no cancel, 1048576 self-damage, 2097152 unused, 4194304 no combo, 8388608 chain override, 16777216 hidden/KO hold, 33554432 cutscene alive.

**Which flags block defense / counter / teleport.** Unguardable: hit flag 26. No cancel: 27. Ignore defense: 28. Flag 38 only changes which ANO the SEQ chain reads. Nothing in the data prevents a *teleport counter*: it is a defender-side evade (SpecialType 18/19, md 865) available when `CmdCheck` returns 6/7.

**Flags that "do nothing" in some attacks [CODE].** 33; 34/32 with `OptData[0] == 0`; 37 without a combo; 38 in a move with no SEQ; 39/40 when the opponent is dead (the ANO ends); 20 with an event id that does not exist in HDAT 13 (the task dies at once); `wOptFlag` 2048 without `DamageWall`; HDAT 4 `HOMING_VELOCIDADE` when `HOMING ≠ 0`; `MAX_REPETICOES` for SpecialTypes other than 0/3/6; `ATK_FLAGS` 8/128 in moves above 12 or with a sequence (128). **How to discover unknown flags**: search `TCBManajer.java` for the bit constant (e.g. `& 256`) and for `hit[... ] ==` comparisons; flags that appear in data but have no reader are inert.
