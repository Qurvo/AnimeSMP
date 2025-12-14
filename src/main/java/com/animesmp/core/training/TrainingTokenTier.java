package com.animesmp.core.training;

public enum TrainingTokenTier {

    // TL ranges & XP ranges are mostly for display / balance.
    // You can tweak numbers later without touching other code.

    BASIC(
            "Basic Training Token",
            0,   // min TL
            5,   // max TL
            120, // min XP this token is "tuned" for
            250  // max XP this token is "tuned" for
    ),

    INTERMEDIATE(
            "Intermediate Training Token",
            5,
            10,
            250,
            450
    ),

    ADVANCED(
            "Advanced Training Token",
            10,
            15,
            450,
            650
    ),

    // EXPERT & ELITE are effectively the same tier in practice
    EXPERT(
            "Expert Training Token",
            15,
            20,
            650,
            850
    ),

    ELITE(
            "Elite Training Token",
            15,
            20,
            650,
            850
    ),

    // MASTER & LEGENDARY are effectively the same tier in practice
    MASTER(
            "Master Training Token",
            20,
            25,
            850,
            1300
    ),

    LEGENDARY(
            "Legendary Training Token",
            20,
            25,
            850,
            1300
    );

    // -----------------------------------------------------
    // FIELDS
    // -----------------------------------------------------

    private final String displayName;
    private final int minLevel;
    private final int maxLevel;
    private final int xpMin;
    private final int xpMax;

    TrainingTokenTier(String displayName, int minLevel, int maxLevel, int xpMin, int xpMax) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.xpMin = xpMin;
        this.xpMax = xpMax;
    }

    // -----------------------------------------------------
    // Used by existing code (TrainingManager, ShopGuiManager)
    // -----------------------------------------------------

    public String getDisplayName() {
        return displayName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getXpMin() {
        return xpMin;
    }

    public int getXpMax() {
        return xpMax;
    }

    // -----------------------------------------------------
    // Convenience helpers
    // -----------------------------------------------------

    public String getNiceName() {
        return displayName;
    }

    public static TrainingTokenTier fromString(String raw) {
        if (raw == null) return null;
        raw = raw.trim().toUpperCase();

        switch (raw) {
            case "BASIC":
                return BASIC;

            case "INTERMEDIATE":
            case "INTER":
                return INTERMEDIATE;

            case "ADVANCED":
            case "ADV":
                return ADVANCED;

            // Treat EXPERT / ELITE as the same “tier” for parsing
            case "EXPERT":
            case "ELITE":
                return EXPERT;

            // Treat MASTER / LEGENDARY as the same “tier” for parsing
            case "MASTER":
            case "LEGENDARY":
                return MASTER;

            default:
                // fallback: try direct valueOf in case we missed something
                try {
                    return TrainingTokenTier.valueOf(raw);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
        }
    }
}
