package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PdStockManager {

    private final AnimeSMPPlugin plugin;

    private LocalDate stockDay;

    private final List<Ability> currentStock = new ArrayList<>();
    private final Map<String, Integer> remaining = new ConcurrentHashMap<>();
    private final Map<String, Integer> costs = new ConcurrentHashMap<>();

    public PdStockManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        ensureTodaysStock();
    }

    public synchronized List<Ability> getCurrentStock() {
        ensureTodaysStock();
        return Collections.unmodifiableList(currentStock);
    }

    public int getCostFor(Ability ability) {
        if (ability == null) return 0;
        return costs.getOrDefault(ability.getId(), defaultCostFor(ability));
    }

    public int getRemaining(Ability ability) {
        if (ability == null) return 0;
        return remaining.getOrDefault(ability.getId(), 0);
    }

    public synchronized void decrementStock(Ability ability) {
        if (ability == null) return;
        String id = ability.getId();
        int left = remaining.getOrDefault(id, 0);
        if (left <= 0) return;
        remaining.put(id, left - 1);
    }

    public synchronized void forceResetNow() {
        stockDay = null;
        currentStock.clear();
        remaining.clear();
        costs.clear();
        ensureTodaysStock();
    }

    public void forceRefreshNow() {
        forceResetNow();
    }

    /**
     * For PD GUI: time until next daily refresh, using vendor-reset zone/hour/minute.
     */
    public String getFormattedTimeRemaining() {
        Duration d = timeUntilNextReset();
        long seconds = Math.max(0, d.getSeconds());
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    // -------------------------------------------------------------------------

    private synchronized void ensureTodaysStock() {
        LocalDate today = LocalDate.now(getVendorZone());
        if (stockDay != null && stockDay.equals(today) && !currentStock.isEmpty()) return;
        stockDay = today;
        rollNewStock();
    }

    /**
     * HARD RULES:
     * - Stock size should be 8 (4 epics + 4 legendaries).
     * - LEGENDARIES = AbilityTier PD (or tier name contains PD).
     * - EPICS = damage-tier == EPIC (or forced by config list pd-shop.epic-ids).
     * - No fallback into RARE. If there are not enough epics, we log a warning and fill remaining slots with legendaries.
     */
    private void rollNewStock() {
        currentStock.clear();
        remaining.clear();
        costs.clear();

        int stockSize = plugin.getConfig().getInt("pd-shop.stock-size", 8);
        if (stockSize != 8) {
            // You asked for 4/4 split; enforce best-effort but warn.
            plugin.getLogger().warning("PdStockManager: pd-shop.stock-size is " + stockSize + " (recommended 8 for 4 epics + 4 legendaries).");
        }
        stockSize = Math.max(2, stockSize);

        int perItemStock = Math.max(1, plugin.getConfig().getInt("pd-shop.per-item-stock", 2));

        int epicMin = plugin.getConfig().getInt("pd-shop.token-costs.epic-min", 3);
        int epicMax = plugin.getConfig().getInt("pd-shop.token-costs.epic-max", 5);
        int legendaryDefault = plugin.getConfig().getInt("pd-shop.token-costs.legendary", 8);

        Map<String, Integer> perAbilityCosts = readIntMap("pd-ability-costs");

        // Forced epic ids (strongest control; use this if your abilities do not expose damage-tier cleanly)
        Set<String> forcedEpicIds = new HashSet<>();
        List<String> epicIdsList = plugin.getConfig().getStringList("pd-shop.epic-ids");
        if (epicIdsList != null) {
            for (String s : epicIdsList) {
                if (s != null && !s.isBlank()) forcedEpicIds.add(s.trim());
            }
        }

        List<Ability> legendaryPool = new ArrayList<>();
        List<Ability> epicPool = new ArrayList<>();

        for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
            if (a == null || a.getTier() == null || a.getId() == null) continue;

            String tierName = a.getTier().name().toUpperCase(Locale.ROOT);

            // Legendaries: PD tier abilities
            if (tierName.contains("PD")) {
                legendaryPool.add(a);
                continue;
            }

            // Epics: forced IDs or damage-tier EPIC
            if (forcedEpicIds.contains(a.getId())) {
                epicPool.add(a);
                continue;
            }

            String dmgTier = safeDamageTierName(a);
            if (dmgTier != null && dmgTier.toUpperCase(Locale.ROOT).contains("EPIC")) {
                epicPool.add(a);
            }
        }

        if (legendaryPool.isEmpty()) {
            plugin.getLogger().warning("PdStockManager: No PD-tier abilities found. PD vendor cannot stock legendaries.");
        }
        if (epicPool.isEmpty()) {
            plugin.getLogger().warning("PdStockManager: No EPIC damage-tier abilities found for epic pool. " +
                    "To force epics, set config list: pd-shop.epic-ids: [ability_id_1, ability_id_2, ...]");
        }

        Random rng = new Random();
        Set<String> picked = new HashSet<>();

        int desiredEpic = Math.max(1, stockSize / 2);
        int desiredLegendary = stockSize - desiredEpic;

        // Pick Epics (best-effort)
        pickIntoStock(rng, epicPool, desiredEpic, picked, perItemStock, perAbilityCosts, epicMin, epicMax, legendaryDefault);

        // Pick Legendaries
        pickIntoStock(rng, legendaryPool, desiredLegendary, picked, perItemStock, perAbilityCosts, epicMin, epicMax, legendaryDefault);

        // If still short, fill with remaining legendaries first, then epics
        while (currentStock.size() < stockSize) {
            Ability fill = randomNotPicked(rng, legendaryPool, picked);
            if (fill == null) fill = randomNotPicked(rng, epicPool, picked);
            if (fill == null) break;

            addAbility(fill, rng, picked, perItemStock, perAbilityCosts, epicMin, epicMax, legendaryDefault);
        }

        // Final sanity log
        if (!currentStock.isEmpty()) {
            long epicCount = currentStock.stream().filter(a -> !a.getTier().name().toUpperCase(Locale.ROOT).contains("PD")).count();
            long legCount = currentStock.size() - epicCount;
            plugin.getLogger().info("PdStockManager: Rolled PD stock. Epics=" + epicCount + ", Legendaries=" + legCount + ", Total=" + currentStock.size());
        } else {
            plugin.getLogger().warning("PdStockManager: Stock roll produced 0 items. Check ability tiers and config.");
        }
    }

    private void pickIntoStock(Random rng,
                               List<Ability> pool,
                               int amount,
                               Set<String> picked,
                               int perItemStock,
                               Map<String, Integer> perAbilityCosts,
                               int epicMin,
                               int epicMax,
                               int legendaryDefault) {
        if (pool == null || pool.isEmpty() || amount <= 0) return;

        int attempts = 0;
        int maxAttempts = 2000;
        while (amount > 0 && attempts++ < maxAttempts) {
            Ability a = randomNotPicked(rng, pool, picked);
            if (a == null) break;
            addAbility(a, rng, picked, perItemStock, perAbilityCosts, epicMin, epicMax, legendaryDefault);
            amount--;
        }
    }

    private Ability randomNotPicked(Random rng, List<Ability> pool, Set<String> picked) {
        if (pool == null || pool.isEmpty()) return null;
        for (int i = 0; i < 250; i++) {
            Ability a = pool.get(rng.nextInt(pool.size()));
            if (a != null && a.getId() != null && !picked.contains(a.getId())) return a;
        }
        return null;
    }

    private void addAbility(Ability chosen,
                            Random rng,
                            Set<String> picked,
                            int perItemStock,
                            Map<String, Integer> perAbilityCosts,
                            int epicMin,
                            int epicMax,
                            int legendaryDefault) {
        if (chosen == null || chosen.getId() == null) return;
        if (!picked.add(chosen.getId())) return;

        currentStock.add(chosen);
        remaining.put(chosen.getId(), perItemStock);

        int cost;
        if (perAbilityCosts.containsKey(chosen.getId())) {
            cost = perAbilityCosts.get(chosen.getId());
        } else {
            String tier = chosen.getTier().name().toUpperCase(Locale.ROOT);
            if (tier.contains("PD")) {
                cost = legendaryDefault;
            } else {
                int span = Math.max(1, (epicMax - epicMin + 1));
                cost = clamp(rng.nextInt(span) + epicMin, 1, 999);
            }
        }
        costs.put(chosen.getId(), cost);
    }

    private int defaultCostFor(Ability ability) {
        if (ability == null || ability.getTier() == null) return 0;
        String tier = ability.getTier().name().toUpperCase(Locale.ROOT);
        if (tier.contains("PD")) {
            return plugin.getConfig().getInt("pd-shop.token-costs.legendary", 8);
        }
        int epicMin = plugin.getConfig().getInt("pd-shop.token-costs.epic-min", 3);
        int epicMax = plugin.getConfig().getInt("pd-shop.token-costs.epic-max", 5);
        return clamp(epicMin, 1, epicMax);
    }

    private String safeDamageTierName(Ability ability) {
        // Tries multiple conventions safely (no compile break)
        try {
            Method m = ability.getClass().getMethod("getDamageTier");
            Object o = m.invoke(ability);
            return o == null ? null : String.valueOf(o);
        } catch (Throwable ignored) { }

        try {
            Method m = ability.getClass().getMethod("getDamageTierName");
            Object o = m.invoke(ability);
            return o == null ? null : String.valueOf(o);
        } catch (Throwable ignored) { }

        return null;
    }

    private Map<String, Integer> readIntMap(String path) {
        Map<String, Integer> out = new HashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return out;
        for (String k : sec.getKeys(false)) {
            out.put(k, sec.getInt(k));
        }
        return out;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private ZoneId getVendorZone() {
        String zone = plugin.getConfig().getString("vendor-reset.zone", "Europe/Lisbon");
        try {
            return ZoneId.of(zone);
        } catch (Exception ignored) {
            return ZoneId.of("Europe/Lisbon");
        }
    }

    private Duration timeUntilNextReset() {
        ZoneId zone = getVendorZone();
        int resetHour = plugin.getConfig().getInt("vendor-reset.hour", 8);
        int resetMin = plugin.getConfig().getInt("vendor-reset.minute", 0);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(resetHour).withMinute(resetMin).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return Duration.between(now, next);
    }
}
