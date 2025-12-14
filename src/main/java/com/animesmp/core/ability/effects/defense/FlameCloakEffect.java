package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlameCloakEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        // Flame Cloak: defensive buff + retaliatory true-burn (handled by CombatStatusListener).
        int durationTicks = 9 * 20;
        plugin.getStatusEffectManager().setFlameCloak(player, durationTicks * 50L);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                durationTicks,
                1,
                false,
                true,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                durationTicks,
                0,
                false,
                true,
                true
        ));

        World world = player.getWorld();

        // Visual flame cloak
        world.spawnParticle(
                Particle.FLAME,
                player.getLocation().add(0, 1.0, 0),
                60,
                0.7, 1.0, 0.7,
                0.02
        );

        world.playSound(
                player.getLocation(),
                Sound.ITEM_FIRECHARGE_USE,
                1.0f,
                1.0f
        );
    }
}
