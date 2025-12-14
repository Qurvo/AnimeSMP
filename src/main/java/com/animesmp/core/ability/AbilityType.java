package com.animesmp.core.ability;

public enum AbilityType {
    MOVEMENT,
    DAMAGE_LIGHT,
    DAMAGE_HEAVY,
    DEFENSE,
    UTILITY,
    ULTIMATE;

    public String getDisplayName() {
        switch (this) {
            case MOVEMENT:
                return "Movement";
            case DAMAGE_LIGHT:
                return "Light Damage";
            case DAMAGE_HEAVY:
                return "Heavy Damage";
            case DEFENSE:
                return "Defense";
            case UTILITY:
                return "Support / Utility";
            case ULTIMATE:
                return "Ultimate";
            default:
                return name();
        }
    }
}
