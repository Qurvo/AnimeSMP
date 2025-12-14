package com.animesmp.core.ability;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.stream.Collectors;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbilityRegistry {

    private final AnimeSMPPlugin plugin;
    private final Map<String, Ability> abilities = new HashMap<>();

    public AbilityRegistry(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAbilities() {
        abilities.clear();

        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("abilities");
        if (sec == null) {
            plugin.getLogger().warning("[AnimeSMP] No 'abilities' section found in config.yml");
            return;
        }

        for (String id : sec.getKeys(false)) {
            ConfigurationSection aSec = sec.getConfigurationSection(id);
            if (aSec == null) continue;

            String displayName = aSec.getString("display-name", id);

            // -----------------------------
            // TYPE: normalize legacy values
            // -----------------------------
            String rawType = aSec.getString("type", "DAMAGE_LIGHT");
            String typeKey = normalizeType(rawType);

            AbilityType type;
            try {
                type = AbilityType.valueOf(typeKey);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[AnimeSMP] Unknown ability type '" + rawType +
                        "' for ability '" + id + "', defaulting to DAMAGE_LIGHT");
                type = AbilityType.DAMAGE_LIGHT;
            }

            // -----------------------------
            // TIER: normalize legacy values
            // -----------------------------
            String rawTier = aSec.getString("tier", "SCROLL");
            String tierKey = normalizeTier(rawTier);

            AbilityTier tier;
            try {
                tier = AbilityTier.valueOf(tierKey);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[AnimeSMP] Unknown ability tier '" + rawTier +
                        "' for ability '" + id + "', defaulting to SCROLL");
                tier = AbilityTier.SCROLL;
            }

            double baseHearts = aSec.contains("base-damage-hearts")
                    ? aSec.getDouble("base-damage-hearts")
                    : aSec.getDouble("damage-hearts", 4.0);
            double cooldownSec = aSec.getDouble("cooldown", 8.0);
            int staminaCost = aSec.getInt("stamina-cost", 15);

            Ability ability = new Ability(
                    id,
                    displayName,
                    type,
                    tier,
                    baseHearts,
                    (int) Math.round(cooldownSec),
                    staminaCost
            );

            abilities.put(id.toLowerCase(), ability);
        }

        plugin.getLogger().info("[AnimeSMP] Loaded " + abilities.size() + " abilities from config.");
    }

    public Ability getAbilityByDisplay(String display) {
        if (display == null) return null;
        for (Ability a : abilities.values()) {
            if (a.getDisplayName().equalsIgnoreCase(display)) {
                return a;
            }
        }
        return null;
    }


    // Map old/loose type names into new enum constants
    private String normalizeType(String raw) {
        if (raw == null) return "DAMAGE_LIGHT";
        String key = raw.trim().toUpperCase();

        switch (key) {
            case "MOVEMENT":
            case "MOBILITY":
                return "MOVEMENT";

            case "DAMAGE":
            case "DMG":
            case "DAMAGE_SINGLE":
            case "DAMAGE_LIGHT":
                return "DAMAGE_LIGHT";

            case "NUKE":
            case "DAMAGE_HEAVY":
                return "DAMAGE_HEAVY";

            case "DEF":
            case "DEFENSE":
            case "DEFENSIVE":
                return "DEFENSE";

            case "SUPPORT":
            case "BUFF":
            case "HEAL":
                return "SUPPORT";

            case "UTILITY":
            case "UTIL":
            case "CONTROL":
                return "UTILITY";

            case "ULT":
            case "ULTIMATE":
                return "ULTIMATE";

            default:
                return "DAMAGE_LIGHT";
        }
    }

    // Map old rarity names into the 4-tier system
    private String normalizeTier(String raw) {
        if (raw == null) return "SCROLL";
        String key = raw.trim().toUpperCase();

        switch (key) {
            // Common / starter / scroll-based
            case "COMMON":
            case "BASIC":
            case "SCROLL":
            case "LOOT":
                return "SCROLL";

            // Vendor-sold abilities (mid-tier)
            case "UNCOMMON":
            case "RARE":
            case "EPIC":
            case "SHOP":
            case "VENDOR":
                return "VENDOR";

            // Quest/trainer unlocks
            case "QUEST":
            case "TRAINER":
            case "MASTER":
                return "TRAINER";

            // PD / endgame / legendary
            case "LEGENDARY":
            case "MYTHIC":
            case "PD":
            case "PDTOKEN":
                return "PD";

            default:
                return "SCROLL";
        }
    }

    /**
     * Get all abilities with the given tier.
     * Used for things like scroll loot, vendor stock, etc.
     */
    public java.util.List<Ability> getAbilitiesByTier(AbilityTier tier) {
        if (tier == null) return java.util.Collections.emptyList();
        return abilities.values().stream()
                .filter(a -> a.getTier() == tier)
                .collect(Collectors.toList());
    }


    public Ability getAbility(String id) {
        if (id == null) return null;
        return abilities.get(id.toLowerCase());
    }

    public Collection<Ability> getAllAbilities() {
        return abilities.values();
    }
}
