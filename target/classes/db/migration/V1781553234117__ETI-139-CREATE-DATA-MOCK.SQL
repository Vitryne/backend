-- ===========================================
-- USUÁRIO DE TESTE
-- ===========================================
-- Senha: "senha123" (hash BCrypt gerado para fins de teste)
INSERT INTO usuario (usuario, email, senha_hash, role, criado_em) VALUES
    ('cliente_teste', 'cliente@vitryne.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CONSUMER', now());


-- ===========================================
-- PRODUTOS
-- ===========================================
INSERT INTO produto (nome, descricao, preco, preco_promocional, tipo, cor, avaliacao, status, fotos_urls) VALUES
                                                                                                              ('Camiseta Oversized Preta', 'Camiseta 100% algodão, modelagem oversized.', 89.90, 69.90, 'camiseta', 'preto', 4.5, 'ATIVO', ARRAY['https://exemplo.com/fotos/camiseta-preta-1.jpg', 'https://exemplo.com/fotos/camiseta-preta-2.jpg']),
                                                                                                              ('Calça Cargo Bege', 'Calça cargo com bolsos laterais, tecido sarja.', 159.90, NULL, 'calca', 'bege', 4.2, 'ATIVO', ARRAY['https://exemplo.com/fotos/calca-cargo-bege.jpg']),
                                                                                                              ('Vestido Midi Floral', 'Vestido midi com estampa floral, manga curta.', 129.90, 99.90, 'vestido', 'floral', 4.8, 'ATIVO', ARRAY['https://exemplo.com/fotos/vestido-floral-1.jpg', 'https://exemplo.com/fotos/vestido-floral-2.jpg']),
                                                                                                              ('Boné Trucker Branco', 'Boné trucker ajustável, tela respirável.', 49.90, NULL, 'acessorio', 'branco', 4.0, 'ATIVO', ARRAY['https://exemplo.com/fotos/bone-branco.jpg']);


-- ===========================================
-- ESTOQUE
-- ===========================================

-- Camiseta Oversized Preta (id 1): P disponível, M esgotado, G disponível
INSERT INTO estoque (produto_id, tamanho, quantidade) VALUES
                                                          (1, 'P', 5),
                                                          (1, 'M', 0),
                                                          (1, 'G', 3);

-- Calça Cargo Bege (id 2): tamanhos numéricos
INSERT INTO estoque (produto_id, tamanho, quantidade) VALUES
                                                          (2, '38', 4),
                                                          (2, '40', 2),
                                                          (2, '42', 0);

-- Vestido Midi Floral (id 3): PP esgotado, M e G disponíveis
INSERT INTO estoque (produto_id, tamanho, quantidade) VALUES
                                                          (3, 'PP', 0),
                                                          (3, 'M', 6),
                                                          (3, 'G', 2);

-- Boné Trucker Branco (id 4): tamanho único
INSERT INTO estoque (produto_id, tamanho, quantidade) VALUES
    (4, 'Único', 10);