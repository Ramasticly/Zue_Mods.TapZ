# gamedata.md — HDATs do `gamedata.pac`

Guia de cada HDAT do `gamedata` (pasta `gamedata/`, arquivo `000.dac.txt`):
para que serve, onde o jogo lê, o que cada campo muda no gameplay e o que já
se sabe dos campos sem nome. Tudo foi conferido no `TCBManajer.java` (que
chama o gamedata de `pGameData`).

O gamedata vale para **o jogo inteiro**: IA da CPU, progressão do arcade,
combos, reações de dano no chão, formações, cartas, loja, esferas do dragão,
tickets e o save inicial. O que é de cada personagem fica no `charNN`
(ver `binChar.md`).

## Antes de editar

- **Unidades.** A tela do jogo tem 480 px de largura e o chão fica em
  y = 240. "Frame" é um quadro da lógica do jogo.
- **ANO** é o número de uma animação no DAC de um personagem.
- **`CPULevel`** é a força da CPU (0 = mais fraca, 7 = mais forte). Ela vem
  do HDAT 4 (arcade) e escolhe a coluna de vários HDATs.
- **Dificuldade**: 0 = fácil, 1 = normal, 2 = difícil.
- **Personagem N** = o número do personagem no jogo. Onde o campo diz
  "personagem + 1", 0 é vazio.
- **Afinidade**: 0 = bem, 1 = mal, 2 = neutro (a mesma do `$ID_ATRIBUTO` do
  personagem e do `$AFINIDADE` das cartas).
- Tabelas de byte vão de 0 a 255; de short, de -32768 a 32767.
- **`$RAW`**: HDATs sem formato detalhado saem como bytes crus. Os que o jogo
  nunca lê estão marcados "sem leitura".
- **Arcade (modo 0)**: o modo de lutas em sequência com 8 estágios. A
  **missão inicial** é a primeira campanha, antes de concluí-la; nela vários
  valores são fixos.

## Índice

| HDAT | Tag no `.txt` | Resumo |
|---|---|---|
| 0 | `$BONUS_EXP_DESEMPENHO` | EXP ganho no fim do estágio conforme o desempenho |
| 1 | `$RAW` | Sem leitura |
| 2 | `$DISTANCIAS_IA` | Distâncias "perto/médio" da CPU e alcance de defesa contra projéteis |
| 3 | `$IA_COMPORTAMENTO` | Chances (%) de a CPU defender, cancelar, contra-atacar, iniciar eventos |
| 4 | `$PROGRESSAO_ARCADE` | Nível e força da CPU e EXP extra por estágio |
| 5 | `$MULTIPLICADOR_COMBO` | Escala de dano e de pontos conforme o combo |
| 6 | `$RAW` | Sem leitura |
| 7 | `$TXT` | Textos desenhados com a fonte de sprite ("READY", "KO!", números…) |
| 8 | `$MAPA_FONTE` | Caractere → desenho da fonte de sprite |
| 9 | `$CONFIG_CENA` | Configuração das cenas da história |
| 10–39 | `$REACAO_DANO_GD_N` | Reações de dano **no chão** (as mais comuns) |
| 40 | `$RAW` | Sem leitura (vazio) |
| 41 | `$BP_POR_ESTAGIO` | BP ganho por estágio do arcade |
| 42 | `$BP_CHEFE` | BP ganho nos chefes do modo 1 |
| 43 | `$EXP_NEXT_LEVEL` | EXP para subir de nível |
| 44 | `$TXT` | Texto sem leitura |
| 45–49 | `$RAW` | Sem leitura |
| 50 | `$FORMACAO` | Formações de cartas: requisitos e qual tabela de efeito usar |
| 51–135 | `$EFEITO_FORMACAO_N` | O efeito de cada formação |
| 136 | `$LOJA_CARTAS` | Cartas à venda / na troca e o preço |
| 137, 138 | `$TXT` | Nome e descrição de cada formação |
| 139–144 | `$RAW` | Sem leitura |
| 145 | `$PERSONAGENS_EXCLUIDOS_CPU` | Sem efeito (a função que lê nunca é chamada) |
| 146 | `$RESULTADO_DISPUTA_KI` | Sequência de ícones para aceitar a disputa de poder |
| 147 | `$DIFICULDADE_DISPUTA` | Força da CPU nas disputas de ki e de poder |
| 148 | `$PADROES_BONUS_PUSH` | Quais botões da disputa de ki são bônus |
| 149 | `$TIERS_PODER_DECK` | Faixas de poder do deck |
| 150 | `$SCORE_EVENTOS_CUTSCENE` | Pontos dos eventos especiais |
| 151, 152 | `$RAW` | Sem leitura |
| 153 | `$ESFERAS_DRAGAO` | Condições para ganhar cada esfera do dragão |
| 154 | `$TXT` | Texto das missões e das condições das esferas |
| 155 | `$BONUS_AFINIDADE` | Bônus por carta da mesma afinidade do personagem |
| 156 | `$SEQUENCIA_PUSH` | Botões que aparecem na disputa de ki |
| 157 | `$DECKS_CPU` | Decks de cartas da CPU |
| 158 | `$DEF_CHARS` | Personagens liberados num save novo |
| 159 | `$RAW` | Sem leitura |
| 160 | `$CARTAS_INICIAIS` | Cartas de um save novo |
| 161 | `$SOUND` | Músicas de cada situação |
| 162 | `$TICKETS_INICIAIS` | Tickets de um save novo |
| 163 | `$INIT_CHAR_VALUES` | Nível, EXP e cartas de cada personagem num save novo |
| 164 | `$ESFERAS_INICIAIS` | Esferas do dragão de um save novo |
| 165 | `$TICKETS_POR_PARTIDA` | Tickets ganhos por luta |
| 166 | `$FLAGS_VIDA_ESPECIAL` | Jogador imortal / CPU com 1 de HP |
| 167 | `$MODO_DEBUG` | Liga o modo debug |
| 168, 169 | `$RAW` | Sem leitura (vazios) |
| 170–250 | `$CARTA_N` | As cartas (carta N = HDAT 170 + N) |
| 251–270 | `$RAW` | Sem leitura |
| 271–279 | `$TXT` | Textos dos idiomas 1–3 (não existem no `gamedata.pac` original) |

