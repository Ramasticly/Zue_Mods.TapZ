## 14. FAQ — ready answers

**Why does my attack not hit?** Check: a hitbox (flag 2) exists in the frames where the attack should connect; its `$OPT` `d1` is a valid reaction code (≤ 249) — and a character-slot code (30–49) points to a **non-empty** slot; the defender's hurtbox (flag 1) overlaps (use `HITBOX` and `POS`); `GOLPE SET` options are not hiding the hit (e.g. "ground only" only affects projectiles).

**Why does the projectile not appear?** Flag 0 hit exists in a frame that plays; `PROJETIL_ANIM_NORMAL/ABAIXO/ACIMA` for the current height difference is non-zero and the ANO exists and is non-empty; already 8 projectiles alive; the move missed/was sequence-blocked (see warnings); a projectile ANO has links with links (ignored). The app shows a warning with the reason.

**Why does a frame never end?** It has no `$TIME`.

**Why does the character not keep moving in the next frame?** The next frame needs `$WACTFLAG: 64` (keep speed) and acceleration must be rewritten.

**How do I change how far the opponent flies?** In the reaction (`EDITAR REAÇÃO`): steps' speeds (unit 0.1 px/tick), durations, and whether ground or air columns apply (opponent height). Use the other reaction group for different hitstop/knockback (5.4).

**Where do I save?** Section 13: recommended `Salvar em pasta separada` → `Tap Battle/TBRE/Chars/charNN/`.

**I added/deleted a character folder or replaced a `.txt` and the app does not show it.** Tap `↻` in the title bar (confirm). It rereads the character folders, effects, scenes and sounds and keeps the same character/move if they still exist. Unsaved edits are lost, so `SALVAR` first.

**How do I undo?** `DESFAZER`/`↶`. Saved files keep what was saved; backups are in `Tap Battle/TBRE/Backups/`.

**Can you make me a sprite/sound?** No. Tell me what is needed (size, frames, names); the user imports them (11.5).

**What are the numbers 0, 3, 6 of moves?** Special move slots; they can only be transplanted onto 0, 3 and 6.

**The app says "recorte fora do PNG".** A `$SPR_n` rectangle is outside its sheet: fix `$x/$y/$w/$h` (remember characters use 2× physical pixels in the `.cnv.txt`).

**What does an amber vs. red warning mean?** Red = the game would close or fail; amber = the editor sees something the game handles differently.
