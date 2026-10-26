package com.zue.gd.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser do dump textual gerado pelo DacDumper. Estilo "state machine", igual ao parser do seu
 * amigo:
 *
 * <p>- Apenas UM loop lendo linha por linha. - Mantém contexto: currentAno / currentMpt / inOpt /
 * inRaz. - NÃO depende de "}" ou "}," para fechar blocos. Um bloco acaba quando outro começa
 * ($ANO_, $MPT_, etc.).
 *
 * <p>Só preenche AnoInstance / MptInstance; quem monta o binário é o writer.
 */
public final class DacParser {

    private final BufferedReader br;
    private final DacRebuilder rebuilder;
    private final ErrorReporter reporter; // opcional — pode ser null
    private final String fileName;         // opcional — para contexto nas mensagens de erro
    private final String pacFolder;        // opcional — idem
    private String line;
    private int lineNumber; // 1-based, para localizar erros

    // estado atual
    private AnoInstance currentAno;
    private MptInstance currentMpt;

    // estados de subblocos (apenas para OPT / RAZ multi-linha)
    private boolean inOpt;
    private boolean inRaz;

    public DacParser(InputStream is, int anoCount) {
        this(is, anoCount, null, null, null);
    }

    /**
     * @param reporter  opcional (pode ser null) — coleta erros de sintaxe/valor sem
     *                  nunca abortar o parse; quando null, custo extra é zero.
     * @param fileName  opcional — nome do arquivo .dac.txt, só para contexto nas mensagens.
     * @param pacFolder opcional — nome da pasta .pac sendo compilada, idem.
     */
    public DacParser(InputStream is, int anoCount, ErrorReporter reporter, String fileName, String pacFolder) {
        this.br = new BufferedReader(new InputStreamReader(is));
        this.rebuilder = new DacRebuilder(anoCount);
        this.reporter = reporter;
        this.fileName = fileName;
        this.pacFolder = pacFolder;
    }

    /** Tags reconhecidas em nível de MPT — usado para sugestão de digitação (TagSuggester). */
    private static final String[] KNOWN_MPT_TAGS = {
        "$TIME:", "$IMG:", "$MID:", "$WACTFLAG:", "$SE:",
        "$MOVE:", "$XSPEED:", "$YSPEED:", "$XACCEL:", "$YACCEL:",
        "$HT_", "$OPT:", "$DATA:", "$RAZ:", "$FLAG:", "$ROT:", "$ALPHA:", "$XZOOM:", "$YZOOM:"
    };

    /** Tags reconhecidas em nível de ANO (fora de qualquer MPT). */
    private static final String[] KNOWN_ANO_TAGS = {
        "$PRIO:", "$LNKS:", "$LINK_", "$MPTS:", "$MPT_"
    };

    /** Retorna a primeira tag "$ALGO:" ou "$ALGO_" encontrada no início da linha, ou null.
     *  Aceita case misto (ex: "$AmO_") pelo mesmo motivo de looksLikeRootBlockHeader. */
    private static String extractLeadingTag(String t) {
        Matcher m = Pattern.compile("^(\\$[A-Za-z_]+[:_])").matcher(t);
        return m.find() ? m.group(1) : null;
    }

    /** true se a linha tem a forma "$TAG_N:{" — um cabeçalho de bloco raiz numerado, como $ANO_/$AMO_/etc.
     *  Aceita qualquer combinação de maiúsculas/minúsculas na tag (ex: "$AmO_1:{"), porque um typo de
     *  digitação frequentemente muda o case de uma letra — restringir a [A-Z] deixaria esses casos passar. */
    private static boolean looksLikeRootBlockHeader(String t) {
        return Pattern.compile("^\\$[A-Za-z]+_\\d+\\s*:\\s*\\{").matcher(t).find();
    }

