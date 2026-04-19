package com.xiaoliang.simukraft.entity;

public enum WorkStatus {
    IDLE("work_status.idle"),
    WORKING("work_status.working");

    private final String displayName;

    WorkStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WorkStatus fromString(String status) {
        if (status == null) return IDLE;
        if (status.equals("work_status.working") || status.equals("工作中")) return WORKING;
        return IDLE;
    }
}