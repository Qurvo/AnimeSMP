package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.economy.EconomyManager;
import com.animesmp.core.training.TrainingManager;
import com.animesmp.core.training.TrainingTokenTier;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VendorCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final EconomyManager econ;
    private final AbilityRegistry registry;
    private final AbilityManager abilityManager;
    private final TrainingManager trainingManager;

    public VendorCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.econ = plugin.getEconomyManager();
        this.registry = plugin.getAbilityRegistry();
        this.abilityManager = plugin.getAbilityManager();
        this.trainingManager = plugin.getTrainingManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendShopList(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("buy")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " buy <itemId>");
                player.sendMessage(ChatColor.GRAY + "Use /" + label + " list to see items.");
                return true;
            }

            String id = args[1].toLowerCase();
            buyItem(player, id);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand. Use:");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - view shop");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " buy <itemId>" + ChatColor.GRAY + " - purchase item");
        return true;
    }

    private void sendShopList(Player player) {
        int yen = econ.getYen(player);

        player.sendMessage(ChatColor.DARK_GRAY + "------------------------------");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " Ability Vendor");
        player.sendMessage(ChatColor.YELLOW + "Your Yen: " + ChatColor.AQUA + yen);
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/vendor buy <itemId>" + ChatColor.GRAY + " to purchase.");
        player.sendMessage("");

        // List items
        player.sendMessage(ChatColor.YELLOW + "flashstep_scroll" + ChatColor.GRAY +
                " - " + ChatColor.AQUA + "750 Yen" + ChatColor.GRAY + " (Ability Scroll: Flashstep)");
        player.sendMessage(ChatColor.YELLOW + "fire_fist_scroll" + ChatColor.GRAY +
                " - " + ChatColor.AQUA + "1000 Yen" + ChatColor.GRAY + " (Ability Scroll: Fire Fist)");
        player.sendMessage(ChatColor.YELLOW + "shockwave_scroll" + ChatColor.GRAY +
                " - " + ChatColor.AQUA + "900 Yen" + ChatColor.GRAY + " (Ability Scroll: Shockwave)");
        player.sendMessage(ChatColor.YELLOW + "basic_token" + ChatColor.GRAY +
                " - " + ChatColor.AQUA + "500 Yen" + ChatColor.GRAY + " (Basic Training Token)");
        player.sendMessage(ChatColor.DARK_GRAY + "------------------------------");
    }

    private void buyItem(Player player, String id) {
        int price;
        ItemStack item = null;

        switch (id) {
            case "flashstep_scroll": {
                Ability ab = registry.getAbility("flashstep");
                if (ab == null) {
                    player.sendMessage(ChatColor.RED + "That ability is not available right now.");
                    return;
                }
                price = 750;
                item = abilityManager.createAbilityScroll(ab, 1);
                break;
            }
            case "fire_fist_scroll": {
                Ability ab = registry.getAbility("fire_fist");
                if (ab == null) {
                    player.sendMessage(ChatColor.RED + "That ability is not available right now.");
                    return;
                }
                price = 1000;
                item = abilityManager.createAbilityScroll(ab, 1);
                break;
            }
            case "shockwave_scroll": {
                Ability ab = registry.getAbility("shockwave");
                if (ab == null) {
                    player.sendMessage(ChatColor.RED + "That ability is not available right now.");
                    return;
                }
                price = 900;
                item = abilityManager.createAbilityScroll(ab, 1);
                break;
            }
            case "basic_token": {
                price = 500;
                item = trainingManager.createToken(TrainingTokenTier.BASIC, 1);
                break;
            }
            default:
                player.sendMessage(ChatColor.RED + "Unknown shop item: " + id);
                player.sendMessage(ChatColor.GRAY + "Use /vendor list to see all items.");
                return;
        }

        if (item == null) {
            player.sendMessage(ChatColor.RED + "Failed to create the item. Contact an admin.");
            return;
        }

        if (!econ.trySpendYen(player, price)) {
            player.sendMessage(ChatColor.RED + "You don't have enough Yen.");
            return;
        }

        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.AQUA + id +
                ChatColor.GREEN + " for " + ChatColor.AQUA + price + " Yen" + ChatColor.GREEN + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("buy");
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            List<String> options = new ArrayList<>();
            options.add("flashstep_scroll");
            options.add("fire_fist_scroll");
            options.add("shockwave_scroll");
            options.add("basic_token");
            return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
