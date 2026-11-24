package com.comunidad360.directory.dto;

import java.util.List;
import java.util.UUID;

public record DirectoryEntryResponse(
        UUID membershipId, UUID userId, String fullName, String email, String phone,
        List<String> roles, List<String> unitCodes, boolean showEmail, boolean showPhone,
        boolean self
) {
}
