package com.vitryne.api.order.service;

import com.vitryne.api.address.AddressNormalizer;
import com.vitryne.api.address.entity.Address;
import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.entity.CartItem;
import com.vitryne.api.order.enums.ReviewIssueType;
import com.vitryne.api.order.enums.ReviewItemStatus;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Calcula e valida a compra a partir do carrinho. Usado pela revisão
 * (somente leitura) e pelo checkout (com estoque travado), garantindo que
 * ambos cheguem aos mesmos valores e ao mesmo reviewToken.
 */
@Component
public class CheckoutEvaluator {

    /** Frete fixo do MVP. */
    static final BigDecimal MVP_SHIPPING = new BigDecimal("0.00");

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final int MAX_INTEGER_DIGITS = 10;
    private static final Pattern ZIP_CODE = Pattern.compile("\\d{8}");

    public Evaluation evaluate(Cart cart, Address deliveryAddress) {
        List<Issue> issues = new ArrayList<>();

        List<Line> lines = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getStock().getId()))
                .map(item -> evaluateLine(item, issues))
                .toList();

        Store store = lines.getFirst().stock().getProduct().getStore();
        boolean multipleStores = lines.stream()
                .map(line -> line.stock().getProduct().getStore().getId())
                .anyMatch(storeId -> !store.getId().equals(storeId));

        if (multipleStores) {
            issues.add(new Issue(
                    ReviewIssueType.MULTIPLE_STORES, null,
                    "O carrinho contém produtos de mais de uma loja"
            ));
        }

        Address validAddress = validateAddress(deliveryAddress, issues);

        BigDecimal itemsSubtotal = lines.stream()
                .map(Line::subtotal)
                .reduce(ZERO_MONEY, BigDecimal::add);
        BigDecimal total = itemsSubtotal.add(MVP_SHIPPING);

        if (total.precision() - total.scale() > MAX_INTEGER_DIGITS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor total do pedido excede o limite permitido"
            );
        }

        boolean canConfirm = issues.stream().noneMatch(issue -> issue.type().isBlocking());
        String token = canConfirm
                ? reviewToken(store, validAddress, lines, MVP_SHIPPING, total)
                : null;

        return new Evaluation(
                store, validAddress, lines, itemsSubtotal, MVP_SHIPPING, total,
                List.copyOf(issues), canConfirm, token
        );
    }

    private Line evaluateLine(CartItem item, List<Issue> issues) {
        Stock stock = item.getStock();
        Product product = stock.getProduct();
        int quantity = item.getQuantity();
        BigDecimal currentPrice = product.calculateFinalPrice();
        String label = product.getName() + " (" + stock.getSize() + ")";

        ReviewItemStatus status = ReviewItemStatus.OK;

        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(currentPrice) != 0) {
            status = ReviewItemStatus.PRICE_CHANGED;
            issues.add(new Issue(
                    ReviewIssueType.PRICE_CHANGED, item.getId(),
                    "O preço de " + label + " mudou de " + money(item.getUnitPrice())
                            + " para " + money(currentPrice)
            ));
        }

        if (!"ACTIVE".equals(product.getStatus())) {
            status = ReviewItemStatus.PRODUCT_INACTIVE;
            issues.add(new Issue(
                    ReviewIssueType.PRODUCT_INACTIVE, item.getId(),
                    "O produto " + label + " não está mais disponível para venda"
            ));
        } else if (stock.getQuantity() <= 0) {
            status = ReviewItemStatus.OUT_OF_STOCK;
            issues.add(new Issue(
                    ReviewIssueType.OUT_OF_STOCK, item.getId(),
                    "O produto " + label + " está esgotado"
            ));
        } else if (quantity > stock.getQuantity()) {
            status = ReviewItemStatus.INSUFFICIENT_QUANTITY;
            issues.add(new Issue(
                    ReviewIssueType.INSUFFICIENT_QUANTITY, item.getId(),
                    "Estoque insuficiente para " + label + ": disponível "
                            + stock.getQuantity() + ", no carrinho " + quantity
            ));
        }

        return new Line(
                item, stock, quantity, item.getUnitPrice(), currentPrice,
                currentPrice.multiply(BigDecimal.valueOf(quantity)), status
        );
    }

    private Address validateAddress(Address address, List<Issue> issues) {
        boolean valid = address != null
                && notBlank(address.getStreet())
                && notBlank(address.getNumber())
                && notBlank(address.getNeighborhood())
                && notBlank(address.getCity())
                && address.getZipCode() != null
                && ZIP_CODE.matcher(address.getZipCode()).matches()
                && address.getState() != null
                && AddressNormalizer.STATES.contains(address.getState());

        if (!valid) {
            issues.add(new Issue(
                    ReviewIssueType.INVALID_DELIVERY_ADDRESS, null,
                    "O endereço de entrega está incompleto ou inválido. Atualize-o antes de continuar."
            ));
            return null;
        }

        return address;
    }

    /**
     * Impressão digital dos valores revisados. Não é segredo: o checkout
     * recalcula tudo e só compara, então um token forjado não altera valores.
     */
    private static String reviewToken(
            Store store,
            Address address,
            List<Line> lines,
            BigDecimal shipping,
            BigDecimal total
    ) {
        StringBuilder canonical = new StringBuilder("v1|store=").append(store.getId());

        canonical.append("|addr=")
                .append(address.getZipCode()).append(';')
                .append(address.getStreet()).append(';')
                .append(address.getNumber()).append(';')
                .append(address.getNeighborhood()).append(';')
                .append(Objects.toString(address.getComplement(), "")).append(';')
                .append(address.getCity()).append(';')
                .append(address.getState());

        for (Line line : lines) {
            canonical.append("|item=")
                    .append(line.stock().getId()).append(':')
                    .append(line.quantity()).append(':')
                    .append(plain(line.currentUnitPrice()));
        }

        canonical.append("|shipping=").append(plain(shipping))
                .append("|total=").append(plain(total));

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String money(BigDecimal value) {
        return plain(value).replace('.', ',');
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record Line(
            CartItem cartItem,
            Stock stock,
            int quantity,
            BigDecimal cartUnitPrice,
            BigDecimal currentUnitPrice,
            BigDecimal subtotal,
            ReviewItemStatus status
    ) {
    }

    public record Issue(ReviewIssueType type, Long cartItemId, String message) {
    }

    public record Evaluation(
            Store store,
            Address deliveryAddress,
            List<Line> lines,
            BigDecimal itemsSubtotal,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            List<Issue> issues,
            boolean canConfirm,
            String reviewToken
    ) {
    }
}
