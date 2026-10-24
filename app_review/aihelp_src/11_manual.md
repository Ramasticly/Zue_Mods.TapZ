## 11. Screens and buttons (what each control does)

### 11.1 Top menu (14 buttons; the user can long-press and drag them to reorder; the first is pinned)
- `SALVAR` — save (section 13).
- `CHAR` — choose the **attacker** and the **opponent** characters; `POS` sets distance/height between them by dragging on screen (and `FLY`: attacker airborne); `CHAR OBJ` shows body box and sprite anchors and edits the body size.
- `SPRITES` — the character's sprite list: add, replace, remove, reorder, change the cut, import PNG/JPG/GIF; also choose the sprite for a frame.
- `LINKS` — linked animations of the current ANO (list, edit, sync flags `SPAWN_POS`/`XY`/`MPT`, edit position over the base animation).
- `GOLPE SET` — the move's `ATK_FLAGS` options (checkboxes; table in 5.5).
- `HITBOX` — draw boxes on the preview (red hit, blue hurt, yellow others).
- `TEMPO` — NORMAL / ESTENDIDO / ATAQUE playback modes.
- `CENTRO` — camera NORMAL (free, pinch to zoom) / TELA (game camera 480×320) / CHAR (auto-frame both).
- `POUSO` — ON/OFF landing & fall ANO changes.
- `TRANSIÇÃO` — LIVRE/TRAVADA: freeze ANO switching from the current frame (only for moves with a sequence).
- `VIRAR` — flip sides. `HIDE-EFF` — hide effects/auras/scenes/projectiles. `FR-PREVIEW` — previous/next frame ghost (settings in `TELA` > FR‑PREVIEW SET). `TELA` — grid, sounds, cutscenes, "lock" (cinematic), yellow box of all sprites.

### 11.2 Timeline
Tap a sprite clip to open its drawer. Lanes: SPRITES (yellow clips), then hitbox/hurtbox/projectile/effect/special-action lanes, SOM (blue), VELOCIDADE (green), ESTILO (orange: flip/transform), LINKS, TRANSIÇÕES (SEQ/END/landing marks). `＋` adds at the pointer (sprite, hitbox, hurtbox, projectile, effect, sound, speed, link, special action); `#` filters lanes. Tool row: `DIVIDIR` splits a clip/item at the pointer, `DUPLICAR`, `EXCLUIR`, `DESFAZER`. Drag a clip's edge to change its duration; drag item edges to trim. Pinch = zoom; vertical drag scrolls lanes.

### 11.3 Edit drawer pages
Sprite grid → `Imagem` (sprite picker, position, zoom, rotation, alpha, "move links together"), `Tempo` (ticks), `Som` (sound id list with preview), `Movimento` (speed/accel per axis, `MANTER VELOCIDADE`, total pixels, remove speed), `Efeitos` (flip X/Y, interpolation), `Copiar` (apply this frame's properties to other frames: next N, previous N, all, this sprite in ANO / in all ANOs). Hitbox/hurtbox menu: type switch, start frame & duration, preview, x/y/w/h, and (hitbox) `DANO`, `REAÇÃO` (◀ ▶, `ESCOLHER`, `EDITAR REAÇÃO`), transition (SEQ/END, `MUDAR ANO DESTINO`, `BLOQUEAR SEQUÊNCIA`). Projectile screen: start & duration, branching tree, projectile ANO, position/speed, homing, sequence.

### 11.4 Reaction editor
`EDITAR REAÇÃO` (character) / `VER REAÇÃO` (game, read-only). Same layout as the main screen: header (`CHÃO | AR` scenario), square preview, time bar, buttons `DIVIDIR · DUPLICAR · EXCLUIR · DESFAZER`, timeline (PASSOS clips + lanes SPEED blocks, VOZ, EFEITOS, SEQ, CURVA; tabs `＋` and `#`), transport, drawer with a page grid (ANIMAÇÃO, TEMPO, SPEED, VOZ, EFEITOS, SEQ, TOTAL). Drag a step's right edge = duration; drag a SPEED block's right edge = how many steps keep that speed. `DIVIDIR` splits a step at the pointer (the defender's animation restarts in the 2nd half). `DADOS / EDITAR`: collapsed panel with text tabs and a pencil to edit/paste text.

### 11.5 Importing what you cannot generate
- Images: `SPRITES` → add/replace → pick the file; then set the frame's `Imagem`.
- Sounds: put the `.wav` in `Tap Battle/assets/charNN/` (ids are in file-name order, from 80) or use the frame's `Som` page.
