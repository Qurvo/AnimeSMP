package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityTier;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RotatingVendorManager {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;

    private final File rotationFile;
    private FileConfiguration rotationConfig;

    private List<String> currentRotation = new ArrayList<>();
    private long nextRotationTimestamp = 0L;

    private final VendorPurchaseManager purchaseManager;

    public RotatingVendorManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.rotationFile = new File(plugin.getDataFolder(), "rotation.yml");
        this.purchaseManager = new VendorPurchaseManager(plugin);

        loadRotation();

        // Check once per minute if we need to rotate
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60L, 20L * 60L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (nextRotationTimestamp <= 0L || now >= nextRotationTimestamp) {
            regenerateRotation();
        }
    }

    // ------------------------------------------------------------------------
    // LOAD / SAVE
    // ------------------------------------------------------------------------

    private void loadRotation() {
        if (!rotationFile.exists()) {
            rotationConfig = new YamlConfiguration();
            currentRotation.clear();
            nextRotationTimestamp = 0L;
            return;
        }

        rotationConfig = YamlConfiguration.loadConfiguration(rotationFile);

        currentRotation = new ArrayList<>(rotationConfig.getStringList("currentRotation"));
        nextRotationTimestamp = rotationConfig.getLong("nextRotationTimestamp", 0L);

        purchaseManager.loadFromConfig(rotationConfig);
    }

    private void saveRotation() {
        if (rotationConfig == null) {
            rotationConfig = new YamlConfiguration();
        }

        rotationConfig.set("currentRotation", currentRotation);
        rotationConfig.set("nextRotationTimestamp", nextRotationTimestamp);

        purchaseManager.saveToConfig(rotationConfig);

        try {
            rotationConfig.save(rotationFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[RotatingVendor] Failed to save rotation.yml");
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // ROTATION LOGIC
    // ------------------------------------------------------------------------

    private void regenerateRotation() {
        Collection<Ability> all = registry.getAllAbilities();
        List<Ability> pool = all.stream()
                .filter(Objects::nonNull)
                // exclude PD abilities from the vendor
                .filter(a -> a.getTier() != AbilityTier.PD)
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            plugin.getLogger().warning("[RotatingVendor] No eligible abilities found for rotation pool.");
            currentRotation = Collections.emptyList();
            nextRotationTimestamp = computeNextRotationTimestamp();
            purchaseManager.resetAllPurchases();
            saveRotation();
            return;
        }

        List<Ability> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        int count = Math.min(8, shuffled.size());
        currentRotation = shuffled.subList(0, count).stream()
                .map(Ability::getId)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        purchaseManager.resetAllPurchases();
        nextRotationTimestamp = computeNextRotationTimestamp();

        saveRotation();
    }

    private long computeNextRotationTimestamp() {
        // 08:00 EU time (Lisbon)
        ZoneId zone = ZoneId.of("Europe/Lisbon");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toInstant().toEpochMilli();
    }

    // ------------------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------------------

    /**
     * Returns the current rotation. If it's empty or expired, we regenerate it
     * on-demand. This means it will only generate after abilities are loaded.
     */
    public List<String> getCurrentRotation() {
        long now = System.currentTimeMillis();
        if (nextRotationTimestamp <= 0L
                || now >= nextRotationTimestamp
                || currentRotation == null
                || currentRotation.isEmpty()) {
            regenerateRotation();
        }
        return Collections.unmodifiableList(currentRotation);
    }

    public String getFormattedTimeRemaining() {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0L, nextRotationTimestamp - now);

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

    public boolean hasPlayerPurchased(java.util.UUID uuid, String abilityId) {
        if (uuid == null || abilityId == null) return false;
        return purchaseManager.hasPurchased(uuid, abilityId.toLowerCase(Locale.ROOT));
    }

    public void recordPurchase(java.util.UUID uuid, String abilityId) {
        if (uuid == null || abilityId == null) return;
        purchaseManager.recordPurchase(uuid, abilityId.toLowerCase(Locale.ROOT));
        saveRotation();
    }

    /**
     * Admin utility: force a new rotation immediately.
     */
    public void forceRefreshNow() {
        regenerateRotation();
    }
}
