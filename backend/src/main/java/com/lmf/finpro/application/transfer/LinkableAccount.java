package com.lmf.finpro.application.transfer;

import com.lmf.finpro.domain.model.Account;

/** Conta de outro espaço (grupo ou pessoal) com a qual a pessoa pode transferir. */
public record LinkableAccount(Account account, String householdName) {}
