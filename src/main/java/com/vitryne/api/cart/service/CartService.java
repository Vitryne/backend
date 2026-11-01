package com.vitryne.api.cart.service;

import com.vitryne.api.cart.dto.AddItemRequestDTO;
import com.vitryne.api.cart.dto.CartItemResponseDTO;
import com.vitryne.api.cart.dto.CartResponseDTO;
import com.vitryne.api.cart.dto.UpdateItemRequestDTO;
import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.entity.CartItem;
import com.vitryne.api.cart.repository.CartRepository;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.shared.exception.CartItemNotFoundException;
import com.vitryne.api.shared.exception.CartNotFoundException;
import com.vitryne.api.shared.exception.InvalidQuantityException;
import com.vitryne.api.shared.exception.StockNotFoundException;
import com.vitryne.api.shared.exception.StockUnavailableException;
import com.vitryne.api.shared.exception.UnavailableQuantityException;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CartService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final CartRepository cartRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public CartResponseDTO searchByUser(Long userId) {
        return toResponseDTO(searchCartByUserId(userId));
    }

    @Transactional
    public CartResponseDTO addItem(Long userId, AddItemRequestDTO request) {
        validateQuantity(request.quantity());

        Stock stock = findStockById(request.stockId());
        validatePurchasable(stock);

        Cart cart = getOrCreate(userId);
        validateSameStore(cart, stock);

        Optional<CartItem> existingItem = findItemByStockId(cart, stock.getId());

        int currentQuantity = existingItem
                .map(CartItem::getQuantity)
                .orElse(0);

        long requestedTotal = (long) currentQuantity + request.quantity();

        if (requestedTotal > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade total do item excede o limite permitido"
            );
        }

        int finalQuantity = (int) requestedTotal;
        validateAvailableQuantity(stock, finalQuantity);

        BigDecimal unitPrice = stock.getProduct().calculateFinalPrice();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(finalQuantity);
            item.setUnitPrice(unitPrice);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .stock(stock)
                    .quantity(finalQuantity)
                    .unitPrice(unitPrice)
                    .build();

            cart.getItems().add(item);
        }

        return persist(cart);
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(
            Long userId,
            Long itemId,
            UpdateItemRequestDTO request
    ) {
        validateQuantity(request.quantity());

        Cart cart = searchCartByUserId(userId);
        CartItem item = findItemById(cart, itemId);
        Stock stock = item.getStock();

        validatePurchasable(stock);
        validateAvailableQuantity(stock, request.quantity());

        item.setQuantity(request.quantity());
        item.setUnitPrice(stock.getProduct().calculateFinalPrice());

        return persist(cart);
    }

    @Transactional
    public CartResponseDTO removeItem(Long userId, Long itemId) {
        Cart cart = searchCartByUserId(userId);
        CartItem item = findItemById(cart, itemId);

        cart.getItems().remove(item);

        return persist(cart);
    }

    @Transactional
    public CartResponseDTO clear(Long userId) {
        Cart cart = searchCartByUserId(userId);
        cart.getItems().clear();

        return persist(cart);
    }

    private Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .totalValueForecast(ZERO_MONEY)
                        .build());
    }

    private Cart searchCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));
    }

    private Stock findStockById(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));
    }

    private CartItem findItemById(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    private Optional<CartItem> findItemByStockId(Cart cart, Long stockId) {
        return cart.getItems().stream()
                .filter(item -> stockId.equals(item.getStock().getId()))
                .findFirst();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }

    private void validatePurchasable(Stock stock) {
        if (!"ACTIVE".equals(stock.getProduct().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O produto não está mais ativo"
            );
        }

        if (!stock.isAvailable()) {
            throw new StockUnavailableException(stock.getSize());
        }
    }

    private void validateAvailableQuantity(Stock stock, int requestedQuantity) {
        if (requestedQuantity > stock.getQuantity()) {
            throw new UnavailableQuantityException(
                    stock.getSize(),
                    stock.getQuantity(),
                    requestedQuantity
            );
        }
    }

    private void validateSameStore(Cart cart, Stock stock) {
        Long storeId = stock.getProduct().getStore().getId();

        boolean hasItemFromAnotherStore = cart.getItems().stream()
                .map(item -> item.getStock().getProduct().getStore().getId())
                .anyMatch(existingStoreId -> !storeId.equals(existingStoreId));

        if (hasItemFromAnotherStore) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O carrinho já contém produtos de outra loja"
            );
        }
    }

    private BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::subtotal)
                .reduce(ZERO_MONEY, BigDecimal::add);
    }

    private CartResponseDTO persist(Cart cart) {
        BigDecimal total = calculateTotal(cart);

        if (total.precision() - total.scale() > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor total do carrinho excede o limite permitido"
            );
        }

        cart.setTotalValueForecast(total);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.saveAndFlush(cart);
        return toResponseDTO(savedCart);
    }

    private CartResponseDTO toResponseDTO(Cart cart) {
        List<CartItemResponseDTO> items = cart.getItems().stream()
                .map(this::toItemResponseDTO)
                .toList();

        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .totalValueForecast(cart.getTotalValueForecast())
                .updatedAt(cart.getUpdatedAt())
                .items(items)
                .build();
    }

    private CartItemResponseDTO toItemResponseDTO(CartItem item) {
        Stock stock = item.getStock();
        Product product = stock.getProduct();
        List<String> photos = product.getPhotoUrls();

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .stockId(stock.getId())
                .productId(product.getId())
                .productName(product.getName())
                .photoUrl(photos == null || photos.isEmpty() ? null : photos.getFirst())
                .size(stock.getSize())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.subtotal())
                .build();
    }
}