---

## HDAT 0 — `$BONUS_EXP_DESEMPENHO`

Tabela de short, 4 colunas × 10 linhas. Vale no fim de cada estágio do
arcade, depois da missão inicial.

O jogo calcula uma nota de desempenho com a vida que sobrou e o tempo gasto
(mais vida e menos tempo = nota maior). Depois procura da `$LINHA_9` para a
`$LINHA_0` a primeira linha em que a nota ≥ coluna 0 × 100.

| Coluna | O que faz |
|---|---|
| 0 | Nota mínima (× 100) para esta linha valer |
| 1 / 2 / 3 | EXP ganho no fácil / normal / difícil |

Ao EXP ainda se soma o `$EXP_EXTRA_*` do HDAT 4. Na missão inicial esta
tabela não vale: o EXP é o `$EXP_ESTAGIO_INICIAL` do HDAT 4.

## HDAT 2 — `$DISTANCIAS_IA`

Tabela de byte, 2 colunas: 0 = com o alvo no chão, 1 = no ar. Distâncias em px.

| Campo | O que faz |
|---|---|
| `$GUARDA_PROJETIL` | A CPU tenta defender projéteis que chegam a menos desta distância (só a coluna 0 é lida) |
| `$PERTO` | Até esta distância a situação é "perto" |
| `$MEDIO` | Até esta distância é "médio"; acima, "longe" |

"Perto/médio/longe" escolhe a linha da tabela de decisão da CPU (HDAT 0 de
cada personagem). Aumentar os valores faz a CPU agir como se estivesse mais
perto.

## HDAT 3 — `$IA_COMPORTAMENTO`

Tabela de byte, 8 colunas = `CPULevel` 0–7. Cada valor é uma **chance em %**.

| Campo | O que faz |
|---|---|
| `$L01_CANCEL_GUARD_CONTRA_ATAQUE` | Chance de a CPU contra-atacar saindo da defesa |
| `$L02_CANCEL_COMBO` | Chance de a CPU cancelar um especial em outro (continuar o combo) |
| `$L03_GUARD` | Chance de a CPU defender um golpe |
| `$L04_BONUS_TEMPO_VITORIA` | Fora do arcade (e do modo 5): bônus de tempo quando você vence = tempo restante × valor ÷ 100 |
| `$L18_INICIAR_PUSH` | Chance de a CPU aceitar a disputa de poder que você iniciou |
| `$L19_CHANCE_RUSH` | Chance de a CPU iniciar um rush (o choque que leva à batalha de cartas ou à disputa de ki) |
| Os outros (`L00`, `L05`–`L17`) | Não são lidos |

