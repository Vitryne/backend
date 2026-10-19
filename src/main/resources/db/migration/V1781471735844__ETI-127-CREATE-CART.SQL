CREATE TABLE IF NOT EXISTS cart (
                       id                      BIGSERIAL PRIMARY KEY,
                       user_id                 BIGINT NOT NULL,
                       total_value_forecast    NUMERIC(10,2) NOT NULL DEFAULT 0,
                       updated_at              TIMESTAMP NOT NULL DEFAULT now(),

                       CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                       CONSTRAINT uq_cart_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_user_id ON cart(user_id);
