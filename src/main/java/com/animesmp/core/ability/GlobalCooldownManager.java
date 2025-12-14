package com.animesmp.core.ability;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlobalCooldownManager {

    // Stores: playerUUID → timestamp when GCD ends
    private final Map<UUID, Long> gcdMap = new HashMap<>();

    // Global cooldown length (ms)
    private static final long GLOBAL_COOLDOWN = 600; // 0.6 seconds

    public boolean isOnGlobalCooldown(Player player) {
        long now = System.currentTimeMillis();
        return gcdMap.getOrDefault(player.getUniqueId(), 0L) > now;
    }

    public long getRemaining(Player player) {
        long now = System.currentTimeMillis();
        return Math.max(0, gcdMap.getOrDefault(player.getUniqueId(), 0L) - now);
    }

    public void applyGlobalCooldown(Player player) {
        long now = System.currentTimeMillis();
        gcdMap.put(player.getUniqueId(), now + GLOBAL_COOLDOWN);
    }

    public void applyAdditional(Player player, long ms) {
        gcdMap.put(player.getUniqueId(),
                Math.max(gcdMap.getOrDefault(player.getUniqueId(), 0L), System.currentTimeMillis()) + ms
        );
    }
}
