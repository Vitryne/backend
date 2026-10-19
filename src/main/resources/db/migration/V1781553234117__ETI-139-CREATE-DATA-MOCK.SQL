-- ===========================================
-- MOCK SEED DATA
-- ===========================================
-- Guarded by a single check so this seed only runs once, even if the
-- Flyway history is lost/reset without the underlying data being wiped.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'cliente_teste') THEN

        -- Password: "senha123" (BCrypt hash generated for testing purposes)
        INSERT INTO users (username, email, password_hash, role, created_at) VALUES
            ('cliente_teste', 'cliente@vitryne.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CONSUMER', now());

        INSERT INTO product (name, description, price, promotional_price, type, color, rating, status, photo_urls) VALUES
            ('Camiseta Oversized Preta', 'Camiseta 100% algodão, modelagem oversized.', 89.90, 69.90, 'camiseta', 'preto', 4.5, 'ACTIVE', ARRAY['https://exemplo.com/fotos/camiseta-preta-1.jpg', 'https://exemplo.com/fotos/camiseta-preta-2.jpg']),
            ('Calça Cargo Bege', 'Calça cargo com bolsos laterais, tecido sarja.', 159.90, NULL, 'calca', 'bege', 4.2, 'ACTIVE', ARRAY['https://exemplo.com/fotos/calca-cargo-bege.jpg']),
            ('Vestido Midi Floral', 'Vestido midi com estampa floral, manga curta.', 129.90, 99.90, 'vestido', 'floral', 4.8, 'ACTIVE', ARRAY['https://exemplo.com/fotos/vestido-floral-1.jpg', 'https://exemplo.com/fotos/vestido-floral-2.jpg']),
            ('Boné Trucker Branco', 'Boné trucker ajustável, tela respirável.', 49.90, NULL, 'acessorio', 'branco', 4.0, 'ACTIVE', ARRAY['https://exemplo.com/fotos/bone-branco.jpg']);

        -- Camiseta Oversized Preta (id 1): P available, M out of stock, G available
        INSERT INTO stock (product_id, size, quantity) VALUES
            (1, 'P', 5),
            (1, 'M', 0),
            (1, 'G', 3);

        -- Calça Cargo Bege (id 2): numeric sizes
        INSERT INTO stock (product_id, size, quantity) VALUES
            (2, '38', 4),
            (2, '40', 2),
            (2, '42', 0);

        -- Vestido Midi Floral (id 3): PP out of stock, M and G available
        INSERT INTO stock (product_id, size, quantity) VALUES
            (3, 'PP', 0),
            (3, 'M', 6),
            (3, 'G', 2);

        -- Boné Trucker Branco (id 4): single size
        INSERT INTO stock (product_id, size, quantity) VALUES
            (4, 'Único', 10);

    END IF;
END $$;
