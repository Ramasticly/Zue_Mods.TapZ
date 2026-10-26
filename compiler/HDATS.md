# HDATs de `gamedata.pac` e `charNN.pac`

Mapa de cada HDAT lido pelo `TCBManajer.java` (via `piGameData` / `piGameDataPos` /
`piGameDataXSize` / `piGameDataYSize`), o que ele faz no jogo e como o Compiler
o extrai. Levantado varrendo **todas** as 1.906 leituras do código, inclusive
as que usam índice de HDAT em variável.

## Convenções

- `GameData.binCnv` transforma cada byte em um `short` sem sinal (`piGameData[i] = byte & 255`).
- **Tabela de byte:** `piGameData[Pos[h] + linha * XSize + coluna]`.
- **Tabela de short (LE):** `piGameData[Pos[h] + linha * 2 * XSize + coluna * 2]` (+1 = byte alto).
  Quando o código escreve `XSize * N + k*2` com N par (cartas, HDAT 5/150), é o mesmo layout de short
  com linha = N / 2.
- `pGameData` = `gamedata.pac`. `ChrGameData[3]` / `[4]` = char do jogador 1 / 2
  (aparece como `ChrGameData[var + 3]`, `[1 - var + 3]` para o oponente, `[pID + 3]`).
- **Extractor** = tag que o `BinExtractor` escreve. Todo HDAT passa pela
  verificação de ida e volta: se o formato detalhado não reproduz os bytes,
  sai `$RAW`.

## Índices de HDAT em variável

| Expressão no código | O que é | HDATs |
|---|---|---|
| `DamageType[p] + 10` (`pGameData`) | reação de dano no chão, modo 876 — `DamageType` < 30 | 10–39 |
| `DamageType[p] + 14` (`ChrGameData[atacante+3]`) | reação aérea/agarre, modo 877 — código de reação ≥ 30 → `DamageType = código - 30` | 14–33 (`$COMP` acima de 49 leria os HDATs 34+ como dano) |
| `PlayerCard[p][i] + 170`, `iCardBuf[i] + 170`, `var + 170` (`GetCardNo`) | tabela da carta N | 170–250 |
| `Pos[50] + formação * XSize + 0` → `Pos[esse valor]` | efeito da formação: a coluna 0 do HDAT 50 **é o índice do HDAT** de efeito | 51–52, 55–135 (os únicos sem leitura direta) |
| `GameData_TextPage[iLanguage * 3 + n]` = `{137, 138, 154, 271…279}` | páginas de texto do gamedata (`GDTString`) | 137/138/154 (idioma 0), 271–279 (idiomas 1–3) |
| `CharData_TextPage[iLanguage]` = `{2, 43, 44, 45}` | textos do char (`CharGDTString`) | char 2, 43, 44, 45 |
| `pTextData.piGameDataPos[iLanguage]` (`TextGDTString`) | text00: **um HDAT por idioma** | text00 0..3 |
| `ChrGameData[s + 6]`, HDAT 0, linha `CardData_TextPage[iLanguage*2 + 0/1]` | nome/descrição da carta (`CardGDTString`) | cardNNN HDAT 0, linhas 0–7 |
| `Pos[var10 + 100]` (modo 839) | menu genérico do motor. O único criador (modo 755) **nunca é chamado** — código morto (`var10 = 49` → leria o HDAT 149) | — |

`iLanguage = gw.iLocale`, e `dragonballtap.getLocale()` força 0: hoje só o idioma 0 é usado.

## gamedata.pac (`pGameData`)

