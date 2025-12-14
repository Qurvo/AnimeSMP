package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.trainer.TrainerQuestManager;
import com.animesmp.core.trainer.TrainerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simple helper command for trainer system.
 *
 * /trainer                -> lists all trainers
 * /trainer open <id>      -> opens that trainer's GUI for testing
 */
public class TrainerCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final TrainerQuestManager questManager;

    public TrainerCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getTrainerQuestManager();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Trainer Help" + ChatColor.GOLD + " =====");
        sender.sendMessage(ChatColor.AQUA + "/trainer" + ChatColor.GRAY + " - List all trainers.");
        sender.sendMessage(ChatColor.AQUA + "/trainer open <id>" + ChatColor.GRAY + " - Open a trainer's quest GUI.");
        sender.sendMessage(ChatColor.DARK_GRAY + "Example IDs: " + ChatColor.GRAY + "moon_trainer, luffy_trainer, etc.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            // List all trainer types
            sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Available Trainers" + ChatColor.GOLD + " =====");
            for (TrainerType type : TrainerType.values()) {
                sender.sendMessage(ChatColor.YELLOW + "- " +
                        ChatColor.AQUA + ChatColor.stripColor(type.getDisplayName()) +
                        ChatColor.GRAY + " (ID: " + ChatColor.DARK_AQUA + type.getId() + ChatColor.GRAY + ")");
            }
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/trainer open <id> " +
                    ChatColor.GRAY + "to open a trainer's GUI.");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("open")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /trainer open <trainerId>");
                return true;
            }

            String id = args[1].toLowerCase(Locale.ROOT);
            TrainerType type = TrainerType.fromId(id);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown trainer id: " + id);
                sender.sendMessage(ChatColor.GRAY + "Use /trainer to list all trainers.");
                return true;
            }

            questManager.openTrainerGui(player, type);
            return true;
        }

        // Fallback / unknown subcommand
        sendHelp(sender);
        return true;
    }

    // ------------------------------------------------------------------------
    // Tab completion
    // ------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return out;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("open".startsWith(prefix)) out.add("open");
            return out;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (TrainerType type : TrainerType.values()) {
                String id = type.getId().toLowerCase(Locale.ROOT);
                if (id.startsWith(prefix)) {
                    out.add(id);
                }
            }
            return out;
        }

        return out;
    }
}
