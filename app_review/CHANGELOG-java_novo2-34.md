# Changelog — java_novo2-33 → java_novo2-34

Conferência do app (editor/simulador de golpes) contra o código do jogo (`TCBManajer.java` de `CodigoFonteLimpo.zip`).
Todas as referências `jogo:NNNN` são linhas desse arquivo; `app:Arquivo` é o arquivo do app. Foco principal: **cutscenes da flag 20**,
que não ficavam iguais ao jogo.

Baseline do 33: 347 testes OK. Agora: 1415 testes OK (ver a seção *Testes*).

---

## 1. Cutscenes (flag 20 → task md 996) — o que divergia

A task nasce em `CheckOptFlag` (jogo:844, criação em jogo:959-973) com prioridade 13729, **roda no mesmo tick** (depois dos lutadores 13569/13570,
dos projéteis 13312+p e do hit-detect md 935 = 13728; antes da câmera md 19 = 13745) e faz, a cada tick, `_SetXY` → `_ActReq` → mapa/stun → teste de término (jogo:16470-16760).

| # | Antes (app 33) | Jogo | Agora (app 34) |
|---|---|---|---|
| 1 | O app dava o passo da animação e **depois** forçava só a raiz na âncora; links acumulavam velocidade. | `_SetXY` vem **antes** do `_ActReq` e reposiciona raiz **e links** (jogo:16572-16586; no PlayerTail: jogo:2016-2032). | `AttackSim.pinTo()` antes do passo: a raiz fica em âncora + 1 tick de velocidade e os links voltam à raiz a cada tick (não acumulam). |
| 2 | A cutscene era desenhada 1 tick **depois** de morrer (`dying`); cutscene criada no frame 0 rodava 1 tick tarde (câmera/stun/SE). | `_TKill` → `_KillObj` tira o objeto da lista de desenho na hora (jogo:29317, 29457) e o `DrawExec` roda depois de todas as tasks (jogo:30190). A task criada no PlayerTail roda no mesmo tick. | A cutscene morre sem ser desenhada no tick do fim; a primeira execução acontece no tick de criação, inclusive no `reset()` (= 1º tick do jogo). O passo extra `ticks<=1` foi removido. |
| 3 | **Mapa 3** (câmera): deslizava 10 px/tick mesmo com cam 0, mexia no `camBase`, usava o x do hit mesmo com stun, soltava o oponente. | Com cam 0 faz `iBackXPos = alvo` e termina no mesmo tick (a cena nunca aparece); só `iBackXPos` muda; `work[7]` vira o md do alvo quando há stun (jogo:16551-16553); a câmera anda 10 px/tick ou salta (jogo:16688-16730); **nunca** devolve o md (a restauração, jogo:16745-16750, só existe no ramo dos outros mapas). | Snap com cam 0 (cena nunca aparece), 10 px/tick com cam ≠ 0, `camBase` intacto, offset ≈ 0 com stun, stun do mapa 3 não é desfeito. |
| 4 | **Mapas 10-15**: `ScreenEdgeOff` só no 1º tick (o lutador "voltava" à borda no 2º); teleporte aplicado e hit do mesmo frame perdido. | `ScreenEdgeOff[...] = true` **todo tick**; 10-12 reposicionam todo tick, 13-15 só no 1º; `PlayerXPos` muda depois do `_SetXY` do PlayerTail → aparece **no tick seguinte** e o hit-detect do mesmo tick usa a posição antiga (jogo:16615-16690). | Edge-off todo tick; posição lógica muda no tick e a desenhada só no PlayerTail seguinte (`applyPendingMoves()` logo após o `detect()`). |
| 5 | **Stun** congelava o oponente no mesmo tick, soltava no primeiro `end` de qualquer cutscene, não pausava projéteis/auras/agarrão. | `md = 925` só vale a partir do tick seguinte (o PlayerTail dele já rodou), o **primeiro** stun guarda o md e é o único que devolve (`work[7] != 925`); em md 925 o PlayerTail pula o `ActReq` (jogo:2032) e projéteis (jogo:15960), auras (jogo:16197, 16267) e agarrão (jogo:14988) pausam quando **qualquer** jogador está em 925. Um hit sobre o alvo troca o md na hora. | `stunned[]` por jogador, dono do stun (`stunSaved`), pausa de projéteis/auras/agarrão, hit tira o alvo do stun. Após a trava cinemática (md 923/926) o oponente fica parado mais 10 ticks (task 924, jogo:14409-14426): `oppHold`. |
| 6 | Só o atacante disparava flag 20. | `CheckOptFlag` também roda nos projéteis (md 971/972: jogo:16014, 16064; **não** roda com `wActFlag&4`) e no PlayerTail do oponente (usa o HDAT 13 **dele**; o alvo do stun/mapas vira o atacante). A cutscene vive enquanto o ANO do projétil existir; se o projétil morre a task morre **sem devolver o md** (`_TKill` zera `obj`: jogo:29317). | Gatilhos de projétil e de oponente; cutscene acompanha o ANO/pat do dono certo. |
| 7 | Frame com OPT 2048 disparava normalmente. | `CheckOptFlag` volta antes de olhar os hits se `wOptFlag&2048` e não há `DamageWall` (jogo:872-884). | `optAbort()` (vale para teleporte, tremor, flag 20, dano próprio). |
| 8 | Mapas 1, 2, 5, 6 sem suporte. | 1: adia o KO (jogo:6944); 2: esconde o alvo derrotado para sempre (`PlayerState&0x1000000`, jogo:16599, 2109, 14868); 5: `iScrollTarget` → câmera de deslocamento `iBackXOffset` centra o alvo (jogo:28971-29030); 6: `bForceWall` (jogo:12487, 12757). | 2 (alvo some com sombra/auras), 5 (câmera de deslocamento com degraus 12/8/4/2 px), 6 (parede em qualquer ponto). 1 só no log. |
| 9 | ALVO ≥ 2 (pack `bobj`) não desenhava nada. | `acttype 2` = pack `bobjNN` do cenário (jogo:523 `strDataFolder2`, `GetResName2` 29351). | O app carrega a 1ª pasta `bobj*` de `assets` (`OWNER_BOBJ`); sem a pasta o log avisa. |
| 10 | Cutscene desenhada sempre antes/depois de grupos fixos. | `DrawExec` ordena **todos** os objetos por `prio − iActPrio`, desempate por `iTaskPrio` (`prio*13+12` raiz, `+11−i` links). Prios: atacante atacando 150+4p (jogo:2558), parado 130+4(1−p) (jogo:12101), dano/DamageEnd/925 130−4p (jogo:2383, 12370, 14430), projétil 153+4p (jogo:15899), sombra 17 (jogo:14876), agarrão 131 (jogo:14929), efeito de hit 200 (328 na ANO 2) (jogo:15854), cutscene z (200 / 10 nos eventos 8-9 / 17-19-21 / 400). | `layers()` virou uma lista única por objeto com essa chave. |
| 11 | Geometria do HDAT 13 por `length/rows`; `hasEvent` aceitava id == linhas; `eventField` podia estourar. | Linha = `2*XSize` bytes, `n56 >= YSize` mata a task; os campos são lidos do arquivo inteiro (jogo:16470+). | `CharData` lê `XSize/YSize` do cabeçalho e lê do arquivo inteiro como o jogo. |
| 12 | Overlay da flag 20 era um retângulo com largura = id. | `CheckOptFlag` usa só x/y, espelhados por `wObjFlag&16` (jogo:959-973). | Ponto com cruz e rótulo "20 · evento N". |
| 13 | Painel da flag 20 não explicava o evento. | — | `CutsceneInfo`: decodifica a linha (alvo, anim, camada, cam, sync, stun, FLAG) e avisa: evento inexistente, x/y ignorados com cam ≠ 0, eventos 8/9, mapa 3 + stun, bobj. |
| 14 | `Saver` não incluía as ANOs da cutscene (ALVO 1) nem a ANO do oponente agarrado (coluna 24 do HDAT 4). | — | `Saver.anosOfMove(dac, move, char)` inclui as duas. |

## 2. Outras correções de fidelidade (fora das cutscenes)

- **Links de lutadores/agarrão/efeitos de hit**: PlayerTail e md 941 chamam `_SetXY` **antes** do `_ActReq` (jogo:2016-2032, 15006); o md 966 chama **depois** (jogo:15864): efeitos de hit ficam parados no ponto do impacto. Links de lutadores não acumulam velocidade. (`AttackSim.rebaseLinks/pinTo/settle`.)
- **SEQ/END chão × ar** decididos só pela altura (`PlayerYPos == chão`, jogo:13255-13266); `FLY` não influencia (só "não cai"/"não pousa").
- **Flags 39/40** com o oponente morto não teleportam e encerram o ANO (`wActFlag |= 2`, jogo:976-990).
- **Aura charf (41-70)** nunca vira (md 978); aura de effect/char herda o flip do dono, menos com hit flag 0 no frame dela (md 977/978, jogo:16115-16290). Auras pausam por md 925 de qualquer jogador.
- **Tremor (3-5)** é re-armado a cada tick enquanto o frame existe (os hits 3-5 não são consumidos), inclusive em projéteis fora da pausa.
- **Zoom 30/31**: só vale com FRAMES > 0; W = 100 não muda o alvo; vale o último hit do frame (jogo:16915-16945).
- **Agarrão**: o objeto só nasce se o ANO que acertou está em 40..62 (jogo:14948); acerta pelo caminho dos projéteis (defensor 10 de hitstop, objeto 5, atacante nenhum) e marca `hitConfirmed` (jogo:14750-14765).
- **Separação de corpos (md 19, jogo:28704-28790)**: no chão, quem anda para dentro do outro o empurra (metade do passo; passo inteiro se o outro está em dano); a flag 16 do golpe ("fantasma") atravessa; se ainda sobrepõem, o outro vai para uma largura de corpo respeitando as bordas.
- **Câmera de deslocamento `iBackXOffset`**: seguir o defensor que bateu na parede (WALL 1 após DamageWall) ou o alvo do mapa 5; desliga parede/rolagem/limite de tela e leva `iBaseXPos` ao meio; limite de tela só vale com o outro lutador vivo.
- **OPT 1024** também desliga o impacto na parede dos dois lutadores (jogo:12500, 12772).
- **Homing**: tabelas exatas do jogo (`SinTable` assimétrica na metade negativa, `AtanTable`/`GetAtanTable` truncados) em vez de `Math.sin/atan2` (`GameTables`).
- **Prioridade de desenho** dos lutadores (ver item 10).
- **Oponente/atacante em stun (md 925)** continuam seguindo a posição lógica (`PlayerTail` só pula o `_ActReq`, `_SetXY` fica: jogo:2016-2032). Task 924 pós-trava conta 10 ticks a partir do 1º tick em md 881 (o atacante ainda está em md 928 no tick seguinte à trava: jogo:14414-14422), ou seja, solta no 12º tick depois da trava.
- **Projétil principal**: nasce no PlayerTail (task 970, prio 13312 < 13569), então só roda no tick seguinte (1ª execução só carrega o frame 0, jogo:15894-15908) e anda a partir do outro. Sub-projéteis (criados por outro projétil de mesma prio) rodam no mesmo tick.
- **1º tick do jogo** (`reset()`): câmera, zoom e tremor agora rodam depois da cutscene também no tick 0.
- **Agarrão (md 939-941)** reescrito: nasce com o defensor em dano e o ANO que acertou entre 40 e 62 (`DamageHitActNo`, jogo:14948) e `SpecialType ≤ 12` (jogo:2292), independe de SEQ/END; nunca é espelhado; fica sem imagem e recomeça do frame 0 enquanto há hitstop do atacante/do objeto ou md 925 (jogo:14988-14996); morre ao sair de md 876/877/925; som com o deslocamento do jogador 1.
- **Flags 39/40 em ANO de projétil ou do oponente**: usam o jogador dono (`PlayerXPos[p]`) e só aparecem no mesmo tick se o PlayerTail do alvo roda depois da fonte.
- **Melee** só detecta com os dois lutadores vivos (jogo:14601-14607); o tremor do defensor no hitstop (`wObjFlag & 8`) bloqueia a detecção naquele tick (jogo:14692, 14722); `HitTarget` (projétil/agarrão = 1, melee = 0) comanda o recuo do atacante na parede; `bScrollLock` do agarrão simples zera a rolagem.
- **SE comum (< 80)** do jogador 1 não recebe o deslocamento de áudios (só os ≥ 80, `ChrSePlay` jogo:2919-2928, `_ActReqMain` jogo:29600-29609).
- **Bit 4 do wActFlag vindo do dado do MPT** não prende mais lutadores, projéteis, auras e agarrão (o PlayerTail/md 971/977/941 recalculam a pausa a cada tick).
- **Estado entre `reset()`s**: câmera vertical, velocidades de reação e zoom são zerados (antes o quadro 0 de um replay herdava a câmera da execução anterior).
- **Homing** para quando o projétil liga o `work[10]` (só com `CancelTime == 0`), não em qualquer hit.
- **`sceneHidden` (OPT 1024)** olha só os lutadores e vale mesmo com OPT 2048 no frame (jogo:855 vem antes do aborto em 884).
- **Projétil que não nascia (bug real do 33):** o hit de projétil (flag 0) no **frame 0 do ANO de um projétil** com `time = 1` era pulado — o app andava um frame antes do primeiro `_ActReq` do jogo (que só carrega o frame 0, jogo md 970 → 971). Resultado: o subprojétil (`SUBPROJETIL_ANIM_ID`) não nascia. Corrigido (primeira execução só carrega o frame; teste de regressão `SpawnDiagTest`, com `time` 1, 2 e 3). Conferido sem mudança: escolha do campo pela altura relativa (`PROJETIL_ANIM_NORMAL`/`ABAIXO`/`ACIMA`, jogo:2542-2590), tiro triplo (ANO, ANO+1, ANO+2) e limite `BulletNum < 8` — fuzz de 4000 sorteios contra uma réplica do jogo, 0 diferenças.

