-- NFR-TENANT-02: xac nhan truy van so cai dung index (tenant_id, created_at) / (tenant_id, sku_id)
-- va < 500 ms tren 1 trieu dong. Cau SQL tuong ung voi truy van ma StockService / InventoryTransactionRepository sinh ra.
--
--   psql -U postgres -d oism_db -v tenant_id=<uuid tenant> -f explain_ledger_queries.sql

\set ON_ERROR_STOP on
SELECT count(*) AS so_dong_so_cai_cua_tenant FROM inventory_transaction WHERE tenant_id = :'tenant_id';

\echo '--- 1. Trang dau so cai (GET /inventory-transactions?page=0&size=20)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM inventory_transaction
WHERE tenant_id = :'tenant_id'
ORDER BY created_at DESC LIMIT 20;

\echo '--- 2. Dem tong cho phan trang (count cua trang 1)'
EXPLAIN (ANALYZE, COSTS OFF)
SELECT count(*) FROM inventory_transaction WHERE tenant_id = :'tenant_id';

\echo '--- 3. Loc theo SKU (GET /inventory-transactions?skuId=)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM inventory_transaction
WHERE tenant_id = :'tenant_id'
  AND sku_id = (SELECT id FROM sku WHERE tenant_id = :'tenant_id' ORDER BY id LIMIT 1)
ORDER BY created_at DESC LIMIT 20;

\echo '--- 4. Keyset trang sau, lui sau 200 ngay (GET /inventory-transactions/keyset?lastCreatedAt=&lastId=)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM inventory_transaction
WHERE tenant_id = :'tenant_id'
  AND (created_at, id) < (now() - interval '200 days', 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid)
ORDER BY created_at DESC, id DESC LIMIT 20;

\echo '--- 5. Loc theo khoang ngay (from/to 7 ngay)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM inventory_transaction
WHERE tenant_id = :'tenant_id' AND created_at >= now() - interval '7 days' AND created_at < now()
ORDER BY created_at DESC LIMIT 20;
