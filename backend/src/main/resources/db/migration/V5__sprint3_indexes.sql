-- V5: Sprint 3 bo sung index toi uu truy van theo tenant, thoi gian, chi nhanh (NFR-TENANT-02)

CREATE INDEX IF NOT EXISTS ix_order_item_tenant_created ON order_item (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS ix_orders_tenant_branch ON orders (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_orders_tenant_channel ON orders (tenant_id, channel);
CREATE INDEX IF NOT EXISTS ix_stock_balance_tenant_branch ON stock_balance (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_stock_transfer_tenant_status ON stock_transfer (tenant_id, status);
CREATE INDEX IF NOT EXISTS ix_stocktake_tenant_branch ON stocktake (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_stocktake_item_tenant_sku ON stocktake_item (tenant_id, sku_id);
CREATE INDEX IF NOT EXISTS ix_stock_transfer_item_tenant_sku ON stock_transfer_item (tenant_id, sku_id);
