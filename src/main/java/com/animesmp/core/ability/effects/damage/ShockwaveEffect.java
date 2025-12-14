package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ShockwaveEffect implements AbilityEffect {

    private final AnimeSMPPlugin plugin;

    public ShockwaveEffect(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, Ability ability) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 0.1, 0);

        double radius = 6.0;

        // Particles ring
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location loc = center.clone().add(x, 0.1, z);
            world.spawnParticle(Particle.CLOUD, loc, 4, 0.1, 0.1, 0.1, 0.01);
            world.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0.01);
        }

        // Knockback + damage
        for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity target)) continue;
            if (target == player) continue;

            // Knockback
            Location tLoc = target.getLocation();
            double dx = tLoc.getX() - center.getX();
            double dz = tLoc.getZ() - center.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.1) dist = 0.1;
            double power = 0.7;

            target.setVelocity(target.getVelocity().add(
                    new org.bukkit.util.Vector(dx / dist * power, 0.3, dz / dist * power)
            ));

            // Damage via calculator
            plugin.getDamageCalculator().applyAbilityDamage(player, target, ability);
        }
    }
}
