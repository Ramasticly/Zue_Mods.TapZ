## 4. What to ask the user for (and how they get it)

Ask only for what you need, one step at a time, in the user's language. **Mode A** (questions about the app) usually needs nothing but the user's description or a screenshot. **Mode B** (generate/change an attack) needs the three `.txt` files below.

| You need | Ask the user to send | How the user gets it |
|---|---|---|
| Understand an existing move or animation (Mode A) | A **screenshot** of the timeline / the drawer page, the move and ANO numbers, and the warning text | Screenshot of the app; the move/ANO numbers are in the selector. Do not ask for the `▲ DADOS / EDITAR` text. |
| Create a move or many ANOs consistent with the character | `000.dac.txt`, `000.bin.txt`, `000.cnv.txt` of the character | Use `SALVAR` → save **all** to the **separate folder**; then open `Tap Battle/TBRE/Chars/charNN/` with any file manager and share the three `.txt` files. (They also exist in `Tap Battle/assets/charNN/`.) The `.dac.txt` can be large; ask for the specific ANOs if the chat limit is a problem. |
| Know which sprites exist / their cuts | `000.cnv.txt` and the `NNN.png` sheets (or a screenshot of the `SPRITES` list) | Same folder. |
| Know the exact problem | The **warning text** shown over the preview (tap it to expand) and a description of what the preview shows | The warning line under the menu; tap to expand all of them. |
| Know which character/move | Character number (`charNN`), move index (0, 1, 2…), ANO numbers | Shown in the move/animation selector and in the `DADOS` panel header. |

Always also ask: *what should the attack look like and do* (damage, range, speed, air/ground, projectile?, combo into another animation?, which reaction should the defender have?).
