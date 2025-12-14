package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public class ShopNpcListener implements Listener {

    private final AnimeSMPPlugin plugin;

    public ShopNpcListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!(entity instanceof LivingEntity)) return;

        LivingEntity npc = (LivingEntity) entity;

        if (!npc.getPersistentDataContainer().has(plugin.getShopNpcKey(), PersistentDataType.STRING)) {
            return;
        }

        String type = npc.getPersistentDataContainer().get(
                plugin.getShopNpcKey(),
                PersistentDataType.STRING
        );

        if (type == null) return;

        type = type.toUpperCase();

        switch (type) {

            // Ability Vendor
            case "ABILITY":
            case "ABILITY_VENDOR":
                plugin.getShopGuiManager().openRotatingAbilityVendor(player);
                break;

            // Training Vendor
            case "TRAINING":
            case "TRAINING_VENDOR":
                plugin.getShopGuiManager().openTrainingShop(player);
                break;

            // PD Vendor
            case "PD":
            case "PD_VENDOR":
            case "PERMA":
                plugin.getPdShopGuiManager().open(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown shop type: " + type);
        }
    }
}
