package com.lmf.finpro.application.client;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnalytics;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Análise de clientes: histórico mês a mês, ranking, ticket médio e concentração de receita nos
 * últimos N meses (incluindo o atual). Transferências entre contas próprias ficam de fora, como no
 * Dashboard; por padrão vale a competência (pagas e pendentes), ou só o que já foi recebido.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAnalyticsApplicationService {

    public static final int MAX_MONTHS = 36;

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;

    /** Relatório com o nome de cada cliente do ranking já resolvido. */
    public record Result(ClientAnalytics.Report report, Map<Long, Client> clientsById) {}

    public Result analyze(Long currentHouseholdId, int monthsCount, boolean onlyReceived) {
        log.debug(
                "Analisando clientes do usuário={} meses={} apenasRecebido={}",
                currentHouseholdId,
                monthsCount,
                onlyReceived);
        if (monthsCount < 1 || monthsCount > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "O período deve ter de 1 a " + MAX_MONTHS + " meses");
        }
        List<YearMonth> months = lastMonths(monthsCount);

        List<Long> accountIds =
                accountRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> transactions =
                accountIds.isEmpty()
                        ? List.of()
                        : transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                                .filter(transaction -> transaction.transferId() == null)
                                .filter(transaction -> !onlyReceived || transaction.isPaid())
                                .toList();

        Map<Long, Client> clientsById =
                clientRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Client::id, client -> client));
        return new Result(ClientAnalytics.analyze(transactions, months), clientsById);
    }

    private List<YearMonth> lastMonths(int count) {
        YearMonth current = YearMonth.from(LocalDate.now(clock));
        List<YearMonth> months = new ArrayList<>();
        for (int offset = count - 1; offset >= 0; offset--) {
            months.add(current.minusMonths(offset));
        }
        return months;
    }
}