## 3. Textos e interface

### 3.1 Projétil que não nasce: agora o app diz por quê
Quando um hit de projétil (flag 0) existe mas **nada nasce**, uma faixa âmbar aparece na prévia (junto dos avisos do `GameRules`; o que fecha o jogo tem prioridade) e diz **qual** é o motivo. Casos: (a) o campo escolhido pela **altura relativa** está vazio, com o nome do campo e a diferença em px (a faixa `NORMAL` vale entre 37 px acima e 51 px abaixo do oponente) — nesse caso o botão **POS** abre o ajuste de altura; (b) `PROJETIL_ANIM_NORMAL` vazio; (c) o ANO do projétil não existe/está vazio, ou, no tiro triplo, falta o ANO+1/ANO+2; (d) já há 8 projéteis vivos; (e) hit de projétil dentro do ANO de projétil com `SUBPROJETIL_ANIM_ID = 0`; (f) ANO solto (modo ANO) sem golpe dono. O aviso vem da execução completa do golpe (`BattleSim.spawnProblem()`), então não depende de onde está a barra de tempo. O jogo se comporta do mesmo jeito nesses casos: o aviso só deixa de ser silencioso. `OCULTAR` esconde o aviso para aquela mensagem.

### 3.1b Golpe com ANO acima de 255 (golpe 11: projétil 342 virava 255)
- **Bug:** `CharData.Move.MAX = 255` + `clamp()` ("o BIN guarda cada valor em até 255"). Os 24 campos do HDAT 4 são `short` de 16 bits (o jogo monta o valor com dois bytes: `BulletAction` jogo:2590-2591, ANIM_INICIAL 2569-2570, SEQ/END 13260-13265, AO_ACERTAR/pouso/agarrão 14951-16049; a classe `hdat4` do projeto lê/escreve do mesmo jeito). Qualquer edição do golpe pelo app (caixas do GOLPE SET, DADOS/EDITAR, texto do golpe) cortava `PROJETIL_ANIM_NORMAL/ABAIXO/ACIMA = 342` para **255**, e o app lançava o ANO 255 (efeito em laço), não o raio do 342. Verificado com os arquivos do usuário: antes da correção o projétil nascia com o ANO 255; agora com o 342.
- **Correção:** limite passa a 32767 (short). O seletor de ANO (`AnoPicker`, usado para escolher o projétil, SEQ, END, AO_ACERTAR...) listava só 0..255: agora lista todas as ANOs do DAC. Textos "Máximo 255" da timeline trocados.
- **Conferidas as outras tabelas:** HDAT 13 (cutscenes, flag 20) = 8 campos `short` de 16 bits, sem limite de 255 no app. Reações de dano (HDAT 10+ / 14-33) = 14 campos de **1 byte**, e isso é limite real do jogo: o ANO é lido como `(byte)` com sinal (-127..127; negativo = espelhado, jogo:12378/12644) e a voz também com sinal (`if (n > 0)`, jogo:12379): voz acima de 127 nunca toca; o editor agora limita a voz a 0..127 e os rótulos mostram as faixas.

### 3.1c Projétil que nunca saiu: agora o aviso explica
Quando o hit de projétil está num ANO de SEQ/END/AO_ACERTAR/pouso e a execução não chegou lá, o aviso diz o motivo: golpe errou (botão POS), TRANSIÇÃO TRAVADA, ou oponente derrotado (flags 39/40 encerram o ANO, como no jogo, jogo:976-990).

### 3.1d Hierarquia de links e animações de flag (conferida contra o `TCBManajer` novo)
- Jogo: `_SetAct` (arquivo novo, linhas 32053-32208) cria os links **só da raiz** e pula a tabela de links do próprio link, então **link de link nunca nasce**; `_ActReq` (31486) avança a raiz e os links dela. Hitboxes, flags de hit, OPT (poeira, 2048...) e SE por flag de um link não valem (o jogo só lê a raiz). Toda animação disparada por flag (efeito de hit, poeira, projétil, aura, cutscene, agarrão, sombra) é uma raiz criada com `_SetAct` + `_ActReq`: **mostra os próprios links (1 nível)**.
- App: já era igual (`AttackSim.reset` só cria `ra.links`; `BattleSim.collect` desenha raiz + links de todos os tipos; flags 0/20/39/40 só lidas da raiz). Travado por `LinkHierarchyTest` (link de link não nasce; flag 0 e 39 dentro de link não valem; projétil e efeito de hit mostram o link e não o link do link).
- **Avisos:** o aviso de tela "é link e tem links/caixas" saiu; agora a **lista de avisos** traz uma linha: "Os ANOs N, N e N têm links que têm outros links ou animações/efeitos; esses não aparecem (o jogo só lê a raiz): o link mostra apenas as sprites do link." (conta links do link, flags que criam animação — projétil, tremor, poeira, cutscene, zoom, teleporte, charf/auras — e poeira do OPT).
- **LINKS:** abrindo com a prévia num ANO que tem links, a lista rola sozinha para o primeiro link (card destacado) e a seta **▶ PRÓXIMO LINK (n/N)** passa para os seguintes.
- Limitação: o `TCBManajer` novo é uma versão modificada (`ExtrasReader`, OPT 225 = vídeos/texturas animadas); essa classe não veio, então o app não simula nem confere esse recurso.

### 3.1e Barra de título e avisos completos
- Nova barra no topo: "Attack Editor by **Zue Mods**" (Zue Mods em azul; toque abre `https://www.youtube.com/@zuemods/videos` no app do YouTube, ou no navegador se não houver) e o botão **AJUDA** à direita. A AJUDA saiu do menu movível (ordens salvas com ela são ignoradas).
- Avisos expandidos mostram **todos** os itens, um por linha, em vez de "+N": o aviso sobre a prévia (`QuickNotice.full`) e a linha de aviso acima da prévia (agora recebe a lista inteira; antes só o primeiro). Recolhidos continuam com o resumo "(+N)".

### 3.1f Menu da sprite repaginado (gaveta da timeline)
Tocar numa sprite abre uma **grade de botões quadrados** (ícone + texto): Imagem (amarelo), Tempo (verde), Som (vermelho), Movimento (verde), Efeitos (cinza), Copiar (azul). Cada um abre uma **página dentro da gaveta** (a prévia continua visível; ◀ volta à grade). Tocar num item de velocidade, som ou estilo da timeline abre direto a página dele.
- **Imagem:** thumb da sprite; `− N +` (segura repete; tocar no número abre a grade de sprites, nunca o teclado); posição x/y e zoom com teclado; "unir zoom X e Y" (marcada; desmarcar mostra x e y); rotação; alpha; **mover links junto** (soma o deslocamento no bx/by dos links da ANO); **Editar posição do Link** (tela com a animação principal de base e controles de bx/by, sprite, tempo e posição do frame do link); Remover imagem (`image = -1`). Mexer em posição/zoom/rotação/alpha liga o MID/RAZ sozinho.
- **Tempo:** `− ticks +`; **0 apaga o TIME** (aviso: frame travado); duração em N.Ns; Remover Frame.
- **Som:** cartão do som atual no mesmo desenho da lista de sons (play/pausa, cortar, mais, substituir, mutar), `−`/`+` nas laterais só pelos ids que existem (0 → sons comuns → 80 → sons do personagem; sem segurar), Escolher e Remover Áudio.
- **Movimento:** velocidade x/y; frame com `64` (recebe do anterior) tem campos zerados que **somam** à recebida, com a linha amarela "N speed x/y mantidos do frame anterior" + Remover e "N recebido do frame anterior, total: N"; aceleração só aparece com speed; frame inicial e duração do trecho; Manter velocidade; total em px; Remover speed.
- **Efeitos:** Flip X/Y e Interpolação (+ explicação). **Copiar:** igual ao antigo ALL.
- **Velocidade recebida (análise do jogo):** ao carregar o frame, sem o bit 64 `speed = 0`; `accel = 0` sempre; cada campo do MOVE **atribui**. Então a recebida conta como **speed** (`v + accel × ticks` do frame anterior), mudar o speed **substitui**, e a accel **não** é herdada. O picker zerado grava `recebida + acréscimo` (valor absoluto, que é o que o jogo entende); se o frame anterior mudar depois, o valor gravado não acompanha.
- Ajustes de layout: os `− n +` são compactos (do tamanho dos campos de antes, não ocupam a largura toda); a informação de cada campo fica logo abaixo do picker dele; o ✕ da gaveta primeiro fecha a página aberta (volta à grade) e só depois fecha o menu de edição.
- Código: `FrameTools` (regras puras, testadas), `Stepper`, `IconView`, `ThumbView`, `LinkPosDialog`, `SoundDialog.embed`. Fora desta rodada (próximas): menus de boxes e efeitos de hit; mover/duplicar frame ficam para a barra sob a prévia.

### 3.2 Menu superior reordenável
- **Segure** um botão do menu superior e **arraste** para frente ou para trás; a barra rola sozinha perto das bordas e o botão de origem fica translúcido durante o arraste. Soltar em cima de outro botão põe o arrastado naquela posição; soltar nas frestas/depois do último vai para o botão mais próximo.
- O **primeiro botão** fica fixo fora da rolagem (por padrão, SALVAR). **Soltar qualquer botão no slot fixo troca o botão fixo.**
- A ordem fica salva no aparelho (`SharedPreferences` "toolbar"); ids desconhecidos ou repetidos são descartados e botões novos entram no fim. Uma dica aparece uma única vez. `RESTAURAR ORDEM` (na ajuda) volta ao padrão.
- Código: `ToolbarOrder` (Java puro, testado), `ReorderBar` (Android, arrastar/soltar), `LayoutManager` (monta os botões).

