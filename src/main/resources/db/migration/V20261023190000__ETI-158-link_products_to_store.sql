ALTER TABLE product
    ADD COLUMN store_id BIGINT;

ALTER TABLE product
    ADD CONSTRAINT fk_product_store
        FOREIGN KEY (store_id)
        REFERENCES store(id);

CREATE INDEX idx_product_store_id ON product(store_id);

DO $$
DECLARE
    v_owner_id BIGINT;
    v_store_id BIGINT;
BEGIN
    INSERT INTO users (
        username,
        email,
        password_hash,
        role,
        active
    )
    VALUES (
        'lojista_teste',
        'lojista@vitryne.com',
        'PASSWORD_NOT_CONFIGURED',
        'STORE',
        FALSE
    )
    RETURNING id INTO v_owner_id;

    INSERT INTO store (
        owner_id,
        name,
        description,
        active
    )
    VALUES (
        v_owner_id,
        'Loja Vitryne Teste',
        'Loja fictícia para demonstração do MVP.',
        TRUE
    )
    RETURNING id INTO v_store_id;

    UPDATE product
       SET store_id = v_store_id
     WHERE store_id IS NULL
       AND name IN (
           'Camiseta Oversized Preta',
           'Calça Cargo Bege',
           'Vestido Midi Floral',
           'Boné Trucker Branco'
       );
END $$;


ALTER TABLE product
    ALTER COLUMN store_id SET NOT NULL;