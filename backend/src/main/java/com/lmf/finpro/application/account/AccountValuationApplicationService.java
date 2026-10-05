package com.lmf.finpro.application.account;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Valor de mercado das contas de investimento. Informar uma data que já tem valor substitui o
 * anterior; datas futuras são recusadas, porque o valor é sempre de algo que já aconteceu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValuationApplicationService {

    private final AccountApplicationService accountApplicationService;
    private final AccountValuationRepositoryPort accountValuationRepositoryPort;
    private final Clock clock;

    public List<AccountValuation> list(Long currentUserId, Long accountId) {
        log.debug(
                "Listando valores de mercado da conta={} do usuário={}", accountId, currentUserId);
        accountApplicationService.getById(currentUserId, accountId);
        return accountValuationRepositoryPort.findAllByAccountId(accountId);
    }

    public AccountValuation save(
            Long currentUserId, Long accountId, LocalDate valuationDate, BigDecimal value) {
        log.debug(
                "Salvando valor de mercado da conta={} na data={} do usuário={}",
                accountId,
                valuationDate,
                currentUserId);
        Account account = accountApplicationService.getById(currentUserId, accountId);
        if (account.type() != AccountType.INVESTMENT) {
            throw new IllegalArgumentException(
                    "Só contas de investimento têm valor de mercado. Mude o tipo da conta para Investimento.");
        }
        if (valuationDate.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A data do valor não pode ser futura");
        }

        AccountValuation saved =
                accountValuationRepositoryPort.save(
                        accountValuationRepositoryPort
                                .findByAccountIdAndDate(accountId, valuationDate)
                                .map(existing -> existing.withValue(value))
                                .orElseGet(
                                        () ->
                                                AccountValuation.create(
                                                        accountId, valuationDate, value)));
        log.debug("Valor de mercado={} salvo na conta={}", saved.id(), accountId);
        return saved;
    }

    public void delete(Long currentUserId, Long accountId, Long valuationId) {
        log.debug(
                "Removendo valor de mercado={} da conta={} do usuário={}",
                valuationId,
                accountId,
                currentUserId);
        accountApplicationService.getById(currentUserId, accountId);
        AccountValuation valuation =
                accountValuationRepositoryPort
                        .findById(valuationId)
                        .filter(existing -> existing.accountId().equals(accountId))
                        .orElseThrow(() -> new ResourceNotFoundException("Valor não encontrado"));
        accountValuationRepositoryPort.deleteById(valuation.id());
        log.debug("Valor de mercado={} removido da conta={}", valuation.id(), accountId);
    }
}
