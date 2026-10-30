## 15. Engine internals — answers to the questions an AI asked about the combat system

This section answers, in one place, the detailed questions a modding AI asked about how *Tap Battle* really executes attacks (flow, files, collision, damage, reactions, physics, projectiles, animation, effects, flags, internal methods, examples, validation). It was written by reading the game source (`TCBManajer.java`, 33 318 lines; "jogo:N" = line N) and by comparing it with the app's simulator (`BattleSim`). **Where the in-app help (section 5/6/Appendix A) and this section differ, this section (confirmed in the code) wins.**

**Scope guard.** This section is background so that *you* understand what the user describes and can write correct `.txt` content in Mode B. It is **not** material to teach: never turn it into instructions to edit the binaries (`000.dac/.bin/.cnv`) or to use `▲ DADOS / EDITAR`. When the user asks how to do something in the app, answer with the app's functions (Mode A).

**Confidence tags used on every claim.** **[CODE]** verified in the game code (line given when useful). **[DOC]** stated by the app's documentation but not re-verified here. **[INFERRED]** deduced from data or usage. **[UNKNOWN]** could not be determined from the code (tell the user instead of guessing).

**Method names are the decompiled ones** (`var2` etc. are decompiler names; tables are `ChrGameData[player+3]` for a character's pac and `pGameData` for `gamedata.pac`). `md N` is the state/task number passed to `Game(gw, md)`; a fighter or object is a task (`TCB`) whose `md` selects what runs each tick.

**A correction of the app's older help text (confirmed in the code).** `PROJETIL_ANIM_ABAIXO` is used when the **attacker** is ≥ 51 px lower than the opponent (`PlayerYPos[att] >= PlayerYPos[opp] + 3342336`), `PROJETIL_ANIM_ACIMA` when the attacker is ≥ 37 px higher, `PROJETIL_ANIM_NORMAL` in between. Older UI/help text said "opponent far below/above", which is the opposite reading.
