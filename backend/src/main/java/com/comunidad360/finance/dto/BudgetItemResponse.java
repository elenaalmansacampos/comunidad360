package com.comunidad360.finance.dto;

import com.comunidad360.finance.BudgetItem;
import com.comunidad360.finance.BudgetItemType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetItemResponse(
        UUID id,
        BudgetItemType type,
        String category,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal plannedAmount,
        Integer position
) {
    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(
                item.getId(),
                item.getType(),
                item.getCategory(),
                item.getDescription(),
                item.getPlannedAmount(),
                item.getPosition()
        );
    }
}
