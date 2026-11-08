package com.oism.application.branch;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** FR-AUTH-04: quan ly chi nhanh/kho cua tenant. Ten duy nhat trong tenant. */
@Service
public class BranchService {

    private static final Set<OrderStatus> OPEN_ORDER_STATUSES =
            Set.of(OrderStatus.DRAFT, OrderStatus.RESERVED, OrderStatus.CONFIRMED);

    private final BranchRepository branchRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final SalesOrderRepository salesOrderRepository;

    public BranchService(BranchRepository branchRepository,
                         StockBalanceRepository stockBalanceRepository,
                         SalesOrderRepository salesOrderRepository) {
        this.branchRepository = branchRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<BranchView> list(UUID tenantId) {
        return branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream().map(BranchView::from).toList();
    }

    @Transactional
    public BranchView create(UUID tenantId, String name, BranchType type, String address) {
        String cleanName = name.trim();
        if (branchRepository.existsByTenantIdAndNameIgnoreCase(tenantId, cleanName)) {
            throw new BusinessException(ErrorCode.AU40901, "Tên chi nhánh đã tồn tại");
        }
        Branch branch = new Branch(tenantId, cleanName, type);
        branch.setAddress(blankToNull(address));
        return BranchView.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchView update(UUID tenantId, UUID id, String name, BranchType type, String address) {
        Branch branch = find(tenantId, id);
        String cleanName = name.trim();
        if (!branch.getName().equalsIgnoreCase(cleanName)
                && branchRepository.existsByTenantIdAndNameIgnoreCase(tenantId, cleanName)) {
            throw new BusinessException(ErrorCode.AU40901, "Tên chi nhánh đã tồn tại");
        }
        branch.setName(cleanName);
        branch.setType(type);
        branch.setAddress(blankToNull(address));
        return BranchView.from(branchRepository.save(branch));
    }

       /**
     * Khong an duoc chi nhanh con ton kho hoac con don chua ket thuc (AU40902),
     * va khong an duoc chi nhanh hoat dong cuoi cung cua tenant (AU40903).
     */
    @Transactional
    public BranchView setActive(UUID tenantId, UUID id, boolean active) {
        Branch branch = find(tenantId, id);
        if (!active && branch.isActive()) {
            boolean hasStock = stockBalanceRepository.existsByTenantIdAndBranchIdAndOnHandGreaterThan(tenantId, id, 0);
            boolean hasOpenOrders = salesOrderRepository.existsByTenantIdAndBranchIdAndStatusIn(
                    tenantId, id, OPEN_ORDER_STATUSES);
            if (hasStock || hasOpenOrders) {
                throw new BusinessException(ErrorCode.AU40902);
            }
            if (branchRepository.countByTenantIdAndActiveTrue(tenantId) <= 1) {
                throw new BusinessException(ErrorCode.AU40903);
            }
        }
        branch.setActive(active);
        return BranchView.from(branchRepository.save(branch));
    }

    private Branch find(UUID tenantId, UUID id) {
        return branchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy chi nhánh"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
