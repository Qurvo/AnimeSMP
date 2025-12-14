package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.combat.DamageCalculator;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class GetsuWaveEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        World world = player.getWorld();
        DamageCalculator calc = AnimeSMPPlugin.getInstance().getDamageCalculator();

        Vector dir = player.getLocation().getDirection().normalize();
        Location origin = player.getEyeLocation().clone();

        double maxDistance = 10.0;
        double step = 0.7;

        for (double d = 1.0; d <= maxDistance; d += step) {
            Location point = origin.clone().add(dir.clone().multiply(d));

            // Main wave particles (water / magic)
            world.spawnParticle(Particle.SPLASH, point, 6, 0.3, 0.2, 0.3, 0.05);
            world.spawnParticle(Particle.END_ROD, point, 2, 0.1, 0.1, 0.1, 0.0);

            // Slight side "ripples" to make it look wider
            Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(0.8);
            Location left = point.clone().add(side);
            Location right = point.clone().subtract(side);
            world.spawnParticle(Particle.SPLASH, left, 3, 0.15, 0.1, 0.15, 0.03);
            world.spawnParticle(Particle.SPLASH, right, 3, 0.15, 0.1, 0.15, 0.03);

            // Damage in a small radius along the wave
            double radius = 1.0;
            for (LivingEntity target : point.getNearbyLivingEntities(radius)) {
                if (target == player) continue;
                if (target.isDead()) continue;

                calc.applyAbilityDamage(player, target, ability);
            }
        }
    }
}
