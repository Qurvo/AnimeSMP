package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.stamina.StaminaManager;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ChakraRenewalEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        StaminaManager stamina = AnimeSMPPlugin.getInstance().getStaminaManager();

        // Flat stamina gain (tunable)
        stamina.addStamina(player, 40); // ~nice chunk of stamina back

        // 6s of Regeneration I
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 6 * 20, 0, false, true, true));

        player.getWorld().spawnParticle(
                Particle.HEART,
                player.getLocation().add(0, 1.2, 0),
                10,
                0.5, 0.8, 0.5,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP,
                0.9f,
                1.4f
        );

        player.sendMessage(ChatColor.AQUA + "Your chakra surges back through your body.");
    }
}
