package com.animesmp.core.trainer;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.AbilityRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class TrainerAbilityManager {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;

    // trainerId -> set of abilityIds it can teach
    private final Map<String, Set<String>> trainerAbilities = new HashMap<>();

    // abilityId -> primary trainerId (first one in config)
    private final Map<String, String> abilityPrimaryTrainer = new HashMap<>();

    // trainerId -> minimum player level required to train
    private final Map<String, Integer> trainerMinLevel = new HashMap<>();

    public TrainerAbilityManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        loadFromConfig();
    }

    private void loadFromConfig() {
        trainerAbilities.clear();
        abilityPrimaryTrainer.clear();
        trainerMinLevel.clear();

        FileConfiguration cfg = plugin.getConfig();
        // separate section so we don't interfere with existing "trainers:" block
        ConfigurationSection trainersSec = cfg.getConfigurationSection("trainer-abilities");
        if (trainersSec == null) {
            plugin.getLogger().warning("No 'trainer-abilities' section found in config.yml for TrainerAbilityManager.");
            return;
        }

        for (String trainerIdRaw : trainersSec.getKeys(false)) {
            String trainerId = trainerIdRaw.toLowerCase(Locale.ROOT);
            ConfigurationSection tSec = trainersSec.getConfigurationSection(trainerIdRaw);
            if (tSec == null) continue;

            int minLevel = tSec.getInt("min-level", 0);
            trainerMinLevel.put(trainerId, minLevel);

            List<String> abilityIds = tSec.getStringList("teachesAbilities");
            if (abilityIds == null || abilityIds.isEmpty()) continue;

            Set<String> set = new HashSet<>();
            for (String rawId : abilityIds) {
                if (rawId == null || rawId.isEmpty()) continue;
                String id = rawId.toLowerCase(Locale.ROOT);

                // Only register if the ability actually exists
                if (registry.getAbility(id) == null) {
                    plugin.getLogger().warning("[TrainerAbilityManager] Trainer '" + trainerIdRaw
                            + "' references unknown ability '" + id + "'");
                    continue;
                }

                set.add(id);

                // If no primary trainer yet, set this trainer as primary for the ability
                abilityPrimaryTrainer.putIfAbsent(id, trainerId);
            }

            if (!set.isEmpty()) {
                trainerAbilities.put(trainerId, set);
            }
        }

        plugin.getLogger().info("[TrainerAbilityManager] Loaded "
                + trainerAbilities.size() + " trainers with ability mappings.");
    }

    // ----------------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------------

    /**
     * Returns true if this trainer is allowed to teach the given ability.
     */
    public boolean canTrainerTeach(String trainerId, String abilityId) {
        if (trainerId == null || abilityId == null) return false;
        Set<String> abilities = trainerAbilities.get(trainerId.toLowerCase(Locale.ROOT));
        if (abilities == null) return false;
        return abilities.contains(abilityId.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns abilities this trainer can teach (as IDs).
     */
    public Set<String> getAbilitiesForTrainer(String trainerId) {
        if (trainerId == null) return Collections.emptySet();
        Set<String> set = trainerAbilities.get(trainerId.toLowerCase(Locale.ROOT));
        if (set == null) return Collections.emptySet();
        return new HashSet<>(set);
    }

    /**
     * Returns the "primary" trainer for an ability (first one in config), or null.
     */
    public String getPrimaryTrainerForAbility(String abilityId) {
        if (abilityId == null) return null;
        return abilityPrimaryTrainer.get(abilityId.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns true if this ability is supposed to be taught by a trainer.
     */
    public boolean isTrainerTaughtAbility(String abilityId) {
        return getPrimaryTrainerForAbility(abilityId) != null;
    }

    /**
     * Minimum player level required to train with this trainer.
     */
    public int getMinLevel(String trainerId) {
        if (trainerId == null) return 0;
        return trainerMinLevel.getOrDefault(trainerId.toLowerCase(Locale.ROOT), 0);
    }

    /**
     * Reloads mappings from config.
     */
    public void reload() {
        loadFromConfig();
    }
}
