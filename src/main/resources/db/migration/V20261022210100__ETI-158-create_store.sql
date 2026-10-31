CREATE TABLE IF NOT EXISTS store (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_store_owner
        UNIQUE (owner_id),

    CONSTRAINT fk_store_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id),

    CONSTRAINT chk_store_name
        CHECK (LENGTH(TRIM(name)) > 0)
);