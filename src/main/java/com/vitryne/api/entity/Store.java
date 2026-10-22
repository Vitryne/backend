package com.vitryne.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.catalina.User;

import static lombok.AccessLevel.*;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Store(User owner, String name, String description) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Store name is required");
        }

        this.owner = owner;
        this.name = name.trim();
        this.description = description;
        this.active = true;
    }
}