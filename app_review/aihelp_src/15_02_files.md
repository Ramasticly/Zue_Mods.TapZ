### 15.2 Data loading and file structure (questions 9–15)

**Loading [CODE].** `Init` (jogo:28611) → `InitGameData` (jogo:29127): `pGameData` = `gamedata.pac`, `pTextData` = text00 (`GameData.Init(gw, bytes, cnvType = 3, filter = 0)`; with `cnvType 3` the `dac` chunk is converted by `binCnv` into `piGameData/piGameDataPos/XSize/YSize`, GameData.java:230-239, 287-316). Per battle md 713 → 714 (jogo:5324-5400) uses `SetLoad` (jogo:30958): `LoadData[3] = PlayerType[0]`, `[4] = PlayerType[1]`, `[5] = 0`; background number from the opponent's HDAT 39 (jogo:5390-5395). `SetLoad` loads a slot only if `CreatedAct[i] != IDList[i] + LoadData[i]` (jogo:30972): **the same id is not re-read**. `ChrGameData[i].Init(gw, name, cnvType = 2, filter)` opens `assets/<name>.pac`: for `cnvType 2` the `bin` chunk becomes `piGameData`/HDATs, the `dac` chunk stays raw (`getData(3)`), the `cnv` chunk stays raw (`getData(0)`, 9 bytes per image, `DrawImage` jogo:620), `png` chunks become textures. `LoadingProcess` (jogo:29180) is the incremental version. A transformation reloads (md 990 → `SetLoad`, jogo:25020-25025).

| `ChrGameData` slot | Resource |
|---|---|
| 0 | common |
| 1 | `back##` (or select0) |
| 2 | `bobj##` |
| 3 / 4 | `char##` of player 1 / 2 (read as `ChrGameData[p+3]`) |
| 5 | `effect[_en]` (the effect pack; `LoadData` 1 = `demo_00`, 2 = `demo_08`) |
| 6–26 | `chardemo##` or `card###` |
| 27, 28 | `charf00##` (face) |
| 29 / 30 | card_preview / font00 |

(`effect_en` etc. chosen by `iLanguage*17`, `GetResName2` jogo:28447.) **Read once**: the pacs at load. **Read every tick**: the DAC bytes through `_ActReqMain`/`_SetAct`; the HDAT 4 fields while md 882/883/970/971/940/941 run; HDAT 3 body data; the reaction table row on each reaction tick; HDAT 1 (Synchro/Angry gain ratios), HDAT 5 (score/combo scaling), HDAT 37[0] (rage multiplier), HDAT 6 (CPU sequences), HDAT 42 (touch buttons: [0] command, [2] cost in hundreds), HDAT 13 (cutscene rows), HDAT 0 (AI move table).

**HDAT 4 — the move table [CODE].** Row = `SpecialType` = the move number; row stride = `2 * piGameDataXSize[4]` bytes (24 fields of 16-bit little-endian = 48 bytes); field `k` is at byte `2k` of the row. The editor's `$GOLPE_n` text keys map to these **byte offsets** (the app reads exactly these in `CharData.read`):

| Byte | `$KEY` in `$GOLPE_n` | Used by the game as |
|---|---|---|
| 0 | `ATK_FLAGS` | `SpecialKind[p]` bitmask (see 15.10) |
| 2 | `ANIM_INICIAL` | the ANO started by `StartPlayerAttack` (jogo:31648) |
| 4 | `PROJETIL_ANIM_NORMAL` | `BulletAction[p][0]` when the heights are similar (jogo:31672) |
| 6 | `PROJETIL_ANIM_AO_ACERTAR` | `BulletAction[p][1]` → projectile task `_work[5]`: the projectile ANO it switches to **on hit** (jogo:31673) |
| 8 | `POUSO_AO_ERRAR` | landing ANO after a missed aerial move (md 883, jogo:21969) |
| 10 / 12 | `ANIM_SEQ_CHAO` / `ANIM_END_CHAO` | SEQ / END on the ground (jogo:21900-21901) |
| 14 / 16 | `ANIM_SEQ_AR` / `ANIM_END_AR` | SEQ / END in the air (jogo:21903-21904) |
| 18 | `SUBPROJETIL_ANIM_ID` | sub-projectile ANO (jogo:24660) |
| 20 | `SUBPROJETIL_ANIM_AO_ACERTAR` | sub-projectile ANO on hit (jogo:24675) |
| 22 | `POUSO_AO_ACERTAR` | landing ANO after a connected aerial move |
| 24 | `OPONENTE_ANIM_SENDO_AGARRADO` | grabbed-opponent overlay ANO (md 940/941, jogo:23627) |
| 26 | `HOMING` | homing is active when this equals **0**, ≥ 1 disables it (looks inverted; jogo:24610) |
| 28 | `HOMING_VELOCIDADE` | homing speed (jogo:24611) |
| 30 | `MAX_REPETICOES` | max repeats (md 883, jogo:21997) |
| 32 | `ANIM_AO_ACERTAR` | alternate hit ANO = the "SEQ_PROJETIL" ANO (jogo:21871) |
| 34 | `PROJETIL_ANIM_ABAIXO` | used when `PlayerYPos[att] >= PlayerYPos[opp] + 3342336`: the **attacker is ≥ 51 px lower** than the opponent (Y grows downward) |
| 36 | `PROJETIL_ANIM_ACIMA` | used when `PlayerYPos[att] <= PlayerYPos[opp] - 2424832`: the attacker is ≥ 37 px higher |
| 38 | `PROJETIL_ANIM_ACERTAR_GLOBAL` | projectile "global hit" ANO (`_work[12]`, jogo:29872) |
| 40 | `PROJETIL_COLISAO_CHAO` | projectile floor ANO (`_work[13]`, jogo:29873) |
| 42 | `SUBPROJETIL_COLISAO_CHAO` | sub-projectile floor ANO (jogo:24687) |
| 44, 46 | `DESCONHECIDO_44/46` | never read through table 4 **[CODE]** |

