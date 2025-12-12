package com.davinawuy.minecraftguard.entity;

public enum GuardRole {
    ARCHER("archer"),
    MACE("mace"),
    SWORD("sword"),
    CROSSBOW("crossbow");

    private final String id;

    GuardRole(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static GuardRole fromId(String id) {
        for (GuardRole role : values()) {
            if (role.id.equalsIgnoreCase(id)) {
                return role;
            }
        }
        return SWORD;
    }
}
