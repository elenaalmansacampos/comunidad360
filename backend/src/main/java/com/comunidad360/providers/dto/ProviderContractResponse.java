package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderContract;
import com.comunidad360.providers.ProviderContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderContractResponse(
        UUID id, String title, String description, LocalDate startDate, LocalDate endDate,
        BigDecimal amount, ProviderContractStatus status, Instant createdAt, Instant updatedAt, long version
) {
    public static ProviderContractResponse from(ProviderContract contract) {
        return new ProviderContractResponse(contract.getId(), contract.getTitle(), contract.getDescription(),
                contract.getStartDate(), contract.getEndDate(), contract.getAmount(), contract.getStatus(),
                contract.getCreatedAt(), contract.getUpdatedAt(), contract.getVersion());
    }
}
