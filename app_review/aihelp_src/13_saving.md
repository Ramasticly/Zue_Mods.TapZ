## 13. Saving, backups and testing in the real game

The `SALVAR` menu first asks **what** to save, then **where**.

What: the current move (HDAT 4 + its ANOs), only the current ANO, all moves, all ANOs, or everything. For the current move there is also **"Salvar em outro personagem (Runtime)"** (transplant, recipe G).

Where:
1. **Salvar em pasta separada (Tap Battle/TBRE/Chars/)** — *recommended*. Copies the whole character (DAC, BIN, CNV, sheets, sounds) to `TBRE/Chars/charNN/` and applies the save there, ready to use; the original is not touched. If the folder already exists the app asks: replace (merge the two), delete the existing one, or save with another name.
2. **Salvar no Runtime (Tap Battle/assets/charNN/)** — overwrites the `.txt` files of the character itself (the binaries stay intact).
3. **Salvar em outro personagem (Runtime)** — only for the current move; saves directly into the target character.

Backups/safety copies go to `Tap Battle/TBRE/Backups/` (e.g. `AAAAMMDD_HHMMSS_charNN_transplante/`).

To test in the actual game: the game reads the character folder under `assets/charNN/`. How a "separate folder" copy reaches the game depends on the user's setup — ask how they run the game, and never advise overwriting the only copy of the original. After saving or replacing `.txt` files by hand, tap `↻` in the title bar (reloads the folders without restarting the app) and select the character in `CHAR` to confirm that the result loads.

The save step also checks the game limits (boxes per frame, links, time…) and warns about anything that would close the game.