### 3.1g Altura da gaveta (Y / T)
- **Altura padrão**: a gaveta de edição agora é flutuante e abre por cima de tudo, ancorada acima da barra de controle, subindo até a **base da prévia** (antes subia só até a altura dos botões dividir/duplicar/excluir). Abrir sempre usa a altura padrão.
- **Arrastar a borda** sobe ou desce a gaveta livremente por toda a tela (até por cima da prévia); vale só para aquela abertura. Toque curto na borda alterna mínimo → padrão → máximo; arrastar rápido para baixo fecha (como antes).
- **Y** (botãozinho na borda, à direita): grava a altura atual como a nova altura padrão (uma por modo, `SharedPreferences "sheet"`).
- **T** (ao lado do Y): põe a **timeline acima do menu**. A gaveta entra no fluxo da tela (sobe no máximo metade dela), a prévia (quadrada) diminui proporcionalmente, a área de rolagem e o menu superior (botões, status, info) somem para liberar espaço; a barra de título e a de controle ficam. T de novo volta ao modo normal. **O modo T nunca é salvo**: o app sempre abre com ele desligado, e o Y salva só a altura (não o T), para não afetar o menu superior.
- **Preview no modo T**: em vez de re-medir a view (o que descentralizava câmera/enquadramento), a view continua medida e desenhada no tamanho de referência e o `SquareHolder` só aplica uma escala uniforme (`scaleX/Y`, pivô no topo e no centro). Câmera, zoom e posições ficam idênticos, só menores; o toque é mapeado pelo próprio Android.
- **Faixas da timeline no modo T**: quanto mais o menu sobe, menos faixas aparecem (máximo 6 com o menu baixo ou fechado, mínimo 2 com ele no máximo; a faixa das sprites conta como uma e as outras rolam). Fora do modo T não há limite. Lógica em `TimelineView.lanesFor/setLaneLimit` (testada); `TimelineTools.sheetFraction/setOnSheetResize` avisam o layout a cada mudança de altura.
- Código: `LayoutManager` (modo, reparentamento, `placeSheet`) e `TimelineTools` (`Geo`, `setGeometry`, `setTOn`, `refit`, botões Y/T). Texto novo no AJUDA ("Altura da gaveta: Y e T").
- **Não executado num aparelho** (só compilado contra o `android-all` 13 e revisado): o reparentamento ao alternar o modo e a medida da prévia no modo T precisam de um teste manual.

### 3.1h Timeline: só faixas com uso
- Na timeline aberta, **Audio, Velocidade e Estilo** só aparecem se algum frame do ANO usa (antes apareciam sempre). Criar um som/velocidade/estilo continua pelo ＋ e pelo menu da sprite (Som / Movimento / Efeitos); o toque numa célula vazia dessas faixas só existe enquanto a faixa existir.
- **Regra de faixas:** itens iguais em frames diferentes dividem a mesma faixa; itens iguais no mesmo frame vão para faixas diferentes. Objetos (hitbox, hurtbox, projétil, efeito, ação) já seguiam isso por `(flag, ocorrência no frame)`. **Transições** agora também: marcas no mesmo frame deixam de dividir a largura dele e ganham uma faixa cada ("Transicoes", "Transicoes 2"...); em frames diferentes ficam na mesma faixa. Lógica em `TimelineView.seqLanes/laneCount` (testada).
- Desenho e toque da timeline só compilados e revisados (não rodados num aparelho).

### 3.1i Menu superior, cores das faixas e menu de Hitbox/Hurtbox
- **Menu superior volta ao fechar a gaveta (bug do modo T):** ao sair do T, `status/info/aviso` ficavam escondidos para sempre (a visibilidade "atual" já era GONE). Agora `topRow`, `mrow`, `status`, `info` e `aviso` ficam num `topGroup` e só a visibilidade do grupo muda. O layout T vale **só com T ligado e a gaveta aberta**: com a gaveta fechada tudo volta ao lugar (menu superior, rolagem, timeline); o T continua "armado" (nunca é salvo) e reaplica ao reabrir.
- **Cores das faixas da timeline:** Hitbox **vermelho** (era azul), Som/Audio **azul** (era vermelho); o menu ＋ acompanha. O ícone "Som" da grade da sprite continua vermelho, como pedido antes.
- **Menu de Hitbox / Hurtbox** (de cima para baixo): interruptor Hurtbox | Hitbox; **Frame de início** (move a box inteira, mantém a duração; colisão **não recusa**: a movida vira a ocorrência seguinte, outra faixa) e **Duração**; **prévia** do sprite com as boxes (`AnimPreview` parada no frame); **x, y, largura, altura** (− N +); remover. Só hitbox: **Dano** e **reação** (prévia, ◀ ▶, escolher, passos — como antes), e **Transição**: transição atual, **mudar ANO destino** e **bloquear sequência** (só se o golpe tem SEQ). Dano e reação são do sprite sob o ponteiro (OPT do frame) e **acompanham a hitbox** ao mudar o frame de início: cada frame leva o par dele e o frame antigo é limpo (o bloco OPT some se ficou vazio); se o frame antigo ainda tem outra hitbox o valor fica, e se o destino já tem hitbox própria (colisão) ele mantém o dano e a reação dele. `optFlag` e os dados 2-3 nunca são mexidos; hurtbox não mexe no OPT. Mudar a **duração** (Stepper ou arrastar a borda na timeline, `ObjModel.trim`) segue a mesma regra: os frames novos copiam o dano e a reação do frame da borda (se o destino não tem outra hitbox) e os frames que saem são zerados (se ficaram sem hitbox).
- **Prévia da box** parada no frame atual (sem reprodução: `AnimPreview.setStaticFrame` avança a simulação até o frame de uma vez, `advanceToPat`) e **só a box editada** (`setOnlyHit` → `AttackRenderer.Options.onlyHitFlag/Occ`): no modo hurtbox a hitbox não aparece e vice-versa; as demais boxes do frame também não.
- **Ao mover o ponteiro** o menu da box só muda se o novo frame tem a box (mesma flag/ocorrência); se não tem, ele e a prévia ficam como estão (antes ele grudava na box mais próxima, mostrando a próxima hitbox). As ações (duração, início, coordenadas) agem na box mostrada.
- `ObjModel.move/locate` (puro, testado em `ObjMoveTest`).
- Não executado num aparelho (só compilado e revisado): layout do T/topGroup, prévia e steppers.

### 3.1j Projétil: aviso do limite, tela do projétil e modo ANO
- **Aviso do limite de 8 projéteis** saiu da tela (nasce e sub-projétil); fica só numa seção do AJUDA ("Limite de 8 projéteis"). Os outros diagnósticos de projétil continuam.
- **Tela do projétil** (hit flag 0), na ordem: **início e duração** (Stepper: move/aumenta o hit), **ramificação do projétil** (árvore; é onde ficam ao acertar/chão/abaixo/acima/global/sub), **ANO do projétil** (− N +, segurar; tocar no número abre a grade de animações; prévia e ✎ editar), **posição e velocidade** (x, y, velocidade inicial), **perseguição** (liga/desliga, giro, repetições) e **sequência** (SEQ_PROJETIL).
- **Modo ANO**: a mesma tela abre no modo ANO, editando o golpe real do HDAT 4 que é "dono" do ANO (`AttackView.moveOwner/editMoveOf`; a cópia genérica da tela acompanha e o desfazer volta os dois). Sem dono, avisa.

### 3.1k Salvar este golpe em outro personagem
- **SALVAR › Salvar golpe atual (HDAT 4 + ANOs)** ganhou a 3ª opção, junto de "pasta separada" e "Runtime": **Salvar em outro personagem (Runtime)** (grava direto no Runtime do destino). Fluxo: escolher o personagem (pastas `charNN` de `Tap Battle/assets/`, menos o atual) → escolher o golpe a substituir → confirmar.
- **Regra dos slots** (`MoveTransplant.canReplace`): os golpes 0, 3 e 6 só substituem o 0, 3 e 6 respectivamente; os demais substituem qualquer golpe exceto 0, 3 e 6.
- **O que vai:** o `$GOLPE_n` do HDAT 4 do destino é trocado (todos os campos de ANO renumerados); os ANOs que o golpe alcança (campos do HDAT 4, ANO+1/+2 do tiro triplo e do agarrão, links e links de links) entram **no fim do DAC do destino**, renumerados e com os links ajustados; as **imagens** usadas viram sprites novas no fim do CNV e das folhas do destino (`SpriteLib.add/commit`, sem repetir a mesma imagem); as **reações do personagem** (OPT 1, 30-49) usadas vão para um slot do destino: igual > livre (nenhum ANO do destino usa) > sobrescreve o mesmo número com aviso, e o OPT 1 dos frames é ajustado.
- **Não vai (avisa):** sons do personagem (id ≥ 80) e hitbox flags 20 (cutscene) e 41-125 (charf/auras: apontam para os dados do destino).
- **Segurança:** os `.txt` do destino são copiados para `Tap Battle/TBRE/Backups/AAAAMMDD_HHMMSS_charNN_transplante/` antes; folhas/CNV são gravados antes dos `.txt` (o DAC nunca aponta para sprite que não existe); usa o que está na memória (inclusive o não salvo). Limite: 4096 ANOs (link guarda 12 bits).
- **Carregamento:** `000.dac.txt` com ANOs além do binário agora é lido (`Dac.growTo` + `CharAssets.applyDacTxt`), para o app enxergar os ANOs novos ao reabrir o personagem destino.
- Lógica pura em `MoveTransplant` (testada em `MoveTransplantTest`); as telas (lista de personagens, golpes, confirmação, resultado) só compiladas e revisadas, **não executadas num aparelho**.

### 3.1l Menu de salvamento: caminhos e backups
- "Onde salvar?" agora mostra: **Salvar em pasta separada** (`Tap Battle/TBRE/Chars/charNN/`), **Salvar no Runtime** (`Tap Battle/assets/charNN/`) e, só em "Salvar golpe atual", **Salvar em outro personagem (Runtime)**.
- A pasta separada saiu de `assets/charNN/salvos/` e foi para **`Tap Battle/TBRE/Chars/charNN/`** (uma subpasta por personagem, para `golpe_00.txt`, `000.dac.txt` etc. de personagens diferentes não se misturarem). Os backups/cópias de segurança (hoje, o do "salvar em outro personagem") ficam em **`Tap Battle/TBRE/Backups/`**. A raiz `Tap Battle/TBRE` é achada a partir da pasta do personagem (`Saver.tbreRoot`). Arquivos já gravados em `salvos/` continuam onde estão.

### 3.1m Ajustes: tela do projétil e GOLPE SET
- **Tela do projétil:** saiu o seletor de duração do topo (o genérico de `object()`); ficou só o "Início e duração" da própria tela.
- **GOLPE SET:** o título agora é só "Opções do golpe" (sem "ATK_FLAGS = N"). Em "Hitbox destrói as hitboxes do oponente" o "(HDAT 3, byte 13)" virou a reação real que esse byte guarda: "do jogo N" (0 a 29) ou "do personagem N" (30 ou mais, N − 30), como o jogo escolhe a reação (md 876/877). `CharData.clashReaction/reactionName`.

### 3.1n Slots vazios de reação do personagem
- **Escolher reação** agora lista também os slots 30-49 (HDAT 14-33) **vazios**, como "＋ vazio · personagem k · adicionar". Tocar abre: **copiar de uma reação do personagem**, **copiar da game data** (globais 0-29), **importar de outro personagem** (lê o `000.bin`/`000.bin.txt` dele, com resumo e miniaturas dos ANOs) ou **em branco** (1 passo). A tabela vai para o slot (desfazível) e o hit passa a apontar para ele.
- **Gravação** (`Saver.patchReaction`): se o bloco `$REACAO_DANO_n` não existe no `000.bin.txt`, ele é **criado** dentro do `$HDAT_14+n` (com `$COL: 14`, `$ROW` e `$SZ` certos e a vírgula do `$info`); se nem o `$HDAT_14+n` existe, avisa e não grava (o `$COUNT` do arquivo não muda).
- **Salvar em outro personagem**: as reações usadas agora preferem um **slot vazio gravável** do destino (um `$HDAT_14+n` que exista no `.bin.txt` dele) antes de usar um cheio sem uso (`Saver.writableReactionSlots`, `MoveTransplant.build(..., writable)`).
- Código: `ReactionSlots` (vazios, resumo, leitura de outro personagem), `ReactionTable.blank()`, `TimelineTools.pickReaction/addReactionDialog/pickSourceGrid/importReactionFromCharacter/fillSlot`. Telas só compiladas e revisadas (não executadas num aparelho).

### 3.1o Editor de reações — etapa 1 (modelo)
- `ReactionTrace`: roda a simulação **real** (`BattleSim.forceHit` + `step`) e guarda uma foto por tick (`BattleSim.damageTick`: tabela, passo, tempo, velocidade/aceleração, posição) e os trechos por passo. A futura timeline de reações desenha daqui, então nunca diverge da prévia. Cenários: chão (oponente em y = 0) e ar (altura pela POS).
- `ReactionTable.duplicateRow/moveRow` (ao lado de `insertRow/removeRow`).
- Testes em `ReactionTraceTest`: duração de chão (DUR_C) e de ar (DUR_A) viram o tamanho de cada trecho, "mantém" (−1) segue a velocidade, deslocamento por tick, 255 (até pousar) e as operações de passo.

