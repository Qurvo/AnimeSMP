package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.daily.DailyRewardManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class DailyMissionListener implements Listener {

    private final DailyRewardManager daily;

    public DailyMissionListener(AnimeSMPPlugin plugin) {
        this.daily = plugin.getDailyRewardManager();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();

        if (killer == null) return;

        if (dead instanceof Player victim) {
            if (victim.getUniqueId().equals(killer.getUniqueId())) return;
            daily.onPlayerKill(killer, victim);
        } else {
            daily.onMobKill(killer);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        Player player = event.getPlayer();

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal <= 0.05) return;

        daily.onTravel(player, horizontal);
    }
}
