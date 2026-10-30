### 15.12 Worked examples and validation tests (questions 88–92)

**A. A simple attack that connects once, frame by frame [CODE-derived].** Data: move row 0 with `ANIM_INICIAL = 5`; ANO 5 = patterns P0 (`$TIME 4`, no boxes), P1 (`$TIME 3`, hurtbox + hitbox, `$OPT {d0 = 12, d1 = 52}`), P2 (`$TIME 6`, hurtbox). Defender on the ground at 40 px, group-1 reaction 2 (`OPT1 52`).
- **t0 (fighter task)**: `CmdCheck` returns 1 → `SpecialType = 0`, md 880 → `StartPlayerAttack`: HDAT 4 row 0 read; `SpecialKind`, `BulletAction` set; `objExec.ano = 0`; `_SetAct(5)` → P0 loaded (`time 4`, speeds 0). md 882.
- **t1–t3**: `time` 4 → 1; `PlayerTail` calls `_ActReq` each tick (move, accel, `--time`). `iHitCount = 0` (no hit block).
- **t4**: `time` reaches 0 → P1 loaded: `hit[]` = hurtbox + hitbox, `OptData[0..1] = 12, 52`, `iHitCount = 1`. Same tick, later in the task list (prio 13728) **md 935** runs `CheckObjHit`: boxes overlap (strict) → returns the hit.
- **md 935 on the hit**: `DamageChange`: guard check (human defender not holding back) → hit. Damage: `D1 = AttackMul*12/100*(100-def)/100` (min 2), combo scaling (`ComboCount = 0` → idx 0), `D3 = min(D2, Life)`; `LIFE_DEC`; `ComboCount[atk]++` (group 1 ⇒ effect ANO 0); effect ANO 0 + SE `HitSound[1] = 4`; `OPT1 52` → index 2 < 30 → md 876, `DamageType = 2`; both fighters get `iHitStop = PlayerHitStop (10)`; `iHitCount = 0`; `tcbPlayer[att].wFlag |= 1`.
- **hit-stop (10 ticks)**: animations frozen (`wActFlag |= 4`), defender jitters ±1 px; `CancelTime = 20`.
- **after hit-stop**: defender md 876 row 0 of gamedata reaction 2: loads `ANO`, speeds (`v*128/10`), voice; each tick accel → move; rows advance by `DUR_C/DUR_A`; at the end `DamageEnd` → md 864. Attacker: P1 runs its remaining 3 ticks, P2 6 ticks, ANO finished (`wActFlag & 2`) → md 883 → md 864 → `AttackEnd`.
- **A pattern that never hits**: P1 keeps testing for its 3 ticks; nothing happens; `CancelTime` stays 0 (no cancel on whiff).

**B. A projectile that hits once.** Move row with `PROJETIL_ANIM_NORMAL = 21`; the cast ANO has a type-0 entry in its pattern (x = 30, y = −40). `PlayerTail` spawns md 970 (`BulletNum` 1) → md 971: the ANO 21 script moves it each tick; its pattern with a type-2 box and `$OPT` (d0 damage, d1 reaction) is armed (`iHitCount = 1`); md 935 detects overlap with the defender's hurtbox via `tcbReqHit` → `DamageChange` with the **owner's** stats; defender hit-stop full, projectile half; if `SpecialType` matches, the owner gets `CancelTime = 20`, `wFlag |= 4096`; the projectile then switches to `PROJETIL_ANIM_AO_ACERTAR`/global ANO or dies (unless `ATK_FLAGS & 1`).
**Multi-hit**: either several consecutive patterns each with a hit block (each re-arms `iHitCount`), or a projectile ANO built the same way; every hit uses full formula with growing combo scaling.

**C. What to edit in a move such as `GOLPE_9` (cast ANO 51, projectile ANO 49, impact ANO 50)** — first read the `$GOLPE_9` text to see which fields point to 49/50/51 (15.7). Only damage: `$OPT` `d0` in the projectile's hit patterns. Only reaction: `d1`. Projectile speed: `$XSPEED` of the projectile ANO. Number of hits: patterns with a type-2 box. Impact animation: `PROJETIL_ANIM_AO_ACERTAR` or `_GLOBAL` ANO contents (or change which ANO they point to). Appearance vs. collision: sprites/`$IMG`/`$MID`/RAZ/`$MOVE` = appearance; type-2/1 boxes and `$OPT` = collision/damage.

**D. Validation tests for an external implementation (compare numbers, not feelings).**
1. *Formula*: `AttackMul=100, DeffenceMul=0, d0=10` → 10; `d0=1` → 2 (minimum 2); flag 28 with `d0=1` → 1; ANO 92 → 0; `d0=0` → 0.
2. *Combo scaling*: same hit repeated: `idx` sequence 0,1,0,1,2,3…13 (c = 0,1,2,3,4…) using the data table; value ≥ 1.
3. *Hit once*: a 10-tick pattern with a hitbox deals damage exactly once; two consecutive patterns with hit blocks deal it twice.
4. *Strict overlap*: boxes touching at an edge (right of A == left of B) → no hit; 1 px overlap → hit; flipped facing mirrors X.
5. *Hit-stop*: both fighters frozen for `PlayerHitStop` ticks (10 specials, 6 basics); group 3 half for body hits; group 4 none.
6. *Reaction rows*: row duration `max(dur,1)` ticks; `-1` keeps, `-2` halves (raw unsigned quirk); speed unit 1 = 0.1 px/tick; X friction zeroes speed when the sign flips; 255 = hold until `DamageTime ≥ 255` then type 2; COMP 11 knocks down on landing; WALL 2/3 restarts at COMP on wall/landing.
7. *Order*: reaction accelerates-then-moves, animation script moves-then-accelerates; load tick of an animation pattern moves nothing; accel is zeroed on load; speed zeroed unless `wActFlag & 64`.
8. *Limits*: 10 boxes/frame, 13 links, 255 patterns, `time ≤ 255`, OPT1 ≤ 249, BulletNum < 8.
9. *Trade*: both hit in the same tick → OPT1 forced to 0, index 1 wins double KO.
10. *KO*: life ≤ 0 → type 2 regardless of OPT1, then lying (ANO 24).

**E. What cannot be reproduced from the extracted data alone [CODE/UNKNOWN].** The numeric content of the data pacs (gamedata reaction rows, combo-scaling table HDAT 5, `HDAT3[13]` clash reactions, card/level stats), the CPU decision code (random guard, AI macros), the lockstep input buffer latency, exact screen/camera scrolling (`md 19`), and the cutscene events (HDAT 13). Everything else in combat is deterministic integer logic that the app's `BattleSim` already mirrors.
