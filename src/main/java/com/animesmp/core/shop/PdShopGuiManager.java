package com.animesmp.core.shop;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityLoreUtil;
import com.animesmp.core.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;

public class PdShopGuiManager {

    private final AnimeSMPPlugin plugin;
    private static final String PD_MENU_TITLE = ChatColor.DARK_RED + "Perma-Death Vendor";

    public PdShopGuiManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        try {
            Inventory inv = Bukkit.createInventory(null, 54, PD_MENU_TITLE);

            List<Ability> stock = plugin.getPdStockManager().getCurrentStock();
            if (stock == null) stock = java.util.Collections.emptyList();

            int slot = 10;
            for (Ability ability : stock) {
                if (ability == null) continue;
                if (slot >= inv.getSize()) break;

                int cost = safeCost(ability);
                int remaining = safeRemaining(ability);

                ItemStack item = plugin.getAbilityManager().createAbilityScroll(ability, 1);

                try {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setLore(AbilityLoreUtil.pdLore(plugin, ability, cost, remaining));
                        item.setItemMeta(meta);
                    }
                } catch (Throwable t) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setLore(List.of(
                                ChatColor.GRAY + safeAbilitySummary(ability),
                                "",
                                ChatColor.GRAY + "Cost: " + ChatColor.RED + cost + ChatColor.GRAY + " PD Tokens",
                                ChatColor.GRAY + "Stock: " + ChatColor.WHITE + remaining
                        ));
                        item.setItemMeta(meta);
                    }
                }

                inv.setItem(slot, item);
                slot++;
                if ((slot + 1) % 9 == 0) slot += 2;
            }

            // Info item (includes next refresh)
            inv.setItem(49, buildInfoItem(player));
            player.openInventory(inv);

        } catch (Throwable t) {
            plugin.getLogger().severe("PdShopGuiManager.open() crashed for " + (player != null ? player.getName() : "null"));
            t.printStackTrace();
            if (player != null) player.sendMessage(ChatColor.RED + "PD Vendor is temporarily unavailable (error).");
        }
    }

    private ItemStack buildInfoItem(Player player) {
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName(ChatColor.GOLD + "PD Vendor Info");

            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            int tokens = profile == null ? 0 : profile.getPdTokens();
            String refresh = plugin.getPdStockManager().getFormattedTimeRemaining();

            im.setLore(List.of(
                    ChatColor.GRAY + "Stock is always " + ChatColor.WHITE + "4 Epics" + ChatColor.GRAY + " and " + ChatColor.WHITE + "4 Legendaries" + ChatColor.GRAY + ".",
                    ChatColor.GRAY + "Global stock per ability is " + ChatColor.WHITE + "2" + ChatColor.GRAY + ".",
                    "",
                    ChatColor.GRAY + "Next refresh in: " + ChatColor.WHITE + refresh,
                    "",
                    ChatColor.GRAY + "Your PD Tokens: " + ChatColor.WHITE + tokens
            ));
            info.setItemMeta(im);
        }
        return info;
    }

    public void handlePurchase(Player player, Ability ability) {
        try {
            if (player == null || ability == null) return;

            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile == null) return;

            int cost = safeCost(ability);
            if (safeRemaining(ability) <= 0) {
                player.sendMessage(ChatColor.RED + "This ability is out of stock.");
                return;
            }

            if (profile.getPdTokens() < cost) {
                player.sendMessage(ChatColor.RED + "You need " + cost + " PD Tokens.");
                return;
            }

            profile.setPdTokens(profile.getPdTokens() - cost);
            profile.unlockAbility(ability.getId());
            safeDecrement(ability);

            player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.AQUA + safeAbilityName(ability)
                    + ChatColor.GREEN + " for " + ChatColor.RED + cost + ChatColor.GREEN + " PD Tokens.");

            try {
                player.getInventory().addItem(plugin.getAbilityManager().createAbilityScroll(ability, 1));
            } catch (Throwable ignored) {
            }

            plugin.getProfileManager().saveProfile(profile);

        } catch (Throwable t) {
            plugin.getLogger().severe("PdShopGuiManager.handlePurchase() crashed for " + (player != null ? player.getName() : "null"));
            t.printStackTrace();
            if (player != null) player.sendMessage(ChatColor.RED + "Purchase failed due to an internal error.");
        }
    }

    // ------------------ SAFE WRAPPERS (keep your reflection compatibility) ------------------

    private int safeCost(Ability a) {
        try {
            return plugin.getPdStockManager().getCostFor(a);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int safeRemaining(Ability a) {
        try {
            return plugin.getPdStockManager().getRemaining(a);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void safeDecrement(Ability a) {
        try {
            plugin.getPdStockManager().decrementStock(a);
        } catch (Throwable ignored) {
        }
    }

    private String safeAbilityName(Ability a) {
        try {
            Method m = a.getClass().getMethod("getName");
            Object o = m.invoke(a);
            return o == null ? a.getId() : String.valueOf(o);
        } catch (Throwable ignored) {
            return a.getId();
        }
    }

    private String safeAbilitySummary(Ability a) {
        return safeAbilityName(a) + " (" + (a.getTier() != null ? a.getTier().name() : "UNKNOWN") + ")";
    }
}
