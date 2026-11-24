package com.comunidad360.directory.dto;

import jakarta.validation.constraints.Size;

public record UpdateDirectoryProfileRequest(
        @Size(max = 40) String phone,
        Boolean showEmail,
        Boolean showPhone
) {
}
