package com.vitryne.api.client.entity;

import com.vitryne.api.address.entity.Address;
import jakarta.persistence.*;
import lombok.*;
import com.vitryne.api.shared.entity.User;

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

    @Embedded
    private Address address;

    public Client(User user, String name, String cpf, Address address) {
        this.user = user;
        this.name = name;
        this.cpf = cpf;
        this.address = address;
    }
}