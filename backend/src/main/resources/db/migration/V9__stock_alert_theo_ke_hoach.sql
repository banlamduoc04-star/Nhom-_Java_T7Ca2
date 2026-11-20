-- V9: Dua bang stock_alert ve dung mo hinh du lieu cua ke hoach (FR-REP-03).
-- Ke hoach muc 4: stock_alert(branch_id, sku_id, available, threshold, status OPEN/CLOSED),
-- "Moi (branch_id, sku_id) toi da 1 canh bao OPEN". Chuan muc 5: ten rang buoc ck_/uq_/ix_.

-- 1. Trang thai RESOLVED -> CLOSED
UPDATE stock_alert SET status = 'CLOSED' WHERE status = 'RESOLVED';

-- 2. Du lieu cu co the co nhieu canh bao OPEN cho cung (branch, sku): giu canh bao moi nhat, dong phan con lai
UPDATE stock_alert a
SET status = 'CLOSED', resolved_at = now(), updated_at = now()
WHERE a.status = 'OPEN'
  AND EXISTS (SELECT 1
              FROM stock_alert b
              WHERE b.tenant_id = a.tenant_id
                AND b.branch_id = a.branch_id
                AND b.sku_id = a.sku_id
                AND b.status = 'OPEN'
                AND (b.created_at, b.id) > (a.created_at, a.id));

-- 3. Ten cot theo ke hoach
ALTER TABLE stock_alert RENAME COLUMN available_at_alert TO available;
ALTER TABLE stock_alert RENAME COLUMN resolved_at TO closed_at;

-- 4. Rang buoc
ALTER TABLE stock_alert ADD CONSTRAINT ck_stock_alert_status CHECK (status IN ('OPEN', 'CLOSED'));
CREATE UNIQUE INDEX uq_stock_alert_open ON stock_alert (tenant_id, branch_id, sku_id) WHERE status = 'OPEN';

-- 5. Index dat ten theo chuan; bang chinh co (tenant_id, created_at), bang lien quan SKU co (tenant_id, sku_id)
DROP INDEX IF EXISTS idx_stock_alert_tenant_status;
DROP INDEX IF EXISTS idx_stock_alert_tenant_sku_branch;
CREATE INDEX ix_stock_alert_tenant_status ON stock_alert (tenant_id, status);
CREATE INDEX ix_stock_alert_tenant_sku ON stock_alert (tenant_id, sku_id);
CREATE INDEX ix_stock_alert_tenant_created ON stock_alert (tenant_id, created_at);
