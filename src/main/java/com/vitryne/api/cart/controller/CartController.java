package com.vitryne.api.cart.controller;

import com.vitryne.api.cart.dto.AddItemRequestDTO;
import com.vitryne.api.cart.dto.UpdateItemRequestDTO;
import com.vitryne.api.cart.dto.CartResponseDTO;
import com.vitryne.api.cart.service.CartService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/cart")
@PreAuthorize("hasRole('CONSUMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDTO> searchByUser(@AuthenticationPrincipal Jwt jwt) {
        CartResponseDTO response = cartService.searchByUser(AuthenticatedUser.id(jwt));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestBody @Valid AddItemRequestDTO request) {
        CartResponseDTO response = cartService.addItem(AuthenticatedUser.id(jwt), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long itemId,
                                                                @RequestBody @Valid UpdateItemRequestDTO request) {
        CartResponseDTO response = cartService.updateItemQuantity(AuthenticatedUser.id(jwt), itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> removeItem(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Long itemId) {
        CartResponseDTO response = cartService.removeItem(AuthenticatedUser.id(jwt), itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items")
    public ResponseEntity<CartResponseDTO> clear(@AuthenticationPrincipal Jwt jwt) {
        CartResponseDTO response = cartService.clear(AuthenticatedUser.id(jwt));
        return ResponseEntity.ok(response);
    }
}
