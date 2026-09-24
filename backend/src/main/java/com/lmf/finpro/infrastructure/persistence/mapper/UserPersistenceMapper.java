package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.infrastructure.persistence.entity.AddressEmbeddable;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        return UserJpaEntity.builder()
                .id(user.id())
                .name(user.name())
                .email(user.email())
                .passwordHash(user.passwordHash())
                .documentType(user.documentType())
                .documentNumber(user.documentNumber())
                .phone(user.phone())
                .taxRegime(user.taxRegime())
                .address(toEmbeddable(user.address()))
                .createdAt(user.createdAt())
                .sessionVersion(user.sessionVersion())
                .build();
    }

    public User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getPhone(),
                entity.getTaxRegime(),
                toDomain(entity.getAddress()),
                entity.getCreatedAt(),
                entity.getSessionVersion());
    }

    private AddressEmbeddable toEmbeddable(Address address) {
        if (address == null) {
            return null;
        }
        return AddressEmbeddable.builder()
                .zipCode(address.zipCode())
                .street(address.street())
                .number(address.number())
                .complement(address.complement())
                .neighborhood(address.neighborhood())
                .city(address.city())
                .state(address.state())
                .build();
    }

    private Address toDomain(AddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Address(
                embeddable.getZipCode(),
                embeddable.getStreet(),
                embeddable.getNumber(),
                embeddable.getComplement(),
                embeddable.getNeighborhood(),
                embeddable.getCity(),
                embeddable.getState());
    }
}