| HDAT | Função (onde é lido) | Layout | Extractor |
|---|---|---|---|
| 0 | Bônus de EXP por desempenho no fim do estágio (modo 745): varre linhas 9→0, primeira cuja col 0 é alcançada; EXP = col (dificuldade+1) | short, 10 × 4 | `$BONUS_EXP_DESEMPENHO` **(novo)** |
| 2 | Distâncias da IA (modo 685): linha 0 col 0 = `iRangeBulletGuard`; linha 1/2 col 0/1 = perto/médio no chão/ar | byte | `$DISTANCIAS_IA` **(novo)** |
| 3 | Comportamento da CPU por `CPULevel` (linhas 1–4, 18, 19) | byte, 8 col | `$IA_COMPORTAMENTO` |
| 4 | Progressão do arcade por estágio: linhas 0–2 `CPULevel`, 3–5 nível somado, 6–8 EXP extra (por dificuldade), 9–11 idem antes da missão inicial | byte, col = estágio | `$PROGRESSAO_ARCADE` **(novo)** |
| 5 | Por combo (`ComboCount-2`, 0..13), `DamageChange`: linha 0 = % dos pontos do golpe especial; linha 1 = valor por hit | short, 14 × 2 | `$MULTIPLICADOR_COMBO` **(novo)** |
| 7 | Texto ASCII do `CREATE_STRING` | texto | `$TXT` |
| 8 | Mapa de fonte do `CREATE_STRING`: código do caractere → padrão do sprite | byte | `$MAPA_FONTE` **(novo)** |
| 9 | Configuração de cena (modo 808): col 0 → `LoadData[5]`, col 1 → cenário (-1 mantém, 0 = atual+1); outras colunas lidas em 808/809/816 | short | `$CONFIG_CENA` **(novo)** |
| 10–39 | Reação de dano no chão (modo 876), linha = `DamageIndex`, 14 bytes | byte, 14 col | `$REACAO_DANO_GD_N` |
| 41 | BP do estágio: linha = estágio (0–7), col = dificuldade, ×10 | byte | `$BP_POR_ESTAGIO` **(novo)** |
| 42 | BP do chefe (modo 1, estágio múltiplo de 5): linha = estágio/5, ×10 | byte | `$BP_CHEFE` **(novo)** |
| 43 | EXP para o próximo nível (linha = nível / 5) | byte | `$EXP_NEXT_LEVEL` |
| 44 | Texto (sem leitura no código) | texto | `$TXT` |
| 50 | Formações: col 0 = **HDAT do efeito** (0 = vazia), col 1 = tipo de requisito (0 = cartas específicas), cols 2–4 = mínimo de cartas de cada afinidade, cols 5–12 = cartas exigidas | byte, 15 col | `$FORMACAO` (`$CARD_ID` → `$TABELA_EFEITO`) |
| 51–52, 55–135 | Efeito de formação (via HDAT 50): só a col 0; linhas 3 ATK, 6 DEF, 7 Synchro (×10), 8 estoque de Synchro (×100), 16 fúria, 20 Rush Red | byte, 1 × 22 | `$EFEITO_FORMACAO_N` **(corrigido)** |
| 53 | Prêmio sorteado no fim da luta (modo 0): linha = prêmio, col = dificuldade×3 + categoria (combo ≥13 / ≥8 / perfect), soma 100 | byte, 9 col | `$PROB_RECOMPENSA` **(novo — antes tratado como efeito de formação)** |
| 54 | Transformação (modo 990): linha = `PlayerType`, col 0 = novo `PlayerType` | byte | `$TRANSFORMACAO` **(novo — idem)** |
| 136 | Loja de cartas: col 0/1 visibilidade, col 2 preço | byte | `$LOJA_CARTAS` |
| 137, 138, 154 | Textos do idioma 0: nome / descrição da formação (linha = formação), textos de estágio | texto | `$TXT` |
| 145 | Chars excluídos da CPU (col 0/1) | byte | `$PERSONAGENS_EXCLUIDOS_CPU` |
| 146 | Resultado da disputa de ki (ícones/posições) | short | `$RESULTADO_DISPUTA_KI` |
| 147 | Dificuldade da disputa por `CPULevel` (linhas 0, 3, 4, 5) | byte, 8 col | `$DIFICULDADE_DISPUTA` |
| 148 | Padrões de bônus do push | byte | `$PADROES_BONUS_PUSH` |
| 149 | Tiers de poder do deck (`GetCardAddAtack`, ×1000) | byte | `$TIERS_PODER_DECK` |
| 150 | Pontos de eventos de cutscene (linhas 0–3) | short | `$SCORE_EVENTOS_CUTSCENE` |
| 153 | Esferas do dragão (`CheckBall`) | short | `$ESFERAS_DRAGAO` |
| 155 | Bônus de afinidade (somado quando a afinidade da carta = char HDAT 3 [9]) | byte | `$BONUS_AFINIDADE` |
| 156 | Sequência de botões do push | short | `$SEQUENCIA_PUSH` |
| 157 | Decks da CPU: 8 cartas + 8 níveis | short | `$DECKS_CPU` |
| 158 | Chars liberados por padrão | byte | `$DEF_CHARS` |
| 160 | Cartas iniciais (id, nível) | byte | `$CARTAS_INICIAIS` |
| 161 | BGM (id+1, loop) — `GdtBGM` | byte | `$SOUND` |
| 162 | Tickets iniciais | byte | `$TICKETS_INICIAIS` |
| 163 | Valores iniciais por char (nível, EXP, 8 cartas) | byte | `$INIT_CHAR_VALUES` |
| 164 | Esferas iniciais | byte, 7 col | `$ESFERAS_INICIAIS` |
| 165 | Tickets por partida (derrota/vitória) | byte | `$TICKETS_POR_PARTIDA` |
| 166 | Flags de vida especial (linhas 0/1) | byte | `$FLAGS_VIDA_ESPECIAL` |
| 167 | Modo debug | byte | `$MODO_DEBUG` |
| 170–250 | Carta N = HDAT 170+N. Atributo K (par) × nível: 6 ATK, 12 DEF, 14 Synchro, 16 estoque, 32 fúria, 40 Rush Red, 44 afinidade, 48 custo, 50 ATK/1000, 52 DEF/1000 | short, 11 níveis × 28 atributos | `$CARTA_N` **(corrigido)** |
| 271–279 | Textos dos idiomas 1–3 (`GameData_TextPage`) | texto | `$TXT` **(novo)** |

