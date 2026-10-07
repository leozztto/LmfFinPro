package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Buscas com checagem de dono compartilhadas pelos montadores de dados de relatório. */
@Component
@RequiredArgsConstructor
class ReportLookups {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    Client findOwnedClientOrThrow(Long currentHouseholdId, Long clientId) {
        return clientRepositoryPort
                .findById(clientId)
                .filter(candidate -> candidate.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }

    Account findOwnedAccountOrThrow(Long currentHouseholdId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(candidate -> candidate.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    /**
     * O usuário que está gerando o relatório: é o emitente que aparece no cabeçalho do documento.
     */
    User findUserOrThrow(Long currentUserId) {
        return userRepositoryPort
                .findById(currentUserId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Usuário não encontrado: " + currentUserId));
    }
}
