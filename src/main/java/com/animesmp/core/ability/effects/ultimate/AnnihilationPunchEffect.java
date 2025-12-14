package com.animesmp.core.ability.effects.ultimate;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AnnihilationPunchEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        // Short dash
        Vector dash = caster.getLocation().getDirection().normalize().multiply(1.2);
        dash.setY(0.2);
        caster.setVelocity(dash);

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                1.0f,
                0.9f
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            Location origin = caster.getLocation().add(0, 1.0, 0);
            Vector forward = caster.getLocation().getDirection().normalize();

            double maxDistance = 4.0;
            double maxAngleDeg = 25.0;

            LivingEntity best = null;

            // Ray trace first (solves "too close" whiffs)
            var ray = caster.getWorld().rayTraceEntities(
                    caster.getEyeLocation(),
                    caster.getEyeLocation().getDirection(),
                    5.0,
                    1.4,
                    (ent) -> ent instanceof LivingEntity && ent != caster
            );
            if (ray != null && ray.getHitEntity() instanceof LivingEntity hit) {
                best = hit;
            }

            double bestDistSq = Double.MAX_VALUE;

            for (Entity e : caster.getWorld().getNearbyEntities(origin, maxDistance, maxDistance, maxDistance)) {
                if (!(e instanceof LivingEntity)) continue;
                if (e == caster) continue;
                if (best != null && e == best) continue;

                LivingEntity le = (LivingEntity) e;
                Vector to = le.getLocation().add(0, 1, 0).toVector()
                        .subtract(origin.toVector());

                double distSq = to.lengthSquared();
                if (distSq > maxDistance * maxDistance) continue;

                double angleDeg = Math.toDegrees(forward.angle(to));
                if (angleDeg > maxAngleDeg) continue;

                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = le;
                }
            }

            if (best == null) {
                origin.getWorld().spawnParticle(
                        Particle.LARGE_SMOKE,
                        origin,
                        10,
                        0.5, 0.5, 0.5,
                        0.0
                );
                return;
            }

            Location hitLoc = best.getLocation().add(0, 1.0, 0);

            hitLoc.getWorld().spawnParticle(
                    Particle.EXPLOSION,
                    hitLoc,
                    2,
                    0.2, 0.2, 0.2,
                    0.0
            );
            hitLoc.getWorld().spawnParticle(
                    Particle.CRIT,
                    hitLoc,
                    20,
                    0.4, 0.4, 0.4,
                    0.0
            );

            // Use config-based ability damage (hybrid true-damage model applied in DamageCalculator)
            plugin.getDamageCalculator().applyAbilityDamage(caster, best, ability);

            Vector kb = best.getLocation().toVector()
                    .subtract(caster.getLocation().toVector())
                    .normalize().multiply(1.4);
            kb.setY(0.7);
            best.setVelocity(best.getVelocity().add(kb));

            caster.getWorld().playSound(
                    caster.getLocation(),
                    Sound.ENTITY_GENERIC_EXPLODE,
                    1.2f,
                    0.7f
            );

        }, 6L);
    }
}