**Sem nenhuma leitura no código:** 1, 6, 40, 45–49, 139–144, 151, 152, 159, 168, 169, 251–270
(continuam em `$RAW`).

## charNN.pac (`ChrGameData[3]` / `[4]`)

| HDAT | Função (onde é lido) | Layout | Extractor |
|---|---|---|---|
| 0 | Decisão da CPU (Game7 modo 952): linha = situação×4 + `CPURand%4`, col = `CPULevel` (chão 0–7 / ar 8–15); linha 44 = pool | byte, 16 × 45 | `$COMPORTAMENTO_IA` (desligado no extractor; com a verificação pode ser ligado) |
| 1 | Regeneração de ki (`DamageChange`): pares divisor/bônus | byte, 8 | `$REGENERACAO_KI` |
| 2 | Textos do idioma 0 (`CharData_TextPage[0]`) | texto | `$TEXTOS_DO_PERSONAGEM` |
| 3 | Status base: [0]/[1] velocidade (também recuo no empurrão), [3] largura (o jogo usa /2), [8] ki ×100, [9] afinidade, [11] HP, [12] ATK ×10, [14] DEF ×10, [17] custo | byte, 18 | `$STATUS_BASE_E_FISICA` |
| 4 | Atributos de golpe (24 shorts) | short, 24 col | `$ATRIBUTOS_DE_GOLPE` |
| 5 | Pontos por golpe especial (ANO 27–66) | short, 40 | `$PONTUACAO_POR_GOLPE` |
| 6 | Sequências da CPU: pares ação/tempo, [8] continua, [9] exige cancel | byte, 10 col | `$SEQUENCIAS_IA` (desligado; idem HDAT 0) |
| 7 | **Crescimento por nível** (linha = nível 0–98; stat = base do HDAT 3 + soma até o nível): col 0 HP, 1 ATK, 2 ki/Synchro, 3 DEF, 21 custo — `GetHP/GetAttack/GetSynchro/GetDeffence/GetCost` | byte, 22 col | `$CRESCIMENTO_POR_NIVEL` **(novo)** |
| 13 | Roteiro de cutscene (modo 996), 8 shorts com sinal | short, 8 col | `$ROTEIRO_CUTSCENE` |
| 14–33 | Reação aérea/agarre (modo 877), do char do **atacante** | byte, 14 col | `$REACAO_DANO_N` |
| 34 | Disputa de poder: linha 0 animação gatilho, linha 1 cutscene | byte | `$DISPUTA_DE_PODER` |
| 35 | Dano de eventos de cutscene (dano, animação) | short | `$DANO_DE_EVENTOS_CUTSCENE` |
| 36 | Vozes por situação (col 0) | byte | `$SISTEMA_DE_VOZES` |
| 37 | Multiplicador de dano na fúria (%) | byte | `$DANO_NO_ESTADO_DE_FURIA` |
| 38 | Carta de recompensa do arcade | short | `$CARTA_RECOMPENSA_ARCADE` |
| 39 | Cenário padrão | byte | `$CENARIO_PADRAO` |
| 40 | Campanha: linha 0 oponentes do arcade, 1 candidatos a chefe (`GetBOSS`), 3 oponentes antes da missão inicial | byte | `$PROGRESSAO_CAMPANHA` |
| 41 | Chars desbloqueados por dificuldade | byte | `$PERSONAGENS_DESBLOQUEAVEIS` |
| 42 | **Comandos de toque** (Game8 modo 38): linha = situação (+20 com Synchro ≥ 100), col 0 golpe, col 1 custo de Synchro ×100; linha+1 = continuação | short | `$COMANDOS_TOUCH` **(novo)** |
| 43–45 | Textos dos idiomas 1–3 (`CharData_TextPage`) | texto | `$TEXTOS_DO_PERSONAGEM` **(novo)** |

