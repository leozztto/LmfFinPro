package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientPersistenceMapper {

    public ClientJpaEntity toEntity(Client client) {
        return ClientJpaEntity.builder()
            .id(client.id())
            .user(UserJpaEntity.builder().id(client.userId()).build())
            .name(client.name())
            .email(client.email())
            .phone(client.phone())
            .documentType(client.documentType())
            .documentNumber(client.documentNumber())
            .workType(client.workType())
            .notes(client.notes())
            .color(client.color())
            .active(client.active())
            .build();
    }

    public Client toDomain(ClientJpaEntity entity) {
        return new Client(
            entity.getId(),
            entity.getUser().getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getDocumentType(),
            entity.getDocumentNumber(),
            entity.getWorkType(),
            entity.getNotes(),
            entity.getColor(),
            entity.getActive()
        );
    }
}
