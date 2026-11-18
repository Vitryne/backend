-- Endereços de entrega do cliente (antes, um único endereço nas colunas de client).
CREATE TABLE IF NOT EXISTS client_address (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT NOT NULL,
    zip_code        VARCHAR(8) NOT NULL,
    street          VARCHAR(200) NOT NULL,
    address_number  VARCHAR(20) NOT NULL,
    neighborhood    VARCHAR(100) NOT NULL,
    complement      VARCHAR(150),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_client_address_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_address_client_id ON client_address(client_id);

-- Endereço selecionado: usado na navegação e como entrega na revisão/checkout.
ALTER TABLE client
    ADD COLUMN IF NOT EXISTS selected_address_id BIGINT;

ALTER TABLE client
    ADD CONSTRAINT fk_client_selected_address
        FOREIGN KEY (selected_address_id) REFERENCES client_address(id);

-- O endereço do cadastro passa a ser o primeiro da lista e o selecionado.
INSERT INTO client_address (client_id, zip_code, street, address_number, neighborhood, complement, city, state)
SELECT c.id, c.zip_code, c.street, c.address_number, c.neighborhood, c.complement, c.city, c.state
FROM client c
WHERE c.zip_code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM client_address ca WHERE ca.client_id = c.id)
ORDER BY c.id;

UPDATE client c
SET selected_address_id = first_address.id
FROM (
    SELECT DISTINCT ON (client_id) client_id, id
    FROM client_address
    ORDER BY client_id, id
) first_address
WHERE first_address.client_id = c.id
  AND c.selected_address_id IS NULL;

-- Colunas antigas mantidas por enquanto (sem uso); remoção em task futura.
ALTER TABLE client
    ALTER COLUMN zip_code       DROP NOT NULL,
    ALTER COLUMN street         DROP NOT NULL,
    ALTER COLUMN address_number DROP NOT NULL,
    ALTER COLUMN neighborhood   DROP NOT NULL,
    ALTER COLUMN city           DROP NOT NULL,
    ALTER COLUMN state          DROP NOT NULL;
