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
| 0 | `$COMPORTAMENTO_IA` | Decisões da CPU (qual sequência usar em cada situação) e tempo de reação |
| 1 | `$REGENERACAO_KI` | Quanto ki se ganha ao bater, apanhar e defender |
| 2, 43–45 | `$TEXTOS_DO_PERSONAGEM` | Falas e mensagens (2 = idioma 0; 43–45 = idiomas 1–3) |
| 3 | `$STATUS_BASE_E_FISICA` | Corpo, velocidades, HP/ATK/DEF/ki base, afinidade, custo |
| 4 | `$ATRIBUTOS_DE_GOLPE` | Uma linha por golpe especial: animações, projétil, flags |
| 5 | `$PONTUACAO_POR_GOLPE` | Pontos ganhos ao acertar com cada animação especial |
| 6 | `$SEQUENCIAS_IA` | Sequências de comandos da CPU (e dos comandos de toque) |
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

## HDAT 0 — `$COMPORTAMENTO_IA`

Tabela de byte, 16 colunas × 45 linhas, lida em `Game7` modo 952. Diz qual
sequência do HDAT 6 a CPU executa em cada situação.

```
$OPONENTE_CHAO_PERTO:{
   $VAR_0: { $CPU_NO_CHAO: [  0,   0,   0,   0,   0,   0,   0,  37], $CPU_NO_AR: [  0, …] },
   …
},
…
$TEMPO_DE_REACAO: { $CPU_NO_CHAO: [ 30, …], $CPU_NO_AR: [ …] },
```

- Cada lista tem **um valor por nível da CPU** (`CPULevel` 0 a 7; 0 = mais
  fraca, 7 = mais forte). O nível vem do HDAT 4 do gamedata (arcade) ou do
  modo treino.
- `$CPU_NO_CHAO` vale quando a CPU está no chão; `$CPU_NO_AR`, quando ela
  está no ar.
- **Valor** = número da sequência do HDAT 6 (`$SEQ_N`) que a CPU executa.
  A `$SEQ_0` costuma ser "nada por 15 frames".
- `$VAR_0` a `$VAR_3`: a cada decisão a CPU sorteia uma das 4 variações. Para
  deixar a CPU previsível, ponha o mesmo valor nas 4.

As 11 situações olham para o **oponente** da CPU:

| Bloco | Linhas | Quando |
|---|---|---|
| `$OPONENTE_CHAO_PERTO` / `_MEDIO` / `_LONGE` | 0–3 / 4–7 / 8–11 | oponente no chão, perto / médio / longe |
| `$OPONENTE_AR_BAIXO_PERTO` / `_MEDIO` / `_LONGE` | 12–15 / 16–19 / 20–23 | oponente no ar, a menos de 80 px do chão |
| `$PROJETIL_CHEGANDO` | 24–27 | um projétil do oponente está chegando (só a CPU do jogador 2) |
| `$OPONENTE_ATACANDO` | 28–31 | o oponente está atacando (só a CPU do jogador 2) |
| `$OPONENTE_AR_ALTO_PERTO` / `_MEDIO` / `_LONGE` | 32–35 / 36–39 / 40–43 | oponente a 80 px ou mais acima do chão |

O jogo testa nessa ordem: projétil, ataque, depois a distância. "Perto" e
"médio" são as distâncias do HDAT 2 do gamedata (`$PERTO` e `$MEDIO`, com o
alvo no chão ou no ar).

`$TEMPO_DE_REACAO` (linha 44): antes de começar cada sequência, a CPU espera
um número sorteado de 0 até o valor − 1 frames. 0 = sem espera. Valores altos
(ex.: 30 no nível 0) deixam a CPU lenta; baixos (ex.: 3 no nível 7), rápida.

O jogo também troca a sequência sozinho em alguns casos. Com a barra de fúria
cheia, a CPU do jogador 2 usa a sequência 55 (ativar a fúria). As sequências
7 e 20 só valem com a fúria ativa; sem ela, viram a 8. Um número maior que o
último `$SEQ_N` do HDAT 6 também vira a 8.

Como testar: no modo treino, escolha o nível da CPU e mude só a coluna desse
nível. Ex.: com o nível 7, troque o último valor de cada lista.

