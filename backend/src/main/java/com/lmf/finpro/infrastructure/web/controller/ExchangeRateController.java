package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.infrastructure.web.dto.exchangerate.ConversionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Conversão pela PTAX do dia, para as telas sugerirem o valor em outra moeda. */
@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateApplicationService exchangeRateApplicationService;

    @GetMapping("/convert")
    public ConversionResponse convert(
            @RequestParam Currency from,
            @RequestParam Currency to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal amount) {
        ExchangeRateApplicationService.Conversion conversion =
                exchangeRateApplicationService.convert(from, to, amount, date);
        return new ConversionResponse(from, to, date, conversion.rate(), conversion.amount());
    }
}
