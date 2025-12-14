package com.animesmp.core.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global anti kill-farm manager.
 * Tracks last time each killer got rewards from each victim.
 */
public class KillCooldownManager {

    private static final KillCooldownManager INSTANCE = new KillCooldownManager();

    public static KillCooldownManager getInstance() {
        return INSTANCE;
    }

    // killer -> (victim -> lastRewardTimeMillis)
    private final Map<UUID, Map<UUID, Long>> lastKills = new HashMap<>();

    /**
     * NEW main method (used by PD and anything that wants a custom cooldown).
     *
     * @return true if this kill should give rewards (cooldown passed),
     *         false if it's within the farm cooldown window.
     */
    public boolean canRewardKill(UUID killer, UUID victim, long cooldownMillis) {
        if (killer == null || victim == null) return false;
        if (killer.equals(victim)) return false;

        long now = System.currentTimeMillis();
        Map<UUID, Long> map = lastKills.computeIfAbsent(killer, k -> new HashMap<>());
        Long last = map.get(victim);

        if (last != null && (now - last) < cooldownMillis) {
            // Still on cooldown → no rewards
            return false;
        }

        // Update timestamp and allow rewards
        map.put(victim, now);
        return true;
    }

    /**
     * BACKWARDS-COMPAT overload (used by existing CombatRewardListener, etc).
     * Uses a default 5 minute cooldown between the same killer and victim.
     */
    public boolean canRewardKill(UUID killer, UUID victim) {
        // 5 minutes = 300_000 ms
        return canRewardKill(killer, victim, 300_000L);
    }

    /**
     * Clear all stored cooldowns (used on plugin disable).
     */
    public void clearAll() {
        lastKills.clear();
    }

    /**
     * BACKWARDS-COMPAT wrapper for old code that calls killCooldownManager.clear().
     */
    public void clear() {
        clearAll();
    }

    public void clearFor(UUID killer) {
        lastKills.remove(killer);
    }
}
