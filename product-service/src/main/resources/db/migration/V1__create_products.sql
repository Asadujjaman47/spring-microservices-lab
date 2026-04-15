CREATE TABLE products (
    id              UUID           PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    price_cents     BIGINT         NOT NULL CHECK (price_cents >= 0),
    stock           INTEGER        NOT NULL CHECK (stock >= 0),
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_products_name ON products (name);
