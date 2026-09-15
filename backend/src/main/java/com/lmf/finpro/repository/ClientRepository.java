package com.lmf.finpro.repository;

import com.lmf.finpro.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByUserId(Long userId);
}
