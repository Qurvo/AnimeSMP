package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.combat.KillCooldownManager;
import com.animesmp.core.level.LevelManager;
import com.animesmp.core.pd.PdEventManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class CombatRewardListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final LevelManager levelManager;
    private final PlayerProfileManager profiles;
    private final PdEventManager pdEvents;
    private final KillCooldownManager killCooldowns;

    public CombatRewardListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getLevelManager();
        this.profiles = plugin.getProfileManager();
        this.pdEvents = plugin.getPdEventManager();
        this.killCooldowns = plugin.getKillCooldownManager();
    }

    // ------------------------------------------------
    // PLAYER KILLS (XP + Yen steal)
    // ------------------------------------------------
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) {
            return;
        }

        UUID kId = killer.getUniqueId();
        UUID vId = victim.getUniqueId();

        // Anti kill-farm cooldown (global for all kill-based systems)
        // Longer during PD to prevent token/yen/xp farming while PD is active.
        long cooldownMs = pdEvents.isActive()
                ? (15L * 60L * 1000L)   // 15 minutes during PD
                : (5L * 60L * 1000L);   // 5 minutes normally

        boolean canReward = killCooldowns.canRewardKill(kId, vId, cooldownMs);
        if (!canReward) {
            // Reduced rewards message + reduced rewards (no yen steal, reduced XP)
            killer.sendMessage(ChatColor.RED + "[Anti-Farm] " + ChatColor.GRAY +
                    "Rewards reduced for repeatedly killing " + ChatColor.YELLOW + victim.getName() +
                    ChatColor.GRAY + (pdEvents.isActive() ? " (PD cooldown)" : "") + ".");

            int baseXpReduced = plugin.getConfig().getInt("combat.xp-per-kill.player", 50);
            baseXpReduced = (int) Math.max(0, Math.round(baseXpReduced * 0.20)); // 20% XP
            if (baseXpReduced > 0) {
                levelManager.giveXp(killer, baseXpReduced);
            }
            return;
        }

        // -------- XP REWARD (base, PD multiplier handled in LevelManager) --------
        int baseXp = plugin.getConfig().getInt("combat.xp-per-kill.player", 50);
        if (baseXp > 0) {
            levelManager.giveXp(killer, baseXp);
        }

        // -------- YEN STEAL LOGIC --------
        // During PD: 50% of victim's yen goes to killer
        // Outside PD: 25% of victim's yen goes to killer

        PlayerProfile vProfile = profiles.getProfile(victim);
        PlayerProfile kProfile = profiles.getProfile(killer);

        int victimYen = vProfile.getYen();

        // PD is intended to be 2x gains. 25% -> 50% is exactly 2x.
        double rate = pdEvents.isActive() ? 0.50 : 0.25;
        int steal = (int) Math.floor(victimYen * rate);

        if (steal > 0) {
            // Adjust balances
            vProfile.setYen(victimYen - steal);
            kProfile.setYen(kProfile.getYen() + steal);

            profiles.saveProfile(vProfile);
            profiles.saveProfile(kProfile);

            // Messages
            killer.sendMessage(ChatColor.GOLD + "[Combat] " + ChatColor.YELLOW +
                    "You stole " + ChatColor.GOLD + steal + "¥ " +
                    ChatColor.YELLOW + "from " + ChatColor.RED + victim.getName() +
                    ChatColor.YELLOW + (pdEvents.isActive() ? " (PD bonus: 50%)" : " (25%)"));

            victim.sendMessage(ChatColor.RED + "[Combat] " + ChatColor.DARK_RED +
                    killer.getName() + ChatColor.RED +
                    " stole " + ChatColor.GOLD + steal + "¥ " +
                    ChatColor.RED + "from you on death.");
        }
    }

    // ------------------------------------------------
    // MOB KILLS (XP only, no yen transfer)
    // ------------------------------------------------
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        // Only mobs, not players
        if (entity instanceof Player) return;

        int baseXp = plugin.getConfig().getInt("combat.xp-per-kill.mob", 10);
        if (baseXp <= 0) return;

        levelManager.giveXp(killer, baseXp);
    }
}
