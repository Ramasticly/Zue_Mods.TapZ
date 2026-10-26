package com.zue.gd.util;

import java.io.*;
import java.util.*;

public final class PacBuilder {

    // Quando true, build() não deixa os binários soltos na pasta de
    // origem — ao final do processamento, empacota tudo em um .pac real
    // via RealPacBuilder (mesmo empacotador usado pelas ferramentas de
    // build do jogo), delegando em pacOutRoot.
    //
    // Nasce false: o comportamento do jogo (Compiler.java, em runtime)
    // não muda em nada — ele nunca liga esta flag. Cabe ao app ligá-la
    // antes de rodar o build em lote.
    //
    // É a ÚNICA flag de modo do pacote: PacExtractor também a consulta
    // (organização da extração em BACKS/CARDS/CHARS/SYSTEM), então o app
    // só precisa de "PacBuilder.isApp = true;" uma vez.
    public static volatile boolean isApp = false;

    // Callback opcional de progresso, por item processado (.spr ou .*.txt
    // convertido). Aditivo: build(folderPath) continua igual, cb == null
    // internamente — nenhuma mudança para quem já chama essa assinatura.
    public interface ProgressCallback {
        void onItemStart(String packName, String fileName, int index, int total);
        void onItemBuilt(String packName, String fileName, int index, int total);
    }

    private static final int IO_BUFFER = 2 * 1024 * 1024; // 2MB

    private PacBuilder() {}

    /**
     * @param folderPath Caminho da pasta que contém os arquivos JSON/TXT para converter.
     */
    public static void build(String folderPath) throws Exception {
        build(folderPath, null, null);
    }

    /**
     * Mesmo comportamento de build(folderPath), com um callback opcional de
     * progresso por item (pasta .spr ou arquivo .txt convertido). Passe
     * cb == null para o comportamento padrão, sem overhead de notificação.
     *
     * @param folderPath Caminho da pasta que contém os arquivos JSON/TXT para converter.
     * @param cb Callback de progresso por item, ou null.
     */
    /**
     * Mesmo comportamento de build(folderPath, cb), com um destino explícito
     * para o .pac final quando isApp == true. Ignorado (pode ser null)
     * quando isApp == false — nesse caso o comportamento é sempre o de
     * gravar os binários soltos na própria pasta, como sempre foi.
     *
     * @param folderPath Caminho da pasta que contém os arquivos JSON/TXT para converter.
     * @param cb Callback de progresso por item, ou null.
     * @param pacOutRoot Pasta onde salvar o .pac final (ex: apk/assets/).
     *                   Obrigatório apenas quando isApp == true.
     */
    public static void build(String folderPath, ProgressCallback cb, File pacOutRoot) throws Exception {
        build(folderPath, cb, pacOutRoot, null);
    }

