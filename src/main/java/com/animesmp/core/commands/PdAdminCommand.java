package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.pd.PdEventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        sender.sendMessage(ChatColor.DARK_RED + "====== " + ChatColor.RED + "PD Admin" + ChatColor.DARK_RED + " ======");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin start" + ChatColor.GRAY + " - Start PD now.");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin stop" + ChatColor.GRAY + " - Stop PD now.");
        sender.sendMessage(ChatColor.GOLD + "/pdadmin status" + ChatColor.GRAY + " - View PD status.");
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

        switch (args[0].toLowerCase()) {
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
            sender.sendMessage(ChatColor.RED + "PD is already active.");
            return;
        }
        pd.adminStartNow();
        sender.sendMessage(ChatColor.GREEN + "PD started.");
    }

    private void handleStop(CommandSender sender) {
        if (!pd.isActive()) {
            sender.sendMessage(ChatColor.RED + "PD is not active.");
            return;
        }
        pd.adminStopNow();
        sender.sendMessage(ChatColor.GREEN + "PD stopped.");
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "====== " + ChatColor.RED + "PD Status" + ChatColor.DARK_RED + " ======");

        boolean active = pd.isActive();
        sender.sendMessage(ChatColor.GRAY + "Active: " + (active ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));

        if (active) {
            long rem = pd.getRemainingMs();
            sender.sendMessage(ChatColor.GRAY + "Time left: " + ChatColor.WHITE + formatHoursMinutes(rem));
            sender.sendMessage(ChatColor.GRAY + "Multipliers: " + ChatColor.AQUA + pd.getXpMultiplier() + "x XP"
                    + ChatColor.GRAY + ", " + ChatColor.YELLOW + pd.getYenMultiplier() + "x Yen");
            sender.sendMessage(ChatColor.GRAY + "Wipe level: " + ChatColor.WHITE + ">= " + pd.getMinWipeLevel());
        } else {
            long next = pd.getNextEligibleAutoStartMs(); // <-- compile fix relies on this existing
            if (next <= 0L) {
                sender.sendMessage(ChatColor.GRAY + "Next auto start: " + ChatColor.DARK_GRAY + "N/A");
            } else {
                ZoneId zone = ZoneId.of(plugin.getConfig().getString("vendor-reset.zone", "Europe/Lisbon"));
                ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), zone);

                long delta = Math.max(0L, next - System.currentTimeMillis());
                sender.sendMessage(ChatColor.GRAY + "Next auto start: " + ChatColor.WHITE + zdt.toLocalTime()
                        + ChatColor.DARK_GRAY + " (" + formatHoursMinutes(delta) + ")");
            }
        }
    }

    private String formatHoursMinutes(long ms) {
        long totalSec = ms / 1000L;
        long hours = totalSec / 3600L;
        long minutes = (totalSec % 3600L) / 60L;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("animesmp.admin")) return out;

        if (args.length == 1) {
            out.add("start");
            out.add("stop");
            out.add("status");
        }
        return out;
    }
}