**Sem leitura:** 8–12. `backNN.pac` = `ChrGameData[1]` (HDAT 0 e 2, ver `hdatBack`).

## O que estava errado nos extractors (corrigido)

- **Cartas 170–250:** o formato antigo tinha 56 "constantes" de 11 em 11 bytes; as ímpares se sobrepõem às pares. Editar `$ATK[5]` = 1234 e recompilar gravava 34770 (o `$ATR_07` seguinte regravava o byte alto). Agora são só os 28 atributos pares; textos antigos continuam aceitos (os `$ATR_` ímpares são ignorados).
- **51–134:** o 53 (prêmios) e o 54 (transformação) eram extraídos como efeito de formação de 22 bytes. Num gamedata recompilado a partir desse texto, eles ficavam destruídos (só os bytes 3, 6, 7, 8 e 16 sobreviviam): quase todo char passava a se transformar no char 0, e prêmios sorteados saíam errados. Além disso, o efeito só escrevia 5 campos (perdia o Rush Red, linha 20, e as demais) e tratava a fúria (linha 16, um valor somado) como sim/não.
- **HDAT 50:** a col 0 se chamava `$CARD_ID`, mas é o índice do HDAT de efeito.
- **text00:** só o HDAT 0 era extraído/compilado; os outros idiomas sumiam do `.pac`.
- **Sem extractor (iam em `$RAW`):** gamedata 0, 2, 4, 5, 8, 9, 41, 42, 271–279; char 7, 42, 43–45.

> Pastas de `gamedata` extraídas pela versão antiga têm 53/54/efeitos já
> perdidos no texto: apague a pasta e extraia de novo do `.pac` original.
