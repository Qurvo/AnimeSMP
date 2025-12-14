package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.pd.PdEventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PdAdminCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final PdEventManager pd;

    public PdAdminCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.pd = plugin.getPdEventManager();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "====== " + ChatColor.RED + "Perma Death Admin" + ChatColor.DARK_RED + " ======");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin start" + ChatColor.GRAY + " - Start a Perma Death window now.");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin stop" + ChatColor.GRAY + " - Force stop Perma Death.");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin status" + ChatColor.GRAY + " - View current PD status.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
                handleStart(sender);
                return true;
            case "stop":
                handleStop(sender);
                return true;
            case "status":
                handleStatus(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void handleStart(CommandSender sender) {
        if (pd.isActive()) {
            sender.sendMessage(ChatColor.RED + "Perma Death is already active.");
            return;
        }
        pd.adminStartNow();
        sender.sendMessage(ChatColor.GREEN + "Perma Death started. All players have been notified.");
    }

    private void handleStop(CommandSender sender) {
        if (!pd.isActive()) {
            sender.sendMessage(ChatColor.RED + "Perma Death is not currently active.");
            return;
        }
        pd.adminStopNow();
        sender.sendMessage(ChatColor.YELLOW + "Perma Death has been stopped.");
    }

    private void handleStatus(CommandSender sender) {
        boolean active = pd.isActive();
        sender.sendMessage(ChatColor.DARK_RED + "====== " + ChatColor.RED + "Perma Death Status" + ChatColor.DARK_RED + " ======");
        sender.sendMessage(ChatColor.GOLD + "Active: " + (active ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
        sender.sendMessage(ChatColor.GOLD + "Min Wipe Level: " + ChatColor.YELLOW + pd.getMinWipeLevel());
        sender.sendMessage(ChatColor.GOLD + "XP Multiplier: " + ChatColor.AQUA + pd.getXpMultiplier());

        if (active) {
            long ms = pd.getRemainingMs();
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            long remSec = seconds % 60;
            sender.sendMessage(ChatColor.GOLD + "Time Remaining: " +
                    ChatColor.YELLOW + minutes + "m " + remSec + "s");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("animesmp.admin")) return out;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String s : new String[]{"start", "stop", "status"}) {
                if (s.startsWith(prefix)) out.add(s);
            }
        }
        return out;
    }
}
