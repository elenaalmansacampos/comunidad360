package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.MeetingAgendaItem;

import java.util.UUID;

public record AgendaItemResponse(
        UUID id,
        Integer position,
        String title,
        String description
) {
    public static AgendaItemResponse from(MeetingAgendaItem item) {
        return new AgendaItemResponse(item.getId(), item.getPosition(), item.getTitle(), item.getDescription());
    }
}
