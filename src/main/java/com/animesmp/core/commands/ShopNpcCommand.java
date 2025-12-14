package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShopNpcCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final NamespacedKey npcKey;

    public ShopNpcCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "shop_npc_type");
    }

    private boolean noPerm(CommandSender sender) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "======== " + ChatColor.YELLOW + "Shop NPC Admin" + ChatColor.GOLD + " ========");
        sender.sendMessage(ChatColor.AQUA + "/shopnpc ability" + ChatColor.GRAY + " - Spawn Ability Vendor at your location.");
        sender.sendMessage(ChatColor.AQUA + "/shopnpc training" + ChatColor.GRAY + " - Spawn Training Token Vendor at your location.");
        sender.sendMessage(ChatColor.AQUA + "/shopnpc pd" + ChatColor.GRAY + " - Spawn PD Vendor at your location.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (noPerm(sender)) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "ability":
                createNpc(player, "ABILITY_VENDOR", ChatColor.GOLD + "Ability Vendor");
                return true;

            case "training":
                createNpc(player, "TRAINING_VENDOR", ChatColor.AQUA + "Training Vendor");
                return true;

            case "pd":
            case "pdshop":
                createNpc(player, "PD_VENDOR", ChatColor.LIGHT_PURPLE + "PD Vendor");
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void createNpc(Player player, String type, String name) {
        Location loc = player.getLocation();

        Villager villager = loc.getWorld().spawn(loc, Villager.class, v -> {
            v.setCustomName(name);
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setCollidable(false);
            v.setVillagerLevel(1);
            v.setProfession(Villager.Profession.NONE);
        });

        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(npcKey, PersistentDataType.STRING, type.toUpperCase(Locale.ROOT));

        player.sendMessage(ChatColor.GREEN + "Spawned " + ChatColor.YELLOW + type +
                ChatColor.GREEN + " NPC at your location.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (!sender.hasPermission("animesmp.admin")) {
            return out;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String opt : Arrays.asList("ability", "training", "pd")) {
                if (opt.startsWith(prefix)) {
                    out.add(opt);
                }
            }
        }

        return out;
    }
}
