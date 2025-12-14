package com.animesmp.core.ability;

public class Ability {

    private final String id;
    private final String displayName;
    private final AbilityType type;
    private final AbilityTier tier;
    private final double baseDamageHearts;
    private final int cooldownSeconds;
    private final int staminaCost;

    public Ability(String id, String displayName, AbilityType type, AbilityTier tier,
                   double baseDamageHearts, int cooldownSeconds, int staminaCost) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.tier = tier;
        this.baseDamageHearts = baseDamageHearts;
        this.cooldownSeconds = cooldownSeconds;
        this.staminaCost = staminaCost;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AbilityType getType() {
        return type;
    }

    public AbilityTier getTier() {
        return tier;
    }

    public double getBaseDamageHearts() {
        return baseDamageHearts;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getStaminaCost() {
        return staminaCost;
    }
}
