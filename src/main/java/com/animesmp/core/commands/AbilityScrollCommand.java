package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.ability.AbilityRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbilityScrollCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;
    private final AbilityManager abilities;

    public AbilityScrollCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.abilities = plugin.getAbilityManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " give <player> <abilityId> [amount]");
            sender.sendMessage(ChatColor.GRAY + "Example: /" + label + " give " + sender.getName() + " fire_fist 1");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (!sub.equals("give")) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " give <player> <abilityId> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        String abilityId = args[2].toLowerCase();
        Ability ability = registry.getAbility(abilityId);
        if (ability == null) {
            sender.sendMessage(ChatColor.RED + "Unknown ability: " + abilityId);
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        ItemStack scroll = abilities.createAbilityScroll(ability, amount);
        if (scroll == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create scroll for ability: " + abilityId);
            return true;
        }

        target.getInventory().addItem(scroll);
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x Ability Scroll for " +
                ability.getDisplayName() + " to " + target.getName() + ".");
        if (!target.equals(sender)) {
            target.sendMessage(ChatColor.GOLD + "You received " + amount + "x Ability Scroll: " +
                    ChatColor.AQUA + ability.getDisplayName() + ChatColor.GOLD + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!sender.hasPermission("animesmp.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("give");
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }

        // Ability ID suggestions for /abilityscroll give <player> <abilityId> [amount]
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> options = new ArrayList<>();
            // Hardcode known ability IDs for now; add more here as you create them
            options.add("flashstep");
            options.add("fire_fist");
            options.add("shockwave");
            return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            List<String> options = new ArrayList<>();
            options.add("1");
            options.add("2");
            options.add("4");
            return StringUtil.copyPartialMatches(args[3], options, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
