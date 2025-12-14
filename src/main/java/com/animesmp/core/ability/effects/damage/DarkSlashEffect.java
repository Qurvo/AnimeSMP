package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DarkSlashEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        Location center = caster.getLocation().add(0, 1.0, 0);

        double radius = 3.0;

        // Hard-coded based on config: 4.0 hearts
        double damageHearts = 4.0;
        double damage = damageHearts * 2.0;

        // Dark burst
        center.getWorld().spawnParticle(
                Particle.LARGE_SMOKE,
                center,
                40,
                0.7, 0.7, 0.7,
                0.01
        );
        center.getWorld().spawnParticle(
                Particle.ENCHANT,
                center,
                30,
                0.6, 0.6, 0.6,
                0.1
        );

        for (Entity e : caster.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == caster) continue;

            ((LivingEntity) e).damage(damage, caster);
        }

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                0.8f,
                0.5f
        );
    }
}
