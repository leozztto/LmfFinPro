package com.lmf.finpro.application;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deixa o serviço de aplicação acrescentar o que o fluxo fez (ids, contagens, desfecho) à linha
 * "Fluxo … concluído" que o aspecto de logging emite — sem o serviço depender de infraestrutura de
 * log. Só ids, contagens e desfechos: nunca valores financeiros, nomes ou e-mails.
 *
 * <p>Os detalhes ficam numa {@link ThreadLocal} que o aspecto zera ao iniciar o fluxo externo e
 * consome ao terminar; fora de um fluxo (ex.: teste unitário) a chamada é inofensiva.
 */
public final class FlowLog {

    private static final ThreadLocal<Map<String, Object>> DETAILS = new ThreadLocal<>();

    private FlowLog() {}

    /** Acrescenta {@code chave=valor} à linha de conclusão do fluxo em andamento. */
    public static void detail(String key, Object value) {
        Map<String, Object> details = DETAILS.get();
        if (details != null && value != null) {
            details.put(key, value);
        }
    }

    /** Usado pelo aspecto: abre a coleta de detalhes do fluxo externo. */
    public static void begin() {
        DETAILS.set(new LinkedHashMap<>());
    }

    /** Usado pelo aspecto: o que foi acumulado até agora, sem encerrar a coleta. */
    public static Map<String, Object> snapshot() {
        Map<String, Object> details = DETAILS.get();
        return details == null ? Map.of() : new LinkedHashMap<>(details);
    }

    /** Usado pelo aspecto: encerra a coleta do fluxo. */
    public static void end() {
        DETAILS.remove();
    }
}
