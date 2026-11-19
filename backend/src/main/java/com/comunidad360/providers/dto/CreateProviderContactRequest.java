package com.comunidad360.providers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProviderContactRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String role,
        @Size(max = 320) String email,
        @Size(max = 50) String phone,
        boolean primaryContact
) {}
