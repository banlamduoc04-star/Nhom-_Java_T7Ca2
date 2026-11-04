-- V2: Module PROD (FR-PROD-01..05): category, brand, product, sku

CREATE TABLE category (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant (id),
    parent_id  UUID         REFERENCES category (id),
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Ten duy nhat trong cung cap (parent_id NULL = cap goc)
CREATE UNIQUE INDEX uq_category_tenant_parent_name
    ON category (tenant_id, COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name));
CREATE INDEX ix_category_tenant_created ON category (tenant_id, created_at);

CREATE TABLE brand (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant (id),
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_brand_tenant_name ON brand (tenant_id, lower(name));
CREATE INDEX ix_brand_tenant_created ON brand (tenant_id, created_at);

CREATE TABLE product (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES tenant (id),
    category_id  UUID         REFERENCES category (id),
    brand_id     UUID         REFERENCES brand (id),
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    has_variants BOOLEAN      NOT NULL DEFAULT FALSE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_product_tenant_created ON product (tenant_id, created_at);
CREATE INDEX ix_product_tenant_category ON product (tenant_id, category_id);
CREATE INDEX ix_product_tenant_brand ON product (tenant_id, brand_id);

CREATE TABLE sku (
    id              UUID           PRIMARY KEY,
    tenant_id       UUID           NOT NULL REFERENCES tenant (id),
    product_id      UUID           NOT NULL REFERENCES product (id),
    code            VARCHAR(64)    NOT NULL,
    attributes      JSONB          NOT NULL DEFAULT '{}'::jsonb,
    barcode         VARCHAR(64),
    retail_price    NUMERIC(19, 2) NOT NULL,
    wholesale_price NUMERIC(19, 2),
    avg_cost        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    reorder_point   INTEGER        NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_sku_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_sku_retail CHECK (retail_price > 0),
    CONSTRAINT ck_sku_wholesale CHECK (wholesale_price IS NULL OR wholesale_price > 0),
    CONSTRAINT ck_sku_cost CHECK (avg_cost >= 0),
    CONSTRAINT ck_sku_reorder CHECK (reorder_point >= 0),
    CONSTRAINT uq_sku_tenant_code UNIQUE (tenant_id, code)
);
CREATE UNIQUE INDEX uq_sku_tenant_barcode ON sku (tenant_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX ix_sku_tenant_created ON sku (tenant_id, created_at);
CREATE INDEX ix_sku_product ON sku (product_id);
