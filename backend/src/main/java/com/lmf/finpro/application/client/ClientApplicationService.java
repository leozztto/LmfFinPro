package com.lmf.finpro.application.client;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientApplicationService {

    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;

    public Client create(
            Long currentHouseholdId,
            String name,
            String email,
            String phone,
            DocumentType documentType,
            String documentNumber,
            ClientWorkType workType,
            String notes,
            String color,
            boolean active) {
        log.debug("Criando cliente para o usuário={}", currentHouseholdId);
        Client saved =
                clientRepositoryPort.save(
                        Client.create(
                                currentHouseholdId,
                                name,
                                email,
                                phone,
                                documentType,
                                documentNumber,
                                workType,
                                notes,
                                color,
                                active));
        log.debug("Cliente={} criado para o usuário={}", saved.id(), currentHouseholdId);
        return saved;
    }

    public List<Client> list(Long currentHouseholdId) {
        log.debug("Listando clientes do usuário={}", currentHouseholdId);
        return clientRepositoryPort.findAllByHouseholdId(currentHouseholdId);
    }

    public Client getById(Long currentHouseholdId, Long clientId) {
        log.debug("Buscando cliente={} do usuário={}", clientId, currentHouseholdId);
        return findOwnedOrThrow(currentHouseholdId, clientId);
    }

    public Client update(
            Long currentHouseholdId,
            Long clientId,
            String name,
            String email,
            String phone,
            DocumentType documentType,
            String documentNumber,
            ClientWorkType workType,
            String notes,
            String color,
            boolean active) {
        log.debug("Atualizando cliente={} do usuário={}", clientId, currentHouseholdId);
        Client existing = findOwnedOrThrow(currentHouseholdId, clientId);
        return clientRepositoryPort.save(
                existing.withDetails(
                        name,
                        email,
                        phone,
                        documentType,
                        documentNumber,
                        workType,
                        notes,
                        color,
                        active));
    }

    public void delete(Long currentHouseholdId, Long clientId) {
        log.debug("Removendo cliente={} do usuário={}", clientId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, clientId);
        if (transactionRepositoryPort.existsByClientId(clientId)) {
            throw new EntityHasLinkedRecordsException(
                    "Este cliente possui transações vinculadas. Exclua-as ou desvincule-as antes de"
                            + " remover o cliente.");
        }
        // budgets.client_id é ON DELETE CASCADE: sem essa checagem, excluir o cliente apagaria
        // orçamentos existentes em silêncio.
        if (budgetRepositoryPort.existsByClientId(clientId)) {
            throw new EntityHasLinkedRecordsException(
                    "Este cliente possui orçamentos vinculados. Exclua-os antes de remover o"
                            + " cliente.");
        }
        clientRepositoryPort.deleteById(clientId);
    }

    /** Acesso a cliente de outro usuário é tratado como inexistente (404), não como 403. */
    private Client findOwnedOrThrow(Long currentHouseholdId, Long clientId) {
        return clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }
}
