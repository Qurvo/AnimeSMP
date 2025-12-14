package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.combat.DamageCalculator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ThunderBeamEffect implements AbilityEffect {

    private static final double RANGE = 18.0;
    private static final double HITBOX = 0.9;

    @Override
    public void execute(Player player, Ability ability) {
        DamageCalculator calc = AnimeSMPPlugin.getInstance().getDamageCalculator();

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Beam particles
        int steps = (int) (RANGE * 2); // 0.5 block per step
        for (int i = 0; i < steps; i++) {
            double dist = i * 0.5;
            Location point = eye.clone().add(dir.clone().multiply(dist));
            player.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    point,
                    4,
                    0.12, 0.12, 0.12,
                    0.0
            );
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                1.0f,
                1.4f
        );

        // First enemy hit in the line
        LivingEntity target = calc.findFirstTargetInLine(player, RANGE, HITBOX);
        if (target != null) {
            calc.applyAbilityDamage(player, target, ability);
            target.getWorld().spawnParticle(
                    Particle.FLASH,
                    target.getLocation().add(0, 1.0, 0),
                    1
            );
        }

        player.sendMessage(ChatColor.YELLOW + "You fire a concentrated beam of thunderous energy!");
    }
}
