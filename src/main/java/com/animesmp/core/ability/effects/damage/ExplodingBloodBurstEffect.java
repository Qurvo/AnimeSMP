package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ExplodingBloodBurstEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        Location center = caster.getLocation().add(0, 1.0, 0);

        double radius = 4.5;

        // Hard-coded based on config: 6.5 hearts
        double damageHearts = 6.5;
        double damage = damageHearts * 2.0;

        // Bloody explosion
        center.getWorld().spawnParticle(
                Particle.CRIMSON_SPORE,
                center,
                80,
                1.0, 0.8, 1.0,
                0.1
        );
        center.getWorld().spawnParticle(
                Particle.CLOUD,
                center,
                25,
                0.6, 0.6, 0.6,
                0.02
        );

        for (Entity e : caster.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == caster) continue;

            LivingEntity le = (LivingEntity) e;
            le.damage(damage, caster);
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                0.9f,
                0.7f
        );
    }
}
