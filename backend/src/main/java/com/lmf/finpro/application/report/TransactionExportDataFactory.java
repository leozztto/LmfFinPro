package com.lmf.finpro.application.report;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados do extrato bruto de transações do mês. */
@Component
@RequiredArgsConstructor
class TransactionExportDataFactory {

    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TagApplicationService tagApplicationService;

    /**
     * Transações do usuário (em todas as contas) dentro do mês, ordenadas por data — ao contrário
     * dos relatórios agregados, inclui transferências, já que o objetivo aqui é auditar o extrato
     * completo, não somar receita/despesa.
     */
    TransactionExportData build(Long currentHouseholdId, YearMonth referenceMonth) {
        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Account> accounts = accountRepositoryPort.findAllByHouseholdId(currentHouseholdId);
        Map<Long, String> accountNameById =
                accounts.stream().collect(Collectors.toMap(Account::id, Account::name));
        Map<Long, Currency> accountCurrencyById =
                accounts.stream().collect(Collectors.toMap(Account::id, Account::currency));
        List<Long> accountIds = accounts.stream().map(Account::id).toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));
        Map<Long, String> clientNameById =
                clientRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));

        List<Transaction> transactionsInMonth =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(
                                transaction ->
                                        !transaction.transactionDate().isBefore(start)
                                                && transaction.transactionDate().isBefore(end))
                        .sorted(Comparator.comparing(Transaction::transactionDate))
                        .toList();
        Map<Long, List<Tag>> tagsByTransaction =
                tagApplicationService.tagsByTransactionIds(
                        currentHouseholdId,
                        transactionsInMonth.stream().map(Transaction::id).toList());

        List<TransactionExportData.TransactionExportRow> rows =
                transactionsInMonth.stream()
                        .map(
                                transaction ->
                                        new TransactionExportData.TransactionExportRow(
                                                transaction.transactionDate(),
                                                accountNameById.getOrDefault(
                                                        transaction.accountId(), "Conta removida"),
                                                transaction.categoryId() == null
                                                        ? "Sem categoria"
                                                        : categoryNameById.getOrDefault(
                                                                transaction.categoryId(),
                                                                "Categoria removida"),
                                                transaction.clientId() == null
                                                        ? ""
                                                        : clientNameById.getOrDefault(
                                                                transaction.clientId(),
                                                                "Cliente removido"),
                                                transaction.description(),
                                                transaction.type(),
                                                transaction.status(),
                                                transaction.baseAmount(),
                                                Tag.joinLabels(
                                                        tagsByTransaction.getOrDefault(
                                                                transaction.id(), List.of())),
                                                transaction.foreignValue(
                                                        accountCurrencyById.getOrDefault(
                                                                transaction.accountId(),
                                                                Currency.BRL))))
                        .toList();

        return new TransactionExportData(referenceMonth, rows);
    }
}
