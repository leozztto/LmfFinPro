package com.lmf.finpro.application.insight;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.Insight;
import com.lmf.finpro.domain.model.InsightDetector;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Insights do grupo ativo para a tela: os mesmos do resumo diário de alertas, mas sem o filtro de
 * "já avisado" — o que vale hoje, esteja ou não já no e-mail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final Clock clock;

    public List<Insight> list(Long currentHouseholdId) {
        log.debug("Detectando insights do usuário={}", currentHouseholdId);
        List<Long> accountIds =
                accountRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .map(Account::id)
                        .toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        List<Transaction> transactions = transactionRepositoryPort.findAllByAccountIds(accountIds);
        Map<Long, String> clientNames =
                clientRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));
        return InsightDetector.detect(
                transactions,
                LocalDate.now(clock),
                id -> categoryRepositoryPort.findById(id).map(Category::name).orElse("Categoria"),
                id -> clientNames.getOrDefault(id, "Cliente"));
    }
}
