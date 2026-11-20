-- V8: Xac thuc webhook san theo API key cua tung tenant (FR-SIM-01).
-- Ke hoach muc 4 - Mo hinh du lieu, bang AUTH channel_credential: channel (SHOPEE/TIKTOK), api_key_hash, active.
-- Chi luu ban bam SHA-256 cua key; key tho chi tra ve mot lan khi Owner sinh key.

CREATE TABLE channel_credential (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenant (id),
    channel      VARCHAR(20) NOT NULL,
    api_key_hash VARCHAR(64) NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_channel_credential_channel CHECK (channel IN ('SHOPEE', 'TIKTOK')),
    CONSTRAINT uq_channel_credential_api_key_hash UNIQUE (api_key_hash)
);
-- Moi tenant x kenh chi co 1 key dang hieu luc; sinh key moi thi key cu bi vo hieu
CREATE UNIQUE INDEX uq_channel_credential_tenant_channel_active
    ON channel_credential (tenant_id, channel) WHERE active;
CREATE INDEX ix_channel_credential_tenant_created ON channel_credential (tenant_id, created_at);
