package com.zue.gd.util;

import java.io.File;

public final class Text00Extractor {

    private Text00Extractor() {}

    public static void process(String packName,
                               String index,
                               byte[] src,
                               int dataPos,
                               int dataSize,
                               File outDir) throws Exception {

        // O text00 tem UM HDAT por idioma: TCBManajer.TextGDTString lê
        // pTextData.piGameDataPos[iLanguage]. A extração antiga só escrevia o
        // HDAT 0 com "$COUNT: 1" e o rebuild perdia os outros idiomas. O
        // BinExtractor (pasta "text00" = texto) faz todos os HDATs no MESMO
        // formato ($HDAT_N / $TXT / $T_N), com verificação de ida e volta.
        BinExtractor.process(packName, index, src, dataPos, dataSize, 3, outDir);
    }
}
