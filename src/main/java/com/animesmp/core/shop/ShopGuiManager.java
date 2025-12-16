package com.animesmp.core.shop;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityLoreUtil;
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
        // 54 slots so we can do a clean layout + header
        Inventory inv = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_PURPLE + "Daily Ability Vendor"
        );

        // Header/info book with refresh timer
        inv.setItem(4, buildVendorInfoItem());

        // Optional border/filler
        fillBorder(inv);

        // Stock from rotation manager
        List<String> stockIds = plugin.getRotatingVendorManager().getCurrentRotation();

        // Place items in middle rows: 10-16, 19-25, 28-34 (up to 21 max)
        int[] slots = new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int idx = 0;
        for (String id : stockIds) {
            if (idx >= slots.length) break;

            Ability a = registry.getAbility(id);
            if (a == null) continue;

            // Vendor sells all except Legendary (PD) already filtered by rotation manager
            int price = getPriceForTier(a.getTier());
            ItemStack item = createAbilityItem(a, price);

            inv.setItem(slots[idx++], item);
        }

        player.openInventory(inv);
    }

    private ItemStack buildVendorInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta == null) return info;

        meta.setDisplayName(ChatColor.GOLD + "Daily Ability Vendor");

        String remaining = plugin.getRotatingVendorManager().getFormattedTimeRemaining();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Stock refreshes daily at " + ChatColor.AQUA + "08:00 EU");
        lore.add(ChatColor.GRAY + "Next refresh in: " + ChatColor.YELLOW + remaining);
        lore.add("");
        lore.add(ChatColor.GRAY + "Click an ability to purchase it.");
        lore.add(ChatColor.DARK_GRAY + "Limit: 1 per ability per day");

        meta.setLore(lore);
        info.setItemMeta(meta);
        return info;
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }

        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            // top row + bottom row + left/right edges
            boolean top = i <= 8;
            boolean bottom = i >= size - 9;
            boolean left = (i % 9) == 0;
            boolean right = (i % 9) == 8;

            if (top || bottom || left || right) {
                // don't overwrite the info book slot
                if (i == 4) continue;
                inv.setItem(i, pane);
            }
        }
    }

    private ItemStack createAbilityItem(Ability a, int price) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.AQUA + a.getDisplayName());
        meta.setLore(AbilityLoreUtil.vendorLore(plugin, a, price));

        item.setItemMeta(meta);
        return item;
    }

    public int getPriceForTier(AbilityTier tier) {
        // Ability Vendor sells Common/Rare/Epic (SCROLL/VENDOR/TRAINER)
        // Prices scale aggressively for rarer stock.
        if (tier == null) return 3000;

        return switch (tier) {
            case SCROLL -> 3000;    // Common
            case VENDOR -> 8000;    // Rare
            case TRAINER -> 18000;  // Epic
            case PD -> 999999;      // Not sold here
        };
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
            if (level >= offer.tier.getMinLevel()) {
                inv.setItem(slot, createTrainingTokenItem(offer));
                slot++;
                if (slot >= 17) break;
            }
        }

        if (slot == 10) {
            ItemStack info = new ItemStack(Material.BOOK);
            ItemMeta meta = info.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Training Vendor");
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "No tokens available for your current",
                        ChatColor.GRAY + "training level (" + level + ").",
                        ChatColor.YELLOW + "Level up to unlock more tokens."
                ));
                info.setItemMeta(meta);
            }
            inv.setItem(13, info);
        }

        player.openInventory(inv);
    }

    private ItemStack createTrainingTokenItem(ShopTokenOffer offer) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

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
