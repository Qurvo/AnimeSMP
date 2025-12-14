package com.animesmp.core.ability.util;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Movement helper for "dash" abilities.
 *
 * Design goals:
 * - Feel like a physical dash/launch (velocity-based), not an instant teleport.
 * - Remain deterministic for PvP (short, controlled window).
 * - Stop on collision (feet + head space must be passable).
 */
public final class DashUtil {

    private DashUtil() {}

    /**
     * Dashes the player forward using velocity over a short number of ticks.
     *
     * @param player        player to move
     * @param totalDistance total horizontal distance in blocks (approx)
     * @param ticks         number of ticks to spread the movement across
     * @param yBoost        small Y boost to help with slabs / micro-steps (0.0–0.10 recommended)
     */
    public static void dash(Player player, double totalDistance, int ticks, double yBoost) {
        dash(player, totalDistance, ticks, yBoost, 0.86);
    }

    /**
     * Advanced dash with custom drag.
     *
     * @param drag velocity decay per tick (0.80–0.92 recommended; lower = snappier stop)
     */
    public static void dash(Player player, double totalDistance, int ticks, double yBoost, double drag) {
        if (player == null || ticks <= 0 || totalDistance <= 0) return;

        final AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        final Location start = player.getLocation().clone();
        final Vector dir = start.getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 1e-6) return;

        final World world = player.getWorld();

        // Convert distance/ticks to a reasonable initial velocity.
        // Vanilla sprint speed is ~0.215 blocks/tick; dashes should be above that but controlled.
        final double blocksPerTick = totalDistance / Math.max(1.0, ticks);
        final double initialSpeed = Math.max(0.25, Math.min(1.25, blocksPerTick * 1.20));

        final Vector baseVel = dir.multiply(initialSpeed).setY(yBoost);

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.8f);

        new BukkitRunnable() {
            int i = 0;
            Vector vel = baseVel.clone();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                if (i++ >= ticks) {
                    cancel();
                    return;
                }

                // Collision check using predicted next position.
                Location loc = player.getLocation();
                Location next = loc.clone().add(vel);

                Block feet = next.getBlock();
                Block head = feet.getRelative(0, 1, 0);
                if (!feet.isPassable() || !head.isPassable()) {
                    cancel();
                    return;
                }

                // Apply velocity (launch feel)
                player.setVelocity(vel);

                // Subtle dash trail
                world.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1.0, 0), 4, 0.15, 0.10, 0.15, 0.01);

                // Decay velocity
                vel = vel.multiply(drag);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