### 3.1p Editor de reações estilo timeline (substitui o `ReactionEditor`)
- **Tela própria** (`ReactionScreen`): prévia da reação no topo (`ReactionPreview` em modo controlado: **TOCAR/PAUSAR, −1, +1, ⏮** e salto para qualquer tick), **timeline** no meio (`ReactionTimelineView`) e **abas** do passo embaixo. Botão **EDITAR REAÇÃO** (do personagem) ou **VER REAÇÃO** (do jogo, só leitura) no menu da hitbox.
- **CHÃO | AR**: escolhe o cenário simulado (oponente em y = 0, ou a 60 px do chão); o jogo usa duração/velocidade de chão no chão e as de ar no ar.
- **Timeline** (ponteiro no centro; arrastar rola e leva a prévia; pinça faz zoom; tocar num passo seleciona): faixas **PASSOS** (miniatura do ANO do oponente, ⇄, duração, "até pousar"; reações seguintes em cinza), **VELOC.** (curva x/y em px/tick), **VALORES** (velocidade/aceleração da tabela; = mantém, ½ metade), **VOZ**, **FLUXO** (próxima reação e parede). O ponteiro entrando num passo seleciona esse passo na aba.
- **Abas**: ANIMAÇÃO (ANO do oponente com grade, espelhar), TEMPO (duração de chão/ar, ATÉ POUSAR = 255), MOVIMENTO (velocidade e aceleração do cenário, **= mantém** e **½**, copiar para o outro cenário), FLUXO (próxima reação, parede, voz) e TOTAL (deslocamento e ticks por passo). Barra: **＋ PASSO, DUPLICAR, ◀ ▶, ✕ REMOVER**, mais DESFAZER no cabeçalho.
- Tudo parte de `ReactionTrace` (a simulação real), então a timeline não diverge da prévia. **Telas só compiladas e revisadas, não executadas num aparelho.**

### 3.1q Salvar em pasta separada = cópia completa
- **Salvar em pasta separada** (`Tap Battle/TBRE/Chars/charNN/`) agora **copia o personagem inteiro** (`000.dac/.bin/.cnv`, os `.txt`, folhas `NNN.png`, sons...) e aplica na cópia a **mesma gravação do Runtime** (`Saver.saveInto`: golpe atual, ANO atual, todos os golpes, todos os ANOs ou tudo). A cópia fica pronta para usar no Runtime; o original não é tocado. Os arquivos soltos de antes (`golpe_NN.txt`, `ano_NNN.txt`, `golpes.txt`) saem do menu.
- **Pasta já existe:** o app pergunta — **UNIR** (copia só o que falta e aplica por cima, o que já estava salvo continua), **DELETAR** (apaga a pasta, só dentro de `TBRE/Chars`, e copia de novo) ou **OUTRO NOME** (campo com `charNN_2`).
- Copiar roda em segundo plano com barra de progresso (`BusyDialog`). Código: `Saver.copyChar/separateDir/freeName/isSeparateCopy/deleteTree/saveInto`; testes em `SaverCopyTest`.

### 3.1r Editor de reações: duração arrastável e faixas SPEED / VOZ / EFEITOS / SEQ
- **Arrastar a duração do passo**: a borda direita do bloco do passo tem uma barra amarela; arrastar muda a duração do **cenário atual** (coluna `DUR_C` no chão, `DUR_A` no ar) em ticks, com linha-guia e o valor na tela, e grava ao soltar (uma só entrada de desfazer). Passos "até pousar" (255) ficam travados. Mínimo 1, máximo 254 (`ReactionEdit`, testado em `ReactionEditTest`).
- **Faixas** como na timeline principal: **PASSOS**, **SPEED** (curva x/y e valores `v`/`a`), **VOZ**, **EFEITOS** (parede) e **SEQ** (próxima reação). Tocar num item abre a aba dele; tocar numa célula vazia (＋) abre para adicionar.
- **Abas**: ANIMAÇÃO | TEMPO | SPEED | VOZ | EFEITOS | SEQ | TOTAL (a antiga FLUXO se dividiu em VOZ, EFEITOS e SEQ).
- Gestos e desenho só compilados e revisados (não executados num aparelho).

### 3.1s AJUDA em capítulos (tela cheia)
- O botão **AJUDA** abre uma **tela cheia** (`HelpScreen`) com a lista de **14 capítulos** (título + resumo): Primeiros passos · Menu superior (os 14 botões, dica de ordem e RESTAURAR ORDEM) · Timeline e ferramentas · Menus de edição da sprite · Menus de box · Golpe e HDAT 4 · Projéteis e ramificação · Sequências e transições · Reações · Speed e aceleração · Links e animações por flag · Sons, sprites e CNV · Salvar e backups · Avisos e limites. Tocar abre o capítulo (texto maior e subtítulos); a **busca** no topo acha seções de qualquer capítulo por palavra (sem diferenciar acento ou caixa; várias palavras = todas); o botão voltar do aparelho volta da seção para a lista.
- `HelpText.chapters()/search()` (puro, testado em `ToolbarTest`); `toolbar()`, `screen()`, `all()` e `forButton()` continuam, derivados dos capítulos.
- Correção de texto: o limite de caixas por frame é **10** (o jogo guarda 50 números, 5 por caixa), e não 50 caixas.

### 3.1t Busca da AJUDA: aproximação, correlação e abas primeiro
- **Aproximação**: a busca tolera erros de digitação (distância de Damerau‑Levenshtein: até 3 letras só exata; 4 a 6 letras, 1 erro; 7 ou mais, 2) e aceita prefixo/trecho. `sped`, `spped`, `spedd`, `speeed` acham `speed`; `hitbx`, `sequnecia` (troca de letras) e `reaçao` também.
- **Correlação**: grupos de sinônimos (speed/velocidade/dash/mover/pixels…, accel/aceleração/gravidade…, hitbox/dano/alcance…, reação/knockback/empurrão…, sequência/combo/transição/cancel…, projétil/tiro/ramificação…, som/áudio/voz…, salvar/backup/pasta…) expandem a consulta (peso 70%).
- **Abas com tags**: toda aba/menu (14 botões do menu superior, páginas Imagem/Tempo/Som/Movimento/Efeitos/Copiar, menus da hitbox, tela do projétil, Editor de reações, etc.) tem `tags` em `HelpText.Section`; a ordem das tags pesa (a primeira vale mais). Ex.: **Movimento** = speed, velocidade, dash, accel, aceleração, mover, pixels, pulo, gravidade…
- **Resultado em duas faixas**: **ABAS E MENUS** primeiro (a melhor com contorno e “MELHOR RESULTADO”), depois **TEXTOS** dentro dos capítulos, cada um com o trecho que bateu (tocar abre o capítulo). Todas as palavras da consulta precisam bater (AND).
- Testes: `HelpSearchTest` (48).

### 3.1u Editor de reações: campos que digitam e que seguram
- **Causa**: toda edição chamava `refresh()` → `buildPage()` e recriava os campos; o botão − / + que estava sendo segurado saía da tela (o `Hold` para) e o campo digitado perdia o foco; o valor digitado só valia ao confirmar.
- **Edição no lugar**: `ReactionScreen.setCell` grava a célula e só atualiza os campos abertos (`upd`/`runUpdaters`: valor do `Stepper`, rótulos, botões marcados); timeline, prévia e simulação seguem em `refreshLight()`, no máximo a cada ~150 ms (continua atualizando enquanto se segura o −/+). A página só é recriada ao trocar de passo, aba ou cenário.
- **Digitar**: `Stepper.live(true)` aplica o valor ao digitar (pausa de 300 ms) e ao confirmar; não reescreve o texto enquanto o campo está em foco e vale o mesmo número (o cursor não pula); seleciona tudo ao focar; a janela usa `SOFT_INPUT_ADJUST_RESIZE`.
- **Desfazer**: `AttackView.editReaction(type, key, change)` junta as edições seguidas da mesma célula numa única entrada e refaz a simulação principal uma vez, 250 ms depois (e ao fechar, `flushReactionEdit()`).
- Só compilado e revisado (gesto/teclado não rodam sem aparelho).

### 3.1v Slots vazios: as três listas tocam a reação de verdade
- `BattleSim.overrideCharReaction(slot, tabela)`: a simulação usa a tabela candidata como se estivesse no slot (sem tocar no personagem); `ReactionPreview.setOverride/setPaused` a usam na prévia.
- As listas “copiar do personagem”, “copiar da game data” e **“importar de outro personagem”** (antes só miniaturas paradas) mostram a reação **em laço, de verdade**, com **⏸ PAUSAR / ▶ TOCAR** (também tocando na prévia) e o botão **USAR** (tocar na célula já não escolhe).
- Testes: `ReactionTraceTest` (+5: slot vazio sem passos, candidata no slot 5, velocidade, outros slots intactos, override removido).

### 3.1w Editor de reações = edição na timeline (menus só quando necessário)
- **Sem abas fixas**: a tela tem prévia + transporte, **timeline grande** e **barra de ferramentas** (＋ · DIVIDIR · DUPLICAR · EXCLUIR · ◀ ▶ · Σ TOTAL; DESFAZER no topo). A **gaveta** de baixo só abre ao tocar num item (✕ fecha) e traz a página dele: PASSO (ANO, espelhar, durações), SPEED, VOZ, EFEITOS, SEQ ou TOTAL.
- **SPEED em corridas, como no ANO** (`ReactionRuns`, puro): um bloco por passo com velocidade própria (x ou y ≠ −1) cobrindo os passos seguintes que mantêm (−1). **Arrastar a borda direita** do bloco move a fronteira com o bloco seguinte (nada é apagado: os passos que trocam de lado passam a manter ou a receber a velocidade do vizinho); encurtar o último bloco abre um bloco parado (0,0). Chão e ar têm corridas próprias (colunas 3/4 e 7/8). Tocar no bloco abre a página SPEED.
- **DIVIDIR** (`ReactionEdit.split/splitKeepingMotion`): parte o passo no ponteiro em dois (duração dividida, “até pousar” fica no 2º; a duração do outro cenário na mesma proporção); o 2º passo mantém a velocidade e, **com aceleração**, parte da velocidade em que o 1º terminou; a voz fica só no 1º. **O ANO do oponente recomeça no 2º passo** (cada passo reinicia a animação): o aviso está no AJUDA e no aviso ao dividir.
- **EXCLUIR** apaga o item selecionado: passo (≥ 2 passos), bloco SPEED (`removeRun`: o bloco passa a seguir o anterior), voz, parede ou SEQ. **＋** cria passo ou abre a gaveta de speed/voz/parede/SEQ no passo selecionado (`startRun` cria a velocidade 0,0 se o passo só mantinha).
- Células vazias de VOZ/EFEITOS/SEQ continuam abrindo a gaveta para criar; o item selecionado de cada faixa ganha contorno branco.
- Testes: `ReactionEditTest` (+20: corridas, estender/encurtar, limites, chão x ar, remover/criar, dividir) e `ReactionTraceTest` (+1: dividir mantém posição ±1 px e velocidade ±8/128 tick a tick). AJUDA: capítulo Reações com a barra de ferramentas e tags.
- Só compilado e revisado: desenho dos blocos, alça de arrastar, toque, gaveta e barra não rodam sem aparelho.

