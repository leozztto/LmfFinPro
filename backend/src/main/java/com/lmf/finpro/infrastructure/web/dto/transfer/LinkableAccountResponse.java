package com.lmf.finpro.infrastructure.web.dto.transfer;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Currency;

/**
 * Conta de outro espaço com a qual dá para transferir. Só o necessário para escolher a conta no
 * formulário: nenhum saldo ou lançamento dela sai daqui.
 *
 * @param householdName nome do espaço onde a conta está (o pessoal ou o grupo)
 */
public record LinkableAccountResponse(
        Long id, String name, AccountType type, Currency currency, String householdName) {}
