package com.animesmp.core.hud;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StatusHudManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final AbilityRegistry abilityRegistry;
    private final AbilityManager abilityManager;

    public StatusHudManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.abilityRegistry = plugin.getAbilityRegistry();
        this.abilityManager = plugin.getAbilityManager();
    }

    public void start() {
        // Update HUD every 0.5s
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                sendCombinedHud(p);
            }
        }, 20L, 10L);
    }

    private void sendCombinedHud(Player player) {
        PlayerProfile profile = profiles.getProfile(player);

        String staminaPart = buildStaminaSegment(profile);
        String abilityPart = buildAbilitySegment(player, profile);

        String combined = staminaPart + ChatColor.DARK_GRAY + "  |  " + abilityPart;

        player.sendActionBar(Component.text(combined));
    }

    // -------------------------------
    // STAMINA SEGMENT
    // -------------------------------

    private String buildStaminaSegment(PlayerProfile profile) {
        int current = (int) profile.getStaminaCurrent();
        int cap = profile.getStaminaCap();

        int bars = (int) ((current / (double) cap) * 10);
        if (bars < 0) bars = 0;
        if (bars > 10) bars = 10;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("█");
            }
        }

        return ChatColor.DARK_AQUA + "STA " +
                ChatColor.AQUA + bar +
                ChatColor.GRAY + " " +
                ChatColor.WHITE + current + "/" + cap;
    }

    // -------------------------------
    // ABILITY SEGMENT
    // -------------------------------

    private String buildAbilitySegment(Player player, PlayerProfile profile) {
        StringBuilder sb = new StringBuilder();

        sb.append(ChatColor.GOLD).append("AB: ");

        int selectedSlot = profile.getSelectedSlot(); // 1–5

        for (int slot = 1; slot <= 5; slot++) {
            String abilityId = profile.getBoundAbilityId(slot);

            // Bracket color + slot number color
            if (slot == selectedSlot) {
                sb.append(ChatColor.YELLOW).append("[");
            } else {
                sb.append(ChatColor.DARK_GRAY).append("[");
            }

            // Slot number
            if (slot == selectedSlot) {
                sb.append(ChatColor.GOLD).append(slot);
            } else {
                sb.append(ChatColor.GRAY).append(slot);
            }

            sb.append(ChatColor.DARK_GRAY).append(":");

            if (abilityId == null || abilityId.isEmpty()) {
                // Empty slot
                sb.append(ChatColor.DARK_GRAY).append("—");
            } else {
                // Has ability
                Ability ab = abilityRegistry.getAbility(abilityId);
                String icon = AbilityIconProvider.getIconFor(abilityId);

                long cdMs = abilityManager.getRemainingCooldown(player, abilityId);

                if (cdMs > 0) {
                    long secs = Math.max(1, cdMs / 1000);
                    sb.append(ChatColor.RED).append(icon).append(secs);
                } else {
                    sb.append(ChatColor.AQUA).append(icon);
                }
            }

            // Closing bracket
            sb.append(ChatColor.DARK_GRAY).append("]");

            if (slot < 5) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }
}
