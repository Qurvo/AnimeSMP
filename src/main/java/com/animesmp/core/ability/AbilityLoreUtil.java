package com.animesmp.core.ability;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.ChatColor;

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
        // For actual scroll items in inventory (no shop/trainer context)
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
    String rarity = (a.getTier() == null)
            ? (ChatColor.GRAY + "Common")
            : a.getTier().getColoredRarityLabel();

    String type = prettyTypePlain(a.getType());

    // One clean line, no internal tier names (VENDOR/TRAINER/PD) and no underscores
    lore.add(ChatColor.GRAY + "Rarity: " + rarity
            + ChatColor.DARK_GRAY + " | "
            + ChatColor.GRAY + "Type: " + ChatColor.WHITE + type);

    return lore;
}

    private static String lookupDescription(AnimeSMPPlugin plugin, String abilityId) {
        if (plugin == null || abilityId == null) return "A powerful technique.";

        // Preferred path: abilities.<id>.description
        String path = "abilities." + abilityId.toLowerCase(Locale.ROOT) + ".description";
        String desc = plugin.getConfig().getString(path);

        if (desc != null && !desc.trim().isEmpty()) return desc.trim();

        // Fallback path (common alternative):
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

    // Match your wording
    raw = raw.replace("damage light", "light damage");
    raw = raw.replace("damage heavy", "heavy damage");

    // Title case
    String[] parts = raw.split("\s+");
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
        if (p.isEmpty()) continue;
        sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
    }
    return sb.toString().trim();
}
}
