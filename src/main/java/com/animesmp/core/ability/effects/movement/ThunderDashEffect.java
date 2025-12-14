package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.util.DashUtil;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Thunderclap Dash: positioning tool. No damage. Adds a brief Speed boost after the dash.
 */
public class ThunderDashEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        if (caster == null || !caster.isOnline()) return;

        // Electric burst + dash (feels like a dash, not a teleport)
        caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, caster.getLocation().add(0, 1.0, 0), 18, 0.35, 0.25, 0.35, 0.02);

        // 8 blocks over 10 ticks
        DashUtil.dash(caster, 11.0, 6, 0.05);

        // Speed 3 for 5s (amplifier 2)
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5 * 20, 2, false, false, true));
    }
}
