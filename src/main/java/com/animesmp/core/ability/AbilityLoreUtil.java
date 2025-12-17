package com.animesmp.core.ability;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AbilityLoreUtil {

    private AbilityLoreUtil() {}

    // ----------------------------
    // Public builders (your format)
    // ----------------------------

    public static List<String> vendorLore(AnimeSMPPlugin plugin, Ability a, int priceYen) {
        List<String> lore = baseLore(plugin, a);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.AQUA + priceYen + "¥");
        return lore;
    }

    public static List<String> trainerLore(AnimeSMPPlugin plugin, Ability a, String questText) {
        List<String> lore = baseLore(plugin, a);
        lore.add("");
        lore.add(ChatColor.GOLD + "Quest: " + ChatColor.WHITE + (questText == null ? "No quest configured." : questText));
        return lore;
    }

    public static List<String> pdLore(AnimeSMPPlugin plugin, Ability a, int priceTokens, int remaining) {
        List<String> lore = baseLore(plugin, a);
        lore.add("");
        lore.add(ChatColor.RED + "Cost: " + ChatColor.WHITE + priceTokens + " PD Tokens");
        lore.add(ChatColor.YELLOW + "Stock: " + ChatColor.AQUA + remaining);
        return lore;
    }

    public static List<String> scrollLore(AnimeSMPPlugin plugin, Ability a) {
        return baseLore(plugin, a);
    }

    // ----------------------------
    // Core format
    // ----------------------------

    private static List<String> baseLore(AnimeSMPPlugin plugin, Ability a) {
        List<String> lore = new ArrayList<>();
        if (a == null) return lore;

        String id = safeId(a);
        String desc = lookupDescription(plugin, id);

        // 1) Short description
        lore.add(ChatColor.GRAY + desc);

        // 2) Rarity + Type (player-facing)
        Rarity rarityEnum = resolveDisplayRarity(plugin, a, id);
        String rarity = coloredRarityLabel(rarityEnum);

        String type = prettyTypePlain(a.getType());

        lore.add(ChatColor.GRAY + "Rarity: " + rarity
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.GRAY + "Type: " + ChatColor.WHITE + type);

        return lore;
    }

    // ----------------------------
    // Rarity resolution (FIX)
    // ----------------------------

    private enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY, ULTIMATE
    }

    private static Rarity resolveDisplayRarity(AnimeSMPPlugin plugin, Ability a, String id) {
        // 1) config damage-tier wins (works for damage abilities + anything you define)
        String dmgTier = lookupDamageTier(plugin, id);
        if (dmgTier != null) {
            Rarity mapped = parseRarity(dmgTier);
            if (mapped != null) return mapped;
        }

        // 2) explicit epic ids list (good for movement/utility/defense where you may not want damage-tier)
        if (plugin != null) {
            List<String> epicIds = plugin.getConfig().getStringList("pd-shop.epic-ids");
            if (epicIds != null) {
                for (String s : epicIds) {
                    if (s != null && s.equalsIgnoreCase(id)) {
                        return Rarity.EPIC;
                    }
                }
            }
        }

        // 3) PD bucket = legendary (unless config said ULTIMATE above)
        try {
            if (a.getTier() != null) {
                String t = a.getTier().name().toUpperCase(Locale.ROOT);
                if (t.contains("PD")) return Rarity.LEGENDARY;
            }
        } catch (Throwable ignored) {}

        // 4) fallback default (your economy has most non-common stuff as rare baseline)
        return Rarity.RARE;
    }

    private static String lookupDamageTier(AnimeSMPPlugin plugin, String abilityId) {
        if (plugin == null || abilityId == null) return null;

        // Preferred path: abilities.<id>.damage-tier
        String path = "abilities." + abilityId.toLowerCase(Locale.ROOT) + ".damage-tier";
        String tier = plugin.getConfig().getString(path);
        if (tier != null && !tier.trim().isEmpty()) return tier.trim();

        // Fallback path (if you ever used it)
        String path2 = "ability-definitions." + abilityId.toLowerCase(Locale.ROOT) + ".damage-tier";
        tier = plugin.getConfig().getString(path2);
        if (tier != null && !tier.trim().isEmpty()) return tier.trim();

        return null;
    }

    private static Rarity parseRarity(String raw) {
        if (raw == null) return null;
        String r = raw.trim().toUpperCase(Locale.ROOT);

        if (r.contains("ULTIMATE")) return Rarity.ULTIMATE;
        if (r.contains("LEGEND")) return Rarity.LEGENDARY;
        if (r.contains("EPIC")) return Rarity.EPIC;
        if (r.contains("RARE")) return Rarity.RARE;
        if (r.contains("COMMON")) return Rarity.COMMON;

        return null;
    }

    private static String coloredRarityLabel(Rarity r) {
        if (r == null) r = Rarity.RARE;

        switch (r) {
            case COMMON:
                return ChatColor.GREEN + "Common";
            case RARE:
                return ChatColor.BLUE + "Rare";
            case EPIC:
                return ChatColor.DARK_PURPLE + "Epic";
            case LEGENDARY:
                return ChatColor.GOLD + "Legendary";
            case ULTIMATE:
                return ChatColor.RED + "" + ChatColor.BOLD + "Ultimate";
            default:
                return ChatColor.BLUE + "Rare";
        }
    }

    // ----------------------------
    // Description + misc helpers
    // ----------------------------

    private static String lookupDescription(AnimeSMPPlugin plugin, String abilityId) {
        if (plugin == null || abilityId == null) return "A powerful technique.";

        String path = "abilities." + abilityId.toLowerCase(Locale.ROOT) + ".description";
        String desc = plugin.getConfig().getString(path);
        if (desc != null && !desc.trim().isEmpty()) return desc.trim();

        String path2 = "ability-definitions." + abilityId.toLowerCase(Locale.ROOT) + ".description";
        desc = plugin.getConfig().getString(path2);
        if (desc != null && !desc.trim().isEmpty()) return desc.trim();

        return "A powerful technique.";
    }

    private static String safeId(Ability a) {
        try {
            String id = a.getId();
            return id == null ? "unknown" : id;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String prettyTypePlain(AbilityType type) {
        if (type == null) return "Unknown";

        String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ').trim();

        raw = raw.replace("damage light", "light damage");
        raw = raw.replace("damage heavy", "heavy damage");

        String[] parts = raw.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
