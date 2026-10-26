package com.zue.gd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pasta #HELP com a documentação dos HDATs (binChar.md e gamedata.md). O
 * Compiler a cria na raiz dos packs: no jogo, junto das pastas dos .pac
 * (<raiz>/#HELP); no app, na categoria SYSTEM (<raiz>/SYSTEM/#HELP).
 *
 * O texto vem de HelpTexts (gerado a partir de compiler/help/*.md). Um
 * arquivo diferente do embutido é regravado, então a ajuda acompanha a
 * versão do Compiler.
 */
public final class HelpFolder {

    public static final String NOME = "#HELP";

    private static final Set<String> feitas = new HashSet<String>();

    private HelpFolder() {}

    /** true se a pasta é a #HELP: não é um pack, não compila nem empacota. */
    public static boolean isHelp(File dir) {
        return dir != null && NOME.equalsIgnoreCase(dir.getName());
    }

    /** Onde fica a #HELP para esta raiz de packs (depende de PacBuilder.isApp). */
    public static File pasta(File raiz) {
        return PacBuilder.isApp ? new File(new File(raiz, "SYSTEM"), NOME) : new File(raiz, NOME);
    }

    /**
     * Cria ou atualiza a #HELP da raiz (no máximo uma vez por raiz em cada
     * execução). Uma falha de escrita é ignorada: a ajuda é opcional.
     */
    public static synchronized void garantir(File raiz) {
        if (raiz == null) return;
        File dir = pasta(raiz);
        String chave = dir.getAbsolutePath();
        if (feitas.contains(chave)) return;
        try {
            escrever(dir, "binChar.md", HelpTexts.BIN_CHAR);
            escrever(dir, "gamedata.md", HelpTexts.GAMEDATA);
            feitas.add(chave);
        } catch (IOException e) {
            // sem permissão ou sem espaço: tenta de novo na próxima chamada
        }
    }

    private static void escrever(File dir, String nome, String[] partes) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) sb.append(p);
        byte[] novo = sb.toString().getBytes(StandardCharsets.UTF_8);
        File f = new File(dir, nome);
        if (f.isFile() && f.length() == novo.length && Arrays.equals(ler(f), novo)) return;
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("não consegui criar " + dir);
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(novo);
        } finally {
            out.close();
        }
    }

    private static byte[] ler(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        try {
            int off = 0;
            while (off < b.length) {
                int r = in.read(b, off, b.length - off);
                if (r <= 0) break;
                off += r;
            }
        } finally {
            in.close();
        }
        return b;
    }
}
