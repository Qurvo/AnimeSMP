package com.animesmp.core.combat;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UltimateBarManager {

    private final AnimeSMPPlugin plugin;

    private final Map<UUID, Double> chargePct = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCombatMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lockoutUntilMs = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChargeSecond = new ConcurrentHashMap<>();
    private final Map<UUID, Double> chargedThisSecond = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> readySoundPlayed = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasLockedOut = new ConcurrentHashMap<>();

    public UltimateBarManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("balance.ultimate.enabled", true);
    }

    public void start() {
        // Smooth + constant updates
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void handleJoin(Player player) {
        ensureBar(player);
        updateBar(player);
    }

    public void handleQuit(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    public boolean isReady(Player player) {
        UUID id = player.getUniqueId();
        return System.currentTimeMillis() >= lockoutUntilMs.getOrDefault(id, 0L)
                && getChargePct(player) >= 100.0;
    }

    public double getChargePct(Player player) {
        return chargePct.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void consumeUltimate(Player player) {
        UUID id = player.getUniqueId();

        chargePct.put(id, 0.0);
        readySoundPlayed.put(id, false);

        long lockoutSeconds = plugin.getConfig().getLong(
                "balance.ultimate.post-cast-lockout-seconds", 90L
        );
        lockoutUntilMs.put(id, System.currentTimeMillis() + lockoutSeconds * 1000L);

        resetClamp(id);
        updateBar(player);
    }

    public void onAbilityDamage(Player attacker, Player victim, double damageHp) {
        addCharge(attacker, damageHp / 2.0,
                "balance.ultimate.charge.per-heart-ability-damage-dealt-percent");
        addCharge(victim, damageHp / 2.0,
                "balance.ultimate.charge.per-heart-damage-taken-percent");
        stampCombat(attacker);
        stampCombat(victim);
    }

    public void onMeleeDamage(Player attacker, Player victim, double damageHp) {
        addCharge(attacker, damageHp / 2.0,
                "balance.ultimate.charge.per-heart-melee-damage-dealt-percent");
        addCharge(victim, damageHp / 2.0,
                "balance.ultimate.charge.per-heart-damage-taken-percent");
        stampCombat(attacker);
        stampCombat(victim);
    }

    public void onPlayerKill(Player killer) {
        double bonus = plugin.getConfig().getDouble(
                "balance.ultimate.charge.on-player-kill-bonus-percent", 25.0
        );
        addChargePct(killer, bonus);
        stampCombat(killer);
    }

    private void addCharge(Player player, double hearts, String key) {
        if (player == null || hearts <= 0) return;

        UUID id = player.getUniqueId();
        if (System.currentTimeMillis() < lockoutUntilMs.getOrDefault(id, 0L)) return;

        double perHeart = plugin.getConfig().getDouble(key, 1.0);
        double add = hearts * perHeart;

        long nowSec = System.currentTimeMillis() / 1000L;
        if (lastChargeSecond.getOrDefault(id, -1L) != nowSec) {
            lastChargeSecond.put(id, nowSec);
            chargedThisSecond.put(id, 0.0);
        }

        double maxPerSec = plugin.getConfig().getDouble(
                "balance.ultimate.charge.max-percent-per-second", 45.0
        );

        double used = chargedThisSecond.getOrDefault(id, 0.0);
        double allowed = Math.max(0.0, maxPerSec - used);
        double finalAdd = Math.min(add, allowed);

        if (finalAdd <= 0) return;

        chargedThisSecond.put(id, used + finalAdd);
        addChargePct(player, finalAdd);
    }

    private void addChargePct(Player player, double add) {
        UUID id = player.getUniqueId();
        double next = Math.min(100.0, chargePct.getOrDefault(id, 0.0) + add);
        chargePct.put(id, next);

        if (next >= 100.0) resetClamp(id);
        updateBar(player);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long nowSec = now / 1000L;

        boolean decayEnabled = plugin.getConfig().getBoolean(
                "balance.ultimate.decay.enabled", false
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();

            boolean lockedNow = now < lockoutUntilMs.getOrDefault(id, 0L);
            boolean lockedBefore = wasLockedOut.getOrDefault(id, false);

            if (lockedBefore && !lockedNow) resetClamp(id);
            wasLockedOut.put(id, lockedNow);

            if (lockedNow) {
                chargePct.put(id, 0.0);
                updateBar(p);
                continue;
            }

            if (decayEnabled) {
                long lastCombat = lastCombatMs.getOrDefault(id, 0L);
                if (now - lastCombat > 8000) {
                    double cur = chargePct.getOrDefault(id, 0.0);
                    chargePct.put(id, Math.max(0.0, cur - 1.25));
                }
            }

            updateBar(p);
        }
    }

    private void resetClamp(UUID id) {
        long sec = System.currentTimeMillis() / 1000L;
        lastChargeSecond.put(id, sec);
        chargedThisSecond.put(id, 0.0);
        readySoundPlayed.put(id, false);
    }

    private void stampCombat(Player p) {
        lastCombatMs.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private BossBar ensureBar(Player player) {
        return bars.computeIfAbsent(player.getUniqueId(), id -> {
            BarColor color = BarColor.BLUE;
            BarStyle style = BarStyle.SOLID;
            BossBar bar = Bukkit.createBossBar("Ultimate", color, style);
            bar.addPlayer(player);
            return bar;
        });
    }

    private void updateBar(Player player) {
        BossBar bar = ensureBar(player);
        double pct = chargePct.getOrDefault(player.getUniqueId(), 0.0);
        bar.setProgress(Math.min(1.0, pct / 100.0));
        bar.setVisible(true);

        if (pct >= 100.0 && !readySoundPlayed.getOrDefault(player.getUniqueId(), false)) {
            readySoundPlayed.put(player.getUniqueId(), true);
            player.playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
    }
}
