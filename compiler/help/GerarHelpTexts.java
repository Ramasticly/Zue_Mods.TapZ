import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gera compiler/global/help/HelpTexts.java a partir dos .md desta pasta
 * (binChar.md e gamedata.md), que o Compiler grava na pasta #HELP.
 *
 * Uso, na pasta compiler/help:   java GerarHelpTexts.java
 *
 * Não faz parte do jogo nem do app (não copie para o projeto). Rode de novo
 * sempre que editar um dos .md.
 */
public class GerarHelpTexts {

    static final String[][] ARQUIVOS = {
        {"BIN_CHAR", "binChar.md"},
        {"GAMEDATA", "gamedata.md"},
    };

    // Um literal de String do Java aceita no máximo 65535 bytes (UTF-8
    // modificado): cada .md vira um array de pedaços abaixo disso.
    static final int MAX_BYTES = 60000;

    public static void main(String[] a) throws IOException {
        Path dir = Paths.get(a.length > 0 ? a[0] : ".");
        Path out = a.length > 1 ? Paths.get(a[1]) : dir.resolve("../global/help/HelpTexts.java").normalize();

        StringBuilder sb = new StringBuilder();
        sb.append("package com.zue.gd.util;\n\n");
        sb.append("// GERADO por compiler/help/GerarHelpTexts.java a partir de compiler/help/*.md.\n");
        sb.append("// Não edite à mão: edite o .md e rode o gerador de novo.\n");
        sb.append("final class HelpTexts {\n\n");
        sb.append("    private HelpTexts() {}\n");
        for (String[] arq : ARQUIVOS) {
            String texto = new String(Files.readAllBytes(dir.resolve(arq[1])), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            sb.append("\n    /** ").append(arq[1]).append(" */\n");
            sb.append("    static final String[] ").append(arq[0]).append(" = {\n");
            int bytes = 0;
            boolean abriu = false;
            for (String linha : texto.split("(?<=\n)")) {
                int b = mutf8(linha);
                if (abriu && bytes + b > MAX_BYTES) {
                    sb.append(",\n");
                    abriu = false;
                    bytes = 0;
                }
                sb.append(abriu ? " +\n" : "").append("        \"").append(escapar(linha)).append('"');
                abriu = true;
                bytes += b;
            }
            if (!abriu) sb.append("        \"\"");
            sb.append("\n    };\n");
        }
        sb.append("}\n");
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("gravado " + out);
    }

    static int mutf8(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            n += (c >= 1 && c <= 0x7F) ? 1 : (c <= 0x7FF ? 2 : 3);
        }
        return n;
    }

    static String escapar(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
