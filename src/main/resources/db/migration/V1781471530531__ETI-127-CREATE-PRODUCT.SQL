CREATE TABLE IF NOT EXISTS product (
                                       id                          BIGSERIAL PRIMARY KEY,
                                       name                        VARCHAR(255) NOT NULL,
    description                 TEXT,
    price                       NUMERIC(10,2) NOT NULL,
    promotional_price           NUMERIC(10,2),
    promotional_start_date      TIMESTAMP,
    promotional_end_date        TIMESTAMP,
    type                        VARCHAR(50) NOT NULL,
    color                       VARCHAR(50),
    rating                      REAL DEFAULT 0,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    stock_warning_threshold     INTEGER,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    photo_urls                  TEXT[],

    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_product_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT chk_product_promotional_price CHECK (promotional_price IS NULL OR promotional_price <= price),
    CONSTRAINT chk_product_promotional_dates CHECK (
                                                       promotional_start_date IS NULL
                                                       OR promotional_end_date IS NULL
                                                       OR promotional_start_date <= promotional_end_date
                                                   )
    );