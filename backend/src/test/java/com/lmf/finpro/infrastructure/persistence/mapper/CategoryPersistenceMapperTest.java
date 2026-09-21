package com.lmf.finpro.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.Test;

/**
 * Categorias globais (sem dono) só existem via inserção direta no banco — nenhum teste de
 * integração passa por esse caminho, então a lógica de mapeamento é coberta aqui isoladamente.
 */
class CategoryPersistenceMapperTest {

    private final CategoryPersistenceMapper mapper = new CategoryPersistenceMapper();

    @Test
    void toEntityLeavesUserNullForAGlobalCategory() {
        Category globalCategory =
                Category.create(null, "Moradia", CategoryType.EXPENSE, null, null);

        CategoryJpaEntity entity = mapper.toEntity(globalCategory);

        assertThat(entity.getUser()).isNull();
    }

    @Test
    void toEntitySetsAUserReferenceForAnOwnedCategory() {
        Category ownedCategory =
                Category.create(10L, "Consultoria", CategoryType.INCOME, null, null);

        CategoryJpaEntity entity = mapper.toEntity(ownedCategory);

        assertThat(entity.getUser()).isNotNull();
        assertThat(entity.getUser().getId()).isEqualTo(10L);
    }

    @Test
    void toDomainMapsANullUserToANullUserId() {
        CategoryJpaEntity entity =
                CategoryJpaEntity.builder()
                        .id(1L)
                        .user(null)
                        .name("Moradia")
                        .type(CategoryType.EXPENSE)
                        .build();

        Category domain = mapper.toDomain(entity);

        assertThat(domain.userId()).isNull();
        assertThat(domain.isGlobal()).isTrue();
    }

    @Test
    void toDomainMapsAnExistingUserToItsId() {
        CategoryJpaEntity entity =
                CategoryJpaEntity.builder()
                        .id(1L)
                        .user(UserJpaEntity.builder().id(10L).build())
                        .name("Consultoria")
                        .type(CategoryType.INCOME)
                        .build();

        Category domain = mapper.toDomain(entity);

        assertThat(domain.userId()).isEqualTo(10L);
        assertThat(domain.isGlobal()).isFalse();
    }
}
