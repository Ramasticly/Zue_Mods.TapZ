package com.zue.gd.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════════════════════
// ExtrasParser — lado do Compiler, LEITURA do formato fonte relacionado a
// funções extras (o que no binário vira o gatilho "OPT 225").
//
// Ponto de entrada único chamado por DacParser.parse():
//   ExtrasParser.isExtrasLine(t)          — pergunta se esta linha é de uma
//                                            função extra conhecida
//   ExtrasParser.handleLine(t, currentMpt) — se for, processa e guarda
//
// DacParser NÃO conhece nenhum campo específico de função extra ($VID,
// futuros $SHD, etc.) — tudo isso vive só aqui. MptInstance também não
// ganha nenhum campo novo por causa deste sistema: todo o estado fica
// neste registro interno, indexado pelo próprio MptInstance como chave.
//
// Isso é o que permite, se um dia as funções extras forem descontinuadas,
// bastar comentar as duas chamadas em DacParser (isExtrasLine/handleLine)
// — nenhuma outra classe do Compiler precisa mudar.
//
// SINTAXE DO FORMATO FONTE, por função:
//
//   $VID:{
//          $FIXO: yes/not, $FULLSCREEN: yes/not, $W: 0, $H: 0,
//          $SKIP: 0, $STOP:{ yes/not, $ANO: X/not, $MPT: N }, $END_ANO: yes/not,
//          $MUTE_VIDEO: yes/not, $MUTE_BGM: yes/not, $MUTE_SE: yes/not, $NEW_BGM: <id>/not,
//        }
//     — FUNC_PLAY_VIDEO. Sub-campos nomeados, em qualquer ordem/linha
//       (o parser procura cada tag independentemente, e aceita o bloco
//       inteiro em múltiplas linhas — ver _accumulating mais abaixo).
//       Todos opcionais individualmente; ausentes viram "not"/0/desligado.
//       NÃO EXISTE MAIS $FLAGS — todos os bits do binário são derivados
//       de campos nomeados (ver histórico de decisão: bits manuais
//       1/2/4 foram analisados e removidos por serem redundantes com
//       comportamento já natural do motor, ou por preferir sincronização
//       manual no binário).
//       $STOP unifica o antigo $STOP+$WAIT: "yes" sozinho (sem $ANO/$MPT,
//       ou com $ANO: not) trava o jogo DESDE O INÍCIO do vídeo. "yes" COM
//       $ANO preenchido trava CONDICIONALMENTE, só quando o objeto
//       alcançar exatamente ($ANO, $MPT) — e nunca ativa se o vídeo for
//       loop (checado em runtime, VideoRequest.stopPointIsActive()).
//       $MPT tem padrão 0 se omitido dentro de um $STOP com $ANO presente.
//       $W/$H == 0 = usa a resolução real do vídeo (resolvida em
//       runtime, via onVideoSizeChanged).
//
// (Novas funções extras: adicionar aqui um novo bloco de reconhecimento
//  + handler, seguindo o mesmo padrão de handleVidLine abaixo.)
//
// ATENÇÃO: $OPT continua sendo tratado por DacParser.handleOptLine, SEM
// nenhuma mudança — ExtrasParser não lê nem entende $OPT. A decisão de
// "esta OPT precisa virar a função 250 dentro do gatilho de extras
// porque coexiste com uma função extra no mesmo MPT" é do ExtrasBuilder,
// no momento de gerar os bytes — não deste parser.
// ══════════════════════════════════════════════════════════════════════
public final class ExtrasParser {

    private ExtrasParser() {
    } // classe utilitária, só métodos estáticos

    // ── Representação de UMA função extra pendente para um MPT ──────────
    // Cada função tem seu próprio código (igual ao runtime, ExtrasReader)
    // e seus próprios campos — sem payload genérico tipo o antigo
    // int[16] zueInt. Adicionar uma função nova = adicionar uma nova
    // subclasse/variante aqui, sem alterar as existentes.
    public static abstract class ExtraFunction {
        public abstract int code();
    }

