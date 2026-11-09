package com.vitryne.api.client.repository;

import com.vitryne.api.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByCpf(String cpf);

    Optional<Client> findByUserId(Long userId);
}
