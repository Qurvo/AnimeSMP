package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

public class RotatingVendorHologram {

    private final AnimeSMPPlugin plugin;
    private final Location baseLocation;

    private ArmorStand line1;
    private ArmorStand line2;

    private BukkitRunnable task;

    public RotatingVendorHologram(AnimeSMPPlugin plugin, Location baseLocation) {
        this.plugin = plugin;
        this.baseLocation = baseLocation.clone();

        spawnHologram();
        startUpdater();
    }

    // -------------------------------------------------------------
    // SPAWNING & UPDATING
    // -------------------------------------------------------------

    private void spawnHologram() {
        World world = baseLocation.getWorld();
        if (world == null) return;

        // First line slightly above base location
        Location l1 = baseLocation.clone().add(0, 1.5, 0);
        Location l2 = baseLocation.clone().add(0, 1.2, 0);

        line1 = spawnLine(l1, ChatColor.GOLD + "Daily Ability Vendor");
        line2 = spawnLine(l2, ChatColor.YELLOW + "Next refresh in: ...");
    }

    private ArmorStand spawnLine(Location loc, String text) {
        World world = loc.getWorld();
        if (world == null) return null;

        ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomNameVisible(true);
            as.setCustomName(text);
            as.setSmall(true);
        });
        return stand;
    }

    private void startUpdater() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isEnabled()) {
                    cancel();
                    return;
                }

                RotatingVendorManager manager = plugin.getRotatingVendorManager();
                if (manager == null) return;

                String time = manager.getFormattedTimeRemaining();

                if (line1 != null && !line1.isDead()) {
                    line1.setCustomName(ChatColor.GOLD + "Daily Ability Vendor");
                }

                if (line2 != null && !line2.isDead()) {
                    line2.setCustomName(ChatColor.YELLOW + "Next refresh in: " + ChatColor.GOLD + time);
                }
            }
        };

        // Update every 10 seconds; we only show minutes, so this is plenty
        task.runTaskTimer(plugin, 20L, 200L);
    }

    // -------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------

    public void destroy() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (line1 != null) {
            line1.remove();
            line1 = null;
        }

        if (line2 != null) {
            line2.remove();
            line2 = null;
        }
    }
}
