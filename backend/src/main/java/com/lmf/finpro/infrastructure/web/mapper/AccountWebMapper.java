package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountWebMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.id(), account.name(), account.type(), account.initialBalance(), account.createdAt()
        );
    }
}
