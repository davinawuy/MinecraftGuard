package com.davinawuy.minecraftguard.entity;

public enum PatrolMode {
    WAYPOINT("waypoint", "Waypoint patrol"),
    RANDOM("random", "Zone patrol"),
    POST("post", "Stand watch");

    private final String id;
    private final String displayName;

    PatrolMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PatrolMode fromId(String id) {
        for (PatrolMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return POST;
    }
}
