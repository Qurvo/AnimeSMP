package com.animesmp.core.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Lightweight hit confirmation.
 */
public final class HitFeedbackUtil {

    private HitFeedbackUtil() {}

    public static void onAbilityHit(Player attacker, LivingEntity target) {
        if (attacker == null || target == null) return;

        attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.35f, 2.0f);

        Location impact = target.getLocation().add(0, Math.max(0.8, target.getHeight() * 0.6), 0);
        target.getWorld().spawnParticle(Particle.CRIT, impact, 10, 0.18, 0.18, 0.18, 0.05);
    }
}
