package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.shop.PdShopGuiManager;
import com.animesmp.core.shop.ShopGuiManager;
import com.animesmp.core.shop.rotation.PdStockManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class VendorListener implements Listener {

    private final AnimeSMPPlugin plugin;

    private final ShopGuiManager shopGui;
    private final AbilityRegistry registry;
    private final PlayerProfileManager profiles;
    private final PdShopGuiManager pdGui;
    private final PdStockManager pdStock;

    private static final String ABILITY_MENU = ChatColor.DARK_PURPLE + "Daily Ability Vendor";
    private static final String TRAINING_MENU = ChatColor.GOLD + "Training Vendor";
    private static final String PD_MENU = ChatColor.DARK_RED + "Perma-Death Vendor";

    public VendorListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.shopGui = plugin.getShopGuiManager();
        this.registry = plugin.getAbilityRegistry();
        this.profiles = plugin.getProfileManager();
        this.pdGui = plugin.getPdShopGuiManager();
        this.pdStock = plugin.getPdStockManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null) return;

        // Prevent all dragging and moving in vendor menus
        if (title.equals(ABILITY_MENU) || title.equals(TRAINING_MENU) || title.equals(PD_MENU)) {
            e.setCancelled(true);
        } else {
            return; // not a vendor menu
        }

        // ================
        // ABILITY VENDOR
        // ================
        if (title.equals(ABILITY_MENU)) {
            handleAbilityVendorClick(player, clicked);
            return;
        }

        // ================
        // TRAINING VENDOR
        // ================
        if (title.equals(TRAINING_MENU)) {
            handleTrainingVendorClick(player, clicked);
            return;
        }

        // ================
        // PD VENDOR
        // ================
        if (title.equals(PD_MENU)) {
            handlePdVendorClick(player, clicked);
        }
    }

    // ================================================================
    // Ability Vendor
    // ================================================================
    private void handleAbilityVendorClick(Player player, ItemStack clicked) {
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Ability ability = registry.getAbilityByDisplay(display);
        if (ability == null) return;

        PlayerProfile profile = profiles.getProfile(player);

        // purchase limit (1 per day)
        if (profile.hasPurchasedToday(ability.getId())) {
            player.sendMessage(ChatColor.RED + "You already purchased " + ability.getDisplayName() + " today.");
            return;
        }

        int price = shopGui.getPriceForTier(ability.getTier());

        if (!plugin.getEconomyManager().trySpendYen(player, price)) {
            player.sendMessage(ChatColor.RED + "Not enough Yen.");
            return;
        }

        // give scroll
        player.getInventory().addItem(plugin.getAbilityManager().createAbilityScroll(ability, 1));
        player.sendMessage(ChatColor.GREEN + "Purchased " + ability.getDisplayName() +
                ChatColor.GREEN + " for " + ChatColor.AQUA + price + "¥");

        profile.markPurchased(ability.getId());
    }

    // ================================================================
    // Training Vendor (Progressive Unlock DYNAMIC - Option C)
    // ================================================================
    private void handleTrainingVendorClick(Player player, ItemStack clicked) {
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        PlayerProfile profile = profiles.getProfile(player);

        ShopGuiManager.ShopTokenOffer offer = shopGui.getTrainingOfferByDisplay(name);
        if (offer == null) return;

        int level = profile.getTrainingLevel();

        // Option C logic: can buy tokens up to their level bracket
        if (level < offer.tier.getMinLevel()) {
            player.sendMessage(ChatColor.RED + "Your training level is too low to buy this token.");
            return;
        }

        int price = offer.price;

        if (!plugin.getEconomyManager().trySpendYen(player, price)) {
            player.sendMessage(ChatColor.RED + "Not enough Yen.");
            return;
        }

        // Give the token
        player.getInventory().addItem(plugin.getTrainingManager().createToken(offer.tier, 1));
        player.sendMessage(ChatColor.GREEN + "Purchased " +
                ChatColor.GOLD + offer.display +
                ChatColor.GREEN + " for " + ChatColor.AQUA + price + "¥");
    }

    // ================================================================
    // PD Vendor
    // ================================================================
    private void handlePdVendorClick(Player player, ItemStack clicked) {
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        Ability ability = plugin.getAbilityManager().getAbilityFromScroll(clicked);
        if (ability == null) {
            String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            ability = registry.getAbilityByDisplay(display);
        }
        if (ability == null) return;

        pdGui.handlePurchase(player, ability);
    }


    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // nothing yet
    }
}
