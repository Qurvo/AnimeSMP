package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ArcaneBurstEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        if (caster == null || !caster.isOnline()) return;

        Location center = caster.getLocation().add(0, 1.0, 0);
        double radius = 5.0;

        // Visual burst at center
        caster.getWorld().spawnParticle(
                Particle.END_ROD,
                center,
                40,
                0.4, 0.6, 0.4,
                0.02
        );
        caster.getWorld().spawnParticle(
                Particle.ENCHANT,
                center,
                30,
                0.6, 0.8, 0.6,
                0.01
        );
        caster.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);

        // Damage nearby enemies
        double baseHearts = ability.getBaseDamageHearts() > 0 ? ability.getBaseDamageHearts() : 5.0;
        double damage = baseHearts * 2.0; // hearts → health points

        for (Entity e : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity target)) continue;
            if (target == caster) continue;
            if (!target.isValid() || target.isDead()) continue;

            // Simple team/owner checks could go here later

            if (target.getLocation().distanceSquared(center) <= radius * radius) {
                target.damage(damage, caster);

                // Small hit puff on each enemy
                Location hitLoc = target.getLocation().add(0, 1.0, 0);
                target.getWorld().spawnParticle(
                        Particle.CRIT,
                        hitLoc,
                        12,
                        0.3, 0.4, 0.3,
                        0.02
                );
            }
        }
    }
}