## HDAT 4 — `$PROGRESSAO_ARCADE`

Tabela de byte, 8 colunas = estágio 0–7 (do 8º estágio em diante vale a
coluna 7).

| Campo | O que faz |
|---|---|
| `$NIVEL_IA_FACIL` / `_NORMAL` / `_DIFICIL` | `CPULevel` do oponente em cada estágio |
| `$NIVEL_EXTRA_FACIL` / `_NORMAL` / `_DIFICIL` | Nível do oponente = nível do seu personagem + este valor |
| `$EXP_EXTRA_FACIL` / `_NORMAL` / `_DIFICIL` | EXP somado ao do HDAT 0 no fim do estágio |
| `$NIVEL_FIXO_INICIAL` | Missão inicial: nível do oponente (fixo) |
| `$NIVEL_IA_FIXO_INICIAL` | Missão inicial: `CPULevel` do oponente |
| `$EXP_ESTAGIO_INICIAL` | Missão inicial: EXP ganho no estágio |

## HDAT 5 — `$MULTIPLICADOR_COMBO`

Tabela de short, 14 colunas: coluna = tamanho do combo − 2 (combo de 2 hits
= coluna 0; de 15 hits ou mais = coluna 13).

| Campo | O que faz |
|---|---|
| `$PONTOS_POR_COMBO` | % dos pontos de golpe especial (HDAT 5 do personagem) conforme o combo |
| `$VALOR_POR_COMBO` | **% do dano** de cada hit conforme o combo, ou seja, a escala de dano (100 = dano cheio). Se a conta der 0, o hit tira 1 |

Exemplo: `$VALOR_POR_COMBO: [100, 100, 100, 100, 75, …]` → do 6º hit em
diante o dano cai para 75%. Valores maiores deixam os combos longos mais
fortes.

## HDAT 7 — `$TXT` (textos da fonte de sprite)

Strings desenhadas com a fonte de sprite do jogo (`CREATE_STRING`): "LV",
"BATTLE", "FINAL ROUND", "READY", "FIGHT!!", "KO!", "TIME OVER", "YOU WIN",
os números (`$T_13` = "0" … `$T_22` = "9") etc. O jogo pede a string pelo
número `$T_N`. As cenas da história também tiram textos daqui.

Cada caractere passa pelo HDAT 8. Use só caracteres que a fonte tem (ASCII);
espaços são pulados.

## HDAT 8 — `$MAPA_FONTE`

128 valores: a coluna é o código do caractere (ASCII) e o valor é o desenho
(padrão) da fonte de sprite. Ex.: coluna 48 (`'0'`) → 0, coluna 65 (`'A'`) →
10. Para trocar um caractere da fonte, aponte para outro padrão.

## HDAT 9 — `$CONFIG_CENA`

Tabela de short. Cada linha é uma cena da história/demonstração (modo 808).

| Coluna | O que faz |
|---|---|
| 0 | Pack de objetos da cena: carrega o número valor + 1 no slot 5 |
| 1 | Cenário: −1 = nenhum/mantém; 0 = o cenário atual + 1; outro = esse cenário |
| 2 | Música: número + 1 (−1 = não troca). As músicas 0, 2 e 8 tocam em loop |
| 3 / 4 | Animação inicial do personagem 1 / 2 na cena |
| 5 | Parâmetro da cena usado no modo 810 (não confirmado) |
| 6 em diante | Pares X/Y de posição dos objetos de efeito da cena (modo 816) |

## HDAT 10 a 39 — `$REACAO_DANO_GD_N`

Reações de dano **no chão**, as que a maioria dos golpes usa. O código de
dano do hit no DAC escolhe a reação: código < 30 → HDAT 10 + código (estas);
código 30 ou mais → reação aérea/agarre do personagem atacante
(`binChar.md`, HDAT 14–33).

