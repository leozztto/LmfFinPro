package com.lmf.finpro.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.infrastructure.persistence.entity.AddressEmbeddable;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * O endereço é obrigatório via API (@NotNull em RegisterRequest), mas o mapper trata a ausência
 * defensivamente — coberto aqui já que nenhum teste de integração passa por esse caminho.
 */
class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    private static Address sampleAddress() {
        return new Address(
                "01310100",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                BrazilianState.SP);
    }

    @Test
    void toEntityMapsANullAddressToANullEmbeddable() {
        User user =
                new User(
                        1L,
                        "Ana",
                        "ana@finpro.test",
                        "hash",
                        DocumentType.CPF,
                        "52998224725",
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        null,
                        LocalDateTime.now(),
                        0);

        UserJpaEntity entity = mapper.toEntity(user);

        assertThat(entity.getAddress()).isNull();
    }

    @Test
    void toEntityMapsANonNullAddress() {
        User user =
                new User(
                        1L,
                        "Ana",
                        "ana@finpro.test",
                        "hash",
                        DocumentType.CPF,
                        "52998224725",
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        sampleAddress(),
                        LocalDateTime.now(),
                        0);

        UserJpaEntity entity = mapper.toEntity(user);

        assertThat(entity.getAddress()).isNotNull();
        assertThat(entity.getAddress().getStreet()).isEqualTo("Avenida Paulista");
    }

    @Test
    void toDomainMapsANullEmbeddableToANullAddress() {
        UserJpaEntity entity =
                UserJpaEntity.builder()
                        .id(1L)
                        .name("Ana")
                        .email("ana@finpro.test")
                        .passwordHash("hash")
                        .documentType(DocumentType.CPF)
                        .documentNumber("52998224725")
                        .taxRegime(TaxRegime.AUTONOMO)
                        .address(null)
                        .createdAt(LocalDateTime.now())
                        .build();

        User domain = mapper.toDomain(entity);

        assertThat(domain.address()).isNull();
    }

    @Test
    void toDomainMapsANonNullEmbeddable() {
        AddressEmbeddable embeddable =
                AddressEmbeddable.builder()
                        .zipCode("01310100")
                        .street("Avenida Paulista")
                        .number("1000")
                        .neighborhood("Bela Vista")
                        .city("São Paulo")
                        .state(BrazilianState.SP)
                        .build();
        UserJpaEntity entity =
                UserJpaEntity.builder()
                        .id(1L)
                        .name("Ana")
                        .email("ana@finpro.test")
                        .passwordHash("hash")
                        .documentType(DocumentType.CPF)
                        .documentNumber("52998224725")
                        .taxRegime(TaxRegime.AUTONOMO)
                        .address(embeddable)
                        .createdAt(LocalDateTime.now())
                        .build();

        User domain = mapper.toDomain(entity);

        assertThat(domain.address()).isNotNull();
        assertThat(domain.address().street()).isEqualTo("Avenida Paulista");
    }
}
