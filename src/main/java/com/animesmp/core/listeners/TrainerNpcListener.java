package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.trainer.TrainerQuestManager;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class TrainerNpcListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final TrainerQuestManager questManager;
    private final NamespacedKey trainerKey;

    public TrainerNpcListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getTrainerQuestManager();
        this.trainerKey = new NamespacedKey(plugin, "trainer_id");
    }

    @EventHandler
    public void onTrainerInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager villager)) return;

        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String trainerId = pdc.get(trainerKey, PersistentDataType.STRING);
        if (trainerId == null || trainerId.isEmpty()) return;

        trainerId = trainerId.toLowerCase(Locale.ROOT);

        // This is one of our trainers: block villager trades
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Simple flavour dialogue per trainer
        switch (trainerId) {
            case "ichigo":
                player.sendMessage(ChatColor.AQUA + "Ichigo: " + ChatColor.GRAY +
                        "Speed and resolve. Let's see if you can keep up.");
                break;
            case "ace":
                player.sendMessage(ChatColor.GOLD + "Ace: " + ChatColor.GRAY +
                        "If you can't handle the heat, step away from the flames.");
                break;
            case "kokushibo":
                player.sendMessage(ChatColor.DARK_PURPLE + "Kokushibo: " + ChatColor.GRAY +
                        "The moon reveals the weak. Do not disappoint it.");
                break;
            case "luffy":
                player.sendMessage(ChatColor.YELLOW + "Luffy: " + ChatColor.GRAY +
                        "Let's get stronger together! Don't hold back!");
                break;
            default:
                player.sendMessage(ChatColor.GOLD + "Trainer: " + ChatColor.GRAY +
                        "Show me your resolve.");
                break;
        }

        // Open the trainer quest GUI for this trainer
        questManager.openTrainerGui(player, trainerId);
    }
}
