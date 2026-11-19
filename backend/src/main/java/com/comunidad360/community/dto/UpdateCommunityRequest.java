package com.comunidad360.community.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommunityRequest(
        @Size(max = 200) String name,
        @Size(max = 20) String taxId,
        @Size(max = 300) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String province,
        @Size(max = 10) String postalCode,
        @Size(max = 50) String timezone,
        @Size(max = 10) String defaultLanguage,
        Long version
) {
}
