package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GaleZoneEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10 * 20, 1, false, true, true));

        player.getWorld().spawnParticle(
                Particle.CLOUD,
                player.getLocation().add(0, 0.5, 0),
                35,
                1.0, 0.3, 1.0,
                0.02
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.WEATHER_RAIN_ABOVE,
                0.8f,
                1.5f
        );

        player.sendMessage(ChatColor.AQUA + "You move like the wind within a gale zone.");
    }
}