    /** Retorna the DacRebuilder com todas as instâncias prontas para o writer */
    public DacRebuilder parse() throws IOException {
        currentAno = null;
        currentMpt = null;
        inOpt = false;
        inRaz = false;
        lineNumber = 0;
        // O registro do ExtrasParser é estático e ninguém chamava reset():
        // crescia a cada .dac compilado (segurando todos os MptInstance) e um
        // $VID sem "}" final deixava _accumulating ligado, engolindo o resto
        // deste arquivo E os próximos .dac. Começa sempre limpo.
        ExtrasParser.reset();

        while (readLine()) {
            String t = line.trim();
            if (t.length() == 0) continue;

            // NOVO: checa isExtrasLine ANTES do filtro de linhas-só-chave
            // abaixo — enquanto um bloco $VID (ou outra função extra)
            // está em acumulação, ExtrasParser precisa VER TODAS as
            // linhas, inclusive as que são só "}" ou "{" (ex.: a linha de
            // fechamento de um $VID multi-linha como:
            //   $VID:{
            //          ...campos...
            //        },
            // ), para contar corretamente as chaves e saber quando o
            // bloco terminou. Sem isto, a linha de fechamento "}," seria
            // descartada pelo filtro abaixo antes de chegar em
            // ExtrasParser, e o bloco nunca fecharia.
            if (ExtrasParser.isExtrasLine(t)) {
                ExtrasParser.handleLine(t, currentMpt);
                continue;
            }

            // ignora linhas só com chaves/virgulas
            if (t.equals("}") || t.equals("},") || t.equals("{") || t.equals("{,")) {
                continue;
            }

            /* ===================== ANO ===================== */

            if (t.startsWith("$ANO_")) {
                handleAnoHeader(t);
                // novo ANO encerra qualquer contexto anterior
                currentMpt = null;
                inOpt = false;
                inRaz = false;
                continue;
            }

            // Cabeçalho de bloco raiz numerado ("$TAG_N:{") que não é $ANO_ nem
            // $MPT_ — quase sempre um typo de um dos dois. Roda em QUALQUER ponto
            // do arquivo (não só antes do primeiro $ANO_ válido), porque um typo
            // como "$AmO_1:{" pode aparecer entre dois ANOs já processados, e um
            // typo como "$MoT_2:{" pode aparecer dentro de um $MPTS: existente —
            // em ambos os casos, sem este aviso, o bloco inteiro desapareceria
            // silenciosamente do binário. A tag esperada muda pelo contexto atual:
            // sem ANO aberto, é provável que fosse $ANO_; com ANO aberto, $MPT_.
            // $LINK_ e $HT_ ficam de fora: são cabeçalhos numerados legitimamente
            // válidos, tratados por handleLink/handleHit mais abaixo.
            if (looksLikeRootBlockHeader(t) && !t.startsWith("$MPT_") && !t.startsWith("$LINK_") && !t.startsWith("$HT_")) {
                String tag = extractLeadingTag(t);
                if (tag != null) {
                    if (currentAno == null) {
                        reportUnknownTag(tag, new String[]{"$ANO_"},
                                "Linha parece um cabeçalho de bloco no nível raiz, mas não é $ANO_ — "
                                        + "se a intenção era iniciar um novo ANO, este bloco inteiro será perdido");
                    } else {
                        reportUnknownTag(tag, new String[]{"$ANO_", "$MPT_"},
                                "Linha parece um cabeçalho de bloco numerado, mas não é $ANO_ nem $MPT_ — "
                                        + "se a intenção era um novo $ANO_ ou $MPT_N, este bloco inteiro será perdido");
                    }
                }
                continue;
            }

            if (currentAno == null) {
                // tudo antes do primeiro $ANO_ é irrelevante (cabeçalho, comentários) —
                // o caso de "parece um cabeçalho mas não é $ANO_" já foi tratado acima.
                continue;
            }

            if (t.contains("$PRIO:")) {
                handlePrio(t);
                continue;
            }

            if (t.startsWith("$LNKS:{")) {
                // apenas marcador; os dados reais vêm em $LINK_
                continue;
            }

            if (t.startsWith("$LINK_")) {
                handleLink(t);
                continue;
            }

            /* ===================== MPT ===================== */

            if (t.startsWith("$MPTS:{")) {
                // só organiza; o que importa são as linhas $MPT_ — mas marca
                // o ANO como existente: é assim que o DacExtractor representa
                // um ANO presente sem PRIO/LINK/MPT ("$MPTS:{ },").
                currentAno.used = true;
                continue;
            }

            if (t.startsWith("$MPT_")) {
                handleMptHeader(t);
                // novo MPT encerra OPT/RAZ anteriores
                inOpt = false;
                inRaz = false;
                continue;
            }

            if (currentMpt == null) {
                // estamos dentro de um ANO, mas ainda não começou nenhum MPT.
                // Se a linha parece uma tag válida só que fora de contexto
                // (ex: um $TIME: antes de qualquer $MPT_), ou uma tag
                // desconhecida, reporta — ajuda a achar $ANO_/$MPT_ faltando.
                if (t.startsWith("$")) {
                    String tag = extractLeadingTag(t);
                    if (tag != null && !matchesAny(tag, KNOWN_ANO_TAGS)) {
                        reportUnknownTag(tag, KNOWN_MPT_TAGS,
                                "Linha com tag \"" + tag + "\" encontrada dentro de um $ANO_ mas fora de qualquer $MPT_ — "
                                        + "verifique se falta um cabeçalho $MPT_N antes desta linha");
                    }
                }
                continue;
            }

            /* ===================== CAMPOS DO MPT ===================== */

            // TIME
            if (t.contains("$TIME:")) {
                Integer v = extractAfterChecked(t, "$TIME:", 0, 255);
                if (v != null) currentMpt.setTime(v);
            }

            // IMG
            if (t.contains("$IMG:")) {
                Integer v = extractAfterChecked(t, "$IMG:", -32768, 65535);
                if (v != null) currentMpt.setImg(v);
            }

            // MID
            if (t.contains("$MID:[")) {
                handleMid(t);
            }

            // WACTFLAG
            if (t.contains("$WACTFLAG:")) {
                Integer v = extractAfterChecked(t, "$WACTFLAG:", -32768, 65535);
                if (v != null) currentMpt.setWactFlag(v);
            }

            // SE
            if (t.contains("$SE:")) {
                Integer v = extractAfterChecked(t, "$SE:", 0, 255);
                if (v != null) currentMpt.setSe(v);
            }

            /* ===================== MOVE ===================== */
            // $MOVE:{ é só um marcador, não precisamos de estado;
            // basta reagir a XSPEED/YSPEED/XACCEL/YACCEL em qualquer linha.
            // Estes SÃO os únicos campos do .dac que aceitam decimal por
            // design (pixels convertidos para fixed-point interno).

            if (t.contains("$XSPEED:")) {
                int v = pixelsToInternal(t, "$XSPEED:");
                currentMpt.setMoveXSpeed(v);
            }

            if (t.contains("$YSPEED:")) {
                int v = pixelsToInternal(t, "$YSPEED:");
                currentMpt.setMoveYSpeed(v);
            }

            if (t.contains("$XACCEL:")) {
                int v = pixelsToInternal(t, "$XACCEL:");
                currentMpt.setMoveXAccel(v);
            }

            if (t.contains("$YACCEL:")) {
                int v = pixelsToInternal(t, "$YACCEL:");
                currentMpt.setMoveYAccel(v);
            }

            /* ===================== HITS ===================== */
            // $HITS:{ é só um marcador; os dados vêm em $HT_x

            if (t.startsWith("$HT_")) {
                handleHit(t);
            }

            /* ===================== OPT ===================== */

            if (t.contains("$OPT:{")) {
                inOpt = true;
                handleOptLine(t);
            } else if (inOpt && (t.contains("$FLAG:") || t.contains("B["))) {
                handleOptLine(t);
            }

            /* ===================== RAZ ===================== */

            if (t.contains("$RAZ:{")) {
                inRaz = true;
                handleRazLine(t);
            } else if (inRaz &&
                    (t.contains("$ROT:") || t.contains("$ALPHA:")
                            || t.contains("$XZOOM:") || t.contains("$YZOOM:"))) {
                handleRazLine(t);
            }

            // Verifica CADA tag "$ALGO" presente na linha (não só a primeira) —
            // importante porque campos ficam frequentemente agrupados na mesma
            // linha (ex: "$ToME: 1, $IMG: 0, $MID:[...]"), e um typo num campo não
            // deve ficar escondido atrás de outros campos válidos que compartilham
            // a linha. Um cabeçalho numerado desconhecido ("$MoT_2:{") é tratado à
            // parte porque, diferente de um campo comum, ele indica um bloco
            // $ANO_/$MPT_ inteiro perdido, não só um valor não lido — $HT_N fica
            // de fora dessa checagem especial porque é o único cabeçalho numerado
            // legitimamente válido aqui dentro (tratado por handleHit acima).
            if (t.startsWith("$")) {
                if (looksLikeRootBlockHeader(t) && !t.startsWith("$MPT_") && !t.startsWith("$HT_")) {
                    String tag = extractLeadingTag(t);
                    if (tag != null) {
                        reportUnknownTag(tag, new String[]{"$ANO_", "$MPT_"},
                                "Cabeçalho de bloco numerado desconhecido dentro de $MPT_" + currentMpt.index
                                        + " — se a intenção era um novo $ANO_ ou $MPT_, este bloco será perdido");
                    }
                } else {
                    checkAllTagsInLine(t);
                }
            }
        }

        return rebuilder;
    }

