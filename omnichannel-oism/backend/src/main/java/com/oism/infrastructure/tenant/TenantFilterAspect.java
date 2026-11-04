package com.oism.infrastructure.tenant;

import com.oism.domain.model.TenantScopedEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Bat Hibernate @Filter "tenantFilter" cho moi lan goi repository khi request co tenant (NFR-TENANT-01).
 * Voi open-in-view=true, session song suot request nen bo loc co hieu luc cho cac truy van sau do.
 * Luu y: @Filter khong ap dung cho findById; repository van phai co ham *AndTenantId tuong minh.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.oism.domain.repository..*(..))")
    public void enableTenantFilter() {
        TenantContext.get().ifPresent(tenantId -> entityManager.unwrap(Session.class)
                .enableFilter(TenantScopedEntity.TENANT_FILTER)
                .setParameter(TenantScopedEntity.TENANT_ID_PARAM, tenantId));
    }
}
