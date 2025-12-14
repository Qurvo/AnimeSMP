package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BeastInstinctEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        // 10s Speed I + Strength I
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 10 * 20, 0, false, true, true));

        player.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                player.getLocation().add(0, 1.0, 0),
                25,
                0.8, 0.6, 0.8,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WOLF_GROWL,
                0.9f,
                0.8f
        );

        player.sendMessage(ChatColor.GOLD + "Your beast instincts awaken!");
    }
}