### 3.1x Editor de reações com o layout da tela principal
- **Causa do “travado/estranho”**: a tela empilhava cabeçalho + prévia + timeline de 194 dp fixos + abas numa coluna só; em tela baixa as faixas de baixo (SPEED/VOZ/EFEITOS/SEQ) ficavam fora da área tocável. Agora a arquitetura é a da principal.
- **Layout** (`ReactionScreen`): cabeçalho (título · CHÃO|AR · FECHAR) → prévia **quadrada de largura cheia** (`LayoutManager.SquareHolder`) → **área que rola** com a barra de tempo + rótulo, os 4 botões **DIVIDIR · DUPLICAR · EXCLUIR · DESFAZER** (mesmo estilo, 10,5 sp, pesos iguais) e a timeline com as abas **＋** e **#** coladas à direita → **transporte fixo** (INICIO · −1 · TOCAR · +1 · velocidade 1x/0.5x/0.25x/2x, com `ReactionPreview.setSpeed`) → **gaveta flutuante** (`ReactionSheet`: alça arrastável, Y grava a altura padrão em `ryH`, ↶ ✕ APLICAR, altura padrão até a base da prévia).
- **Timeline** (`ReactionTimelineView` reescrita, medidas de `ReactionLayout`, puro e testado): régua 14 dp, clipes 40 dp (um por passo, amarelos, com miniatura), resumo 10 dp, faixas de 22 dp, teto 188 dp, coluna de nomes de 58 dp, rolagem vertical, ponteiro no centro, pinça. **Fechada** mostra linhas de resumo; **aberta** (padrão) mostra SPEED (blocos arrastáveis), VOZ, EFEITOS, SEQ e CURVA (x/y em px/tick). Tocar em PASSOS ou no cabeçalho abre/fecha; o filtro **#** liga/desliga faixas. Desenho e toque usam as **mesmas linhas** (`ReactionLayout.laneAt`).
- **Gaveta = grade + páginas** como a da sprite: tocar num passo abre a grade (ANIMAÇÃO · TEMPO · SPEED · VOZ · EFEITOS · SEQ · TOTAL); tocar num item de faixa abre a página dele direto; ◀/✕ voltam; o botão voltar do aparelho fecha uma camada por vez. Os atualizadores (edição no lugar) continuam.
- **＋** (menu): passo novo, speed, voz, parede, SEQ, mover ◀ ▶ e Σ total.
- Testes: `ReactionLayoutTest` (23: alturas, teto, resumo x aberta, filtro, rolagem, hit-test). Desenho, gestos, gaveta e botões só compilados e revisados.

### 3.1y Botão “Ajuda de IA” → `Download/TBRHelp.md`
- Na tela AJUDA, o botão **Ajuda de IA** (cabeçalho e cartão no topo da lista) gera o **TBRHelp.md**, grava em `Download/` e abre o **compartilhar** do Android (`AiHelpShare`: `BusyDialog`, escrita UTF-8, `MediaScannerConnection` devolve o `content://`, `ACTION_SEND` com `EXTRA_STREAM`, `ClipData` e `FLAG_GRANT_READ_URI_PERMISSION`; sem FileProvider nem manifest). Falhas mostram o motivo (ex.: falta “acesso a todos os arquivos”).
- **Documento** (`AiHelp`, 77 KB, **em inglês**, com a instrução de **responder no idioma do usuário** e o aviso de que o arquivo não está necessariamente nesse idioma): 0 instruções à IA · 1 o app · 2 arquivos e pastas (Runtime, `TBRE/Chars`, `TBRE/Backups`; os `.txt` mandam sobre os binários) · 3 formatos exatos de `000.dac.txt`, `000.bin.txt` (`$GOLPE_n`, `$HDAT_14+t` / `$REACAO_DANO_t`) e `000.cnv.txt` · 4 o que pedir ao usuário e como ele obtém · 5 dicionário (flags dos hits, `$OPT`, `WACTFLAG`, reações, ATK_FLAGS, sons, sprites) · 6 mecânica (tempo, speed/accel, hit, sequências, pouso, projéteis, efeitos) · 7 limites · 8 receitas · 9 checklist de validação · 10 como testar · 11 telas e botões · 12 **contrato de saída** (blocos completos, alvo na 1ª linha, onde colar, como testar) · 13 salvar e backups · 14 FAQ · Anexo A: a ajuda do app inteira, literal (PT).
- **Gerado, não digitado**: os exemplos de ANO / golpe / reação / CNV passam pelo **leitor e pelo escritor do próprio app**; a tabela do HDAT 4 vem de `DacText.MOVE_KEYS`; os grupos de reação de `Opt1`; os limites de `GameRules`; o anexo de `HelpText`. O texto fixo fica em `AiHelpDoc` (gerado de `aihelp_src/*.md`).
- AJUDA: nova seção “Ajuda de IA (TBRHelp.md)” (e tags de busca: ia, chatgpt, gemini, claude, markdown…).
- Testes: `AiHelpTest` (46, ver 3.1zb: determinismo, tamanho, instruções de idioma, seções 0–14 + anexo, exemplo de ANO relido pelo parser e reescrito igual, exemplo de golpe relido, todos os campos do HDAT 4, limites iguais ao `GameRules`, grupos de `Opt1`, anexo literal). Escrita em Download e intent só compilados e revisados.

### 3.1z Idioma da interface (PT / EN / ES) — infraestrutura
- Botão **PT → EN → ES** ao lado do AJUDA (barra de título): troca **na hora**, grava a escolha no aparelho (`SharedPreferences "lang"`) e retraduz a tela aberta no lugar (nada é recriado, as edições ficam). O código e os dados internos continuam em português: arquivos, chaves, textos gravados e o **TBRHelp.md não mudam**.
- `I18n` (puro): `tr(texto)` resolve 1) texto exato do catálogo, 2) **modelo com `{0}`** (ex.: `Salvando em {0}`), 3) **fragmentos** por palavra inteira (maior primeiro), com cache; PT é custo zero. Termos neutros (DAC, CNV, ANO, MPT, HDAT, OPT, SEQ, END) ficam iguais nas 3 colunas. Catálogo em `I18nCatalog*` (triplas PT/EN/ES).
- Ganchos mecânicos: `new TextView` → `I18nTextView` (guarda o original; `getText()` devolve o original, a lógica não vê a tradução; `retranslate()`), `CheckBox` → `I18nCheckBox`, `AlertDialog.Builder` → `I18nBuilder` (título, mensagem, botões, listas), `Toast.makeText` → `I18nUi.toast`, `Canvas.drawText` → `I18nUi.drawText` (timelines e prévias desenhadas à mão), `clip()` das timelines e o adaptador dos seletores.
- Testes: `I18nTest` (28). Só compilado e revisado na tela.

### 3.1za Idioma: catálogo da interface (1 361 entradas PT/EN/ES)
- Catálogo gerado de tabelas (`I18nCatalog0..4`): menus, botões, timeline, gaveta, reações, sprites, links, som, salvar, diálogos, avisos e toasts. Textos com números viram modelos `{0}` (os `%d`/`%s` do código são convertidos). Palavras em português são traduzidas; DAC, CNV, ANO, MPT, HDAT, OPT, SEQ, END e `$KEYS` ficam como estão.
- Dados não são traduzidos: o painel **DADOS** e os nomes de arquivo do seletor (`noTranslate`).
- **Teste de cobertura** (`I18nCoverageTest`, 5): o catálogo não tem entradas vazias, os `{n}` batem nas 3 colunas e as 2 243 literais de UI dos 77 arquivos de tela estão no catálogo ou na lista de ignoradas (`I18nIgnored`). Textos montados em tempo de execução sem modelo no catálogo continuam em PT até serem adicionados.

### 3.1zb TBRHelp.md — seção 15 “Engine internals” (respostas às 97 perguntas da IA)
- Nova seção 15 (≈ 78 KB, 13 subseções) com as respostas, **cada uma com rótulo de confiança**: `[CODE jogo:N]` (lido no `TCBManajer.java`), `[DOC]`, `[INFERRED]`, `[UNKNOWN]`. Cobre: fluxo do ataque, arquivos e tabelas (HDAT 3/4/5/6/13/14+), caixas e colisão (md 935), dano e fórmula, reações/COMP/paredes, física, projéteis e sub-projéteis, ANO/MPT/links/sprites, efeitos, som e câmera, flags (hit 0–125, `ATK_FLAGS`, `wOptFlag`), MDs e variáveis, exemplos frame a frame com **10 testes de validação**, e o que **não dá para reproduzir só com os dados**.
- **Nomes das perguntas × nomes reais**: vários não existem no jogo (`playerattack`, `damageend`, `Commonnormal`, `hitFlag`…); a tabela da seção diz o nome real (`PlayerAttack[]`, `AttackEnd`, `CommonNormal`, `CheckOptFlag`, `DamageChange`, `_ActReqMain`…).
- A tabela do HDAT 4 ganhou a coluna **byte offset** (o jogo guarda os 24 campos em ordem diferente da dos `$GOLPE_n`). Bits do WACTFLAG completados: 1 = repetir, 16/32 = espelhar X/Y.
- Pergunta do GOLPE_9 (Kamehameha, ANO 49/50/51): o papel de um ANO é dado só por **qual campo do `$GOLPE_n` aponta para ele**; a seção explica como mudar só dano, reação, velocidade, nº de acertos e animação de impacto.
- Testes: `AiHelpTest` 31 → 46 (seção 15 e subseções, rótulos, tabela de nomes, citações `jogo:N` dentro do arquivo do jogo quando ele está ao lado, nomes de métodos existem no código, offsets do HDAT 4).

### 3.1zc Correção: projétil ABAIXO / ACIMA estava invertido nos textos
- No jogo (jogo:31664-31672) `PROJETIL_ANIM_ABAIXO` vale quando o **atacante** está ≥ 51 px mais baixo que o oponente e `PROJETIL_ANIM_ACIMA` quando está ≥ 37 px mais alto. A tela de ramificação (“oponente bem abaixo/acima”), a AJUDA e o TBRHelp.md diziam o contrário. Agora: “atacante bem abaixo/acima do oponente” (PT/EN/ES). O simulador (`BattleSim`) já estava certo.

### 3.1zd Idioma: AJUDA inteira em EN/ES, busca nos três idiomas e botão na tela de ajuda
- **Todos os textos da AJUDA** (capítulos, resumos, títulos, corpos, tags e a dica de ordem dos botões: 209 trechos) estão em PT/EN/ES em `I18nHelpCatalog*` (gerado de `i18n_src/help_tr.json`). Entram só na busca exata (textos longos não viram “fragmentos” do `I18n.tr`). PT continua a fonte; o **anexo A do TBRHelp.md usa sempre o PT** e o arquivo é **idêntico** em qualquer idioma.
- **Botão PT/EN/ES no cabeçalho da AJUDA** (ao lado de “Ajuda de IA”): troca na hora, grava a escolha, atualiza o botão da tela principal (`I18nUi.registerLang`/`cycle`) e re-renderiza capítulos, busca e dica de busca. Seção nova **“Idioma da interface”** em *Primeiros passos*.
- **Busca multilíngue**: os termos de cada seção em PT + EN + ES entram juntos no índice, e os grupos de sinônimos ganharam palavras em EN/ES (digitar `velocity`, `velocidad` ou `velocidade` acha Movimento; `backup`/`respaldo`/`backup` acha SALVAR; `landing`/`aterrizaje` acha POUSO). O trecho mostrado vem do texto no idioma atual (se só bateu em outro idioma, mostra o começo do texto).
- **Compartilhamento**: a mensagem que acompanha o `TBRHelp.md` (`AiHelp.shareMessage()`), o título do seletor e os avisos/toasts de erro passam por modelos `{0}` do catálogo e saem em PT/EN/ES; o arquivo e o nome dele não mudam.
- Testes: `HelpI18nTest` (25): todo texto tem EN e ES, números e identificadores (`$KEYS`, `SEQ_PROJETIL`, ANO, DAC…) iguais nas três línguas, buscas em EN/ES, trecho da busca em EN, mensagem e título de compartilhamento nos 3 idiomas e `TBRHelp.md` idêntico em PT/EN/ES. Troca de idioma e tela de ajuda só compiladas e revisadas (não rodadas num aparelho).

