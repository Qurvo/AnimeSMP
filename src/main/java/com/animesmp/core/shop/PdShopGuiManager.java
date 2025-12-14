package com.animesmp.core.shop;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityTier;
import com.animesmp.core.shop.rotation.PdStockManager;
import com.animesmp.core.economy.EconomyManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PdShopGuiManager {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;
    private final PdStockManager stockManager;
    private final EconomyManager econ;

    private static final String TITLE = ChatColor.DARK_RED + "Perma-Death Vendor";

    private static final int PRICE = 8; // 8 PD tokens per PD ability

    public PdShopGuiManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.stockManager = plugin.getPdStockManager();
        this.econ = plugin.getEconomyManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "PD Vendor");
        im.setLore(Arrays.asList(
                ChatColor.YELLOW + "Purchase Legendary (PD) Abilities.",
                "",
                ChatColor.GRAY + "Currency: " + ChatColor.RED + "PD Tokens",
                ChatColor.GRAY + "Price: " + ChatColor.RED + PRICE + " tokens each",
                "",
                ChatColor.GRAY + "Global stock:",
                ChatColor.GRAY + "3 PER ability per day",
                ChatColor.GRAY + "Resets at 08:00 EU"
        ));
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Fetch all PD-tier abilities
        List<Ability> pdAbilities = new ArrayList<>();
        for (Ability a : registry.getAllAbilities()) {
            if (a != null && a.getTier() == AbilityTier.PD) {
                pdAbilities.add(a);
            }
        }

        int slot = 10;
        for (Ability ability : pdAbilities) {
            int remaining = stockManager.getStock(ability.getId());

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + ability.getDisplayName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Legendary PD Ability",
                    ChatColor.GRAY + "Price: " + ChatColor.RED + PRICE + " PD Tokens",
                    "",
                    ChatColor.YELLOW + "Stock remaining: " + ChatColor.AQUA + remaining,
                    "",
                    ChatColor.GREEN + "Click to purchase"
            ));
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
            if (slot == 17) break; // only show up to 7 PD abilities cleanly
        }

        // Close button
        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta em = exit.getItemMeta();
        em.setDisplayName(ChatColor.RED + "Close");
        exit.setItemMeta(em);
        inv.setItem(26, exit);

        player.openInventory(inv);
    }

    public void handlePurchase(Player player, Ability ability) {
        if (ability == null) return;

        String id = ability.getId();
        int stock = stockManager.getStock(id);

        if (stock <= 0) {
            player.sendMessage(ChatColor.RED + "That ability is sold out for today!");
            return;
        }

        if (!econ.trySpendPdTokens(player, PRICE)) {
            player.sendMessage(ChatColor.RED + "You do not have enough PD Tokens.");
            return;
        }

        // Grant scroll
        ItemStack scroll = plugin.getAbilityManager().createAbilityScroll(ability, 1);
        player.getInventory().addItem(scroll);

        // Reduce stock
        stockManager.decreaseStock(id);

        player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.LIGHT_PURPLE + ability.getDisplayName()
                + ChatColor.GREEN + " for " + ChatColor.RED + PRICE + " PD Tokens.");
    }
}
