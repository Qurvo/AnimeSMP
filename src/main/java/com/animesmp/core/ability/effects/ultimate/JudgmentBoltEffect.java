package com.animesmp.core.ability.effects.ultimate;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class JudgmentBoltEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        double maxDistance = 25.0;
        double step = 0.5;

        Location strike = null;

        for (double d = step; d <= maxDistance; d += step) {
            Location test = eye.clone().add(dir.clone().multiply(d));
            Block b = test.getBlock();
            if (!b.getType().isAir()) {
                strike = b.getLocation().add(0.5, 1.0, 0.5);
                break;
            }
        }

        if (strike == null) {
            strike = eye.clone().add(dir.multiply(maxDistance));
        }

        Location finalStrike = strike.clone();

        // Delay a bit for dramatic call
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.7f,
                0.7f
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Auto-lock: strike every player in the zone (including the one you aimed near).
            double lockRadius = 18.0;
            var world = finalStrike.getWorld();
            if (world == null) return;

            for (Entity ent : world.getNearbyEntities(finalStrike, lockRadius, lockRadius, lockRadius)) {
                if (!(ent instanceof Player p)) continue;
                if (p == caster) continue;
                Location loc = p.getLocation().add(0, 0.2, 0);
                world.strikeLightningEffect(loc);
                plugin.getDamageCalculator().applyAbilityDamage(caster, p, ability);
            }

            // Also add one dramatic strike at the aimed point.
            world.strikeLightningEffect(finalStrike);

            finalStrike.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    finalStrike,
                    40,
                    0.8, 1.2, 0.8,
                    0.0
            );

            // No extra AoE damage: damage comes from direct locks to keep it readable.
        }, 15L);
    }
}
