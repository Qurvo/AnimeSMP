package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ShopNpcListener implements Listener {

    private final AnimeSMPPlugin plugin;

    private static final String ABILITY_MENU = ChatColor.DARK_PURPLE + "Daily Ability Vendor";

    public ShopNpcListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        Entity npc = e.getRightClicked();
        if (player == null || npc == null) return;

        NamespacedKey key = plugin.getShopNpcKey();
        if (key == null) return;

        PersistentDataContainer pdc = npc.getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null) return;

        String type = raw.trim().toUpperCase();

        // Cancel default interaction (villager trading etc.)
        e.setCancelled(true);

        boolean isAbilityVendor =
                type.equals("ABILITY") ||
                        type.equals("VENDOR") ||
                        type.equals("DAILY") ||
                        type.equals("ABILITY_VENDOR") ||
                        type.contains("ABILITY");

        boolean isPdVendor =
                type.equals("PD") ||
                        type.equals("PD_VENDOR") ||
                        type.equals("PERMADEATH") ||
                        type.equals("PERMA-DEATH") ||
                        type.contains("PD") ||
                        type.contains("DEATH");

        if (isAbilityVendor) {
            openDailyAbilityVendorDirect(player);
            plugin.getTutorialManager().handleNpcInteraction(player, "ABILITY_VENDOR");
            return;
        }

        if (isPdVendor) {
            // HARD SAFETY: PD vendor should never crash the server.
            try {
                plugin.getPdShopGuiManager().open(player);
                plugin.getTutorialManager().handleNpcInteraction(player, "PD_VENDOR");
            } catch (Throwable t) {
                plugin.getLogger().severe("PD vendor GUI crashed on open() for " + player.getName());
                t.printStackTrace();
                player.sendMessage(ChatColor.RED + "PD Vendor is temporarily unavailable (error). Check console.");
            }
            return;
        }

        player.sendMessage(ChatColor.RED + "Unknown SHOP NPC type: " + raw);
    }

    /**
     * Opens the Daily Ability Vendor GUI without depending on ShopGuiManager method names.
     * Uses RotatingVendorManager#getCurrentRotation() which your ShopGuiManager already expects.
     */
    private void openDailyAbilityVendorDirect(Player player) {
        List<String> ids = plugin.getRotatingVendorManager().getCurrentRotation();
        Inventory inv = Bukkit.createInventory(null, 54, ABILITY_MENU);

        int slot = 10;
        for (String id : ids) {
            Ability a = plugin.getAbilityRegistry().getAbility(id);
            if (a == null) continue;

            int price = plugin.getShopGuiManager().getPriceForTier(a.getTier());

            ItemStack scroll = plugin.getAbilityManager().createAbilityScroll(a, 1);
            // Add minimal vendor lore line (your AbilityLoreUtil can override in your scroll builder if needed)
            try {
                var meta = scroll.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore == null) lore = new java.util.ArrayList<>();
                    lore.add("");
                    lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + price + "¥");
                    lore.add(ChatColor.DARK_GRAY + "Resets in: " + ChatColor.WHITE + plugin.getRotatingVendorManager().getFormattedTimeRemaining());
                    meta.setLore(lore);
                    scroll.setItemMeta(meta);
                }
            } catch (Throwable ignored) {}

            inv.setItem(slot, scroll);

            slot++;
            if ((slot + 1) % 9 == 0) slot += 2; // keep spacing similar to your other menus
            if (slot >= inv.getSize()) break;
        }

        player.openInventory(inv);
    }
}
