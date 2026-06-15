package com.vitryne.api.entity;

import com.vitryne.api.exception.EstoqueInsuficienteException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String descricao;

    private Double preco;

    @Column(name = "preco_promocional")
    private Double precoPromocional;

    private String tipo;

    private String cor;

    private Double avaliacao;

    private String status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fotos_urls", columnDefinition = "text[]")
    private List<String> fotosUrls;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Estoque> estoques;

    //relacionar produto com uma Loja futuramente


    public Double calcularPrecoFinal(){
        return (precoPromocional != null) ? precoPromocional : preco;
    }

    public void aplicarDesconto(Double percentual){
        if(percentual == null || percentual <= 0 || percentual >= 100){
            throw new IllegalArgumentException("Percentual de desconto inválido");
        }

        this.precoPromocional = this.preco * (1 - percentual / 100);
    }

    public void removerDesconto(){
        this.precoPromocional = null;
    }

    public Boolean verificarDisponibilidade(String tamanho){
        return estoques.stream().anyMatch(
                e -> e.getTamanho().equals(tamanho) && e.getQuantidade() > 0
        );
    }

    public void AtualizarEstoque(String tamanho, Integer quantidade){
        if(quantidade == null || quantidade < 0 ){
            throw new IllegalArgumentException("Quantidade invalida");
        }

        Estoque estoque = buscarEstoquePorTamanho(tamanho).orElseGet(() -> {
            Estoque novo = Estoque.builder().produto(this).tamanho(tamanho).quantidade(0).build();
            this.estoques.add(novo);
            return novo;
        });
        estoque.setQuantidade(quantidade);
    }

    public void darBaixaEstoque(String tamanho, Integer qtd) {
        if (qtd == null || qtd <= 0) {
            throw new IllegalArgumentException("Quantidade para baixa inválida: " + qtd);
        }

        Estoque estoque = buscarEstoquePorTamanho(tamanho)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tamanho não cadastrado para este produto: " + tamanho));

        if (estoque.getQuantidade() < qtd) {
            throw new EstoqueInsuficienteException(tamanho, estoque.getQuantidade(), qtd);
        }

        estoque.setQuantidade(estoque.getQuantidade() - qtd);
    }

    private java.util.Optional<Estoque> buscarEstoquePorTamanho(String tamanho){
        return estoques.stream().filter(e -> e.getTamanho().equals(tamanho)).findFirst();
    }

}
