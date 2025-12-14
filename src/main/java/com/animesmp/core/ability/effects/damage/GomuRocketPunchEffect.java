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

public class GomuRocketPunchEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location origin = caster.getEyeLocation().clone();
        Vector dir = origin.getDirection().normalize();

        double maxDistance = 16.0;
        double step = 0.7;

        LivingEntity hit = null;

        for (double t = 0; t < maxDistance; t += step) {
            Location point = origin.clone().add(dir.clone().multiply(t));

            point.getWorld().spawnParticle(
                    Particle.CRIT,
                    point,
                    2,
                    0.03, 0.03, 0.03,
                    0.0
            );

            for (Entity e : point.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                if (!(e instanceof LivingEntity)) continue;
                if (e == caster) continue;
                hit = (LivingEntity) e;
                break;
            }
            if (hit != null) break;
        }

        if (hit != null) {
            // Config: gomu_rocket_punch damage-hearts: 3.5
            double damageHearts = 3.5;
            double damage = damageHearts * 2.0;

            hit.damage(damage, caster);

            Vector kb = hit.getLocation().toVector()
                    .subtract(caster.getLocation().toVector())
                    .normalize()
                    .multiply(0.8);
            kb.setY(0.25);
            hit.setVelocity(hit.getVelocity().add(kb));

            hit.getWorld().playSound(
                    hit.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_STRONG,
                    1.0f,
                    1.0f
            );
        } else {
            caster.getWorld().playSound(
                    caster.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                    0.7f,
                    1.4f
            );
        }
    }
}
