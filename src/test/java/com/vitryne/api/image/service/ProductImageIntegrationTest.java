package com.vitryne.api.image.service;

import com.vitryne.api.image.exception.ImageStorageException;
import com.vitryne.api.image.storage.ImageStorage;
import com.vitryne.api.image.storage.StoredImage;
import com.vitryne.api.product.dto.CreateProductRequestDTO;
import com.vitryne.api.product.dto.CreateStockRequestDTO;
import com.vitryne.api.product.dto.EditProductRequestDTO;
import com.vitryne.api.product.service.ProductService;
import com.vitryne.api.shared.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Upload, vínculo com produto e limpeza das órfãs contra Postgres real, com armazenamento em memória. */
@Testcontainers
@SpringBootTest(properties = {
        "jwt.public.key=classpath:keys/dev/public.pem",
        "jwt.private.key=classpath:keys/dev/private.pem",
        "vitryne.scheduling.enabled=false",
        "vitryne.images.orphan-retention=24h"
})
class ProductImageIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000);

    @Autowired
    private ImageService imageService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrphanImageCleanupJob cleanupJob;

    @Autowired
    private FakeImageStorage storage;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetStorage() {
        storage.failOnUploadNumber.set(0);
    }

    @Test
    void uploadDevolveUrlsNaOrdemERegistraComoLivres() {
        long owner = newStore();

        List<String> urls = imageService.uploadProductImages(List.of(jpeg("capa.jpg"), png("verso.png")), owner);

        assertThat(urls).hasSize(2);
        assertThat(storage.objects.values()).containsAll(urls);
        assertThat(urls.getFirst()).contains("/vitryne/stores/" + storeIdOf(owner) + "/products/");
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url IN (?, ?) "
                + "AND product_id IS NULL AND orphaned_since IS NOT NULL", urls.get(0), urls.get(1))).isEqualTo(2);
    }

    @Test
    void arquivoInvalidoBarraOEnvioDeTodos() {
        long owner = newStore();
        int before = storage.objects.size();
        MockMultipartFile falso = new MockMultipartFile(
                "files", "foto.jpg", "image/jpeg", "texto".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> imageService.uploadProductImages(List.of(jpeg("ok.jpg"), falso), owner))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("foto.jpg");
        assertThat(storage.objects).hasSize(before);
    }

    @Test
    void recusaMaisDeOitoArquivosPorEnvio() {
        long owner = newStore();
        List<MultipartFile> nove = IntStream.range(0, 9).mapToObj(i -> (MultipartFile) jpeg(i + ".jpg")).toList();

        assertThatThrownBy(() -> imageService.uploadProductImages(nove, owner))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Envie no máximo 8 imagens por vez");
    }

    @Test
    void falhaNoMeioDoEnvioApagaOQueJaTinhaSubido() {
        long owner = newStore();
        int before = storage.objects.size();
        storage.failOnUploadNumber.set(storage.uploads.get() + 2);

        assertThatThrownBy(() -> imageService.uploadProductImages(
                List.of(jpeg("1.jpg"), jpeg("2.jpg"), jpeg("3.jpg")), owner
        )).isInstanceOf(ImageStorageException.class);

        assertThat(storage.objects).hasSize(before);
    }

    @Test
    void cadastroUsaAsFotosDaLojaNaOrdemEMarcaComoEmUso() {
        long owner = newStore();
        List<String> urls = upload(owner, 2);

        Long productId = productService.createProduct(createRequest(urls), owner).id();

        assertThat(photoUrls(productId)).containsExactlyElementsOf(urls);
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE product_id = ? AND orphaned_since IS NULL",
                productId)).isEqualTo(2);
    }

    @Test
    void cadastroRecusaUrlExternaEFotoDeOutraLoja() {
        long owner = newStore();
        long otherOwner = newStore();
        List<String> otherStoreUrls = upload(otherOwner, 1);

        assertThatThrownBy(() -> productService.createProduct(
                createRequest(List.of("https://placehold.co/600x400")), owner
        )).isInstanceOf(InvalidInputException.class).hasMessageContaining("não foi enviada por esta loja");

        assertThatThrownBy(() -> productService.createProduct(createRequest(otherStoreUrls), owner))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("não foi enviada por esta loja");
    }

    @Test
    void mesmaFotoNaoPodeEstarEmDoisProdutos() {
        long owner = newStore();
        List<String> urls = upload(owner, 1);
        productService.createProduct(createRequest(urls), owner);

        assertThatThrownBy(() -> productService.createProduct(createRequest(urls), owner))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("já é usada em outro produto");
    }

    @Test
    void produtoAceitaNoMaximoOitoFotos() {
        long owner = newStore();
        List<String> nove = new ArrayList<>(upload(owner, 8));
        nove.addAll(upload(owner, 1));

        assertThatThrownBy(() -> productService.createProduct(createRequest(nove), owner))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("O produto pode ter no máximo 8 fotos");
    }

    @Test
    void edicaoLiberaAFotoRemovidaEMantemUrlAntigaDoProduto() {
        long owner = newStore();
        List<String> urls = upload(owner, 2);
        Long productId = productService.createProduct(createRequest(urls), owner).id();
        String legada = "https://placehold.co/legado.png";
        jdbc.update("UPDATE product SET photo_urls = array_append(photo_urls, ?) WHERE id = ?", legada, productId);

        productService.editProduct(productId, owner, editRequest(List.of(urls.get(1), legada)));

        assertThat(photoUrls(productId)).containsExactly(urls.get(1), legada);
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url = ? "
                + "AND product_id IS NULL AND orphaned_since IS NOT NULL", urls.get(0))).isEqualTo(1);

        assertThatThrownBy(() -> productService.editProduct(
                productId, owner, editRequest(List.of(urls.get(1), "https://placehold.co/novo.png"))
        )).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void exclusaoDoProdutoLiberaAsFotos() {
        long owner = newStore();
        List<String> urls = upload(owner, 2);
        Long productId = productService.createProduct(createRequest(urls), owner).id();

        productService.deleteProducts(List.of(productId), owner);

        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url IN (?, ?) "
                + "AND product_id IS NULL AND orphaned_since IS NOT NULL", urls.get(0), urls.get(1))).isEqualTo(2);
    }

    @Test
    void limpezaApagaSoAsLivresHaMaisDoQueOPrazo() {
        long owner = newStore();
        List<String> urls = upload(owner, 3);
        String antiga = urls.get(0);
        String recente = urls.get(1);
        String emUso = urls.get(2);
        productService.createProduct(createRequest(List.of(emUso)), owner);
        jdbc.update("UPDATE uploaded_image SET orphaned_since = now() - interval '25 hours' WHERE url = ?", antiga);
        jdbc.update("UPDATE uploaded_image SET created_at = now() - interval '25 hours' WHERE url = ?", emUso);

        cleanupJob.purgeOrphans(Instant.now());

        assertThat(storage.objects.values()).doesNotContain(antiga).contains(recente, emUso);
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url = ?", antiga)).isZero();
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url IN (?, ?)", recente, emUso)).isEqualTo(2);
    }

    @Test
    void limpezaTentaDeNovoQuandoOArmazenamentoFalha() {
        long owner = newStore();
        String url = upload(owner, 1).getFirst();
        jdbc.update("UPDATE uploaded_image SET orphaned_since = now() - interval '2 days' WHERE url = ?", url);
        String publicId = jdbc.queryForObject("SELECT public_id FROM uploaded_image WHERE url = ?", String.class, url);
        storage.failingDeletes.add(publicId);

        cleanupJob.purgeOrphans(Instant.now());
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url = ?", url)).isEqualTo(1);

        storage.failingDeletes.remove(publicId);
        cleanupJob.purgeOrphans(Instant.now().plus(Duration.ofMinutes(1)));
        assertThat(count("SELECT count(*) FROM uploaded_image WHERE url = ?", url)).isZero();
    }

    // ---- apoio ----

    private List<String> upload(long owner, int quantity) {
        List<MultipartFile> files = IntStream.range(0, quantity)
                .mapToObj(i -> (MultipartFile) jpeg("foto" + i + ".jpg"))
                .toList();
        return imageService.uploadProductImages(files, owner);
    }

    private static MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", ImageFileValidatorTest.JPEG);
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", ImageFileValidatorTest.PNG);
    }

    private static CreateProductRequestDTO createRequest(List<String> photoUrls) {
        return new CreateProductRequestDTO(
                "Camiseta", "Algodão", new BigDecimal("59.90"), "CAMISETA", "Preta", null,
                photoUrls, List.of(new CreateStockRequestDTO("M", 3))
        );
    }

    private static EditProductRequestDTO editRequest(List<String> photoUrls) {
        return new EditProductRequestDTO(
                "Camiseta", "Algodão", new BigDecimal("59.90"), "CAMISETA", "Preta", null, photoUrls
        );
    }

    /** @return id do usuário dono da loja */
    private long newStore() {
        long n = SEQ.incrementAndGet();
        long owner = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, role) VALUES (?, 'x', 'STORE') RETURNING id",
                Long.class, "lojista" + n + "@teste.com"
        );
        jdbc.update("INSERT INTO store (owner_id, name) VALUES (?, ?)", owner, "Loja " + n);
        return owner;
    }

    private long storeIdOf(long owner) {
        return jdbc.queryForObject("SELECT id FROM store WHERE owner_id = ?", Long.class, owner);
    }

    private List<String> photoUrls(Long productId) {
        return jdbc.queryForList(
                "SELECT unnest(photo_urls) FROM product WHERE id = ?", String.class, productId
        );
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @TestConfiguration
    static class StorageConfig {
        @Bean
        @Primary
        FakeImageStorage fakeImageStorage() {
            return new FakeImageStorage();
        }
    }

    /** Armazenamento em memória: publicId → url. */
    static class FakeImageStorage implements ImageStorage {
        final ConcurrentHashMap<String, String> objects = new ConcurrentHashMap<>();
        final Set<String> failingDeletes = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final AtomicInteger uploads = new AtomicInteger();
        final AtomicInteger failOnUploadNumber = new AtomicInteger();

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public StoredImage upload(byte[] content, String folder) {
            if (uploads.incrementAndGet() == failOnUploadNumber.get()) {
                throw ImageStorageException.uploadFailed(new RuntimeException("falha simulada"));
            }
            String publicId = folder + "/" + UUID.randomUUID();
            String url = "https://res.cloudinary.com/teste/image/upload/" + publicId + ".jpg";
            objects.put(publicId, url);
            return new StoredImage(publicId, url);
        }

        @Override
        public void delete(String publicId) {
            if (failingDeletes.contains(publicId)) {
                throw ImageStorageException.deleteFailed(publicId, new RuntimeException("falha simulada"));
            }
            objects.remove(publicId);
        }
    }
}
