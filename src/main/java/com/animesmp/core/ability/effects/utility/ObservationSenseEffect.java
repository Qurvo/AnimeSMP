package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ObservationSenseEffect implements AbilityEffect {

    private static final double RADIUS = 16.0;

    @Override
    public void execute(Player player, Ability ability) {
        int count = 0;

        for (Entity e : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (e instanceof LivingEntity && e != player) {
                LivingEntity le = (LivingEntity) e;
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 6 * 20, 0, false, true, true));
                count++;
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 8 * 20, 0, false, false, false));

        player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 1.5, 0),
                30,
                0.6, 0.6, 0.6,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                1.0f,
                1.2f
        );

        player.sendMessage(ChatColor.GOLD + "Observation Sense: " +
                ChatColor.YELLOW + count + ChatColor.GRAY + " entities detected nearby.");
    }
}
