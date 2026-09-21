package com.lmf.finpro.application.client;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientApplicationService {

    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    public Client create(
            Long currentUserId,
            String name,
            String email,
            String phone,
            DocumentType documentType,
            String documentNumber,
            ClientWorkType workType,
            String notes,
            String color,
            boolean active) {
        return clientRepositoryPort.save(
                Client.create(
                        currentUserId,
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

    public List<Client> list(Long currentUserId) {
        return clientRepositoryPort.findAllByUserId(currentUserId);
    }

    public Client getById(Long currentUserId, Long clientId) {
        return findOwnedOrThrow(currentUserId, clientId);
    }

    public Client update(
            Long currentUserId,
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
        Client existing = findOwnedOrThrow(currentUserId, clientId);
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

    public void delete(Long currentUserId, Long clientId) {
        findOwnedOrThrow(currentUserId, clientId);
        if (transactionRepositoryPort.existsByClientId(clientId)) {
            throw new EntityHasLinkedRecordsException(
                    "Este cliente possui transações vinculadas. Exclua-as ou desvincule-as antes de remover o cliente.");
        }
        clientRepositoryPort.deleteById(clientId);
    }

    /** Acesso a cliente de outro usuário é tratado como inexistente (404), não como 403. */
    private Client findOwnedOrThrow(Long currentUserId, Long clientId) {
        return clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }
}