    /* ======================================================
       Helpers básicos
    ====================================================== */

    private boolean readLine() throws IOException {
        line = br.readLine();
        if (line != null) lineNumber++;
        return (line != null);
    }

    /** Monta e reporta um erro com o contexto atual (arquivo, pasta, ano, mpt, linha). */
    private void reportError(String fieldTag, String message) {
        reportError(fieldTag, message, null);
    }

    private void reportError(String fieldTag, String message, String suggestion) {
        if (reporter == null) return;
        reporter.report(CompileError.builder()
                .fileKind("dac")
                .fileName(fileName)
                .pacFolder(pacFolder)
                .lineNumber(lineNumber)
                .rawLine(line != null ? line.trim() : null)
                .fieldTag(fieldTag)
                .anoIndex(currentAno != null ? currentAno.index : null)
                .mptIndex(currentMpt != null ? currentMpt.index : null)
                .message(message)
                .suggestion(suggestion));
    }

    /** Todas as tags de campo válidas em qualquer contexto do .dac — usado por
     *  checkAllTagsInLine para varrer uma linha inteira em busca de tags que não
     *  batem em NENHUMA delas, mesmo quando outras tags da mesma linha são válidas. */
    private static final String[] KNOWN_ALL_TAGS = {
        "$ANO_", "$PRIO:", "$LNKS:", "$LINK_", "$MPTS:", "$MPT_",
        "$BX:", "$BY:", "$ANO:", // campos dentro de $LINK_
        "$TIME:", "$IMG:", "$MID:", "$WACTFLAG:", "$SE:",
        "$MOVE:", "$XSPEED:", "$YSPEED:", "$XACCEL:", "$YACCEL:",
        "$HITS:", "$HT_", "$FLAG:", "$COORDS:", // $HITS: marcador; $HT_N cabeçalho numerado válido; campos dentro de $HT_
        "$OPT:", "$DATA:", "$RAZ:", "$ROT:", "$ALPHA:", "$XZOOM:", "$YZOOM:"
    };

