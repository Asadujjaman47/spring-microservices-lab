CREATE TABLE orders (
    id                  UUID         PRIMARY KEY,
    user_id             UUID         NOT NULL,
    product_id          UUID         NOT NULL,
    quantity            INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price_cents    BIGINT       NOT NULL CHECK (unit_price_cents >= 0),
    total_price_cents   BIGINT       NOT NULL CHECK (total_price_cents >= 0),
    status              VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_product_id ON orders (product_id);