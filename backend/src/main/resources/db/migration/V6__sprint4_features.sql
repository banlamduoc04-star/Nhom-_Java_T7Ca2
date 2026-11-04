-- Flyway migration V6: Sprint 4 schema features (Stock alerts, Daily report)

-- 1. Bảng cảnh báo tồn kho thấp (FR-REP-03, JC-30)
CREATE TABLE IF NOT EXISTS stock_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    threshold INT NOT NULL,
    available_at_alert INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_stock_alert_tenant_status
    ON stock_alert(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_stock_alert_tenant_sku_branch
    ON stock_alert(tenant_id, sku_id, branch_id);

-- 2. Bảng báo cáo doanh thu & lợi nhuận chốt theo ngày (FR-SIM-03, JC-31)
CREATE TABLE IF NOT EXISTS daily_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    report_date DATE NOT NULL,
    total_revenue NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_cogs NUMERIC(15, 2) NOT NULL DEFAULT 0,
    gross_profit NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_orders INT NOT NULL DEFAULT 0,
    completed_orders INT NOT NULL DEFAULT 0,
    cancelled_orders INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_report_tenant_date UNIQUE (tenant_id, report_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_report_tenant_date
    ON daily_report(tenant_id, report_date DESC);
