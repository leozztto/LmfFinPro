package com.lmf.finpro.application.networth;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Debt;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import com.lmf.finpro.domain.port.out.DebtBalanceRepositoryPort;
import com.lmf.finpro.domain.port.out.DebtRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Patrimônio líquido (contas + investimentos − dívidas) hoje e no fim de cada um dos últimos N
 * meses. Transferências entram, porque movem dinheiro entre contas (e para os investimentos); no
 * total elas se anulam.
 */
@Service
@RequiredArgsConstructor
public class NetWorthApplicationService {

    public static final int MAX_MONTHS = 60;

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountValuationRepositoryPort accountValuationRepositoryPort;
    private final DebtRepositoryPort debtRepositoryPort;
    private final DebtBalanceRepositoryPort debtBalanceRepositoryPort;
    private final Clock clock;

    public NetWorthCalculator.Report summary(Long currentUserId, int monthsCount) {
        if (monthsCount < 1 || monthsCount > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "O período deve ter de 1 a " + MAX_MONTHS + " meses");
        }
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        List<YearMonth> months = new ArrayList<>();
        for (int offset = monthsCount - 1; offset >= 0; offset--) {
            months.add(currentMonth.minusMonths(offset));
        }

        List<Account> accounts = accountRepositoryPort.findAllByUserId(currentUserId);
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        List<Debt> debts = debtRepositoryPort.findAllByUserId(currentUserId);
        return NetWorthCalculator.calculate(
                accounts,
                accountIds.isEmpty()
                        ? List.of()
                        : transactionRepositoryPort.findAllByAccountIds(accountIds),
                accountValuationRepositoryPort.findAllByAccountIds(accountIds),
                debts,
                debtBalanceRepositoryPort.findAllByDebtIds(debts.stream().map(Debt::id).toList()),
                months,
                currentMonth);
    }
}