Os campos são **os mesmos** do `$REACAO_DANO_N` do personagem: `$DUR_C`/
`$DUR_A` (duração no chão/ar), `$ANO` (animação de quem apanhou; negativo =
virada), física (`$XSPEED` … `$YACCEL_A`, 10 = 1 px por frame, −1 = mantém,
−2 = metade), `$COMP` (o que acontece ao tocar o chão/parede), `$WALL` e
`$VOZ`. A explicação completa de cada campo e dos valores de `$COMP` está em
`binChar.md`.

Reações com papel fixo no código:

| Reação | Uso |
|---|---|
| 2 | Reação "padrão": usada ao acabar uma duração 255 e ao terminar um agarre |
| 12 | Quique no chão (com `$WALL` 2/3) |
| 13 | Queda depois de uma reação que termina no ar |
| 29 | Substitui a reação 2 quando a vida acaba |

As animações (`$ANO`) são do DAC de **quem apanha**, então todos os
personagens precisam ter as animações usadas aqui.

## HDAT 41 — `$BP_POR_ESTAGIO`

Linha = estágio do arcade, coluna = dificuldade. BP ganho no fim do estágio =
valor × 10 (no máximo 999000 por estágio). Vale depois da missão inicial.

O original só tem 6 linhas, então os estágios 6 e 7 leem o HDAT seguinte.
Para controlar esses estágios, adicione as linhas 6 e 7.

## HDAT 42 — `$BP_CHEFE`

No modo 1, a cada 5 estágios (o chefe): BP = `$LINHA_(estágio ÷ 5)` × 10.

## HDAT 43 — `$EXP_NEXT_LEVEL`

EXP para subir de nível. Cada `$LVL_N` vale para os níveis N a N+4 (o jogo lê
a linha nível ÷ 5). O EXP que sobra passa para o próximo nível. O nível
máximo é 99.

## HDAT 50 — `$FORMACAO`

Tabela de byte, 15 colunas. Cada `$LINHA_N` é uma formação (a `$LINHA_0` não
é usada). O nome e a descrição da formação N estão na linha N dos HDATs 137
e 138.

| Campo | Coluna | O que faz |
|---|---|---|
| `$TABELA_EFEITO` | 0 | **Número do HDAT** com o efeito da formação (51–135). 0 = formação desligada |
| `$TIPO_REQUISITO` | 1 | 0 = por afinidade (`$REQ_AFINIDADE`); outro = por cartas específicas (`$CARTAS_EXIGIDAS`) |
| `$REQ_AFINIDADE` | 2–4 | Mínimo de cartas de afinidade [bem, mal, neutro] no deck |
| `$CARTAS_EXIGIDAS` | 5–12 | Até 8 números de carta, que precisam estar todas no deck (0 = fim da lista). Uma carta já usada por outra formação de cartas não conta de novo |
| `$DESCONHECIDO` | 13–14 | Não são lidos |

As formações são testadas em ordem. Das formações **por afinidade**, só a
primeira que bate vale. Por isso as mais exigentes (8 cartas) vêm antes das
mais fáceis (4 cartas).

No `gamedata` original as linhas 1–61 usam os HDATs 51–111 e as linhas 62–84
estão desligadas (`$TABELA_EFEITO: 0`). Os HDATs 112–135 não são usados por
nenhuma formação: são slots livres para formações novas (preencha uma linha
desligada, aponte para um deles e escreva o nome/descrição nos HDATs 137/138).

## HDAT 51 a 135 — `$EFEITO_FORMACAO_N`

O efeito de uma formação: 1 coluna × 22 linhas, das quais o jogo lê 6. Os
efeitos se somam aos das cartas.

| Campo | Linha | O que faz |
|---|---|---|
| `$ATAQUE` | 3 | Soma ao ATK do personagem (% do dano), igual ao `$ATK` de uma carta |
| `$DEFESA` | 6 | Soma à DEF (% de redução do dano recebido) |
| `$BONUS_KI` | 7 | Ki no início da luta. O `.txt` mostra × 10 (o valor real de ki); limitado ao ki máximo |
| `$BONUS_BARRA_KI` | 8 | Barras extras de ki máximo (1 = +100) |
| `$FURIA` | 16 | Se a soma de cartas e formações for diferente de 0, a luta começa com a **fúria permanente** |
| `$RUSH_RED` | 20 | Sem efeito (o jogo nunca usa) |
| `$LINHA_N` (outras) | — | Não são lidas |

