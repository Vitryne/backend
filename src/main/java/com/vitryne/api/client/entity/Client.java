package com.vitryne.api.client.entity;

import com.vitryne.api.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    /**
     * Endereço de entrega escolhido pelo cliente (navegação e checkout).
     * Os endereços ficam em client_address; as antigas colunas de endereço
     * desta tabela não são mais usadas.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_address_id")
    private ClientAddress selectedAddress;

    public Client(User user, String name, String cpf) {
        this.user = user;
        this.name = name;
        this.cpf = cpf;
    }

    public void selectAddress(ClientAddress address) {
        if (address == null || address.getClient() != this) {
            throw new IllegalArgumentException("O endereço não pertence a este cliente");
        }

        this.selectedAddress = address;
    }
}
