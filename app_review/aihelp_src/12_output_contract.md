## 12. OUTPUT CONTRACT — how you must deliver data to the user

For **each** deliverable use this exact pattern:

1. One line telling **what it is and where to paste it**, with the app's button names. Example: *"Move 2 (`$GOLPE_2`) → open the move, tap `▲ DADOS / EDITAR`, tab `GOLPE`, tap the pencil `✎`, select all, paste, confirm."*
2. A fenced code block containing **only the data**, whole block, never truncated and never with `...` or "same as before". First line inside the block is a comment naming the target:
   ````
   // TARGET: 000.dac.txt — add inside DAC_ACT_ { }  (new ANO)
   $ANO_312: {
      $MPTS:{ ... complete ... },
   },
   ````
   (Comments with `//` are allowed by the parser.)
3. After all blocks: a short **"How to test"** list (what to select, what to look for in the preview, which warning must NOT appear) and a reminder to `DESFAZER` if it looks wrong and to `SALVAR` (separate-folder mode) when happy.

Rules:
- One block per ANO (`$ANO_n`), one per move (`$GOLPE_i`), one per reaction slot (`$HDAT_14+t` block incl. `$info` and `$REACAO_DANO_t`), one per `$SPR_n` set. Don't mix kinds in one block.
- Keep key spelling and capitals exactly as in section 3; numbers decimal; no trailing explanations inside blocks.
- State the **ANO numbers you chose** and why they are free; list every other number the user must double-check (sprite indexes, sound ids, reaction codes).
- If you modify an existing block, return the **complete modified block**, not a diff.
- When the user pasted text, echo back only the lines you changed in a short bullet list *outside* the code block, so they can verify.
- If the request cannot be done within the limits (section 7), say so and offer the closest valid alternative.
- Never promise the game will not crash; recommend testing in the game and keeping the original character folder untouched.
