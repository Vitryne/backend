ALTER TABLE users
    ALTER COLUMN username DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower
    ON users (LOWER(email));


CREATE TABLE IF NOT EXISTS client (
                        id              BIGSERIAL PRIMARY KEY,
                        user_id         BIGINT NOT NULL,
                        name            VARCHAR(150) NOT NULL,
                        cpf             VARCHAR(11) NOT NULL,

                        zip_code        VARCHAR(8) NOT NULL,
                        street          VARCHAR(200) NOT NULL,
                        address_number  VARCHAR(20) NOT NULL,
                        neighborhood    VARCHAR(100) NOT NULL,
                        complement      VARCHAR(150),
                        city            VARCHAR(100) NOT NULL,
                        state           VARCHAR(2) NOT NULL,

                        CONSTRAINT uq_client_user UNIQUE (user_id),
                        CONSTRAINT uq_client_cpf UNIQUE (cpf),

                        CONSTRAINT fk_client_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
);


CREATE TABLE IF NOT EXISTS store_bank_account (
                                    id                BIGSERIAL PRIMARY KEY,
                                    holder_name       VARCHAR(150) NOT NULL,
                                    holder_document   VARCHAR(14) NOT NULL,
                                    account_type      VARCHAR(20) NOT NULL,
                                    bank_code         VARCHAR(20) NOT NULL,
                                    agency            VARCHAR(20) NOT NULL,
                                    account_number    VARCHAR(30) NOT NULL,
                                    pix_key           VARCHAR(255),

                                    CONSTRAINT chk_bank_account_type
                                        CHECK (account_type IN ('CHECKING', 'SAVINGS', 'PAYMENT'))
);


ALTER TABLE store
ALTER COLUMN description TYPE VARCHAR(2000);

ALTER TABLE store
    ADD COLUMN cnpj VARCHAR(14),
    ADD COLUMN phone VARCHAR(25),
    ADD COLUMN legal_name VARCHAR(255),

    ADD COLUMN zip_code VARCHAR(8),
    ADD COLUMN street VARCHAR(200),
    ADD COLUMN address_number VARCHAR(20),
    ADD COLUMN neighborhood VARCHAR(100),
    ADD COLUMN complement VARCHAR(150),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN state VARCHAR(2),

    ADD COLUMN bank_account_id BIGINT,
    ADD COLUMN main_category VARCHAR(100),
    ADD COLUMN delivery_radius_km NUMERIC(6,2),
    ADD COLUMN preparation_time_minutes INTEGER,
    ADD COLUMN photo_url VARCHAR(2048),
    ADD COLUMN banner_url VARCHAR(2048);

ALTER TABLE store
    ADD CONSTRAINT uq_store_cnpj UNIQUE (cnpj),

    ADD CONSTRAINT uq_store_bank_account UNIQUE (bank_account_id),

    ADD CONSTRAINT fk_store_bank_account
        FOREIGN KEY (bank_account_id)
        REFERENCES store_bank_account(id),

    ADD CONSTRAINT chk_store_delivery_radius
        CHECK (delivery_radius_km > 0),

    ADD CONSTRAINT chk_store_preparation_time
        CHECK (preparation_time_minutes BETWEEN 1 AND 1440);


CREATE TABLE IF NOT EXISTS store_tag (
                           store_id BIGINT NOT NULL,
                           tag      VARCHAR(50) NOT NULL,

                           PRIMARY KEY (store_id, tag),

                           CONSTRAINT fk_store_tag_store
                               FOREIGN KEY (store_id) REFERENCES store(id)
);


CREATE TABLE IF NOT EXISTS store_opening_period (
                                      store_id     BIGINT NOT NULL,
                                      day_of_week  VARCHAR(9) NOT NULL,
                                      opens_at     TIME NOT NULL,
                                      closes_at    TIME NOT NULL,

                                      PRIMARY KEY (store_id, day_of_week, opens_at),

                                      CONSTRAINT fk_store_opening_period_store
                                          FOREIGN KEY (store_id) REFERENCES store(id),

                                      CONSTRAINT chk_store_opening_day
                                          CHECK (day_of_week IN (
                                                                 'MONDAY',
                                                                 'TUESDAY',
                                                                 'WEDNESDAY',
                                                                 'THURSDAY',
                                                                 'FRIDAY',
                                                                 'SATURDAY',
                                                                 'SUNDAY'
                                              )),

                                      CONSTRAINT chk_store_opening_interval
                                          CHECK (opens_at < closes_at)
);