package com.comunidad360.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_item")
public class BudgetItem {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetItemType type;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "planned_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedAmount;

    @Column(nullable = false)
    private Integer position;

    protected BudgetItem() {
    }

    public BudgetItem(UUID id, UUID communityId, UUID budgetId, BudgetItemType type, String category,
                      String description, BigDecimal plannedAmount, Integer position) {
        this.id = id;
        this.communityId = communityId;
        this.budgetId = budgetId;
        this.type = type;
        this.category = category;
        this.description = description;
        this.plannedAmount = plannedAmount;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public BudgetItemType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public Integer getPosition() {
        return position;
    }
}
