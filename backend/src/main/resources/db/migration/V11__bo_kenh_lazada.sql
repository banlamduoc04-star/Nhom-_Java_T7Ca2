-- V11: Bo kenh LAZADA them o V7. Ke hoach muc 4: orders.channel chi gom POS/MANUAL/SHOPEE/TIKTOK
-- (FR-ORD-01: don tu POS, Admin tao tay, webhook Shopee/TikTok Shop). Quyet dinh nhom ngay 11/11/2026.

-- Du lieu cu (chi co o moi truong dev/demo: seeder va Simulator) chuyen ve kenh MANUAL de giu nguyen
-- don, so cai va bao cao; ma don ngoai giu lai de truy vet.
UPDATE orders SET channel = 'MANUAL', updated_at = now() WHERE channel = 'LAZADA';
-- daily_report la so lieu tong hop: xoa dong LAZADA (cap nhat thanh MANUAL co the trung unique voi dong MANUAL
-- cung ngay); chay lai POST /reports/daily/run cho ngay do neu can so lieu chinh xac.
DELETE FROM daily_report WHERE channel = 'LAZADA';

ALTER TABLE orders DROP CONSTRAINT IF EXISTS ck_orders_channel;
ALTER TABLE orders ADD CONSTRAINT ck_orders_channel CHECK (channel IN ('POS', 'MANUAL', 'SHOPEE', 'TIKTOK'));

-- daily_report.channel cung theo cung tap kenh voi orders
ALTER TABLE daily_report ADD CONSTRAINT ck_daily_report_channel CHECK (channel IN ('POS', 'MANUAL', 'SHOPEE', 'TIKTOK'));
