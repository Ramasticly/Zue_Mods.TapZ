## 1. What the app is and how the user works

*Attack Editor* loads a character's files from the phone, **simulates a whole fight in a preview** (attacker, opponent, projectiles, effects, camera, cutscenes) the same way the game does, and lets the user edit animations, moves, projectiles and hit reactions with a **timeline** (like a video editor) and edit menus. The user then saves, and the game reads the saved text files.

Screen (portrait), top to bottom: title bar (`Attack Editor by Zue Mods`, buttons `↻` reload folders, `PT/EN/ES` language, `AJUDA`); the **top menu** (14 buttons, section 11); `MODO: GOLPES | ANO` and the move/animation selector; a status/warning line; the **preview** (square); the **timeline** with its tool row (`DIVIDIR`, `DUPLICAR`, `EXCLUIR`, `DESFAZER`, tabs `＋` and `#`); a collapsed `▲ DADOS / EDITAR` panel (do not teach it); and the fixed transport bar (`INICIO`, `-1`, play/pause, `+1`, speed). Tapping an item on the timeline opens an **edit drawer** (bottom sheet) over the screen; drag its handle to resize, `Y` saves the height as default, `T` puts the timeline above the menu.

Two modes: **GOLPES** (moves: a move is one row of HDAT 4 with its ANOs, projectiles, sequence and reactions) and **ANO** (play/edit one single animation, ignoring the move around it).

## 2. Files and folders (Android, "all files access")

```
/storage/emulated/0/Tap Battle/
  assets/
    charNN/            one folder per character (NN = 2 digits). The game reads this ("Runtime").
      000.dac          binary animations      (never rewritten by the app)
      000.bin          binary data/HDATs       (never rewritten by the app)
      000.cnv          binary sprite-cut table (never rewritten by the app)
      000.dac.txt      readable animations     -> the app WRITES here
      000.bin.txt      readable data (moves, reactions) -> the app WRITES here
      000.cnv.txt      readable sprite cuts    -> the app WRITES here
      NNN.png          sprite sheets (000.png, 001.png, ...) — read-only for the app (kept intact)
      *.wav            character sounds (ids 80, 81, ... in file-name order)
    effect/            shared effects pack (dac + cnv + sheets)
    charfNN/           auxiliary pack (CHARF auras/bubbles)
    bobjNN/            scenery objects used by cutscenes
  TBRE/
    Chars/charNN/      "separate folder" saves: a COMPLETE copy of the character with the changes applied, ready to use
    Backups/           safety copies (e.g. before transplanting a move into another character)
```

**The `.txt` files win over the binaries.** When loading, the app (and the game build) takes `$ANO_n` blocks from `000.dac.txt`, moves (`$GOLPE_n`) and reactions (`$REACAO_DANO_n`) from `000.bin.txt`, and sprite cuts from `000.cnv.txt` when they exist. ANOs that exist only in the `.txt` (beyond the binary's last ANO) are appended to the animation table. So **a change is delivered as text** and the binaries need not be touched.

To make the app re-read the disk after files were changed outside the app: pick the character again in `CHAR` (or restart the app).
