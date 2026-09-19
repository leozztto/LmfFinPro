package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.ClientPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.ClientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository clientJpaRepository;
    private final ClientPersistenceMapper mapper;

    @Override
    public Client save(Client client) {
        return mapper.toDomain(clientJpaRepository.save(mapper.toEntity(client)));
    }

    @Override
    public Optional<Client> findById(Long id) {
        return clientJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Client> findAllByUserId(Long userId) {
        return clientJpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        clientJpaRepository.deleteById(id);
    }
}
