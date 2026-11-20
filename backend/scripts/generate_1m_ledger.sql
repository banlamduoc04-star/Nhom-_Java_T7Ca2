-- Sinh 1.000.000 dong so cai inventory_transaction de do hieu nang (NFR-TENANT-02, NFR-PERF-01).
-- CHI DUNG TREN CSDL KIEM THU HIEU NANG, KHONG CHAY TREN DU LIEU THAT: cac dong nay khong cap nhat stock_balance.
--
-- Cach chay (tenant phai co san chi nhanh va SKU, vd tao qua API /auth/register + /products):
--   psql -U postgres -d oism_db -v tenant_id=<uuid tenant> -f generate_1m_ledger.sql
--
-- Du lieu: chia deu cho moi chi nhanh x SKU cua tenant, created_at rai trong 365 ngay gan nhat,
-- ly do hop le theo ck_invtx_reason (PURCHASE/SALE/RETURN/ADJUST/TRANSFER_OUT/TRANSFER_IN).

\set ON_ERROR_STOP on
SELECT set_config('oism.perf_tenant', :'tenant_id', false);

DO $$
DECLARE
    v_tenant UUID := current_setting('oism.perf_tenant')::uuid;
    v_user UUID;
    v_branches UUID[];
    v_skus UUID[];
    v_total INT := 1000000;
    v_batch INT := 100000;
BEGIN
    SELECT array_agg(id ORDER BY id) INTO v_branches FROM branch WHERE tenant_id = v_tenant;
    SELECT array_agg(id ORDER BY id) INTO v_skus FROM sku WHERE tenant_id = v_tenant;
    SELECT id INTO v_user FROM app_user WHERE tenant_id = v_tenant AND role = 'OWNER' LIMIT 1;
    IF v_branches IS NULL OR v_skus IS NULL THEN
        RAISE EXCEPTION 'Tenant % chua co chi nhanh hoac SKU', v_tenant;
    END IF;

    FOR b IN 0..(v_total / v_batch - 1) LOOP
        INSERT INTO inventory_transaction (id, tenant_id, branch_id, sku_id, direction, reason, quantity,
                                           balance_after, unit_cost, ref_type, ref_id, created_by, created_at)
        SELECT gen_random_uuid(),
               v_tenant,
               v_branches[1 + (n % array_length(v_branches, 1))],
               v_skus[1 + ((n / 7) % array_length(v_skus, 1))],
               CASE WHEN n % 3 = 0 THEN 'OUT' ELSE 'IN' END,
               (ARRAY['PURCHASE', 'RETURN', 'ADJUST', 'TRANSFER_IN', 'SALE', 'TRANSFER_OUT'])
                   [1 + CASE WHEN n % 3 = 0 THEN 4 + (n % 2) ELSE n % 4 END],
               1 + (n % 10),
               100 + (n % 900),
               60000.00,
               'BENCHMARK',
               gen_random_uuid(),
               v_user,
               now() - make_interval(secs => (n::double precision * 31536000 / v_total))
        FROM generate_series(b * v_batch, (b + 1) * v_batch - 1) AS n;
        RAISE NOTICE 'Da chen % / % dong', (b + 1) * v_batch, v_total;
    END LOOP;
END $$;

ANALYZE inventory_transaction;
