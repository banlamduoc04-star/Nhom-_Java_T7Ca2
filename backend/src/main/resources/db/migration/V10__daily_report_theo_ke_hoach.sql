-- V10: Dua du lieu ve dung mo hinh cua ke hoach (Ke hoach muc 4 - Mo hinh du lieu).

-- 1. daily_report (FR-SIM-03): branch_id, report_date, channel, order_count, revenue, cogs, gross_profit;
--    duy nhat (tenant_id, branch_id, report_date, channel). Bang chi chua so lieu tong hop tu orders/order_item,
--    nen tao lai bang; DailyReportJob / POST /reports/daily/run tinh lai khi can.
DROP TABLE IF EXISTS daily_report;

CREATE TABLE daily_report (
    id           UUID           PRIMARY KEY,
    tenant_id    UUID           NOT NULL REFERENCES tenant (id),
    branch_id    UUID           NOT NULL REFERENCES branch (id),
    report_date  DATE           NOT NULL,
    channel      VARCHAR(20)    NOT NULL,
    order_count  INTEGER        NOT NULL DEFAULT 0,
    revenue      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    cogs         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    gross_profit NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_report_tenant_branch_date_channel UNIQUE (tenant_id, branch_id, report_date, channel),
    CONSTRAINT ck_daily_report_order_count CHECK (order_count >= 0)
);
CREATE INDEX ix_daily_report_tenant_created ON daily_report (tenant_id, created_at);
CREATE INDEX ix_daily_report_tenant_date ON daily_report (tenant_id, report_date);

-- 2. Go so cai thu hai them o V7 (inventory_items, ledger_entries): id BIGSERIAL, tenant_id VARCHAR, song song voi
--    inventory_transaction - trai quyet dinh kien truc 2 cua ke hoach (mot so cai duy nhat). Khong co code nao dung.
DROP TABLE IF EXISTS inventory_items;
DROP TABLE IF EXISTS ledger_entries;
