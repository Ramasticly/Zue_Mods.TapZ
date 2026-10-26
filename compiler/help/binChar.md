# binChar.md — HDATs do personagem (`charNN.pac`)

Guia de cada HDAT do `000.bin.txt` de um personagem: para que serve, onde o
jogo lê, o que cada campo muda no gameplay e o que já se sabe dos campos ainda
sem nome. Tudo aqui foi conferido no `TCBManajer.java` (as linhas de código
citadas são desse arquivo).

O jogo carrega o `bin` do personagem do jogador 1 em `ChrGameData[3]` e o do
jogador 2 em `ChrGameData[4]`. Cada personagem tem o seu: mudar o `char01`
muda só aquele personagem.

## Antes de editar

- **Unidades.** A tela do jogo tem 480 px de largura e o chão fica em
  y = 240. "px" aqui é esse pixel. "Frame" é um quadro da lógica do jogo.
- **ANO** é o número de uma animação do DAC do próprio personagem
  (`000.dac.txt`, bloco `$ANO_N`). Quando um campo pede uma animação, coloque
  o número do `$ANO_N` correspondente.
- **Ki.** 100 de ki = 1 barra. O jogo chama o ki de `Synchro` internamente.
- **Tabela de byte e de short.** Cada HDAT é uma tabela com `$COL` colunas e
  `$ROW` linhas. Nas tabelas de byte cada valor vai de 0 a 255; nas de short,
  de -32768 a 32767. O tipo de cada HDAT está indicado abaixo.
- **`$RAW`.** HDATs sem formato detalhado (ou com o extractor desligado em
  `BinExtractor.SKIP_HDATS`) saem como bytes crus `$RAW:B[...]`. O layout de
  cada um também está descrito aqui, caso você queira editar à mão.
- **Campos `$DESCONHECIDO_*`.** O jogo não lê esses bytes (ou lê sem efeito
  visível). Pode deixar como estão.

## Índice

| HDAT | Tag no `.txt` | Resumo |
|---|---|---|
| 0 | `$RAW` | Decisões da CPU (qual sequência usar em cada situação) |
| 1 | `$REGENERACAO_KI` | Quanto ki se ganha ao bater, apanhar e defender |
| 2, 43–45 | `$TEXTOS_DO_PERSONAGEM` | Falas e mensagens (2 = idioma 0; 43–45 = idiomas 1–3) |
| 3 | `$STATUS_BASE_E_FISICA` | Corpo, velocidades, HP/ATK/DEF/ki base, afinidade, custo |
| 4 | `$ATRIBUTOS_DE_GOLPE` | Uma linha por golpe especial: animações, projétil, flags |
| 5 | `$PONTUACAO_POR_GOLPE` | Pontos ganhos ao acertar com cada animação especial |
| 6 | `$RAW` | Sequências de comandos da CPU (e dos comandos de toque) |
| 7 | `$CRESCIMENTO_POR_NIVEL` | Quanto HP/ATK/DEF/ki/custo sobe em cada nível |
| 8–12 | `$RAW` | Sem leitura no código |
| 13 | `$ROTEIRO_CUTSCENE` | Eventos visuais disparados pela hitbox tipo 20 do DAC |
| 14–33 | `$REACAO_DANO_N` | Reações aéreas e de agarre que este personagem aplica no oponente |
| 34 | `$DISPUTA_DE_PODER` | Animação que inicia a disputa de poder e a imagem final |
| 35 | `$DANO_DE_EVENTOS_CUTSCENE` | Dano dos eventos especiais (rush, Extreme, disputa) |
| 36 | `$SISTEMA_DE_VOZES` | Voz tocada em cada situação especial |
| 37 | `$DANO_NO_ESTADO_DE_FURIA` | % do dano enquanto a fúria está ativa |
| 38 | `$CARTA_RECOMPENSA_ARCADE` | Carta ganha ao terminar o arcade com este personagem |
| 39 | `$CENARIO_PADRAO` | Cenário da luta quando ele é o oponente |
| 40 | `$PROGRESSAO_CAMPANHA` | Oponentes do arcade e candidatos a chefe |
| 41 | `$PERSONAGENS_DESBLOQUEAVEIS` | Personagens liberados ao terminar o arcade |
| 42 | `$COMANDOS_TOUCH` | O que um toque no oponente faz em cada situação |

---

## Índice dos golpes (linha do HDAT 4)

