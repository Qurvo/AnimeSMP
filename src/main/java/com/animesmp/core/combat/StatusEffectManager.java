package com.animesmp.core.combat;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight runtime-only status system for PvP tuning.
 * These are intentionally NOT persisted across restarts.
 */
public class StatusEffectManager {

    private final AnimeSMPPlugin plugin;

    private final Map<UUID, Long> silencedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fallImmunityUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> parryUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> flameCloakUntil = new ConcurrentHashMap<>();

    // Active Ki Barrier domes: owner -> blocks
    private final Map<UUID, Set<Location>> kiBarrierBlocks = new ConcurrentHashMap<>();

    public StatusEffectManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    // ---------------- Silenced ----------------

    public void silence(Player p, long durationMs) {
        silencedUntil.put(p.getUniqueId(), now() + durationMs);
    }

    public boolean isSilenced(Player p) {
        Long until = silencedUntil.get(p.getUniqueId());
        return until != null && until > now();
    }

    public long silenceRemainingMs(Player p) {
        Long until = silencedUntil.get(p.getUniqueId());
        if (until == null) return 0L;
        return Math.max(0L, until - now());
    }

    // ---------------- Fall immunity ----------------

    public void grantFallImmunity(Player p, long durationMs) {
        fallImmunityUntil.put(p.getUniqueId(), now() + durationMs);
    }

    public boolean hasFallImmunity(Player p) {
        Long until = fallImmunityUntil.get(p.getUniqueId());
        return until != null && until > now();
    }

    // ---------------- Parry window ----------------

    public void setParryWindow(Player p, long durationMs) {
        parryUntil.put(p.getUniqueId(), now() + durationMs);
    }

    public boolean isParrying(Player p) {
        Long until = parryUntil.get(p.getUniqueId());
        return until != null && until > now();
    }

    // ---------------- Flame cloak ----------------

    public void setFlameCloak(Player p, long durationMs) {
        flameCloakUntil.put(p.getUniqueId(), now() + durationMs);
    }

    public boolean hasFlameCloak(Player p) {
        Long until = flameCloakUntil.get(p.getUniqueId());
        return until != null && until > now();
    }

    // ---------------- Ki Barrier dome blocks ----------------

    public void registerKiBarrier(Player owner, Set<Location> blocks) {
        kiBarrierBlocks.put(owner.getUniqueId(), blocks);
    }

    public boolean isKiBarrierBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (Set<Location> set : kiBarrierBlocks.values()) {
            for (Location l : set) {
                if (l.getWorld() == loc.getWorld() && l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clearKiBarrier(Player owner) {
        kiBarrierBlocks.remove(owner.getUniqueId());
    }
}