### 3.1ze TBRHelp.md: dois modos (dúvida do app × gerar ataque) — sem binário e sem DADOS / EDITAR
- Seção 0 do MD agora define **Modo A** (dúvida/“como faço…?”: a IA ensina **só pelas funções do app**, toque a toque com os nomes exatos dos botões) e **Modo B** (“gere/altere um ataque”: a IA **pede os `000.dac.txt`, `000.bin.txt` e `000.cnv.txt`** e devolve `.txt`). Regras “Never”: nunca ensinar a editar `000.dac/.bin/.cnv` (binários) e nunca mandar o usuário ao painel `▲ DADOS / EDITAR` (abas, lápis ✎, colar texto).
- Ajustados: seções 3/4 (sem pedir o texto do DADOS/EDITAR), 8 (receitas sem colar no painel), 11 (painel descrito como existente, “não ensine”), **12 (contrato de saída só para o Modo B: entrega `.txt`, onde pôr os arquivos, tocar em ↻)**, 13, 14 (FAQ do ↻), 1/2 (barra de título) e 15 (aviso de escopo).
- AJUDA do app: “Ajuda de IA (TBRHelp.md)” com o novo fluxo, “Barra de título” e nova seção **Recarregar pastas (↻)** (PT/EN/ES).
- Testes: `AiHelpTest` 46 → 52 (modos A/B, proibições, nenhuma instrução de colar no DADOS/EDITAR fora das proibições, contrato do Modo B com ↻, ↻ no manual/FAQ/salvar).

### 3.1zf Botão ↻ — recarregar as pastas que o app lê
- Novo botão **↻** na barra de título, antes do de idioma e do AJUDA. Pede confirmação (“Edições não salvas serão perdidas”; o app não detecta edição pendente, então o aviso é sempre mostrado) e então **relê** `assets`: pastas `charNN`, effect, `bobj*`, charf, gamedata (já era relido a cada golpe) e recria o `SoundPlayer` (sons). Libera os `CharAssets` antigos (nenhum cache estático precisou ser limpo).
- Volta ao **mesmo personagem, oponente, modo e golpe/ANO** pelo nome da pasta (`FolderReload`, Java puro); se a pasta sumiu abre a primeira da lista e avisa; o índice de golpe/ANO é limitado ao tamanho novo. Sai do sub-editor de ANO/projétil e fecha a gaveta.
- `FilePicker`, `Saver`, listas de transplante/importação de reação e sons comuns já liam as pastas na hora (não precisam do ↻).
- Testes: `ReloadTest` (20): manter/trocar/limpar a seleção, pasta nova/apagada, lista vazia, `clamp`, e o diálogo/toasts nos 3 idiomas. O botão e o diálogo só foram compilados e revisados.

### 3.1zg Timeline: os rótulos do começo das faixas traduzem na hora
- **Causa**: ao trocar o idioma só os `TextView`/`CheckBox` eram retraduzidos; as views desenhadas à mão (`TimelineView`, `ReactionTimelineView`, prévias) não eram repintadas e o gutter ficava em PT até algo redesenhar. `I18nUi.retranslate` agora chama `invalidate()` em toda view da árvore.
- Os rótulos do gutter eram traduzidos **duas vezes** (em `clip` e de novo em `drawText`); agora uma vez só, medindo a largura do texto já traduzido (EN/ES são mais longos) — `TimelineView` e `ReactionTimelineView`.
- Catálogo: faltavam `vel` (→ `spd`/`vel`), `acel` (→ `accel`/`acel`) e `+ estilo` (→ `+ style`/`+ estilo`) nas dicas e no resumo de movimento da faixa.
- Teste novo `TimelineLabelsTest`: percorre os rótulos do gutter das duas timelines (nomes de objetos e ações, janelas 512, filtros `#`, PASSOS/SPEED/VOZ/EFEITOS/SEQ/CURVA, REAÇÃO/JOGO n, passo n, chips de parede/voz/SEQ, marcas de transição…) e exige EN e ES diferentes do PT (exceto cognatos/termos neutros listados) com os números preservados. Repintura na troca só compilada e revisada (não rodada num aparelho).

### 3.1zh Timeline: loop do ANO, duração, ponteiro, clique no clipe, DIVIDIR e velocidade X/Y
- **Adicionar/duplicar sprite não era alcançado** (a duração e o ponteiro travavam no último sprite antigo): dois motivos. (1) O **loop** ficava no frame antigo e o simulador dava a volta ali; (2) um frame **sem TIME** segura para sempre no jogo, então nada depois dele é alcançado. Agora, ao pôr um sprite depois de um frame sem TIME, este passa a durar 3 ticks (o que a timeline mostrava) e o sprite novo é quem segura (`FrameOps.timed`); no meio da ANO a cópia dura o mesmo que o original.
- **O loop é do ANO**: `FrameOps.normalizeLoop` leva o bit de loop para o novo último frame depois de qualquer edição (adicionar, duplicar, dividir, excluir o último, remover frame na página Tempo, apagar sprites em massa). Aplicado no `AttackView.replaceAno`/`replaceAnoAsync`/`SpriteMass.deleteFrames`; só desliga quando o usuário desliga (`AttackView.setLoop`). Loop num frame do meio (ANO importado) não é mexido.
- **Clique num clipe não leva o ponteiro ao início**: sprite, som, velocidade, estilo, objetos (hitbox, hurtbox, projétil, efeito, ação) e transições só movem o ponteiro se ele estava fora do clipe (antes → início; depois → último tick); dentro, só seleciona (`FrameOps.clipTarget`, `TimelineOps.seekIntoClip`, novo `TimelineView.Listener.onClip`). Editar no inspetor também mantém o ponteiro quando ele já está no frame (`AttackView.seekToPatKeep`), então dá para ficar no meio de um frame.
- **DIVIDIR**: um trecho de **velocidade** agora se divide no ponteiro (o frame do corte vira a cabeça de um 2º trecho com a velocidade já acumulada, 16.16 exato, deslocamento total igual — `FrameOps.splitMove`); um **objeto** (hitbox etc.) se divide exatamente no ponteiro, mesmo no meio de um frame (antes só na divisa mais próxima e recusava objetos de 1 frame); mensagens claras quando o ponteiro está fora/no início.
- **Velocidade X e Y em faixas separadas** ("Velocidade X" / "Velocidade Y"): cada trecho aparece na faixa do eixo que usa (speed ou accel); o menu é o mesmo, tocar numa célula vazia cria a velocidade daquele eixo; o resumo (timeline fechada) continua uma linha.
- AJUDA (PT/EN/ES): faixas, ponteiro/clique/loop e DIVIDIR atualizados.
- Testes: `TimelineEditTest` (41): duplicar/inserir/apagar/dividir com loop (duração antes/depois via `BattleSim.measureLength`), frame sem TIME, `clipTarget`, eixos, `splitMove` com e sem accel. Gestos da timeline só compilados e revisados (não rodados num aparelho).

### 3.1zi Agarrão na transição, Hurtbox em massa, POSE e projétil/efeito sem pouso de personagem
- **Tentar agarrão** (md 882 do jogo: o agarrão dispara no fim do ANO inicial ou no 1º frame com a janela 512, com `ATK_FLAGS` 8 ou 128 sem sequência): ganhou uma **marca na faixa de transições** (`SeqMark.GRAB`, "AGARRAR → ANO+1") no fim do ANO inicial (ou no 1º frame com a janela), mesmo sem o bit 512 em nenhum frame; o objeto "Tentar agarrão" continua editável. **Menu com a distância do agarrão** (HDAT 3, byte 5) nos dois lugares: `CharData.setPullDistance`, leitura/gravação de `$DISTANCIA_AGARRE` no `000.bin.txt` (`CharAssets.applyBinTxt`, `Saver.patchBody`), com desfazer.
- **Hurtbox**: dois botões no menu — "Aplicar a todas as Hurtboxes (neste ANO)" e "(todos os ANOs)" — copiam posição e tamanho para todas as outras hurtboxes (flag 1) sem mexer em hitbox, `cut` nem frames sem hurtbox (`ObjModel.setAllCoords`); "todos os ANOs" pede confirmação, usa um único desfazer (`replaceAnosAsync`) e lembra de gravar com SALVAR > todos os ANOs.
- **POSE** (menu TELA, abaixo de FR-PREVIEW SET; desligado por padrão): desenha o **ANO 0** do atacante por baixo, na mesma âncora do golpe, para comparar altura e tamanho.
- **Bug corrigido — projétil/efeito aberto sozinho era tratado como personagem** (pousava no ANO 13 ao tocar o chão): `CharData.Move.rootKind` (personagem, projétil, efeito) definido ao abrir o ANO (projétil = referenciado pelo golpe, efeito = ANO 201–255 ou "✎ EDITAR EFEITO"). Projétil: segue o md 971 — no chão troca para `PROJETIL_COLISAO_CHAO` (`SUBPROJETIL_COLISAO_CHAO` no sub-projétil) se existir, senão continua; `y` preso ao chão sem zerar a velocidade, sem som de pouso, sem POUSO/QUEDA, sem sombra. Efeito: sem chão, pouso, queda nem sombra. ANOs de personagem seguem iguais.
- AJUDA (PT/EN/ES) e MD atualizados. Testes: `GrabEditTest` (12), `HurtboxAllTest` (8), `RootKindTest` (18), POSE em `TimelineEditTest`. POSE, botões e a marca do agarrão só compilados e revisados (não rodados num aparelho).

### 3.1zj Adicionar sprites de outros pacs (.cnv)
- No seletor de **+ ADICIONAR** (lista de SPRITES) agora aparecem também os arquivos **.cnv / .cnv.txt**. Tocar num deles lê o CNV e **recorta as folhas PNG da mesma pasta** (leitura sem efeitos colaterais: não apaga PNGs soltos nem regrava nada na pasta de origem) e abre o **Picker de sprites do pac**: grade com miniaturas, toque para marcar, **TODAS / NENHUMA** e **ADICIONAR n** (`PacSpritePicker`, `FilePicker.IMAGES_CNV`).
- **Escala**: o personagem usa escala 2 e efeito/charf/bobj escala 1; sprites de pacs de escala 1 entram **ampliadas 2x sem suavizar** (pixel art) para manter o tamanho na tela. Recortes repetidos (várias entradas apontando para a mesma região) entram uma vez só; entradas vazias são ignoradas; o CNV do próprio personagem é recusado.
- Lógica pura em `SpriteImportPlan` (sprites distintas, fator de escala, nomes de arquivo) com `SpriteImportPlanTest` (27). A AJUDA (PT/EN/ES) descreve o recurso. Só compilado e revisado (não rodado num aparelho).

### 3.1zk Sons globais em OGG (`se_NN` / `se_NNN`) e SE de 0 a 255
- **`$SE` agora é um byte sem sinal (0–255)** (patch do `TCBManajer`): `Dac` lê `u8` (antes `s8`), o `$SE` do texto é validado de 0 a 255, o seletor da página Som anda pelos ids certos e o simulador toca o arquivo certo (`SoundIds`, `BattleSim.pushSe`): id 0 nada · **1–79 = se_00…se_78** (comuns do jogo) · **80–99 = áudios do personagem** (até 20) · **100–255 = comuns novos se_99…se_254**. Nomes de arquivo com 3 dígitos a partir do `se_100.ogg` (`SoundPlayer.commonFiles` aceita `se_NN` e `se_NNN`).
- **Adicionar som global**: o ＋ da lista de sons pergunta se é do personagem (WAV) ou global (OGG). O novo global é o próximo `se_`: depois do `se_78` o app cria **arquivos silenciosos `se_79…se_99`** (preenchimento, escondidos da lista, com aviso antes) e o novo vira **`se_100`** (id 101); depois `se_101`… até `se_254` (id 255). Nunca apaga um `se_`: “excluir” = silenciar.
- **Mesma edição dos WAV** nos globais: usar, ouvir, **cortar**, **mais (volume, tom, velocidade)**, **substituir** e **silenciar**. O áudio é decodificado para o formato do jogo, editado e gravado de novo como OGG.
- **Conversor OGG** (`OggEncode`): sem biblioteca extra, **Opus dentro de contêiner Ogg** com `MediaCodec` (`audio/opus`) + `MediaMuxer` (`MUXER_OUTPUT_OGG`), **Android 10 / API 29 ou mais novo** — em aparelho sem isso o app avisa e não grava. O codec é Opus (não Vorbis): o `SoundPool` do Android lê; o jogo precisa abrir o arquivo com o mesmo decodificador.
- Testes puros: `SoundIdsTest` (61: ids↔arquivos, 3 dígitos, preenchimento, limite 254, `$SE` 200 no texto, o simulador tocando 150/85/100/8/255, reamostragem para 48 kHz). **O codificador OGG só foi compilado** (não roda na JVM): precisa ser conferido num aparelho com Android 10+ e no jogo patchado.
- AJUDA (PT/EN/ES) e MD atualizados (ids, `se_NNN`, preenchimento, Opus).

