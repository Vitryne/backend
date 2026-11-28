-- Imagens enviadas ao armazenamento externo (Cloudinary).
-- product_id nulo = imagem livre: ainda não usada em produto ou removida dele.
-- orphaned_since marca desde quando está livre; a rotina de limpeza apaga as
-- livres há mais tempo que o prazo configurado.
CREATE TABLE uploaded_image (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT        NOT NULL REFERENCES store (id) ON DELETE CASCADE,
    product_id      BIGINT        REFERENCES product (id) ON DELETE SET NULL,
    public_id       VARCHAR(255)  NOT NULL UNIQUE,
    url             VARCHAR(2048) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    orphaned_since  TIMESTAMPTZ
);

CREATE INDEX idx_uploaded_image_product ON uploaded_image (product_id);
CREATE INDEX idx_uploaded_image_orphan ON uploaded_image (orphaned_since) WHERE product_id IS NULL;
