-- V3: Module INV (FR-INV-01..05): ton kho, so cai chi ghi them, nha cung cap, phieu nhap, chuyen kho, kiem ke

CREATE TABLE stock_balance (
    id         UUID        PRIMARY KEY,
    tenant_id  UUID        NOT NULL REFERENCES tenant (id),
    branch_id  UUID        NOT NULL REFERENCES branch (id),
    sku_id     UUID        NOT NULL REFERENCES sku (id),
    on_hand    INTEGER     NOT NULL DEFAULT 0,
    reserved   INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_balance UNIQUE (tenant_id, branch_id, sku_id),
    CONSTRAINT ck_stock_balance CHECK (on_hand >= reserved AND reserved >= 0)
);
CREATE INDEX ix_stock_balance_tenant_sku ON stock_balance (tenant_id, sku_id);

-- So cai: moi bien dong mot dong, khong sua/xoa (NFR-SEC-03); chi co created_at
CREATE TABLE inventory_transaction (
    id            UUID           PRIMARY KEY,
    tenant_id     UUID           NOT NULL REFERENCES tenant (id),
    branch_id     UUID           NOT NULL REFERENCES branch (id),
    sku_id        UUID           NOT NULL REFERENCES sku (id),
    direction     VARCHAR(3)     NOT NULL,
    reason        VARCHAR(20)    NOT NULL,
    quantity      INTEGER        NOT NULL,
    balance_after INTEGER        NOT NULL,
    unit_cost     NUMERIC(19, 2),
    ref_type      VARCHAR(30),
    ref_id        UUID,
    created_by    UUID,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_invtx_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_invtx_reason CHECK (reason IN ('PURCHASE', 'SALE', 'RETURN', 'ADJUST', 'TRANSFER_OUT', 'TRANSFER_IN')),
    CONSTRAINT ck_invtx_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invtx_balance CHECK (balance_after >= 0)
);
CREATE INDEX ix_invtx_tenant_created ON inventory_transaction (tenant_id, created_at);
CREATE INDEX ix_invtx_tenant_sku ON inventory_transaction (tenant_id, sku_id);

CREATE FUNCTION forbid_inventory_transaction_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'inventory_transaction chi duoc ghi them; sua sai bang but toan bu (% bi tu choi)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invtx_append_only
    BEFORE UPDATE OR DELETE ON inventory_transaction
    FOR EACH ROW EXECUTE FUNCTION forbid_inventory_transaction_change();

CREATE TABLE supplier (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant (id),
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    address    VARCHAR(500),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_supplier_tenant_created ON supplier (tenant_id, created_at);

CREATE TABLE purchase_receipt (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenant (id),
    branch_id    UUID        NOT NULL REFERENCES branch (id),
    supplier_id  UUID        REFERENCES supplier (id),
    code         VARCHAR(40) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID,
    created_by   UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_purchase_receipt_status CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT uq_purchase_receipt_code UNIQUE (tenant_id, code)
);
CREATE INDEX ix_purchase_receipt_tenant_created ON purchase_receipt (tenant_id, created_at);

CREATE TABLE purchase_receipt_item (
    id         UUID           PRIMARY KEY,
    tenant_id  UUID           NOT NULL REFERENCES tenant (id),
    receipt_id UUID           NOT NULL REFERENCES purchase_receipt (id),
    sku_id     UUID           NOT NULL REFERENCES sku (id),
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_pri_quantity CHECK (quantity > 0),
    CONSTRAINT ck_pri_price CHECK (unit_price >= 0)
);
CREATE INDEX ix_pri_receipt ON purchase_receipt_item (receipt_id);
CREATE INDEX ix_pri_tenant_sku ON purchase_receipt_item (tenant_id, sku_id);

-- Chuyen kho, kiem ke: bang tao san, nghiep vu lam o Sprint 3
CREATE TABLE stock_transfer (
    id             UUID        PRIMARY KEY,
    tenant_id      UUID        NOT NULL REFERENCES tenant (id),
    from_branch_id UUID        NOT NULL REFERENCES branch (id),
    to_branch_id   UUID        NOT NULL REFERENCES branch (id),
    code           VARCHAR(40) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfer_status CHECK (status IN ('DRAFT', 'IN_TRANSIT', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_transfer_branches CHECK (from_branch_id <> to_branch_id),
    CONSTRAINT uq_stock_transfer_code UNIQUE (tenant_id, code)
);
CREATE INDEX ix_stock_transfer_tenant_created ON stock_transfer (tenant_id, created_at);

CREATE TABLE stock_transfer_item (
    id          UUID        PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    transfer_id UUID        NOT NULL REFERENCES stock_transfer (id),
    sku_id      UUID        NOT NULL REFERENCES sku (id),
    quantity    INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfer_item_qty CHECK (quantity > 0)
);

CREATE TABLE stocktake (
    id          UUID        PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    branch_id   UUID        NOT NULL REFERENCES branch (id),
    code        VARCHAR(40) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    approved_by UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_stocktake_status CHECK (status IN ('OPEN', 'APPROVED')),
    CONSTRAINT uq_stocktake_code UNIQUE (tenant_id, code)
);
CREATE INDEX ix_stocktake_tenant_created ON stocktake (tenant_id, created_at);

CREATE TABLE stocktake_item (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenant (id),
    stocktake_id UUID        NOT NULL REFERENCES stocktake (id),
    sku_id       UUID        NOT NULL REFERENCES sku (id),
    system_qty   INTEGER     NOT NULL,
    counted_qty  INTEGER,
    difference   INTEGER,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
