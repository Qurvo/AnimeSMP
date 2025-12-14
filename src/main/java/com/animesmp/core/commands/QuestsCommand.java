package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.trainer.TrainerQuestManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestsCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;
    private final TrainerQuestManager questManager;

    public QuestsCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getTrainerQuestManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        List<String> lines = questManager.buildQuestSummaryLines(player);

        player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Your Trainer Quests" + ChatColor.GOLD + " =====");

        if (lines.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You have no active trainer quests.");
            return true;
        }

        for (String line : lines) {
            player.sendMessage(line);
        }

        return true;
    }
}