    /**
     * Varre a linha inteira em busca de TODAS as ocorrências de "$TAG" (não só a
     * primeira), e reporta cada uma que não bate em nenhuma tag conhecida —
     * necessário porque vários campos costumam ficar na mesma linha
     * (ex: "$ToME: 1, $IMG: 0, $MID:[-28, -124],"), e um typo num campo não deve
     * passar despercebido só porque outro campo válido na mesma linha já fez a
     * linha inteira parecer "reconhecida".
     */
    private void checkAllTagsInLine(String t) {
        Matcher m = Pattern.compile("\\$[A-Za-z_]+[:_]?").matcher(t);
        while (m.find()) {
            String candidate = m.group();
            // normaliza para o formato "$TAG:" ou "$TAG_" antes de comparar
            if (!candidate.endsWith(":") && !candidate.endsWith("_")) candidate = candidate + ":";
            if (!matchesAny(candidate, KNOWN_ALL_TAGS)) {
                reportUnknownTag(candidate, KNOWN_ALL_TAGS,
                        currentMpt != null
                                ? "Tag desconhecida dentro de $MPT_" + currentMpt.index
                                : "Tag desconhecida dentro de $ANO_" + (currentAno != null ? currentAno.index : "?"));
            }
        }
    }

    /** true se a tag (com ":" ou "_" no final) bate com alguma das conhecidas, ignorando o sufixo. */
    private static boolean matchesAny(String tag, String[] known) {
        for (String k : known) {
            if (tag.equals(k)) return true;
        }
        return false;
    }

