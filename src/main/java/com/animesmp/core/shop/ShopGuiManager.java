package com.animesmp.core.shop;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityTier;
import com.animesmp.core.economy.EconomyManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.training.TrainingManager;
import com.animesmp.core.training.TrainingTokenTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopGuiManager {

    private final AnimeSMPPlugin plugin;
    private final EconomyManager econ;
    private final AbilityRegistry registry;
    private final AbilityManager abilityManager;
    private final TrainingManager trainingManager;
    private final PlayerProfileManager profiles;

    private final NamespacedKey offerKey;

    public ShopGuiManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.econ = plugin.getEconomyManager();
        this.registry = plugin.getAbilityRegistry();
        this.abilityManager = plugin.getAbilityManager();
        this.trainingManager = plugin.getTrainingManager();
        this.profiles = plugin.getProfileManager();
        this.offerKey = new NamespacedKey(plugin, "shop_offer_id");
    }

    // ================================================================
    //  ABILITY VENDOR GUI (Daily Rotation handled by RotatingVendorManager)
    // ================================================================
    public void openRotatingAbilityVendor(Player player) {
        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_PURPLE + "Daily Ability Vendor"
        );

        // Use the actual RotatingVendorManager API: getCurrentRotation()
        List<String> stockIds = plugin.getRotatingVendorManager().getCurrentRotation();

        int slot = 0;
        for (String id : stockIds) {
            Ability a = registry.getAbility(id);
            if (a == null) continue;
            ItemStack item = createAbilityItem(a);
            inv.setItem(slot++, item);
            if (slot >= 27) break;
        }

        player.openInventory(inv);
    }

    private ItemStack createAbilityItem(Ability a) {
        int price = getPriceForTier(a.getTier());

        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + a.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Rarity: " + a.getTier().name(),
                ChatColor.YELLOW + "Price: " + price + "¥",
                "",
                ChatColor.GREEN + "Click to purchase"
        ));
        item.setItemMeta(meta);

        return item;
    }

    public int getPriceForTier(AbilityTier tier) {
        if (tier == null) {
            return 2000;
        }

        String name = tier.name().toUpperCase();

        if (name.equals("SCROLL")) {
            return 2000;
        }
        if (name.equals("VENDOR")) {
            return 2500;
        }
        if (name.equals("TRAINER")) {
            return 5000;
        }
        if (name.equals("PD")) {
            return 12000;
        }

        // fallback for anything else (including missing ultimates, etc.)
        return 2000;
    }



    // ================================================================
    // TRAINING VENDOR GUI  — Option C (Progressive Unlock)
    // ================================================================
    public static class ShopTokenOffer {
        public final TrainingTokenTier tier;
        public final int price;
        public final String display;

        public ShopTokenOffer(TrainingTokenTier tier, int price, String display) {
            this.tier = tier;
            this.price = price;
            this.display = display;
        }
    }

    private final List<ShopTokenOffer> tokenOffers = Arrays.asList(
            new ShopTokenOffer(TrainingTokenTier.BASIC, 500, ChatColor.GOLD + "Basic Training Token"),
            new ShopTokenOffer(TrainingTokenTier.ADVANCED, 1000, ChatColor.GOLD + "Advanced Training Token"),
            new ShopTokenOffer(TrainingTokenTier.ELITE, 1500, ChatColor.GOLD + "Elite Training Token"),
            new ShopTokenOffer(TrainingTokenTier.MASTER, 2000, ChatColor.GOLD + "Master Training Token"),
            new ShopTokenOffer(TrainingTokenTier.LEGENDARY, 2500, ChatColor.GOLD + "Legendary Training Token")
    );

    // Old name kept for compatibility with listeners that call openTrainingShop
    public void openTrainingShop(Player player) {
        openTrainingVendor(player);
    }

    public void openTrainingVendor(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int level = profile.getTrainingLevel();

        Inventory inv = Bukkit.createInventory(
                null,
                27,
                ChatColor.GOLD + "Training Vendor"
        );

        int slot = 10;

        for (ShopTokenOffer offer : tokenOffers) {
            // Option C — can buy tokens UP TO your current level range
            if (level >= offer.tier.getMinLevel()) {
                inv.setItem(slot, createTrainingTokenItem(offer));
                slot++;
                if (slot >= 17) break;
            }
        }

        // If nothing available, show info
        if (slot == 10) {
            ItemStack info = new ItemStack(Material.BOOK);
            ItemMeta meta = info.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Training Vendor");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No tokens available for your current",
                    ChatColor.GRAY + "training level (" + level + ").",
                    ChatColor.YELLOW + "Level up to unlock more tokens."
            ));
            info.setItemMeta(meta);
            inv.setItem(13, info);
        }

        player.openInventory(inv);
    }

    private ItemStack createTrainingTokenItem(ShopTokenOffer offer) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(offer.display);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Usable for Training Level:");
        lore.add(ChatColor.AQUA.toString() + offer.tier.getMinLevel()
                + ChatColor.GRAY + " - "
                + ChatColor.AQUA.toString() + offer.tier.getMaxLevel());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Price: " + offer.price + "¥");
        lore.add(ChatColor.GREEN + "Click to purchase");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    // ================================================================
    // Unified Vendor Helper: Get Training Token Offer by Display Name
    // ================================================================
    public ShopTokenOffer getTrainingOfferByDisplay(String display) {
        if (display == null) return null;
        String stripped = ChatColor.stripColor(display);
        for (ShopTokenOffer offer : tokenOffers) {
            if (ChatColor.stripColor(offer.display).equalsIgnoreCase(stripped)) {
                return offer;
            }
        }
        return null;
    }
}
