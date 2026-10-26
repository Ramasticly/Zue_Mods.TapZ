package com.zue.gd.util;

// GERADO por compiler/help/GerarHelpTexts.java a partir de compiler/help/*.md.
// Não edite à mão: edite o .md e rode o gerador de novo.
final class HelpTexts {

    private HelpTexts() {}

    /** binChar.md */
    static final String[] BIN_CHAR = {
        "# binChar.md — HDATs do personagem (`charNN.pac`)\n" +
        "\n" +
        "Guia de cada HDAT do `000.bin.txt` de um personagem: para que serve, onde o\n" +
        "jogo lê, o que cada campo muda no gameplay e o que já se sabe dos campos ainda\n" +
        "sem nome. Tudo aqui foi conferido no `TCBManajer.java` (as linhas de código\n" +
        "citadas são desse arquivo).\n" +
        "\n" +
        "O jogo carrega o `bin` do personagem do jogador 1 em `ChrGameData[3]` e o do\n" +
        "jogador 2 em `ChrGameData[4]`. Cada personagem tem o seu: mudar o `char01`\n" +
        "muda só aquele personagem.\n" +
        "\n" +
        "## Antes de editar\n" +
        "\n" +
        "- **Unidades.** A tela do jogo tem 480 px de largura e o chão fica em\n" +
        "  y = 240. \"px\" aqui é esse pixel. \"Frame\" é um quadro da lógica do jogo.\n" +
        "- **ANO** é o número de uma animação do DAC do próprio personagem\n" +
        "  (`000.dac.txt`, bloco `$ANO_N`). Quando um campo pede uma animação, coloque\n" +
        "  o número do `$ANO_N` correspondente.\n" +
        "- **Ki.** 100 de ki = 1 barra. O jogo chama o ki de `Synchro` internamente.\n" +
        "- **Tabela de byte e de short.** Cada HDAT é uma tabela com `$COL` colunas e\n" +
        "  `$ROW` linhas. Nas tabelas de byte cada valor vai de 0 a 255; nas de short,\n" +
        "  de -32768 a 32767. O tipo de cada HDAT está indicado abaixo.\n" +
        "- **`$RAW`.** HDATs sem formato detalhado (ou com o extractor desligado em\n" +
        "  `BinExtractor.SKIP_HDATS`) saem como bytes crus `$RAW:B[...]`. O layout de\n" +
        "  cada um também está descrito aqui, caso você queira editar à mão.\n" +
        "- **Campos `$DESCONHECIDO_*`.** O jogo não lê esses bytes (ou lê sem efeito\n" +
        "  visível). Pode deixar como estão.\n" +
        "\n" +
        "## Índice\n" +
        "\n" +
        "| HDAT | Tag no `.txt` | Resumo |\n" +
        "|---|---|---|\n" +
        "| 0 | `$RAW` | Decisões da CPU (qual sequência usar em cada situação) |\n" +
        "| 1 | `$REGENERACAO_KI` | Quanto ki se ganha ao bater, apanhar e defender |\n" +
        "| 2, 43–45 | `$TEXTOS_DO_PERSONAGEM` | Falas e mensagens (2 = idioma 0; 43–45 = idiomas 1–3) |\n" +
        "| 3 | `$STATUS_BASE_E_FISICA` | Corpo, velocidades, HP/ATK/DEF/ki base, afinidade, custo |\n" +
        "| 4 | `$ATRIBUTOS_DE_GOLPE` | Uma linha por golpe especial: animações, projétil, flags |\n" +
        "| 5 | `$PONTUACAO_POR_GOLPE` | Pontos ganhos ao acertar com cada animação especial |\n" +
        "| 6 | `$RAW` | Sequências de comandos da CPU (e dos comandos de toque) |\n" +
        "| 7 | `$CRESCIMENTO_POR_NIVEL` | Quanto HP/ATK/DEF/ki/custo sobe em cada nível |\n" +
        "| 8–12 | `$RAW` | Sem leitura no código |\n" +
        "| 13 | `$ROTEIRO_CUTSCENE` | Eventos visuais disparados pela hitbox tipo 20 do DAC |\n" +
        "| 14–33 | `$REACAO_DANO_N` | Reações aéreas e de agarre que este personagem aplica no oponente |\n" +
        "| 34 | `$DISPUTA_DE_PODER` | Animação que inicia a disputa de poder e a imagem final |\n" +
        "| 35 | `$DANO_DE_EVENTOS_CUTSCENE` | Dano dos eventos especiais (rush, Extreme, disputa) |\n" +
        "| 36 | `$SISTEMA_DE_VOZES` | Voz tocada em cada situação especial |\n" +
        "| 37 | `$DANO_NO_ESTADO_DE_FURIA` | % do dano enquanto a fúria está ativa |\n" +
        "| 38 | `$CARTA_RECOMPENSA_ARCADE` | Carta ganha ao terminar o arcade com este personagem |\n" +
        "| 39 | `$CENARIO_PADRAO` | Cenário da luta quando ele é o oponente |\n" +
        "| 40 | `$PROGRESSAO_CAMPANHA` | Oponentes do arcade e candidatos a chefe |\n" +
        "| 41 | `$PERSONAGENS_DESBLOQUEAVEIS` | Personagens liberados ao terminar o arcade |\n" +
        "| 42 | `$COMANDOS_TOUCH` | O que um toque no oponente faz em cada situação |\n" +
        "\n" +
        "---\n" +
        "\n" +
        "## Índice dos golpes (linha do HDAT 4)\n" +
        "\n" +
        "Vários HDATs usam o \"número do golpe\" (`SpecialType` no código). É ele que\n" +
        "escolhe a linha `$GOLPE_N` do HDAT 4. Ele depende do comando e de onde o\n" +
        "personagem está:\n" +
        "\n" +
        "| Golpe | Comando | Onde |\n" +
        "|---|---|---|\n" +
        "| 0 / 3 / 6 | Especial 1 | chão / pulo / voando |\n" +
        "| 1 / 4 / 7 | Especial 2 | chão / pulo / voando |\n" +
        "| 2 / 5 / 8 | Especial 3 | chão / pulo / voando |\n" +
        "| 9 / 10 | Super especial (1 barra de ki) | chão ou voando / pulo |\n" +
        "| 11 / 12 | Especial máximo (2 barras de ki) | chão ou voando / pulo |\n" +
        "| 13 | — | nunca é usado pelo jogo |\n" +
        "| 14 / 15 | Ativar a fúria (precisa da barra de fúria cheia) | chão ou pulo / voando |\n" +
        "| 16 / 17 | Comando 23 (só sai pelas sequências automáticas da CPU e do toque) | chão / ar |\n" +
        "| 18 / 19 | Comandos 6 e 7 | qualquer lugar |\n" +
        "\n" +
        "\"Pulo\" é estar no ar sem poder voar; \"voando\" é estar no ar com\n" +
        "`$PODE_VOAR: 1`. Os nomes que o próprio jogo dá aos golpes estão nas linhas\n" +
        "2–16 do HDAT 2 (ex.: `必殺技1(地上)` = especial 1, no chão).\n" +
        "\n" +
        "Se o personagem tem menos linhas no HDAT 4 do que o número do golpe pedido, o\n" +
        "jogo lê dados do HDAT seguinte. Mantenha uma linha para cada golpe usado.\n" +
        "\n" +
        "## Hitboxes do DAC que dependem destes HDATs\n" +
        "\n" +
        "No `000.dac.txt`, cada `$HIT` tem um tipo (o 1º valor). Alguns tipos se ligam\n" +
        "aos HDATs do personagem:\n" +
        "\n" +
        "| Tipo | Efeito |\n" +
        "|---|---|\n" +
        "| 0 | Ponto de onde sai o projétil do golpe (HDAT 4 `$PROJETIL_*`) |\n" +
        "| 2 | Janela de cancelamento: abre a continuação dos comandos de toque (HDAT 42) |\n" +
        "| 20 | Dispara um evento do HDAT 13. A **largura** da hitbox é o número do evento (`$EVT_N`) e o X/Y é a posição |\n" +
        "\n" +
        "---\n" +
        "\n" +
        "## HDAT 0 — decisões da CPU (`$RAW`)\n" +
        "\n" +
        "O extractor detalhado está desligado de propósito, então este HDAT sai em\n" +
        "`$RAW`. É uma tabela de byte com 16 colunas × 45 linhas, lida em `Game7` modo 952.\n" +
        "\n" +
        "- **Linha** = situação × 4 + variação (0–3, sorteada pela CPU).\n" +
        "- **Coluna** = `CPULevel` (dificuldade da CPU, 0–7) quando a CPU está no chão,\n" +
        "  ou 8 + `CPULevel` quando está no ar.\n" +
        "- **Valor** = número da sequência do HDAT 6 que a CPU vai executar (0 = fica\n" +
        "  parada ou andando).\n" +
        "\n" +
        "As 11 situações olham para o **oponente** da CPU:\n" +
        "\n" +
        "| Situação | Linhas | Quando |\n" +
        "|---|---|---|\n" +
        "| 0 / 1 / 2 | 0–3 / 4–7 / 8–11 | oponente no chão, perto / médio / longe |\n" +
        "| 3 / 4 / 5 | 12–15 / 16–19 / 20–23 | oponente no ar baixo, perto / médio / longe |\n" +
        "| 6 | 24–27 | um projétil do oponente está chegando (só a CPU do jogador 2) |\n" +
        "| 7 | 28–31 | o oponente está atacando |\n" +
        "| 8 / 9 / 10 | 32–35 / 36–39 / 40–43 | oponente no ar alto (80 px ou mais acima do chão), perto / médio / longe |\n" +
        "\n" +
        "\"Perto\" e \"médio\" são as distâncias do HDAT 2 do gamedata.\n" +
        "\n" +
        "A linha 44 é o **tempo de reação**: antes de executar a sequência, a CPU\n" +
        "espera um número sorteado de 0 até esse valor − 1 frames. Valores altos (ex.:\n" +
        "30 no nível 0) deixam a CPU lenta; baixos (ex.: 3 no nível 7), rápida.\n" +
        "\n" +
        "O jogo também troca a sequência sozinho em alguns casos. Com a barra de fúria\n" +
        "cheia, a CPU usa a sequência 55 (ativar a fúria). As sequências 7 e 20 só\n" +
        "valem com a fúria ativa; sem ela, viram a 8. Um número maior que o tamanho do\n" +
        "HDAT 6 também vira a 8.\n" +
        "\n" +
        "## HDAT 1 — `$REGENERACAO_KI`\n" +
        "\n" +
        "Ganho de ki em cada troca de golpes. Cada campo é um par de bytes\n" +
        "`multiplicador/divisor`: o ki ganho = dano × multiplicador ÷ divisor. Se o\n" +
        "divisor é 100, o `.txt` mostra só a porcentagem.\n" +
        "\n" +
        "| Campo | Quem ganha | Quando |\n" +
        "|---|---|---|\n" +
        "| `AO_ATACAR` | atacante | a cada hit que conecta. Não vale para os golpes 11 e 12 (especial máximo). Nas animações 28–39 o ganho ainda é multiplicado por um bônus interno |\n" +
        "| `AO_RECEBER_DANO` | quem apanhou | a cada hit recebido (além disso, a barra de fúria sobe 10 por hit) |\n" +
        "| `AO_DEFENDER` | quem defendeu | a cada hit bloqueado |\n" +
        "| `AO_BATER_NA_GUARDA` | atacante | quando o golpe é bloqueado |\n" +
        "\n" +
        "Exemplo: `AO_ATACAR: 30` → um hit de 300 de dano dá 90 de ki ao atacante.\n" +
        "Valores altos deixam o ki enchendo rápido e os especiais saindo com\n" +
        "frequência.\n" +
        "\n" +
        "## HDAT 2 (e 43, 44, 45) — `$TEXTOS_DO_PERSONAGEM`\n" +
        "\n" +
        "Textos do personagem. O HDAT 2 é o idioma 0 (o único que o jogo usa hoje); o\n" +
        "43, o 44 e o 45 são os idiomas 1, 2 e 3. Cada `$LINHA_N` é uma string (`_`\n" +
        "vira quebra de linha na tela).\n" +
        "\n" +
        "| Linha | Onde aparece |\n" +
        "|---|---|\n" +
        "| 0 | Título/apresentação do personagem na seleção |\n" +
        "| 1 | \"X ficou disponível!\" (quando ele é liberado) |\n" +
        "| 2–16 | Mensagens de golpe aprendido (ataques fortes B/C no chão, especiais 1–3, super, máximo; depois as versões no ar). Na missão inicial, a linha estágio + 1 aparece depois de cada vitória |\n" +
        "| 19 | Fala padrão antes da luta (quando a linha do oponente está vazia) |\n" +
        "| 21 + N | Fala antes da luta contra o personagem N |\n" +
        "| 101 + N | Fala de vitória contra o personagem N |\n" +
        "\n" +
        "O formato do texto é o mesmo do `$T_N`: `\"...\"` grava em UTF-8 (o que o jogo\n" +
        "lê), `SJIS\"...\"` mantém o Shift_JIS original e `\\xHH` é um byte cru. Para o\n" +
        "texto aparecer certo no jogo, tire o `SJIS` da frente.\n" +
        "\n" +
        "## HDAT 3 — `$STATUS_BASE_E_FISICA`\n" +
        "\n" +
        "Uma linha de 18 bytes. Define o corpo, o movimento e os status de base do\n" +
        "personagem.\n" +
        "\n" +
        "### Corpo\n" +
        "\n" +
        "| Campo | Byte | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$LARGURA` | 3 | Largura do corpo. O `.txt` mostra a **metade** (o byte guarda a largura inteira; `12.5` é aceito). Define até onde o personagem chega na parede do cenário e a distância mínima entre os dois lutadores (eles se empurram quando ficam mais perto que a média das larguras) |\n" +
        "| `$ALTURA` | 4 | Altura do corpo em px. Os lutadores só se empurram se os dois estiverem a menos de `$ALTURA` px do chão: mais alto que isso, um passa por cima do outro. Um personagem que **não voa** só faz o movimento aéreo (animações 16–18) se estiver pelo menos `$ALTURA` px acima do chão |\n" +
        "\n" +
        "### Movimento (px por frame)\n" +
        "\n" +
        "| Campo | Byte | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$VELOCIDADE_MOVIMENTO` | 0 | Andar na direção do oponente, subir/descer voando e a velocidade com que ele persegue o alvo (movimento automático) |\n" +
        "| `$VELOCIDADE_RECUO` | 1 | Andar para trás, se afastando do oponente |\n" +
        "| `$VELOCIDADE_DASH_VERTICAL` | 7 | Velocidade durante o dash (animações 2 e 3), tanto para frente/trás quanto para cima/baixo. O nome diz \"vertical\", mas vale para as duas direções |\n" +
        "| `$DISTANCIA_AGARRE` | 5 | Distância em px até o oponente em que o atacante aparece nos golpes com `$ATK_FLAGS` 8 ou 128 (teletransporte/agarre) |\n" +
        "| `$PODE_VOAR` | 6 | 1 = voa livremente (fica no ar, usa os golpes \"voando\" e `$VELOCIDADE_MOVIMENTO` para subir/descer); 0 = só pula |\n" +
        "\n" +
        "### Status (no nível 0; o HDAT 7 soma o crescimento por nível)\n" +
        "\n" +
        "| Campo | Byte | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$VIDA_INICIAL` | 11 | HP base. HP total = este valor + a soma da coluna 0 do HDAT 7 até o nível atual |\n" +
        "| `$ATK_BASE` | 12 | Ataque. O `.txt` mostra **×10**, que é como o jogo exibe na seleção (100 no byte = 1000 no texto). O jogo usa o valor do byte como **% do dano** do golpe: dano = dano do golpe × ATK ÷ 100. ATK total = byte + soma da coluna 1 do HDAT 7 + cartas |\n" +
        "| `$DEFESA_BASE` | 14 | Defesa, também mostrada ×10. É a **% de redução** do dano recebido: dano × (100 − DEF) ÷ 100. DEF total = byte + soma da coluna 3 do HDAT 7 + cartas |\n" +
        "| `$KI_MAXIMO_BASE` | 8 | Ki máximo, mostrado ×100 (1 no byte = 1 barra). Ki máximo = byte × 100 + soma da coluna 2 do HDAT 7 + cartas. Na maioria dos personagens a linha 0 do HDAT 7 já soma 200 (2 barras): com base 100 fica 3 barras |\n" +
        "| `$VELOCIDADE_CARGA_KI` | 10 | Ki ganho por frame enquanto segura o botão de carregar (multiplicado por um bônus interno de carga, em %) |\n" +
        "| `$TIPO_COUNTER` | 13 | Reação aplicada no oponente quando um golpe com `$ATK_FLAGS` 2 (contra-ataque) é atingido. 0–29 = reação de chão do gamedata (HDAT 10 + valor); 30–59 = reação aérea/agarre deste personagem (HDAT 14 + valor − 30) |\n" +
        "\n" +
        "O dano mínimo de qualquer hit é 2.\n" +
        "\n" +
        "### Identidade\n" +
        "\n" +
        "| Campo | Byte | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ID_ATRIBUTO` | 9 | Afinidade do personagem: 0 = bem, 1 = mal, 2 = neutro. Define o ícone nas telas de status/seleção e dá o bônus de afinidade (HDAT 155 do gamedata) para cada carta da mesma afinidade no deck |\n" +
        "| `$PONTOS_EQUIPAR_CARTAS` | 17 | Custo máximo das cartas que ele pode equipar. Custo total = byte + soma da coluna 21 do HDAT 7 |\n" +
        "| `$ANIM_ENTRADA` | 16 | Animação tocada quando se tenta usar um golpe \"selado\". Hoje nenhum golpe fica selado (o jogo nunca liga o selo), então **o campo não tem efeito** |\n" +
        "| `$DESCONHECIDO_2`, `$DESCONHECIDO_15` | 2, 15 | Não são lidos |\n" +
        "\n" +
        "## HDAT 4 — `$ATRIBUTOS_DE_GOLPE`\n" +
        "\n" +
        "Tabela de short. Cada `$GOLPE_N` é uma linha (o número do golpe está na\n" +
        "tabela acima) com `$COL` valores; o tamanho da linha é 2 × `$COL` bytes. O\n" +
        "`char01` original tem 23 colunas. Animações = ANO do DAC do personagem;\n" +
        "0 = nenhuma.\n" +
        "\n" +
        "### Início e flags\n" +
        "\n" +
        "| Campo | Offset | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ATK_FLAGS` | 0 | Bits de comportamento (some os valores) — ver abaixo |\n" +
        "| `$ANIM_INICIAL` | 2 | Animação que o golpe toca. Se o personagem já estava num especial (ANO ≥ 40) e este também começa em ANO ≥ 40, aparece o efeito visual de cancelamento (menos nos golpes 9–12) |\n" +
        "\n" +
        "Bits de `$ATK_FLAGS`:\n" +
        "\n" +
        "| Bit | Efeito |\n" +
        "|---|---|\n" +
        "| 1 | Projétil **perfurante**: continua depois de acertar (sem o bit, para no primeiro hit) |\n" +
        "| 2 | **Contra-ataque**: se um golpe do oponente atingir este golpe, o oponente sofre a reação de `$TIPO_COUNTER` (HDAT 3) |\n" +
        "| 4 | Solta **3 projéteis** por ponto de disparo (animações `$PROJETIL_ANIM_*`, +1 e +2) |\n" +
        "| 8 | No início do golpe, o atacante **aparece colado** no oponente (a `$DISTANCIA_AGARRE` dele, na mesma altura) |\n" +
        "| 16 | Os dois lutadores **atravessam** um ao outro durante o golpe (sem empurrão de corpo) |\n" +
        "| 32 | O projétil **nasce na posição do oponente** |\n" +
        "| 64 | O projétil nasce **no chão, embaixo do oponente** |\n" +
        "| 128 | Igual ao 8, mas só **depois que o golpe acerta** (teletransporte de perseguição) |\n" +
        "\n" +
        "Os golpes 14 e 15 sempre ligam a fúria, independente das flags.\n" +
        "\n" +
        "### Projétil\n" +
        "\n" +
        "O projétil sai das hitboxes tipo 0 do DAC do atacante (no máximo 8 projéteis\n" +
        "ao mesmo tempo).\n" +
        "\n" +
        "| Campo | Offset | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$PROJETIL_ANIM_NORMAL` | 4 | Animação do projétil quando os dois estão mais ou menos na mesma altura (0 = o golpe não solta projétil) |\n" +
        "| `$PROJETIL_ANIM_ABAIXO` | 34 | Usada quando o atacante está 51 px ou mais **abaixo** do oponente (tiro para cima) |\n" +
        "| `$PROJETIL_ANIM_ACIMA` | 36 | Usada quando o atacante está 37 px ou mais **acima** do oponente (tiro para baixo) |\n" +
        "| `$PROJETIL_ANIM_AO_ACERTAR` | 6 | Animação do projétil quando ele acerta (explosão). 0 = o projétil some |\n" +
        "| `$PROJETIL_ANIM_ACERTAR_GLOBAL` | 38 | Animação do projétil quando ele acerta mas o oponente **não entra em reação de dano** (por exemplo, defendeu). 0 = usa a de cima |\n" +
        "| `$PROJETIL_COLISAO_CHAO` | 40 | Animação do projétil quando ele bate no chão. 0 = usa a de `$PROJETIL_ANIM_AO_ACERTAR` (ou some, se ela também for 0) |\n" +
        "| `$HOMING` | 26 | **0 = persegue** o oponente; qualquer outro valor = vai reto. O nome engana: 1 desliga a perseguição |\n" +
        "| `$HOMING_VELOCIDADE` | 28 | Força da perseguição (quanto o projétil curva por frame). Só vale com `$HOMING: 0` |\n" +
        "| `$MAX_REPETICOES` | 30 | Só nos golpes 0, 3 e 6 (especial 1): quantos tiros extras o jogador pode soltar apertando o botão de carregar nos frames do DAC com a flag de OPT 512. O golpe alterna as animações 40 e 41 e termina na 42. 0 = sem repetição |\n" +
        "\n" +
        "### Subprojétil\n" +
        "\n" +
        "Cada hitbox tipo 0 do próprio projétil pode soltar um segundo projétil:\n" +
        "\n" +
        "| Campo | Offset | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$SUBPROJETIL_ANIM_ID` | 18 | Animação do subprojétil (0 = não solta) |\n" +
        "| `$SUBPROJETIL_ANIM_AO_ACERTAR` | 20 | Animação quando o subprojétil acerta (0 = some) |\n" +
        "| `$SUBPROJETIL_COLISAO_CHAO` | 42 | Animação quando ele bate no chão |\n" +
        "\n" +
        "O subprojétil usa o `$PROJETIL_ANIM_ACERTAR_GLOBAL` do golpe para o caso \"acertou sem reação\".\n" +
        "\n" +
        "### Atacante depois do hit\n" +
        "\n" +
        "| Campo | Offset | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ANIM_AO_ACERTAR` | 32 | Quando o golpe conecta e o oponente entra em reação, o atacante troca para esta animação (continuação do golpe). 0 = segue normal |\n" +
        "| `$ANIM_SEQ_CHAO` | 10 | Atacante no chão: depois de conectar, vai para esta animação (próximo trecho do combo) |\n" +
        "| `$ANIM_END_CHAO` | 12 | Atacante no chão: depois de `$ANIM_SEQ_CHAO`, vai para esta (final do combo) |\n" +
        "| `$ANIM_SEQ_AR` / `$ANIM_END_AR` | 14 / 16 | O mesmo, com o atacante no ar |\n" +
        "| `$POUSO_AO_ERRAR` | 8 | Se o atacante cai no chão durante o golpe **sem ter acertado**, toca esta animação. 0 = encerra o golpe e toca a aterrissagem padrão (ANO 13) |\n" +
        "| `$POUSO_AO_ACERTAR` | 22 | O mesmo, quando o golpe **já acertou** |\n" +
        "\n" +
        "### Oponente\n" +
        "\n" +
        "| Campo | Offset | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$OPONENTE_ANIM_SENDO_AGARRADO` | 24 | Enquanto o oponente está numa reação de dano (ANO 40–62) causada por este golpe, uma animação **do DAC do atacante** é desenhada por cima dele (efeito de agarre, explosão presa no corpo etc.). 0 = nada |\n" +
        "\n" +
        "### Desconhecidos\n" +
        "\n" +
        "`$DESCONHECIDO_44`, `$DESCONHECIDO_46`, ... são as colunas depois do offset\n" +
        "42. O jogo não as lê.\n" +
        "\n" +
        "## HDAT 5 — `$PONTUACAO_POR_GOLPE`\n" +
        "\n" +
        "Tabela de short com 40 valores, um para cada animação especial de 27 a 66\n" +
        "(`$ANO_27` … `$ANO_66`). Ao acertar o oponente com uma dessas animações, o\n" +
        "jogador ganha esses pontos no placar. Com combo de 2 hits ou mais, os pontos\n" +
        "ainda são multiplicados pela linha `$PONTOS_POR_COMBO` do HDAT 5 do gamedata e\n" +
        "arredondados para cima, para a centena. 0 = sem pontos.\n" +
        "\n" +
        "## HDAT 6 — sequências da CPU (`$RAW`)\n" +
        "\n" +
        "O extractor detalhado está desligado, então sai em `$RAW`. É uma tabela de\n" +
        "byte com 10 colunas: cada linha é uma sequência (o número usado pelo HDAT 0 e\n" +
        "pelo HDAT 42).\n" +
        "\n" +
        "| Coluna | O que faz |\n" +
        "|---|---|\n" +
        "| 0, 2, 4, 6 | Os 4 passos da sequência: comando + 1 (0 = fim da sequência). Comando 1 = defender; comando 24 = levantar voo (só se o personagem voa e está no chão); os outros são combinações de botões/direções da tabela `SeqKey` do `TCBManajer` |\n" +
        "| 1, 3, 5, 7 | Duração de cada passo, em frames |\n" +
        "| 8 | Sequência seguinte (encadeia). Vale só para a CPU |\n" +
        "| 9 | 1 = aborta a sequência se o passo for um ataque e não houver janela de cancelamento |\n" +
        "\n" +
        "Sequências com papel fixo: 55 = ativar a fúria; 7 e 20 = só com a fúria\n" +
        "ativa (senão viram a 8); 36 = usada pela CPU num modo especial com 100 de ki.\n" +
        "\n" +
        "## HDAT 7 — `$CRESCIMENTO_POR_NIVEL`\n" +
        "\n" +
        "Tabela de byte, 99 linhas (níveis 0 a 98) × 22 colunas. O status num nível N\n" +
        "é a base do HDAT 3 + **a soma das linhas 0 até N**.\n" +
        "\n" +
        "| Coluna | Soma em | Aparece na tela de \"subiu de nível\" como |\n" +
        "|---|---|---|\n" +
        "| 0 | HP (`$VIDA_INICIAL`) | — |\n" +
        "| 1 | ATK (`$ATK_BASE`) | ATK + valor × 1000 |\n" +
        "| 2 | Ki máximo (`$KI_MAXIMO_BASE` × 100) | Ki + valor |\n" +
        "| 3 | DEF (`$DEFESA_BASE`) | DEF + valor × 1000 |\n" +
        "| 21 | Custo de cartas (`$PONTOS_EQUIPAR_CARTAS`) | Custo + valor |\n" +
        "\n" +
        "As colunas 4–20 não são lidas. O nível máximo é 99 (linha 98).\n" +
        "\n" +
        "## HDAT 8 a 12 — sem leitura\n" +
        "\n" +
        "O jogo não lê esses HDATs (saem em `$RAW`).\n" +
        "\n" +
        "## HDAT 13 — `$ROTEIRO_CUTSCENE`\n" +
        "\n" +
        "Tabela de short, 8 valores por linha. Cada `$EVT_N` é um evento visual\n" +
        "disparado por uma hitbox **tipo 20** no DAC do personagem. A **largura** da\n" +
        "hitbox é o número do evento e o X/Y é onde ele aparece. É assim que os golpes\n" +
        "mostram efeitos de tela, mudam a câmera ou prendem o oponente.\n" +
        "\n" +
        "| Campo | Índice | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ALVO` | 0 | De onde vem a animação: 0 = efeitos do sistema, 1 = DAC do próprio personagem, outro = objeto do cenário |\n" +
        "| `$DURACAO` | 1 | Quantos frames o evento dura enquanto o personagem continua na mesma animação. 0 = até a animação do personagem mudar |\n" +
        "| `$Z_LAYER` | 2 | Prioridade de desenho: 0 → 200 (eventos 8 e 9: 10); 1 → 17 (evento 8: 19, evento 9: 21); outro → 400. Para comparar: os personagens ficam entre 130 e 154 |\n" +
        "| `$ANIM_ID` | 3 | Animação com o personagem virado para a direita. 0 = o evento não desenha nada (mas ainda aplica `$TRAVAR_VIT` e `$FLAG`) |\n" +
        "| `$ANIM_FLIP` | 4 | Animação com o personagem virado para a esquerda |\n" +
        "| `$CAM_MODE` | 5 | 0 = segue o ponto da hitbox (e o zoom); 1 = efeito de tela cheia; outro = fixo no canto (0,0) |\n" +
        "| `$TRAVAR_VIT` | 6 | Diferente de 0 = congela o oponente enquanto o evento dura |\n" +
        "| `$FLAG` | 7 | Efeito extra (tabela abaixo) |\n" +
        "\n" +
        "Valores de `$FLAG`:\n" +
        "\n" +
        "| Valor | Efeito |\n" +
        "|---|---|\n" +
        "| 0 | nada |\n" +
        "| 1 | adia o fim do round por KO enquanto o evento roda (o golpe termina mesmo se alguém zerar a vida) |\n" +
        "| 2 | se o oponente já está sem vida, marca-o como nocauteado |\n" +
        "| 3 | a câmera desliza até o atacante (o evento termina quando chega) |\n" +
        "| 4 | desliga o scroll automático da câmera para o atacante |\n" +
        "| 5 | a câmera passa a seguir o oponente |\n" +
        "| 6 | força parede nas bordas da tela |\n" +
        "| 7 / 8 / 9 | oponente / atacante / os dois podem sair pelas bordas |\n" +
        "| 10 / 11 / 12 | joga o oponente para fora da tela à esquerda / à direita / em cima |\n" +
        "| 13 / 14 / 15 | joga o atacante para fora da tela à esquerda / à direita / em cima |\n" +
        "\n" +
        "Os valores 3, 4, 7 e 9 também desligam o scroll automático do atacante; 8 e 9\n" +
        "desligam o do oponente.\n" +
        "\n" +
        "## HDAT 14 a 33 — `$REACAO_DANO_N`\n" +
        "\n" +
        "Reações **aéreas e de agarre** que este personagem aplica no oponente. Um\n" +
        "golpe do DAC escolhe a reação pelo código de dano do hit: códigos menores que\n" +
        "30 usam as reações de chão do **gamedata** (HDAT 10 + código); códigos de 30\n" +
        "em diante usam estas, do **atacante**: HDAT 14 + (código − 30). O oponente\n" +
        "\"veste\" a reação de quem bateu.\n" +
        "\n" +
        "Cada `$FR_N` é um passo da reação, em ordem; o oponente passa para o passo\n" +
        "seguinte quando a duração acaba. Os valores são bytes com sinal.\n" +
        "\n" +
        "| Campo | Coluna | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$DUR_C` / `$DUR_A` | 0 / 1 | Duração do passo, em frames, com o oponente no chão / no ar. Se uma duração 255 chega ao fim, a reação vira a reação padrão 2 (na prática, 255 = espera o `$COMP`) |\n" +
        "| `$ANO` | 2 | Animação do oponente (do DAC **dele**). Negativo = a mesma animação, com o oponente virado para o outro lado |\n" +
        "| `$XSPEED` `$YSPEED` `$XACCEL` `$YACCEL` | 3–6 | Velocidade e aceleração com o oponente no chão. 10 = 1 px por frame. X negativo empurra o oponente para trás (para longe de quem bateu); Y negativo sobe. A aceleração é somada à velocidade a cada frame (a velocidade X para quando troca de sinal). −1 = mantém o valor atual; −2 = metade do valor anterior |\n" +
        "| `$XSPEED_A` … `$YACCEL_A` | 7–10 | O mesmo, com o oponente no ar. Num **agarre**, `$XSPEED_A`/`$YSPEED_A` são a posição do oponente relativa ao atacante, em px |\n" +
        "| `$COMP` | 11 | O que acontece ao tocar o chão ou a parede (tabela abaixo) |\n" +
        "| `$WALL` | 12 | 2 ou 3 = o `$COMP` reage à **parede** do cenário em vez do chão; ao cair no chão, o oponente quica (reação 12) |\n" +
        "| `$VOZ` | 13 | Som tocado no início do passo (1–79 = som global − 1; 80+ = voz do personagem; 0 = nada) |\n" +
        "\n" +
        "Valores de `$COMP`:\n" +
        "\n" +
        "| Valor | Efeito |\n" +
        "|---|---|\n" +
        "| 0 | Nada especial; ao passar do chão, fica no chão |\n" +
        "| 11 | Ao tocar o chão (ou parar de subir), cai deitado (knockdown) |\n" +
        "| 12 | Ao tocar o chão, se o oponente apertou o botão de carregar durante a reação, levanta na hora (recuperação); senão, passa para a reação 12 |\n" +
        "| 1–49 (outros) | Ao tocar o chão (ou a parede, com `$WALL` 2/3) passa para a reação de número `$COMP`. Se o oponente já está no chão, isso acontece na hora |\n" +
        "| 50 ou mais (ex.: 255) | Ao cair no chão, termina a reação com a aterrissagem padrão |\n" +
        "\n" +
        "Quando todos os passos acabam: se o oponente ainda está subindo ou descendo,\n" +
        "vira a reação 13 (queda), ou a 2 se estiver sem vida; se está parado, volta a\n" +
        "lutar (ou cai, se estiver sem vida).\n" +
        "\n" +
        "## HDAT 34 — `$DISPUTA_DE_PODER`\n" +
        "\n" +
        "| Campo | Linha | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ANIM_GATILHO` | 0 | Quando este personagem está nesta animação (normalmente o raio de energia), pode começar a **disputa de poder** (o minigame de toques). Começa se o oponente estiver a 240 px ou mais, parado/andando/no ar (ANO 0–12 ou 81) e com 100 de ki ou mais. Se o oponente for a CPU, ela aceita com a chance da linha 18 do HDAT 3 do gamedata |\n" +
        "| `$TEM_CUTSCENE` | 1 | Para o **vencedor** da disputa: 0 = mostra a imagem final (ANO 280 do DAC dele) por 90 frames; outro valor = pula a imagem. O mesmo vale para o fim da cena do golpe Extreme. O nome engana: 0 é que mostra a imagem |\n" +
        "\n" +
        "A tela da disputa usa as animações 265–269 e 278–279 do DAC dos dois\n" +
        "personagens.\n" +
        "\n" +
        "## HDAT 35 — `$DANO_DE_EVENTOS_CUTSCENE`\n" +
        "\n" +
        "Tabela de short: `$DANO` (coluna 0) e `$ANIM` (coluna 1). É o dano dos\n" +
        "eventos que não vêm de um hit do DAC. Ele passa pela mesma conta de ATK/DEF\n" +
        "(e fúria) de um golpe normal: dano final = `$DANO` × ATK ÷ 100 × (100 − DEF) ÷ 100.\n" +
        "\n" +
        "| Linha | Evento |\n" +
        "|---|---|\n" +
        "| 0 | Dano no perdedor da disputa de poder (HDAT 34) |\n" +
        "| 1 | Dano no fim do rush de cartas |\n" +
        "| 2 | Dano de cada hit da cena do golpe Extreme |\n" +
        "| 2–5 | `$ANIM` = animação de cada hit da cena do Extreme (até 4 hits) |\n" +
        "| 6 | Dano do golpe final da cena do Extreme |\n" +
        "\n" +
        "As outras combinações não são lidas.\n" +
        "\n" +
        "## HDAT 36 — `$SISTEMA_DE_VOZES`\n" +
        "\n" +
        "Um byte por situação: o som tocado. 0 = sem voz; 1–79 = som global (o jogo\n" +
        "toca o número − 1); 80 ou mais = voz do próprio personagem.\n" +
        "\n" +
        "| Campo | Linha | Quando toca |\n" +
        "|---|---|---|\n" +
        "| `$CUTSCENE_INICIO` | 2 | Início da cena do golpe Extreme |\n" +
        "| `$CUTSCENE_HIT_1` … `_4` | 3–6 | Cada hit da cena do Extreme |\n" +
        "| `$CUTSCENE_POS_EVENTO` | 7 | Depois do evento especial |\n" +
        "| `$SELECAO_DE_CARTAS` | 8 | Ao entrar na escolha de cartas (só o jogador 1) |\n" +
        "| `$DISPUTA_DE_KI` | 9 | No início da disputa de ki |\n" +
        "| `$EVENTO_10` | 10 | No fim da disputa de ki |\n" +
        "| `$DESCONHECIDO_0`, `_1`, `_11`… | 0, 1, 11+ | Não são lidos |\n" +
        "\n" +
        "## HDAT 37 — `$DANO_NO_ESTADO_DE_FURIA`\n" +
        "\n" +
        "Porcentagem aplicada ao dano que este personagem causa **enquanto a fúria\n" +
        "está ativa** (100 = normal, 150 = +50%). Vale para os golpes e para o dano\n" +
        "dos eventos (HDAT 35).\n" +
        "\n" +
        "## HDAT 38 — `$CARTA_RECOMPENSA_ARCADE`\n" +
        "\n" +
        "Número da carta (short) que o jogador ganha ao **passar do último estágio**\n" +
        "do arcade com este personagem. Ganhar uma carta que já tem sobe o nível dela\n" +
        "em 1; se ela já está no nível máximo (10), vira tickets (HDAT 165 do\n" +
        "gamedata).\n" +
        "\n" +
        "## HDAT 39 — `$CENARIO_PADRAO`\n" +
        "\n" +
        "Número do cenário usado na luta **quando este personagem é o oponente**\n" +
        "(CPU). No modo 8 vale o do jogador 2.\n" +
        "\n" +
        "## HDAT 40 — `$PROGRESSAO_CAMPANHA`\n" +
        "\n" +
        "Valores = número do personagem + 1 (0 = vazio).\n" +
        "\n" +
        "| Campo | Linha | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ARCADE` | 0 | Ordem dos oponentes no arcade. O jogo pula personagens não liberados e o próprio chefe. Quando chega num 0 (ou no estágio 6 no fácil), a próxima luta é a do chefe |\n" +
        "| `$BOSS_CANDIDATOS` | 1 | Candidatos a chefe final: o primeiro que já estiver liberado é o chefe |\n" +
        "| `$DESCONHECIDO` | 2 | Não é lido |\n" +
        "| `$HISTORIA` | 3 | Oponentes da **missão inicial** (antes de concluí-la): um por estágio, 8 estágios fixos |\n" +
        "\n" +
        "## HDAT 41 — `$PERSONAGENS_DESBLOQUEAVEIS`\n" +
        "\n" +
        "Ao terminar o arcade com este personagem, libera os personagens da linha da\n" +
        "dificuldade jogada (`$EASY`, `$NORMAL`, `$HARD`). Valores = número + 1;\n" +
        "0 = fim da lista. Cada um liberado mostra a linha 1 do HDAT 2 dele.\n" +
        "\n" +
        "## HDAT 42 — `$COMANDOS_TOUCH`\n" +
        "\n" +
        "Tabela de short, 3 colunas. Define o que acontece quando o jogador **toca no\n" +
        "oponente** (a até 48 px dele). O personagem passa a executar sozinho uma\n" +
        "sequência do HDAT 6. Só vale para o jogador humano, fora de um ataque.\n" +
        "\n" +
        "A linha é escolhida assim:\n" +
        "\n" +
        "```\n" +
        "linha = estado + distância + (20 se ki >= 100)\n" +
        "estado:    0 = no chão, 40 = pulando, 80 = voando\n" +
        "distância: 0 = menos de 80 px, 5 = 80–159 px, 10 = 160–319 px, 15 = 320 px ou mais\n" +
        "```\n" +
        "\n" +
        "| Coluna | O que faz |\n" +
        "|---|---|\n" +
        "| 0 | Número da sequência do HDAT 6 que o personagem executa (0 = o toque não faz nada) |\n" +
        "| 1 | Nas linhas de continuação: ki exigido, × 100 |\n" +
        "| 2 | Não é lida |\n" +
        "\n" +
        "Antes da tabela, o jogo testa a fúria. Com a barra de fúria cheia (e a fúria\n" +
        "desligada), o toque ativa a fúria (sequência 55). Com a fúria ativa e 200 de\n" +
        "ki, ele usa a sequência 7.\n" +
        "\n" +
        "Continuação: depois do primeiro toque, as linhas seguintes (N+1, N+2, …, até\n" +
        "4) são combos. Cada uma sai se o jogador tocar a até 88 px do oponente\n" +
        "durante a janela da hitbox tipo 2 do DAC, com o oponente em reação de dano e\n" +
        "com o ki da coluna 1. Uma linha de continuação com coluna 0 = 0 encerra o\n" +
        "combo.\n"
    };