Between 37 px above and 51 px below the opponent the NORMAL ANO is used. ANO fields are 16-bit signed (0 = none); the value `-1` appears as "absent".

**Other tables used by attacks [CODE].** HDAT 3 (body data, row 0): byte 0/1 forward/back speed, byte 3 body width (half used for walls), byte 4 `PlayerYLimit`, byte 5 grab distance, byte 6 `AirType`/"can fly", byte 7 dash speed, byte 10 charge gain, byte 12/14 attack/defense base, **byte 13 clash reaction** (the reaction a hitbox with `ATK_FLAGS` bit 2 forces on the opponent), byte 16 sealed-art ANO. HDAT 13: cutscene rows (stride `2*XSize[13]` bytes). HDAT 14–33 (character) / 10–39 (gamedata): reactions, 14 columns (15.5). HDAT 6: CPU sequences. HDAT 5: score and combo-scaling table. HDAT 37/35: rage multiplier and event damage.

**ANO / DAC structure [CODE].** `ObjReq.acttype` is the `ChrGameData` index (fighter p+3, effects 5, common 0); `acttype & 0xFF000` marks pseudo-sprites skipped by `_ActReqMain` (jogo:31773). `_SetAct` (jogo:32325): `getData(3)` is the raw `dac` chunk: `[2]` = ANO count (short), `[4]` = ANO table offset, `[6]` = entry data base; the pointer is `short @ table + ano*2` (−1 = absent); the entry is at `(ptr << 2) + base`: byte 0 = flags (4 = priority byte present, 2 = link block), byte 1 = `mpt` (pattern count) (jogo:32354-32366). Links are 6 bytes each (x, y, flags|ANO & 4095); up to 13 (`lnk[13]`). **Calling `_SetAct` with the same `ano` and `acttype` is a no-op** (jogo:32338) — that is why the game sets `objExec.ano = 0` first to restart. A missing ANO leaves `apt = 0` and the next `_ActReq` sets `wActFlag |= 2` (the move ends next tick). Pattern fields: see 15.8.

**Alignment, sentinels, limits an editor must respect [CODE].** All multi-byte values are little-endian; `piGameData` is a `short[]` of **unsigned bytes** (the code casts `(byte)` for signed reads: reaction ANO, speeds, voice); ANO numbers in links are 12-bit (≤ 4095); `hit[50]` = 10 boxes max per frame; `lnk[13]`; pattern count 1 byte (255); `time` 1 byte; `OptData` 4 bytes; OPT1 ≥ 250 crashes (`HitEffectActNo[5]`); reaction `COMP ≥ 30` in md 876 reads out-of-range tables; `-1` ANO = absent; `-1`/`-2` in reaction speeds are sentinels (15.5). **Local vs. global indices**: ANO numbers in a character's HDAT 4 / links / DAC are **local to that character's DAC**; hit flags 10–17 use the **effect pack** (ANO 50 + flag − 10), flags 41–70 use the **charf pack** (ANO flag − 41 + 3), flags 71–125 use the **character's own DAC** (ANO 201 + flag − 71); reaction types 0–29 are global (gamedata), 30–49 are the attacker's own table. **Differences between characters/versions**: none found in the code (one code path for all `char##`); data differ only in the pacs **[INFERRED]**.
