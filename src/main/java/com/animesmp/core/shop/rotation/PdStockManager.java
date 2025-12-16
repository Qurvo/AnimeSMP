package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import org.bukkit.configuration.ConfigurationSection;

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

    private void rollNewStock() {
        currentStock.clear();
        remaining.clear();
        costs.clear();

        int stockSize = Math.max(6, plugin.getConfig().getInt("pd-shop.stock-size", 9));

        int wEpic = Math.max(1, plugin.getConfig().getInt("pd-shop.weights.epic", 80));
        int wLegendary = Math.max(0, plugin.getConfig().getInt("pd-shop.weights.legendary", 20));

        int epicMin = plugin.getConfig().getInt("pd-shop.token-costs.epic-min", 3);
        int epicMax = plugin.getConfig().getInt("pd-shop.token-costs.epic-max", 5);
        int legendaryDefault = plugin.getConfig().getInt("pd-shop.token-costs.legendary", 12);

        int perItemStock = Math.max(1, plugin.getConfig().getInt("pd-shop.per-item-stock", 1));

        Map<String, Integer> perAbilityCosts = readIntMap("pd-ability-costs");

        List<Ability> epicPool = new ArrayList<>();
        List<Ability> legendaryPool = new ArrayList<>();

        for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
            if (a == null || a.getTier() == null) continue;
            String tier = a.getTier().name().toUpperCase(Locale.ROOT);

            // Epics: TRAINER tier bucket
            if (tier.contains("TRAINER")) {
                epicPool.add(a);
            }
            // Legendaries: PD tier bucket
            else if (tier.contains("PD")) {
                legendaryPool.add(a);
            }
        }

        if (epicPool.isEmpty() && legendaryPool.isEmpty()) {
            plugin.getLogger().warning("PdStockManager: No eligible abilities found for PD vendor stock. Pools are empty.");
            return;
        }

        Random rng = new Random();
        Set<String> picked = new HashSet<>();

        int maxAttempts = 500;
        int attempts = 0;

        while (currentStock.size() < stockSize && attempts < maxAttempts) {
            attempts++;

            Ability chosen = weightedPick(rng, epicPool, legendaryPool, wEpic, wLegendary);
            if (chosen == null) continue;
            if (!picked.add(chosen.getId())) continue;

            currentStock.add(chosen);

            remaining.put(chosen.getId(), perItemStock);

            int cost;
            if (perAbilityCosts.containsKey(chosen.getId())) {
                cost = perAbilityCosts.get(chosen.getId());
            } else {
                String tier = chosen.getTier().name().toUpperCase(Locale.ROOT);
                if (tier.contains("TRAINER")) {
                    int span = Math.max(1, (epicMax - epicMin + 1));
                    cost = clamp(rng.nextInt(span) + epicMin, 1, 999);
                } else if (tier.contains("PD")) {
                    cost = legendaryDefault;
                } else {
                    cost = legendaryDefault;
                }
            }
            costs.put(chosen.getId(), cost);
        }

        if (currentStock.isEmpty()) {
            plugin.getLogger().warning("PdStockManager: Stock roll produced 0 items (attempts=" + attempts + "). Check pools and tiers.");
        } else if (attempts >= maxAttempts) {
            plugin.getLogger().warning("PdStockManager: Stock roll hit max attempts (" + maxAttempts + "). Pool may be too small for stock-size.");
        }
    }

    private Ability weightedPick(Random rng, List<Ability> epicPool, List<Ability> legendaryPool, int wEpic, int wLegendary) {
        if (legendaryPool.isEmpty()) return epicPool.isEmpty() ? null : epicPool.get(rng.nextInt(epicPool.size()));
        if (epicPool.isEmpty()) return legendaryPool.get(rng.nextInt(legendaryPool.size()));

        int total = Math.max(1, wEpic) + Math.max(0, wLegendary);
        int roll = rng.nextInt(total);

        if (roll < wEpic) return epicPool.get(rng.nextInt(epicPool.size()));
        return legendaryPool.get(rng.nextInt(legendaryPool.size()));
    }

    private int defaultCostFor(Ability ability) {
        if (ability == null || ability.getTier() == null) return 0;
        String tier = ability.getTier().name().toUpperCase(Locale.ROOT);
        if (tier.contains("TRAINER")) {
            int epicMin = plugin.getConfig().getInt("pd-shop.token-costs.epic-min", 3);
            int epicMax = plugin.getConfig().getInt("pd-shop.token-costs.epic-max", 5);
            return clamp(epicMin, 1, epicMax);
        }
        if (tier.contains("PD")) {
            return plugin.getConfig().getInt("pd-shop.token-costs.legendary", 12);
        }
        return plugin.getConfig().getInt("pd-shop.token-costs.legendary", 12);
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
        try { return ZoneId.of(zone); } catch (Exception ignored) { return ZoneId.of("Europe/Lisbon"); }
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
