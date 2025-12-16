package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.pd.PdEventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

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

        double cfgXp = plugin.getConfig().getDouble("pd-event.xp-multiplier", 2.0);
        double cfgYen = plugin.getConfig().getDouble("pd-event.yen-multiplier", 2.0);

        sender.sendMessage(ChatColor.DARK_RED + "====== " + ChatColor.RED + "Perma Death Status" + ChatColor.DARK_RED + " ======");
        sender.sendMessage(ChatColor.GOLD + "Active: " + (active ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
        sender.sendMessage(ChatColor.GOLD + "Min Wipe Level: " + ChatColor.YELLOW + pd.getMinWipeLevel());

        sender.sendMessage(ChatColor.GOLD + "XP Multiplier: " + ChatColor.AQUA + cfgXp + "x");
        sender.sendMessage(ChatColor.GOLD + "Yen Multiplier: " + ChatColor.AQUA + cfgYen + "x");

        if (active) {
            long ms = pd.getRemainingMs();
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            long remSec = seconds % 60;
            sender.sendMessage(ChatColor.GOLD + "Time Remaining: " + ChatColor.YELLOW + minutes + "m " + remSec + "s");
        } else {
            boolean autoEnabled = plugin.getConfig().getBoolean("pd-event.auto-start.enabled", true);
            int minOnline = plugin.getConfig().getInt("pd-event.auto-start.min-online", 10);

            sender.sendMessage(ChatColor.GOLD + "Auto Start: " + (autoEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            sender.sendMessage(ChatColor.GOLD + "Auto Start Min Online: " + ChatColor.YELLOW + minOnline);

            long nextEligible = pd.getNextEligibleAutoStartMs();
            if (nextEligible > 0) {
                long ms = Math.max(0L, nextEligible - System.currentTimeMillis());
                long seconds = ms / 1000;
                long minutes = seconds / 60;
                long remSec = seconds % 60;
                sender.sendMessage(ChatColor.GOLD + "Next Auto-Start Eligible In: " + ChatColor.YELLOW + minutes + "m " + remSec + "s");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Next Auto-Start Eligible In: " + ChatColor.YELLOW + "0m 0s");
            }
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
