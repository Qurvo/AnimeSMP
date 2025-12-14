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

public class BloodSickleSlashEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location origin = caster.getLocation().add(0, 1.3, 0);
        Vector forward = origin.getDirection().normalize();

        double radius = 4.0;
        double maxAngleDeg = 55.0;

        // Config: blood_sickle_slash damage-hearts: 4.0
        double damageHearts = 4.0;
        double damage = damageHearts * 2.0;

        // Arc of “blood blades”
        for (double angle = -maxAngleDeg; angle <= maxAngleDeg; angle += 5.0) {
            Vector dir = forward.clone().rotateAroundY(Math.toRadians(angle));
            Location point = origin.clone().add(dir.multiply(radius));

            origin.getWorld().spawnParticle(
                    Particle.CRIMSON_SPORE,
                    point,
                    3,
                    0.06, 0.06, 0.06,
                    0.0
            );
        }

        for (Entity e : caster.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == caster) continue;

            LivingEntity le = (LivingEntity) e;
            Vector to = le.getLocation().add(0, 1, 0).toVector()
                    .subtract(origin.toVector());

            double angle = forward.angle(to);
            double angleDeg = Math.toDegrees(angle);
            if (angleDeg > maxAngleDeg) continue;
            if (to.lengthSquared() > radius * radius) continue;

            le.damage(damage, caster);
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                0.9f,
                1.2f
        );
    }
}