    public static final class VidFunction extends ExtraFunction {
        public int skip;
        public int width;
        public int height;

        // ── Campos das funcionalidades planejadas ────────────────────────
        // Todos representados aqui como valores já resolvidos — a tradução
        // para bits dentro de "flags" (o que de fato vai pro binário)
        // acontece em ExtrasBuilder, não aqui. Isto mantém ExtrasParser
        // focado só em "ler o texto", sem se preocupar com layout binário.
        public boolean muteBgm;
        public boolean muteSe;
        public boolean muteVideo;
        public boolean endAno;
        public boolean fullscreen;
        public boolean fixo; // NOVO — nome final do antigo "trava posição"

        public boolean hasNewBgm;
        public int newBgmId;

        // $STOP unificado (era $STOP + $WAIT separados). stopEnabled=false
        // quando $STOP está ausente ou "not" — nesse caso hasStopPoint/
        // stopAno/stopMpt são irrelevantes.
        public boolean stopEnabled;
        public boolean hasStopPoint;
        public int stopAno;
        public int stopMpt;

        @Override
        public int code() {
            return 60; // FUNC_PLAY_VIDEO — deve bater com ExtrasReader.FUNC_PLAY_VIDEO
        }
    }

    // ── $GIF:{ $ID: N } — FUNC_ANIM_TEX ──────────────────────────────────
    // Referencia diretamente um índice na fila animTex[] de GameData
    // (ver GameData.getAnimTex/decodeData, marcador "webp") — SEM passar
    // pelo cnv que os PNGs manuais usam. Por isso o payload é mínimo: só
    // o índice. Não existe "modo" como campo desta função — toda
    // AnimatedTexture é sempre pré-decodificada no load do .pac (ver
    // AnimatedTexture.java para o porquê disso). $GIF só diz "qual".
    public static final class GifFunction extends ExtraFunction {
        public int gifIndex;

        @Override
        public int code() {
            return 61; // FUNC_ANIM_TEX — deve bater com ExtrasReader.FUNC_ANIM_TEX
        }
    }

    // ── Registro: qual MptInstance tem quais funções extras pendentes ────
    // Chave = a própria instância do MPT (identidade, não precisa de
    // equals/hashCode customizado). Populado aqui, consultado por
    // ExtrasBuilder na hora de gerar os bytes.
    private static final Map<MptInstance, List<ExtraFunction>> _pending =
            new HashMap<MptInstance, List<ExtraFunction>>();

    // Chamado por ExtrasBuilder para saber se um MPT tem função(ões) extra(s)
    // pendente(s) de escrita. Retorna lista vazia (nunca null) se não houver.
    public static List<ExtraFunction> getFunctionsFor(MptInstance mpt) {
        List<ExtraFunction> list = _pending.get(mpt);
        return (list != null) ? list : java.util.Collections.<ExtraFunction>emptyList();
    }

    // Limpa o registro inteiro. Chamar entre compilações de .pac diferentes
    // para não vazar estado de um arquivo para outro (o mapa é estático).
    public static void reset() {
        _pending.clear();
        _accumulating = null;
        _accumulatingMpt = null;
        _braceDepth = 0;
    }

    // ── Suporte a $VID (e futuras funções) em MÚLTIPLAS linhas ───────────
    // Como o $VID cresceu bastante (vários sub-campos + $WAIT aninhado),
    // é natural quebrar em várias linhas no arquivo fonte por legibilidade
    // — mas DacParser.parse() processa uma linha de cada vez. Por isso,
    // ExtrasParser acumula o texto de todas as linhas do bloco atual até
    // as chaves balancearem (mesma contagem de '{' e '}' desde a abertura
    // do $VID), e só então processa o bloco completo de uma vez.
    private static StringBuilder _accumulating = null;
    private static int _braceDepth = 0;
    private static MptInstance _accumulatingMpt = null;

    // ── Ponto de entrada chamado por DacParser ───────────────────────────

