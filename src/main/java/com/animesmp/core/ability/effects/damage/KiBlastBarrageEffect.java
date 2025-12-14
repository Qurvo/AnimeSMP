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

public class KiBlastBarrageEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location origin = caster.getEyeLocation().clone();
        Vector forward = origin.getDirection().normalize();

        double maxDistance = 12.0;
        double step = 0.6;
        double radius = 2.0;

        // Config: ki_blast_barrage damage-hearts: 4.0 (multi-hit feeling)
        double damageHearts = 4.0;
        double damage = damageHearts * 2.0;

        // Visual: rapid streak of blasts
        for (double t = 0.0; t < maxDistance; t += step) {
            Location point = origin.clone().add(forward.clone().multiply(t));

            origin.getWorld().spawnParticle(
                    Particle.END_ROD,
                    point,
                    2,
                    0.05, 0.05, 0.05,
                    0.0
            );
        }

        // Hit: small cylinder along the line
        for (Entity e : caster.getWorld().getNearbyEntities(
                caster.getLocation(), maxDistance, 3, maxDistance)) {

            if (!(e instanceof LivingEntity)) continue;
            if (e == caster) continue;

            LivingEntity le = (LivingEntity) e;
            Location eye = caster.getEyeLocation();
            Vector toTarget = le.getLocation().add(0, 1, 0).toVector()
                    .subtract(eye.toVector());

            double projLen = toTarget.dot(forward); // distance along forward
            if (projLen < 0 || projLen > maxDistance) continue;

            // distance from line
            Vector closest = eye.toVector().add(forward.clone().multiply(projLen));
            double distSq = le.getLocation().add(0, 1, 0).toVector()
                    .subtract(closest).lengthSquared();
            if (distSq > radius * radius) continue;

            le.damage(damage, caster);
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                0.8f,
                1.5f
        );
    }
}
