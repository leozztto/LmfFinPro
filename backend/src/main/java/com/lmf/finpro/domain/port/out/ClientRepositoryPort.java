package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Client;
import java.util.List;
import java.util.Optional;

public interface ClientRepositoryPort {
    Client save(Client client);

    Optional<Client> findById(Long id);

    List<Client> findAllByUserId(Long userId);

    void deleteById(Long id);
}
