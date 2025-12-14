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

public class InfernoNovaEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        Location center = caster.getLocation().add(0, 1.0, 0);
        double radius = 7.0;

        // Flame ring + explosion
        for (double r = 1.0; r <= radius; r += 0.7) {
            int points = (int) (r * 12);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location p = center.clone().add(x, 0, z);
                center.getWorld().spawnParticle(
                        Particle.FLAME,
                        p,
                        1,
                        0.05, 0.05, 0.05,
                        0.0
                );
            }
        }

        center.getWorld().spawnParticle(
                Particle.EXPLOSION,
                center,
                2,
                0.2, 0.2, 0.2,
                0.0
        );

        for (Entity e : caster.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e == caster) continue;

            // Main hit
            plugin.getDamageCalculator().applyAbilityDamage(caster, le, ability);

            // Burn that bypasses fire resistance (true DOT)
            le.setFireTicks(Math.max(le.getFireTicks(), 6 * 20));
            int runs = 6;
            double totalHp = 6.0; // 3 hearts total over 6s
            double perTick = totalHp / runs;
            for (int i = 0; i < runs; i++) {
                int delay = i * 20;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (le.isDead()) return;
                    plugin.getDamageCalculator().applyTrueDamage(caster, le, perTick);
                    le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1.0, 0), 8, 0.4, 0.5, 0.4, 0.02);
                }, delay);
            }

            Vector kb = le.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize().multiply(0.9);
            kb.setY(0.4);
            le.setVelocity(le.getVelocity().add(kb));
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_BLAZE_SHOOT,
                1.2f,
                0.8f
        );
        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                1.0f,
                1.0f
        );
    }
}