O número no nome (`$EFEITO_FORMACAO_N`) é só uma etiqueta (HDAT − 51). Quem
liga a formação ao efeito é o `$TABELA_EFEITO` do HDAT 50.

O HDAT 135 tem só 17 linhas (0–16) no original. Ele tem todos os campos que
o jogo usa (`$RUSH_RED`, a linha 20, não faz nada), então também funciona como
efeito.

**HDATs 53 e 54.** No `gamedata` original eles são os efeitos das formações
3 e 4, e o extractor os mostra assim. O jogo também lê o 53 como tabela de
prêmio (fim de estágio do arcade), mas descarta o resultado. E lê o 54 como
tabela de transformação de personagem num modo que nunca é chamado. Por isso
só o papel de efeito de formação tem efeito no jogo. Num gamedata em que eles
não tenham 1 × 22, saem como `$PROB_RECOMPENSA` / `$TRANSFORMACAO`.

## HDAT 136 — `$LOJA_CARTAS`

Uma linha por carta (`$BUY_CARTA_N` = carta N; o jogo lê as cartas 1 a 89).

| Campo | O que faz |
|---|---|
| `$VISIVEL_MODO_A` | YES = a carta aparece na lista de **troca** |
| `$VISIVEL_MODO_B` | YES = a carta aparece na **loja** para comprar |
| `$PRECO_TICKETS` | Preço em tickets na loja. A compra sobe 1 nível da carta (até o 10) |

## HDAT 137, 138 e 154 — textos (`$TXT`)

| HDAT | Conteúdo |
|---|---|
| 137 | Nome de cada formação (linha = formação do HDAT 50) |
| 138 | Descrição de cada formação |
| 154 | Texto das missões e das condições das esferas do dragão (linha = rodada × 7 + esfera, a mesma numeração do HDAT 153; as linhas 0–6 são as missões iniciais) |

Formato do texto: `"..."` grava em UTF-8 (o que o jogo lê), `SJIS"..."`
mantém o Shift_JIS original e `\xHH` é um byte cru. `_` vira quebra de linha
na tela.

## HDAT 145 — `$PERSONAGENS_EXCLUIDOS_CPU`

Dois números de personagem que, pelo nome da função, não deveriam aparecer
como oponente da CPU. A função que lê este HDAT nunca é chamada: hoje **não
tem efeito**.

## HDAT 146 — `$RESULTADO_DISPUTA_KI`

Quando a **CPU** inicia a disputa de poder, você precisa tocar uma sequência
de ícones para aceitar. São 4 padrões, um sorteado a cada vez. O nome da tag
engana: é a sequência de ícones, não um resultado.

| Campo | O que faz |
|---|---|
| `$ENTRADA_N` | Um padrão (o jogo sorteia entre 0 e 3) |
| `$NUM_ICONES` | Quantos ícones tocar |
| `$POSICOES` | X/Y de cada ícone (área de toque de 64 × 64 px), **na ordem** em que devem ser tocados |
| `$DESCONHECIDO` | Não é lido |

Tocar fora de ordem ou passar de 110 frames cancela a disputa.

## HDAT 147 — `$DIFICULDADE_DISPUTA`

Tabela de byte, 10 colunas = `CPULevel` 0–9.

| Campo | Onde | O que faz |
|---|---|---|
| `$L0_ESPERA_PROXIMO_BOTAO` | disputa de ki | A CPU marca 1 ponto a cada (valor + 0–5) frames: menor = CPU mais rápida |
| `$L3_INTERVALO_INPUT_CPU` | disputa de poder | A cada tantos frames a CPU empurra |
| `$L4_FORCA_MIN_CPU` / `$L5_FORCA_MAX_CPU` | disputa de poder | Força de cada empurrão da CPU: sorteada entre o mínimo e o máximo |
| `L1`, `L2`, `L6` | — | Não são lidas |

## HDAT 148 — `$PADROES_BONUS_PUSH`

Na disputa de ki, alguns botões são **bônus**. Coluna = `CPULevel` (no modo 8,
coluna 6). A cada leva de botões, o jogo sorteia uma linha inicial e avança
uma linha por botão: se o valor for maior que 0, aquele botão é bônus.

