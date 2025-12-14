package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class IronBodyEffect implements AbilityEffect {

    // ~8 seconds of tankiness
    private static final int DURATION_TICKS = 8 * 20;

    @Override
    public void execute(Player player, Ability ability) {

        // Resistance to make abilities hurt less (we can later swap to custom reduction)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                DURATION_TICKS,
                1,
                false,
                false,
                true
        ));

        // Small slowness as tradeoff
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        // Tiny armor feel
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        // Visual + sound
        player.getWorld().spawnParticle(
                Particle.BLOCK,
                player.getLocation().add(0, 1.0, 0),
                30,
                0.4, 0.8, 0.4,
                0.1,
                org.bukkit.Material.IRON_BLOCK.createBlockData()
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_ANVIL_USE,
                1.0f,
                0.8f
        );
    }
}