Vários HDATs usam o "número do golpe" (`SpecialType` no código). É ele que
escolhe a linha `$GOLPE_N` do HDAT 4. Ele depende do comando e de onde o
personagem está:

| Golpe | Comando | Onde |
|---|---|---|
| 0 / 3 / 6 | Especial 1 | chão / pulo / voando |
| 1 / 4 / 7 | Especial 2 | chão / pulo / voando |
| 2 / 5 / 8 | Especial 3 | chão / pulo / voando |
| 9 / 10 | Super especial (1 barra de ki) | chão ou voando / pulo |
| 11 / 12 | Especial máximo (2 barras de ki) | chão ou voando / pulo |
| 13 | — | nunca é usado pelo jogo |
| 14 / 15 | Ativar a fúria (precisa da barra de fúria cheia) | chão ou pulo / voando |
| 16 / 17 | Comando 23 (só sai pelas sequências automáticas da CPU e do toque) | chão / ar |
| 18 / 19 | Comandos 6 e 7 | qualquer lugar |

"Pulo" é estar no ar sem poder voar; "voando" é estar no ar com
`$PODE_VOAR: 1`. Os nomes que o próprio jogo dá aos golpes estão nas linhas
2–16 do HDAT 2 (ex.: `必殺技1(地上)` = especial 1, no chão).

Se o personagem tem menos linhas no HDAT 4 do que o número do golpe pedido, o
jogo lê dados do HDAT seguinte. Mantenha uma linha para cada golpe usado.

## Hitboxes do DAC que dependem destes HDATs

No `000.dac.txt`, cada `$HIT` tem um tipo (o 1º valor). Alguns tipos se ligam
aos HDATs do personagem:

| Tipo | Efeito |
|---|---|
| 0 | Ponto de onde sai o projétil do golpe (HDAT 4 `$PROJETIL_*`) |
| 2 | Janela de cancelamento: abre a continuação dos comandos de toque (HDAT 42) |
| 20 | Dispara um evento do HDAT 13. A **largura** da hitbox é o número do evento (`$EVT_N`) e o X/Y é a posição |

---

## HDAT 0 — decisões da CPU (`$RAW`)

O extractor detalhado está desligado de propósito, então este HDAT sai em
`$RAW`. É uma tabela de byte com 16 colunas × 45 linhas, lida em `Game7` modo 952.

- **Linha** = situação × 4 + variação (0–3, sorteada pela CPU).
- **Coluna** = `CPULevel` (dificuldade da CPU, 0–7) quando a CPU está no chão,
  ou 8 + `CPULevel` quando está no ar.
- **Valor** = número da sequência do HDAT 6 que a CPU vai executar (0 = fica
  parada ou andando).

As 11 situações olham para o **oponente** da CPU:

| Situação | Linhas | Quando |
|---|---|---|
| 0 / 1 / 2 | 0–3 / 4–7 / 8–11 | oponente no chão, perto / médio / longe |
| 3 / 4 / 5 | 12–15 / 16–19 / 20–23 | oponente no ar baixo, perto / médio / longe |
| 6 | 24–27 | um projétil do oponente está chegando (só a CPU do jogador 2) |
| 7 | 28–31 | o oponente está atacando |
| 8 / 9 / 10 | 32–35 / 36–39 / 40–43 | oponente no ar alto (80 px ou mais acima do chão), perto / médio / longe |

"Perto" e "médio" são as distâncias do HDAT 2 do gamedata.

A linha 44 é o **tempo de reação**: antes de executar a sequência, a CPU
espera um número sorteado de 0 até esse valor − 1 frames. Valores altos (ex.:
30 no nível 0) deixam a CPU lenta; baixos (ex.: 3 no nível 7), rápida.

O jogo também troca a sequência sozinho em alguns casos. Com a barra de fúria
cheia, a CPU usa a sequência 55 (ativar a fúria). As sequências 7 e 20 só
valem com a fúria ativa; sem ela, viram a 8. Um número maior que o tamanho do
HDAT 6 também vira a 8.

## HDAT 1 — `$REGENERACAO_KI`

Ganho de ki em cada troca de golpes. Cada campo é um par de bytes
`multiplicador/divisor`: o ki ganho = dano × multiplicador ÷ divisor. Se o
divisor é 100, o `.txt` mostra só a porcentagem.

