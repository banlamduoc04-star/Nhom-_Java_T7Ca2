-- V4: Module ORD (FR-ORD-01..03): orders, order_item, order_status_history, payment

CREATE TABLE orders (
    id                UUID           PRIMARY KEY,
    tenant_id         UUID           NOT NULL REFERENCES tenant (id),
    branch_id         UUID           NOT NULL REFERENCES branch (id),
    code              VARCHAR(40)    NOT NULL,
    channel           VARCHAR(20)    NOT NULL,
    external_order_id VARCHAR(100),
    status            VARCHAR(20)    NOT NULL,
    customer_name     VARCHAR(255),
    customer_phone    VARCHAR(20),
    shipping_address  VARCHAR(500),
    total_amount      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    reserved_until    TIMESTAMPTZ,
    created_by        UUID,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_orders_channel CHECK (channel IN ('POS', 'MANUAL', 'SHOPEE', 'TIKTOK')),
    CONSTRAINT ck_orders_status CHECK (status IN ('DRAFT', 'RESERVED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT uq_orders_code UNIQUE (tenant_id, code)
);
-- Chong trung don san: cung kenh + cung ma don ngoai (chi ap dung khi co ma ngoai)
CREATE UNIQUE INDEX uq_orders_external ON orders (tenant_id, channel, external_order_id)
    WHERE external_order_id IS NOT NULL;
CREATE INDEX ix_orders_tenant_created ON orders (tenant_id, created_at);
CREATE INDEX ix_orders_tenant_status ON orders (tenant_id, status);

CREATE TABLE order_item (
    id         UUID           PRIMARY KEY,
    tenant_id  UUID           NOT NULL REFERENCES tenant (id),
    order_id   UUID           NOT NULL REFERENCES orders (id),
    sku_id     UUID           NOT NULL REFERENCES sku (id),
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    cost_price NUMERIC(19, 2),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_item_qty CHECK (quantity > 0),
    CONSTRAINT ck_order_item_price CHECK (unit_price >= 0)
);
CREATE INDEX ix_order_item_order ON order_item (order_id);
CREATE INDEX ix_order_item_tenant_sku ON order_item (tenant_id, sku_id);

CREATE TABLE order_status_history (
    id          UUID        PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    order_id    UUID        NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_by  UUID,
    reason      VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_order_history_order ON order_status_history (order_id);

CREATE TABLE payment (
    id            UUID           PRIMARY KEY,
    tenant_id     UUID           NOT NULL REFERENCES tenant (id),
    order_id      UUID           NOT NULL REFERENCES orders (id),
    method        VARCHAR(10)    NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL,
    cash_received NUMERIC(19, 2),
    change_amount NUMERIC(19, 2),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_method CHECK (method IN ('CASH', 'QR')),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
);
CREATE INDEX ix_payment_order ON payment (order_id);
