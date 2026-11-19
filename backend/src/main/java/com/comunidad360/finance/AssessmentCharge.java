package com.comunidad360.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_charge")
public class AssessmentCharge {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "coefficient_snapshot", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientSnapshot;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected AssessmentCharge() {
    }

    public AssessmentCharge(UUID id, UUID communityId, UUID assessmentId, UUID unitId, BigDecimal amount,
                            BigDecimal coefficientSnapshot, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.assessmentId = assessmentId;
        this.unitId = unitId;
        this.amount = amount;
        this.coefficientSnapshot = coefficientSnapshot;
        this.status = ChargeStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getCoefficientSnapshot() {
        return coefficientSnapshot;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