O build ainda aceita o formato do extractor antigo (`$PROXIMO_CHAO`,
`$CHAO:[N0:…]`, `$POOL_DE_SEQUENCIAS`).

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

## HDAT 6 — `$SEQUENCIAS_IA`

Tabela de byte, 10 colunas: cada linha é uma sequência de até 4 comandos. A
CPU executa as sequências escolhidas pelo HDAT 0, e o jogador as escolhidas
pelos toques (HDAT 42).

```
$SEQ_10: { $P1: [ 5,  15], $P2: [ 9,  14], $P3: [ 4,   0], $P4: [ 0,   0], $PROXIMA:  12, $SO_COM_CANCEL: 0 },  // andar p/ frente 15f > pular p/ frente 14f > especial 1 0f > seq 12
```

| Campo | Colunas | O que faz |
|---|---|---|
| `$P1` … `$P4` | 0–7 | Os 4 passos: `[comando, frames]`. O comando fica apertado esses frames (0 = 1 frame). Comando 0 = fim: os passos depois dele não rodam |
| `$PROXIMA` | 8 | Sequência que começa quando esta acaba (0 = nenhuma). Só vale para a CPU do jogador 2 fora do modo 8; nos toques é ignorada |
| `$SO_COM_CANCEL` | 9 | 1 = se um passo de golpe acabar sem janela de cancelamento (o golpe não acertou), a sequência para ali |

O comentário no fim de cada linha descreve a sequência em palavras (o build
ignora). Comandos:

| Valor | Comando |
|---|---|
| 0 | fim da sequência |
| 1 | nada: não aperta nada e a CPU escolhe de novo pelo HDAT 0 (se a situação mudou, começa a sequência nova). Use só nas sequências da CPU |
| 2 | defender (enquanto o oponente ataca ou um projétil chega) |
| 3 | ataque (ANO 30; pulando, ANO 34) |
| 4 / 7 / 16 | especial 1 / 2 / 3 (golpes 0/3/6, 1/4/7, 2/5/8) |
| 5 / 6 | andar (ou mover voando) para frente / para trás |
| 8 | super especial (golpe 9/10, 1 barra de ki) |
| 9 / 10 / 11 | pular para frente (ANO 11) / para cima (ANO 10) / para trás (ANO 12) |
| 12 / 13 | dash para frente (ANO 6; no ar, 8) / para trás (ANO 7; no ar, 9) |
| 14 / 15 / 18 | ataque + frente (ANO 31) / + baixo (ANO 36) / + trás (ANO 37); pulando: 35 / 38 / 39 |
| 17 | ativar a fúria (golpe 14/15, precisa da barra de fúria cheia) |
| 19 | golpe 16/17 (só sai por sequência) |
| 20 | carregar ki (para sozinho com o ki cheio) |
| 21 / 22 | ki + trás / ki + frente: golpe 18 ou 19 (depende do lado para onde o personagem está virado) |
| 23 / 24 | investida para frente (ANO 2) / para trás (ANO 3) |
| 25 | levantar voo (só quem voa e está no chão) |
| 26 / 27 | subida rápida (ANO 82) / descida rápida (ANO 83), só quem voa. Quem não voa faz o especial 1 |
| 28 | especial máximo (golpe 11/12, 2 barras de ki) |
| 29 / 30 | investida para cima / para baixo (ANO 2) |
| 31 ou mais | nada |

Os especiais 1–3 não gastam ki, a não ser que cancelem outro especial: aí
gastam 100. O super gasta 100 e o máximo, 200.

Sequências com papel fixo: 55 = ativar a fúria; 7 e 20 = só com a fúria
ativa (senão viram a 8); 8 = a reserva; 36 = usada pela CPU num modo especial
com 100 de ki.

Para criar uma sequência nova, adicione um `$SEQ_N` no fim e aumente o
`$ROW` do `$info` (senão o jogo troca o número novo pela 8).

O build ainda aceita o formato antigo (`{$ACT:…, $ACT_TIME:…}`, `$CONTINUA`,
`$EXIGE_CANCEL`).

## HDAT 7 — `$CRESCIMENTO_POR_NIVEL`

