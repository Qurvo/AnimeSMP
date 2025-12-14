package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ShadowGuardEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        // 6s of Resistance I + 6s of Night Vision (thematic + small defensive buff)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6 * 20, 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 6 * 20, 0, false, false, false));

        player.getWorld().spawnParticle(
                Particle.SMOKE,
                player.getLocation().add(0, 1.2, 0),
                30,
                0.7, 1.1, 0.7,
                0.01
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                0.6f,
                0.5f
        );

        player.sendMessage(ChatColor.DARK_PURPLE + "Shadows coil around you, shielding your body.");
    }
}
