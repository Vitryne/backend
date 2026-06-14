CREATE TABLE usuario (
                         id          BIGSERIAL PRIMARY KEY,
                         usuario     VARCHAR(50) NOT NULL,
                         email       VARCHAR(255) NOT NULL,
                         senha_hash  VARCHAR(255) NOT NULL,
                         role        VARCHAR(20) NOT NULL,
                         criado_em   TIMESTAMP NOT NULL DEFAULT now(),

                         CONSTRAINT uq_usuario_usuario UNIQUE (usuario),
                         CONSTRAINT uq_usuario_email UNIQUE (email),
                         CONSTRAINT chk_usuario_role CHECK (role IN ('CONSUMER', 'STORE', 'DELIVERER', 'ADMIN'))
);

CREATE INDEX idx_usuario_email ON usuario(email);