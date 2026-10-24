# Changelog — java_novo2-33 → java_novo2-34

Conferência do app (editor/simulador de golpes) contra o código do jogo (`TCBManajer.java` de `CodigoFonteLimpo.zip`).
Todas as referências `jogo:NNNN` são linhas desse arquivo; `app:Arquivo` é o arquivo do app. Foco principal: **cutscenes da flag 20**,
que não ficavam iguais ao jogo.

Baseline do 33: 347 testes OK. Agora: 700 testes OK (ver a seção *Testes*).

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
| BattleTest | 238 |
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
| **FrameToolsTest (novo)** | 36 |
| **ToolbarTest (novo)** | 65 |
| **ObjMoveTest (novo)** | 40 |

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
