package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import org.springframework.stereotype.Component;

@Component
public class ClientWebMapper {

    public ClientResponse toResponse(Client client) {
        return new ClientResponse(
            client.id(), client.name(), client.email(), client.phone(),
            client.documentType(), client.documentNumber(), client.workType(),
            client.notes(), client.color(), client.active()
        );
    }
}
