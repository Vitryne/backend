package com.vitryne.api.service;

import com.vitryne.api.dto.AddItemRequestDTO;
import com.vitryne.api.dto.UpdateItemRequestDTO;
import com.vitryne.api.dto.CartResponseDTO;
import com.vitryne.api.dto.CartItemResponseDTO;
import com.vitryne.api.entity.Cart;
import com.vitryne.api.entity.Stock;
import com.vitryne.api.entity.ItemCart;
import com.vitryne.api.exception.*;
import com.vitryne.api.repository.CartRepository;
import com.vitryne.api.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CartService {

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
        if (!stock.isAvailable()) {
            log.warn("Stock unavailable. Stock size: [{}]", stock.getSize());
            throw new StockUnavailableException(stock.getSize());
        }
        Cart cart = getOrCreate(userId);
        ItemCart existingItem = findItemByStockId(cart, request.stockId());
        Integer finalQuantity = (existingItem != null)
                ? existingItem.getQuantity() + request.quantity()
                : request.quantity();
        if (finalQuantity > stock.getQuantity()) {
            logInsufficientStockWarning(stock, finalQuantity);
            throw new UnavailableQuantityException(
                    stock.getSize(), stock.getQuantity(), finalQuantity);
        }
        Double unitPrice = stock.getProduct().calculateFinalPrice();
        if (existingItem != null) {
            existingItem.setQuantity(finalQuantity);
            existingItem.setUnitPrice(unitPrice);
        } else {
            ItemCart newItem = ItemCart.builder()
                    .cart(cart)
                    .stockId(request.stockId())
                    .quantity(request.quantity())
                    .unitPrice(unitPrice)
                    .build();
            cart.getItems().add(newItem);
            log.info("Item added to cart successfully.");
        }
        return persist(cart);
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(Long userId, Long itemId, UpdateItemRequestDTO request) {
        Integer quantity = request.quantity();
        validateQuantity(quantity);
        Cart cart = searchCartByUserId(userId);
        ItemCart item = findItemById(cart, itemId);
        Stock stock = findStockById(item.getStockId());
        if(quantity > stock.getQuantity()){
            logInsufficientStockWarning(stock, quantity);
            throw new UnavailableQuantityException(stock.getSize(), stock.getQuantity(), quantity);
        }
        item.setQuantity(quantity);
        log.info("Quantity updated successfully [{}]", quantity);
        return persist(cart);
    }

    @Transactional
    public CartResponseDTO removeItem(Long userId, Long itemId) {
        Cart cart = searchCartByUserId(userId);
        ItemCart item = findItemById(cart, itemId);
        cart.getItems().remove(item);
        log.info("Item removed successfully: [{}]", item.getId());
        return persist(cart);
    }

    @Transactional
    public CartResponseDTO clear(Long userId) {
        Cart cart = searchCartByUserId(userId);
        cart.getItems().clear();
        log.info("Cart cleared successfully: [{}]", cart.getId());
        return persist(cart);
    }


    private CartResponseDTO persist(Cart cart) {
        log.info("Starting cart persistence: [{}]", cart.getId());
        try{
            cart.setTotalValueForecast(calculateTotal(cart));
            cart.setUpdatedAt(LocalDateTime.now());
            Cart savedCart = cartRepository.save(cart);
            log.info("Cart persisted successfully: [{}]", savedCart.getId());
            return toResponseDTO(savedCart);
        } catch (Exception e) {
            log.error("Error persisting cart: [{}]",
                cart.getId(), e
            );
            throw e;
        }

    }

    private Double calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .mapToDouble(this::calculateSubtotal)
                .sum();
    }

    private Double calculateSubtotal(ItemCart item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return 0.0;
        }
        return item.getUnitPrice() * item.getQuantity();
    }

    private Cart getOrCreate(Long userId) {
        log.info("Getting cart for user: [{}]", userId);
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("No cart exists for user: [{}], creating a new one.", userId);
                    Cart cart = Cart.builder()
                            .userId(userId)
                            .totalValueForecast(0.0)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    Cart savedCart = cartRepository.save(cart);
                    log.info("Cart created. User: [{}], Cart: [{}]",
                            userId, cart.getId()
                    );
                    return savedCart;
                });
    }


    private ItemCart findItemById(Cart cart, Long itemId) {
        log.info("Finding item by id: [{}]", itemId);
        ItemCart cartItem = cart.getItems().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Error finding item by id: [{}], Cart: [{}]", itemId, cart.getId());
                    return new CartItemNotFoundException(itemId);
                });
        return cartItem;
    }

    private ItemCart findItemByStockId(Cart cart, Long stockId) {
        log.info("Finding item in cart by stockId: [{}]", stockId);
        ItemCart cartItem = cart.getItems().stream()
                .filter(i -> i.getStockId().equals(stockId))
                .findFirst()
                .orElse(null);
        if(cartItem == null){
            log.warn("Item not found in cart for stockId: [{}]", stockId);
        }
        return cartItem;
    }

    private void validateQuantity(Integer quantity) {
        log.info("Validating product quantity: [{}]", quantity);
        if (quantity == null || quantity <= 0) {
            log.warn("Invalid product quantity: [{}]", quantity);
            throw new InvalidQuantityException(quantity);
        }
        log.info("Product quantity is valid: [{}]", quantity);
    }

    private CartResponseDTO toResponseDTO(Cart cart) {
        List<ItemCart> cartItems = cart.getItems();
        List<Long> stockIds = cartItems.stream()
                .map(ItemCart::getStockId)
                .toList();
        Map<Long, Stock> stockById = stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, s -> s));
        List<CartItemResponseDTO> items = cartItems.stream()
                .map(item -> toItemResponseDTO(item, stockById.get(item.getStockId())))
                .toList();
        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .totalValueForecast(cart.getTotalValueForecast())
                .updatedAt(cart.getUpdatedAt())
                .items(items)
                .build();
    }

    private CartItemResponseDTO toItemResponseDTO(ItemCart item, Stock stock) {
        CartItemResponseDTO.CartItemResponseDTOBuilder builder = CartItemResponseDTO.builder()
                .id(item.getId())
                .stockId(item.getStockId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(calculateSubtotal(item));
        if (stock != null) {
            builder.size(stock.getSize())
                    .productId(stock.getProduct().getId())
                    .productName(stock.getProduct().getName())
                    .photoUrl(stock.getProduct().getPhotoUrls().isEmpty()
                            ? null
                            : stock.getProduct().getPhotoUrls().get(0));
        }
        return builder.build();
    }

    private Stock findStockById(Long stockId) {
        log.info("Finding stock by id: [{}]", stockId);
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> {
            log.warn("Error finding stock by id: [{}]", stockId);
            return new StockNotFoundException(stockId);
        });
        log.info("Stock found successfully. id: [{}]", stockId);
        return stock;
    }

    private Cart searchCartByUserId(Long userId) {
        log.info("Finding cart for user: [{}]", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Error finding cart for user: [{}]", userId);
                    return new CartNotFoundException(userId);
                });
        log.info("Cart found successfully. User: [{}], Cart: [{}]", userId, cart.getId());
        return cart;
    }

    private static void logInsufficientStockWarning(Stock stock, Integer finalQuantity) {
        log.warn("Requested quantity [{}] exceeds available stock [{}] for size [{}]",
                finalQuantity, stock.getQuantity(), stock.getSize()
        );
    }
}
