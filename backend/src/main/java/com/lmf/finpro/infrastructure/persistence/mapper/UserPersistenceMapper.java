package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.User;
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
            .taxRegime(user.taxRegime())
            .createdAt(user.createdAt())
            .build();
    }

    public User toDomain(UserJpaEntity entity) {
        return new User(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getTaxRegime(),
            entity.getCreatedAt()
        );
    }
}
