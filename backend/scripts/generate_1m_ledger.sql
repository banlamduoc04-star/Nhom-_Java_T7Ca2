-- Script sinh 1 triệu dòng sổ cái tồn kho để kiểm thử hiệu năng (NFR-PERF-01, NFR-TENANT-02, JC-30)
-- Sử dụng: \i generate_1m_ledger.sql hoặc chạy trong psql/DBeaver

DO $$
DECLARE
    v_tenant_id UUID;
    v_branch_id UUID;
    v_sku_id UUID;
    v_user_id UUID;
    v_batch_size INT := 50000;
    v_total INT := 1000000;
    v_iterations INT;
BEGIN
    -- Lấy tenant mẫu hoặc tạo mới nếu chưa có
    SELECT id INTO v_tenant_id FROM tenant LIMIT 1;
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy tenant trong CSDL. Vui lòng chạy ứng dụng để nạp seed data trước.';
    END IF;

    SELECT id INTO v_branch_id FROM branch WHERE tenant_id = v_tenant_id LIMIT 1;
    SELECT id INTO v_sku_id FROM sku WHERE tenant_id = v_tenant_id LIMIT 1;
    SELECT id INTO v_user_id FROM app_user WHERE tenant_id = v_tenant_id LIMIT 1;

    RAISE NOTICE 'Bắt đầu sinh % dòng sổ cái cho tenant % ...', v_total, v_tenant_id;

    v_iterations := v_total / v_batch_size;

    FOR i IN 1..v_iterations LOOP
        INSERT INTO inventory_transaction (
            id, tenant_id, branch_id, sku_id, direction, reason, quantity,
            balance_after, unit_cost, ref_type, ref_id, created_by, created_at
        )
        SELECT
            gen_random_uuid(),
            v_tenant_id,
            v_branch_id,
            v_sku_id,
            CASE WHEN (s % 2 = 0) THEN 'IN' ELSE 'OUT' END,
            'BENCHMARK_STRESS_TEST',
            (s % 10) + 1,
            1000 + (s % 500),
            150000.00,
            'BENCHMARK',
            gen_random_uuid(),
            'benchmark-runner',
            NOW() - ((v_total - (i - 1) * v_batch_size - s) || ' seconds')::INTERVAL
        FROM generate_series(1, v_batch_size) AS s;

        RAISE NOTICE 'Đã chèn batch % / % (% dòng)', i, v_iterations, i * v_batch_size;
    END LOOP;

    RAISE NOTICE 'Hoàn thành sinh 1.000.000 dòng inventory_transaction thành công!';
END $$;