## HDAT 149 — `$TIERS_PODER_DECK`

Faixas de poder do deck, só depois da missão inicial ou no modo 8. O poder =
ATK das cartas × 1000 + `$ATQ` das cartas + bônus de afinidade + ATK das
formações × 1000. O tier é a maior faixa com limiar × 1000 ≤ poder.

Com tier 1 ou mais, a cena do golpe Extreme mostra o painel de bônus e ganha
a sequência extra. O tier também entra no fim do rush de cartas e na disputa
de ki.

## HDAT 150 — `$SCORE_EVENTOS_CUTSCENE`

Pontos (short) dados nos eventos especiais.

| Linha | Evento |
|---|---|
| 0 | Vencer o rush de cartas |
| 2 | Cada hit da cena do golpe Extreme |
| 4 | Fim da cena do Extreme |
| 6 | Vencer a disputa de poder |
| 1, 3, 5 | Não são lidas |

## HDAT 153 — `$ESFERAS_DRAGAO`

Condições para ganhar as esferas do dragão. São 11 rodadas (`$ROUND_N`: a
coleção 0 a 10) × 7 esferas. A checagem acontece ao terminar o arcade (último
estágio) ou no modo 8, para as esferas que ainda faltam na rodada atual. Na
missão inicial as esferas vêm uma por estágio, sem esta tabela.

| Campo | Coluna | Condição (0 = não exige) |
|---|---|---|
| `$PLAYER_TYPE` | 0 | Jogar com o personagem valor − 1 (**sempre exigido**) |
| `$DIFICULDADE` | 1 | Dificuldade = valor − 1 |
| `$SCORE_MIN` | 2 | Placar ≥ valor × 100 |
| `$COMBO_MIN` | 3 | Combo máximo ≥ valor |
| `$SEM_DERROTA` | 4 | Não ter perdido nenhum estágio |
| `$TEMPO_LIMITE` | 5 | Terminar em menos de valor **segundos** |
| `$MODO_FLAG` | 8 | Diferente de 0 = só vale no modo 8 (contra outra pessoa); 0 = não vale no modo 8 |
| `$DESCONHECIDO_6`, `_7`, `_9`, `$NUM_ESFERA` | 6, 7, 9, 10 | Não são lidos |

O texto de cada condição está no HDAT 154. Juntar as 7 esferas passa para a
rodada seguinte.

## HDAT 155 — `$BONUS_AFINIDADE`

Valor somado ao `$ATQ` e ao `$DEF_EXIBICAO` (milésimos) de cada carta cuja
afinidade é a mesma do personagem. Exemplo: 200 com 8 cartas = +1600 = +1 de
ATK e de DEF.

## HDAT 156 — `$SEQUENCIA_PUSH`

Os botões que aparecem na disputa de ki. A cada leva, o jogo usa a linha
`CPULevel` × 8 + (0 a 5, sorteado); no modo 8, o nível 6. Só 6 das 8 linhas de
cada nível são usadas.

| Campo | O que faz |
|---|---|
| `$NUM_BOTOES` | Quantos botões nesta leva |
| `$POS_X` / `$POS_Y` | Posição do botão na tela |
| `$DELAY` | Frames até o botão aparecer |
| `$JANELA` | Tempo do botão na tela |

## HDAT 157 — `$DECKS_CPU`

Decks de cartas da CPU. Linha = sorteio de 0 a 15 + dificuldade × 16
(fácil 0–15, normal 16–31, difícil 32–47). Na missão inicial é a linha
48 + estágio.

| Campo | O que faz |
|---|---|
| `$CARTAS` | 8 números de carta (0 = vazio) |
| `$NIVEIS` | O nível de cada carta (0–10) |

## HDAT 158 — `$DEF_CHARS`

Usado ao criar um save novo. Cada `$CHAR` libera um personagem.

| Campo | O que faz |
|---|---|
| `$index` | Personagem liberado desde o início |
| `$isDef` | Diferente de 0 = também conta como comprado (só nas 13 primeiras linhas) |

## HDAT 160 — `$CARTAS_INICIAIS`

