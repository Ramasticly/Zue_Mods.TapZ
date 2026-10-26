package com.namcobandaigames.dragonballtap.apk;

// ══════════════════════════════════════════════════════════════════════
// VideoRequest — dados PUROS decodificados do payload do $VID (função
// FUNC_PLAY_VIDEO dentro do gatilho de Extras). Não tem lógica nenhuma,
// só guarda "o que foi pedido" — quem decide o que FAZER com esses dados
// é VideoTexture (a classe orquestradora).
//
// Uma nova instância é criada a cada vez que playFromDac() é chamado, a
// partir da leitura do binário. Isso separa claramente "o pedido atual"
// de "o estado do player em andamento" (que vive em VideoPlayerGL) — e
// permite comparar dois pedidos (é o mesmo vídeo de novo, ou é outro?)
// sem espalhar essa lógica pela classe inteira.
//
// FORMATO BINÁRIO lido (a partir do dataPtr recebido, já após o código
// de função FUNC_PLAY_VIDEO ter sido consumido pelo chamador):
//   [2 bytes] canSkip
//   [2 bytes] flags (bitmask — ver tabela abaixo. TODO bit é DERIVADO;
//             não existe mais nenhum bit manual/campo $FLAGS no formato
//             fonte — o autor nunca escreve este valor diretamente)
//   [2 bytes] width   (0 = usar resolução real do vídeo)
//   [2 bytes] height  (0 = usar resolução real do vídeo)
//   [2 bytes] stopAno   — SÓ presente se bit FLAG_HAS_STOP_POINT
//   [1 byte]  stopMpt   — SÓ presente se bit FLAG_HAS_STOP_POINT
//   [1 byte]  newBgmId  — SÓ presente se bit FLAG_HAS_NEW_BGM
//
// TABELA DE BITS de `flags` — TODOS derivados de campos nomeados do
// formato fonte, nenhum setável numericamente:
//   1    = $FIXO: yes — trava a posição no lugar onde o vídeo começou
//          (mantém $W/$H; desliga TODO comportamento herdado do pai —
//          flip/zoom/alpha — sem cobrir a tela toda como fullscreen faz)
//   8    = $FULLSCREEN: yes — ignora $MID/zoom/flip, cobre a tela toda
//   16   = $STOP: yes (dentro de $STOP:{...}) — trava o jogo
//          (bActionStop). Sozinho (sem $ANO dentro de $STOP) = trava
//          desde o início do vídeo até o fim. Com $ANO presente = trava
//          condicional (ver bit 32).
//   32   = $STOP tem $ANO/$MPT preenchidos (não "not") — a trava do bit
//          16 passa a ser CONDICIONAL: só ativa quando o objeto alcançar
//          exatamente (stopAno, stopMpt), e nunca ativa se o vídeo for
//          loop (ver waitIsActive()).
//   64   = $MUTE_BGM: yes
//   128  = $MUTE_SE: yes
//   256  = $MUTE_VIDEO: yes
//   512  = tem $NEW_BGM anexado (payload: newBgmId)
//   1024 = $END_ANO: yes — para o vídeo automaticamente ao trocar de $ANO
//          do PRÓPRIO objeto (ver TCBManajer._SetAct / onAnoChanging)
//
// BITS REMOVIDOS (análise concluída — ver histórico de decisão):
//   "segue x/y do pai": nunca foi um bit — é o comportamento padrão
//     sempre ativo; $FIXO é quem desliga isso.
//   "segue zoom do pai": removido — zoom é tipicamente sincronizado
//     manualmente no binário quando necessário, não precisa de bit.
//   "atrelado à vida do pai": removido — filhos (via $LINK) já morrem
//     automaticamente quando o pai troca de $ANO (_SetAct já mata os
//     links antes de trocar); e quando o pai só chega ao fim de
//     animação SEM trocar de $ANO, ele não morre, então o vídeo não
//     precisaria morrer também. $END_ANO já cobre o caso de "o próprio
//     objeto trocou de $ANO".
//
// SINTAXE DO FORMATO FONTE ($STOP unificado com o antigo $WAIT):
//   $STOP:{ yes, $ANO: not, $MPT: 0 }             — trava desde o início
//   $STOP:{ yes, $ANO: 16, $MPT: 15 }             — trava condicional
//   $STOP:{ not }  (ou $STOP ausente)             — nunca trava
// ══════════════════════════════════════════════════════════════════════
final class VideoRequest {

