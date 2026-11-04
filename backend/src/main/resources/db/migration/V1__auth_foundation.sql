-- V1: Nen tang AUTH (FR-AUTH-01..05): tenant, branch, app_user, refresh_token
-- Quy uoc (Ke hoach du an OISM, muc 4): id UUID; moi bang nghiep vu co tenant_id, created_at, updated_at.
-- Bang tenant khong co tenant_id. Cac module khac (PROD, INV, ORD, REP) them migration V2, V3... rieng.

CREATE TABLE tenant (
    id                  UUID         PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    reservation_minutes INTEGER      NOT NULL DEFAULT 30,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tenant_status      CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_tenant_reservation CHECK (reservation_minutes > 0)
);

CREATE TABLE branch (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenant (id),
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    address     VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_branch_type CHECK (type IN ('STORE', 'WAREHOUSE')),
    CONSTRAINT uq_branch_tenant_name UNIQUE (tenant_id, name)
);
CREATE INDEX ix_branch_tenant_created ON branch (tenant_id, created_at);

CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL REFERENCES tenant (id),
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    branch_id     UUID         REFERENCES branch (id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_app_user_role CHECK (role IN ('OWNER', 'STAFF', 'CASHIER'))
);
-- Email duy nhat toan he thong (khong phan biet hoa thuong); so dien thoai duy nhat de dang nhap khong nham
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));
CREATE UNIQUE INDEX uq_app_user_phone ON app_user (phone) WHERE phone IS NOT NULL;
CREATE INDEX ix_app_user_tenant_created ON app_user (tenant_id, created_at);

CREATE TABLE refresh_token (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenant (id),
    user_id     UUID         NOT NULL REFERENCES app_user (id),
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);
CREATE INDEX ix_refresh_token_tenant_created ON refresh_token (tenant_id, created_at);