Cartas que um save novo recebe. `$ID` = número da carta; `$NIVEL` = quantas
vezes ela é recebida (1 = no nível 0, 2 = no nível 1, …).

## HDAT 161 — `$SOUND`

Música de cada situação. `$SND_N` = situação, `$id` = número da música (−1 =
nenhuma), `$loop` ≠ 0 = repete.

O jogo pede as linhas 0, 1, 2, 12, 18, 19, 23–26, 43–47 e 28–34. As linhas
28–34 são as músicas de batalha: o jogo alterna entre as 7.

## HDAT 162 — `$TICKETS_INICIAIS`

Tickets de um save novo.

## HDAT 163 — `$INIT_CHAR_VALUES`

Um `$CHAR` por personagem (na ordem dos números), aplicado ao criar um save
novo.

| Campo | O que faz |
|---|---|
| `$level` | Nível inicial (1 = o primeiro nível) |
| `$exp` | EXP inicial |
| `$card1` … `$card8` | As 8 cartas equipadas |

## HDAT 164 — `$ESFERAS_INICIAIS`

Esferas do dragão de um save novo: `$ROUND_N` = rodada, 7 colunas = esferas;
diferente de 0 = já vem com ela. Rodadas completas contam como concluídas.

## HDAT 165 — `$TICKETS_POR_PARTIDA`

Os nomes das tags enganam. O que cada campo faz de fato:

| Campo | O que faz |
|---|---|
| `$LIVRE` | Valores usados durante a missão inicial |
| `$MISSAO_0` / `_1` / `_2` | Valores no fácil / normal / difícil, depois da missão inicial |
| `$MISSAO_3` | Não é lido |
| `$DERROTA` (coluna 0) | Tickets ganhos na tela de resultado de cada luta |
| `$VITORIA` (coluna 1) | Tickets ganhos quando a carta de recompensa do arcade já está no nível máximo e vira tickets |

## HDAT 166 — `$FLAGS_VIDA_ESPECIAL`

| Campo | O que faz |
|---|---|
| `$IMUNIDADE_JOGADOR` | Diferente de 0 = o jogador 1 não perde vida |
| `$OPONENTE_1HP` | Diferente de 0 = o jogador 2 começa a luta com 1 de HP |

Úteis para testar golpes e cenas.

## HDAT 167 — `$MODO_DEBUG`

Diferente de 0 = liga o modo debug do jogo ao entrar no arcade.

## HDAT 170 a 250 — `$CARTA_N`

Carta N = HDAT 170 + N. Cada atributo é uma lista com um valor por **nível
da carta** (0 a 10). `$ATRIBUTOS: 28` marca o formato da tabela; não mude.

| Campo | O que faz |
|---|---|
| `$ATK` | Soma ao ATK do personagem (% do dano dos golpes) |
| `$ATQ` | ATK em milésimos (o número mostrado na carta). O total das cartas ÷ 1000 também soma ao ATK |
| `$DEF` | Soma à DEF (% de redução do dano recebido) |
| `$DEF_EXIBICAO` | DEF em milésimos (o número mostrado). O total ÷ 1000 também soma à DEF |
| `$SYNCHRO` | Ki no início da luta (× 10; somado de todas as cartas e formações; limitado ao ki máximo) |
| `$SYNCHRO_ESTOQUE` | Barras extras de ki máximo (1 = +100) |
| `$ANGRY` | Se a soma for diferente de 0, a luta começa com **fúria permanente** |
| `$RUSH_RED` | Sem efeito (o jogo nunca usa) |
| `$AFINIDADE` | 0 = bem, 1 = mal, 2 = neutro: conta para as formações por afinidade e para o bônus do HDAT 155 |
| `$CUSTO` | Custo para equipar. A soma das cartas não pode passar do custo do personagem |
| `$ATR_NN` | Atributos que o jogo não lê |

O "poder" (BP) mostrado na carta = `$ATQ` + `$DEF_EXIBICAO` (+ 2 × bônus de
afinidade) + (`$ATK` + `$DEF`) × 1000, com mínimo de 1000.

Ganhar ou comprar uma carta que já tem sobe o nível dela em 1.

Exemplo: com `$ATK: [0, …]` e `$ATQ: [700, 800, …]`, oito cartas no nível 1
somam 6400 milésimos = +6 de ATK.
