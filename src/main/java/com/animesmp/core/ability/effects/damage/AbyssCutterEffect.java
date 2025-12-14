package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AbyssCutterEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location origin = caster.getLocation().add(0, 1.0, 0);
        Vector forward = caster.getLocation().getDirection().normalize();

        double maxDistance = 10.0;
        double step = 0.5;
        double radius = 1.5;

        // Config: abyss_cutter damage-hearts: 4.0
        double damageHearts = 4.0;
        double damage = damageHearts * 2.0;

        // Dark horizontal wave
        for (double t = 0; t < maxDistance; t += step) {
            Location point = origin.clone().add(forward.clone().multiply(t));

            point.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    point,
                    2,
                    0.1, 0.1, 0.1,
                    0.0
            );
            point.getWorld().spawnParticle(
                    Particle.ENCHANT,
                    point,
                    1,
                    0.05, 0.05, 0.05,
                    0.0
            );

            for (Entity e : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!(e instanceof LivingEntity)) continue;
                if (e == caster) continue;

                ((LivingEntity) e).damage(damage, caster);
            }
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_WITHER_HURT,
                0.9f,
                0.8f
        );
    }
}
