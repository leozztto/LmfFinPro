package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class AccountWebMapper {

    public AccountResponse toResponse(
            Account account,
            BigDecimal currentBalance,
            BigDecimal currentBalanceInBrl,
            boolean hasEntries,
            Long ownerUserId,
            String ownerName,
            boolean canUnshare) {
        return new AccountResponse(
                account.id(),
                account.name(),
                account.type(),
                account.initialBalance(),
                currentBalance,
                account.createdAt(),
                account.scope(),
                account.currency(),
                currentBalanceInBrl,
                hasEntries,
                ownerUserId,
                ownerName,
                canUnshare);
    }
}
