package com.comunidad360.providers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProviderRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20) String taxId,
        @Size(max = 100) String category,
        @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 300) String address,
        @Size(max = 2000) String notes
) {}
