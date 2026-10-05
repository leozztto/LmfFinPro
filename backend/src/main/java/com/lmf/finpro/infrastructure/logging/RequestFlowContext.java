package com.lmf.finpro.infrastructure.logging;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ponte entre o {@link UseCaseLoggingAspect} e o {@link RequestIdFilter}: dentro de uma requisição
 * HTTP, o fluxo de negócio executado não ganha uma linha própria — o aspecto o registra aqui e o
 * filtro o anexa à linha de acesso. Assim, cada requisição gera uma única linha em INFO, com o
 * fluxo, os ids tocados e o tamanho das listas devolvidas.
 *
 * <p>A coleta vive numa {@link ThreadLocal} aberta e fechada pelo filtro. Fora dela (scheduler,
 * testes de serviço) {@link #record} devolve {@code false} e o aspecto loga o fluxo por conta
 * própria.
 */
final class RequestFlowContext {

    private static final ThreadLocal<Summary> CURRENT = new ThreadLocal<>();

    private RequestFlowContext() {}

    /** Fluxos executados na requisição (na ordem) e o contexto acumulado por eles. */
    record Summary(StringBuilder flows, Map<String, Object> context) {}

    static void begin() {
        CURRENT.set(new Summary(new StringBuilder(), new LinkedHashMap<>()));
    }

    /**
     * @return {@code false} se não há requisição HTTP em andamento nesta thread
     */
    static boolean record(String flow, Map<String, Object> context) {
        Summary summary = CURRENT.get();
        if (summary == null) {
            return false;
        }
        if (summary.flows().length() > 0) {
            summary.flows().append(',');
        }
        summary.flows().append(flow);
        summary.context().putAll(context);
        return true;
    }

    /** Devolve o resumo da requisição e encerra a coleta; {@code null} se não houve coleta. */
    static Summary end() {
        Summary summary = CURRENT.get();
        CURRENT.remove();
        return summary;
    }
}
