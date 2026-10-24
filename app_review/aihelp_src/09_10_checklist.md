## 9. Validation checklist (run it mentally before delivering data)

1. Syntax: every `{` closes; keys are known; `$MPT_i` numbered 0.. consecutively; every frame has `$IMG`; the ANO has ≥ 1 frame.
2. Per ANO: ≤ 255 frames; ≤ 13 links, no link-of-link; each `$LINK` `$ANO` exists.
3. Per frame: ≤ 10 `$HITS`; `$TIME` ≤ 255 (and present unless a frozen frame is intended); `$IMG` < number of `$SPR_` entries; `$OPT` `d1` ≤ 249 and a hitbox exists when `d1` matters.
4. A move: `ANIM_INICIAL` exists; every referenced ANO number exists (0 = none); no SEQ to its own ANO; if it grabs (8/128) it must not also use `MAX_REPETICOES` or have a SEQ with 128.
5. Projectiles: ≤ 8 alive at a time (consider repeats and triple); the projectile ANO has a hitbox (flag 2) with damage/reaction.
6. Speed/accel: remember accel is not inherited, `WACTFLAG` 64 to carry speed, the first frame after a jump has gravity written again.
7. Interpolation (`$OPT` d2 ≠ 0): next frame has `$RAZ` and the same number of boxes.
8. New ANO numbers do not collide with existing ones or the reserved 201–255 effect range (unless intended).
9. Reaction rows: 14 values/columns, speeds −128..127, `DUR` ≥ 1, `$ROW`/`$SZ` consistent when hand-editing.

## 10. How to test in the app

1. Open the character (`CHAR`), choose the move (`MODO: GOLPES`) or the ANO (`MODO: ANO`).
2. Press play (`TOCAR`) or drag the time bar / timeline; use `-1` / `+1` to step tick by tick; `INICIO` resets.
3. Top-menu helpers: `CHAR`→`POS` changes distance/height between fighters (tests which projectile ANO spawns: normal/below/above) and `FLY`; `HITBOX` shows boxes (red hit, blue hurt, yellow others); `TEMPO` NORMAL/ESTENDIDO/ATAQUE (extended keeps the attacker on the last frame so you can see projectiles and the opponent's reaction to the end); `POUSO` ON/OFF; `TRANSIÇÃO` LIVRE/TRAVADA; `VIRAR` flips sides; `HIDE-EFF` hides effects; `FR-PREVIEW` ghost of previous/next frame; `TELA` grid/sounds/cutscenes options and a game-camera view; `CENTRO` camera modes.
4. Read the warning line: red = the game would close or fail; amber = the editor sees something the game treats differently. Tap it to expand.
5. If something looks wrong: `DESFAZER` (↶) undoes the last edit; `FR-PREVIEW` helps align sprites; the `Σ`/Movimento total shows pixels.
6. Test the saved result in the game itself (see section 13). The app is a faithful simulator but the game is the final judge.
