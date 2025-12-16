package com.animesmp.core.player;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerProfileManager {

    private final AnimeSMPPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public PlayerProfileManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "profiles");
        if (!folder.exists()) folder.mkdirs();
    }

    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::loadProfile);
    }

    private File getFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    private PlayerProfile loadProfile(UUID uuid) {
        PlayerProfile profile = new PlayerProfile(uuid);
        File file = getFile(uuid);

        if (!file.exists()) return profile;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        profile.setLevel(cfg.getInt("level", 1));
        profile.setXp(cfg.getInt("xp", 0));

        profile.setSkillPoints(cfg.getInt("skillPoints", 0));
        profile.setConPoints(cfg.getInt("conPoints", 0));
        profile.setStrPoints(cfg.getInt("strPoints", 0));
        profile.setTecPoints(cfg.getInt("tecPoints", 0));
        profile.setDexPoints(cfg.getInt("dexPoints", 0));

        profile.setTrainingLevel(cfg.getInt("trainingLevel", 0));
        profile.setTrainingXp(cfg.getInt("trainingXp", 0));

        profile.setStaminaCap(cfg.getInt("staminaCap", 100));
        profile.setStaminaCurrent(cfg.getDouble("staminaCurrent", 100));
        profile.setStaminaRegenPerSecond(cfg.getDouble("staminaRegenPerSecond", 6.0));

        profile.setYen(cfg.getInt("yen", 0));
        profile.setPdTokens(cfg.getInt("pdTokens", 0));

        profile.setSelectedSlot(cfg.getInt("selectedSlot", 1));

        profile.setTutorialCompleted(cfg.getBoolean("tutorialCompleted", false));

        // Load daily rotating vendor purchases
        List<String> purchased = cfg.getStringList("purchasedToday");
        profile.clearDailyPurchases();
        for (String id : purchased) profile.markPurchased(id);

        // Load ability bindings
        ConfigurationSection bindings = cfg.getConfigurationSection("bindings");
        if (bindings != null) {
            for (String key : bindings.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    String abil = bindings.getString(key);
                    profile.setBoundAbilityId(slot, abil);
                } catch (Exception ignored) {}
            }
        }

        for (String id : cfg.getStringList("unlockedAbilities"))
            profile.unlockAbility(id);

        profile.setActiveTrainerQuestId(cfg.getString("activeTrainerQuestId", null));
        profile.setActiveTrainerQuestProgress(cfg.getInt("activeTrainerQuestProgress", 0));

        for (String id : cfg.getStringList("completedTrainerQuests"))
            profile.addCompletedTrainerQuest(id);

        return profile;
    }

    public void saveProfile(PlayerProfile profile) {
        if (profile == null) return;

        File file = getFile(profile.getUuid());
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("level", profile.getLevel());
        cfg.set("xp", profile.getXp());

        cfg.set("skillPoints", profile.getSkillPoints());
        cfg.set("conPoints", profile.getConPoints());
        cfg.set("strPoints", profile.getStrPoints());
        cfg.set("tecPoints", profile.getTecPoints());
        cfg.set("dexPoints", profile.getDexPoints());

        cfg.set("trainingLevel", profile.getTrainingLevel());
        cfg.set("trainingXp", profile.getTrainingXp());

        cfg.set("staminaCap", profile.getStaminaCap());
        cfg.set("staminaCurrent", profile.getStaminaCurrent());
        cfg.set("staminaRegenPerSecond", profile.getStaminaRegenPerSecond());

        cfg.set("yen", profile.getYen());
        cfg.set("pdTokens", profile.getPdTokens());

        cfg.set("selectedSlot", profile.getSelectedSlot());
        cfg.set("tutorialCompleted", profile.isTutorialCompleted());

        // Save daily rotating vendor purchases
        cfg.set("purchasedToday", new ArrayList<>(profile.getPurchasedToday()));

        // Save ability bindings
        cfg.set("bindings", null);
        for (Map.Entry<Integer, String> e : profile.getBoundAbilityIds().entrySet())
            cfg.set("bindings." + e.getKey(), e.getValue());

        cfg.set("unlockedAbilities", new ArrayList<>(profile.getUnlockedAbilities()));

        cfg.set("activeTrainerQuestId", profile.getActiveTrainerQuestId());
        cfg.set("activeTrainerQuestProgress", profile.getActiveTrainerQuestProgress());
        cfg.set("completedTrainerQuests", new ArrayList<>(profile.getCompletedTrainerQuests()));

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save profile " + profile.getUuid());
            e.printStackTrace();
        }
    }

    public void saveAll() {
        profiles.values().forEach(this::saveProfile);
    }

    public void handleJoin(Player player) {
        getProfile(player);
    }

    public void handleQuit(Player player) {
        saveProfile(profiles.get(player.getUniqueId()));
    }
}
