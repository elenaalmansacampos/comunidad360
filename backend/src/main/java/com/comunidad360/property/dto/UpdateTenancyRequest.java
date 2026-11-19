package com.comunidad360.property.dto;

import com.comunidad360.property.TenancyStatus;

import java.time.LocalDate;

public record UpdateTenancyRequest(
        LocalDate validFrom,
        LocalDate validTo,
        TenancyStatus status,
        Long version
) {
}
