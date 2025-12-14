package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.combat.DamageCalculator;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.Collection;

public class RumbleClawEffect implements AbilityEffect {

    private static final double RADIUS = 4.0;
    private static final double CONE_DEGREES = 70.0;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        DamageCalculator calc = plugin.getDamageCalculator();

        World world = player.getWorld();
        Location origin = player.getLocation().add(0, 1.0, 0);
        Vector facing = origin.getDirection().normalize();

        // Visual – small cone of particles
        for (int i = 0; i < 40; i++) {
            double dist = 1.0 + (Math.random() * 2.0);
            double yawOffset = (Math.random() * CONE_DEGREES) - (CONE_DEGREES / 2.0);
            Vector dir = facing.clone();
            dir.rotateAroundY(Math.toRadians(yawOffset));
            Location point = origin.clone().add(dir.multiply(dist));
            world.spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0.01);
        }

        Collection<LivingEntity> enemies = calc.getNearbyEnemies(player, RADIUS);
        for (LivingEntity target : enemies) {
            Vector toTarget = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector());
            double angle = facing.angle(toTarget.normalize()) * (180.0 / Math.PI);
            if (angle <= CONE_DEGREES / 2.0) {
                double dmg = calc.calculateAbilityDamage(player, target, ability);
                target.damage(dmg, player);

                // Small knockback
                Vector kb = toTarget.normalize().setY(0.2).multiply(0.4);
                target.setVelocity(target.getVelocity().add(kb));
            }
        }

        world.playSound(player.getLocation(),
                Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 1.0f);
    }
}
