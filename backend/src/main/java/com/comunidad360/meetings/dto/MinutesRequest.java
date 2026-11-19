package com.comunidad360.meetings.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MinutesRequest(@NotNull UUID documentId) {
}
