package com.vitryne.api.product.service;

import com.vitryne.api.product.dto.*;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.product.enums.ProductStatus;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.shared.exception.ProductNotFoundException;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.exception.StoreNotFoundException;
import com.vitryne.api.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long PRODUCT_ID = 20L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ProductService service;

    @Test
    void deveCadastrarProdutoComLojaEEstoquesIniciais() {
        Store store = mock(Store.class);

        when(storeRepository.findByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(store));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
                    return product;
                });

        var response = service.createProduct(createRequest(), OWNER_ID);

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();

        assertThat(response).isNotNull();
        assertThat(saved.getStore()).isSameAs(store);
        assertThat(saved.getName()).isEqualTo("Camiseta");
        assertThat(saved.getPrice()).isEqualByComparingTo("100.00");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");

        assertThat(saved.getStocks())
                .extracting(Stock::getSize)
                .containsExactly("P", "M");

        assertThat(saved.getStocks())
                .extracting(Stock::getQuantity)
                .containsExactly(5, 0);

        assertThat(saved.getStocks())
                .allSatisfy(stock ->
                        assertThat(stock.getProduct()).isSameAs(saved)
                );

        // ownerId é o ID do usuário, não o ID da loja.
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void naoDeveCadastrarProdutoSemLojaDoProprietario() {
        when(storeRepository.findByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createProduct(createRequest(), OWNER_ID)
        ).isInstanceOf(StoreNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deveEditarSemSubstituirProdutoOuApagarSeusVinculos() {
        Product product = product();
        product.addStock("M", 7);

        Store originalStore = product.getStore();
        Stock originalStock = product.getStocks().getFirst();

        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        ReflectionTestUtils.setField(product, "createdAt", createdAt);
        ReflectionTestUtils.setField(product, "rating", 4.5);
        ReflectionTestUtils.setField(product, "status", "INACTIVE");

        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now().plusSeconds(86400);

        product.configureSale(new BigDecimal("80.00"), start, end);

        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(product));

        service.editProduct(
                PRODUCT_ID,
                OWNER_ID,
                editRequest(new BigDecimal("120.00"))
        );

        assertThat(product.getName()).isEqualTo("Camiseta atualizada");
        assertThat(product.getPrice()).isEqualByComparingTo("120.00");
        assertThat(product.getPhotoUrls())
                .containsExactly("https://example.com/new.jpg");

        assertThat(product.getId()).isEqualTo(PRODUCT_ID);
        assertThat(product.getStore()).isSameAs(originalStore);
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
        assertThat(product.getRating()).isEqualTo(4.5);
        assertThat(product.getStatus()).isEqualTo("INACTIVE");

        assertThat(product.getStocks()).containsExactly(originalStock);
        assertThat(originalStock.getQuantity()).isEqualTo(7);

        assertThat(product.getPromotionalPrice())
                .isEqualByComparingTo("80.00");
        assertThat(product.getPromotionalStartDate()).isEqualTo(start);
        assertThat(product.getPromotionalEndDate()).isEqualTo(end);

        verify(productRepository, never()).save(any());
        verifyNoInteractions(storeRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"consulta", "edicao", "status", "promocao"})
    void deveRecusarOperacaoQuandoProdutoNaoPertenceAoProprietario(
            String operation
    ) {
        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            switch (operation) {
                case "consulta" ->
                        service.findByIdManager(PRODUCT_ID, OWNER_ID);

                case "edicao" ->
                        service.editProduct(
                                PRODUCT_ID,
                                OWNER_ID,
                                editRequest(new BigDecimal("120.00"))
                        );

                case "status" ->
                        service.alterStatus(
                                PRODUCT_ID,
                                OWNER_ID,
                                new StatusRequestDTO(ProductStatus.INACTIVE)
                        );

                case "promocao" ->
                        service.configureSale(
                                PRODUCT_ID,
                                OWNER_ID,
                                new ConfigureSaleRequestDTO(
                                        new BigDecimal("80.00"),
                                        Instant.now(),
                                        Instant.now().plusSeconds(86400)
                                )
                        );

                default -> throw new AssertionError(operation);
            }
        }).isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
        verify(productRepository, never()).flush();
    }

    @Test
    void deveRetornarListaVaziaQuandoLojaNaoTemProdutos() {
        when(productRepository.findAllByStoreOwnerId(OWNER_ID))
                .thenReturn(List.of());

        assertThat(service.listManagedProducts(OWNER_ID)).isEmpty();
    }

    @Test
    void deveListarProdutosAtivosEInativosNaGestao() {
        Product active = product();
        Product inactive = product();

        ReflectionTestUtils.setField(inactive, "status", "INACTIVE");

        when(productRepository.findAllByStoreOwnerId(OWNER_ID))
                .thenReturn(List.of(active, inactive));

        assertThat(service.listManagedProducts(OWNER_ID)).hasSize(2);
    }

    @Test
    void deveConsultarSomenteAtivosNoCatalogoPublico() {
        when(productRepository.findAllByStatus("ACTIVE"))
                .thenReturn(List.of(product()));

        assertThat(service.listProducts()).hasSize(1);

        verify(productRepository).findAllByStatus("ACTIVE");
        verify(productRepository, never()).findAll();
    }

    @Test
    void naoDeveRetornarProdutoInativoNaConsultaPublica() {
        when(productRepository.findByIdAndStatus(PRODUCT_ID, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdClient(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).findById(any());
    }

    @Test
    void deveDesativarProdutoSemApagarEstoque() {
        Product product = product();
        product.addStock("G", 8);

        Stock stock = product.getStocks().getFirst();

        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(product));

        var request = new StatusRequestDTO(ProductStatus.INACTIVE);

        service.alterStatus(PRODUCT_ID, OWNER_ID, request);
        service.alterStatus(PRODUCT_ID, OWNER_ID, request);

        assertThat(product.getStatus()).isEqualTo("INACTIVE");
        assertThat(product.getStocks()).containsExactly(stock);
        assertThat(stock.getQuantity()).isEqualTo(8);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void deveReativarProdutoInativo() {
        Product product = product();
        ReflectionTestUtils.setField(product, "status", "INACTIVE");

        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(product));

        service.alterStatus(
                PRODUCT_ID,
                OWNER_ID,
                new StatusRequestDTO(ProductStatus.ACTIVE)
        );

        assertThat(product.getStatus()).isEqualTo("ACTIVE");
    }

    private Product product() {
        Product product = Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal("100.00"))
                .type("camiseta")
                .color("preto")
                .status("ACTIVE")
                .photoUrls(List.of("https://example.com/original.jpg"))
                .store(mock(Store.class))
                .build();

        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        return product;
    }

    private CreateProductRequestDTO createRequest() {
        return new CreateProductRequestDTO(
                "Camiseta",
                "Camiseta de algodão",
                new BigDecimal("100.00"),
                "camiseta",
                "preto",
                3,
                List.of("https://example.com/original.jpg"),
                List.of(
                        new CreateStockRequestDTO(" p ", 5),
                        new CreateStockRequestDTO("M", 0)
                )
        );
    }

    private EditProductRequestDTO editRequest(BigDecimal price) {
        return new EditProductRequestDTO(
                "Camiseta atualizada",
                "Descrição atualizada",
                price,
                "camiseta",
                "azul",
                2,
                List.of("https://example.com/new.jpg")
        );
    }
}