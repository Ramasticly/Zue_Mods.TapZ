package com.zue.gd.util;

/**
 * Representa UM erro (ou aviso) encontrado durante a compilação de um arquivo fonte
 * (.dac.txt, .cnv.txt, .bin.txt) para o binário do jogo.
 *
 * <p>Esta classe é <b>puramente um contêiner de dados</b>. Ela não decide como o erro
 * deve ser exibido — isso é responsabilidade de cada app que consome o compiler_core.
 * Um app de linha de comando pode simplesmente imprimir {@link #toString()}; um app
 * com editor visual pode usar {@link #lineNumber} e {@link #rawLine} para abrir o
 * arquivo no trecho certo e sublinhar, e {@link #suggestion} para popular um menu de
 * "você quis dizer?".
 *
 * <p>Imutável: construído com todos os campos de uma vez, via {@link Builder}.
 */
public final class CompileError {

    public enum Severity {
        /** Impede a geração de um binário confiável — mas o core NÃO aborta o build por conta disso. */
        ERROR,
        /** Situação estranha, mas não necessariamente errada (ex: campo redundante). */
        WARNING
    }

    /* ======================================================
       Identificação do problema
    ====================================================== */

    /** ERROR ou WARNING. */
    public final Severity severity;

    /** Mensagem pronta para leitura humana, ex: "Tag desconhecida: $AMO". */
    public final String message;

    /** Sugestão de correção, ex: "$ANO". Null se não houver nenhuma candidata razoável. */
    public final String suggestion;

    /* ======================================================
       Localização no projeto (pasta .pac / arquivo)
    ====================================================== */

    /** Nome da pasta .pac sendo compilada, ex: "gamedata", "char03". Pode ser null se não aplicável. */
    public final String pacFolder;

    /** Nome do arquivo fonte, ex: "personagem.dac.txt". */
    public final String fileName;

    /** Tipo de arquivo fonte: "dac", "cnv" ou "bin". */
    public final String fileKind;

    /* ======================================================
       Localização dentro do arquivo
    ====================================================== */

    /** Número da linha (1-based) onde o problema foi detectado. -1 se não aplicável (ex: erro em bloco regex do .cnv). */
    public final int lineNumber;

    /** Conteúdo bruto da linha/trecho onde o problema ocorreu, para permitir sublinhar na UI. Pode ser null. */
    public final String rawLine;

    /* ======================================================
       Contexto lógico (dentro da estrutura do jogo)
    ====================================================== */

    /** Índice do $ANO_ em questão. Null se não aplicável. */
    public final Integer anoIndex;

    /** Índice do $MPT_ em questão. Null se não aplicável. */
    public final Integer mptIndex;

    /** Índice do $HDAT_ em questão (formato .bin). Null se não aplicável. */
    public final Integer hdatIndex;

    /** Tag/campo específico relacionado ao erro, ex: "$SE". Null se não aplicável. */
    public final String fieldTag;

    private CompileError(Builder b) {
        this.severity = b.severity;
        this.message = b.message;
        this.suggestion = b.suggestion;
        this.pacFolder = b.pacFolder;
        this.fileName = b.fileName;
        this.fileKind = b.fileKind;
        this.lineNumber = b.lineNumber;
        this.rawLine = b.rawLine;
        this.anoIndex = b.anoIndex;
        this.mptIndex = b.mptIndex;
        this.hdatIndex = b.hdatIndex;
        this.fieldTag = b.fieldTag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(severity).append("] ");
        if (pacFolder != null) sb.append(pacFolder).append('/');
        if (fileName != null) sb.append(fileName);
        if (lineNumber >= 0) sb.append(":").append(lineNumber);
        sb.append(" - ").append(message);
        if (fieldTag != null) sb.append(" (campo ").append(fieldTag).append(')');
        if (anoIndex != null) sb.append(" [ANO_").append(anoIndex).append(']');
        if (mptIndex != null) sb.append(" [MPT_").append(mptIndex).append(']');
        if (hdatIndex != null) sb.append(" [HDAT_").append(hdatIndex).append(']');
        if (suggestion != null) sb.append(" -- sugestão: ").append(suggestion);
        return sb.toString();
    }

    /* ======================================================
       Builder
    ====================================================== */

    public static final class Builder {
        private Severity severity = Severity.ERROR;
        private String message;
        private String suggestion;
        private String pacFolder;
        private String fileName;
        private String fileKind;
        private int lineNumber = -1;
        private String rawLine;
        private Integer anoIndex;
        private Integer mptIndex;
        private Integer hdatIndex;
        private String fieldTag;

        public Builder severity(Severity s) { this.severity = s; return this; }
        public Builder message(String m) { this.message = m; return this; }
        public Builder suggestion(String s) { this.suggestion = s; return this; }
        public Builder pacFolder(String p) { this.pacFolder = p; return this; }
        public Builder fileName(String f) { this.fileName = f; return this; }
        public Builder fileKind(String k) { this.fileKind = k; return this; }
        public Builder lineNumber(int l) { this.lineNumber = l; return this; }
        public Builder rawLine(String r) { this.rawLine = r; return this; }
        public Builder anoIndex(Integer a) { this.anoIndex = a; return this; }
        public Builder mptIndex(Integer m) { this.mptIndex = m; return this; }
        public Builder hdatIndex(Integer h) { this.hdatIndex = h; return this; }
        public Builder fieldTag(String t) { this.fieldTag = t; return this; }

        public CompileError build() {
            if (message == null) throw new IllegalStateException("CompileError requer message");
            return new CompileError(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
