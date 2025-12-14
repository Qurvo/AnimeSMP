package com.animesmp.core.ability;

/**
 * Balance-facing tier used for PvP damage caps and cooldown floors.
 *
 * This is intentionally separate from {@link AbilityTier} which is used for
 * acquisition/rarity (SCROLL/VENDOR/TRAINER/PD).
 */
public enum DamageTier {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
    ULTIMATE;

    public static DamageTier fromString(String raw) {
        if (raw == null) return null;
        try {
            return DamageTier.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
