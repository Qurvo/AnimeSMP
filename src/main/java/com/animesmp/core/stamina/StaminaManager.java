package com.animesmp.core.stamina;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaminaManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final Set<UUID> inCombat = new HashSet<>();
    private BukkitTask task;

    public StaminaManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        startTask();
    }

    private void startTask() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tickPlayer(player);
            }
        }, 20L, 20L);
    }

    private void tickPlayer(Player player) {
        PlayerProfile profile = profiles.getProfile(player);

        double current = profile.getStaminaCurrent();
        double cap = profile.getStaminaCap();

        double regenOutOfCombat = 6.0;
        double regenInCombat = 3.0;

        double regen = inCombat.contains(player.getUniqueId()) ? regenInCombat : regenOutOfCombat;

        regen += profile.getTrainingLevel() * 0.1;

        double updated = Math.min(cap, current + regen);

        profile.setStaminaCurrent(updated);
        profile.setStaminaRegenPerSecond(regen);
    }

    public boolean hasStamina(Player player, int cost) {
        return profiles.getProfile(player).getStaminaCurrent() >= cost;
    }

    public boolean consumeStamina(Player player, int cost) {
        PlayerProfile profile = profiles.getProfile(player);

        double current = profile.getStaminaCurrent();
        if (current < cost) return false;

        profile.setStaminaCurrent(current - cost);
        markInCombat(player);
        return true;
    }

    public void addStamina(Player player, double amount) {
        PlayerProfile profile = profiles.getProfile(player);

        double updated = Math.min(profile.getStaminaCap(), profile.getStaminaCurrent() + amount);
        profile.setStaminaCurrent(updated);
    }

    public void markInCombat(Player player) {
        inCombat.add(player.getUniqueId());
    }
}