| Campo | Quem ganha | Quando |
|---|---|---|
| `AO_ATACAR` | atacante | a cada hit que conecta. Não vale para os golpes 11 e 12 (especial máximo). Nas animações 28–39 o ganho ainda é multiplicado por um bônus interno |
| `AO_RECEBER_DANO` | quem apanhou | a cada hit recebido (além disso, a barra de fúria sobe 10 por hit) |
| `AO_DEFENDER` | quem defendeu | a cada hit bloqueado |
| `AO_BATER_NA_GUARDA` | atacante | quando o golpe é bloqueado |

Exemplo: `AO_ATACAR: 30` → um hit de 300 de dano dá 90 de ki ao atacante.
Valores altos deixam o ki enchendo rápido e os especiais saindo com
frequência.

## HDAT 2 (e 43, 44, 45) — `$TEXTOS_DO_PERSONAGEM`

Textos do personagem. O HDAT 2 é o idioma 0 (o único que o jogo usa hoje); o
43, o 44 e o 45 são os idiomas 1, 2 e 3. Cada `$LINHA_N` é uma string (`_`
vira quebra de linha na tela).

| Linha | Onde aparece |
|---|---|
| 0 | Título/apresentação do personagem na seleção |
| 1 | "X ficou disponível!" (quando ele é liberado) |
| 2–16 | Mensagens de golpe aprendido (ataques fortes B/C no chão, especiais 1–3, super, máximo; depois as versões no ar). Na missão inicial, a linha estágio + 1 aparece depois de cada vitória |
| 19 | Fala padrão antes da luta (quando a linha do oponente está vazia) |
| 21 + N | Fala antes da luta contra o personagem N |
| 101 + N | Fala de vitória contra o personagem N |

O formato do texto é o mesmo do `$T_N`: `"..."` grava em UTF-8 (o que o jogo
lê), `SJIS"..."` mantém o Shift_JIS original e `\xHH` é um byte cru. Para o
texto aparecer certo no jogo, tire o `SJIS` da frente.

## HDAT 3 — `$STATUS_BASE_E_FISICA`

Uma linha de 18 bytes. Define o corpo, o movimento e os status de base do
personagem.

### Corpo

| Campo | Byte | O que faz |
|---|---|---|
| `$LARGURA` | 3 | Largura do corpo. O `.txt` mostra a **metade** (o byte guarda a largura inteira; `12.5` é aceito). Define até onde o personagem chega na parede do cenário e a distância mínima entre os dois lutadores (eles se empurram quando ficam mais perto que a média das larguras) |
| `$ALTURA` | 4 | Altura do corpo em px. Os lutadores só se empurram se os dois estiverem a menos de `$ALTURA` px do chão: mais alto que isso, um passa por cima do outro. Um personagem que **não voa** só faz o movimento aéreo (animações 16–18) se estiver pelo menos `$ALTURA` px acima do chão |

### Movimento (px por frame)

| Campo | Byte | O que faz |
|---|---|---|
| `$VELOCIDADE_MOVIMENTO` | 0 | Andar na direção do oponente, subir/descer voando e a velocidade com que ele persegue o alvo (movimento automático) |
| `$VELOCIDADE_RECUO` | 1 | Andar para trás, se afastando do oponente |
| `$VELOCIDADE_DASH_VERTICAL` | 7 | Velocidade durante o dash (animações 2 e 3), tanto para frente/trás quanto para cima/baixo. O nome diz "vertical", mas vale para as duas direções |
| `$DISTANCIA_AGARRE` | 5 | Distância em px até o oponente em que o atacante aparece nos golpes com `$ATK_FLAGS` 8 ou 128 (teletransporte/agarre) |
| `$PODE_VOAR` | 6 | 1 = voa livremente (fica no ar, usa os golpes "voando" e `$VELOCIDADE_MOVIMENTO` para subir/descer); 0 = só pula |

### Status (no nível 0; o HDAT 7 soma o crescimento por nível)

