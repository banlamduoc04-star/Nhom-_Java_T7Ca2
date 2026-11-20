-- Sinh 100.000 don (CONFIRMED/COMPLETED) kem dong hang de do bao cao < 2 s (NFR-PERF-01: "duoi 100.000 ban ghi").
-- CHI DUNG TREN CSDL KIEM THU HIEU NANG: khong tru kho, khong ghi so cai.
--
--   psql -U postgres -d oism_db -v tenant_id=<uuid tenant> -f generate_100k_orders.sql

\set ON_ERROR_STOP on
SELECT set_config('oism.perf_tenant', :'tenant_id', false);

DO $$
DECLARE
    v_tenant UUID := current_setting('oism.perf_tenant')::uuid;
    v_user UUID;
    v_branches UUID[];
    v_skus UUID[];
    v_total INT := 100000;
BEGIN
    SELECT array_agg(id ORDER BY id) INTO v_branches FROM branch WHERE tenant_id = v_tenant;
    SELECT array_agg(id ORDER BY id) INTO v_skus FROM sku WHERE tenant_id = v_tenant;
    SELECT id INTO v_user FROM app_user WHERE tenant_id = v_tenant AND role = 'OWNER' LIMIT 1;

    CREATE TEMP TABLE perf_orders ON COMMIT DROP AS
    SELECT gen_random_uuid() AS id, n,
           v_branches[1 + (n % array_length(v_branches, 1))] AS branch_id,
           (ARRAY['POS', 'MANUAL', 'SHOPEE', 'TIKTOK'])[1 + (n % 4)] AS channel,
           now() - make_interval(secs => (n::double precision * 7776000 / v_total)) AS created_at -- 90 ngay
    FROM generate_series(1, v_total) AS n;

    INSERT INTO orders (id, tenant_id, branch_id, code, channel, external_order_id, status, total_amount,
                        created_by, created_at, updated_at)
    SELECT id, v_tenant, branch_id, 'PERF-' || lpad(n::text, 7, '0'), channel,
           CASE WHEN channel IN ('SHOPEE', 'TIKTOK') THEN 'PERF-EXT-' || n END,
           CASE WHEN n % 2 = 0 THEN 'COMPLETED' ELSE 'CONFIRMED' END,
           200000.00, v_user, created_at, created_at
    FROM perf_orders;

    -- moi don 2 dong hang
    INSERT INTO order_item (id, tenant_id, order_id, sku_id, quantity, unit_price, cost_price, created_at, updated_at)
    SELECT gen_random_uuid(), v_tenant, o.id, v_skus[1 + ((o.n * 2 + k) % array_length(v_skus, 1))],
           1, 100000.00, 60000.00, o.created_at, o.created_at
    FROM perf_orders o CROSS JOIN generate_series(0, 1) AS k;
END $$;

ANALYZE orders;
ANALYZE order_item;
