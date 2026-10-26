package com.zue.gd.util;

import java.io.File;

public final class Text00Builder {

    private Text00Builder() {}

    public static byte[] build(File txtFile) throws Exception {
        // Mesmo formato do BinBuilder ($COUNT + $HDAT_N com $TXT): compila TODOS
        // os HDATs (um por idioma — TCBManajer.TextGDTString lê o HDAT
        // iLanguage). A versão anterior só gerava o HDAT 0. Textos antigos com
        // "$COUNT: 1" continuam gerando o mesmo binário de antes.
        return BinBuilder.build(txtFile);
    }
}
