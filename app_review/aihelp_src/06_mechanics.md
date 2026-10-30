## 6. How a move works in the game (what you need to design one)

### 6.1 Time
60 ticks per second. A frame lasts `$TIME` ticks; an ANO ends after its last frame (unless the last frames are looped/redirected by a sequence). A frame **without `$TIME` freezes forever** and also does not add acceleration.

### 6.2 Movement (speed and acceleration)
- `SPEED` = constant pixels per tick while the frame lasts (`speed 50` for 5 ticks = 250 px; in text, decimals like `2.5` are px/tick).
- `ACCEL` adds its value to the speed **every tick after the object moved**: accel 50 for 5 ticks from rest moves 0, 50, 100, 150, 200 = 500 px and ends at speed 250.
- On loading each frame the game **zeroes speed and accel**, unless the frame has `$WACTFLAG` bit **64** ("keep speed"): then the frame starts with the speed the previous frame ended with (its speed + accumulated acceleration). A received speed counts as speed (a frame's own `$XSPEED` replaces it); accel is never inherited, so write it again in every frame that needs it.
- A jump is written as several frames: first frame with a negative `$YSPEED` (up) and a positive `$YACCEL` (gravity), the following frames with `$WACTFLAG: 64` so the speed carries over.
- Linked animations and frames synchronised with the parent (0x2000/0x8000) do not move by themselves.
- The editor's `Movimento` page and the `Σ` total show the pixels travelled.

### 6.3 Hits, damage and reactions
A **hitbox** (flag 2) in a frame whose `$OPT` has `d0` = damage and `d1` = reaction code (`group*50+index`) hits when it overlaps the opponent's **hurtbox** (flag 1); the same hit does not re-hit within the same frame. The hit applies the damage, plays hit effect/sound/hitstop by the reaction **group** (5.4) and puts the opponent in the **reaction** (a list of steps with the opponent's own ANOs and velocities). A hit also triggers the move's **sequence** if defined. A hitbox is always **one object per Frame** in the timeline (the hit repeats on every Frame, even with identical boxes); hurtbox, projectile, effect and action boxes with equal values across consecutive Frames still join into one object.

### 6.4 Sequences (combos inside a move)
HDAT 4 fields: `ANIM_SEQ_CHAO` / `ANIM_SEQ_AR` = ANO to go to when a hit connects while the attacker is on the ground / in the air; `ANIM_END_CHAO` / `ANIM_END_AR` = ANO to go to when a **new** hit connects while in the SEQ ANO (the END ANO itself has no further transition); `ANIM_AO_ACERTAR` (SEQ_PROJETIL) = ANO the attacker goes to when a **projectile's first hit** connects. A hit with flag 38 prevents the SEQ from firing. A SEQ may not point to its own ANO. If you define only ground or only air the app asks whether to use the same ANO for the other.

### 6.5 Landing
`POUSO_AO_ERRAR` / `POUSO_AO_ACERTAR` = ANO the attacker switches to when it touches the floor after a missed / connected aerial move. The `POUSO` button of the app toggles this in the preview.

### 6.6 Projectiles
A frame with flag 0 spawns the projectile ANO chosen from the HDAT 4 by the height difference between attacker and opponent: `PROJETIL_ANIM_NORMAL` (near the same height), `..._ABAIXO` (the **attacker** is ≥ 51 px lower than the opponent), `..._ACIMA` (the attacker is ≥ 37 px higher). `PROJETIL_ANIM_AO_ACERTAR` = the projectile's hit-reaction ANO, `PROJETIL_COLISAO_CHAO` = floor-collision ANO, `PROJETIL_ANIM_ACERTAR_GLOBAL` = global-hit ANO. A projectile ANO may itself contain flag 0 frames that spawn **sub-projectiles** (`SUBPROJETIL_*`). The main projectile spawns at attacker x + hit x (if it would land behind the attacker the game throws it 480 px ahead, so the minimum x is 0). `HOMING` + `HOMING_VELOCIDADE` make it chase the opponent; `MAX_REPETICOES` > 0 repeats the projectile (uses the 512 window, so a repeating move cannot also use "try grab"). **Max 8 live projectiles**; spawning with 8 alive creates nothing silently.

### 6.7 Effects and auras
Hit flags 10–17, 41–70 and 71–125 spawn effects/auras while the flag is present in the frame (5.2). For your own effect: make an ANO numbered `201 + (flag − 71)` in the character and put a hit with that flag (offset x,y) in the frames where it should be visible.

### 6.7b Dust / smoke ($OPT flag bits 4096-32768)
The `$OPT` flag bits 12-15 (value / 4096) are the dust **type 1-15**: one smoke is created per frame loaded, standing still in the world at the object's position (ANO +80 when the object is facing left; at most 6 alive at once, the 7th removes the oldest). Types 1-5 = `bobj` pack ANOs 10-14; 6-7 = `effect` ANOs 78-79; 8-15 = `effect` ANOs 70-77. A smoke ANO can spawn child smokes: each of its hits (flag N) creates `bobj` ANO N+70 at the hit's x/y. The app draws them and previews each type in the action menu. In the effects lane clips are labelled `effect N` / `charf N` / `char N` (N = ANO of that pack).

### 6.8 Interpolation and transforms
`$RAZ` (rotation, alpha, zoom) can be interpolated toward the next frame by `$OPT` d2 ≠ 0. The flag belongs to the frame that has it: that frame goes to the NEXT one (the frame before does not interpolate to it); on a frame without time it does nothing; on the LAST frame the "next" wraps to frame 1 (even without LOOP). The next frame should have the same number of boxes.

### 6.9 Camera, shake, cutscenes
Flags 3–5 (shake), 30/31 (zoom) and 20 (cutscene) act on the camera. Cutscenes are driven by data tables of the character (HDAT 13) — don't invent them; reuse an existing event.
