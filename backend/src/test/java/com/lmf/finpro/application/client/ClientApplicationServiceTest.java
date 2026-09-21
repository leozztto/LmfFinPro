package com.lmf.finpro.application.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientApplicationServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks private ClientApplicationService service;

    private static Client existingClient() {
        return Client.create(
                10L,
                "Empresa X",
                "contato@empresax.com",
                "11987654321",
                DocumentType.CNPJ,
                "11444777000161",
                ClientWorkType.PJ,
                null,
                null,
                true);
    }

    @Test
    void createSavesClientBuiltFromInput() {
        when(clientRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Client created =
                service.create(
                        10L,
                        "Empresa X",
                        "contato@empresax.com",
                        "11987654321",
                        DocumentType.CNPJ,
                        "11444777000161",
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);

        assertThat(created.userId()).isEqualTo(10L);
        assertThat(created.name()).isEqualTo("Empresa X");
        assertThat(created.active()).isTrue();
    }

    @Test
    void listReturnsAllClientsForUser() {
        Client client = existingClient();
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(client));

        assertThat(service.list(10L)).containsExactly(client);
    }

    @Test
    void getByIdThrowsWhenClientBelongsToAnotherUser() {
        Client client =
                new Client(1L, 10L, "Empresa X", null, null, null, null, null, null, null, true);
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.getById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReplacesAllEditableFields() {
        Client existing =
                new Client(
                        1L,
                        10L,
                        "Antigo",
                        "old@x.com",
                        "11900000000",
                        DocumentType.CPF,
                        "52998224725",
                        ClientWorkType.AUTONOMO,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Client updated =
                service.update(
                        10L,
                        1L,
                        "Novo",
                        "new@x.com",
                        "11988887777",
                        DocumentType.CPF,
                        "52998224725",
                        ClientWorkType.AUTONOMO,
                        "obs",
                        "#fff",
                        false);

        assertThat(updated.name()).isEqualTo("Novo");
        assertThat(updated.email()).isEqualTo("new@x.com");
        assertThat(updated.active()).isFalse();
    }

    @Test
    void deleteRemovesClientWhenNoLinkedTransactions() {
        Client client = existingClient();
        Client withId =
                new Client(
                        1L,
                        client.userId(),
                        client.name(),
                        client.email(),
                        client.phone(),
                        client.documentType(),
                        client.documentNumber(),
                        client.workType(),
                        client.notes(),
                        client.color(),
                        client.active());
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(withId));
        when(transactionRepositoryPort.existsByClientId(1L)).thenReturn(false);

        service.delete(10L, 1L);

        verify(clientRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenClientHasLinkedTransactions() {
        Client client = existingClient();
        Client withId =
                new Client(
                        1L,
                        client.userId(),
                        client.name(),
                        client.email(),
                        client.phone(),
                        client.documentType(),
                        client.documentNumber(),
                        client.workType(),
                        client.notes(),
                        client.color(),
                        client.active());
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(withId));
        when(transactionRepositoryPort.existsByClientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(clientRepositoryPort, never()).deleteById(any());
    }
}
