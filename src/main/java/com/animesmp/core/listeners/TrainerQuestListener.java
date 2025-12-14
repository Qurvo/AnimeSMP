package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.trainer.TrainerQuest;
import com.animesmp.core.trainer.TrainerQuestManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class TrainerQuestListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final TrainerQuestManager questManager;
    private final PlayerProfileManager profiles;
    private final NamespacedKey abilityKey;

    public TrainerQuestListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getTrainerQuestManager();
        this.profiles = plugin.getProfileManager();
        this.abilityKey = new NamespacedKey(plugin, "trainer_ability_id");
    }

    @EventHandler
    public void onTrainerGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() == null ||
                event.getClickedInventory() != top) {
            return; // only react to clicks in the top (trainer) inventory
        }

        String title = event.getView().getTitle();
        if (title == null) return;
        String stripped = ChatColor.stripColor(title);
        if (stripped == null || !stripped.startsWith("Trainer: ")) return;

        // This is our trainer GUI, cancel item movement
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String abilityId = pdc.get(abilityKey, PersistentDataType.STRING);
        if (abilityId == null || abilityId.isEmpty()) return;

        PlayerProfile profile = profiles.getProfile(player);
        TrainerQuest quest = questManager.getQuest(abilityId);

        // Already mastered
        if (profile.hasCompletedTrainerQuest(abilityId)) {
            player.sendMessage(ChatColor.GREEN + "You have already mastered this ability.");
            return;
        }

        if (quest == null) {
            player.sendMessage(ChatColor.RED + "No trainer quest is configured for this ability.");
            return;
        }

        String activeId = profile.getActiveTrainerQuestId();
        if (activeId != null && activeId.equalsIgnoreCase(abilityId)) {
            int prog = profile.getActiveTrainerQuestProgress();
            player.sendMessage(ChatColor.AQUA + "Training progress: "
                    + ChatColor.YELLOW + prog + "/" + quest.getRequiredAmount());

            if (prog >= quest.getRequiredAmount()) {
                // Safety: ensure completion if auto-complete already should have triggered
                questManager.completeQuest(player, abilityId);
                player.closeInventory();
            }
            return;
        }

        // Start this quest (replaces any existing active trainer quest)
        questManager.startQuest(player, abilityId);
        player.closeInventory();
    }
}
