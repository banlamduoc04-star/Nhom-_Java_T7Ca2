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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Canh bao ton kho thap (FR-REP-03): available <= reorder_point thi mo canh bao OPEN va day thong bao;
 * available vuot nguong thi tu dong dong (CLOSED). Moi (chi nhanh, SKU) toi da 1 canh bao OPEN.
 * checkStockLevel duoc goi sau MOI thay doi on_hand/reserved (InventoryLedgerService.post, ReservationService),
 * cung giao dich va sau khi dong stock_balance da bi khoa, nen hai giao dich khong the cung mo trung canh bao.
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

    /** Danh gia lai canh bao cua mot (chi nhanh, SKU). Bat buoc chay trong giao dich cua thao tac kho. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkStockLevel(UUID tenantId, UUID branchId, UUID skuId) {
        Optional<Sku> skuOpt = skuRepository.findByIdAndTenantId(skuId, tenantId);
        if (skuOpt.isEmpty()) {
            return;
        }
        Sku sku = skuOpt.get();
        int threshold = sku.getReorderPoint();
        int available = stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)
                .map(StockBalance::getAvailable).orElse(0);
        Optional<StockAlert> openAlert = stockAlertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN);

        // reorder_point = 0 nghia la khong theo doi; canh bao dang mo (neu co) cung duoc dong
        boolean low = threshold > 0 && available <= threshold;
        if (low && openAlert.isEmpty()) {
            StockAlert alert = stockAlertRepository.save(new StockAlert(tenantId, branchId, skuId, threshold, available));
            publish(alert, sku, available);
        } else if (!low && openAlert.isPresent()) {
            StockAlert alert = openAlert.get();
            alert.close();
            stockAlertRepository.save(alert);
            publish(alert, sku, available);
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
        if (branchId != null && status == null) {
            alerts = alerts.stream().filter(a -> branchId.equals(a.getBranchId())).toList();
        }
        if (alerts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, String> branchNames = branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        List<UUID> skuIds = alerts.stream().map(StockAlert::getSkuId).distinct().toList();
        List<Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds);
        Map<UUID, String> skuCodes = skus.stream().collect(Collectors.toMap(Sku::getId, Sku::getCode, (a, b) -> a));

        List<UUID> prodIds = skus.stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findByTenantIdAndIdIn(tenantId, prodIds).stream()
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
                a.getAvailable(),
                a.getCreatedAt(),
                a.getClosedAt()
        )).toList();
    }

    /** Owner/Staff dong canh bao bang tay (da xu ly). Khong thay hoac khac tenant -> 404 IV40401. */
    @Transactional
    public void closeAlert(UUID tenantId, UUID alertId) {
        StockAlert alert = stockAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy cảnh báo"));
        alert.close();
        stockAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public long countOpenAlerts(UUID tenantId) {
        return stockAlertRepository.countByTenantIdAndStatus(tenantId, StockAlert.Status.OPEN);
    }

    private void publish(StockAlert alert, Sku sku, int available) {
        eventPublisher.publishEvent(new StockAlertEvent(alert.getTenantId(), alert.getId(), alert.getBranchId(),
                alert.getSkuId(), sku.getCode(), alert.getStatus().name(), alert.getThreshold(), available,
                Instant.now()));
    }
}
