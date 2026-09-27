package com.lmf.finpro.infrastructure.client.bcb;

import java.math.BigDecimal;
import java.util.List;

/** Resposta OData de {@code CotacaoMoedaPeriodo}: um boletim por linha. */
public record BcbPtaxResponseDto(List<Quote> value) {

    /**
     * @param dataHoraCotacao ex.: {@code 2026-09-18 13:03:34.742036}
     * @param tipoBoletim "Abertura", "Intermediário" ou "Fechamento"
     */
    public record Quote(BigDecimal cotacaoVenda, String dataHoraCotacao, String tipoBoletim) {}
}
