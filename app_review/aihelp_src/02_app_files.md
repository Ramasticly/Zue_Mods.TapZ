## 1. What the app is and how the user works

*Attack Editor* loads a character's files from the phone, **simulates a whole fight in a preview** (attacker, opponent, projectiles, effects, camera, cutscenes) the same way the game does, and lets the user edit animations, moves, projectiles and hit reactions with a **timeline** (like a video editor) and edit menus. The user then saves, and the game reads the saved text files.

Screen (portrait), top to bottom: title bar (`Attack Editor by Zue Mods`, buttons `↻` reload folders, `PT/EN/ES` language, `AJUDA`); the **top menu** (14 buttons, section 11); `MODO: GOLPES | ANO` and the move/animation selector; a status/warning line; the **preview** (square); the **timeline** with its tool row (`DIVIDIR`, `DUPLICAR`, `EXCLUIR`, `DESFAZER`, tabs `＋` and `#`); a collapsed `▲ DADOS / EDITAR` panel (do not teach it); and the fixed transport bar (`INICIO`, `-1`, play/pause, `+1`, speed). Tapping an item on the timeline opens an **edit drawer** (bottom sheet) over the screen; drag its handle to resize, `Y` saves the height as default, `T` puts the timeline above the menu.

Two modes: **GOLPES** (moves: a move is one row of HDAT 4 with its ANOs, projectiles, sequence and reactions) and **ANO** (play/edit one single animation, ignoring the move around it).

## 2. Files and folders (Android, "all files access")

```
/storage/emulated/0/Tap Battle/
  assets/
    charNN/            one folder per character (NN = 2 digits). The game reads this ("Runtime").
      NAME.dac.txt     readable animations     -> the app READS and WRITES here
      NAME.bin.txt     readable data (moves, reactions) -> the app READS and WRITES here
      NAME.cnv.txt     readable sprite cuts    -> the app READS and WRITES here
      NNN.png          sprite sheets (000.png, 001.png, ...) — read-only for the app (kept intact)
      *.wav            character sounds (ids 80, 81, ... in file-name order)
    effect/            shared effects pack (dac + cnv + sheets)
    charfNN/           auxiliary pack (CHARF auras/bubbles)
    bobjNN/            scenery objects used by cutscenes
  TBRE/
    Chars/charNN/      "separate folder" saves: a COMPLETE copy of the character with the changes applied, ready to use
    Backups/           safety copies (e.g. before transplanting a move into another character)
```

**The app reads ONLY the `.txt` files — never a binary.** `NAME` is any name: the game extractor writes `000.dac.txt`, `000.bin.txt`, `000.cnv.txt`, but many mods save other names (`char13.dac.txt`...), and the app accepts them (the `000.` one first if there are several; normally there is only one of each kind per folder). Animations (`$ANO_n`) come from `*.dac.txt`, moves (`$GOLPE_n`), cutscenes and reactions from `*.bin.txt` (format `BIN_CNV_ {$COUNT, $HDAT_n...}`), sprite cuts from `*.cnv.txt`. The game data (`gamedata` folder) is also read from its `*.dac.txt` (it is `BIN_CNV_` inside). If a needed `.txt` is missing the app shows a clear error naming the missing file and does not open that folder; it does not fall back to the binary (some apps extract only the `.txt`). So **a change is delivered as text** and the binaries are irrelevant to the app.

To make the app re-read the disk after files were changed outside the app: pick the character again in `CHAR` (or restart the app).
