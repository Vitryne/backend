package com.vitryne.api.controller;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.AtualizarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.service.CarrinhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<CarrinhoResponseDTO> buscarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(carrinhoService.buscarPorUsuario(usuarioId));
    }

    @PostMapping("/{usuarioId}/itens")
    public ResponseEntity<CarrinhoResponseDTO> adicionarItem(@PathVariable Long usuarioId,
                                                             @RequestBody AdicionarItemRequestDTO request) {
        return ResponseEntity.ok(carrinhoService.adicionarItem(usuarioId, request));
    }

    @PutMapping("/{usuarioId}/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> atualizarQuantidadeItem(@PathVariable Long usuarioId,
                                                                       @PathVariable Long itemId,
                                                                       @RequestBody AtualizarItemRequestDTO request) {
        return ResponseEntity.ok(carrinhoService.atualizarQuantidadeItem(usuarioId, itemId, request.quantidade()));
    }

    @DeleteMapping("/{usuarioId}/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> removerItem(@PathVariable Long usuarioId,
                                                           @PathVariable Long itemId) {
        return ResponseEntity.ok(carrinhoService.removerItem(usuarioId, itemId));
    }

    @DeleteMapping("/{usuarioId}/itens")
    public ResponseEntity<CarrinhoResponseDTO> limpar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(carrinhoService.limpar(usuarioId));
    }
}
