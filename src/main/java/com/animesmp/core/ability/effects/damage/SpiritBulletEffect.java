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

public class SpiritBulletEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        Location eye = caster.getEyeLocation().clone();
        Vector dir = eye.getDirection().normalize();

        double maxDistance = 20.0;
        double step = 0.6;

        LivingEntity hitTarget = null;

        for (double traveled = 0.0; traveled < maxDistance; traveled += step) {
            eye.add(dir);

            // Particle trail
            eye.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    eye,
                    2,
                    0.02, 0.02, 0.02,
                    0.0
            );

            for (Entity e : eye.getWorld().getNearbyEntities(eye, 0.5, 0.5, 0.5)) {
                if (!(e instanceof LivingEntity)) continue;
                if (e == caster) continue;

                hitTarget = (LivingEntity) e;
                break;
            }

            if (hitTarget != null) break;
        }

        if (hitTarget != null) {
            // Hard-coded based on config: 3.0 hearts
            double damageHearts = 3.0;
            double damage = damageHearts * 2.0;

            hitTarget.damage(damage, caster);

            hitTarget.getWorld().playSound(
                    hitTarget.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_HURT,
                    0.7f,
                    1.4f
            );
        } else {
            caster.getWorld().playSound(
                    caster.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                    0.4f,
                    1.8f
            );
        }
    }
}
