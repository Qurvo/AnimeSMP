package com.animesmp.core.ability.effects.damage;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.combat.DamageCalculator;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WindTyphoonSlashEffect implements AbilityEffect {

    private static final double RADIUS = 4.5;
    private static final double KNOCKBACK = 0.8;

    @Override
    public void execute(Player player, Ability ability) {
        DamageCalculator calc = AnimeSMPPlugin.getInstance().getDamageCalculator();

        // Visual: small wind "typhoon" around the player
        player.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                player.getLocation().add(0, 1.0, 0),
                12,
                1.3, 0.4, 1.3,
                0.0
        );

        player.getWorld().spawnParticle(
                Particle.CLOUD,
                player.getLocation().add(0, 0.2, 0),
                30,
                1.5, 0.2, 1.5,
                0.02
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                1.2f
        );

        // Damage + directional knockback
        for (LivingEntity target : calc.getNearbyEnemies(player, RADIUS)) {
            calc.applyAbilityDamage(player, target, ability);

            Vector kbDir = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .setY(0)
                    .normalize()
                    .multiply(KNOCKBACK);
            kbDir.setY(0.25);

            target.setVelocity(target.getVelocity().add(kbDir));
        }

        player.sendMessage(ChatColor.AQUA + "You unleash a raging wind typhoon slash.");
    }
}
