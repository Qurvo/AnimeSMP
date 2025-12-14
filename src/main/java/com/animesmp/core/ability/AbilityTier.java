package com.animesmp.core.ability;

import org.bukkit.ChatColor;

public enum AbilityTier {
    SCROLL,   // mostly Common (world scrolls)
    VENDOR,   // Rare (yen vendor)
    TRAINER,  // Epic (trainer quests)
    PD;       // Legendary (PD shop / PD tokens)

    /**
     * Logical rarity name based on how we use tiers.
     * SCROLL   -> Common
     * VENDOR   -> Rare
     * TRAINER  -> Epic
     * PD       -> Legendary
     */
    public String getRarityName() {
        return switch (this) {
            case SCROLL -> "Common";
            case VENDOR -> "Rare";
            case TRAINER -> "Epic";
            case PD -> "Legendary";
        };
    }

    /**
     * Color for the rarity text.
     * Common   -> gray
     * Uncommon -> green (reserved if we add later)
     * Rare     -> blue
     * Epic     -> purple
     * Legendary-> gold
     */
    public ChatColor getRarityColor() {
        return switch (this) {
            case SCROLL -> ChatColor.GRAY;          // Common
            case VENDOR -> ChatColor.BLUE;          // Rare
            case TRAINER -> ChatColor.DARK_PURPLE;  // Epic
            case PD -> ChatColor.GOLD;              // Legendary
        };
    }

    /**
     * Combined colored label, e.g. "§7Common", "§9Rare", etc.
     */
    public String getColoredRarityLabel() {
        return getRarityColor() + getRarityName();
    }
}
