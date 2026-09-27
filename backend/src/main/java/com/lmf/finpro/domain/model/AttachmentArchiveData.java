package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * Conteúdo do pacote de comprovantes do ano (para o IR / contador): cada anexo com o caminho que
 * terá dentro do ZIP e os dados da transação a que pertence, para o índice em CSV.
 *
 * @param path caminho dentro do ZIP, ex.: {@code 2026-03/2026-03-15_aluguel_42.pdf}
 */
public record AttachmentArchiveData(Year year, List<Entry> entries) {

    public record Entry(
            String path,
            String storageKey,
            LocalDate date,
            String description,
            CategoryType type,
            BigDecimal amount,
            TransactionStatus status,
            String accountName,
            String categoryName,
            String clientName,
            AttachmentDocumentType documentType,
            String originalFileName) {}
}
