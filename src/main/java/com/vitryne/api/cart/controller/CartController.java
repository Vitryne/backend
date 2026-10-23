package com.vitryne.api.cart.controller;

import com.vitryne.api.cart.dto.AddItemRequestDTO;
import com.vitryne.api.cart.dto.UpdateItemRequestDTO;
import com.vitryne.api.cart.dto.CartResponseDTO;
import com.vitryne.api.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> searchByUser(@PathVariable Long userId) {
        CartResponseDTO response = cartService.searchByUser(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponseDTO> addItem(@PathVariable Long userId,
                                                     @RequestBody @Valid AddItemRequestDTO request) {
        CartResponseDTO response = cartService.addItem(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(@PathVariable Long userId,
                                                                @PathVariable Long itemId,
                                                                @RequestBody @Valid UpdateItemRequestDTO request) {
        CartResponseDTO response = cartService.updateItemQuantity(userId, itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponseDTO> removeItem(@PathVariable Long userId,
                                                        @PathVariable Long itemId) {
        CartResponseDTO response = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/items")
    public ResponseEntity<CartResponseDTO> clear(@PathVariable Long userId) {
        CartResponseDTO response = cartService.clear(userId);
        return ResponseEntity.ok(response);
    }
}
