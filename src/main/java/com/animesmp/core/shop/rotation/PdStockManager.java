package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class PdStockManager {

    private final AnimeSMPPlugin plugin;
    private final File file;
    private FileConfiguration config;

    // Map<abilityId, remainingStock>
    private final Map<String, Integer> stock = new HashMap<>();

    private long nextReset = 0L;

    public PdStockManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pd_stock.yml");

        load();

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60L, 20L * 60L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (now >= nextReset) {
            resetStock();
        }
    }

    // Load from disk
    private void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            resetStock();
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);

        nextReset = config.getLong("nextReset", 0L);

        stock.clear();

        if (config.isConfigurationSection("stock")) {
            for (String key : config.getConfigurationSection("stock").getKeys(false)) {
                stock.put(key.toLowerCase(), config.getInt("stock." + key, 3));
            }
        }
    }

    // Save to disk
    private void save() {
        config.set("nextReset", nextReset);

        config.set("stock", null);
        for (Map.Entry<String, Integer> e : stock.entrySet()) {
            config.set("stock." + e.getKey(), e.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[PD Stock] Failed to save pd_stock.yml");
        }
    }

    private long computeNextReset() {
        ZoneId zone = ZoneId.of("Europe/Lisbon");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toInstant().toEpochMilli();
    }

    public void resetStock() {
        stock.clear();

        // We don't know all PD abilities here, so abilities will be added lazily on access
        nextReset = computeNextReset();

        save();
    }

    public int getStock(String abilityId) {
        abilityId = abilityId.toLowerCase();
        return stock.getOrDefault(abilityId, 3);
    }

    public void decreaseStock(String abilityId) {
        abilityId = abilityId.toLowerCase();
        int current = stock.getOrDefault(abilityId, 3);
        current = Math.max(0, current - 1);
        stock.put(abilityId, current);
        save();
    }

    /**
     * Admin utility: force a stock reset immediately.
     */
    public void forceResetNow() {
        resetStock();
    }

    public String getFormattedTimeRemaining() {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0L, nextReset - now);

        long totalSeconds = remaining / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
