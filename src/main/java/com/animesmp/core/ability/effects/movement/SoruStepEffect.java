package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.util.DashUtil;
import com.animesmp.core.ability.util.TrueInvisibilityUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Soru Step: dash + brief true invis (hide armor/hand items via ProtocolLib).
 */
public class SoruStepEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        if (caster == null || !caster.isOnline()) return;

        // 9 blocks over 9 ticks
        DashUtil.dash(caster, 11.0, 8, 0.05);

        // Brief invis window (2s)
        int dur = 2 * 20;
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, dur, 0, false, false, true));
        if (TrueInvisibilityUtil.isProtocolLibPresent()) {
            TrueInvisibilityUtil.applyTrueInvis(caster, dur);
        }
    }
}
