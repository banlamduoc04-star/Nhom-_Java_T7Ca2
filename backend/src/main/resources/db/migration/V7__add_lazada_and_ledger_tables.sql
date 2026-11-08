ALTER TABLE orders DROP CONSTRAINT IF EXISTS ck_orders_channel;
ALTER TABLE orders ADD CONSTRAINT ck_orders_channel CHECK (channel IN ('POS', 'MANUAL', 'SHOPEE', 'TIKTOK', 'LAZADA'));

CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    quantity BIGINT NOT NULL,
    average_unit_cost NUMERIC(18, 4) NOT NULL,
    version BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_tenant_sku UNIQUE (tenant_id, sku_code)
);
CREATE INDEX IF NOT EXISTS idx_tenant_sku ON inventory_items (tenant_id, sku_code);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    debit_amount NUMERIC(18, 4) NOT NULL,
    credit_amount NUMERIC(18, 4) NOT NULL,
    balance_after NUMERIC(18, 4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tenant_account_created ON ledger_entries (tenant_id, account_id, created_at DESC, id DESC);
