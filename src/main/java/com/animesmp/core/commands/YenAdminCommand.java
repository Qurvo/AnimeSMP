package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YenAdminCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager econ;

    public YenAdminCommand(AnimeSMPPlugin plugin) {
        this.econ = plugin.getEconomyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        switch (sub) {
            case "balance": {
                int yen = econ.getYen(target);
                int tokens = econ.getPdTokens(target);
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s balances:");
                sender.sendMessage(ChatColor.YELLOW + "Yen: " + ChatColor.AQUA + yen);
                sender.sendMessage(ChatColor.YELLOW + "PD Tokens: " + ChatColor.AQUA + tokens);
                return true;
            }

            case "set": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " set <player> <amount>");
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                econ.setYen(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Yen to " + amount + ".");
                return true;
            }

            case "add": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " add <player> <amount>");
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                econ.addYen(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Added " + amount + " Yen to " + target.getName() + ".");
                return true;
            }

            case "tokenset": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " tokenset <player> <amount>");
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                econ.setPdTokens(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s PD Tokens to " + amount + ".");
                return true;
            }

            case "tokenadd": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " tokenadd <player> <amount>");
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                econ.addPdTokens(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Added " + amount + " PD Tokens to " + target.getName() + ".");
                return true;
            }

            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " balance <player>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " add <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " tokenset <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " tokenadd <player> <amount>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!sender.hasPermission("animesmp.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("balance");
            options.add("set");
            options.add("add");
            options.add("tokenset");
            options.add("tokenadd");
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("set")
                || args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("tokenset")
                || args[0].equalsIgnoreCase("tokenadd"))) {
            List<String> options = new ArrayList<>();
            options.add("100");
            options.add("500");
            options.add("1000");
            return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
