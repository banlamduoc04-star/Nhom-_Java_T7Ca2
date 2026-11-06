package com.oism.application.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class StockReportViews {

    public record ValuationSummary(
            BigDecimal totalInventoryValue,
            int totalOnHand,
            int totalActiveSkus,
            List<ValuationItem> items
    ) {}

    public record ValuationItem(
            UUID branchId,
            String branchName,
            UUID skuId,
            String skuCode,
            String productName,
            int onHand,
            BigDecimal avgCost,
            BigDecimal totalValue
    ) {}

    public record TopSellingItem(
            UUID skuId,
            String skuCode,
            String productName,
            int quantitySold,
            BigDecimal totalRevenue
    ) {}

    public record SlowMovingItem(
            UUID branchId,
            String branchName,
            UUID skuId,
            String skuCode,
            String productName,
            int onHand,
            BigDecimal avgCost,
            BigDecimal totalValue,
            int daysWithoutSale
    ) {}
}

