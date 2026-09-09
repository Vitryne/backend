CREATE TABLE IF NOT EXISTS users (
                        id            BIGSERIAL PRIMARY KEY,
                        username      VARCHAR(50) NOT NULL,
                        email         VARCHAR(255) NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        role          VARCHAR(20) NOT NULL,
                        created_at    TIMESTAMP NOT NULL DEFAULT now(),

                        CONSTRAINT uq_users_username UNIQUE (username),
                        CONSTRAINT uq_users_email UNIQUE (email),
                        CONSTRAINT chk_users_role CHECK (role IN ('CONSUMER', 'STORE', 'DELIVERER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
