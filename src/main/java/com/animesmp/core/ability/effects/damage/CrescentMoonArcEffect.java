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

public class CrescentMoonArcEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        World world = player.getWorld();
        DamageCalculator calc = AnimeSMPPlugin.getInstance().getDamageCalculator();

        // Base direction the player is looking
        Vector look = player.getLocation().getDirection().normalize();

        // Short "fan" / arc in front of the player
        int rays = 7;                       // how many rays in the arc
        double maxAngle = Math.toRadians(50); // total spread
        double maxDistance = 7.0;           // how far the slash reaches
        double step = 0.8;                  // step distance along each ray

        Location origin = player.getEyeLocation().clone();

        for (int i = 0; i < rays; i++) {
            // angle from -maxAngle/2 to +maxAngle/2
            double t = (i / (double) (rays - 1)) - 0.5; // -0.5..0.5
            double angle = t * maxAngle;

            Vector dir = rotateAroundY(look.clone(), angle);

            for (double d = 1.5; d <= maxDistance; d += step) {
                Location point = origin.clone().add(dir.clone().multiply(d));

                // Visual: sweeping crescent particles
                world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CRIT, point, 2, 0.1, 0.1, 0.1, 0.0);

                // Damage entities in a small radius around the slash
                double radius = 0.8;
                for (LivingEntity target : point.getNearbyLivingEntities(radius)) {
                    if (target == player) continue;
                    if (target.isDead()) continue;

                    // Use central damage scaling
                    calc.applyAbilityDamage(player, target, ability);
                }
            }
        }
    }

    /**
     * Rotate a vector around the Y axis by angle (radians).
     */
    private Vector rotateAroundY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX();
        double z = v.getZ();
        v.setX(x * cos - z * sin);
        v.setZ(x * sin + z * cos);
        return v;
    }
}
