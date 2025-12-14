package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.training.TrainingTokenTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrainingTokenCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final NamespacedKey tokenKey;

    public TrainingTokenCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.tokenKey = new NamespacedKey(plugin, "training_token_tier");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /traintoken give <player> <tier> [amount]");
            sender.sendMessage(ChatColor.GRAY + "Tiers: BASIC, INTERMEDIATE, ADVANCED, EXPERT, MASTER");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sub.equals("give")) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use: /traintoken give <player> <tier> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        TrainingTokenTier tier = TrainingTokenTier.fromString(args[2]);
        if (tier == null) {
            sender.sendMessage(ChatColor.RED + "Unknown tier: " + args[2]);
            sender.sendMessage(ChatColor.GRAY + "Valid: BASIC, INTERMEDIATE, ADVANCED, EXPERT, MASTER");
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack token = createToken(tier, amount);
        target.getInventory().addItem(token);

        sender.sendMessage(ChatColor.GREEN + "Given " + amount + "x " +
                ChatColor.AQUA + tier.getNiceName() +
                ChatColor.GREEN + " to " +
                ChatColor.YELLOW + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " +
                ChatColor.AQUA + amount + "x " + tier.getNiceName() + ChatColor.GREEN + ".");

        return true;
    }

    private ItemStack createToken(TrainingTokenTier tier, int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();

        ChatColor nameColor;
        switch (tier) {
            case BASIC:
                nameColor = ChatColor.GRAY; // Common
                break;
            case INTERMEDIATE:
                nameColor = ChatColor.GREEN; // Uncommon
                break;
            case ADVANCED:
                nameColor = ChatColor.BLUE; // Rare
                break;
            case EXPERT:
                nameColor = ChatColor.DARK_PURPLE; // Epic
                break;
            case MASTER:
                nameColor = ChatColor.GOLD; // Legendary
                break;
            default:
                nameColor = ChatColor.WHITE;
        }

        meta.setDisplayName(nameColor + tier.getNiceName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "------------------------");
        lore.add(ChatColor.GRAY + "Right-click to start a");
        lore.add(ChatColor.GRAY + "special training session.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Rarity: " + rarityText(tier));
        lore.add(ChatColor.DARK_GRAY + "------------------------");
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tokenKey, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }

    private String rarityText(TrainingTokenTier tier) {
        switch (tier) {
            case BASIC:
                return ChatColor.GRAY + "Common";
            case INTERMEDIATE:
                return ChatColor.GREEN + "Uncommon";
            case ADVANCED:
                return ChatColor.BLUE + "Rare";
            case EXPERT:
                return ChatColor.DARK_PURPLE + "Epic";
            case MASTER:
                return ChatColor.GOLD + "Legendary";
            default:
                return ChatColor.GRAY + "Common";
        }
    }

    // --------------- Tab Complete ---------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("animesmp.admin")) {
            return out;
        }

        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                out.add("give");
            }
            return out;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }

        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (String tier : new String[]{"BASIC", "INTERMEDIATE", "ADVANCED", "EXPERT", "MASTER"}) {
                if (tier.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(tier);
                }
            }
        }

        return out;
    }

    public NamespacedKey getTokenKey() {
        return tokenKey;
    }
}
