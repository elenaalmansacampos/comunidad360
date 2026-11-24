package com.comunidad360.authorization;

public enum AuthorizationScope {
    VOTE("VOTE_CAST"),
    WORK_PERMIT("WORK_PERMIT_CREATE"),
    INCIDENT("INCIDENT_CREATE"),
    BOARD("BOARD_CREATE"),
    LOST_FOUND("LOST_ITEM_CREATE");

    private final String permission;

    AuthorizationScope(String permission) {
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}
