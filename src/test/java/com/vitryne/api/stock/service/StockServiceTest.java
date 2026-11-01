package com.vitryne.api.stock.service;

import com.vitryne.api.product.dto.CreateStockRequestDTO;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.stock.dto.AdjustStockRequestDTO;
import com.vitryne.api.stock.dto.ReplenishStockRequestDTO;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long PRODUCT_ID = 20L;
    private static final Long STOCK_ID = 30L;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockService service;

    @Test
    void deveAdicionarTamanhoSemRemoverEstoquesExistentes() {
        Product product = product();
        Stock existing = product.addStock("P", 5);

        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(product));

        when(stockRepository.existsByProductIdAndSize(PRODUCT_ID, "M"))
                .thenReturn(false);

        when(stockRepository.saveAndFlush(any(Stock.class)))
                .thenAnswer(invocation -> {
                    Stock stock = invocation.getArgument(0);
                    ReflectionTestUtils.setField(stock, "id", STOCK_ID);
                    ReflectionTestUtils.setField(stock, "version", 0L);
                    return stock;
                });

        var response = service.addStock(
                PRODUCT_ID,
                OWNER_ID,
                new CreateStockRequestDTO(" m ", 0)
        );

        assertThat(response.id()).isEqualTo(STOCK_ID);
        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.size()).isEqualTo("M");
        assertThat(response.quantity()).isZero();
        assertThat(response.available()).isFalse();

        assertThat(product.getStocks()).hasSize(2).contains(existing);
        assertThat(existing.getQuantity()).isEqualTo(5);

        verify(stockRepository)
                .existsByProductIdAndSize(PRODUCT_ID, "M");
    }

    @Test
    void naoDeveAdicionarTamanhoDuplicado() {
        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(product()));

        when(stockRepository.existsByProductIdAndSize(PRODUCT_ID, "M"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addStock(
                PRODUCT_ID,
                OWNER_ID,
                new CreateStockRequestDTO(" m ", 3)
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );

        verify(stockRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDeveAdicionarTamanhoEmProdutoDeOutraLoja() {
        when(productRepository.findByIdAndStoreOwnerId(
                PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStock(
                PRODUCT_ID,
                OWNER_ID,
                new CreateStockRequestDTO("M", 3)
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                );

        verifyNoInteractions(stockRepository);
    }

    @Test
    void deveSomarReposicaoAoEstoqueAtual() {
        Stock stock = stock(5, 2L);
        found(stock);

        var response = service.replenishStock(
                PRODUCT_ID,
                STOCK_ID,
                OWNER_ID,
                new ReplenishStockRequestDTO(3)
        );

        assertThat(stock.getQuantity()).isEqualTo(8);
        assertThat(response.quantity()).isEqualTo(8);
        assertThat(response.available()).isTrue();

        verify(stockRepository).flush();
    }

    @Test
    void deveAjustarQuantidadeTotalPermitindoZero() {
        Stock stock = stock(5, 2L);
        found(stock);

        var response = service.adjustStock(
                PRODUCT_ID,
                STOCK_ID,
                OWNER_ID,
                new AdjustStockRequestDTO(0, 2L)
        );

        assertThat(stock.getQuantity()).isZero();
        assertThat(response.quantity()).isZero();
        assertThat(response.available()).isFalse();

        verify(stockRepository).flush();
    }

    @Test
    void naoDeveAjustarComVersaoDesatualizada() {
        Stock stock = stock(5, 3L);
        found(stock);

        assertThatThrownBy(() -> service.adjustStock(
                PRODUCT_ID,
                STOCK_ID,
                OWNER_ID,
                new AdjustStockRequestDTO(2, 2L)
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );

        assertThat(stock.getQuantity()).isEqualTo(5);
        verify(stockRepository, never()).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"reposicao", "ajuste"})
    void deveRecusarEstoqueForaDoProdutoOuDaLoja(String operation) {
        when(stockRepository.findByIdAndProductIdAndProductStoreOwnerId(
                STOCK_ID, PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            if (operation.equals("reposicao")) {
                service.replenishStock(
                        PRODUCT_ID,
                        STOCK_ID,
                        OWNER_ID,
                        new ReplenishStockRequestDTO(1)
                );
            } else {
                service.adjustStock(
                        PRODUCT_ID,
                        STOCK_ID,
                        OWNER_ID,
                        new AdjustStockRequestDTO(1, 0L)
                );
            }
        })
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                );

        verify(stockRepository, never()).flush();
        verify(stockRepository, never()).findById(any());
    }

    @Test
    void devePropagarConflitoConcorrenteSemRepetirReposicao() {
        Stock stock = stock(5, 2L);
        found(stock);

        var conflict =
                new OptimisticLockingFailureException("Conflito simulado");

        doThrow(conflict).when(stockRepository).flush();

        assertThatThrownBy(() -> service.replenishStock(
                PRODUCT_ID,
                STOCK_ID,
                OWNER_ID,
                new ReplenishStockRequestDTO(3)
        )).isSameAs(conflict);

        verify(stockRepository, times(1)).flush();
    }

    private void found(Stock stock) {
        when(stockRepository.findByIdAndProductIdAndProductStoreOwnerId(
                STOCK_ID, PRODUCT_ID, OWNER_ID
        )).thenReturn(Optional.of(stock));
    }

    private Stock stock(int quantity, long version) {
        Stock stock = new Stock(product(), "M", quantity);

        ReflectionTestUtils.setField(stock, "id", STOCK_ID);
        ReflectionTestUtils.setField(stock, "version", version);

        return stock;
    }

    private Product product() {
        Product product = Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal("100.00"))
                .type("camiseta")
                .status("ACTIVE")
                .photoUrls(List.of("https://example.com/photo.jpg"))
                .store(mock(Store.class))
                .build();

        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        return product;
    }
}