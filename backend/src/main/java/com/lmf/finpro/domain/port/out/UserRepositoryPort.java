package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    /**
     * Só os ids: quem percorre todos os usuários carrega cada um separadamente, para que um
     * registro com problema não impeça a leitura dos demais.
     */
    List<Long> findAllIds();

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    /** Consulta leve (só a coluna), chamada a cada requisição autenticada. */
    Optional<Integer> findSessionVersion(Long userId);
}
