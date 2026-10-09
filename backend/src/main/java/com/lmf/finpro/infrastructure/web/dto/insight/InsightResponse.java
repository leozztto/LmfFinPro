package com.lmf.finpro.infrastructure.web.dto.insight;

import java.math.BigDecimal;

/**
 * @param type {@code SUBSCRIPTION}, {@code UNUSUAL_EXPENSE} ou {@code LATE_CLIENT}
 * @param reference só na despesa fora do padrão: a média da categoria
 * @param count assinatura: meses seguidos; cliente: recebimentos atrasados; 0 nos demais
 */
public record InsightResponse(
        String type, String subject, BigDecimal amount, BigDecimal reference, int count) {}
