package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/** Cua hang (tenant). Bang nay khong co cot tenant_id. */
@Entity
@Table(name = "tenant")
@FilterDef(
        name = TenantScopedEntity.TENANT_FILTER,
        parameters = @ParamDef(name = TenantScopedEntity.TENANT_ID_PARAM, type = UUID.class))
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "reservation_minutes", nullable = false)
    private int reservationMinutes = 30;

    protected Tenant() {
    }

    public Tenant(String name) {
        this.name = name;
    }

        public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public int getReservationMinutes() {
        return reservationMinutes;
    }

    public void setReservationMinutes(int reservationMinutes) {
        this.reservationMinutes = reservationMinutes;
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
