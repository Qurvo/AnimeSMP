package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HardenSkinEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        // 8s of Resistance I
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 0, false, true, true));

        player.getWorld().spawnParticle(
                Particle.BLOCK, // subtle dust-like effect
                player.getLocation().add(0, 1.0, 0),
                25,
                0.6, 1.0, 0.6,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_ANVIL_USE,
                0.9f,
                1.1f
        );

        player.sendMessage(ChatColor.GOLD + "Your skin hardens like stone.");
    }
}
