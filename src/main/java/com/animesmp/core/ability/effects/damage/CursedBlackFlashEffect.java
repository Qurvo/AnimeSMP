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

public class CursedBlackFlashEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location center = caster.getLocation().add(0, 1.0, 0);
        Vector forward = caster.getLocation().getDirection().normalize();

        double radius = 3.0;
        double maxAngleDeg = 40.0;
        double damageHearts = 8.0;
        double damage = damageHearts * 2.0;

        // Dark flash around hand
        center.getWorld().spawnParticle(
                Particle.LARGE_SMOKE,
                center,
                12,
                0.4, 0.4, 0.4,
                0.0
        );
        center.getWorld().spawnParticle(
                Particle.CRIT,
                center,
                8,
                0.3, 0.3, 0.3,
                0.0
        );

        for (Entity e : caster.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == caster) continue;

            LivingEntity le = (LivingEntity) e;
            Vector to = le.getLocation().add(0, 1, 0).toVector()
                    .subtract(center.toVector());

            double angleDeg = Math.toDegrees(forward.angle(to));
            if (angleDeg > maxAngleDeg) continue;
            if (to.lengthSquared() > radius * radius) continue;

            le.damage(damage, caster);
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                1.0f,
                0.7f
        );
    }
}
