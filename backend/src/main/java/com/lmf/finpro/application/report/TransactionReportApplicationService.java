package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionReportData;
import com.lmf.finpro.domain.model.TransactionReportData.CategoryTotal;
import com.lmf.finpro.domain.model.TransactionReportData.Row;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Relatórios de receitas e de despesas com filtros opcionais. A busca vai direto ao banco com só os
 * filtros informados ({@link TransactionRepositoryPort#search}); aqui ficam a validação dos
 * filtros, a resolução de nomes e os totais. Transferências entre contas próprias ficam de fora,
 * como nos demais relatórios de receita/despesa.
 */
@Service
@RequiredArgsConstructor
public class TransactionReportApplicationService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ReceiptGeneratorPort receiptGeneratorPort;
    private final ReportCsvExporterPort reportCsvExporterPort;

    public byte[] generate(
            Long currentUserId,
            CategoryType type,
            TransactionReportFilters filters,
            ReportFormat format) {
        TransactionReportData data = buildData(currentUserId, type, filters);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportTransactionReport(data)
                : receiptGeneratorPort.generateTransactionReport(data);
    }

    TransactionReportData buildData(
            Long currentUserId, CategoryType type, TransactionReportFilters filters) {
        validate(filters);

        Map<Long, Account> accountById =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .collect(Collectors.toMap(Account::id, account -> account));
        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));
        Map<Long, String> clientNameById =
                clientRepositoryPort.findAllByUserId(currentUserId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));

        // Id de conta/categoria/cliente que não é do usuário: 404, em vez de um relatório vazio.
        if (filters.accountId() != null && !accountById.containsKey(filters.accountId())) {
            throw new ResourceNotFoundException("Conta não encontrada: " + filters.accountId());
        }
        if (filters.categoryId() != null && !categoryNameById.containsKey(filters.categoryId())) {
            throw new ResourceNotFoundException(
                    "Categoria não encontrada: " + filters.categoryId());
        }
        if (filters.clientId() != null && !clientNameById.containsKey(filters.clientId())) {
            throw new ResourceNotFoundException("Cliente não encontrado: " + filters.clientId());
        }

        List<Transaction> transactions =
                transactionRepositoryPort.search(
                        new TransactionSearchCriteria(
                                currentUserId,
                                type,
                                filters.startDate(),
                                filters.endDate(),
                                filters.accountId(),
                                filters.accountScope(),
                                filters.categoryId(),
                                filters.clientId(),
                                filters.status(),
                                filters.minAmount(),
                                filters.maxAmount(),
                                filters.description(),
                                true));

        List<Row> rows =
                transactions.stream()
                        .map(
                                transaction ->
                                        new Row(
                                                transaction.transactionDate(),
                                                transaction.description(),
                                                accountName(accountById, transaction.accountId()),
                                                categoryName(
                                                        categoryNameById, transaction.categoryId()),
                                                transaction.clientId() == null
                                                        ? ""
                                                        : clientNameById.getOrDefault(
                                                                transaction.clientId(),
                                                                "Cliente removido"),
                                                transaction.status(),
                                                transaction.amount()))
                        .toList();

        return new TransactionReportData(
                type,
                periodLabel(filters),
                describeFilters(filters, accountById, categoryNameById, clientNameById),
                rows,
                sum(rows, null),
                sum(rows, TransactionStatus.PAID),
                sum(rows, TransactionStatus.PENDING),
                categoryTotals(rows));
    }

    private void validate(TransactionReportFilters filters) {
        if (filters.startDate() != null
                && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new IllegalArgumentException(
                    "A data inicial deve ser anterior ou igual à data final");
        }
        if ((filters.minAmount() != null && filters.minAmount().signum() < 0)
                || (filters.maxAmount() != null && filters.maxAmount().signum() < 0)) {
            throw new IllegalArgumentException(
                    "Os valores mínimo e máximo não podem ser negativos");
        }
        if (filters.minAmount() != null
                && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new IllegalArgumentException(
                    "O valor mínimo deve ser menor ou igual ao valor máximo");
        }
    }

    private static String periodLabel(TransactionReportFilters filters) {
        LocalDate start = filters.startDate();
        LocalDate end = filters.endDate();
        if (start != null && end != null) {
            return start.format(DATE_FORMAT) + " a " + end.format(DATE_FORMAT);
        }
        if (start != null) {
            return "A partir de " + start.format(DATE_FORMAT);
        }
        if (end != null) {
            return "Até " + end.format(DATE_FORMAT);
        }
        return "Todo o período";
    }

    /** Uma linha legível por filtro informado; o período já vai no cabeçalho. */
    private static List<String> describeFilters(
            TransactionReportFilters filters,
            Map<Long, Account> accountById,
            Map<Long, String> categoryNameById,
            Map<Long, String> clientNameById) {
        List<String> labels = new ArrayList<>();
        if (filters.accountId() != null) {
            labels.add("Conta: " + accountById.get(filters.accountId()).name());
        }
        if (filters.accountScope() != null) {
            labels.add(
                    "Uso da conta: "
                            + (filters.accountScope() == AccountScope.BUSINESS
                                    ? "Empresa (PJ)"
                                    : "Pessoal (PF)"));
        }
        if (filters.categoryId() != null) {
            labels.add("Categoria: " + categoryNameById.get(filters.categoryId()));
        }
        if (filters.clientId() != null) {
            labels.add("Cliente: " + clientNameById.get(filters.clientId()));
        }
        if (filters.status() != null) {
            labels.add(
                    "Situação: "
                            + (filters.status() == TransactionStatus.PAID ? "Paga" : "Pendente"));
        }
        if (filters.minAmount() != null) {
            labels.add("Valor mínimo: " + money(filters.minAmount()));
        }
        if (filters.maxAmount() != null) {
            labels.add("Valor máximo: " + money(filters.maxAmount()));
        }
        if (filters.description() != null && !filters.description().isBlank()) {
            labels.add("Descrição contém: \"" + filters.description().trim() + "\"");
        }
        return labels;
    }

    /** Subtotal por categoria, do maior para o menor. */
    private static List<CategoryTotal> categoryTotals(List<Row> rows) {
        Map<String, List<Row>> byCategory =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Row::categoryName,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        return byCategory.entrySet().stream()
                .map(
                        entry ->
                                new CategoryTotal(
                                        entry.getKey(),
                                        entry.getValue().size(),
                                        entry.getValue().stream()
                                                .map(Row::amount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                .toList();
    }

    private static BigDecimal sum(List<Row> rows, TransactionStatus status) {
        return rows.stream()
                .filter(row -> status == null || row.status() == status)
                .map(Row::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String accountName(Map<Long, Account> accountById, Long accountId) {
        Account account = accountById.get(accountId);
        return account == null ? "Conta removida" : account.name();
    }

    private static String categoryName(Map<Long, String> categoryNameById, Long categoryId) {
        return categoryId == null
                ? "Sem categoria"
                : categoryNameById.getOrDefault(categoryId, "Categoria removida");
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
