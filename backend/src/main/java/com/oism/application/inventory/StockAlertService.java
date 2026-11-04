package com.oism.application.inventory;

import com.oism.domain.event.StockAlertEvent;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockAlert;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockAlertRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quan ly canh bao ton kho thap (FR-REP-03, JC-30).
 * Sinh canh bao khi available <= reorder_point, tu dong dong khi nhap hang vuot nguong.
 */
@Service
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StockAlertService(StockAlertRepository stockAlertRepository,
                             StockBalanceRepository stockBalanceRepository,
                             SkuRepository skuRepository,
                             ProductRepository productRepository,
                             BranchRepository branchRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.stockAlertRepository = stockAlertRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void checkStockLevel(UUID tenantId, UUID branchId, UUID skuId) {
        Optional<Sku> skuOpt = skuRepository.findById(skuId);
        if (skuOpt.isEmpty()) {
            return;
        }
        Sku sku = skuOpt.get();
        int threshold = sku.getReorderPoint();
        if (threshold <= 0) {
            return;
        }

        Optional<StockBalance> balanceOpt = stockBalanceRepository.findForUpdate(tenantId, branchId, skuId);
        int available = balanceOpt.map(StockBalance::getAvailable).orElse(0);

        Optional<StockAlert> openAlert = stockAlertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN);

        if (available <= threshold) {
            if (openAlert.isEmpty()) {
                StockAlert alert = stockAlertRepository.save(new StockAlert(tenantId, branchId, skuId, threshold, available));
                eventPublisher.publishEvent(new StockAlertEvent(
                        tenantId, alert.getId(), branchId, skuId, sku.getCode(), "OPEN", threshold, available, Instant.now()));
            }
        } else {
            if (openAlert.isPresent()) {
                StockAlert alert = openAlert.get();
                alert.resolve();
                stockAlertRepository.save(alert);
                eventPublisher.publishEvent(new StockAlertEvent(
                        tenantId, alert.getId(), branchId, skuId, sku.getCode(), "RESOLVED", threshold, available, Instant.now()));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<StockAlertView> listAlerts(UUID tenantId, StockAlert.Status status, UUID branchId) {
        List<StockAlert> alerts;
        if (branchId != null && status != null) {
            alerts = stockAlertRepository.findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(tenantId, branchId, status);
        } else if (status != null) {
            alerts = stockAlertRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else {
            alerts = stockAlertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }

        if (alerts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> branchIds = alerts.stream().map(StockAlert::getBranchId).distinct().toList();
        Map<UUID, String> branchNames = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        List<UUID> skuIds = alerts.stream().map(StockAlert::getSkuId).distinct().toList();
        List<Sku> skus = skuRepository.findAllById(skuIds);
        Map<UUID, String> skuCodes = skus.stream().collect(Collectors.toMap(Sku::getId, Sku::getCode, (a, b) -> a));

        List<UUID> prodIds = skus.stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findAllById(prodIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        Map<UUID, String> skuProductNames = skus.stream()
                .collect(Collectors.toMap(Sku::getId, s -> prodNames.getOrDefault(s.getProductId(), s.getCode()), (a, b) -> a));

        return alerts.stream().map(a -> new StockAlertView(
                a.getId(),
                a.getBranchId(),
                branchNames.getOrDefault(a.getBranchId(), "N/A"),
                a.getSkuId(),
                skuCodes.getOrDefault(a.getSkuId(), "N/A"),
                skuProductNames.getOrDefault(a.getSkuId(), "N/A"),
                a.getStatus(),
                a.getThreshold(),
                a.getAvailableAtAlert(),
                a.getCreatedAt(),
                a.getResolvedAt()
        )).toList();
    }

    @Transactional
    public void resolveAlert(UUID tenantId, UUID alertId) {
        StockAlert alert = stockAlertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy cảnh báo"));
        alert.resolve();
        stockAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public long countOpenAlerts(UUID tenantId) {
        return stockAlertRepository.countByTenantIdAndStatus(tenantId, StockAlert.Status.OPEN);
    }
}
