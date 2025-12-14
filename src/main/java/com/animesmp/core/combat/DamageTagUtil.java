package com.animesmp.core.combat;

import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight tag to mark vanilla damage events that were caused by an AnimeSMP ability.
 * Prevents double-counting (e.g. for ultimate charge) when ability damage calls target.damage().
 */
public final class DamageTagUtil {

    private static final Map<UUID, Long> taggedUntil = new ConcurrentHashMap<>();

    private DamageTagUtil() {}

    public static void markAbilityDamage(LivingEntity target, long ttlMs) {
        if (target == null) return;
        taggedUntil.put(target.getUniqueId(), System.currentTimeMillis() + Math.max(1L, ttlMs));
    }

    public static boolean isAbilityDamage(LivingEntity target) {
        if (target == null) return false;
        Long until = taggedUntil.get(target.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            taggedUntil.remove(target.getUniqueId());
            return false;
        }
        return true;
    }
}