### 3.1zl Objeto corta na divisa de frames, Movimento em SPEED X / SPEED Y, botão SONS e crash com frame -1
- **DIVIDIR de objeto** (hitbox, hurtbox, projétil, efeito, ação): o objeto pertence ao frame e não acaba no meio dele, então corta na **divisa de frames mais próxima do ponteiro** (empate: a anterior) e o **ponteiro vai para essa divisa** (`ObjModel.splitBoundary`). Objeto de um frame só: avisa que não há divisa. DIVIDIR de sprite e de trecho de velocidade continua cortando no ponteiro.
- **Menu Movimento em duas seções, SPEED X e SPEED Y** (como Hitbox/Hurtbox): cada eixo tem velocidade, velocidade recebida (com “Remover” só do eixo — `FrameTools.zeroReceivedAxis`), aceleração (só aparece quando o *eixo* tem speed — `showAccelAxis`), **total de pixels do eixo** e **REMOVER SPEED só do eixo** (`FrameTools.clearAxis`; o trecho que vinha do frame só sai se o frame ficou sem movimento nenhum). Frame inicial/duração do trecho e MANTER VELOCIDADE aparecem nas duas e uma atualiza a outra. Faixas da timeline e filtro `#` não mudaram.
- **Botão `SONS` de volta ao menu superior**, ao lado de `SPRITES` (agora 15 botões): abre a lista de sons (personagem + globais, ＋, ouvir, cortar, mais, substituir, silenciar) **sem “Usar este som”** — só gerenciar. Quem já tinha ordem de botões salva recebe o SONS logo depois do SPRITES (`ToolbarOrder.NEXT_TO_PREVIOUS`).
- **Gerenciamento de frames sem crash por índice -1** (`ArrayIndexOutOfBoundsException: index=-1` em `FrameOps.applyAll`, ao copiar de um frame para outros): a página “Copiar” continuava ativa quando a seleção passava para um objeto/link/transição (`idx = -1`) e o APLICAR chamava `applyAll(…, -1, …)`. Correção em três camadas: (1) a gaveta volta para a grade ao selecionar objeto/link/transição e `applyAllTab`/`applySpriteTab`/`follow` só agem com sprite selecionado (`spriteSelected`); (2) todas as operações puras de `FrameOps`, `FrameTools` e `ObjModel` ignoram índice fora de `[0, len)` em vez de lançar (`FrameOps.ok`); `TimelineOps` recusa índice inválido com aviso; (3) `FrameIndexRobustTest` chama por reflexão cada operação pública com índices −2, −1, 0, len−1, len, len+3 (56 métodos, ~3,9 milhões de chamadas): 0 exceção.
- AJUDA (PT/EN/ES: SONS, Movimento X/Y, DIVIDIR, “Os 15 botões”) e MD (11.1/11.2/11.3) atualizados. **Telas novas (Movimento X/Y, botão SONS, DIVIDIR) só compiladas e revisadas — não rodadas num aparelho.**

### 3.1zm Importar sprites de outro pac: `.cnv` em texto
- O `.cnv` que vem de outros pacs é **texto** (`CNV_TXT_ { $SPR_0:{$src:5, $x:96, …} }`) com as coordenadas **originais** (sem ×2), e o app o lia como binário (“CNV vazio ou pequeno demais”/lixo). `CharAssets.readCnv` agora tenta **texto primeiro** e só cai no binário se não houver nenhuma entrada de texto. Ordem: `.cnv.txt` (coordenadas finais, já ×2 no personagem → o app divide por 2) → `.cnv` em texto (coordenadas originais, lidas como estão; o recorte multiplica pela escala do personagem) → `.cnv` binário.
- `Cnv.fromTxt(texto, personagem, unidadesFisicas)`: `false` = `.cnv` em texto (não divide), `true` = `.cnv.txt` (divide por 2 em personagem, como antes).
- Testes: `SpriteImportPlanTest` 27 → 38 (amostra do usuário em personagem e em effect, BOM e comentário, `.cnv.txt` ao lado vence, binário continua, arquivo pequeno dá erro, plano e ampliação 1→2). AJUDA (PT/EN/ES) e MD atualizados. 
- **Seletor de sprites de outro pac** (`PacSpritePicker`): uma linha de diagnóstico mostra como o arquivo foi entendido (`.cnv.txt` coordenadas finais ÷2 / `.cnv` texto coordenadas originais / binário) e o recorte da 1ª sprite (ex.: `SPR_0: 0,18 240x60 → recorte 0,36 480x120`); o botão **ESCALA: AUTO (2) / 1 / 2** força a escala do pac quando a pasta não é reconhecida. Pasta de personagem agora é `char` + número com qualquer sufixo/maiúscula (`char05_copia`, `Char5`); `charf`/`chara` continuam escala 1. Só compilado/revisado, não rodado num aparelho.

- **Escala do pac deduzida (corrige `.cnv` em texto que aparecia como “escala 1 → 2” e saía com recorte pela metade)**: a escala 2 não depende mais só do nome da pasta. Agora vale personagem se a pasta é `char`+número **ou tem `000.bin`**; e, se ainda assim sobrar escala 1 (e o arquivo não é `.cnv.txt`), o app compara os recortes com o tamanho dos PNG (`SpriteImportPlan.guessScale`): se todos só cabem nas folhas multiplicados por 2 e, multiplicados, quase enchem alguma folha (≥ 85 %), é pac de escala 2 e o CNV é relido assim — a linha de diagnóstico avisa “escala 2 deduzida do tamanho das folhas”. Só promove 1 → 2, nunca o contrário; o botão ESCALA continua mandando.

### 3.1zn Seletores de pasta e CNV: “...↑”, VOLTAR e substituir por sprite de CNV
- **“...↑”**: em todo seletor de arquivos (sprites e sons) o primeiro item de qualquer pasta, **menos a raiz**, é `...↑`, que sobe um nível (`FilePicker.hasUpRow`; `SUBIR`/`PRINCIPAL` continuam).
- **VOLTAR na escolha das sprites de um CNV** (botão ou voltar do celular) volta ao **seletor de arquivos, na pasta onde estava o .cnv**, em vez de cair na lista de sprites (também quando o CNV é do próprio personagem ou não abre).
- **Substituir imagem** agora mostra também os `.cnv`/`.cnv.txt`: tocar num abre as sprites dele e dá para escolher **uma só** (marcar outra desmarca a anterior; botão SUBSTITUIR); ela é recortada com a escala deduzida do pac, ampliada 1→2 se preciso, e grava no lugar da SPR_n.
- AJUDA (PT/EN/ES) e MD atualizados. **Telas só compiladas/revisadas, não rodadas num aparelho.**

### 3.1zo `$src` do CNV é o ÍNDICE da folha, não o número do nome
- Numa pasta com `017.png`, `018.cnv` e `019.png`, o `$src:18` procurava `018.png` (que não existe) e o recorte falhava. Agora `$src` = **posição da folha na lista dos `NNN.png` da pasta, em ordem de nome** (`SheetFiles`, Java puro): `$src:18` = `019.png`. Com PNG contínuos a partir de `000` (o caso comum) índice e número coincidem — nada muda.
- Todos os usos de recorte seguem a regra: preview (`SpriteCache`), lista de sprites/edição (`SpriteLib`: abrir, gravar, folha nova com o próximo número depois da última, `shiftWavs` pelo maior número de folha), seletor de sprites de outro pac e dedução de escala (`PacSpritePicker.sheetSizes`). Folha inexistente vira erro legível (“folha $src:N não existe (a pasta tem K folha(s) PNG)”), sem exceção por frame.
- Testes: `SheetFilesTest` (17: o caso 017/018.cnv/019, ordem por número, maiúscula, ignora `.cnv`/`.txt`/PNG com outro nome, fora do intervalo, `nextNumber` com buraco). `SpriteCache`/`SpriteLib`/seletor só compilados/revisados, não rodados num aparelho. AJUDA (PT/EN/ES) e MD atualizados.

### 3.1zp Recorte que falha mostra o motivo, num alerta que dá para copiar
- `CropProblem` (Java puro) diz **por que** um recorte do CNV não funciona: sem entrada, recorte vazio, coordenada negativa, **folha `$src:N` inexistente** (cita quantas folhas PNG a pasta tem e que `$src` é índice), **PNG ilegível**, **recorte fora da folha** e **passa da borda** (aviso; mostra a folha com tamanho, x/y finais e o multiplicador do CNV).
- **Alerta copiável** (`ErrorListDialog`): texto selecionável + botão COPIAR (não fecha) + FECHAR. Abre sozinho quando: o preview não consegue recortar sprites (lista de tudo, com a pasta: atacante, oponente, efeitos, charf, bobj — uma vez por conjunto de erros); o seletor de sprites de outro CNV abre com sprites que não recortam (cabeçalho com o caminho do CNV, como foi lido — `.cnv.txt` / texto / binário —, a escala e as folhas PNG da pasta em ordem); adicionar/substituir falha (antes só “N falharam”; agora `SPR_n: motivo`, inclusive erro ao gravar as folhas).
- Na lista de sprites do personagem e no seletor de outro CNV, a sprite com problema mostra o motivo em **vermelho** e há uma faixa “N sprite(s) com problema de recorte — toque para ver”.
- `SpriteLib.cutProblem/cutProblems`, `SpriteCache.errors()`, `AttackView.spriteProblems()`. Testes: `CropProblemTest` (11). Telas e alertas só compilados/revisados, não rodados num aparelho. AJUDA (PT/EN/ES) e MD atualizados.

### 3.1zq + do fim da timeline (CAMADA / Adicionar Sprite), thumb 1:1, SPEEDLINE e Teleportar atrás do oponente
- **Erro de recorte**: o alerta deixou de abrir sozinho (preview e seletor de CNV). Ficam o motivo vermelho em cada sprite, a faixa “N sprite(s) com problema de recorte — toque para ver” (abre o alerta copiável) e o alerta depois de adicionar/substituir que falhou.
- **+ do fim da faixa de sprites** abre um menu: **CAMADA** (− N +; **maior = a ANO aparece mais em cima**; no jogo o `iActPrio` maior fica mais atrás, então o app grava o `$PRIO` invertido — `LayerOps`) e **Adicionar Sprite** (cria depois do ponteiro e abre a gaveta direto na página **Imagem**).
- **Thumb da sprite** no menu Imagem: quadrada (1:1), metade da largura da tela.
- **Botão SPEEDLINE** (menu superior, depois de HITBOX; 16 botões): pontos e linhas **verdes** com a trajetória do movimento — um ponto no início de cada Frame que se move, um no fim do último, um extra onde a altura muda de sentido dentro de um Frame; marcador amarelo que anda pelo Frame atual e destaque no ponto do Frame atual. **Arrastar** um ponto muda só a speed do Frame anterior (o TIME nunca muda); **tocar** abre: *Remover este ponto* (o Frame perde a speed própria e continua com a do anterior; no primeiro ponto ou num movimento de 1 Frame tira a speed toda, com aviso e confirmação), *Zerar Speed* (sem manter e sem speed) e *Aceleração…* (prévia da velocidade por tick, accel x/y por Frame, velocidade do movimento em % mantendo o padrão e presets: começar devagar e acelerar rápido, acelerar até o fim, desacelerar rápido, desacelerar até parar, acelerar e desacelerar, constante — todos preservam o deslocamento total). Modelo puro `SpeedLine`/`AccelPresets`, conferido contra o `AttackSim`.
- **Teleportar atrás do oponente** (ações especiais do ＋): Puxar oponente (flag 40) com **x = −120** + **Flip X** no sprite, reconhecidos pela combinação (flag 40 com x < 0 + Flip X) como **um item só**: nome próprio, apagar/esticar/mover leva o Flip X junto, o **x do Teleportar atrás é sempre < 0** (o Puxar oponente comum continua sem limite) e o Puxar oponente comum não aparece (nem na faixa nem para adicionar) num Frame que já tem Teleportar atrás.
- Testes: `LayerOpsTest` (11), `SpeedLineTest` (38), `TeleportBehindTest` (17, inclui a simulação: o oponente vai para trás do atacante), `ToolbarTest` (84). Menu do +, thumb, SPEEDLINE (desenho, arrastar, toque, diálogo) e a gaveta do Teleportar atrás **só compilados/revisados, não rodados num aparelho**. AJUDA (PT/EN/ES) e MD atualizados.

