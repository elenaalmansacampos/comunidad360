package com.comunidad360.works.dto;

import com.comunidad360.works.WorkPermit;
import com.comunidad360.works.WorkPermitStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkPermitResponse(
        UUID id, String title, String description, String location, LocalDate startDate,
        LocalDate endDate, WorkPermitStatus status, UUID createdByUserId, String createdByName,
        UUID reviewedByUserId, String reviewedByName, Instant reviewedAt, String reviewNotes,
        String fileUrl, String fileName, Instant createdAt, Instant updatedAt, long version
) {
    public static WorkPermitResponse from(WorkPermit permit, String createdByName, String reviewedByName) {
        String fileUrl = permit.getFileKey() == null ? null
                : "/communities/" + permit.getCommunityId() + "/work-permits/" + permit.getId() + "/file";
        return new WorkPermitResponse(permit.getId(), permit.getTitle(), permit.getDescription(),
                permit.getLocation(), permit.getStartDate(), permit.getEndDate(), permit.getStatus(),
                permit.getCreatedByUserId(), createdByName, permit.getReviewedByUserId(), reviewedByName,
                permit.getReviewedAt(), permit.getReviewNotes(), fileUrl, permit.getFileName(),
                permit.getCreatedAt(), permit.getUpdatedAt(), permit.getVersion());
    }
}
