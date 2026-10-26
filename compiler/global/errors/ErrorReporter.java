package com.zue.gd.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coletor de {@link CompileError}s ao longo de uma compilação.
 *
 * <p><b>Contrato central:</b> este objeto é sempre <b>opcional</b> (pode ser {@code null})
 * em qualquer parser/builder do compiler_core. Quando null, todo {@code report(...)} deve
 * ser tratado como no-op pelo chamador (verificar {@code reporter != null} antes de
 * chamar) — isso garante que apps que não usam o sistema de erros tenham custo zero e
 * comportamento idêntico ao de antes desta mudança (valores truncados/ignorados
 * silenciosamente, binário gerado do mesmo jeito).
 *
 * <p>O reporter NUNCA lança exceção e NUNCA aborta a compilação por conta própria — ele
 * só acumula. Cabe a quem chamou {@code build(...)} decidir, ao final, se
 * {@link #hasErrors()} deve impedir a gravação do binário.
 *
 * <p>Uma instância nova deve ser criada a cada compilação (por arquivo, ou por pasta
 * .pac, dependendo da granularidade que o app quiser) — não deve ser reaproveitada
 * entre arquivos diferentes, para não misturar erros de compilações distintas.
 */
public final class ErrorReporter {

    private final List<CompileError> errors = new ArrayList<CompileError>();

    /** Registra um erro/aviso. Nunca lança exceção. */
    public void report(CompileError error) {
        if (error != null) errors.add(error);
    }

    /** Atalho para reportar sem passar por CompileError.builder() manualmente no ponto de chamada. */
    public void report(CompileError.Builder builder) {
        report(builder.build());
    }

    public boolean hasErrors() {
        for (CompileError e : errors) {
            if (e.severity == CompileError.Severity.ERROR) return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        for (CompileError e : errors) {
            if (e.severity == CompileError.Severity.WARNING) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public int count() {
        return errors.size();
    }

    /** Lista imutável de todos os erros/avisos coletados, na ordem em que ocorreram. */
    public List<CompileError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Limpa o estado acumulado. Útil se o app quiser reaproveitar a mesma instância entre arquivos. */
    public void clear() {
        errors.clear();
    }

    /**
     * Helper estático: chama {@code reporter.report(builder)} apenas se reporter != null.
     * Uso típico dentro de código de parsing:
     * {@code ErrorReporter.reportIfPresent(reporter, CompileError.builder()...);}
     */
    public static void reportIfPresent(ErrorReporter reporter, CompileError.Builder builder) {
        if (reporter != null) reporter.report(builder);
    }
}
