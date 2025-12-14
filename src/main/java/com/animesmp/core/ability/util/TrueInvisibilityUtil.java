package com.animesmp.core.ability.util;

import com.animesmp.core.AnimeSMPPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_EQUIPMENT;

/**
 * Uses ProtocolLib to hide armor and hand items client-side while invisibility is active.
 *
 * If ProtocolLib is not present, callers should fall back to normal potion invisibility.
 */
public final class TrueInvisibilityUtil {

    private TrueInvisibilityUtil() {
    }

    public static boolean isProtocolLibPresent() {
        Plugin pl = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        return pl != null && pl.isEnabled();
    }

    public static void applyTrueInvis(Player p, int durationTicks) {
        if (p == null || durationTicks <= 0) return;
        if (!isProtocolLibPresent()) return;

        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        int entityId = p.getEntityId();

        // Hide now
        sendEquipment(manager, p, entityId, true);

        // Restore later
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                sendEquipment(manager, p, entityId, false);
            }
        }.runTaskLater(plugin, durationTicks);
    }

    private static void sendEquipment(ProtocolManager manager, Player owner, int entityId, boolean hide) {
        PacketContainer packet = manager.createPacket(ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, entityId);

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = new ArrayList<>();
        if (hide) {
            ItemStack air = new ItemStack(Material.AIR);
            items.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, air));
            items.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, air));
            items.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, air));
            items.add(new Pair<>(EnumWrappers.ItemSlot.FEET, air));
            items.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, air));
            items.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, air));
        } else {
            ItemStack[] armor = owner.getInventory().getArmorContents();
            // Bukkit order: boots, leggings, chestplate, helmet
            ItemStack boots = armor.length > 0 ? armor[0] : null;
            ItemStack legs = armor.length > 1 ? armor[1] : null;
            ItemStack chest = armor.length > 2 ? armor[2] : null;
            ItemStack head = armor.length > 3 ? armor[3] : null;

            items.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, head));
            items.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, chest));
            items.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, legs));
            items.add(new Pair<>(EnumWrappers.ItemSlot.FEET, boots));
            items.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, owner.getInventory().getItemInMainHand()));
            items.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, owner.getInventory().getItemInOffHand()));
        }

        packet.getSlotStackPairLists().write(0, items);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(owner)) continue;
            try {
                manager.sendServerPacket(viewer, packet);
            } catch (Exception ignored) {
                // ProtocolLib versions differ in thrown exceptions; ignore send failures safely.
            }
        }
    }
}