    /** gamedata.md */
    static final String[] GAMEDATA = {
        "# gamedata.md — HDATs do `gamedata.pac`\n" +
        "\n" +
        "Guia de cada HDAT do `gamedata` (pasta `gamedata/`, arquivo `000.dac.txt`):\n" +
        "para que serve, onde o jogo lê, o que cada campo muda no gameplay e o que já\n" +
        "se sabe dos campos sem nome. Tudo foi conferido no `TCBManajer.java` (que\n" +
        "chama o gamedata de `pGameData`).\n" +
        "\n" +
        "O gamedata vale para **o jogo inteiro**: IA da CPU, progressão do arcade,\n" +
        "combos, reações de dano no chão, formações, cartas, loja, esferas do dragão,\n" +
        "tickets e o save inicial. O que é de cada personagem fica no `charNN`\n" +
        "(ver `binChar.md`).\n" +
        "\n" +
        "## Antes de editar\n" +
        "\n" +
        "- **Unidades.** A tela do jogo tem 480 px de largura e o chão fica em\n" +
        "  y = 240. \"Frame\" é um quadro da lógica do jogo.\n" +
        "- **ANO** é o número de uma animação no DAC de um personagem.\n" +
        "- **`CPULevel`** é a força da CPU (0 = mais fraca, 7 = mais forte). Ela vem\n" +
        "  do HDAT 4 (arcade) e escolhe a coluna de vários HDATs.\n" +
        "- **Dificuldade**: 0 = fácil, 1 = normal, 2 = difícil.\n" +
        "- **Personagem N** = o número do personagem no jogo. Onde o campo diz\n" +
        "  \"personagem + 1\", 0 é vazio.\n" +
        "- **Afinidade**: 0 = bem, 1 = mal, 2 = neutro (a mesma do `$ID_ATRIBUTO` do\n" +
        "  personagem e do `$AFINIDADE` das cartas).\n" +
        "- Tabelas de byte vão de 0 a 255; de short, de -32768 a 32767.\n" +
        "- **`$RAW`**: HDATs sem formato detalhado saem como bytes crus. Os que o jogo\n" +
        "  nunca lê estão marcados \"sem leitura\".\n" +
        "- **Arcade (modo 0)**: o modo de lutas em sequência com 8 estágios. A\n" +
        "  **missão inicial** é a primeira campanha, antes de concluí-la; nela vários\n" +
        "  valores são fixos.\n" +
        "\n" +
        "## Índice\n" +
        "\n" +
        "| HDAT | Tag no `.txt` | Resumo |\n" +
        "|---|---|---|\n" +
        "| 0 | `$BONUS_EXP_DESEMPENHO` | EXP ganho no fim do estágio conforme o desempenho |\n" +
        "| 1 | `$RAW` | Sem leitura |\n" +
        "| 2 | `$DISTANCIAS_IA` | Distâncias \"perto/médio\" da CPU e alcance de defesa contra projéteis |\n" +
        "| 3 | `$IA_COMPORTAMENTO` | Chances (%) de a CPU defender, cancelar, contra-atacar, iniciar eventos |\n" +
        "| 4 | `$PROGRESSAO_ARCADE` | Nível e força da CPU e EXP extra por estágio |\n" +
        "| 5 | `$MULTIPLICADOR_COMBO` | Escala de dano e de pontos conforme o combo |\n" +
        "| 6 | `$RAW` | Sem leitura |\n" +
        "| 7 | `$TXT` | Textos desenhados com a fonte de sprite (\"READY\", \"KO!\", números…) |\n" +
        "| 8 | `$MAPA_FONTE` | Caractere → desenho da fonte de sprite |\n" +
        "| 9 | `$CONFIG_CENA` | Configuração das cenas da história |\n" +
        "| 10–39 | `$REACAO_DANO_GD_N` | Reações de dano **no chão** (as mais comuns) |\n" +
        "| 40 | `$RAW` | Sem leitura (vazio) |\n" +
        "| 41 | `$BP_POR_ESTAGIO` | BP ganho por estágio do arcade |\n" +
        "| 42 | `$BP_CHEFE` | BP ganho nos chefes do modo 1 |\n" +
        "| 43 | `$EXP_NEXT_LEVEL` | EXP para subir de nível |\n" +
        "| 44 | `$TXT` | Texto sem leitura |\n" +
        "| 45–49 | `$RAW` | Sem leitura |\n" +
        "| 50 | `$FORMACAO` | Formações de cartas: requisitos e qual tabela de efeito usar |\n" +
        "| 51–135 | `$EFEITO_FORMACAO_N` | O efeito de cada formação |\n" +
        "| 136 | `$LOJA_CARTAS` | Cartas à venda / na troca e o preço |\n" +
        "| 137, 138 | `$TXT` | Nome e descrição de cada formação |\n" +
        "| 139–144 | `$RAW` | Sem leitura |\n" +
        "| 145 | `$PERSONAGENS_EXCLUIDOS_CPU` | Sem efeito (a função que lê nunca é chamada) |\n" +
        "| 146 | `$RESULTADO_DISPUTA_KI` | Sequência de ícones para aceitar a disputa de poder |\n" +
        "| 147 | `$DIFICULDADE_DISPUTA` | Força da CPU nas disputas de ki e de poder |\n" +
        "| 148 | `$PADROES_BONUS_PUSH` | Quais botões da disputa de ki são bônus |\n" +
        "| 149 | `$TIERS_PODER_DECK` | Faixas de poder do deck |\n" +
        "| 150 | `$SCORE_EVENTOS_CUTSCENE` | Pontos dos eventos especiais |\n" +
        "| 151, 152 | `$RAW` | Sem leitura |\n" +
        "| 153 | `$ESFERAS_DRAGAO` | Condições para ganhar cada esfera do dragão |\n" +
        "| 154 | `$TXT` | Texto das missões e das condições das esferas |\n" +
        "| 155 | `$BONUS_AFINIDADE` | Bônus por carta da mesma afinidade do personagem |\n" +
        "| 156 | `$SEQUENCIA_PUSH` | Botões que aparecem na disputa de ki |\n" +
        "| 157 | `$DECKS_CPU` | Decks de cartas da CPU |\n" +
        "| 158 | `$DEF_CHARS` | Personagens liberados num save novo |\n" +
        "| 159 | `$RAW` | Sem leitura |\n" +
        "| 160 | `$CARTAS_INICIAIS` | Cartas de um save novo |\n" +
        "| 161 | `$SOUND` | Músicas de cada situação |\n" +
        "| 162 | `$TICKETS_INICIAIS` | Tickets de um save novo |\n" +
        "| 163 | `$INIT_CHAR_VALUES` | Nível, EXP e cartas de cada personagem num save novo |\n" +
        "| 164 | `$ESFERAS_INICIAIS` | Esferas do dragão de um save novo |\n" +
        "| 165 | `$TICKETS_POR_PARTIDA` | Tickets ganhos por luta |\n" +
        "| 166 | `$FLAGS_VIDA_ESPECIAL` | Jogador imortal / CPU com 1 de HP |\n" +
        "| 167 | `$MODO_DEBUG` | Liga o modo debug |\n" +
        "| 168, 169 | `$RAW` | Sem leitura (vazios) |\n" +
        "| 170–250 | `$CARTA_N` | As cartas (carta N = HDAT 170 + N) |\n" +
        "| 251–270 | `$RAW` | Sem leitura |\n" +
        "| 271–279 | `$TXT` | Textos dos idiomas 1–3 (não existem no `gamedata.pac` original) |\n" +
        "\n" +
        "---\n" +
        "\n" +
        "## HDAT 0 — `$BONUS_EXP_DESEMPENHO`\n" +
        "\n" +
        "Tabela de short, 4 colunas × 10 linhas. Vale no fim de cada estágio do\n" +
        "arcade, depois da missão inicial.\n" +
        "\n" +
        "O jogo calcula uma nota de desempenho com a vida que sobrou e o tempo gasto\n" +
        "(mais vida e menos tempo = nota maior). Depois procura da `$LINHA_9` para a\n" +
        "`$LINHA_0` a primeira linha em que a nota ≥ coluna 0 × 100.\n" +
        "\n" +
        "| Coluna | O que faz |\n" +
        "|---|---|\n" +
        "| 0 | Nota mínima (× 100) para esta linha valer |\n" +
        "| 1 / 2 / 3 | EXP ganho no fácil / normal / difícil |\n" +
        "\n" +
        "Ao EXP ainda se soma o `$EXP_EXTRA_*` do HDAT 4. Na missão inicial esta\n" +
        "tabela não vale: o EXP é o `$EXP_ESTAGIO_INICIAL` do HDAT 4.\n" +
        "\n" +
        "## HDAT 2 — `$DISTANCIAS_IA`\n" +
        "\n" +
        "Tabela de byte, 2 colunas: 0 = com o alvo no chão, 1 = no ar. Distâncias em px.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$GUARDA_PROJETIL` | A CPU tenta defender projéteis que chegam a menos desta distância (só a coluna 0 é lida) |\n" +
        "| `$PERTO` | Até esta distância a situação é \"perto\" |\n" +
        "| `$MEDIO` | Até esta distância é \"médio\"; acima, \"longe\" |\n" +
        "\n" +
        "\"Perto/médio/longe\" escolhe a linha da tabela de decisão da CPU (HDAT 0 de\n" +
        "cada personagem). Aumentar os valores faz a CPU agir como se estivesse mais\n" +
        "perto.\n" +
        "\n" +
        "## HDAT 3 — `$IA_COMPORTAMENTO`\n" +
        "\n" +
        "Tabela de byte, 8 colunas = `CPULevel` 0–7. Cada valor é uma **chance em %**.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$L01_CANCEL_GUARD_CONTRA_ATAQUE` | Chance de a CPU contra-atacar saindo da defesa |\n" +
        "| `$L02_CANCEL_COMBO` | Chance de a CPU cancelar um especial em outro (continuar o combo) |\n" +
        "| `$L03_GUARD` | Chance de a CPU defender um golpe |\n" +
        "| `$L04_BONUS_TEMPO_VITORIA` | Fora do arcade (e do modo 5): bônus de tempo quando você vence = tempo restante × valor ÷ 100 |\n" +
        "| `$L18_INICIAR_PUSH` | Chance de a CPU aceitar a disputa de poder que você iniciou |\n" +
        "| `$L19_CHANCE_RUSH` | Chance de a CPU iniciar um rush (o choque que leva à batalha de cartas ou à disputa de ki) |\n" +
        "| Os outros (`L00`, `L05`–`L17`) | Não são lidos |\n" +
        "\n" +
        "## HDAT 4 — `$PROGRESSAO_ARCADE`\n" +
        "\n" +
        "Tabela de byte, 8 colunas = estágio 0–7 (do 8º estágio em diante vale a\n" +
        "coluna 7).\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$NIVEL_IA_FACIL` / `_NORMAL` / `_DIFICIL` | `CPULevel` do oponente em cada estágio |\n" +
        "| `$NIVEL_EXTRA_FACIL` / `_NORMAL` / `_DIFICIL` | Nível do oponente = nível do seu personagem + este valor |\n" +
        "| `$EXP_EXTRA_FACIL` / `_NORMAL` / `_DIFICIL` | EXP somado ao do HDAT 0 no fim do estágio |\n" +
        "| `$NIVEL_FIXO_INICIAL` | Missão inicial: nível do oponente (fixo) |\n" +
        "| `$NIVEL_IA_FIXO_INICIAL` | Missão inicial: `CPULevel` do oponente |\n" +
        "| `$EXP_ESTAGIO_INICIAL` | Missão inicial: EXP ganho no estágio |\n" +
        "\n" +
        "## HDAT 5 — `$MULTIPLICADOR_COMBO`\n" +
        "\n" +
        "Tabela de short, 14 colunas: coluna = tamanho do combo − 2 (combo de 2 hits\n" +
        "= coluna 0; de 15 hits ou mais = coluna 13).\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$PONTOS_POR_COMBO` | % dos pontos de golpe especial (HDAT 5 do personagem) conforme o combo |\n" +
        "| `$VALOR_POR_COMBO` | **% do dano** de cada hit conforme o combo, ou seja, a escala de dano (100 = dano cheio). Se a conta der 0, o hit tira 1 |\n" +
        "\n" +
        "Exemplo: `$VALOR_POR_COMBO: [100, 100, 100, 100, 75, …]` → do 6º hit em\n" +
        "diante o dano cai para 75%. Valores maiores deixam os combos longos mais\n" +
        "fortes.\n" +
        "\n" +
        "## HDAT 7 — `$TXT` (textos da fonte de sprite)\n" +
        "\n" +
        "Strings desenhadas com a fonte de sprite do jogo (`CREATE_STRING`): \"LV\",\n" +
        "\"BATTLE\", \"FINAL ROUND\", \"READY\", \"FIGHT!!\", \"KO!\", \"TIME OVER\", \"YOU WIN\",\n" +
        "os números (`$T_13` = \"0\" … `$T_22` = \"9\") etc. O jogo pede a string pelo\n" +
        "número `$T_N`. As cenas da história também tiram textos daqui.\n" +
        "\n" +
        "Cada caractere passa pelo HDAT 8. Use só caracteres que a fonte tem (ASCII);\n" +
        "espaços são pulados.\n" +
        "\n" +
        "## HDAT 8 — `$MAPA_FONTE`\n" +
        "\n" +
        "128 valores: a coluna é o código do caractere (ASCII) e o valor é o desenho\n" +
        "(padrão) da fonte de sprite. Ex.: coluna 48 (`'0'`) → 0, coluna 65 (`'A'`) →\n" +
        "10. Para trocar um caractere da fonte, aponte para outro padrão.\n" +
        "\n" +
        "## HDAT 9 — `$CONFIG_CENA`\n" +
        "\n" +
        "Tabela de short. Cada linha é uma cena da história/demonstração (modo 808).\n" +
        "\n" +
        "| Coluna | O que faz |\n" +
        "|---|---|\n" +
        "| 0 | Pack de objetos da cena: carrega o número valor + 1 no slot 5 |\n" +
        "| 1 | Cenário: −1 = nenhum/mantém; 0 = o cenário atual + 1; outro = esse cenário |\n" +
        "| 2 | Música: número + 1 (−1 = não troca). As músicas 0, 2 e 8 tocam em loop |\n" +
        "| 3 / 4 | Animação inicial do personagem 1 / 2 na cena |\n" +
        "| 5 | Parâmetro da cena usado no modo 810 (não confirmado) |\n" +
        "| 6 em diante | Pares X/Y de posição dos objetos de efeito da cena (modo 816) |\n" +
        "\n" +
        "## HDAT 10 a 39 — `$REACAO_DANO_GD_N`\n" +
        "\n" +
        "Reações de dano **no chão**, as que a maioria dos golpes usa. O código de\n" +
        "dano do hit no DAC escolhe a reação: código < 30 → HDAT 10 + código (estas);\n" +
        "código 30 ou mais → reação aérea/agarre do personagem atacante\n" +
        "(`binChar.md`, HDAT 14–33).\n" +
        "\n" +
        "Os campos são **os mesmos** do `$REACAO_DANO_N` do personagem: `$DUR_C`/\n" +
        "`$DUR_A` (duração no chão/ar), `$ANO` (animação de quem apanhou; negativo =\n" +
        "virada), física (`$XSPEED` … `$YACCEL_A`, 10 = 1 px por frame, −1 = mantém,\n" +
        "−2 = metade), `$COMP` (o que acontece ao tocar o chão/parede), `$WALL` e\n" +
        "`$VOZ`. A explicação completa de cada campo e dos valores de `$COMP` está em\n" +
        "`binChar.md`.\n" +
        "\n" +
        "Reações com papel fixo no código:\n" +
        "\n" +
        "| Reação | Uso |\n" +
        "|---|---|\n" +
        "| 2 | Reação \"padrão\": usada ao acabar uma duração 255 e ao terminar um agarre |\n" +
        "| 12 | Quique no chão (com `$WALL` 2/3) |\n" +
        "| 13 | Queda depois de uma reação que termina no ar |\n" +
        "| 29 | Substitui a reação 2 quando a vida acaba |\n" +
        "\n" +
        "As animações (`$ANO`) são do DAC de **quem apanha**, então todos os\n" +
        "personagens precisam ter as animações usadas aqui.\n" +
        "\n" +
        "## HDAT 41 — `$BP_POR_ESTAGIO`\n" +
        "\n" +
        "Linha = estágio do arcade, coluna = dificuldade. BP ganho no fim do estágio =\n" +
        "valor × 10 (no máximo 999000 por estágio). Vale depois da missão inicial.\n" +
        "\n" +
        "O original só tem 6 linhas, então os estágios 6 e 7 leem o HDAT seguinte.\n" +
        "Para controlar esses estágios, adicione as linhas 6 e 7.\n" +
        "\n" +
        "## HDAT 42 — `$BP_CHEFE`\n" +
        "\n" +
        "No modo 1, a cada 5 estágios (o chefe): BP = `$LINHA_(estágio ÷ 5)` × 10.\n" +
        "\n" +
        "## HDAT 43 — `$EXP_NEXT_LEVEL`\n" +
        "\n" +
        "EXP para subir de nível. Cada `$LVL_N` vale para os níveis N a N+4 (o jogo lê\n" +
        "a linha nível ÷ 5). O EXP que sobra passa para o próximo nível. O nível\n" +
        "máximo é 99.\n" +
        "\n" +
        "## HDAT 50 — `$FORMACAO`\n" +
        "\n" +
        "Tabela de byte, 15 colunas. Cada `$LINHA_N` é uma formação (a `$LINHA_0` não\n" +
        "é usada). O nome e a descrição da formação N estão na linha N dos HDATs 137\n" +
        "e 138.\n" +
        "\n" +
        "| Campo | Coluna | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$TABELA_EFEITO` | 0 | **Número do HDAT** com o efeito da formação (51–135). 0 = formação desligada |\n" +
        "| `$TIPO_REQUISITO` | 1 | 0 = por afinidade (`$REQ_AFINIDADE`); outro = por cartas específicas (`$CARTAS_EXIGIDAS`) |\n" +
        "| `$REQ_AFINIDADE` | 2–4 | Mínimo de cartas de afinidade [bem, mal, neutro] no deck |\n" +
        "| `$CARTAS_EXIGIDAS` | 5–12 | Até 8 números de carta, que precisam estar todas no deck (0 = fim da lista). Uma carta já usada por outra formação de cartas não conta de novo |\n" +
        "| `$DESCONHECIDO` | 13–14 | Não são lidos |\n" +
        "\n" +
        "As formações são testadas em ordem. Das formações **por afinidade**, só a\n" +
        "primeira que bate vale. Por isso as mais exigentes (8 cartas) vêm antes das\n" +
        "mais fáceis (4 cartas).\n" +
        "\n" +
        "No `gamedata` original as linhas 1–61 usam os HDATs 51–111 e as linhas 62–84\n" +
        "estão desligadas (`$TABELA_EFEITO: 0`). Os HDATs 112–135 não são usados por\n" +
        "nenhuma formação: são slots livres para formações novas (preencha uma linha\n" +
        "desligada, aponte para um deles e escreva o nome/descrição nos HDATs 137/138).\n" +
        "\n" +
        "## HDAT 51 a 135 — `$EFEITO_FORMACAO_N`\n" +
        "\n" +
        "O efeito de uma formação: 1 coluna × 22 linhas, das quais o jogo lê 6. Os\n" +
        "efeitos se somam aos das cartas.\n" +
        "\n" +
        "| Campo | Linha | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$ATAQUE` | 3 | Soma ao ATK do personagem (% do dano), igual ao `$ATK` de uma carta |\n" +
        "| `$DEFESA` | 6 | Soma à DEF (% de redução do dano recebido) |\n" +
        "| `$BONUS_KI` | 7 | Ki no início da luta. O `.txt` mostra × 10 (o valor real de ki); limitado ao ki máximo |\n" +
        "| `$BONUS_BARRA_KI` | 8 | Barras extras de ki máximo (1 = +100) |\n" +
        "| `$FURIA` | 16 | Se a soma de cartas e formações for diferente de 0, a luta começa com a **fúria permanente** |\n" +
        "| `$RUSH_RED` | 20 | Sem efeito (o jogo nunca usa) |\n" +
        "| `$LINHA_N` (outras) | — | Não são lidas |\n" +
        "\n" +
        "O número no nome (`$EFEITO_FORMACAO_N`) é só uma etiqueta (HDAT − 51). Quem\n" +
        "liga a formação ao efeito é o `$TABELA_EFEITO` do HDAT 50.\n" +
        "\n" +
        "O HDAT 135 tem só 17 linhas (0–16) no original. Ele tem todos os campos que\n" +
        "o jogo usa (`$RUSH_RED`, a linha 20, não faz nada), então também funciona como\n" +
        "efeito.\n" +
        "\n" +
        "**HDATs 53 e 54.** No `gamedata` original eles são os efeitos das formações\n" +
        "3 e 4, e o extractor os mostra assim. O jogo também lê o 53 como tabela de\n" +
        "prêmio (fim de estágio do arcade), mas descarta o resultado. E lê o 54 como\n" +
        "tabela de transformação de personagem num modo que nunca é chamado. Por isso\n" +
        "só o papel de efeito de formação tem efeito no jogo. Num gamedata em que eles\n" +
        "não tenham 1 × 22, saem como `$PROB_RECOMPENSA` / `$TRANSFORMACAO`.\n" +
        "\n" +
        "## HDAT 136 — `$LOJA_CARTAS`\n" +
        "\n" +
        "Uma linha por carta (`$BUY_CARTA_N` = carta N; o jogo lê as cartas 1 a 89).\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$VISIVEL_MODO_A` | YES = a carta aparece na lista de **troca** |\n" +
        "| `$VISIVEL_MODO_B` | YES = a carta aparece na **loja** para comprar |\n" +
        "| `$PRECO_TICKETS` | Preço em tickets na loja. A compra sobe 1 nível da carta (até o 10) |\n" +
        "\n" +
        "## HDAT 137, 138 e 154 — textos (`$TXT`)\n" +
        "\n" +
        "| HDAT | Conteúdo |\n" +
        "|---|---|\n" +
        "| 137 | Nome de cada formação (linha = formação do HDAT 50) |\n" +
        "| 138 | Descrição de cada formação |\n" +
        "| 154 | Texto das missões e das condições das esferas do dragão (linha = rodada × 7 + esfera, a mesma numeração do HDAT 153; as linhas 0–6 são as missões iniciais) |\n" +
        "\n" +
        "Formato do texto: `\"...\"` grava em UTF-8 (o que o jogo lê), `SJIS\"...\"`\n" +
        "mantém o Shift_JIS original e `\\xHH` é um byte cru. `_` vira quebra de linha\n" +
        "na tela.\n" +
        "\n" +
        "## HDAT 145 — `$PERSONAGENS_EXCLUIDOS_CPU`\n" +
        "\n" +
        "Dois números de personagem que, pelo nome da função, não deveriam aparecer\n" +
        "como oponente da CPU. A função que lê este HDAT nunca é chamada: hoje **não\n" +
        "tem efeito**.\n" +
        "\n" +
        "## HDAT 146 — `$RESULTADO_DISPUTA_KI`\n" +
        "\n" +
        "Quando a **CPU** inicia a disputa de poder, você precisa tocar uma sequência\n" +
        "de ícones para aceitar. São 4 padrões, um sorteado a cada vez. O nome da tag\n" +
        "engana: é a sequência de ícones, não um resultado.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$ENTRADA_N` | Um padrão (o jogo sorteia entre 0 e 3) |\n" +
        "| `$NUM_ICONES` | Quantos ícones tocar |\n" +
        "| `$POSICOES` | X/Y de cada ícone (área de toque de 64 × 64 px), **na ordem** em que devem ser tocados |\n" +
        "| `$DESCONHECIDO` | Não é lido |\n" +
        "\n" +
        "Tocar fora de ordem ou passar de 110 frames cancela a disputa.\n" +
        "\n" +
        "## HDAT 147 — `$DIFICULDADE_DISPUTA`\n" +
        "\n" +
        "Tabela de byte, 10 colunas = `CPULevel` 0–9.\n" +
        "\n" +
        "| Campo | Onde | O que faz |\n" +
        "|---|---|---|\n" +
        "| `$L0_ESPERA_PROXIMO_BOTAO` | disputa de ki | A CPU marca 1 ponto a cada (valor + 0–5) frames: menor = CPU mais rápida |\n" +
        "| `$L3_INTERVALO_INPUT_CPU` | disputa de poder | A cada tantos frames a CPU empurra |\n" +
        "| `$L4_FORCA_MIN_CPU` / `$L5_FORCA_MAX_CPU` | disputa de poder | Força de cada empurrão da CPU: sorteada entre o mínimo e o máximo |\n" +
        "| `L1`, `L2`, `L6` | — | Não são lidas |\n" +
        "\n" +
        "## HDAT 148 — `$PADROES_BONUS_PUSH`\n" +
        "\n" +
        "Na disputa de ki, alguns botões são **bônus**. Coluna = `CPULevel` (no modo 8,\n" +
        "coluna 6). A cada leva de botões, o jogo sorteia uma linha inicial e avança\n" +
        "uma linha por botão: se o valor for maior que 0, aquele botão é bônus.\n" +
        "\n" +
        "## HDAT 149 — `$TIERS_PODER_DECK`\n" +
        "\n" +
        "Faixas de poder do deck, só depois da missão inicial ou no modo 8. O poder =\n" +
        "ATK das cartas × 1000 + `$ATQ` das cartas + bônus de afinidade + ATK das\n" +
        "formações × 1000. O tier é a maior faixa com limiar × 1000 ≤ poder.\n" +
        "\n" +
        "Com tier 1 ou mais, a cena do golpe Extreme mostra o painel de bônus e ganha\n" +
        "a sequência extra. O tier também entra no fim do rush de cartas e na disputa\n" +
        "de ki.\n" +
        "\n" +
        "## HDAT 150 — `$SCORE_EVENTOS_CUTSCENE`\n" +
        "\n" +
        "Pontos (short) dados nos eventos especiais.\n" +
        "\n" +
        "| Linha | Evento |\n" +
        "|---|---|\n" +
        "| 0 | Vencer o rush de cartas |\n" +
        "| 2 | Cada hit da cena do golpe Extreme |\n" +
        "| 4 | Fim da cena do Extreme |\n" +
        "| 6 | Vencer a disputa de poder |\n" +
        "| 1, 3, 5 | Não são lidas |\n" +
        "\n" +
        "## HDAT 153 — `$ESFERAS_DRAGAO`\n" +
        "\n" +
        "Condições para ganhar as esferas do dragão. São 11 rodadas (`$ROUND_N`: a\n" +
        "coleção 0 a 10) × 7 esferas. A checagem acontece ao terminar o arcade (último\n" +
        "estágio) ou no modo 8, para as esferas que ainda faltam na rodada atual. Na\n" +
        "missão inicial as esferas vêm uma por estágio, sem esta tabela.\n" +
        "\n" +
        "| Campo | Coluna | Condição (0 = não exige) |\n" +
        "|---|---|---|\n" +
        "| `$PLAYER_TYPE` | 0 | Jogar com o personagem valor − 1 (**sempre exigido**) |\n" +
        "| `$DIFICULDADE` | 1 | Dificuldade = valor − 1 |\n" +
        "| `$SCORE_MIN` | 2 | Placar ≥ valor × 100 |\n" +
        "| `$COMBO_MIN` | 3 | Combo máximo ≥ valor |\n" +
        "| `$SEM_DERROTA` | 4 | Não ter perdido nenhum estágio |\n" +
        "| `$TEMPO_LIMITE` | 5 | Terminar em menos de valor **segundos** |\n" +
        "| `$MODO_FLAG` | 8 | Diferente de 0 = só vale no modo 8 (contra outra pessoa); 0 = não vale no modo 8 |\n" +
        "| `$DESCONHECIDO_6`, `_7`, `_9`, `$NUM_ESFERA` | 6, 7, 9, 10 | Não são lidos |\n" +
        "\n" +
        "O texto de cada condição está no HDAT 154. Juntar as 7 esferas passa para a\n" +
        "rodada seguinte.\n" +
        "\n" +
        "## HDAT 155 — `$BONUS_AFINIDADE`\n" +
        "\n" +
        "Valor somado ao `$ATQ` e ao `$DEF_EXIBICAO` (milésimos) de cada carta cuja\n" +
        "afinidade é a mesma do personagem. Exemplo: 200 com 8 cartas = +1600 = +1 de\n" +
        "ATK e de DEF.\n" +
        "\n" +
        "## HDAT 156 — `$SEQUENCIA_PUSH`\n" +
        "\n" +
        "Os botões que aparecem na disputa de ki. A cada leva, o jogo usa a linha\n" +
        "`CPULevel` × 8 + (0 a 5, sorteado); no modo 8, o nível 6. Só 6 das 8 linhas de\n" +
        "cada nível são usadas.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$NUM_BOTOES` | Quantos botões nesta leva |\n" +
        "| `$POS_X` / `$POS_Y` | Posição do botão na tela |\n" +
        "| `$DELAY` | Frames até o botão aparecer |\n" +
        "| `$JANELA` | Tempo do botão na tela |\n" +
        "\n" +
        "## HDAT 157 — `$DECKS_CPU`\n" +
        "\n" +
        "Decks de cartas da CPU. Linha = sorteio de 0 a 15 + dificuldade × 16\n" +
        "(fácil 0–15, normal 16–31, difícil 32–47). Na missão inicial é a linha\n" +
        "48 + estágio.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$CARTAS` | 8 números de carta (0 = vazio) |\n" +
        "| `$NIVEIS` | O nível de cada carta (0–10) |\n" +
        "\n" +
        "## HDAT 158 — `$DEF_CHARS`\n" +
        "\n" +
        "Usado ao criar um save novo. Cada `$CHAR` libera um personagem.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$index` | Personagem liberado desde o início |\n" +
        "| `$isDef` | Diferente de 0 = também conta como comprado (só nas 13 primeiras linhas) |\n" +
        "\n" +
        "## HDAT 160 — `$CARTAS_INICIAIS`\n" +
        "\n" +
        "Cartas que um save novo recebe. `$ID` = número da carta; `$NIVEL` = quantas\n" +
        "vezes ela é recebida (1 = no nível 0, 2 = no nível 1, …).\n" +
        "\n" +
        "## HDAT 161 — `$SOUND`\n" +
        "\n" +
        "Música de cada situação. `$SND_N` = situação, `$id` = número da música (−1 =\n" +
        "nenhuma), `$loop` ≠ 0 = repete.\n" +
        "\n" +
        "O jogo pede as linhas 0, 1, 2, 12, 18, 19, 23–26, 43–47 e 28–34. As linhas\n" +
        "28–34 são as músicas de batalha: o jogo alterna entre as 7.\n" +
        "\n" +
        "## HDAT 162 — `$TICKETS_INICIAIS`\n" +
        "\n" +
        "Tickets de um save novo.\n" +
        "\n" +
        "## HDAT 163 — `$INIT_CHAR_VALUES`\n" +
        "\n" +
        "Um `$CHAR` por personagem (na ordem dos números), aplicado ao criar um save\n" +
        "novo.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$level` | Nível inicial (1 = o primeiro nível) |\n" +
        "| `$exp` | EXP inicial |\n" +
        "| `$card1` … `$card8` | As 8 cartas equipadas |\n" +
        "\n" +
        "## HDAT 164 — `$ESFERAS_INICIAIS`\n" +
        "\n" +
        "Esferas do dragão de um save novo: `$ROUND_N` = rodada, 7 colunas = esferas;\n" +
        "diferente de 0 = já vem com ela. Rodadas completas contam como concluídas.\n" +
        "\n" +
        "## HDAT 165 — `$TICKETS_POR_PARTIDA`\n" +
        "\n" +
        "Os nomes das tags enganam. O que cada campo faz de fato:\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$LIVRE` | Valores usados durante a missão inicial |\n" +
        "| `$MISSAO_0` / `_1` / `_2` | Valores no fácil / normal / difícil, depois da missão inicial |\n" +
        "| `$MISSAO_3` | Não é lido |\n" +
        "| `$DERROTA` (coluna 0) | Tickets ganhos na tela de resultado de cada luta |\n" +
        "| `$VITORIA` (coluna 1) | Tickets ganhos quando a carta de recompensa do arcade já está no nível máximo e vira tickets |\n" +
        "\n" +
        "## HDAT 166 — `$FLAGS_VIDA_ESPECIAL`\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$IMUNIDADE_JOGADOR` | Diferente de 0 = o jogador 1 não perde vida |\n" +
        "| `$OPONENTE_1HP` | Diferente de 0 = o jogador 2 começa a luta com 1 de HP |\n" +
        "\n" +
        "Úteis para testar golpes e cenas.\n" +
        "\n" +
        "## HDAT 167 — `$MODO_DEBUG`\n" +
        "\n" +
        "Diferente de 0 = liga o modo debug do jogo ao entrar no arcade.\n" +
        "\n" +
        "## HDAT 170 a 250 — `$CARTA_N`\n" +
        "\n" +
        "Carta N = HDAT 170 + N. Cada atributo é uma lista com um valor por **nível\n" +
        "da carta** (0 a 10). `$ATRIBUTOS: 28` marca o formato da tabela; não mude.\n" +
        "\n" +
        "| Campo | O que faz |\n" +
        "|---|---|\n" +
        "| `$ATK` | Soma ao ATK do personagem (% do dano dos golpes) |\n" +
        "| `$ATQ` | ATK em milésimos (o número mostrado na carta). O total das cartas ÷ 1000 também soma ao ATK |\n" +
        "| `$DEF` | Soma à DEF (% de redução do dano recebido) |\n" +
        "| `$DEF_EXIBICAO` | DEF em milésimos (o número mostrado). O total ÷ 1000 também soma à DEF |\n" +
        "| `$SYNCHRO` | Ki no início da luta (× 10; somado de todas as cartas e formações; limitado ao ki máximo) |\n" +
        "| `$SYNCHRO_ESTOQUE` | Barras extras de ki máximo (1 = +100) |\n" +
        "| `$ANGRY` | Se a soma for diferente de 0, a luta começa com **fúria permanente** |\n" +
        "| `$RUSH_RED` | Sem efeito (o jogo nunca usa) |\n" +
        "| `$AFINIDADE` | 0 = bem, 1 = mal, 2 = neutro: conta para as formações por afinidade e para o bônus do HDAT 155 |\n" +
        "| `$CUSTO` | Custo para equipar. A soma das cartas não pode passar do custo do personagem |\n" +
        "| `$ATR_NN` | Atributos que o jogo não lê |\n" +
        "\n" +
        "O \"poder\" (BP) mostrado na carta = `$ATQ` + `$DEF_EXIBICAO` (+ 2 × bônus de\n" +
        "afinidade) + (`$ATK` + `$DEF`) × 1000, com mínimo de 1000.\n" +
        "\n" +
        "Ganhar ou comprar uma carta que já tem sobe o nível dela em 1.\n" +
        "\n" +
        "Exemplo: com `$ATK: [0, …]` e `$ATQ: [700, 800, …]`, oito cartas no nível 1\n" +
        "somam 6400 milésimos = +6 de ATK.\n"
    };
}
