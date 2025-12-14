package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.trainer.TrainerManager;
import com.animesmp.core.trainer.TrainerType;
import com.animesmp.core.trainer.TrainerManager.TrainerSpawn;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrainerAdminCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final TrainerManager trainerManager;

    public TrainerAdminCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.trainerManager = plugin.getTrainerManager();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Trainer Admin" + ChatColor.GOLD + " ======");
        sender.sendMessage(ChatColor.AQUA + "/traineradmin list" + ChatColor.GRAY + " - List all saved trainers.");
        sender.sendMessage(ChatColor.AQUA + "/traineradmin spawn <type>" + ChatColor.GRAY + " - Spawn/move trainer at your position.");
        sender.sendMessage(ChatColor.AQUA + "/traineradmin remove <type>" + ChatColor.GRAY + " - Remove trainer NPC + saved spawn.");
        sender.sendMessage(ChatColor.AQUA + "/traineradmin locate [type]" + ChatColor.GRAY + " - Show trainer coordinates.");
    }

    private TrainerType parseType(String raw) {
        if (raw == null) return null;
        raw = raw.toLowerCase(Locale.ROOT);
        for (TrainerType t : TrainerType.values()) {
            if (t.getId().equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw)) {
                return t;
            }
        }
        return null;
    }

    // ------------------------------------------------
    // Command
    // ------------------------------------------------

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

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;

            case "list":
                handleList(sender);
                return true;

            case "spawn":
                handleSpawn(sender, args);
                return true;

            case "remove":
                handleRemove(sender, args);
                return true;

            case "locate":
                handleLocate(sender, args);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    // /traineradmin list
    private void handleList(CommandSender sender) {
        Map<TrainerType, TrainerSpawn> spawns = trainerManager.getSpawns();
        if (spawns.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No trainers are currently registered.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Registered Trainers:");
        for (Map.Entry<TrainerType, TrainerSpawn> entry : spawns.entrySet()) {
            TrainerType type = entry.getKey();
            TrainerSpawn spawn = entry.getValue();
            Location loc = spawn.toLocation();
            if (loc == null) {
                sender.sendMessage(ChatColor.GRAY + "- " + type.getDisplayName() +
                        ChatColor.DARK_GRAY + " (world missing: " + spawn.worldName + ")");
                continue;
            }

            sender.sendMessage(ChatColor.AQUA + "- " + type.getDisplayName() +
                    ChatColor.GRAY + " @ " +
                    ChatColor.YELLOW + loc.getWorld().getName() +
                    ChatColor.GRAY + " [" +
                    ChatColor.WHITE + loc.getBlockX() + ", " +
                    loc.getBlockY() + ", " +
                    loc.getBlockZ() + ChatColor.GRAY + "]");
        }
    }

    // /traineradmin spawn <type>
    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this (needs a location).");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /traineradmin spawn <type>");
            return;
        }

        TrainerType type = parseType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown trainer type: " + args[1]);
            sender.sendMessage(ChatColor.GRAY + "Valid types:");
            StringBuilder sb = new StringBuilder(ChatColor.GRAY.toString());
            for (TrainerType t : TrainerType.values()) {
                sb.append(t.getId()).append(" ");
            }
            sender.sendMessage(sb.toString());
            return;
        }

        Player player = (Player) sender;
        trainerManager.spawnTrainerAt(player, type);

        sender.sendMessage(ChatColor.GREEN + "Spawned trainer " +
                ChatColor.AQUA + type.getDisplayName() +
                ChatColor.GREEN + " at your location.");
    }

    // /traineradmin remove <type>
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /traineradmin remove <type>");
            return;
        }

        TrainerType type = parseType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown trainer type: " + args[1]);
            return;
        }

        boolean removed = trainerManager.removeTrainer(type);
        if (removed) {
            sender.sendMessage(ChatColor.GREEN + "Removed trainer " +
                    ChatColor.AQUA + type.getDisplayName() +
                    ChatColor.GREEN + " (NPC + saved location).");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No saved trainer or NPC found for type " + type.getId());
        }
    }

    // /traineradmin locate [type]
    private void handleLocate(CommandSender sender, String[] args) {
        Map<TrainerType, TrainerSpawn> spawns = trainerManager.getSpawns();
        if (spawns.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No trainers are currently registered.");
            return;
        }

        if (args.length >= 2) {
            TrainerType type = parseType(args[1]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown trainer type: " + args[1]);
                return;
            }
            TrainerSpawn spawn = spawns.get(type);
            if (spawn == null) {
                sender.sendMessage(ChatColor.YELLOW + "No saved location for trainer type " + type.getId());
                return;
            }
            Location loc = spawn.toLocation();
            if (loc == null) {
                sender.sendMessage(ChatColor.YELLOW + "World " + spawn.worldName + " is missing for this trainer.");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "Trainer " + type.getDisplayName() +
                    ChatColor.GRAY + " is at " +
                    ChatColor.YELLOW + loc.getWorld().getName() +
                    ChatColor.GRAY + " [" +
                    ChatColor.WHITE + loc.getBlockX() + ", " +
                    loc.getBlockY() + ", " +
                    loc.getBlockZ() + ChatColor.GRAY + "]");
            return;
        }

        // No type provided -> list all
        handleList(sender);
    }

    // ------------------------------------------------
    // Tab complete
    // ------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (!sender.hasPermission("animesmp.admin")) return out;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : new String[]{"help", "list", "spawn", "remove", "locate"}) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn")
                || args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("locate"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (TrainerType t : TrainerType.values()) {
                String id = t.getId().toLowerCase(Locale.ROOT);
                if (id.startsWith(prefix)) {
                    out.add(id);
                }
            }
            return out;
        }

        return out;
    }
}
