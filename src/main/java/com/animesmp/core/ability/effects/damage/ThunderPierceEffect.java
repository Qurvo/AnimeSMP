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

public class ThunderPierceEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location eye = caster.getEyeLocation().clone();
        Vector dir = eye.getDirection().normalize();

        double maxDistance = 18.0;
        double step = 0.5;

        LivingEntity target = null;

        // Visual lightning line + hit detection
        for (double t = 0; t < maxDistance; t += step) {
            Location point = eye.clone().add(dir.clone().multiply(t));

            point.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    point,
                    3,
                    0.03, 0.03, 0.03,
                    0.0
            );

            for (Entity e : point.getWorld().getNearbyEntities(point, 0.4, 0.4, 0.4)) {
                if (!(e instanceof LivingEntity)) continue;
                if (e == caster) continue;
                target = (LivingEntity) e;
                break;
            }
            if (target != null) break;
        }

        if (target != null) {
            // Config: thunder_pierce damage-hearts: 4.5
            double damageHearts = 4.5;
            double damage = damageHearts * 2.0;

            target.damage(damage, caster);

            target.getWorld().playSound(
                    target.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                    0.8f,
                    1.6f
            );
        } else {
            caster.getWorld().playSound(
                    caster.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                    0.6f,
                    1.8f
            );
        }
    }
}
