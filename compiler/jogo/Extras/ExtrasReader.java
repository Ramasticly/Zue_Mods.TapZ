package com.namcobandaigames.dragonballtap.apk;

// ══════════════════════════════════════════════════════════════════════
// ExtrasReader — runtime do jogo, lado da LEITURA do gatilho de funções
// extras (antigo "OPT 225"). Substitui o antigo zExtras.
//
// Ponto de entrada único chamado por TCBManajer._ActReqMain:
//   ExtrasReader.isExtrasFlag(rawOptFlag)  — pergunta se é o gatilho especial
//   ExtrasReader.handle(...)               — se for, processa e devolve dataPtr
//
// TCBManajer NÃO conhece o valor 225, funcCount, nem nenhum código de
// função — tudo isso vive só aqui. Se um dia as funções extras forem
// descontinuadas, o único ajuste necessário é isExtrasFlag(...) sempre
// devolver false — nenhuma mudança em TCBManajer é necessária.
//
// FORMATO BINÁRIO do gatilho (a partir do dataPtr recebido em handle(),
// ou seja, já depois do rawOptFlag de 2 bytes ter sido consumido por
// _ActReqMain):
//   [1 byte]           funcCount   — quantas funções neste gatilho
//   [funcCount bytes]  funcCodes[] — um código de função por byte, na
//                                    ORDEM em que serão executadas
//   [payload variável] cada função abaixo lê e avança dataPtr sozinha,
//                       na mesma ordem de funcCodes[]
//
// Isso permite qualquer combinação de funções no mesmo frame (incluindo
// "ler uma OPT real", código FUNC_REAL_OPT), sem competir por espaço
// fixo. O Compiler (ExtrasBuilder, lado da escrita) é responsável por
// emitir os bytes desta MESMA forma e ordem — qualquer descompasso aqui
// desalinha a leitura de TODOS os campos seguintes do frame,
// silenciosamente.
//
// INSTÂNCIA POR OBJETO: uma ExtrasReader vive pendurada em obj.extras,
// criada sob demanda por _ActReqMain na primeira vez que o gatilho
// aparece para aquele ObjReq. Guarda o VideoTexture (se houver) e o
// estado de "leave" herdado do zExtras original.
// ══════════════════════════════════════════════════════════════════════
public class ExtrasReader {

    // Instância única gerenciada por este objeto — VideoTexture (GL).
    // Mesma vida útil de antes: uma instância por objeto, criada sob
    // demanda, morre com stopFunctions().
    private VideoTexture player;

    // NOVO — índice de textura animada (webp) ativo para este objeto,
    // via $GIF (FUNC_ANIM_TEX). -1 = nenhum (comportamento normal de
    // DrawImage/cnv se aplica). Diferente de "player" (VideoTexture),
    // isto NÃO é um orquestrador com estado de reprodução — é só um
    // índice estático guardado, consultado por
    // TCBManajer.DrawExec/getActiveAnimTexIndex() a cada frame. A
    // textura em si (e seu avanço de frame) vive em GameData.animTex[],
    // fora do ciclo de vida desta classe.
    private int _animTexIndex = -1;

    public static volatile boolean leave = false;

    // ── Códigos de função conhecidos ────────────────────────────────────
    // Cada um cabe em 1 byte (0-255). Adicionar uma função nova = só
    // adicionar mais um "case" abaixo com seu próprio código único;
    // nenhuma outra função existente precisa mudar.
    //
    // FUNC_SHADER foi adiado (ver histórico de planejamento) — quando for
    // retomado, basta reservar um novo código aqui (ex.: 200) e adicionar
    // o case correspondente em processFunction(), sem tocar em mais nada.
    private static final int FUNC_PLAY_VIDEO = 60;
    private static final int FUNC_ANIM_TEX = 61; // NOVO — $GIF: id (ver case abaixo p/ payload exato)
    private static final int FUNC_REAL_OPT = 250; // "ler uma OPT real" como função

    // Chamado por TCBManajer._ActReqMain para decidir se o rawOptFlag lido
    // é o gatilho especial de funções extras, ou uma OPT normal de
    // gameplay. Encapsula o valor mágico 225 — nenhum outro arquivo deveria
    // comparar contra 225 diretamente.
    public static boolean isExtrasFlag(int rawOptFlag) {
        return rawOptFlag == 225;
    }

    // Chamado por TCBManajer._ActReqMain quando isExtrasFlag(...) retornou
    // true. obj.wOptFlag e obj.OptData[0..3] já foram zerados pelo chamador
    // antes desta chamada (225 nunca é observável como wOptFlag real).
    //
    // dac/dataPtr: o buffer binário do .pac e a posição ATUAL de leitura,
    //   já avançada além do rawOptFlag de 2 bytes (ou seja, dataPtr aponta
    //   para o primeiro byte deste gatilho: o byte de funcCount).
    //
    // Retorna o dataPtr final, avançado além de TUDO que foi consumido
    // (funcCount + lista de códigos + payload de cada função executada),
    // para que _ActReqMain continue o parsing do restante do frame a
    // partir daí.
    public static int handle(GlobalWork global, ObjReq obj, TCBManajer tm, byte[] dac, int dataPtr) {
        if (obj.extras == null) {
            obj.extras = new ExtrasReader();
        }
        return obj.extras.instanceHandle(global, obj, tm, dac, dataPtr);
    }

