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

public class CrescentTyphoonSlashEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location center = caster.getLocation().add(0, 1.0, 0);
        Vector forward = caster.getLocation().getDirection().normalize();

        double radius = 6.0;
        double maxAngleDeg = 80.0;
        double damageHearts = 7.5;
        double damage = damageHearts * 2.0;

        // Big crescent with wind
        for (double r = 2.0; r <= radius; r += 0.6) {
            for (double angle = -maxAngleDeg; angle <= maxAngleDeg; angle += 10.0) {
                Vector dir = forward.clone().rotateAroundY(Math.toRadians(angle));
                Location p = center.clone().add(dir.multiply(r));
                center.getWorld().spawnParticle(
                        Particle.CLOUD,
                        p,
                        1,
                        0.05, 0.05, 0.05,
                        0.0
                );
            }
        }

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

            Vector kb = to.normalize().multiply(0.7);
            kb.setY(0.25);
            le.setVelocity(le.getVelocity().add(kb));
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                1.1f
        );
    }
}
