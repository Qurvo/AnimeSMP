package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.trainer.TrainerQuestManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TrainerQuestsCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;

    public TrainerQuestsCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        TrainerQuestManager tqm = plugin.getTrainerQuestManager();

        // This uses the helper already in TrainerQuestManager
        List<String> lines = tqm.buildQuestSummaryLines(player);

        player.sendMessage(ChatColor.GOLD + "===== " +
                ChatColor.AQUA + "Trainer Quests" +
                ChatColor.GOLD + " =====");

        if (lines == null || lines.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You have no active trainer quests.");
            player.sendMessage(ChatColor.DARK_GRAY + "Talk to a trainer NPC to start one.");
            return true;
        }

        for (String line : lines) {
            player.sendMessage(line);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No subcommands for now
        return Collections.emptyList();
    }
}
