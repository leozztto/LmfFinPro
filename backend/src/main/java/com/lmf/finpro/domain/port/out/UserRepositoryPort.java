package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    /** Consulta leve (só a coluna), chamada a cada requisição autenticada. */
    Optional<Integer> findSessionVersion(Long userId);
}
