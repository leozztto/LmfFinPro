package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/**
 * Observação automática sobre os dados do usuário, detectada por {@link InsightDetector}.
 *
 * @param type {@link AlertType#INSIGHT_SUBSCRIPTION}, {@link AlertType#INSIGHT_UNUSUAL_EXPENSE} ou
 *     {@link AlertType#INSIGHT_LATE_CLIENT}
 * @param key chave de deduplicação em {@code sent_alerts} (até 50 caracteres): o mesmo insight só é
 *     avisado uma vez por chave
 * @param subject descrição da cobrança, da despesa ou nome do cliente
 * @param amount assinatura: valor mensal; despesa fora do padrão: valor da despesa; cliente: soma
 *     dos recebimentos em atraso
 * @param reference só na despesa fora do padrão: a média da categoria; {@code null} nos demais
 * @param count assinatura: meses seguidos cobrados; cliente: quantos recebimentos atrasados; 0 na
 *     despesa fora do padrão
 */
public record Insight(
        AlertType type,
        String key,
        String subject,
        BigDecimal amount,
        BigDecimal reference,
        int count) {}
