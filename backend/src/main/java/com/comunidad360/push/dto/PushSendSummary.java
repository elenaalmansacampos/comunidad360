package com.comunidad360.push.dto;

public record PushSendSummary(int sent, int failed, int removed) {

    public static PushSendSummary empty() {
        return new PushSendSummary(0, 0, 0);
    }

    public int attempted() {
        return sent + failed + removed;
    }
}
