package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.util.DashUtil;
import com.animesmp.core.ability.util.TrueInvisibilityUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Shadow Slip: dash + stronger "true" invis feel.
 */
public class ShadowSlipEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        if (caster == null || !caster.isOnline()) return;

        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, caster.getLocation().add(0, 1, 0), 14, 0.45, 0.65, 0.45, 0.0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.65f, 0.75f);

        // 7 blocks over 10 ticks (smooth slip)
        DashUtil.dash(caster, 7.0, 5, 0.05);

        // 3 seconds invis
        int dur = 3 * 20;
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, dur, 0, false, false, true));
        if (TrueInvisibilityUtil.isProtocolLibPresent()) {
            TrueInvisibilityUtil.applyTrueInvis(caster, dur);
        }
    }
}