    /** Reporta uma tag "$ALGO" desconhecida, sugerindo a mais próxima entre as conhecidas. */
    private void reportUnknownTag(String tag, String[] known, String context) {
        if (reporter == null) return;
        String suggestion = TagSuggester.suggest(tag.replaceAll("[:_]$", ""), stripSuffixes(known));
        reportError(tag, "Tag desconhecida: \"" + tag + "\". " + context, suggestion);
    }

    private static String[] stripSuffixes(String[] tags) {
        String[] out = new String[tags.length];
        for (int i = 0; i < tags.length; i++) out[i] = tags[i].replaceAll("[:_]$", "");
        return out;
    }

    /**
     * Extrai um inteiro após a tag e valida contra [min, max], reportando (sem
     * abortar) quando o valor é decimal, não-numérico, ou fora do range esperado.
     * @return o valor extraído (mesmo fora de range), ou null se nem foi possível
     *         interpretar como número — quem chama decide se ainda assim usa 0.
     */
    private Integer extractAfterChecked(String s, String tag, int min, int max) {
        String raw = rawValueAfter(s, tag);
        if (raw == null) return null;

        if (raw.contains(".")) {
            reportError(tag, tag + " espera um inteiro, mas recebeu \"" + raw + "\" (valor decimal)");
        }

        int value;
        try {
            value = (int) Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            reportError(tag, tag + " espera um valor numérico, mas recebeu \"" + raw + "\"");
            return null;
        }

        if (value < min || value > max) {
            reportError(tag, tag + " = " + value + " está fora do range esperado [" + min + ".." + max
                    + "] — será truncado ao gravar no binário");
        }

        return value;
    }

    /** Extrai o texto bruto do valor logo após a tag (até vírgula, espaço ou fim da linha), sem converter. */
    private String rawValueAfter(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return null;
        p += tag.length();
        int n = s.length();
        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        int start = p;
        if (p < n && (s.charAt(p) == '-' || s.charAt(p) == '+')) p++;
        while (p < n && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;

        if (p == start) return null;
        return s.substring(start, p);
    }

    /** Extrai o PRIMEIRO inteiro encontrado na string (ex: "$PRIO: 3," → 3) */
    private int extractInt(String s) {
        if (s == null) return 0;
        int n = s.length();
        int i = 0;

        // achar início do número (primeiro dígito ou '-')
        while (i < n) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-') break;
            i++;
        }
        if (i >= n) return 0;

        int start = i;
        i++; // já passou pelo primeiro caractere (dígito ou '-')

        while (i < n) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') break;
            i++;
        }

