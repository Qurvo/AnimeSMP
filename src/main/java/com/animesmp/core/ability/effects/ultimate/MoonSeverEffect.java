package com.animesmp.core.ability.effects.ultimate;

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

public class MoonSeverEffect implements AbilityEffect {

    private static final double RANGE = 10.0;
    private static final double RADIUS = 6.0;
    private static final double CONE_DEGREES = 80.0;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        DamageCalculator calc = plugin.getDamageCalculator();

        World world = player.getWorld();
        Location origin = player.getLocation().add(0, 1.0, 0);
        Vector facing = origin.getDirection().normalize();

        // Big crescent of particles
        double step = 0.6;
        for (double d = 1.0; d <= RANGE; d += step) {
            for (double angleOffset = -CONE_DEGREES / 2.0; angleOffset <= CONE_DEGREES / 2.0; angleOffset += 10.0) {
                Vector dir = facing.clone();
                dir.rotateAroundY(Math.toRadians(angleOffset));
                Location point = origin.clone().add(dir.multiply(d));
                world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.0);
            }
        }

        // Hit enemies in a big frontal cone
        Collection<LivingEntity> enemies = calc.getNearbyEnemies(player, RADIUS);
        for (LivingEntity target : enemies) {
            Vector toTarget = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector());
            double angle = facing.angle(toTarget.normalize()) * (180.0 / Math.PI);

            if (angle <= CONE_DEGREES / 2.0) {
                double dmg = calc.calculateAbilityDamage(player, target, ability) * 1.2; // extra ult scaling
                target.damage(dmg, player);

                // Strong knockback
                Vector kb = toTarget.normalize().setY(0.3).multiply(0.9);
                target.setVelocity(target.getVelocity().add(kb));
            }
        }

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.4f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 1.2f);
    }
}
