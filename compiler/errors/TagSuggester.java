package com.zue.gd.util;

/**
 * Sugere, a partir de uma tag desconhecida escrita pelo usuário (ex: "$AMO"), qual tag
 * válida conhecida é mais provável de ser a intenção (ex: "$ANO"), usando distância de
 * edição (Levenshtein).
 *
 * <p>Puramente utilitário — não decide nada sobre onde/como a sugestão é exibida.
 */
public final class TagSuggester {

    private TagSuggester() {}

    /**
     * Retorna a tag de {@code known} mais próxima de {@code unknown}, ou null se
     * nenhuma estiver razoavelmente próxima (evita sugestões absurdas tipo comparar
     * "$SE" com "$XSPEED").
     *
     * @param unknown     a tag desconhecida como apareceu no texto fonte
     * @param known       lista de tags válidas no contexto atual
     * @param maxDistance distância máxima aceitável para considerar uma sugestão válida
     */
    public static String suggest(String unknown, String[] known, int maxDistance) {
        if (unknown == null || known == null || known.length == 0) return null;

        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (String candidate : known) {
            int d = levenshtein(unknown, candidate);
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
            }
        }

        return (best != null && bestDist <= maxDistance) ? best : null;
    }

    /** Sobrecarga com limite padrão (2 edições) — cobre a maioria dos typos comuns. */
    public static String suggest(String unknown, String[] known) {
        return suggest(unknown, known, 2);
    }

    private static int levenshtein(String a, String b) {
        int la = a.length(), lb = b.length();
        int[][] dp = new int[la + 1][lb + 1];

        for (int i = 0; i <= la; i++) dp[i][0] = i;
        for (int j = 0; j <= lb; j++) dp[0][j] = j;

        for (int i = 1; i <= la; i++) {
            for (int j = 1; j <= lb; j++) {
                int cost = (Character.toUpperCase(a.charAt(i - 1)) == Character.toUpperCase(b.charAt(j - 1))) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[la][lb];
    }
}
