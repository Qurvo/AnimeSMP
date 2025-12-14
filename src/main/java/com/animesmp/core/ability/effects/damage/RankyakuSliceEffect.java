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

public class RankyakuSliceEffect implements AbilityEffect {

    private static final double RANGE = 12.0;
    private static final double WIDTH = 1.2;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        DamageCalculator calc = plugin.getDamageCalculator();

        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();

        // Visual slash line
        double step = 0.5;
        for (double d = 0; d <= RANGE; d += step) {
            Location point = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
        }

        // Find first target in line
        LivingEntity target = calc.findFirstTargetInLine(player, RANGE, WIDTH);
        if (target != null) {
            double dmg = calc.calculateAbilityDamage(player, target, ability);
            target.damage(dmg, player);
            target.getWorld().playSound(target.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.9f);
        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
        }
    }
}
