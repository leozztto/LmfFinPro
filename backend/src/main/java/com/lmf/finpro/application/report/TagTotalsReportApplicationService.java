package com.lmf.finpro.application.report;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.TagTotalsCalculator;
import com.lmf.finpro.domain.model.TagTotalsReportData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Relatório "Totais por tag": quanto entrou, saiu e sobrou em cada tag no período — ex.: o
 * resultado de um projeto marcado com {@code #site-acme}. Receitas e despesas por competência
 * (pagas e pendentes, a menos que filtre a situação); transferências entre contas próprias ficam de
 * fora, como nos outros relatórios de receita/despesa.
 */
@Service
@RequiredArgsConstructor
public class TagTotalsReportApplicationService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TagApplicationService tagApplicationService;
    private final ReceiptGeneratorPort receiptGeneratorPort;
    private final ReportCsvExporterPort reportCsvExporterPort;

    public byte[] generate(
            Long currentUserId, TagTotalsReportFilters filters, ReportFormat format) {
        TagTotalsReportData data = buildData(currentUserId, filters);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportTagTotalsReport(data)
                : receiptGeneratorPort.generateTagTotalsReport(data);
    }

    TagTotalsReportData buildData(Long currentUserId, TagTotalsReportFilters filters) {
        if (filters.startDate() != null
                && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new IllegalArgumentException(
                    "A data inicial deve ser anterior ou igual à data final");
        }
        Map<Long, Tag> tagById = tagApplicationService.tagsById(currentUserId);
        for (Long tagId : filters.tagIds()) {
            if (!tagById.containsKey(tagId)) {
                throw new ResourceNotFoundException("Tag não encontrada: " + tagId);
            }
        }
        boolean filteredByTag = !filters.tagIds().isEmpty();

        List<Transaction> transactions =
                transactionRepositoryPort.search(
                        new TransactionSearchCriteria(
                                currentUserId,
                                null,
                                filters.startDate(),
                                filters.endDate(),
                                null,
                                filters.accountScope(),
                                null,
                                null,
                                filters.status(),
                                null,
                                null,
                                null,
                                true,
                                filters.tagIds()));
        Map<Long, List<Long>> tagIdsByTransaction =
                tagApplicationService
                        .tagsByTransactionIds(
                                currentUserId, transactions.stream().map(Transaction::id).toList())
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().stream().map(Tag::id).toList()));

        Collection<Tag> tagsInReport =
                filteredByTag
                        ? filters.tagIds().stream().map(tagById::get).toList()
                        : tagById.values();
        return new TagTotalsReportData(
                TransactionReportApplicationService.periodLabel(
                        filters.startDate(), filters.endDate()),
                describeFilters(filters, tagById),
                TagTotalsCalculator.byTag(
                        transactions, tagIdsByTransaction, tagsInReport, filteredByTag),
                filteredByTag
                        ? null
                        : TagTotalsCalculator.untagged(transactions, tagIdsByTransaction));
    }

    private static List<String> describeFilters(
            TagTotalsReportFilters filters, Map<Long, Tag> tagById) {
        List<String> labels = new ArrayList<>();
        if (filters.accountScope() != null) {
            labels.add(
                    "Uso da conta: "
                            + (filters.accountScope() == AccountScope.BUSINESS
                                    ? "Empresa (PJ)"
                                    : "Pessoal (PF)"));
        }
        if (filters.status() != null) {
            labels.add(
                    "Situação: "
                            + (filters.status() == TransactionStatus.PAID
                                    ? "Pagas/recebidas"
                                    : "Pendentes"));
        }
        if (!filters.tagIds().isEmpty()) {
            labels.add(
                    "Tags: "
                            + Tag.joinLabels(filters.tagIds().stream().map(tagById::get).toList()));
        }
        return labels;
    }
}
