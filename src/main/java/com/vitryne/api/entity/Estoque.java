package com.vitryne.api.entity;

import com.vitryne.api.exception.EstoqueInsuficienteException;
import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "estoque")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tamanho;

    private Integer quantidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    public void diminuirEstoque(int quantidade){
        if(quantidade <= 0){
            throw new IllegalArgumentException("Quantidade invalida");
        }

        if(this.quantidade < quantidade){
            throw new EstoqueInsuficienteException(this.tamanho, this.quantidade, quantidade);
        }

        this.quantidade -= quantidade;
    }

    public void aumentarEstoque(int quantidade){
        if(quantidade <= 0){
            throw new IllegalArgumentException("Quantidade invalida");
        }

        this.quantidade += quantidade;
    }

    public Boolean estaDisponivel(){
        return this.quantidade > 0;
    }
}