        String num = s.substring(start, i);
        try {
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Extrai um inteiro logo após uma tag, ex: extractAfter("$PRIO: 3, $OUTRO", "$PRIO:") -> 3 */
    private int extractAfter(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return 0;

        p += tag.length();
        int n = s.length();

        // pular espaços
        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        // ler possível sinal
        StringBuilder sb = new StringBuilder();
        if (p < n && (s.charAt(p) == '-' || s.charAt(p) == '+')) {
            sb.append(s.charAt(p));
            p++;
        }

        // ler dígitos
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

    /**
     * Extrai um valor YES/NOT logo após uma tag e retorna 1 (YES) ou 0 (NOT/ausente), ex:
     * extractYesNot("...SPAWN_POS: YES...", "SPAWN_POS:") -> 1
     */
    private int extractYesNot(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return 0;

        p += tag.length();
        int n = s.length();

        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        StringBuilder sb = new StringBuilder();
        while (p < n && Character.isLetter(s.charAt(p))) {
            sb.append(s.charAt(p));
            p++;
        }

        return sb.toString().equalsIgnoreCase("YES") ? 1 : 0;
    }

    /**
     * Lê um valor em pixels (possivelmente decimal) e converte para coordenadas internas do jogo
     * multiplicando por 65536. Aceita tanto o formato novo ("3.5") quanto o antigo (inteiro bruto
     * como "229376"). Se o valor não tiver ponto decimal e for grande (>= 65536), assume que já
     * está no formato interno e retorna diretamente sem multiplicar.
     */
    private int pixelsToInternal(String s, String tag) {
        int p = s.indexOf(tag);
        if (p < 0) return 0;
        p += tag.length();
        int n = s.length();
        while (p < n && Character.isWhitespace(s.charAt(p))) p++;

        StringBuilder sb = new StringBuilder();
        if (p < n && (s.charAt(p) == '-' || s.charAt(p) == '+')) {
            sb.append(s.charAt(p++));
        }
        boolean hasDecimal = false;
        while (p < n) {
            char c = s.charAt(p);
            if (c >= '0' && c <= '9') {
                sb.append(c);
                p++;
            } else if (c == '.' && !hasDecimal) {
                hasDecimal = true;
                sb.append(c);
                p++;
            } else break;
        }
        if (sb.length() == 0) return 0;
        try {
            if (hasDecimal) {
                // Novo formato: valor em pixels → converte para interno
                return (int) (Double.parseDouble(sb.toString()) * 65536.0);
            } else {
                int raw = Integer.parseInt(sb.toString());
                // Se o valor é pequeno (< 65536), é um pixel value sem decimais
                // Se for grande, já está no formato interno (arquivo antigo)
                if (raw >= -65536 && raw <= 65536) {
                    return (int) ((double) raw * 65536.0);
                }
                return raw; // formato antigo, valor interno bruto
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /* ======================================================
       Handlers de linha (estilo parser do amigo)
    ====================================================== */

    private void handleAnoHeader(String t) {
        Matcher mAno = Pattern.compile("\\$ANO_\\s*([0-9]+)").matcher(t);
        if (!mAno.find()) return;

        int anoIndex = Integer.parseInt(mAno.group(1));
        currentAno = rebuilder.getAno(anoIndex);
        // NÃO marca used aqui: o DacExtractor imprime "$ANO_N: { }" também
        // para ANOs inexistentes (ponteiro -1 no binário). Marcar no cabeçalho
        // transformava todo ANO inexistente num ANO vazio (0 MPTs) — e o
        // _SetAct do jogo só pula o ANO quando o ponteiro é -1; com 0 MPTs ele
        // segue e lê frames de lixo. used é ligado por $PRIO/$LINK_/$MPTS/$MPT_.
    }

    private void handlePrio(String t) {
        int v = extractAfter(t, "$PRIO:");
        try {
            currentAno.setPriority(v);
        } catch (IllegalArgumentException e) {
            reportError("$PRIO", e.getMessage());
        }
    }

    private void handleLink(String t) {
        if (currentAno == null) return;

        int bx = extractAfter(t, "$BX:");
        int by = extractAfter(t, "$BY:");
        int spawnPos = extractYesNot(t, "SPAWN_POS:");
        int xy = extractYesNot(t, "XY:");
        int mpt = extractYesNot(t, "MPT:");
        int ano12 = extractAfter(t, "$ANO:");

        try {
            currentAno.addLink(bx, by, spawnPos, xy, mpt, ano12);
        } catch (IllegalArgumentException e) {
            reportError("$LINK", e.getMessage());
        }
    }

    private void handleMptHeader(String t) {
        if (currentAno == null) return;

        Matcher m = Pattern.compile("\\$MPT_\\s*([0-9]+)").matcher(t);
        if (!m.find()) return;

        int idx = Integer.parseInt(m.group(1));
        currentMpt = currentAno.getOrCreateMpt(idx);
    }

    private void handleMid(String t) {
        if (currentMpt == null) return;

        int start = t.indexOf("$MID:[");
        if (start < 0) return;
        start += "$MID:[".length();

        int end = t.indexOf(']', start);
        if (end <= start) return;

        String inside = t.substring(start, end).replace(" ", "");
        String[] parts = inside.split(",");
        if (parts.length < 2) return;

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            currentMpt.setMid(x, y);
        } catch (Exception ignored) {
        }
    }

    private void handleHit(String t) {
        if (currentMpt == null) return;

        int flagBase = extractAfter(t, "$FLAG:");

        int posCoords = t.indexOf("$COORDS:");
        if (posCoords < 0) {
            reportError("$HT_" + currentMpt.hits.size(), "$HT_N sem $COORDS: — hit ignorado");
            return;
        }

        int b1 = t.indexOf('[', posCoords);
        int b2 = t.indexOf(']', b1 + 1);
        if (b1 < 0 || b2 < 0) {
            reportError("$COORDS", "$COORDS malformado (colchetes ausentes) — hit ignorado");
            return;
        }

        String inside = t.substring(b1 + 1, b2).replace(" ", "");
        String[] parts = inside.split(",");
        if (parts.length < 4) {
            reportError("$COORDS", "$COORDS espera 4 valores [x, y, w, h], mas encontrou " + parts.length + " — hit ignorado");
            return;
        }

        for (String p : parts) {
            if (p.contains(".")) {
                reportError("$COORDS", "$COORDS espera inteiros, mas encontrou \"" + p + "\" (valor decimal)");
                break;
            }
        }

        int x, y, w, h;
        try {
            x = Integer.parseInt(parts[0]);
            y = Integer.parseInt(parts[1]);
            w = Integer.parseInt(parts[2]);
            h = Integer.parseInt(parts[3]);
        } catch (Exception e) {
            reportError("$COORDS", "$COORDS contém valor não-numérico — hit ignorado");
            return;
        }

        boolean longMode = false;
        if (x < -128 || x > 127) longMode = true;
        if (y < -128 || y > 127) longMode = true;
        if (w < 0 || w > 255) longMode = true;
        if (h < 0 || h > 255) longMode = true;

        currentMpt.addHit(flagBase, x, y, w, h, longMode);
    }

    private void handleOptLine(String t) {
        if (currentMpt == null) return;

        int flag = currentMpt.opt.used ? currentMpt.opt.flag : 0;
        int[] data = new int[4];
        if (currentMpt.opt.used) {
            data[0] = currentMpt.opt.data[0];
            data[1] = currentMpt.opt.data[1];
            data[2] = currentMpt.opt.data[2];
            data[3] = currentMpt.opt.data[3];
        }

        if (t.contains("$FLAG")) {
            flag = extractAfter(t, "$FLAG:");
        }

        int b1 = t.indexOf("B[");
        if (b1 >= 0) {
            int b2 = t.indexOf(']', b1 + 2);
            if (b2 > b1) {
                String inside = t.substring(b1 + 2, b2).replace(" ", "");
                String[] parts = inside.split(",");
                for (int i = 0; i < 4 && i < parts.length; i++) {
                    try {
                        data[i] = Integer.parseInt(parts[i]);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        currentMpt.setOpt(flag, data[0], data[1], data[2], data[3]);
    }

    private void handleRazLine(String t) {
        if (currentMpt == null) return;

        // valores atuais ou defaults como no parser original
        int rot = currentMpt.raz.used ? currentMpt.raz.rot : 0;
        int alpha = currentMpt.raz.used ? currentMpt.raz.alpha : 256;
        int xz = currentMpt.raz.used ? currentMpt.raz.xzoom : 100;
        int yz = currentMpt.raz.used ? currentMpt.raz.yzoom : 100;

        if (t.contains("$ROT:")) {
            rot = extractAfter(t, "$ROT:");
        }
        if (t.contains("$ALPHA:")) {
            alpha = extractAfter(t, "$ALPHA:");
        }
        if (t.contains("$XZOOM:")) {
            xz = extractAfter(t, "$XZOOM:");
        }
        if (t.contains("$YZOOM:")) {
            yz = extractAfter(t, "$YZOOM:");
        }

        currentMpt.setRaz(rot, alpha, xz, yz);
    }
}