| Campo | Byte | O que faz |
|---|---|---|
| `$VIDA_INICIAL` | 11 | HP base. HP total = este valor + a soma da coluna 0 do HDAT 7 até o nível atual |
| `$ATK_BASE` | 12 | Ataque. O `.txt` mostra **×10**, que é como o jogo exibe na seleção (100 no byte = 1000 no texto). O jogo usa o valor do byte como **% do dano** do golpe: dano = dano do golpe × ATK ÷ 100. ATK total = byte + soma da coluna 1 do HDAT 7 + cartas |
| `$DEFESA_BASE` | 14 | Defesa, também mostrada ×10. É a **% de redução** do dano recebido: dano × (100 − DEF) ÷ 100. DEF total = byte + soma da coluna 3 do HDAT 7 + cartas |
| `$KI_MAXIMO_BASE` | 8 | Ki máximo, mostrado ×100 (1 no byte = 1 barra). Ki máximo = byte × 100 + soma da coluna 2 do HDAT 7 + cartas. Na maioria dos personagens a linha 0 do HDAT 7 já soma 200 (2 barras): com base 100 fica 3 barras |
| `$VELOCIDADE_CARGA_KI` | 10 | Ki ganho por frame enquanto segura o botão de carregar (multiplicado por um bônus interno de carga, em %) |
| `$TIPO_COUNTER` | 13 | Reação aplicada no oponente quando um golpe com `$ATK_FLAGS` 2 (contra-ataque) é atingido. 0–29 = reação de chão do gamedata (HDAT 10 + valor); 30–59 = reação aérea/agarre deste personagem (HDAT 14 + valor − 30) |

O dano mínimo de qualquer hit é 2.

### Identidade

| Campo | Byte | O que faz |
|---|---|---|
| `$ID_ATRIBUTO` | 9 | Afinidade do personagem: 0 = bem, 1 = mal, 2 = neutro. Define o ícone nas telas de status/seleção e dá o bônus de afinidade (HDAT 155 do gamedata) para cada carta da mesma afinidade no deck |
| `$PONTOS_EQUIPAR_CARTAS` | 17 | Custo máximo das cartas que ele pode equipar. Custo total = byte + soma da coluna 21 do HDAT 7 |
| `$ANIM_ENTRADA` | 16 | Animação tocada quando se tenta usar um golpe "selado". Hoje nenhum golpe fica selado (o jogo nunca liga o selo), então **o campo não tem efeito** |
| `$DESCONHECIDO_2`, `$DESCONHECIDO_15` | 2, 15 | Não são lidos |

## HDAT 4 — `$ATRIBUTOS_DE_GOLPE`

Tabela de short. Cada `$GOLPE_N` é uma linha (o número do golpe está na
tabela acima) com `$COL` valores; o tamanho da linha é 2 × `$COL` bytes. O
`char01` original tem 23 colunas. Animações = ANO do DAC do personagem;
0 = nenhuma.

### Início e flags

| Campo | Offset | O que faz |
|---|---|---|
| `$ATK_FLAGS` | 0 | Bits de comportamento (some os valores) — ver abaixo |
| `$ANIM_INICIAL` | 2 | Animação que o golpe toca. Se o personagem já estava num especial (ANO ≥ 40) e este também começa em ANO ≥ 40, aparece o efeito visual de cancelamento (menos nos golpes 9–12) |

Bits de `$ATK_FLAGS`:

| Bit | Efeito |
|---|---|
| 1 | Projétil **perfurante**: continua depois de acertar (sem o bit, para no primeiro hit) |
| 2 | **Contra-ataque**: se um golpe do oponente atingir este golpe, o oponente sofre a reação de `$TIPO_COUNTER` (HDAT 3) |
| 4 | Solta **3 projéteis** por ponto de disparo (animações `$PROJETIL_ANIM_*`, +1 e +2) |
| 8 | No início do golpe, o atacante **aparece colado** no oponente (a `$DISTANCIA_AGARRE` dele, na mesma altura) |
| 16 | Os dois lutadores **atravessam** um ao outro durante o golpe (sem empurrão de corpo) |
| 32 | O projétil **nasce na posição do oponente** |
| 64 | O projétil nasce **no chão, embaixo do oponente** |
| 128 | Igual ao 8, mas só **depois que o golpe acerta** (teletransporte de perseguição) |

Os golpes 14 e 15 sempre ligam a fúria, independente das flags.

### Projétil

O projétil sai das hitboxes tipo 0 do DAC do atacante (no máximo 8 projéteis
ao mesmo tempo).

