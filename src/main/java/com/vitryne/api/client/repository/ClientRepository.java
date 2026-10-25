package com.vitryne.api.client.repository;

import com.vitryne.api.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByCpf(String cpf);
}
