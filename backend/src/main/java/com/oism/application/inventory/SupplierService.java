package com.oism.application.inventory;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Supplier;
import com.oism.domain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** FR-INV-05: them, sua, ngung dung nha cung cap. */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierView> list(UUID tenantId) {
        return supplierRepository.findByTenantIdOrderByNameAsc(tenantId).stream().map(SupplierView::from).toList();
    }

    @Transactional
    public SupplierView create(UUID tenantId, String name, String phone, String address) {
        return SupplierView.from(supplierRepository.save(
                new Supplier(tenantId, name.trim(), blankToNull(phone), blankToNull(address))));
    }

    @Transactional
    public SupplierView update(UUID tenantId, UUID id, String name, String phone, String address, boolean active) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy nhà cung cấp"));
        supplier.setName(name.trim());
        supplier.setPhone(blankToNull(phone));
        supplier.setAddress(blankToNull(address));
        supplier.setActive(active);
        return SupplierView.from(supplierRepository.save(supplier));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
