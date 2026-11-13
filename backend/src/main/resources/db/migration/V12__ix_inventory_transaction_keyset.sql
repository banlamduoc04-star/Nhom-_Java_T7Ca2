-- V12: Index cho phan trang keyset so cai (NFR-TENANT-02): ORDER BY created_at DESC, id DESC va
-- dieu kien (created_at, id) < (con tro) dung tron index, khong loc lai tren hang trieu dong.
CREATE INDEX ix_invtx_tenant_created_id ON inventory_transaction (tenant_id, created_at, id);
