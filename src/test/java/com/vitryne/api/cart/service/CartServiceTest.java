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
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long CART_ID = 20L;
    private static final Long ITEM_ID = 30L;
    private static final Long STOCK_P = 101L;
    private static final Long STOCK_M = 102L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private CartService service;

    @Test
    void primeiroItemDeveCriarCarrinhoDoUsuarioComAVariacaoSelecionada() {
        Product product = product(1L, 50L, "19.90", "ACTIVE");
        Stock stock = stock(STOCK_P, product, "P", 8);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        stockEncontrado(stock);
        simularPersistencia();

        CartResponseDTO response = service.addItem(
                USER_ID, new AddItemRequestDTO(STOCK_P, 2)
        );

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).saveAndFlush(captor.capture());

        Cart saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getItems()).hasSize(1);

        CartItem item = saved.getItems().getFirst();
        assertThat(item.getCart()).isSameAs(saved);
        assertThat(item.getStock()).isSameAs(stock);

        CartItemResponseDTO dto = response.items().getFirst();
        assertThat(dto.stockId()).isEqualTo(STOCK_P);
        assertThat(dto.productId()).isEqualTo(50L);
        assertThat(dto.size()).isEqualTo("P");
        assertThat(dto.quantity()).isEqualTo(2);
        assertThat(dto.unitPrice()).isEqualByComparingTo("19.90");
        assertThat(dto.subtotal()).isEqualByComparingTo("39.80");
        assertThat(response.totalValueForecast()).isEqualByComparingTo("39.80");

        assertThat(stock.getQuantity()).isEqualTo(8);
    }

    @Test
    void mesmaVariacaoDeveSomarNaMesmaLinhaEOutroTamanhoDeveCriarOutraLinha() {
        Product product = product(1L, 50L, "19.90", "ACTIVE");
        Stock p = stock(STOCK_P, product, "P", 5);
        Stock m = stock(STOCK_M, product, "M", 5);

        Cart cart = carrinho();
        carrinhoEncontrado(cart);
        stockEncontrado(p);
        stockEncontrado(m);
        simularPersistencia();

        service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 1));
        CartItem primeiraLinha = cart.getItems().getFirst();

        service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 1));
        CartResponseDTO response = service.addItem(
                USER_ID, new AddItemRequestDTO(STOCK_M, 1)
        );

        assertThat(response.id()).isEqualTo(CART_ID);
        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getItems().getFirst()).isSameAs(primeiraLinha);

        assertThat(response.items())
                .extracting(CartItemResponseDTO::stockId)
                .containsExactly(STOCK_P, STOCK_M);

        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("39.80");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo("19.90");
        assertThat(response.totalValueForecast()).isEqualByComparingTo("59.70");
    }

    @Test
    void deveValidarQuantidadeAcumuladaAoAdicionarMesmaVariacao() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 3);
        Cart cart = carrinho();
        CartItem item = incluirLinha(cart, ITEM_ID, stock, 2);

        carrinhoEncontrado(cart);
        stockEncontrado(stock);

        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 2))
        ).isInstanceOf(UnavailableQuantityException.class);

        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).containsExactly(item);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void putDeveSubstituirQuantidadeEAplicarPrecoVigente() {
        Product product = product(1L, 50L, "25.00", "ACTIVE");
        Stock stock = stock(STOCK_P, product, "P", 10);
        Cart cart = carrinho();
        CartItem item = incluirLinha(cart, ITEM_ID, stock, 2);

        Instant now = Instant.now();
        product.configureSale(
                new BigDecimal("19.90"),
                now.minusSeconds(3600),
                now.plusSeconds(3600)
        );

        carrinhoEncontrado(cart);
        simularPersistencia();

        CartResponseDTO response = service.updateItemQuantity(
                USER_ID, ITEM_ID, new UpdateItemRequestDTO(3)
        );

        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("19.90");
        assertThat(response.totalValueForecast()).isEqualByComparingTo("59.70");
        assertThat(stock.getQuantity()).isEqualTo(10);

        verifyNoInteractions(stockRepository);
    }

    @Test
    void putAcimaDoEstoqueDevePreservarQuantidadeAnterior() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 3);
        Cart cart = carrinho();
        CartItem item = incluirLinha(cart, ITEM_ID, stock, 2);
        carrinhoEncontrado(cart);

        assertThatThrownBy(() ->
                service.updateItemQuantity(USER_ID, ITEM_ID, new UpdateItemRequestDTO(4))
        ).isInstanceOf(UnavailableQuantityException.class);

        assertThat(item.getQuantity()).isEqualTo(2);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void deveRejeitarQuantidadeInvalidaAntesDeBuscarDados(Integer quantity) {
        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, quantity))
        ).isInstanceOf(InvalidQuantityException.class);

        assertThatThrownBy(() ->
                service.updateItemQuantity(USER_ID, ITEM_ID, new UpdateItemRequestDTO(quantity))
        ).isInstanceOf(InvalidQuantityException.class);

        verifyNoInteractions(cartRepository, stockRepository);
    }

    @Test
    void naoDeveAdicionarVariacaoInexistente() {
        when(stockRepository.findById(STOCK_P)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 1))
        ).isInstanceOf(StockNotFoundException.class);

        verifyNoInteractions(cartRepository);
    }

    @Test
    void naoDeveAdicionarVariacaoEsgotada() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 0);
        stockEncontrado(stock);

        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 1))
        ).isInstanceOf(StockUnavailableException.class);

        verifyNoInteractions(cartRepository);
    }

    @Test
    void naoDeveAdicionarProdutoInativoMesmoComEstoque() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "INACTIVE"), "P", 5);
        stockEncontrado(stock);

        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_P, 1))
        ).isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
        );

        verifyNoInteractions(cartRepository);
    }

    @Test
    void naoDeveMisturarLojasNoMesmoCarrinho() {
        Stock primeiraLoja = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 5);
        Stock outraLoja = stock(STOCK_M, product(2L, 60L, "30.00", "ACTIVE"), "M", 5);

        Cart cart = carrinho();
        CartItem original = incluirLinha(cart, ITEM_ID, primeiraLoja, 1);

        carrinhoEncontrado(cart);
        stockEncontrado(outraLoja);

        assertThatThrownBy(() ->
                service.addItem(USER_ID, new AddItemRequestDTO(STOCK_M, 1))
        ).isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
        );

        assertThat(cart.getItems()).containsExactly(original);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void consultaDeveUsarPrecoRegistradoNaLinhaSemRecalcularPeloCatalogo() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "25.00", "ACTIVE"), "P", 8);
        Cart cart = carrinho();
        CartItem item = incluirLinha(cart, ITEM_ID, stock, 2);

        item.setUnitPrice(new BigDecimal("19.90"));
        cart.setTotalValueForecast(new BigDecimal("39.80"));
        carrinhoEncontrado(cart);

        CartResponseDTO response = service.searchByUser(USER_ID);

        CartItemResponseDTO dto = response.items().getFirst();
        assertThat(dto.id()).isEqualTo(ITEM_ID);
        assertThat(dto.stockId()).isEqualTo(STOCK_P);
        assertThat(dto.productId()).isEqualTo(50L);
        assertThat(dto.productName()).isEqualTo("Camiseta");
        assertThat(dto.photoUrl()).isEqualTo("https://example.com/camiseta.jpg");
        assertThat(dto.unitPrice()).isEqualByComparingTo("19.90");
        assertThat(dto.subtotal()).isEqualByComparingTo("39.80");
        assertThat(response.totalValueForecast()).isEqualByComparingTo("39.80");

        verifyNoInteractions(stockRepository);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void consultaSemCarrinhoDeveManterContratoAtual() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.searchByUser(USER_ID))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void removerItemDevePreservarOutroTamanhoERecalcularTotal() {
        Product product = product(1L, 50L, "19.90", "ACTIVE");
        Stock p = stock(STOCK_P, product, "P", 5);
        Stock m = stock(STOCK_M, product, "M", 7);

        Cart cart = carrinho();
        incluirLinha(cart, ITEM_ID, p, 2);
        CartItem restante = incluirLinha(cart, 31L, m, 1);

        carrinhoEncontrado(cart);
        simularPersistencia();

        CartResponseDTO response = service.removeItem(USER_ID, ITEM_ID);

        assertThat(cart.getItems()).containsExactly(restante);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().stockId()).isEqualTo(STOCK_M);
        assertThat(response.totalValueForecast()).isEqualByComparingTo("19.90");
        assertThat(p.getQuantity()).isEqualTo(5);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void naoDeveAlterarNemRemoverItemQueNaoEstaNoCarrinhoDoUsuario() {
        Cart cart = carrinho();
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 5);
        CartItem original = incluirLinha(cart, ITEM_ID, stock, 1);
        carrinhoEncontrado(cart);

        Long itemDeOutroCarrinho = 999L;

        assertThatThrownBy(() -> service.updateItemQuantity(
                USER_ID, itemDeOutroCarrinho, new UpdateItemRequestDTO(2)
        )).isInstanceOf(CartItemNotFoundException.class);

        assertThatThrownBy(() -> service.removeItem(USER_ID, itemDeOutroCarrinho))
                .isInstanceOf(CartItemNotFoundException.class);

        assertThat(cart.getItems()).containsExactly(original);
        assertThat(original.getQuantity()).isEqualTo(1);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void limparDeveManterCarrinhoEZerarTotalSemAlterarEstoque() {
        Stock stock = stock(STOCK_P, product(1L, 50L, "19.90", "ACTIVE"), "P", 5);
        Cart cart = carrinho();
        incluirLinha(cart, ITEM_ID, stock, 2);
        cart.setTotalValueForecast(new BigDecimal("39.80"));

        carrinhoEncontrado(cart);
        simularPersistencia();

        CartResponseDTO response = service.clear(USER_ID);

        assertThat(response.id()).isEqualTo(CART_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalValueForecast()).isEqualByComparingTo("0.00");
        assertThat(stock.getQuantity()).isEqualTo(5);
        verifyNoInteractions(stockRepository);
    }


    private Cart carrinho() {
        return Cart.builder()
                .id(CART_ID)
                .userId(USER_ID)
                .totalValueForecast(new BigDecimal("0.00"))
                .build();
    }

    private void carrinhoEncontrado(Cart cart) {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
    }

    private void stockEncontrado(Stock stock) {
        when(stockRepository.findById(stock.getId())).thenReturn(Optional.of(stock));
    }

    private void simularPersistencia() {
        when(cartRepository.saveAndFlush(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Cart.class));
    }

    private Product product(Long storeId, Long productId, String price, String status) {
        Store store = Store.builder().id(storeId).build();

        Product product = Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal(price))
                .type("camiseta")
                .status(status)
                .photoUrls(List.of("https://example.com/camiseta.jpg"))
                .store(store)
                .build();

        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private Stock stock(Long id, Product product, String size, int quantity) {
        Stock stock = new Stock(product, size, quantity);
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }

    private CartItem incluirLinha(Cart cart, Long itemId, Stock stock, int quantity) {
        CartItem item = CartItem.builder()
                .id(itemId)
                .cart(cart)
                .stock(stock)
                .quantity(quantity)
                .unitPrice(stock.getProduct().calculateFinalPrice())
                .build();

        cart.getItems().add(item);
        return item;
    }
}