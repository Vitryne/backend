package com.vitryne.api.order.service;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.entity.CartItem;
import com.vitryne.api.order.enums.ReviewIssueType;
import com.vitryne.api.order.enums.ReviewItemStatus;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.product.enums.ProductStatus;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutEvaluatorTest {

    private final CheckoutEvaluator evaluator = new CheckoutEvaluator();
    private final Store store = Store.builder().id(1L).name("Loja").build();

    @Test
    void carrinhoValidoDeveCalcularValoresComFreteZeroEGerarToken() {
        Product product = product(1L, "19.90", "ACTIVE", store);
        Cart cart = cart(line(10L, stock(101L, product, "P", 5), 2, "19.90"));

        CheckoutEvaluator.Evaluation result = evaluator.evaluate(cart, address());

        assertThat(result.canConfirm()).isTrue();
        assertThat(result.issues()).isEmpty();
        assertThat(result.itemsSubtotal()).isEqualByComparingTo("39.80");
        assertThat(result.shippingAmount()).isEqualByComparingTo("0.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("39.80");
        assertThat(result.reviewToken()).hasSize(64);
        assertThat(result.lines().getFirst().status()).isEqualTo(ReviewItemStatus.OK);
    }

    @Test
    void precoAlteradoDeveInformarSemBloquearEUsarPrecoAtual() {
        Product product = product(1L, "19.90", "ACTIVE", store);
        Cart cart = cart(line(10L, stock(101L, product, "P", 5), 2, "25.00"));

        CheckoutEvaluator.Evaluation result = evaluator.evaluate(cart, address());

        assertThat(result.canConfirm()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("39.80");
        assertThat(result.lines().getFirst().status()).isEqualTo(ReviewItemStatus.PRICE_CHANGED);
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(ReviewIssueType.PRICE_CHANGED);
            assertThat(issue.cartItemId()).isEqualTo(10L);
            assertThat(issue.message()).contains("25,00").contains("19,90");
        });
    }

    @Test
    void indisponibilidadeDeveBloquearConfirmacaoSemToken() {
        Product active = product(1L, "10.00", "ACTIVE", store);
        Product inactive = product(2L, "10.00", ProductStatus.INACTIVE.name(), store);
        Cart cart = cart(
                line(10L, stock(101L, active, "P", 1), 2, "10.00"),
                line(11L, stock(102L, active, "M", 0), 1, "10.00"),
                line(12L, stock(103L, inactive, "G", 5), 1, "10.00")
        );

        CheckoutEvaluator.Evaluation result = evaluator.evaluate(cart, address());

        assertThat(result.canConfirm()).isFalse();
        assertThat(result.reviewToken()).isNull();
        assertThat(result.lines())
                .extracting(CheckoutEvaluator.Line::status)
                .containsExactly(
                        ReviewItemStatus.INSUFFICIENT_QUANTITY,
                        ReviewItemStatus.OUT_OF_STOCK,
                        ReviewItemStatus.PRODUCT_INACTIVE
                );
        assertThat(result.issues())
                .extracting(CheckoutEvaluator.Issue::type)
                .containsExactly(
                        ReviewIssueType.INSUFFICIENT_QUANTITY,
                        ReviewIssueType.OUT_OF_STOCK,
                        ReviewIssueType.PRODUCT_INACTIVE
                );
    }

    @Test
    void enderecoAusenteOuInvalidoDeveBloquear() {
        Cart cart = cart(line(10L, stock(101L, product(1L, "10.00", "ACTIVE", store), "P", 5), 1, "10.00"));
        Address invalidState = Address.builder()
                .zipCode("87000000").street("Rua").number("1").neighborhood("Centro")
                .city("Maringá").state("XX").build();

        for (Address address : new Address[]{null, invalidState}) {
            CheckoutEvaluator.Evaluation result = evaluator.evaluate(cart, address);

            assertThat(result.canConfirm()).isFalse();
            assertThat(result.deliveryAddress()).isNull();
            assertThat(result.issues())
                    .extracting(CheckoutEvaluator.Issue::type)
                    .containsExactly(ReviewIssueType.INVALID_DELIVERY_ADDRESS);
        }
    }

    @Test
    void tokenDeveMudarQuandoPrecoQuantidadeOuEnderecoMudam() {
        Product product = product(1L, "19.90", "ACTIVE", store);
        Stock stock = stock(101L, product, "P", 5);
        CartItem item = line(10L, stock, 2, "19.90");
        Cart cart = cart(item);

        String original = evaluator.evaluate(cart, address()).reviewToken();
        assertThat(evaluator.evaluate(cart, address()).reviewToken()).isEqualTo(original);

        item.setQuantity(3);
        String otherQuantity = evaluator.evaluate(cart, address()).reviewToken();
        item.setQuantity(2);

        ReflectionTestUtils.setField(product, "price", new BigDecimal("29.90"));
        String otherPrice = evaluator.evaluate(cart, address()).reviewToken();
        ReflectionTestUtils.setField(product, "price", new BigDecimal("19.90"));

        Address otherAddress = Address.builder()
                .zipCode("87000000").street("Outra rua").number("123A").neighborhood("Centro")
                .city("Maringá").state("PR").build();
        String otherAddressToken = evaluator.evaluate(cart, otherAddress).reviewToken();

        assertThat(List.of(otherQuantity, otherPrice, otherAddressToken))
                .doesNotContain(original)
                .doesNotHaveDuplicates();
    }

    static Address address() {
        return Address.builder()
                .zipCode("87000000")
                .street("Rua de Teste")
                .number("123A")
                .neighborhood("Centro")
                .city("Maringá")
                .state("PR")
                .build();
    }

    private Cart cart(CartItem... items) {
        Cart cart = Cart.builder().id(20L).userId(10L).totalValueForecast(new BigDecimal("0.00")).build();
        for (CartItem item : items) {
            item.setCart(cart);
            cart.getItems().add(item);
        }
        return cart;
    }

    private CartItem line(Long id, Stock stock, int quantity, String unitPrice) {
        return CartItem.builder()
                .id(id)
                .stock(stock)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
    }

    private Product product(Long id, String price, String status, Store store) {
        Product product = Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal(price))
                .type("camiseta")
                .status(status)
                .photoUrls(List.of("https://example.com/camiseta.jpg"))
                .store(store)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Stock stock(Long id, Product product, String size, int quantity) {
        Stock stock = new Stock(product, size, quantity);
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }
}