    /**
     * Mesmo comportamento de build(folderPath, cb, pacOutRoot), com um
     * ErrorReporter opcional (pode ser null — nesse caso custo extra é zero)
     * que coleta erros de sintaxe/valor de TODOS os arquivos processados
     * (.dac.txt, .cnv.txt, .bin.txt) sem nunca abortar a geração dos
     * binários — cabe a quem chamou decidir, ao final, se
     * reporter.hasErrors() deve impedir o uso do resultado.
     *
     * @param folderPath Caminho da pasta que contém os arquivos JSON/TXT para converter.
     * @param cb Callback de progresso por item, ou null.
     * @param pacOutRoot Pasta onde salvar o .pac final (ex: apk/assets/). Obrigatório apenas quando isApp == true.
     * @param reporter Coletor de erros de validação, ou null para não coletar.
     */
    public static void build(String folderPath, ProgressCallback cb, File pacOutRoot, ErrorReporter reporter) throws Exception {

        File pacFolder = new File(folderPath);

        if (!pacFolder.exists() || !pacFolder.isDirectory()) {
            throw new FileNotFoundException("Diretório não encontrado: " + folderPath);
        }
        if (HelpFolder.isHelp(pacFolder)) return; // documentação, não é um pack

        // packName sem ".pac" para verificações internas
        String packName = pacFolder.getName().toLowerCase();

        File[] files = pacFolder.listFiles();
        if (files == null) return;

        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });

        int total = countBuildableItems(files);
        int index = 0;

        // Loop de processamento
        for (File f : files) {
            String fileName = f.getName().toLowerCase();

            // 1. IGNORAR: PNG, WAV e ACT agora são ignorados (mantidos intactos na pasta)
            if (fileName.endsWith(".png") || fileName.endsWith(".wav") || fileName.endsWith(".act")) {
                continue;
            }

            // 2. SPR: Processa diretórios de sprites se existirem
            if (f.isDirectory() && fileName.endsWith(".spr")) {
                if (cb != null) cb.onItemStart(packName, f.getName(), index, total);
                SprBuilder.build(f, reporter);
                if (cb != null) cb.onItemBuilt(packName, f.getName(), index, total);
                index++;
                continue;
            }

            // 3. CONVERSÃO: Apenas arquivos de texto (.txt) que representam BIN, CNV ou DAC
            if (!f.isFile() || !fileName.endsWith(".txt")) {
                continue;
            }

            if (cb != null) cb.onItemStart(packName, f.getName(), index, total);

            byte[] output = null;

            if (fileName.endsWith(".bin.txt")) {
                output = BinBuilder.build(f, reporter);
            }
            else if (fileName.endsWith(".cnv.txt")) {
                output = CnvBuilder.build(packName, f, reporter);
            }
            else if (fileName.endsWith(".dac.txt")) {
                // Lógica específica para gamedata e text00 sem necessidade de ".pac" no nome
                if ("gamedata".equals(packName)) {
                    output = BinBuilder.build(f, reporter);
                }
                else if ("text00".equals(packName)) {
                    output = Text00Builder.build(f);
                }
                else {
                    output = buildDacBinary(f, reporter, packName);
                }
            }

            // 4. SALVAMENTO: Grava o binário na mesma pasta e remove o arquivo .txt original (opcional)
            if (output != null) {
                File outFile = new File(pacFolder, stripTxtExtension(f.getName()));
                writeFile(outFile, output);
                
                // Opcional: f.delete(); // Se quiser limpar o .txt após gerar o binário
            }

            if (cb != null) cb.onItemBuilt(packName, f.getName(), index, total);
            index++;
        }

        // 5. EMPACOTAMENTO FINAL (só app): os binários já estão gravados na
        // pasta (passo 4, igual ao comportamento do jogo). Aqui apenas
        // empacota essa mesma pasta em um .pac real via RealPacBuilder, que
        // já sabe ignorar .txt/.json e tratar sub-pastas .spr — nenhuma
        // lógica de empacotamento duplicada aqui.
        //
        // RealPacBuilder.buildSingle NUNCA lança exceção — ela mesma
        // captura tudo internamente e só notifica erro via callback (que
        // pode ser null). Por isso passamos um callback aqui, mesmo sem
        // precisar do progresso: sem ele, uma falha ao empacotar (ex:
        // pacOutRoot sem permissão de escrita, arquivo corrompido) seria
        // engolida em silêncio — o build "passaria" sem gerar o .pac.
        if (isApp) {
            if (pacOutRoot == null) {
                throw new IllegalArgumentException(
                        "isApp=true requer pacOutRoot (pasta de destino do .pac final)");
            }

            final String[] errorHolder = new String[1];
            RealPacBuilder.buildSingle(pacFolder, pacOutRoot, new RealPacBuilder.ProgressCallback() {
                @Override public void onBuildStart(String p, int idx, int t) {}
                @Override public void onSegmentPacked(String p, int idx, String f, int s) {}
                @Override public void onBuildDone(String p) {}
                @Override public void onError(String e) { errorHolder[0] = e; }
            });

            if (errorHolder[0] != null) {
                throw new Exception("Falha ao empacotar " + packName + ".pac: " + errorHolder[0]);
            }

            // Confirma que o .pac de fato existe no destino — cobre casos em
            // que buildSingle silenciosamente não escreveu nada (ex: pasta
            // vazia) sem necessariamente reportar erro.
            String pacFileName = packName.toLowerCase().endsWith(".pac") ? packName : packName + ".pac";
            File expectedPac = new File(pacOutRoot, pacFileName);
            if (!expectedPac.exists() || expectedPac.length() == 0) {
                throw new Exception(".pac não foi gravado em " + expectedPac.getAbsolutePath());
            }
        }
    }

    // Conta quantos itens de fato geram trabalho notificável (pastas .spr
    // e arquivos .txt), para o "total" do callback refletir o trabalho
    // real em vez do total de arquivos da pasta (que inclui png/wav/act
    // e os binários já gerados em builds anteriores).
    private static int countBuildableItems(File[] files) {
        int count = 0;
        for (File f : files) {
            String fileName = f.getName().toLowerCase();
            if (f.isDirectory() && fileName.endsWith(".spr")) {
                count++;
            } else if (f.isFile() && fileName.endsWith(".txt")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Apaga, antes de um build, os binários que o build vai regenerar a partir
     * de um .txt que ainda existe na pasta: "000.dac" quando há "000.dac.txt"
     * (idem .bin/.cnv) e, dentro de cada pasta .spr, "000.bin" quando há
     * "000.bin.txt". Assim um .txt que falhar ao compilar não deixa o binário
     * antigo ser carregado em silêncio.
     *
     * Nunca apaga o que o build NÃO recria (.act, .png, .wav, .mp4, .webp, ou
     * um binário sem .txt correspondente). A versão anterior (duplicada em
     * Compiler e ModApkBuilder) montava o nome pelo índice do bloco na tabela
     * do .pac — mas o PacExtractor sempre grava ACT/BIN/CNV/DAC/SPR como "000",
     * então ela apagava o arquivo errado ou, quando o bloco 0 era ACT, apagava
     * o 000.act (dado cru, sem .txt para regenerar).
     */
    public static void cleanProjectFolder(File pacFolder) {
        if (pacFolder == null || !pacFolder.isDirectory()) return;
        cleanGeneratedBinaries(pacFolder);
        File[] subs = pacFolder.listFiles();
        if (subs == null) return;
        for (File f : subs) {
            if (f.isDirectory() && f.getName().toLowerCase().endsWith(".spr")) {
                cleanGeneratedBinaries(f);
            }
        }
    }

    private static void cleanGeneratedBinaries(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isFile() || !f.getName().toLowerCase().endsWith(".txt")) continue;
            String target = stripTxtExtension(f.getName());
            String t = target.toLowerCase();
            if (t.endsWith(".bin") || t.endsWith(".cnv") || t.endsWith(".dac")) {
                File out = new File(dir, target);
                if (out.isFile()) out.delete();
            }
        }
    }

    // Case-insensitive, igual ao SprBuilder: com ".TXT" maiúsculo o
    // lastIndexOf(".txt") dava -1 e o binário era gravado POR CIMA do .txt.
    private static String stripTxtExtension(String name) {
        int idx = name.toLowerCase().lastIndexOf(".txt");
        return (idx > 0) ? name.substring(0, idx) : name;
    }

    private static void writeFile(File target, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(target);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private static byte[] buildDacBinary(File dumpFile, ErrorReporter reporter, String packName) throws Exception {
        byte[] dump = readFast(dumpFile);
        int anoCount = descobrirAnoCount(dump);
        if (anoCount <= 0) {
            throw new Exception("DAC sem $ANO_: " + dumpFile.getName());
        }

        ByteArrayInputStream in = new ByteArrayInputStream(dump);
        // ExtrasParser guarda estado ESTÁTICO entre parse() e build() — o
        // jogo (SetLoad) e o export de APK (thread própria) podem compilar ao
        // mesmo tempo, então parse+build de um .dac precisam ser atômicos.
        synchronized (ExtrasParser.class) {
            try {
                DacParser parser = new DacParser(in, anoCount, reporter, dumpFile.getName(), packName);
                return parser.parse().build();
            } finally {
                ExtrasParser.reset(); // solta as referências deste .dac (o mapa é estático)
            }
        }
    }

    private static byte[] readFast(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) f.length());
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static int descobrirAnoCount(byte[] bytes) throws Exception {
        // Usando MS932 se for compatível com Shift-JIS de jogos antigos, 
        // ou UTF-8 conforme seu padrão
        String s = new String(bytes, "UTF-8");
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("\\$ANO_\\s*(\\d+)").matcher(s);

        int max = -1;
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v > max) max = v;
        }
        return max < 0 ? 0 : max + 1;
    }
}
