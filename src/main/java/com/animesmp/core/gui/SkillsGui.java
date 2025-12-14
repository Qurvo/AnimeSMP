package com.animesmp.core.gui;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SkillsGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "[AnimeSMP] Skill Allocation";

    public static void open(AnimeSMPPlugin plugin, Player player) {
        PlayerProfileManager profiles = plugin.getProfileManager();
        PlayerProfile profile = profiles.getProfile(player);

        Inventory inv = Bukkit.createInventory(player, 27, TITLE);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        int skillPoints = profile.getSkillPoints();

        int maxCon = 20;
        int maxStr = 20;
        int maxTec = 20;
        int maxDex = 10;

        inv.setItem(10, createStatItem(
                Material.RED_DYE,
                ChatColor.RED + "" + ChatColor.BOLD + "Constitution (CON)",
                profile.getConPoints(),
                maxCon,
                skillPoints,
                "Increases max HP & ability resistance."));

        inv.setItem(12, createStatItem(
                Material.IRON_SWORD,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Strength (STR)",
                profile.getStrPoints(),
                maxStr,
                skillPoints,
                "Increases ability damage & ability knockback."));

        inv.setItem(14, createStatItem(
                Material.CLOCK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Technique (TEC)",
                profile.getTecPoints(),
                maxTec,
                skillPoints,
                "Reduces ability cooldowns & stamina costs."));

        inv.setItem(16, createStatItem(
                Material.FEATHER,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Dexterity (DEX)",
                profile.getDexPoints(),
                maxDex,
                skillPoints,
                "Boosts effectiveness of CON/STR/TEC."));

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Skill Points");
        iMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Unspent Skill Points: " + ChatColor.AQUA + skillPoints,
                "",
                ChatColor.YELLOW + "Left-click" + ChatColor.GRAY + " a stat to invest a point.",
                ChatColor.DARK_GRAY + "Caps: CON/STR/TEC = 20, DEX = 10."
        ));
        info.setItemMeta(iMeta);
        inv.setItem(22, info);

        player.openInventory(inv);
    }

    private static ItemStack createStatItem(Material material,
                                            String name,
                                            int value,
                                            int cap,
                                            int skillPoints,
                                            String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Current: " + ChatColor.AQUA + value + ChatColor.GRAY + " / " + ChatColor.AQUA + cap,
                ChatColor.GRAY + "Unspent points: " + ChatColor.AQUA + skillPoints,
                "",
                ChatColor.WHITE + description,
                "",
                ChatColor.YELLOW + "Left-click" + ChatColor.GRAY + " to add +1 point."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