| Campo | Offset | O que faz |
|---|---|---|
| `$PROJETIL_ANIM_NORMAL` | 4 | Animação do projétil quando os dois estão mais ou menos na mesma altura (0 = o golpe não solta projétil) |
| `$PROJETIL_ANIM_ABAIXO` | 34 | Usada quando o atacante está 51 px ou mais **abaixo** do oponente (tiro para cima) |
| `$PROJETIL_ANIM_ACIMA` | 36 | Usada quando o atacante está 37 px ou mais **acima** do oponente (tiro para baixo) |
| `$PROJETIL_ANIM_AO_ACERTAR` | 6 | Animação do projétil quando ele acerta (explosão). 0 = o projétil some |
| `$PROJETIL_ANIM_ACERTAR_GLOBAL` | 38 | Animação do projétil quando ele acerta mas o oponente **não entra em reação de dano** (por exemplo, defendeu). 0 = usa a de cima |
| `$PROJETIL_COLISAO_CHAO` | 40 | Animação do projétil quando ele bate no chão. 0 = usa a de `$PROJETIL_ANIM_AO_ACERTAR` (ou some, se ela também for 0) |
| `$HOMING` | 26 | **0 = persegue** o oponente; qualquer outro valor = vai reto. O nome engana: 1 desliga a perseguição |
| `$HOMING_VELOCIDADE` | 28 | Força da perseguição (quanto o projétil curva por frame). Só vale com `$HOMING: 0` |
| `$MAX_REPETICOES` | 30 | Só nos golpes 0, 3 e 6 (especial 1): quantos tiros extras o jogador pode soltar apertando o botão de carregar nos frames do DAC com a flag de OPT 512. O golpe alterna as animações 40 e 41 e termina na 42. 0 = sem repetição |

### Subprojétil

Cada hitbox tipo 0 do próprio projétil pode soltar um segundo projétil:

| Campo | Offset | O que faz |
|---|---|---|
| `$SUBPROJETIL_ANIM_ID` | 18 | Animação do subprojétil (0 = não solta) |
| `$SUBPROJETIL_ANIM_AO_ACERTAR` | 20 | Animação quando o subprojétil acerta (0 = some) |
| `$SUBPROJETIL_COLISAO_CHAO` | 42 | Animação quando ele bate no chão |

O subprojétil usa o `$PROJETIL_ANIM_ACERTAR_GLOBAL` do golpe para o caso "acertou sem reação".

### Atacante depois do hit

| Campo | Offset | O que faz |
|---|---|---|
| `$ANIM_AO_ACERTAR` | 32 | Quando o golpe conecta e o oponente entra em reação, o atacante troca para esta animação (continuação do golpe). 0 = segue normal |
| `$ANIM_SEQ_CHAO` | 10 | Atacante no chão: depois de conectar, vai para esta animação (próximo trecho do combo) |
| `$ANIM_END_CHAO` | 12 | Atacante no chão: depois de `$ANIM_SEQ_CHAO`, vai para esta (final do combo) |
| `$ANIM_SEQ_AR` / `$ANIM_END_AR` | 14 / 16 | O mesmo, com o atacante no ar |
| `$POUSO_AO_ERRAR` | 8 | Se o atacante cai no chão durante o golpe **sem ter acertado**, toca esta animação. 0 = encerra o golpe e toca a aterrissagem padrão (ANO 13) |
| `$POUSO_AO_ACERTAR` | 22 | O mesmo, quando o golpe **já acertou** |

### Oponente

| Campo | Offset | O que faz |
|---|---|---|
| `$OPONENTE_ANIM_SENDO_AGARRADO` | 24 | Enquanto o oponente está numa reação de dano (ANO 40–62) causada por este golpe, uma animação **do DAC do atacante** é desenhada por cima dele (efeito de agarre, explosão presa no corpo etc.). 0 = nada |

### Desconhecidos

`$DESCONHECIDO_44`, `$DESCONHECIDO_46`, ... são as colunas depois do offset
42. O jogo não as lê.

## HDAT 5 — `$PONTUACAO_POR_GOLPE`

Tabela de short com 40 valores, um para cada animação especial de 27 a 66
(`$ANO_27` … `$ANO_66`). Ao acertar o oponente com uma dessas animações, o
jogador ganha esses pontos no placar. Com combo de 2 hits ou mais, os pontos
ainda são multiplicados pela linha `$PONTOS_POR_COMBO` do HDAT 5 do gamedata e
arredondados para cima, para a centena. 0 = sem pontos.

## HDAT 6 — sequências da CPU (`$RAW`)

O extractor detalhado está desligado, então sai em `$RAW`. É uma tabela de
byte com 10 colunas: cada linha é uma sequência (o número usado pelo HDAT 0 e
pelo HDAT 42).

