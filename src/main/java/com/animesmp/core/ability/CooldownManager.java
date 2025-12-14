package com.animesmp.core.ability;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player player, Ability ability) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return false;
        Long until = map.get(ability.getId());
        return until != null && until > System.currentTimeMillis();
    }

    public long getRemaining(Player player, Ability ability) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return 0L;
        Long until = map.get(ability.getId());
        if (until == null) return 0L;
        long diff = until - System.currentTimeMillis();
        return Math.max(diff, 0L);
    }

    public void setCooldown(Player player, Ability ability, long millis) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(ability.getId(), System.currentTimeMillis() + millis);
    }
}