    // Reconhece linhas de qualquer função extra suportada, OU uma linha de
    // continuação de um bloco já em acumulação. Ao adicionar uma nova
    // função extra no futuro, adicionar sua tag de abertura aqui com "||"
    // (ex.: || t.contains("$SHD:{")).
    public static boolean isExtrasLine(String t) {
        if (_accumulating != null) {
            return true; // já estamos no meio de um bloco — toda linha até fechar é nossa
        }
        return t.contains("$VID:{") || t.contains("$GIF:{");
    }

    public static void handleLine(String t, MptInstance currentMpt) {
        if (currentMpt == null) return;

        if (_accumulating == null) {
            // Início de um novo bloco $VID — começa a acumular.
            _accumulating = new StringBuilder();
            _accumulatingMpt = currentMpt;
            _braceDepth = 0;
        }

        _accumulating.append(t).append(' ');
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '{') _braceDepth++;
            else if (c == '}') _braceDepth--;
        }

        if (_braceDepth <= 0) {
            // Bloco fechado — processa o texto completo acumulado de uma vez.
            String fullBlock = _accumulating.toString();
            _accumulating = null;
            MptInstance targetMpt = _accumulatingMpt;
            _accumulatingMpt = null;

            if (fullBlock.contains("$VID:{")) {
                handleVidLine(fullBlock, targetMpt);
            } else if (fullBlock.contains("$GIF:{")) {
                handleGifLine(fullBlock, targetMpt);
            }
            // (novas funções extras multi-linha: adicionar "else if" aqui)
        }
    }

    // ── Handlers específicos por função ──────────────────────────────────

    private static void handleVidLine(String t, MptInstance currentMpt) {
        VidFunction f = new VidFunction();
        f.skip = extractIntAfter(t, "$SKIP:");
        f.width = extractIntAfter(t, "$W:");
        f.height = extractIntAfter(t, "$H:");

        f.muteBgm = extractYesNotAfter(t, "$MUTE_BGM:");
        f.muteSe = extractYesNotAfter(t, "$MUTE_SE:");
        f.muteVideo = extractYesNotAfter(t, "$MUTE_VIDEO:");
        f.endAno = extractYesNotAfter(t, "$END_ANO:");
        f.fullscreen = extractYesNotAfter(t, "$FULLSCREEN:");
        f.fixo = extractYesNotAfter(t, "$FIXO:");

        // $NEW_BGM: <id> ou $NEW_BGM: not — distingue os dois casos pelo
        // valor textual, já que "not" não é um inteiro válido.
        String newBgmRaw = extractRawValueAfter(t, "$NEW_BGM:");
        if (newBgmRaw != null && !isNotValue(newBgmRaw)) {
            f.hasNewBgm = true;
            try {
                f.newBgmId = Integer.parseInt(newBgmRaw.trim());
            } catch (Exception ignored) {
                f.hasNewBgm = false;
            }
        }

        // $STOP:{ yes/not, $ANO: X/not, $MPT: N } — bloco aninhado,
        // unifica o antigo par $STOP (bit sozinho) + $WAIT (bloco
        // separado). O bloco inteiro só existe se a tag "$STOP:{"
        // aparecer; dentro dele, o primeiro valor (antes de qualquer
        // vírgula) é o yes/not que liga/desliga o mecanismo por completo.
        int stopPos = t.indexOf("$STOP:{");
        if (stopPos >= 0) {
            int stopEnd = t.indexOf('}', stopPos);
            if (stopEnd > stopPos) {
                String stopBlock = t.substring(stopPos, stopEnd + 1);

                // O yes/not "solto" logo após "$STOP:{" (antes de $ANO/$MPT).
                f.stopEnabled = extractLeadingYesNot(stopBlock, "$STOP:{");

                if (f.stopEnabled) {
                    String anoRaw = extractRawValueAfter(stopBlock, "$ANO:");
                    if (anoRaw != null && !isNotValue(anoRaw)) {
                        f.hasStopPoint = true;
                        f.stopAno = extractIntAfter(stopBlock, "$ANO:");
                        // $MPT tem padrão 0 se omitido dentro de um $STOP
                        // com $ANO presente.
                        f.stopMpt = extractIntAfter(stopBlock, "$MPT:");
                    }
                }
            }
        }

        List<ExtraFunction> list = _pending.get(currentMpt);
        if (list == null) {
            list = new ArrayList<ExtraFunction>();
            _pending.put(currentMpt, list);
        }
        list.add(f);
        currentMpt.used = true;
    }

    // $GIF:{ $ID: N } — payload mínimo, um único campo. Segue o mesmo
    // formato de bloco nomeado que $VID usa (em vez de um "$GIF: N"
    // posicional cru) por consistência e para deixar espaço a campos
    // futuros (ex.: um dia decidir sobrepor $W/$H) sem quebrar a sintaxe.
    private static void handleGifLine(String t, MptInstance currentMpt) {
        GifFunction f = new GifFunction();
        f.gifIndex = extractIntAfter(t, "$ID:");

        List<ExtraFunction> list = _pending.get(currentMpt);
        if (list == null) {
            list = new ArrayList<ExtraFunction>();
            _pending.put(currentMpt, list);
        }
        list.add(f);
        currentMpt.used = true;
    }

    // Lê o valor "yes"/"not" que aparece logo após a abertura de um bloco
    // (ex.: "$STOP:{ yes, ..."), antes de qualquer outra tag nomeada.
    // Diferente de extractYesNotAfter (que procura uma TAG:valor), aqui o
    // valor vem "solto", sem nome, como primeiro item do bloco.
    private static boolean extractLeadingYesNot(String block, String openTag) {
        int p = block.indexOf(openTag);
        if (p < 0) return false;
        p += openTag.length();
        int n = block.length();

        while (p < n && Character.isWhitespace(block.charAt(p))) p++;

        int start = p;
        while (p < n) {
            char c = block.charAt(p);
            if (c == ',' || c == '}') break;
            p++;
        }
        String raw = block.substring(start, p).trim();
        return raw.equalsIgnoreCase("yes");
    }

    // ── Helper: lê um valor "yes"/"not" (case-insensitive) após uma tag.
    //    Ausência da tag, ou qualquer valor que não seja "yes", é tratado
    //    como "not" (false) — mesma convenção tolerante que o resto do
    //    formato já usa (ex.: extractYesNot de DacParser para SPAWN_POS). ──
    private static boolean extractYesNotAfter(String s, String tag) {
        String raw = extractRawValueAfter(s, tag);
        return raw != null && raw.trim().equalsIgnoreCase("yes");
    }

    // Extrai o valor textual bruto logo após uma tag, até a próxima
    // vírgula, chave, ou fim da string — sem assumir que é um número.
    // Retorna null se a tag não existir na linha.
    private static String extractRawValueAfter(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return null;
        p += tag.length();
        int n = s.length();

        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        int start = p;
        while (p < n) {
            char c = s.charAt(p);
            if (c == ',' || c == '}' || c == '{') break;
            p++;
        }
        return s.substring(start, p);
    }

    private static boolean isNotValue(String raw) {
        String trimmed = raw.trim();
        return trimmed.equalsIgnoreCase("not") || trimmed.length() == 0;
    }

    // ── Helper de extração de inteiro após uma tag, na mesma linha ───────
    // Mesmo comportamento de DacParser.extractAfter/extractInt (duplicado
    // aqui de propósito, para ExtrasParser não depender de métodos
    // privados de DacParser — mantém as duas classes desacopladas).
    private static int extractIntAfter(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return 0;
        p += tag.length();
        int n = s.length();

        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        StringBuilder sb = new StringBuilder();
        if (p < n && (s.charAt(p) == '-' || s.charAt(p) == '+')) {
            sb.append(s.charAt(p));
            p++;
        }
        while (p < n) {
            char c = s.charAt(p);
            if (c >= '0' && c <= '9') {
                sb.append(c);
                p++;
            } else {
                break;
            }
        }

        if (sb.length() == 0) return 0;
        try {
            return Integer.parseInt(sb.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
