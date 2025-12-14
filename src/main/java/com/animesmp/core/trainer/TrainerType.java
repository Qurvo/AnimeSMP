package com.animesmp.core.trainer;

import org.bukkit.ChatColor;

public enum TrainerType {

    KOKUSHIBO(
            "kokushibo",
            ChatColor.DARK_PURPLE + "Kokushibo - Moon Trainer",
            "Specializes in moon/cursed slashes and heavy damage techniques."
    ),

    LUFFY(
            "luffy",
            ChatColor.GOLD + "Luffy - Gear Trainer",
            "Teaches punchy close-range skills and movement dashes."
    ),

    ACE(
            "ace",
            ChatColor.RED + "Ace - Flame Trainer",
            "Master of fire-based techniques and explosive abilities."
    ),

    ICHIGO(
            "ichigo",
            ChatColor.AQUA + "Ichigo - Spirit Trainer",
            "Focuses on spiritual slashes and burst damage."
    );

    private final String id;
    private final String displayName;
    private final String description;

    TrainerType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static TrainerType fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase();
        for (TrainerType t : values()) {
            if (t.id.equalsIgnoreCase(lower)) {
                return t;
            }
        }
        return null;
    }
}