### 3.1zr Prévia do menu de sprite a 30% e desfazer/refazer por hierarquias
- **Menu de sprite (página Imagem)**: a prévia fica 1:1 com **30% da largura da tela** e o **− N +** vai para a frente dela, em **x = 65% da largura** e centrado na altura da prévia (y + h/2); em tela estreita o x é limitado para o − N + caber inteiro.
- **Desfazer / refazer em dois níveis** (`UndoHistory`, Java puro; antes era uma pilha única sem refazer):
  1. **Dentro de um menu** (gaveta aberta = sessão): cada campo/botão alterado é um passo (o − + segurado continua fundindo em 4 s); **desfazer** vai do último passo até o original, **refazer** repõe tudo; mudança nova depois de desfazer **apaga o refazer**. Só enxerga o que foi feito naquele menu.
  2. **Depois de APLICAR**: o menu inteiro vira **um único passo** na pilha da timeline — o desfazer da barra desfaz o menu todo de uma vez e o refazer repõe tudo; ação nova apaga o refazer. Ações fora de gaveta (DIVIDIR, DUPLICAR, EXCLUIR, arrastar clipe, arrastar ponto da SPEEDLINE…) seguem como um passo cada.
  3. Fechar sem aplicar (✕/tocar fora) desfaz o que sobrou na sessão e a descarta.
- **Botões**: o “↶ DESFAZER” da barra virou **duas setas ↶ ↷ de larguras iguais** que somam a largura antiga; a gaveta ganhou o **↷** ao lado do ↶. Com menu aberto os botões da barra agem no menu; sem menu, nas ações/menus já aplicados. Esmaecem quando não há o que desfazer/refazer.
- **Como**: cada ponto de `pushUndo` do `AttackView` (ANOs, frames, golpe, reações, agarrão, silenciar sons…) passou a registrar uma `Edit` (`UndoEdits.swapAno/swapAnos/swapMpts/swapMove/swapReaction/swapPull`) que, ao reverter, lê o estado atual e devolve a edição inversa — por isso refazer funciona sem reescrever cada edição. O editor de reações e o diálogo “Editar…” seguem na pilha externa (sem botão de refazer novo).
- Testes: `UndoHistoryTest` (32: sessão passo a passo até o original e de volta, refazer apagado por mudança nova e por fusão de chave, commit = uma entrada, sessão vazia, discard, ordem do grupo) e `UndoEditsTest` (10: ida e volta com DAC/golpe/agarrão reais). **A gaveta (prévia 30%), os botões ↶ ↷ e o fluxo APLICAR/✕ só compilados e revisados, não rodados num aparelho.** AJUDA (PT/EN/ES) e MD atualizados.

### 3.1zs SPEEDLINE: número do Frame e tick nos pontos
- Cada ponto da SPEEDLINE mostra o **número do Frame** (base 1, o ponto final mostra o Frame seguinte, onde o personagem estará); o **ponto de tick** (onde a altura muda de sentido dentro de um Frame) mostra o **tick dentro do Frame** (`t3`). Texto claro com contorno escuro; o do Frame atual em branco e maior; pontos muito próximos empilham o texto. `SpeedLine.label` testado (`SpeedLineTest` 42); o desenho do texto só compilado/revisado, não rodado num aparelho. AJUDA (PT/EN/ES) e MD atualizados.

### 3.1zt Aviso de hitstop ao avançar tick pelo botão
- **Por que o ponteiro “trava” com Hitbox**: o acerto congela o atacante por um **contador fixo de ticks** (`iHitStop`; no jogo `PlayerHitStop` = 10, ou 6 no golpe do tipo 12; **grupo 3 do OPT1 = metade, grupo 4 = nenhum**; projétil: defensor cheio, projétil metade, atacante nada). Não espera o hitspark: o hitspark (md 966) chama `_ActReq` todo tick **sem olhar o hitstop**, toca em paralelo na velocidade normal. Congelados: `time` do Frame, speed, aceleração e links; o defensor treme e a simulação segue. (O app usa 10 fixo: o golpe do tipo 12 fica 4 ticks mais longo que no jogo — lacuna conhecida, não alterada.)
- **Mantido o comportamento**; agora o **botão de avançar tick** trava o número do tick durante o hitstop e mostra um **aviso amarelo** (“Hitstop: o acerto congela o Frame por mais N ticks…”) que conta N a cada toque e some quando o tick é liberado. Timeline, barra de progresso, voltar tick e reprodução ignoram o aviso (os ticks passam normalmente). `HitstopNotice` puro + `HitstopNoticeTest` (8, inclui o hitstop real do `BattleSim`: aviso = passos parados). O aviso na tela só compilado/revisado. AJUDA (PT/EN/ES) e MD atualizados.

### 3.3 Menu AJUDA
Botão **AJUDA** (barra de título, à direita): descreve, pela utilidade na edição e na execução de golpes, SALVAR, CHAR (POS/FLY/CHAR OBJ), SPRITES, LINKS, GOLPE SET, HITBOX, TEMPO, CENTRO, POUSO, TRANSIÇÃO, VIRAR, HIDE-EFF, FR-PREVIEW, TELA, e o resto da tela (MODO golpes/ANO, barra de tempo e transporte, DIVIDIR/DUPLICAR/EXCLUIR/DESFAZER, timeline com ＋ e #, ferramentas, DADOS/EDITAR, avisos). Texto em `HelpText` (Java puro; teste confere que todo botão do menu tem entrada). Seção nova **Speed x Accel**: speed = velocidade constante (px por tick); accel = soma o valor na velocidade atual a cada tick, vale só no frame em que está escrita e a velocidade acumulada só passa adiante com Manter velocidade (bit 64); exemplos de 250 px (speed 50 × 5 ticks) e 500 px (accel 50 × 5 ticks).

### 3.4 Textos corrigidos

- Flag de link `0x1000` (SPAWN_POS) estava descrita **ao contrário**: bit ligado = o link **não** copia a posição do pai ao nascer (jogo:30047-30056).
- Flag 26: vale para qualquer defensor (humano ou CPU). Flag 33: o bit que ela liga não é lido por ninguém (sem efeito). OPT 1024: também desliga o impacto na parede. Nota do FLY corrigida.
- Painel da flag 20 e overlay da flag 20/40 (itens 12-13).

## 4. Validador `GameRules` (novo)

Avisos na barra ao abrir o golpe, aviso na prévia ao editar e linha de ATENÇÃO ao salvar. "Fecha o jogo" = exceção dentro do `Run()` (`bThreadActive = false`).

- > 10 caixas por frame (`hit[50]`) · > 13 links (`lnk[13]`) · > 255 frames · tempo > 255 · OPT 1 > 249 com hitbox → **o jogo fecha/erra**.
- Imagem fora do CNV (frame fica sem desenho) · link de link e caixas em ANO de link (ignorados: o jogo só olha a ANO raiz, `CheckObjHit` jogo:2468).
- Interpolação (`OPT[2]`) com o frame seguinte tendo outra quantidade de caixas: o `_Hokan` lê o frame seguinte pulando as caixas do frame **atual** (jogo:29790-29800) → rotação/alfa/zoom errados.

## 5. Testes

Baseline 347 → ver tabela. Expectativas alteradas só onde o jogo prova que o app estava errado: flag 39 aparece no tick seguinte; agarrão com distância 0 é afastado pela separação de corpos.

| Teste | OK |
|---|---|
| BattleTest | 240 |
| FrameOpsTest | 56 |
| SpriteMassTest | 23 |
| AudioTest | 20 |
| SpriteAllocTest | 13 |
| OptimizeTest | 13 |
| EffectsTest | 12 |
| GroupsTest | 4 |
| **CutsceneTest (novo)** | 65 |
| **FidelityTest (novo)** | 26 |
| **GameRulesTest (novo)** | 31 |
| **SpawnDiagTest (novo)** | 50 |
| **LinkHierarchyTest (novo)** | 8 |
| **FrameToolsTest (novo)** | 47 |
| **ToolbarTest (novo)** | 84 |
| **ObjMoveTest (novo)** | 42 |
| **MoveTransplantTest (novo)** | 37 |
| **ReactionSlotsTest (novo)** | 20 |
| **ReactionTraceTest (novo)** | 26 |
| **SaverCopyTest (novo)** | 17 |
| **ReactionEditTest (novo)** | 35 |
| **HelpSearchTest (novo)** | 48 |
| **ReactionLayoutTest (novo)** | 23 |
| **AiHelpTest (novo)** | 52 |
| **I18nTest (novo)** | 28 |
| **I18nCoverageTest (novo)** | 5 |
| **HelpI18nTest (novo)** | 25 |
| **ReloadTest (novo)** | 20 |
| **TimelineLabelsTest (novo)** | 2 |
| **TimelineEditTest (novo)** | 50 |
| **GrabEditTest (novo)** | 12 |
| **HurtboxAllTest (novo)** | 8 |
| **RootKindTest (novo)** | 18 |
| **SpriteImportPlanTest (novo)** | 50 |
| **SoundIdsTest (novo)** | 61 |
| **FilePickerTest (novo)** | 5 |
| **SheetFilesTest (novo)** | 17 |
| **CropProblemTest (novo)** | 11 |
| **LayerOpsTest (novo)** | 11 |
| **TeleportBehindTest (novo)** | 17 |
| **SpeedLineTest (novo)** | 42 |
| **UndoHistoryTest (novo)** | 32 |
| **UndoEditsTest (novo)** | 10 |
| **HitstopNoticeTest (novo)** | 8 |
| **FrameIndexRobustTest (novo)** | 1 |

## 6. Já estava conforme (conferido, sem mudança)

Parser do DAC, `_ActReqMain`/`_Hokan` (exceto o quirk acima), desenho (flip/zoom/rotação/alfa/blend), escala ×2 só de personagem, `CheckObjHit`, hitstop, OPT 1, dano (inclui a escala de combo do HDAT 5: `XSize` conta *shorts* nas tabelas de 16 bits, então `XSize*2` bytes é uma linha), spawn de projétil, md 971/882/883/876/877, offsets de HDAT 2/3/4/7/13, auras 10-17/71-125, poeira por OPT, início de batalha, áudio e textos das flags de hitbox.

## 7. Não simulado / limitações

- Recorte de 480×320 e `bEffectFullScreen` (cam 1); z = 400 isento do zoom; cutscene de tela travada não acompanha o retângulo do zoom.
- Fila de SE (4 itens, 1 por frame); tremor de ±1 px do defensor no hitstop (só usado como condição da separação de corpos).
- Auras pedidas por projéteis (`tcbReqHit`) e md 918/911-915.
- Mapa 1 só aparece no log (o app não decide o fim da luta).
- Stun + projétil: o jogo pode travar a luta (projétil pausado nunca termina). O app reproduz.
- Se o dono morre (projétil) com stun ativo, o jogo não devolve o md do alvo; o app reproduz e o alvo fica parado.
- Cutscene num pack de efeito editado só no `.txt` não é recompilada (precisa recarregar o personagem).
- Arrastar os botões (`ReorderBar`) e o diálogo AJUDA usam `DragEvent`/`AlertDialog` do Android e **não foram executados num aparelho** (só compilados contra o `android-all` 13 e revisados); a lógica de ordem (`ToolbarOrder`) e os textos (`HelpText`) têm teste JVM.

## 8. Arquivos

Alterados: `Ano`, `AttackData`, `AttackRenderer`, `AttackSim`, `AttackView`, `BattleSim`, `CharData`, `LayoutManager`, `LinkScreen`, `MainActivity`, `ObjModel`, `Saver`, `TimelineTools`, `test/BattleTest`.
Novos: `CutsceneInfo`, `GameRules`, `GameTables`, `ToolbarOrder`, `ReorderBar`, `HelpText`, `FrameTools`, `Stepper`, `IconView`, `ThumbView`, `LinkPosDialog`, `LayoutManagerFmt`, `test/LinkHierarchyTest`, `test/CutsceneTest`, `test/FidelityTest`, `test/GameRulesTest`, `test/SpawnDiagTest`, `test/ToolbarTest`, `test/ObjMoveTest`.
