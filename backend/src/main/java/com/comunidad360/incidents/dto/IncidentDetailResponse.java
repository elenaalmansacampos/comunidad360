package com.comunidad360.incidents.dto;

import java.util.List;

public record IncidentDetailResponse(
        IncidentResponse incident,
        List<IncidentCommentResponse> comments,
        List<IncidentAttachmentResponse> attachments,
        List<IncidentHistoryResponse> history
) {
}