| Coluna | O que faz |
|---|---|
| 0, 2, 4, 6 | Os 4 passos da sequência: comando + 1 (0 = fim da sequência). Comando 1 = defender; comando 24 = levantar voo (só se o personagem voa e está no chão); os outros são combinações de botões/direções da tabela `SeqKey` do `TCBManajer` |
| 1, 3, 5, 7 | Duração de cada passo, em frames |
| 8 | Sequência seguinte (encadeia). Vale só para a CPU |
| 9 | 1 = aborta a sequência se o passo for um ataque e não houver janela de cancelamento |

Sequências com papel fixo: 55 = ativar a fúria; 7 e 20 = só com a fúria
ativa (senão viram a 8); 36 = usada pela CPU num modo especial com 100 de ki.

## HDAT 7 — `$CRESCIMENTO_POR_NIVEL`

Tabela de byte, 99 linhas (níveis 0 a 98) × 22 colunas. O status num nível N
é a base do HDAT 3 + **a soma das linhas 0 até N**.

| Coluna | Soma em | Aparece na tela de "subiu de nível" como |
|---|---|---|
| 0 | HP (`$VIDA_INICIAL`) | — |
| 1 | ATK (`$ATK_BASE`) | ATK + valor × 1000 |
| 2 | Ki máximo (`$KI_MAXIMO_BASE` × 100) | Ki + valor |
| 3 | DEF (`$DEFESA_BASE`) | DEF + valor × 1000 |
| 21 | Custo de cartas (`$PONTOS_EQUIPAR_CARTAS`) | Custo + valor |

As colunas 4–20 não são lidas. O nível máximo é 99 (linha 98).

## HDAT 8 a 12 — sem leitura

O jogo não lê esses HDATs (saem em `$RAW`).

## HDAT 13 — `$ROTEIRO_CUTSCENE`

Tabela de short, 8 valores por linha. Cada `$EVT_N` é um evento visual
disparado por uma hitbox **tipo 20** no DAC do personagem. A **largura** da
hitbox é o número do evento e o X/Y é onde ele aparece. É assim que os golpes
mostram efeitos de tela, mudam a câmera ou prendem o oponente.

| Campo | Índice | O que faz |
|---|---|---|
| `$ALVO` | 0 | De onde vem a animação: 0 = efeitos do sistema, 1 = DAC do próprio personagem, outro = objeto do cenário |
| `$DURACAO` | 1 | Quantos frames o evento dura enquanto o personagem continua na mesma animação. 0 = até a animação do personagem mudar |
| `$Z_LAYER` | 2 | Prioridade de desenho: 0 → 200 (eventos 8 e 9: 10); 1 → 17 (evento 8: 19, evento 9: 21); outro → 400. Para comparar: os personagens ficam entre 130 e 154 |
| `$ANIM_ID` | 3 | Animação com o personagem virado para a direita. 0 = o evento não desenha nada (mas ainda aplica `$TRAVAR_VIT` e `$FLAG`) |
| `$ANIM_FLIP` | 4 | Animação com o personagem virado para a esquerda |
| `$CAM_MODE` | 5 | 0 = segue o ponto da hitbox (e o zoom); 1 = efeito de tela cheia; outro = fixo no canto (0,0) |
| `$TRAVAR_VIT` | 6 | Diferente de 0 = congela o oponente enquanto o evento dura |
| `$FLAG` | 7 | Efeito extra (tabela abaixo) |

Valores de `$FLAG`:

| Valor | Efeito |
|---|---|
| 0 | nada |
| 1 | adia o fim do round por KO enquanto o evento roda (o golpe termina mesmo se alguém zerar a vida) |
| 2 | se o oponente já está sem vida, marca-o como nocauteado |
| 3 | a câmera desliza até o atacante (o evento termina quando chega) |
| 4 | desliga o scroll automático da câmera para o atacante |
| 5 | a câmera passa a seguir o oponente |
| 6 | força parede nas bordas da tela |
| 7 / 8 / 9 | oponente / atacante / os dois podem sair pelas bordas |
| 10 / 11 / 12 | joga o oponente para fora da tela à esquerda / à direita / em cima |
| 13 / 14 / 15 | joga o atacante para fora da tela à esquerda / à direita / em cima |

Os valores 3, 4, 7 e 9 também desligam o scroll automático do atacante; 8 e 9
desligam o do oponente.

## HDAT 14 a 33 — `$REACAO_DANO_N`

