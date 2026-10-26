package com.zue.gd.util;

public final class DacRebuilder {

    private final AnoInstance[] anos;

    public DacRebuilder(int anoCount) {
        if (anoCount <= 0)
            throw new IllegalArgumentException("anoCount deve ser > 0");

        this.anos = new AnoInstance[anoCount];
        for (int i = 0; i < anoCount; i++) {
            anos[i] = new AnoInstance(i);
        }
    }

    /** Pega a instância de um ANO específico */
    public AnoInstance getAno(int idx) {
        if (idx < 0 || idx >= anos.length)
            throw new IndexOutOfBoundsException("Índice de ANO inválido: " + idx);
        return anos[idx];
    }

    /** Gera o arquivo DAC final */
    public byte[] build() {
        return new DacWriterCore(anos).build();
    }
    
    
    
    
    
    
    public int getAnoCount() {
    return anos.length;
}



}