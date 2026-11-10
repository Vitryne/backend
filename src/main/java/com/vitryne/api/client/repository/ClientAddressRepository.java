package com.vitryne.api.client.repository;

import com.vitryne.api.client.entity.ClientAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientAddressRepository extends JpaRepository<ClientAddress, Long> {

    List<ClientAddress> findAllByClientUserIdOrderByIdAsc(Long userId);

    Optional<ClientAddress> findByIdAndClientUserId(Long id, Long userId);

    Optional<ClientAddress> findFirstByClientUserIdOrderByIdAsc(Long userId);
}
