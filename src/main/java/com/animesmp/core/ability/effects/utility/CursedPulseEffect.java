package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.AnimeSMPPlugin;
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

public class CursedPulseEffect implements AbilityEffect {

    private static final double RADIUS = 8.0;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        for (Entity e : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (e instanceof LivingEntity && e != player) {
                LivingEntity le = (LivingEntity) e;
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 1, false, true, true));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4 * 20, 0, false, true, true));

                // Silence (blocks ability casts)
                if (le instanceof Player p) {
                    plugin.getStatusEffectManager().silence(p, 4000L);
                }
            }
        }

        player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 1.0, 0),
                40,
                1.0, 0.6, 1.0,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_EVOKER_CAST_SPELL,
                1.0f,
                0.8f
        );

        player.sendMessage(ChatColor.DARK_PURPLE + "A cursed pulse disrupts enemies around you.");
    }
}