Reações **aéreas e de agarre** que este personagem aplica no oponente. Um
golpe do DAC escolhe a reação pelo código de dano do hit: códigos menores que
30 usam as reações de chão do **gamedata** (HDAT 10 + código); códigos de 30
em diante usam estas, do **atacante**: HDAT 14 + (código − 30). O oponente
"veste" a reação de quem bateu.

Cada `$FR_N` é um passo da reação, em ordem; o oponente passa para o passo
seguinte quando a duração acaba. Os valores são bytes com sinal.

| Campo | Coluna | O que faz |
|---|---|---|
| `$DUR_C` / `$DUR_A` | 0 / 1 | Duração do passo, em frames, com o oponente no chão / no ar. Se uma duração 255 chega ao fim, a reação vira a reação padrão 2 (na prática, 255 = espera o `$COMP`) |
| `$ANO` | 2 | Animação do oponente (do DAC **dele**). Negativo = a mesma animação, com o oponente virado para o outro lado |
| `$XSPEED` `$YSPEED` `$XACCEL` `$YACCEL` | 3–6 | Velocidade e aceleração com o oponente no chão. 10 = 1 px por frame. X negativo empurra o oponente para trás (para longe de quem bateu); Y negativo sobe. A aceleração é somada à velocidade a cada frame (a velocidade X para quando troca de sinal). −1 = mantém o valor atual; −2 = metade do valor anterior |
| `$XSPEED_A` … `$YACCEL_A` | 7–10 | O mesmo, com o oponente no ar. Num **agarre**, `$XSPEED_A`/`$YSPEED_A` são a posição do oponente relativa ao atacante, em px |
| `$COMP` | 11 | O que acontece ao tocar o chão ou a parede (tabela abaixo) |
| `$WALL` | 12 | 2 ou 3 = o `$COMP` reage à **parede** do cenário em vez do chão; ao cair no chão, o oponente quica (reação 12) |
| `$VOZ` | 13 | Som tocado no início do passo (1–79 = som global − 1; 80+ = voz do personagem; 0 = nada) |

Valores de `$COMP`:

| Valor | Efeito |
|---|---|
| 0 | Nada especial; ao passar do chão, fica no chão |
| 11 | Ao tocar o chão (ou parar de subir), cai deitado (knockdown) |
| 12 | Ao tocar o chão, se o oponente apertou o botão de carregar durante a reação, levanta na hora (recuperação); senão, passa para a reação 12 |
| 1–49 (outros) | Ao tocar o chão (ou a parede, com `$WALL` 2/3) passa para a reação de número `$COMP`. Se o oponente já está no chão, isso acontece na hora |
| 50 ou mais (ex.: 255) | Ao cair no chão, termina a reação com a aterrissagem padrão |

Quando todos os passos acabam: se o oponente ainda está subindo ou descendo,
vira a reação 13 (queda), ou a 2 se estiver sem vida; se está parado, volta a
lutar (ou cai, se estiver sem vida).

## HDAT 34 — `$DISPUTA_DE_PODER`

| Campo | Linha | O que faz |
|---|---|---|
| `$ANIM_GATILHO` | 0 | Quando este personagem está nesta animação (normalmente o raio de energia), pode começar a **disputa de poder** (o minigame de toques). Começa se o oponente estiver a 240 px ou mais, parado/andando/no ar (ANO 0–12 ou 81) e com 100 de ki ou mais. Se o oponente for a CPU, ela aceita com a chance da linha 18 do HDAT 3 do gamedata |
| `$TEM_CUTSCENE` | 1 | Para o **vencedor** da disputa: 0 = mostra a imagem final (ANO 280 do DAC dele) por 90 frames; outro valor = pula a imagem. O mesmo vale para o fim da cena do golpe Extreme. O nome engana: 0 é que mostra a imagem |

A tela da disputa usa as animações 265–269 e 278–279 do DAC dos dois
personagens.

## HDAT 35 — `$DANO_DE_EVENTOS_CUTSCENE`

Tabela de short: `$DANO` (coluna 0) e `$ANIM` (coluna 1). É o dano dos
eventos que não vêm de um hit do DAC. Ele passa pela mesma conta de ATK/DEF
(e fúria) de um golpe normal: dano final = `$DANO` × ATK ÷ 100 × (100 − DEF) ÷ 100.

| Linha | Evento |
|---|---|
| 0 | Dano no perdedor da disputa de poder (HDAT 34) |
| 1 | Dano no fim do rush de cartas |
| 2 | Dano de cada hit da cena do golpe Extreme |
| 2–5 | `$ANIM` = animação de cada hit da cena do Extreme (até 4 hits) |
| 6 | Dano do golpe final da cena do Extreme |

