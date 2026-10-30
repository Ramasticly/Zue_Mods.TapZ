## 8. Recipes (what to tell the user, step by step)

Recipes A, C (steps 2 and 4), E, F and G are **Mode A**: tell the user which app functions to use. Recipe B and the data parts of C/D are **Mode B**: you produce `.txt` content (section 12) and the user reloads with `↻`. In neither mode teach binary editing or the `▲ DADOS / EDITAR` panel.

**A. Change speed/range/timing of an existing attack**
1. User selects the character (`CHAR`) and the move in the selector (`MODO: GOLPES`).
2. User taps the sprite on the timeline whose frame to edit → drawer grid → `Tempo` (ticks) or `Movimento` (speed/accel; `MANTER VELOCIDADE` = WACTFLAG 64).
3. Several frames at once: `Copiar` page of the drawer (apply this frame's properties to the next N / all frames / this sprite in the ANO).
4. Test (section 10), then `SALVAR`.

**B. New attack by copying a similar one (recommended)**
1. Ask for the character's `000.dac.txt`, `000.bin.txt` and `000.cnv.txt` and which move is the base.
2. Decide new ANO numbers: first free numbers after the highest existing ANO (ask the user for the highest, or the `.dac.txt`); remember effect/aura ANOs 201–255 are reserved by flags 71–125.
3. Write the new `$ANO_n` blocks (complete, with all frames) and the edited `$GOLPE_i` (point `ANIM_INICIAL`, projectile fields, SEQ/END, landing to the new ANOs).
4. Deliver per section 12: the new `$ANO_n` blocks go inside `DAC_ACT_ { }` of `000.dac.txt` and the `$GOLPE_i` block replaces the one in `000.bin.txt`. The user keeps a copy of the original `.txt` first (`SALVAR` → separate folder is a full backup), puts the new files in the character folder and taps `↻`.
5. Test; fix warnings; `SALVAR`.

**C. Add a projectile**
1. Make a projectile ANO (frames with the projectile sprites, a hurtbox/hitbox flag 2 with `$OPT` damage+reaction, `$MOVE: {$XSPEED: n}` speed, `$TIME`). Often copy an existing projectile ANO.
2. In the attack's frame add a hit `{$FLAG: 0, $COORDS: [x,y,0,0]}` at the spawn point.
3. In `$GOLPE_i` set `PROJETIL_ANIM_NORMAL` (and `_ABAIXO`/`_ACIMA` if the attacker can be much lower/higher than the opponent) to the projectile ANO. Optional: `PROJETIL_ANIM_AO_ACERTAR`, `PROJETIL_COLISAO_CHAO`, `ATK_FLAGS` 4 (triple), 32/64 (spawn at opponent), 1 (explode only on floor), `HOMING`.
4. In the app the "projectile screen" (tap the flag-0 hit) edits start/duration, branching tree, ANO, position/speed, homing and sequence.

**D. Combo / sequence inside a move**
Set `ANIM_SEQ_CHAO`/`ANIM_SEQ_AR` (and optionally `ANIM_END_*`) to the follow-up ANO; make sure the first ANO has a hitbox with a reaction. Use hit flag 38 on a hit that must **not** trigger the sequence. The timeline lane "Transições" shows what will fire; `VER FLUXO` shows the flow. `TRANSIÇÃO` (top button) freezes ANO switching while editing one ANO.

**E. Change what the defender does when hit (reactions)**
Pick the reaction code in the hitbox menu (`REAÇÃO`, `ESCOLHER`), or edit the character's reaction (`EDITAR REAÇÃO`: steps, speeds, voice, wall, next reaction). Empty character slots can be filled. Remember the saved `000.bin.txt` must contain the `$HDAT_(14+slot)` block.

**F. New sprites / sounds**
Sprites: user opens `SPRITES` → add/replace (PNG/JPG/GIF), adjust cut, then uses the new `$IMG` index in frames. Sounds: add `.wav` to the character folder (ids shift!) or use the sound list in the frame's `Som` page.

**G. Put a move in another character**
`SALVAR` → `Salvar golpe atual` → `Salvar em outro personagem (Runtime)`: choose character, choose which move to replace (0/3/6 only into 0/3/6; others into non-special), the move's HDAT 4 row, its ANOs (appended at the end of the target DAC), sprites/CNV and the character's reactions it needs are copied; a backup goes to `TBRE/Backups/`. Sounds (id 80+) and hit flags 20 and 41–125 are not copied.