    // Atravessa um gatilho de extras SEM executar nada e devolve o dataPtr
    // logo depois dele. dataPtr: logo após o rawOptFlag (225) de 2 bytes,
    // como em handle(). Usado por TCBManajer._Hokan, que lê o PRÓXIMO frame
    // só para interpolar e pulava a OPT com 6 bytes fixos — com $VID/$GIF no
    // próximo frame, o $RAZ era lido do lugar errado.
    //
    // Tamanhos: precisam bater com processFunction()/VideoTexture.playFromDac
    // (e com ExtrasBuilder, do lado do Compiler).
    public static int skip(byte[] dac, int dataPtr) {
        int funcCount = dac[dataPtr++] & 255;
        int codesStart = dataPtr;
        dataPtr += funcCount;
        for (int i = 0; i < funcCount; i++) {
            switch (dac[codesStart + i] & 255) {
                case FUNC_PLAY_VIDEO: {
                    // skip, flags, width, height (2 bytes cada) + opcionais
                    int flags = (dac[dataPtr + 3] & 255) << 8 | (dac[dataPtr + 2] & 255);
                    dataPtr += 8;
                    if ((flags & VideoRequest.FLAG_HAS_STOP_POINT) != 0) dataPtr += 3; // stopAno(2) + stopMpt(1)
                    if ((flags & VideoRequest.FLAG_HAS_NEW_BGM) != 0) dataPtr += 1;    // newBgmId
                    break;
                }
                case FUNC_ANIM_TEX:
                    dataPtr += 2;
                    break;
                case FUNC_REAL_OPT:
                    dataPtr += 6; // wOptFlag(2) + OptData[4]
                    break;
                default:
                    break; // código desconhecido: payload de tamanho zero (ver processFunction)
            }
        }
        return dataPtr;
    }

    // ── Lógica de instância (precisa ser de instância porque guarda o
    //    VideoTexture/player, que é específico de cada ObjReq) ──────────
    private int instanceHandle(GlobalWork global, ObjReq obj, TCBManajer tm, byte[] dac, int dataPtr) {
        try {
            int funcCount = dac[dataPtr++] & 255;

            if (leave) {
                stopFunctions();
                // Ainda precisamos consumir os bytes deste gatilho para não
                // desalinhar o restante do frame, mesmo estando "saindo".
                return skipAllFunctions(global, obj, tm, funcCount, dac, dataPtr);
            }

            // 1. Lê a lista de códigos primeiro (funcCount bytes seguidos).
            int[] funcCodes = new int[funcCount];
            for (int i = 0; i < funcCount; i++) {
                funcCodes[i] = dac[dataPtr++] & 255;
            }

            // 2. Executa cada função, na ordem listada, cada uma consumindo
            //    seu próprio payload a partir de onde a anterior parou.
            for (int i = 0; i < funcCount; i++) {
                dataPtr = processFunction(global, obj, tm, funcCodes[i], dac, dataPtr);
            }

        } catch (Throwable e) {
            Logue.StackTrace(e);
        }
        return dataPtr;
    }

    // Executa UMA função (já sabendo seu código) e devolve o dataPtr
    // avançado além do payload dela. Isolado num método próprio para ser
    // reaproveitado tanto pelo fluxo normal quanto por skipAllFunctions.
    private int processFunction(GlobalWork global, ObjReq obj, TCBManajer tm,
                                  int funcCode, byte[] dac, int dataPtr) {
        switch (funcCode) {
            case FUNC_PLAY_VIDEO:
                if (this.player == null) {
                    this.player = new VideoTexture();
                }
                // playFromDac lê seus próprios parâmetros diretamente do
                // binário e devolve o dataPtr avançado.
                dataPtr = this.player.playFromDac(global, obj, tm, dac, dataPtr);
                break;

            case FUNC_ANIM_TEX:
                // Payload: [2 bytes LE] índice na fila animTex[] deste
                // acttype (ver GameData.getAnimTex). Só o índice — não
                // existe conceito de "modo" para esta função: toda
                // AnimatedTexture na fila é sempre pré-decodificada no
                // load do .pac (ver AnimatedTexture.java para o porquê).
                // Um objeto só aponta para "qual textura animada", igual
                // apontaria para "qual imageId".
                _animTexIndex = (dac[dataPtr + 1] & 255) << 8 | (dac[dataPtr] & 255);
                dataPtr += 2;
                break;

            case FUNC_REAL_OPT:
                // Mesma leitura que o ramo padrão (não-225) fazia:
                // uma OPT/flag de gameplay real, "carona" no mesmo
                // gatilho junto de outras funções customizadas.
                obj.wOptFlag = (short) ((dac[dataPtr + 1] & 255) << 8 | (dac[dataPtr] & 255));
                dataPtr += 2;
                for (int k = 0; k < 4; k++) {
                    obj.OptData[k] = dac[dataPtr++] & 255;
                }
                break;

            default:
                // Código desconhecido: nada a consumir com segurança além
                // do próprio byte de código (já lido pelo chamador). Isso
                // significa que payloads de códigos não reconhecidos DEVEM
                // ter tamanho zero, ou o parser desalinha a partir daqui —
                // o Compiler (ExtrasBuilder) nunca deve emitir um código
                // desconhecido por esta versão do ExtrasReader.
                break;
        }
        return dataPtr;
    }

