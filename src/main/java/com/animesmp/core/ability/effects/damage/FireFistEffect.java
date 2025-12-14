package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class FireFistEffect implements AbilityEffect {

    private final AnimeSMPPlugin plugin;

    public FireFistEffect(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, Ability ability) {
        World world = player.getWorld();
        Location origin = player.getEyeLocation().clone();
        Vector dir = origin.getDirection().normalize();

        double maxDistance = 8.0;
        double step = 0.5;
        double hitRadius = 1.2;

        Set<Entity> hit = new HashSet<>();

        for (double d = 1.0; d <= maxDistance; d += step) {
            Location point = origin.clone().add(dir.clone().multiply(d));

            world.spawnParticle(Particle.FLAME, point, 4, 0.2, 0.2, 0.2, 0.01);
            world.spawnParticle(Particle.SMALL_FLAME, point, 2, 0.1, 0.1, 0.1, 0.01);

            for (Entity e : world.getNearbyEntities(point, hitRadius, hitRadius, hitRadius)) {
                if (!(e instanceof LivingEntity target)) continue;
                if (target == player) continue;
                if (hit.contains(e)) continue;

                hit.add(e);
                plugin.getDamageCalculator().applyAbilityDamage(player, target, ability);
            }
        }
    }
}