Tabela de byte, 22 colunas × 99 linhas. Diz quanto cada status sobe a cada
nível. Cada linha é um nível, com o número que o jogo mostra:

```
$NIVEL_2:  { $HP:   0, $ATK:   1, $KI:   0, $DEF:   0, $CUSTO:   8, $NAO_LIDOS: [0, …] },  // soma: HP +0, ATK +1, KI +200, DEF +0, CUSTO +8
```

O status num nível N = a base do HDAT 3 + **a soma de `$NIVEL_1` até
`$NIVEL_N`**. O `$NIVEL_1` já vale desde o começo. O comentário de cada linha
mostra essa soma até ali (sem a base do HDAT 3); o build o ignora.

| Campo | Coluna | Soma em | Na tela de "subiu de nível" |
|---|---|---|---|
| `$HP` | 0 | HP (`$VIDA_INICIAL`) | — |
| `$ATK` | 1 | ATK (`$ATK_BASE`): cada ponto = +1% de dano | ATK + valor × 1000 |
| `$KI` | 2 | Ki máximo (`$KI_MAXIMO_BASE` × 100; 100 = 1 barra) | Ki + valor |
| `$DEF` | 3 | DEF (`$DEFESA_BASE`): cada ponto = +1% de redução de dano | DEF + valor × 1000 |
| `$CUSTO` | 21 | Custo de cartas (`$PONTOS_EQUIPAR_CARTAS`) | Custo + valor |
| `$NAO_LIDOS` | 4–20 | Não são lidas | — |

Valores de 0 a 255. O nível máximo é 99: linhas além do `$NIVEL_99` não são
usadas. A tela de "subiu de nível" mostra os ganhos do nível novo; um campo 0
não aparece.

Como testar: para ver o efeito rápido, ponha valores altos no `$NIVEL_2` (ex.:
`$ATK: 50`) e suba o personagem do nível 1 para o 2.

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

Tabela de short, 3 colunas × 120 linhas. Define o que acontece quando o
jogador **toca no oponente** (a até 48 px dele): o personagem passa a executar
sozinho uma sequência do HDAT 6. Só vale para o jogador humano, fora de um
ataque.

As linhas vêm em 24 grupos de 5, um por situação:

```
$CHAO_PERTO:{
   $TOQUE:   [60, 0, 0],
   $COMBO_1: [61, 0, 0],
   $COMBO_2: [62, 0, 0],
   $COMBO_3: [0, 0, 0],
   $COMBO_4: [0, 0, 0],
},
```

O nome do grupo diz a situação: **estado** do personagem + **distância** em X
até o oponente + `_KI` com 100 de ki ou mais.

| Parte do nome | Quando |
|---|---|
| `CHAO_` / `PULO_` / `VOANDO_` | no chão / pulando (ANO 10–12) / voando |
| `PERTO` / `MEDIO` / `LONGE` / `MUITO_LONGE` | menos de 80 px / 80–159 / 160–319 / 320 px ou mais |
| `_KI` | com 100 de ki ou mais (sem `_KI`: menos de 100) |

A linha do grupo é `estado (0/40/80) + distância (0/5/10/15) + 20 se ki ≥ 100`.

| Campo | O que faz |
|---|---|
| `$TOQUE` | `[sequência, -, -]`: a sequência do HDAT 6 do primeiro toque (0 = o toque não faz nada) |
| `$COMBO_1` … `$COMBO_4` | `[sequência, ki exigido × 100, -]`: as continuações. Cada uma sai se o jogador tocar de novo a até 88 px do oponente durante a janela da hitbox tipo 2 do DAC, com o oponente apanhando e com o ki pedido. Sequência 0 encerra o combo |

A coluna marcada `-` não é lida.

Antes da tabela, o jogo testa a fúria. Com a barra de fúria cheia (e a fúria
desligada), o toque ativa a fúria (sequência 55). Com a fúria ativa e 200 de
ki, ele usa a sequência 7.

Como testar: troque o `$TOQUE` do `$CHAO_PERTO` por uma sequência fácil de
reconhecer (ex.: a de carregar ki) e toque no oponente de perto, no chão e com
menos de 100 de ki.

O build ainda aceita o formato de tabela antigo (`$LINHA_N: [...]`).