    // Usado só quando leave==true: ainda precisamos saber quantos bytes
    // pular, mas idealmente sem disparar side effects desnecessários.
    //
    // LIMITAÇÃO CONHECIDA: FUNC_PLAY_VIDEO ainda dispara o side effect real
    // (inicia o vídeo) mesmo aqui — por segurança e simplicidade este método
    // reaproveita processFunction() por completo. O payload de vídeo NÃO tem
    // tamanho fixo (8 bytes + 3 se FLAG_HAS_STOP_POINT + 1 se
    // FLAG_HAS_NEW_BGM); para só atravessar o gatilho sem side effects, use
    // ExtrasReader.skip(dac, dataPtr).
    private int skipAllFunctions(GlobalWork global, ObjReq obj, TCBManajer tm,
                                   int funcCount, byte[] dac, int dataPtr) {
        int[] funcCodes = new int[funcCount];
        for (int i = 0; i < funcCount; i++) {
            funcCodes[i] = dac[dataPtr++] & 255;
        }
        for (int i = 0; i < funcCount; i++) {
            dataPtr = processFunction(global, obj, tm, funcCodes[i], dac, dataPtr);
        }
        return dataPtr;
    }

    public void stopFunctions() {
        if (this.player != null) {
            this.player.stop(); // sem retomada de minutagem — reinicia do zero se tocar de novo
            this.player = null; // mata a instância antiga para evitar memory leak
        }
        _animTexIndex = -1; // NOVO — nenhum estado a "parar" de fato (a textura em si
                             // continua viva/tocando em GameData; isto só desassocia
                             // ESTE objeto dela), mas zera a referência.
    }

    // Chamado por TCBManajer._SetAct quando o obj dono desta ExtrasReader
    // está prestes a trocar de $ANO (ou de acttype) — ou seja, sempre que
    // var11.ano != novoAno || var11.acttype != novoActtype em _SetAct,
    // antes do novo ACT ser processado.
    //
    // 1. Delega para VideoTexture decidir se o vídeo atual pediu
    //    $END_ANO=yes (nesse caso, encerra agora).
    // 2. NOVO: reseta _animTexIndex incondicionalmente. Cada $ANO é um
    //    estado de animação independente — $GIF disparado em um $ANO NÃO
    //    deve permanecer ativo nos $ANOs seguintes do mesmo objeto (bug
    //    observado: um $GIF disparado uma vez "grudava" e substituía o
    //    desenho de todo $ANO posterior daquele obj, mesmo $ANOs sem
    //    nenhum $GIF, porque _animTexIndex só era limpo em
    //    stopFunctions(), chamado apenas quando o objeto inteiro morre —
    //    não a cada troca de $ANO). Se o novo $ANO também tiver $GIF,
    //    _ActReqMain/processFunction reataca _animTexIndex normalmente
    //    ao processar o novo ACT, logo depois desta chamada.
    public void onAnoChanging(GlobalWork global) {
        if (this.player != null) {
            this.player.onAnoChanging(global);
        }
        _animTexIndex = -1;
    }

    // Exposto pro laço de desenho do TCBManajer saber se este objeto tem um
    // vídeo ativo agora, e pra ele mesmo (a instância de VideoTexture) —
    // é ele quem guarda as flags/posição/tamanho computados em playFromDac.
    //
    // Critério: existe um PEDIDO de vídeo em andamento (hasActiveRequest),
    // não "o ExoPlayer está tocando agora" — isso é intencional (ver
    // VideoTexture, princípio de design 1): decidir isso com base em
    // estado oscilante do player foi a causa raiz dos bugs de reinício
    // repetido na versão anterior.
    public VideoTexture getActiveVideo() {
        return (this.player != null && this.player.hasActiveRequest()) ? this.player : null;
    }

    // Exposto para TCBManajer.DrawExec decidir, no único call-site de
    // DrawImage, se deve desviar para DrawImageAnimTex. -1 = nenhuma
    // textura animada ativa para este objeto — comportamento normal
    // (cnv) se aplica. Ver GameData.getAnimTex(int) para a fila
    // consultada com este índice.
    public int getActiveAnimTexIndex() {
        return _animTexIndex;
    }

}
