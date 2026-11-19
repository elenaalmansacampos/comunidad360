package com.comunidad360.meetings.dto;

import java.util.List;

public record MeetingDetailResponse(
        MeetingResponse meeting,
        List<AgendaItemResponse> agenda,
        List<AttendanceResponse> attendance,
        List<AgreementResponse> agreements
) {
}
