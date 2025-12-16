package com.animesmp.core.trainer;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityLoreUtil;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TrainerQuestManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final AbilityRegistry abilityRegistry;

    private final Map<String, List<String>> trainerAbilities = new HashMap<>();
    private final Map<String, TrainerQuest> questData = new HashMap<>();

    private final NamespacedKey abilityKey;

    public TrainerQuestManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.abilityRegistry = plugin.getAbilityRegistry();
        this.abilityKey = new NamespacedKey(plugin, "trainer_ability_id");

        loadTrainerAbilities();
        loadQuestData();
    }

    private void loadTrainerAbilities() {
        trainerAbilities.clear();

        trainerAbilities.put("ichigo", Arrays.asList(
                "soru_step",
                "thunderclap_dash",
                "crescent_moon_arc",
                "getsu_wave",
                "breathing_focus",
                "observation_sense"
        ));

        trainerAbilities.put("ace", Arrays.asList(
                "fire_fist",
                "exploding_blood_burst",
                "shockwave_roar",
                "thunder_beam",
                "chakra_renewal",
                "ki_barrier"
        ));

        trainerAbilities.put("kokushibo", Arrays.asList(
                "crescent_typhoon_slash",
                "wind_typhoon_slash",
                "cursed_black_flash",
                "full_moon_typhoon",
                "cursed_pulse",
                "shadow_bind"
        ));

        trainerAbilities.put("luffy", Arrays.asList(
                "sky_walk",
                "ki_blast_barrage",
                "thunder_pierce",
                "iron_body",
                "water_parry",
                "beast_instinct"
        ));
    }

    private void loadQuestData() {
        questData.clear();

        questData.put("soru_step", new TrainerQuest("soru_step", "Sprint a total of 300 meters.", 300, TrainerQuest.Type.DISTANCE_SPRINT));
        questData.put("thunderclap_dash", new TrainerQuest("thunderclap_dash", "Sprint a total of 400 meters.", 400, TrainerQuest.Type.DISTANCE_SPRINT));
        questData.put("crescent_moon_arc", new TrainerQuest("crescent_moon_arc", "Defeat 20 mobs using any damage ability.", 20, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("getsu_wave", new TrainerQuest("getsu_wave", "Defeat 25 mobs using any damage ability.", 25, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("breathing_focus", new TrainerQuest("breathing_focus", "Defeat 15 mobs while above 75% stamina.", 15, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("observation_sense", new TrainerQuest("observation_sense", "Kill 5 players without dying.", 5, TrainerQuest.Type.PLAYER_KILL_MELEE));

        questData.put("fire_fist", new TrainerQuest("fire_fist", "Deal 250 total fire-type damage.", 250, TrainerQuest.Type.DAMAGE_FIRE));
        questData.put("exploding_blood_burst", new TrainerQuest("exploding_blood_burst", "Deal 300 total fire or explosion damage.", 300, TrainerQuest.Type.DAMAGE_FIRE));
        questData.put("shockwave_roar", new TrainerQuest("shockwave_roar", "Defeat 25 mobs using heavy abilities.", 25, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("thunder_beam", new TrainerQuest("thunder_beam", "Deal 350 total lightning or beam damage.", 350, TrainerQuest.Type.DAMAGE_FIRE));
        questData.put("chakra_renewal", new TrainerQuest("chakra_renewal", "Defeat 20 mobs while above 50% stamina.", 20, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("ki_barrier", new TrainerQuest("ki_barrier", "Kill 5 players while under 50% HP.", 5, TrainerQuest.Type.PLAYER_KILL_MELEE));

        questData.put("crescent_typhoon_slash", new TrainerQuest("crescent_typhoon_slash", "Kill 10 players with melee attacks.", 10, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("wind_typhoon_slash", new TrainerQuest("wind_typhoon_slash", "Defeat 25 mobs using damage abilities.", 25, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("cursed_black_flash", new TrainerQuest("cursed_black_flash", "Kill 8 players with melee attacks.", 8, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("full_moon_typhoon", new TrainerQuest("full_moon_typhoon", "Kill 12 players during night time.", 12, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("cursed_pulse", new TrainerQuest("cursed_pulse", "Deal 250 total cursed damage.", 250, TrainerQuest.Type.DAMAGE_FIRE));
        questData.put("shadow_bind", new TrainerQuest("shadow_bind", "Defeat 20 mobs while they are debuffed.", 20, TrainerQuest.Type.MOB_KILL_LIGHT));

        questData.put("sky_walk", new TrainerQuest("sky_walk", "Sprint a total of 400 meters.", 400, TrainerQuest.Type.DISTANCE_SPRINT));
        questData.put("ki_blast_barrage", new TrainerQuest("ki_blast_barrage", "Defeat 30 mobs using any damage ability.", 30, TrainerQuest.Type.MOB_KILL_LIGHT));
        questData.put("thunder_pierce", new TrainerQuest("thunder_pierce", "Kill 8 players with melee attacks.", 8, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("iron_body", new TrainerQuest("iron_body", "Kill 5 players while under 40% HP.", 5, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("water_parry", new TrainerQuest("water_parry", "Parry and kill 3 players in duels.", 3, TrainerQuest.Type.PLAYER_KILL_MELEE));
        questData.put("beast_instinct", new TrainerQuest("beast_instinct", "Defeat 20 mobs without dropping below 50% HP.", 20, TrainerQuest.Type.MOB_KILL_LIGHT));
    }

    public List<Ability> getAbilitiesForTrainer(String trainerId) {
        List<String> ids = trainerAbilities.getOrDefault(trainerId.toLowerCase(Locale.ROOT), Collections.emptyList());
        List<Ability> result = new ArrayList<>();
        for (String id : ids) {
            Ability a = abilityRegistry.getAbility(id);
            if (a != null) result.add(a);
        }
        return result;
    }

    public TrainerQuest getQuest(String abilityId) {
        if (abilityId == null) return null;
        return questData.get(abilityId.toLowerCase(Locale.ROOT));
    }

    public void startQuest(Player player, String abilityId) {
        PlayerProfile p = profiles.getProfile(player);

        if (p.hasCompletedTrainerQuest(abilityId)) {
            player.sendMessage(ChatColor.YELLOW + "You already mastered this ability.");
            return;
        }

        TrainerQuest q = getQuest(abilityId);
        if (q == null) {
            player.sendMessage(ChatColor.RED + "Trainer quest not found for this ability.");
            return;
        }

        p.setActiveTrainerQuestId(abilityId);
        p.setActiveTrainerQuestProgress(0);

        player.sendMessage(ChatColor.GREEN + "You begin training: " + ChatColor.AQUA + q.getRequirementText());
    }

    public void addProgress(Player player, TrainerQuest.Type type, int amount) {
        PlayerProfile p = profiles.getProfile(player);

        String activeId = p.getActiveTrainerQuestId();
        if (activeId == null) return;

        TrainerQuest quest = getQuest(activeId);
        if (quest == null) return;
        if (quest.getType() != type) return;

        int newVal = p.getActiveTrainerQuestProgress() + amount;
        p.setActiveTrainerQuestProgress(newVal);

        if (newVal >= quest.getRequiredAmount()) {
            completeQuest(player, activeId);
        }
    }

    public void completeQuest(Player player, String abilityId) {
        PlayerProfile p = profiles.getProfile(player);

        p.addCompletedTrainerQuest(abilityId);
        p.setActiveTrainerQuestId(null);
        p.setActiveTrainerQuestProgress(0);

        Ability ability = abilityRegistry.getAbility(abilityId);
        String display = (ability != null ? ability.getDisplayName() : abilityId);

        player.sendMessage(ChatColor.GOLD + "Training Complete! " + ChatColor.AQUA + "You have mastered " + display + "!");

        p.unlockAbility(abilityId);
    }

    public void openTrainerGui(Player player, TrainerType type) {
        if (type == null) return;
        openTrainerGui(player, type.name().toLowerCase(Locale.ROOT));
    }

    public void openTrainerGui(Player player, String trainerId) {
        List<Ability> abilities = getAbilitiesForTrainer(trainerId);
        if (abilities.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This trainer has nothing to teach right now.");
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_AQUA + "Trainer: " + trainerId.toUpperCase(Locale.ROOT)
        );

        int slot = 10;
        for (Ability ability : abilities) {
            inv.setItem(slot, buildAbilityItem(player, ability));
            slot++;
            if (slot == 17) slot = 19;
            if (slot >= 26) break;
        }

        player.openInventory(inv);
    }

    private ItemStack buildAbilityItem(Player player, Ability ability) {
        PlayerProfile profile = profiles.getProfile(player);
        String abilityId = ability.getId();

        boolean completed = profile.hasCompletedTrainerQuest(abilityId);
        boolean inProgress = abilityId.equalsIgnoreCase(profile.getActiveTrainerQuestId());

        TrainerQuest q = getQuest(abilityId);

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return book;

        meta.setDisplayName(ChatColor.AQUA + ability.getDisplayName());

        String questSummary = (q == null ? null : q.getRequirementText());
        List<String> lore = AbilityLoreUtil.trainerLore(plugin, ability, questSummary);

        // Prepend state indicators
        List<String> prefix = new ArrayList<>();
        if (completed) {
            prefix.add(ChatColor.GREEN + "✔ Already Mastered");
            prefix.add("");
        } else if (q != null) {
            if (inProgress) {
                int prog = profile.getActiveTrainerQuestProgress();
                prefix.add(ChatColor.GOLD + "[IN PROGRESS]");
                prefix.add(ChatColor.AQUA + "Progress: " + prog + "/" + q.getRequiredAmount());
                prefix.add("");
            }
        }

        if (!prefix.isEmpty()) {
            prefix.addAll(lore);
            lore = prefix;
        }

        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(abilityKey, PersistentDataType.STRING, abilityId.toLowerCase(Locale.ROOT));

        book.setItemMeta(meta);
        return book;
    }

    public List<String> buildQuestSummaryLines(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile p = profiles.getProfile(player);

        lines.add(ChatColor.GOLD + "====== Trainer Quests ======");

        if (questData.isEmpty()) {
            lines.add(ChatColor.GRAY + "No trainer quests configured.");
            return lines;
        }

        for (TrainerQuest q : questData.values()) {
            String abilityId = q.getAbilityId();
            Ability ability = abilityRegistry.getAbility(abilityId);
            String name = (ability != null ? ability.getDisplayName() : abilityId);

            boolean completed = p.hasCompletedTrainerQuest(abilityId);
            boolean inProgress = abilityId.equalsIgnoreCase(p.getActiveTrainerQuestId());

            String trainerName = findTrainerNameForAbility(abilityId);

            String status;
            if (completed) status = ChatColor.GREEN + "COMPLETED";
            else if (inProgress) status = ChatColor.GOLD + "IN PROGRESS";
            else status = ChatColor.RED + "LOCKED";

            lines.add(ChatColor.AQUA + "- " + name
                    + ChatColor.DARK_GRAY + " [" + trainerName + "] "
                    + status);

            lines.add(ChatColor.GRAY + "  " + q.getRequirementText());
            if (inProgress) {
                int prog = p.getActiveTrainerQuestProgress();
                lines.add(ChatColor.GRAY + "  Progress: " + ChatColor.YELLOW + prog + "/" + q.getRequiredAmount());
            }
        }

        return lines;
    }

    private String findTrainerNameForAbility(String abilityId) {
        for (Map.Entry<String, List<String>> e : trainerAbilities.entrySet()) {
            if (e.getValue().contains(abilityId)) {
                String id = e.getKey();
                return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
            }
        }
        return "Unknown";
    }
}
