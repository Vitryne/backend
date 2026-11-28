package com.vitryne.api.client.repository;

import com.vitryne.api.client.entity.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByCpf(String cpf);

    Optional<Client> findByUserId(Long userId);

    /** Perfil (/client/me): usuário e endereço selecionado na mesma consulta. */
    @EntityGraph(attributePaths = {"user", "selectedAddress"})
    Optional<Client> findProfileByUserId(Long userId);
}
