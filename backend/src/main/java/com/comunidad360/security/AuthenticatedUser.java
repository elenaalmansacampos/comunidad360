package com.comunidad360.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String fullName,
        String preferredLanguage
) {
}
