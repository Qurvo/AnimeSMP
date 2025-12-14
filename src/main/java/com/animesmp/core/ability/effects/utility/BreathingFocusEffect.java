package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.stamina.StaminaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BreathingFocusEffect implements AbilityEffect {

    private static final int DURATION_TICKS = 8 * 20;
    private static final int TICK_INTERVAL = 10; // every 0.5s

    @Override
    public void execute(Player player, Ability ability) {
        StaminaManager stamina = AnimeSMPPlugin.getInstance().getStaminaManager();

        player.getWorld().spawnParticle(
                Particle.CLOUD,
                player.getLocation().add(0, 1.0, 0),
                20,
                0.7, 1.0, 0.7,
                0.02
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_BREATH,
                0.8f,
                1.0f
        );

        player.sendMessage(ChatColor.BLUE + "You focus your breathing, rapidly restoring stamina.");

        // Extra stamina regen pulses over ~8 seconds
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= DURATION_TICKS) {
                    cancel();
                    return;
                }

                // Small chunk each pulse – tune as needed
                stamina.addStamina(player, 5);

                ticks += TICK_INTERVAL;
            }
        }.runTaskTimer(AnimeSMPPlugin.getInstance(), 0L, TICK_INTERVAL);
    }
}
