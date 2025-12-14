package com.animesmp.core.combat;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared targeting helpers used across ability effects.
 */
public final class TargetingUtil {

    private TargetingUtil() {}

    public static LivingEntity findNearestLivingTarget(Player caster,
                                                      double radius,
                                                      boolean prioritizePlayers,
                                                      boolean includeMobsIfNoPlayers) {
        if (caster == null) return null;
        Location center = caster.getLocation();
        World world = center.getWorld();
        if (world == null) return null;

        List<LivingEntity> players = new ArrayList<>();
        List<LivingEntity> mobs = new ArrayList<>();

        for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.equals(caster)) continue;
            if (le.isDead()) continue;

            if (le instanceof Player) players.add(le);
            else mobs.add(le);
        }

        Comparator<LivingEntity> byDist = Comparator.comparingDouble(le -> le.getLocation().distanceSquared(center));

        if (prioritizePlayers && !players.isEmpty()) {
            players.sort(byDist);
            return players.get(0);
        }

        if (includeMobsIfNoPlayers) {
            List<LivingEntity> all = new ArrayList<>();
            all.addAll(players);
            all.addAll(mobs);
            if (all.isEmpty()) return null;
            all.sort(byDist);
            return all.get(0);
        }

        if (!players.isEmpty()) {
            players.sort(byDist);
            return players.get(0);
        }

        return null;
    }

    public static List<LivingEntity> getLivingTargetsInRadius(Player caster, Location center, double radius, boolean excludeCaster) {
        List<LivingEntity> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (excludeCaster && caster != null && le.equals(caster)) continue;
            if (le.isDead()) continue;
            out.add(le);
        }
        return out;
    }
}
