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

public class ShockwaveRoarEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location center = caster.getLocation().add(0, 1, 0);

        double radius = 6.0;
        double damageHearts = 7.0; // from config
        double damage = damageHearts * 2.0;

        // Expanding ring particles
        for (double r = 1.0; r <= radius; r += 0.7) {
            int points = (int) (r * 10);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location p = center.clone().add(x, 0, z);
                center.getWorld().spawnParticle(
                        Particle.EXPLOSION,
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
            le.damage(damage, caster);

            Vector kb = le.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize().multiply(1.0);
            kb.setY(0.4);
            le.setVelocity(le.getVelocity().add(kb));
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                1.2f,
                1.0f
        );
    }
}