As outras combinações não são lidas.

## HDAT 36 — `$SISTEMA_DE_VOZES`

Um byte por situação: o som tocado. 0 = sem voz; 1–79 = som global (o jogo
toca o número − 1); 80 ou mais = voz do próprio personagem.

| Campo | Linha | Quando toca |
|---|---|---|
| `$CUTSCENE_INICIO` | 2 | Início da cena do golpe Extreme |
| `$CUTSCENE_HIT_1` … `_4` | 3–6 | Cada hit da cena do Extreme |
| `$CUTSCENE_POS_EVENTO` | 7 | Depois do evento especial |
| `$SELECAO_DE_CARTAS` | 8 | Ao entrar na escolha de cartas (só o jogador 1) |
| `$DISPUTA_DE_KI` | 9 | No início da disputa de ki |
| `$EVENTO_10` | 10 | No fim da disputa de ki |
| `$DESCONHECIDO_0`, `_1`, `_11`… | 0, 1, 11+ | Não são lidos |

## HDAT 37 — `$DANO_NO_ESTADO_DE_FURIA`

Porcentagem aplicada ao dano que este personagem causa **enquanto a fúria
está ativa** (100 = normal, 150 = +50%). Vale para os golpes e para o dano
dos eventos (HDAT 35).

## HDAT 38 — `$CARTA_RECOMPENSA_ARCADE`

Número da carta (short) que o jogador ganha ao **passar do último estágio**
do arcade com este personagem. Ganhar uma carta que já tem sobe o nível dela
em 1; se ela já está no nível máximo (10), vira tickets (HDAT 165 do
gamedata).

## HDAT 39 — `$CENARIO_PADRAO`

Número do cenário usado na luta **quando este personagem é o oponente**
(CPU). No modo 8 vale o do jogador 2.

## HDAT 40 — `$PROGRESSAO_CAMPANHA`

Valores = número do personagem + 1 (0 = vazio).

| Campo | Linha | O que faz |
|---|---|---|
| `$ARCADE` | 0 | Ordem dos oponentes no arcade. O jogo pula personagens não liberados e o próprio chefe. Quando chega num 0 (ou no estágio 6 no fácil), a próxima luta é a do chefe |
| `$BOSS_CANDIDATOS` | 1 | Candidatos a chefe final: o primeiro que já estiver liberado é o chefe |
| `$DESCONHECIDO` | 2 | Não é lido |
| `$HISTORIA` | 3 | Oponentes da **missão inicial** (antes de concluí-la): um por estágio, 8 estágios fixos |

## HDAT 41 — `$PERSONAGENS_DESBLOQUEAVEIS`

Ao terminar o arcade com este personagem, libera os personagens da linha da
dificuldade jogada (`$EASY`, `$NORMAL`, `$HARD`). Valores = número + 1;
0 = fim da lista. Cada um liberado mostra a linha 1 do HDAT 2 dele.

## HDAT 42 — `$COMANDOS_TOUCH`

Tabela de short, 3 colunas. Define o que acontece quando o jogador **toca no
oponente** (a até 48 px dele). O personagem passa a executar sozinho uma
sequência do HDAT 6. Só vale para o jogador humano, fora de um ataque.

A linha é escolhida assim:

```
linha = estado + distância + (20 se ki >= 100)
estado:    0 = no chão, 40 = pulando, 80 = voando
distância: 0 = menos de 80 px, 5 = 80–159 px, 10 = 160–319 px, 15 = 320 px ou mais
```

| Coluna | O que faz |
|---|---|
| 0 | Número da sequência do HDAT 6 que o personagem executa (0 = o toque não faz nada) |
| 1 | Nas linhas de continuação: ki exigido, × 100 |
| 2 | Não é lida |

Antes da tabela, o jogo testa a fúria. Com a barra de fúria cheia (e a fúria
desligada), o toque ativa a fúria (sequência 55). Com a fúria ativa e 200 de
ki, ele usa a sequência 7.

Continuação: depois do primeiro toque, as linhas seguintes (N+1, N+2, …, até
4) são combos. Cada uma sai se o jogador tocar a até 88 px do oponente
durante a janela da hitbox tipo 2 do DAC, com o oponente em reação de dano e
com o ki da coluna 1. Uma linha de continuação com coluna 0 = 0 encerra o
combo.
