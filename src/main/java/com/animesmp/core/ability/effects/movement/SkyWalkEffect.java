package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkyWalkEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin != null) {
            plugin.getStatusEffectManager().grantFallImmunity(caster, 6000L);
        }

        // Strong upward + forward boost
        Vector vel = caster.getLocation().getDirection().normalize().multiply(0.6);
        vel.setY(0.9);

        caster.setVelocity(vel);

        caster.getWorld().spawnParticle(
                Particle.CLOUD,
                caster.getLocation().add(0, 0.1, 0),
                12,
                0.5, 0.1, 0.5,
                0.0
        );

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_PHANTOM_FLAP,
                0.8f,
                1.2f
        );
    }
}
