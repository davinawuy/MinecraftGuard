package com.davinawuy.minecraftguard.entity;

import net.minecraft.world.World;

public enum ShiftSchedule {
    DAY("day", "Day shift"),
    NIGHT("night", "Night shift"),
    ALWAYS("always", "Always on duty");

    private final String id;
    private final String displayName;

    ShiftSchedule(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOnDuty(World world) {
        return switch (this) {
            case DAY -> world.isDay();
            case NIGHT -> !world.isDay();
            default -> true;
        };
    }

    public static ShiftSchedule fromId(String id) {
        for (ShiftSchedule schedule : values()) {
            if (schedule.id.equalsIgnoreCase(id)) {
                return schedule;
            }
        }
        return ALWAYS;
    }
}
