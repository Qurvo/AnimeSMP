package com.animesmp.core.trainer;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Trainers teach ONLY RARE abilities.
 *
 * In your tier naming:
 * - Rare abilities are AbilityTier.VENDOR (per your design)
 * - Epic abilities are AbilityTier.TRAINER (do NOT assign to trainers anymore)
 */
public class TrainerAbilityManager {

    private final AnimeSMPPlugin plugin;

    // trainerId -> abilityIds
    private final Map<String, List<String>> trainerAbilities = new HashMap<>();

    public TrainerAbilityManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        trainerAbilities.clear();

        boolean autoFill = plugin.getConfig().getBoolean("trainer-abilities.auto-fill-rares", true);
        if (autoFill) {
            autoDistributeRares();
            return;
        }

        // Manual config fallback: trainer-abilities.<trainerId>: [abilityId, ...]
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("trainer-abilities");
        if (sec == null) {
            autoDistributeRares();
            return;
        }

        for (String trainerId : sec.getKeys(false)) {
            if ("auto-fill-rares".equalsIgnoreCase(trainerId)) continue;
            List<String> list = plugin.getConfig().getStringList("trainer-abilities." + trainerId);
            if (list != null && !list.isEmpty()) {
                trainerAbilities.put(trainerId, new ArrayList<>(list));
            }
        }

        // If manual config empty, still fill
        if (trainerAbilities.isEmpty()) autoDistributeRares();
    }

    private void autoDistributeRares() {
        // Collect all RARE ability ids (tier contains "VENDOR")
        List<String> rares = new ArrayList<>();
        for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
            if (a == null || a.getTier() == null) continue;
            String tier = a.getTier().name().toUpperCase(Locale.ROOT);

            // Rare abilities = VENDOR tier in your system
            if (tier.contains("VENDOR")) {
                rares.add(a.getId());
            }
        }

        // Get trainer ids from config
        List<String> trainerIds = new ArrayList<>();
        ConfigurationSection trainers = plugin.getConfig().getConfigurationSection("trainers");
        if (trainers != null) trainerIds.addAll(trainers.getKeys(false));

        // If we can’t detect trainers, keep everything under a single bucket
        if (trainerIds.isEmpty()) {
            trainerAbilities.put("default", rares);
            return;
        }

        // Distribute evenly
        Collections.shuffle(rares, new Random());
        for (String id : trainerIds) {
            trainerAbilities.put(id, new ArrayList<>());
        }

        int idx = 0;
        for (String abilityId : rares) {
            String trainerId = trainerIds.get(idx % trainerIds.size());
            trainerAbilities.get(trainerId).add(abilityId);
            idx++;
        }
    }

    public List<String> getAbilitiesForTrainer(String trainerId) {
        if (trainerId == null) return Collections.emptyList();
        List<String> list = trainerAbilities.get(trainerId);
        if (list != null) return list;

        // fallback
        List<String> def = trainerAbilities.get("default");
        return def != null ? def : Collections.emptyList();
    }
}
