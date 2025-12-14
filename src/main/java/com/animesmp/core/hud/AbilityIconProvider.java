package com.animesmp.core.hud;

public class AbilityIconProvider {

    /**
     * Returns a small emoji/icon to represent an ability in the HUD.
     * All IDs should be lowercase ability IDs.
     */
    public static String getIconFor(String abilityId) {
        if (abilityId == null) return "⬜";

        switch (abilityId.toLowerCase()) {

            // Movement
            case "flashstep":
                return "⚡";

            // Damage
            case "fire_fist":
                return "🔥";
            case "shockwave":
                return "💥";

            // Defense / Utility
            case "flame_cloak":
                return "🛡️🔥";

            // Fallback
            default:
                return "⬜";
        }
    }
}
