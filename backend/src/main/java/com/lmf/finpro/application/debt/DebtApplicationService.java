package com.lmf.finpro.application.debt;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Debt;
import com.lmf.finpro.domain.model.DebtBalance;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.domain.port.out.DebtBalanceRepositoryPort;
import com.lmf.finpro.domain.port.out.DebtRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dívidas acompanhadas no patrimônio. O saldo devedor é sempre o último informado; toda dívida
 * nasce com um e precisa manter pelo menos um. Datas futuras são recusadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebtApplicationService {

    private final DebtRepositoryPort debtRepositoryPort;
    private final DebtBalanceRepositoryPort debtBalanceRepositoryPort;
    private final Clock clock;

    /** Dívida com o último saldo devedor informado ({@code null} só se não houver nenhum). */
    public record DebtSummary(Debt debt, DebtBalance lastBalance) {
        public BigDecimal currentBalance() {
            return lastBalance == null ? BigDecimal.ZERO : lastBalance.balance();
        }
    }

    public List<DebtSummary> list(Long currentUserId) {
        log.debug("Listando dívidas do usuário={}", currentUserId);
        List<Debt> debts = debtRepositoryPort.findAllByUserId(currentUserId);
        Map<Long, List<DebtBalance>> balancesByDebt =
                debtBalanceRepositoryPort
                        .findAllByDebtIds(debts.stream().map(Debt::id).toList())
                        .stream()
                        .collect(Collectors.groupingBy(DebtBalance::debtId));
        return debts.stream()
                .map(
                        debt ->
                                new DebtSummary(
                                        debt,
                                        NetWorthCalculator.latestBalance(
                                                        balancesByDebt.getOrDefault(
                                                                debt.id(), List.of()),
                                                        null)
                                                .orElse(null)))
                .toList();
    }

    @Transactional
    public DebtSummary create(
            Long currentUserId,
            String name,
            DebtType type,
            String creditor,
            BigDecimal initialBalance,
            LocalDate balanceDate) {
        log.debug("Criando dívida do tipo={} para o usuário={}", type, currentUserId);
        requireNotFuture(balanceDate);
        Debt debt =
                debtRepositoryPort.save(
                        Debt.create(currentUserId, name.trim(), type, blankToNull(creditor)));
        DebtBalance balance =
                debtBalanceRepositoryPort.save(
                        DebtBalance.create(debt.id(), balanceDate, initialBalance));
        log.debug("Dívida={} criada para o usuário={}", debt.id(), currentUserId);
        return new DebtSummary(debt, balance);
    }

    public DebtSummary update(
            Long currentUserId, Long debtId, String name, DebtType type, String creditor) {
        log.debug("Atualizando dívida={} do usuário={}", debtId, currentUserId);
        Debt debt = findOwnedOrThrow(currentUserId, debtId);
        Debt updated =
                debtRepositoryPort.save(debt.withDetails(name.trim(), type, blankToNull(creditor)));
        return new DebtSummary(updated, lastBalance(debtId));
    }

    /** Os saldos informados vão junto (ON DELETE CASCADE). */
    public void delete(Long currentUserId, Long debtId) {
        log.debug("Removendo dívida={} do usuário={}", debtId, currentUserId);
        findOwnedOrThrow(currentUserId, debtId);
        debtRepositoryPort.deleteById(debtId);
    }

    public List<DebtBalance> listBalances(Long currentUserId, Long debtId) {
        log.debug("Listando saldos da dívida={} do usuário={}", debtId, currentUserId);
        findOwnedOrThrow(currentUserId, debtId);
        return debtBalanceRepositoryPort.findAllByDebtId(debtId);
    }

    public DebtBalance saveBalance(
            Long currentUserId, Long debtId, LocalDate balanceDate, BigDecimal balance) {
        log.debug(
                "Salvando saldo da dívida={} na data={} do usuário={}",
                debtId,
                balanceDate,
                currentUserId);
        findOwnedOrThrow(currentUserId, debtId);
        requireNotFuture(balanceDate);
        return debtBalanceRepositoryPort.save(
                debtBalanceRepositoryPort
                        .findByDebtIdAndDate(debtId, balanceDate)
                        .map(existing -> existing.withBalance(balance))
                        .orElseGet(() -> DebtBalance.create(debtId, balanceDate, balance)));
    }

    public void deleteBalance(Long currentUserId, Long debtId, Long balanceId) {
        log.debug(
                "Removendo saldo={} da dívida={} do usuário={}", balanceId, debtId, currentUserId);
        findOwnedOrThrow(currentUserId, debtId);
        DebtBalance balance =
                debtBalanceRepositoryPort
                        .findById(balanceId)
                        .filter(existing -> existing.debtId().equals(debtId))
                        .orElseThrow(() -> new ResourceNotFoundException("Saldo não encontrado"));
        if (debtBalanceRepositoryPort.countByDebtId(debtId) <= 1) {
            throw new IllegalArgumentException(
                    "A dívida precisa de pelo menos um saldo devedor. Para quitá-la, informe saldo zero.");
        }
        debtBalanceRepositoryPort.deleteById(balance.id());
    }

    private DebtBalance lastBalance(Long debtId) {
        return NetWorthCalculator.latestBalance(
                        debtBalanceRepositoryPort.findAllByDebtId(debtId), null)
                .orElse(null);
    }

    private void requireNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A data do saldo não pode ser futura");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Dívida de outro usuário é tratada como inexistente (404), não como 403. */
    private Debt findOwnedOrThrow(Long currentUserId, Long debtId) {
        return debtRepositoryPort
                .findById(debtId)
                .filter(debt -> debt.belongsTo(currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Dívida não encontrada"));
    }
}
