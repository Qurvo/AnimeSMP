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

public class FullMoonTyphoonEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        final Location origin = caster.getLocation().add(0, 1.0, 0);
        final Vector forward = caster.getLocation().getDirection().normalize();

        final double maxRadius = 8.0;
        final double maxAngleDeg = 110.0;
        final double damageHearts = 13.0;
        final double damage = damageHearts * 2.0;

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                1.0f,
                1.2f
        );

        // multi-wave rings
        int waves = 3;
        for (int w = 0; w < waves; w++) {
            final double startRadius = 2.0 + w * 1.5;
            final int waveIndex = w;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double r = startRadius;
                while (r <= maxRadius) {
                    int points = (int) (r * 14);
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        Location p = origin.clone().add(x, 0, z);
                        origin.getWorld().spawnParticle(
                                Particle.CLOUD,
                                p,
                                1,
                                0.05, 0.05, 0.05,
                                0.0
                        );
                    }
                    r += 1.5;
                }

                // Damage + knockback
                for (Entity e : origin.getWorld().getNearbyEntities(origin, maxRadius, maxRadius, maxRadius)) {
                    if (!(e instanceof LivingEntity)) continue;
                    if (e == caster) continue;

                    LivingEntity le = (LivingEntity) e;
                    Vector to = le.getLocation().add(0, 1, 0).toVector()
                            .subtract(origin.toVector());

                    double angleDeg = Math.toDegrees(forward.angle(to));
                    if (angleDeg > maxAngleDeg) continue;
                    if (to.lengthSquared() > maxRadius * maxRadius) continue;

                    le.damage(damage, caster);

                    Vector kb = to.normalize().multiply(0.9);
                    kb.setY(0.5 + 0.2 * waveIndex);
                    le.setVelocity(le.getVelocity().add(kb));
                }

            }, w * 10L); // each wave 0.5s apart
        }
    }
}
