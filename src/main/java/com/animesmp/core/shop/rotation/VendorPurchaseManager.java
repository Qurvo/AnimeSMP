package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class VendorPurchaseManager {

    private final AnimeSMPPlugin plugin;

    // Map<UUID, Set<abilityId>>
    private final Map<UUID, Set<String>> purchasedMap = new HashMap<>();

    public VendorPurchaseManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------------
    // LOAD / SAVE
    // ------------------------------------------------------------------------

    public void loadFromConfig(FileConfiguration config) {
        purchasedMap.clear();
        if (config == null) return;

        ConfigurationSection root = config.getConfigurationSection("purchases");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<String> values = root.getStringList(key);
                Set<String> set = new HashSet<>();
                for (String id : values) {
                    if (id != null && !id.isEmpty()) {
                        set.add(id.toLowerCase(Locale.ROOT));
                    }
                }
                if (!set.isEmpty()) {
                    purchasedMap.put(uuid, set);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[RotatingVendor] Invalid UUID in purchases: " + key);
            }
        }
    }

    public void saveToConfig(FileConfiguration config) {
        if (config == null) return;

        config.set("purchases", null); // clear old data

        if (purchasedMap.isEmpty()) return;

        for (Map.Entry<UUID, Set<String>> entry : purchasedMap.entrySet()) {
            UUID uuid = entry.getKey();
            Set<String> set = entry.getValue();
            if (set == null || set.isEmpty()) continue;

            config.set("purchases." + uuid.toString(), new ArrayList<>(set));
        }
    }

    // ------------------------------------------------------------------------
    // RUNTIME API
    // ------------------------------------------------------------------------

    public boolean hasPurchased(UUID uuid, String abilityId) {
        if (uuid == null || abilityId == null) return false;
        Set<String> set = purchasedMap.get(uuid);
        if (set == null) return false;
        return set.contains(abilityId.toLowerCase(Locale.ROOT));
    }

    public void recordPurchase(UUID uuid, String abilityId) {
        if (uuid == null || abilityId == null) return;
        purchasedMap
                .computeIfAbsent(uuid, u -> new HashSet<>())
                .add(abilityId.toLowerCase(Locale.ROOT));
    }

    public void resetAllPurchases() {
        purchasedMap.clear();
    }
}
