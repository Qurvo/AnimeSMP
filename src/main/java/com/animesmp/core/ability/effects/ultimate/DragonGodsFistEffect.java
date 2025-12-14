package com.animesmp.core.ability.effects.ultimate;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DragonGodsFistEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location center = caster.getLocation().add(0, 1.0, 0);
        Vector forward = caster.getLocation().getDirection().normalize();

        double radius = 6.0;
        double maxAngleDeg = 70.0;
        double damageHearts = 12.5;
        double damage = damageHearts * 2.0;

        // Dragon-ish vertical spiral
        for (double y = 0; y <= 4.0; y += 0.4) {
            double r = 1.0 + (y * 0.5);
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location p = center.clone().add(x, y, z);
                center.getWorld().spawnParticle(
                        Particle.FLAME,
                        p,
                        1,
                        0.02, 0.02, 0.02,
                        0.0
                );
            }
        }

        for (Entity e : caster.getWorld().getNearbyEntities(center, radius, 4.0, radius)) {
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
            kb.setY(1.0);
            le.setVelocity(le.getVelocity().add(kb));
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                1.3f,
                0.9f
        );
    }
}
