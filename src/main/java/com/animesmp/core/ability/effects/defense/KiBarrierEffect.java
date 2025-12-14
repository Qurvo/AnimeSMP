package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class KiBarrierEffect implements AbilityEffect {

    private static final double KNOCKBACK_RADIUS = 4.0;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null || player == null) return;

        World world = player.getWorld();

        // Knockback nearby entities (mobs + players), excluding caster
        for (Entity e : player.getNearbyEntities(KNOCKBACK_RADIUS, KNOCKBACK_RADIUS, KNOCKBACK_RADIUS)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.equals(player)) continue;

            Vector kb = le.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(0.65)
                    .setY(0.32);
            le.setVelocity(kb);
        }

        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.0, 0), 60, 1.2, 1.2, 1.2, 0.0);
        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.3f);
        player.sendMessage(ChatColor.AQUA + "Your Ki flares outward, forming a barrier!");

        int durationTicks = 6 * 20;
        int radius = 3;

        // Center at player's feet block, but keep dome centered nicely
        Location center = player.getLocation().getBlock().getLocation().add(0.5, 0.5, 0.5);
        Material domeMat = Material.OBSIDIAN;

        // Track all blocks we change so we can restore perfectly (shell + floor)
        Map<Location, Material> previous = new HashMap<>();
        Set<Location> placed = new HashSet<>();

        // Build list for shell (dome walls)
        List<Location> shell = computeSphereShell(center, radius, 0.40);
        shell.sort(Comparator.comparingInt(Location::getBlockY));

        // Place floor immediately so you get the arena floor before the dome finishes building
        placeFloorDisk(center, radius, domeMat, previous, placed);

        new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                // Finished building the shell
                if (idx >= shell.size()) {
                    cancel();

                    // Register barrier for your status manager (uses placed set)
                    plugin.getStatusEffectManager().registerKiBarrier(player, placed);

                    // Dissolve later (must restore *all* placed blocks including floor)
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> dissolve(plugin, player, placed, previous),
                            durationTicks);
                    return;
                }

                // Build bottom-up by layers for the nice formation animation
                int y = shell.get(idx).getBlockY();
                while (idx < shell.size() && shell.get(idx).getBlockY() == y) {
                    Location l = shell.get(idx++);
                    placeIfAllowed(l, domeMat, previous, placed);
                }

                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.0, 0), 10, 0.8, 0.8, 0.8, 0.0);
                world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 1.8f);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Dissolve top-down and restore every block we changed (shell + floor).
     */
    private void dissolve(AnimeSMPPlugin plugin,
                          Player owner,
                          Set<Location> placed,
                          Map<Location, Material> previous) {

        // Restore everything we placed, top-down for visuals
        List<Location> rev = new ArrayList<>(placed);
        rev.sort((a, b) -> Integer.compare(b.getBlockY(), a.getBlockY()));

        new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (idx >= rev.size()) {
                    cancel();
                    plugin.getStatusEffectManager().clearKiBarrier(owner);
                    return;
                }

                int y = rev.get(idx).getBlockY();
                while (idx < rev.size() && rev.get(idx).getBlockY() == y) {
                    Location l = rev.get(idx++);
                    restore(l, placed, previous);
                }

                if (owner.isOnline()) {
                    owner.getWorld().spawnParticle(Particle.SMOKE, owner.getLocation().add(0, 1.0, 0),
                            10, 0.8, 0.8, 0.8, 0.0);
                    owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                            0.25f, 0.9f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static List<Location> computeSphereShell(Location center, int radius, double thickness) {
        List<Location> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;

        double r = radius;
        double min = r - thickness;
        double max = r + thickness;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist < min || dist > max) continue;
                    out.add(center.clone().add(x, y, z).getBlock().getLocation());
                }
            }
        }

        Map<String, Location> uniq = new HashMap<>();
        for (Location l : out) {
            uniq.put(l.getWorld().getName() + ":" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(), l);
        }
        return new ArrayList<>(uniq.values());
    }

    /**
     * Places an obsidian floor disk (replaces surface blocks too) and tracks originals for restore.
     */
    private static void placeFloorDisk(Location center,
                                       int radius,
                                       Material mat,
                                       Map<Location, Material> previous,
                                       Set<Location> placed) {

        World w = center.getWorld();
        if (w == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY(); // player's feet-level block layer
        int cz = center.getBlockZ();

        int r2 = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > r2) continue;

                Location l = new Location(w, cx + x, cy, cz + z);
                Block b = l.getBlock();

                // Replace any surface block for the floor (as requested)
                if (!previous.containsKey(l)) {
                    previous.put(l, b.getType());
                }
                b.setType(mat, false);
                placed.add(l);
            }
        }
    }

    private static void placeIfAllowed(Location l,
                                       Material mat,
                                       Map<Location, Material> previous,
                                       Set<Location> placed) {
        if (l == null || l.getWorld() == null) return;

        Block b = l.getBlock();
        Material current = b.getType();

        if (!isReplaceable(current)) return;

        // Store original once
        previous.putIfAbsent(l, current);

        b.setType(mat, false);
        placed.add(l);
    }

    private static boolean isReplaceable(Material m) {
        return m == Material.AIR
                || m == Material.CAVE_AIR
                || m == Material.VOID_AIR
                || m == Material.WATER
                || m == Material.LAVA
                || m == Material.GRASS_BLOCK
                || m == Material.TALL_GRASS
                || m == Material.SEAGRASS
                || m == Material.SNOW
                || m == Material.VINE;
    }

    private static void restore(Location l,
                                Set<Location> placed,
                                Map<Location, Material> previous) {
        if (l == null || l.getWorld() == null) return;
        if (!placed.contains(l)) return;

        Block b = l.getBlock();
        Material prev = previous.getOrDefault(l, Material.AIR);
        b.setType(prev, false);
    }
}
