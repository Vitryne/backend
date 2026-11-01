package com.vitryne.api.product.controller;

import com.vitryne.api.product.entity.Product;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.product.service.ProductService;
import com.vitryne.api.shared.exception.GlobalExceptionHandler;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private Store store;

    private MockMvc mvc;


    @BeforeEach
    void setUp() {
        ProductService service =
                new ProductService(productRepository, storeRepository);

        this.mvc = MockMvcBuilders
                .standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void homeDeveConsultarAtivosEManterProdutoEsgotadoNaVitrine()
            throws Exception {
        Product disponivel = produto(10L);
        tamanho(disponivel, 101L, "P", 3);

        Product esgotado = produto(20L);
        tamanho(esgotado, 201L, "M", 0);

        when(productRepository.findAllByStatus("ACTIVE"))
                .thenReturn(List.of(disponivel, esgotado));

        mvc.perform(get("/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].availableProduct").value(false))
                .andExpect(jsonPath("$[1].id").value(10))
                .andExpect(jsonPath("$[1].availableProduct").value(true))
                .andExpect(jsonPath("$[1].finalPrice").value(100.0))
                .andExpect(jsonPath("$[1].photoUrls[0]")
                        .value("https://example.com/frente.jpg"));

        verify(productRepository).findAllByStatus("ACTIVE");
        verify(productRepository, never()).findAll();
    }

    @Test
    void homeSemProdutosAtivosDeveRetornarArrayVazio() throws Exception {
        when(productRepository.findAllByStatus("ACTIVE"))
                .thenReturn(List.of());

        mvc.perform(get("/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void detalheDeveRetornarPrecoPromocionalFotosEIdDeCadaVariacao()
            throws Exception {
        Product produto = produto(10L);

        tamanho(produto, 102L, "M", 0);
        tamanho(produto, 101L, "P", 5);

        Instant agora = Instant.now();
        produto.configureSale(
                new BigDecimal("79.90"),
                agora.minusSeconds(3600),
                agora.plusSeconds(3600)
        );

        detalheEncontrado(produto);

        mvc.perform(get("/product/client/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.finalPrice").value(79.90))
                .andExpect(jsonPath("$.onSale").value(true))
                .andExpect(jsonPath("$.photoUrls", hasSize(2)))
                .andExpect(jsonPath("$.photoUrls[0]")
                        .value("https://example.com/frente.jpg"))
                .andExpect(jsonPath("$.photoUrls[1]")
                        .value("https://example.com/costas.jpg"))
                .andExpect(jsonPath("$.availableProduct").value(true))
                .andExpect(jsonPath("$.availableSizes", hasSize(2)))
                .andExpect(jsonPath("$.availableSizes[0].id").value(101))
                .andExpect(jsonPath("$.availableSizes[0].size").value("P"))
                .andExpect(jsonPath("$.availableSizes[0].quantity").value(5))
                .andExpect(jsonPath("$.availableSizes[0].available").value(true))
                .andExpect(jsonPath("$.availableSizes[1].id").value(102))
                .andExpect(jsonPath("$.availableSizes[1].size").value("M"))
                .andExpect(jsonPath("$.availableSizes[1].quantity").value(0))
                .andExpect(jsonPath("$.availableSizes[1].available").value(false));
    }

    @Test
    void produtoAtivoTotalmenteEsgotadoDeveRetornar200ComTamanhos()
            throws Exception {
        Product produto = produto(10L);
        tamanho(produto, 101L, "P", 0);
        tamanho(produto, 102L, "M", 0);
        detalheEncontrado(produto);

        mvc.perform(get("/product/client/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableProduct").value(false))
                .andExpect(jsonPath("$.availableSizes", hasSize(2)))
                .andExpect(jsonPath("$.availableSizes[0].available").value(false))
                .andExpect(jsonPath("$.availableSizes[1].available").value(false));
    }

    @Test
    void produtoAtivoSemVariacoesDeveRetornarListaVaziaENaoPermitirCompra()
            throws Exception {
        Product produto = produto(10L);
        detalheEncontrado(produto);

        mvc.perform(get("/product/client/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableProduct").value(false))
                .andExpect(jsonPath("$.availableSizes").isArray())
                .andExpect(jsonPath("$.availableSizes", hasSize(0)));
    }

    @Test
    void promocaoFuturaNaoDeveReduzirPrecoExibido() throws Exception {
        Product produto = produto(10L);

        Instant agora = Instant.now();
        produto.configureSale(
                new BigDecimal("79.90"),
                agora.plusSeconds(3600),
                agora.plusSeconds(7200)
        );

        detalheEncontrado(produto);

        mvc.perform(get("/product/client/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onSale").value(false))
                .andExpect(jsonPath("$.finalPrice").value(100.0));
    }

    @Test
    void promocaoExpiradaDeveVoltarAoPrecoNormal() throws Exception {
        Product produto = produto(10L);


        Instant agora = Instant.now();
        ReflectionTestUtils.setField(produto, "promotionalPrice",
                new BigDecimal("79.90"));
        ReflectionTestUtils.setField(produto, "promotionalStartDate",
                agora.minusSeconds(7200));
        ReflectionTestUtils.setField(produto, "promotionalEndDate",
                agora.minusSeconds(3600));

        detalheEncontrado(produto);

        mvc.perform(get("/product/client/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onSale").value(false))
                .andExpect(jsonPath("$.finalPrice").value(100.0));
    }

    @Test
    void produtoNaoEncontradoEntreOsAtivosDeveRetornar404() throws Exception {
        when(productRepository.findByIdAndStatus(99L, "ACTIVE"))
                .thenReturn(Optional.empty());

        mvc.perform(get("/product/client/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found: 99"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(productRepository).findByIdAndStatus(99L, "ACTIVE");
        verify(productRepository, never()).findById(99L);
    }

    private Product produto(long id) {
        Product produto = Product.builder()
                .name("Camiseta preta")
                .description("Camiseta de algodão")
                .price(new BigDecimal("100.00"))
                .type("camiseta")
                .color("preto")
                .status("ACTIVE")
                .photoUrls(List.of(
                        "https://example.com/frente.jpg",
                        "https://example.com/costas.jpg"
                ))
                .store(store)
                .build();

        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }

    private void tamanho(Product produto, long stockId, String size, int quantity) {
        Stock stock = produto.addStock(size, quantity);
        ReflectionTestUtils.setField(stock, "id", stockId);
    }

    private void detalheEncontrado(Product produto) {
        when(productRepository.findByIdAndStatus(produto.getId(), "ACTIVE"))
                .thenReturn(Optional.of(produto));
    }
}