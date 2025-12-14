package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterParryEffect implements AbilityEffect {

    private static final double RADIUS = 3.0;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        // Parry stance: you are "frozen" but gain a short parry window.
        plugin.getStatusEffectManager().setParryWindow(player, 2200L); // ~2.2s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 3, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 45, 1, false, false, true));

        player.getWorld().spawnParticle(
                Particle.SPLASH,
                player.getLocation().add(0, 1.0, 0),
                25,
                0.8, 0.8, 0.8,
                0.2
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_SWIM,
                0.8f,
                1.4f
        );

        player.sendMessage(ChatColor.AQUA + "Water Parry: hold your stance and punish the next hit.");

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.sendMessage(ChatColor.GRAY + "Your parry window fades."), 50L);
    }
}
