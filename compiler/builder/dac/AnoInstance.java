package com.zue.gd.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um ANO (Act Node) completo.
 * Cada ANO contém:
 *  - prioridade (opcional)
 *  - lista de links (opcional)
 *  - lista de MPTs
 *
 * Esta classe NÃO faz cálculos de flags nem offsets.
 * Ela apenas armazena os dados de forma organizada.
 */
public final class AnoInstance {

    public final int index;

    /** Indica se este ANO realmente existe (entryPtr != -1 no DAC). */
    public boolean used;

    /* ======================================================
       PRIORITY
    ====================================================== */
    public boolean hasPriority;
    public int priority;

    /* ======================================================
       LINKS
    ====================================================== */

    /**
     * Um link de ANO:
     *  BX
     *  BY
     *  SPAWN_POS (bit 12) - 0 = nasce na posição do pai, 1 = nasce na posição própria (BX/BY)
     *  XY        (bit 14) - 0 = não segue o pai continuamente, 1 = segue a posição do pai a cada frame
     *  MPT       (bit 15) - 0 = frames livres, 1 = frames sincronizados com o pai
     *  ANO   (12 bits)
     */
    public static final class Link {
        public int bx;
        public int by;
        public int spawnPos;
        public int xy;
        public int mpt;
        public int ano12;

        /** Recompõe os 4 bits altos (12-15) no mesmo layout usado pelo binário. Bit 13 sempre 0 (sem uso confirmado). */
        public int flag4() {
            return (spawnPos & 1) | ((xy & 1) << 2) | ((mpt & 1) << 3);
        }
    }

    /** Lista de todos os links deste ANO */
    public final List<Link> links = new ArrayList<Link>();

    /* ======================================================
       MPTS
    ====================================================== */

    /** Todos os MPTs deste ANO (podem existir buracos) */
    public final List<MptInstance> mpts = new ArrayList<MptInstance>();

    public AnoInstance(int index) {
        this.index = index;
    }

    /**
     * Garante que existe um MPT no índice desejado.
     * Se ainda não existe, cria.
     */
    public MptInstance getOrCreateMpt(int mptIndex) {
        while (mpts.size() <= mptIndex) {
            mpts.add(new MptInstance(this, mpts.size()));
        }
        used = true;
        MptInstance m = mpts.get(mptIndex);
        m.used = true;
        return m;
    }

    /* ======================================================
       HELPERS DO ANO
    ====================================================== */

    public void setPriority(int prio) {
        if (prio < -128 || prio > 127)
            throw new IllegalArgumentException("Priority fora do range de byte: " + prio);

        this.hasPriority = true;
        this.priority = prio;
        this.used = true;
    }

    public void addLink(int bx, int by, int spawnPos, int xy, int mpt, int ano12) {
        if (spawnPos < 0 || spawnPos > 1)
            throw new IllegalArgumentException("spawnPos deve ser 0 ou 1 (1 bit)");
        if (xy < 0 || xy > 1)
            throw new IllegalArgumentException("xy deve ser 0 ou 1 (1 bit)");
        if (mpt < 0 || mpt > 1)
            throw new IllegalArgumentException("mpt deve ser 0 ou 1 (1 bit)");

        if (ano12 < 0 || ano12 > 4095)
            throw new IllegalArgumentException("ano12 deve estar no range 0..4095 (12 bits)");

        Link l = new Link();
        l.bx = bx;
        l.by = by;
        l.spawnPos = spawnPos;
        l.xy = xy;
        l.mpt = mpt;
        l.ano12 = ano12;

        links.add(l);
        used = true;
    }

    /**
     * Quantos MPTs realmente existem (mpt.used == true).
     * O DAC ignora MPTs “vazios”.
     */
    public int getMptCountUsed() {
        int max = 0;
        for (int i = 0; i < mpts.size(); i++) {
            if (mpts.get(i).used) {
                max = i + 1;
            }
        }
        return max;
    }
}