    // ── Bits de $FLAGS — TODOS derivados, nenhum setável manualmente.
    //    ExtrasBuilder é o único lugar que os calcula. ───────────────────
    static final int FLAG_FIXO            = 1;
    static final int FLAG_FULLSCREEN      = 8;
    static final int FLAG_STOP            = 16;
    static final int FLAG_HAS_STOP_POINT  = 32;
    static final int FLAG_MUTE_BGM        = 64;
    static final int FLAG_MUTE_SE         = 128;
    static final int FLAG_MUTE_VIDEO      = 256;
    static final int FLAG_HAS_NEW_BGM     = 512;
    static final int FLAG_END_ANO         = 1024;

    // ── Identidade do pedido (para comparar "é o mesmo vídeo?") ──────────
    final int acttype;
    final int videoIndex;

    // ── Campos decodificados diretamente do payload ──────────────────────
    final int canSkipSeconds;
    final int flags;
    final int width;  // 0 = usar resolução real do vídeo
    final int height; // 0 = usar resolução real do vídeo

    // $STOP unificado — stopAno/stopMpt só relevantes se hasStopPoint.
    // Se stop==true e hasStopPoint==false: trava desde o início (bit 16
    // sozinho). Se stop==true e hasStopPoint==true: trava condicional.
    final boolean stop;
    final boolean hasStopPoint;
    final int stopAno;
    final int stopMpt;

    final boolean hasNewBgm;
    final int newBgmId;

    // ── Campos que vêm de fora do payload do $VID, mas fazem parte do
    //    "pedido" completo (já lidos pelo motor antes do gatilho disparar) ──
    final int rawSeconds;     // $TIME em segundos (0 = vídeo inteiro sem corte)
    final boolean loopRequested; // $WACTFLAG bit 1

    // ── Posição/tamanho no momento do pedido (para FLAG_FIXO) ────────────
    final int requestX;
    final int requestY;

    VideoRequest(int acttype, int videoIndex, int canSkipSeconds, int flags, int width, int height,
                 boolean stop, boolean hasStopPoint, int stopAno, int stopMpt,
                 boolean hasNewBgm, int newBgmId,
                 int rawSeconds, boolean loopRequested,
                 int requestX, int requestY) {
        this.acttype = acttype;
        this.videoIndex = videoIndex;
        this.canSkipSeconds = canSkipSeconds;
        this.flags = flags;
        this.width = width;
        this.height = height;
        this.stop = stop;
        this.hasStopPoint = hasStopPoint;
        this.stopAno = stopAno;
        this.stopMpt = stopMpt;
        this.hasNewBgm = hasNewBgm;
        this.newBgmId = newBgmId;
        this.rawSeconds = rawSeconds;
        this.loopRequested = loopRequested;
        this.requestX = requestX;
        this.requestY = requestY;
    }

    boolean hasFlag(int bit) {
        return (flags & bit) != 0;
    }

    // Mesmo vídeo que outro pedido? (mesma identidade acttype+videoIndex)
    // Usado pela idempotência em VideoTexture — NÃO depende de nenhum
    // estado do ExoPlayer (isPlaying, isBusy, etc.), só dos dados do
    // próprio pedido, evitando a corrida que já vimos causar reinícios
    // repetidos.
    boolean isSameVideoAs(VideoRequest other) {
        return other != null && this.acttype == other.acttype && this.videoIndex == other.videoIndex;
    }

    // true quando o $STOP condicional (com $ANO/$MPT) deve ser observado.
    // Loop desativa isso (nunca faz sentido esperar um vídeo que não tem
    // fim natural) — aplicado aqui, na leitura, para que o resto do
    // sistema nem precise checar "é loop?" de novo.
    boolean stopPointIsActive() {
        return stop && hasStopPoint && !loopRequested;
    }

    // true quando o $STOP deve travar o jogo DESDE O INÍCIO do vídeo
    // (yes, sem $ANO/$MPT preenchidos).
    boolean stopsImmediately() {
        return stop && !hasStopPoint;
    }
}
