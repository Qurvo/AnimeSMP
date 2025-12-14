package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.training.TrainingManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrainCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final TrainingManager training;

    public TrainCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.training = plugin.getTrainingManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            training.sendTrainingInfo(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
                training.startTrainingWithToken(player);
                return true;

            case "addxp":
                if (!player.hasPermission("animesmp.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /train addxp <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Amount must be positive.");
                        return true;
                    }
                    training.addTrainingXp(player, amount);
                    player.sendMessage(ChatColor.GREEN + "Added " + amount + " training XP.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Amount must be a number.");
                }
                return true;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use:");
                player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - show training info");
                player.sendMessage(ChatColor.YELLOW + "/" + label + " start" + ChatColor.GRAY + " - train using a token");
                if (player.hasPermission("animesmp.admin")) {
                    player.sendMessage(ChatColor.YELLOW + "/" + label + " addxp <amount>" +
                            ChatColor.GRAY + " - debug: add training XP");
                }
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("info");
            options.add("start");
            if (player.hasPermission("animesmp.admin")) {
                options.add("addxp");
            }
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("addxp") && player.hasPermission("animesmp.admin")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("50");
            suggestions.add("100");
            suggestions.add("250");
            return StringUtil.copyPartialMatches(args[1], suggestions, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
