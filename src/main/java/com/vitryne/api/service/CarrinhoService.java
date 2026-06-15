package com.vitryne.api.service;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.dto.ItemCarrinhoResponseDTO;
import com.vitryne.api.entity.Carrinho;
import com.vitryne.api.entity.ItemCarrinho;
import com.vitryne.api.exception.CarrinhoNaoEncontradoException;
import com.vitryne.api.exception.ItemCarrinhoNaoEncontradoException;
import com.vitryne.api.repository.CarrinhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;

    @Transactional(readOnly = true)
    public CarrinhoResponseDTO buscarPorUsuario(Long usuarioId) {
        return toResponseDTO(buscarCarrinho(usuarioId));
    }

    @Transactional
    public CarrinhoResponseDTO adicionarItem(Long usuarioId, AdicionarItemRequestDTO request) {
        validarQuantidade(request.quantidade());
        validarPreco(request.precoUnitario());

        Carrinho carrinho = obterOuCriar(usuarioId);

        ItemCarrinho existente = buscarItemPorEstoque(carrinho, request.estoqueId());
        if (existente != null) {
            existente.setQuantidade(existente.getQuantidade() + request.quantidade());
        } else {
            ItemCarrinho novo = ItemCarrinho.builder()
                    .carrinho(carrinho)
                    .estoqueId(request.estoqueId())
                    .quantidade(request.quantidade())
                    .precoUnitario(request.precoUnitario())
                    .build();
            carrinho.getItens().add(novo);
        }

        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO atualizarQuantidadeItem(Long usuarioId, Long itemId, Integer quantidade) {
        validarQuantidade(quantidade);

        Carrinho carrinho = buscarCarrinho(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        item.setQuantidade(quantidade);

        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(Long usuarioId, Long itemId) {
        Carrinho carrinho = buscarCarrinho(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        carrinho.getItens().remove(item);

        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO limpar(Long usuarioId) {
        Carrinho carrinho = buscarCarrinho(usuarioId);
        carrinho.getItens().clear();

        return persistir(carrinho);
    }


    private CarrinhoResponseDTO persistir(Carrinho carrinho) {
        carrinho.setPrevisaoValorTotal(calcularTotal(carrinho));
        carrinho.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(carrinhoRepository.save(carrinho));
    }

    private Double calcularTotal(Carrinho carrinho) {
        return carrinho.getItens().stream()
                .mapToDouble(this::calcularSubtotal)
                .sum();
    }

    private Double calcularSubtotal(ItemCarrinho item) {
        if (item.getPrecoUnitario() == null || item.getQuantidade() == null) {
            return 0.0;
        }
        return item.getPrecoUnitario() * item.getQuantidade();
    }

    private Carrinho obterOuCriar(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> carrinhoRepository.save(
                        Carrinho.builder()
                                .usuarioId(usuarioId)
                                .previsaoValorTotal(0.0)
                                .atualizadoEm(LocalDateTime.now())
                                .build()
                ));
    }

    private Carrinho buscarCarrinho(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new CarrinhoNaoEncontradoException(usuarioId));
    }

    private ItemCarrinho buscarItemPorId(Carrinho carrinho, Long itemId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemCarrinhoNaoEncontradoException(itemId));
    }

    private ItemCarrinho buscarItemPorEstoque(Carrinho carrinho, Long estoqueId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getEstoqueId().equals(estoqueId))
                .findFirst()
                .orElse(null);
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade inválida: " + quantidade);
        }
    }

    private void validarPreco(Double preco) {
        if (preco == null || preco < 0) {
            throw new IllegalArgumentException("Preço unitário inválido: " + preco);
        }
    }


    private CarrinhoResponseDTO toResponseDTO(Carrinho carrinho) {
        List<ItemCarrinhoResponseDTO> itens = carrinho.getItens().stream()
                .map(this::toItemResponseDTO)
                .toList();

        return CarrinhoResponseDTO.builder()
                .id(carrinho.getId())
                .usuarioId(carrinho.getUsuarioId())
                .previsaoValorTotal(carrinho.getPrevisaoValorTotal())
                .atualizadoEm(carrinho.getAtualizadoEm())
                .itens(itens)
                .build();
    }

    private ItemCarrinhoResponseDTO toItemResponseDTO(ItemCarrinho item) {
        return ItemCarrinhoResponseDTO.builder()
                .id(item.getId())
                .estoqueId(item.getEstoqueId())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .subtotal(calcularSubtotal(item))
                .build();
    }
}
