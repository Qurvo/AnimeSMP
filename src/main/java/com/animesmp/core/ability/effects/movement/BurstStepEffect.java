package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.util.DashUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class BurstStepEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location start = caster.getLocation().clone();

        // Burst at origin, then a short dash
        start.getWorld().spawnParticle(Particle.EXPLOSION, start.add(0, 1, 0), 8, 0.35, 0.35, 0.35, 0.0);

        // 6.5 blocks over 8 ticks
        DashUtil.dash(caster, 8, 6, 0.05);
    }
}
