package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.gui.SkillsGui;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.stats.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class SkillsGuiListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatsManager statsManager;

    private static final int SLOT_CON = 10;
    private static final int SLOT_STR = 12;
    private static final int SLOT_TEC = 14;
    private static final int SLOT_DEX = 16;

    private static final int MAX_CON = 20;
    private static final int MAX_STR = 20;
    private static final int MAX_TEC = 20;
    private static final int MAX_DEX = 10;

    public SkillsGuiListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.statsManager = plugin.getStatsManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        HumanEntity clicker = event.getWhoClicked();

        if (!(clicker instanceof Player)) {
            return;
        }
        Player player = (Player) clicker;

        if (!event.getView().getTitle().equals(SkillsGui.TITLE)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) {
            return;
        }

        PlayerProfile profile = profiles.getProfile(player);
        int skillPoints = profile.getSkillPoints();

        if (skillPoints <= 0) {
            player.sendMessage(ChatColor.RED + "You have no unspent skill points.");
            return;
        }

        boolean changed = false;

        if (slot == SLOT_CON) {
            if (profile.getConPoints() >= MAX_CON) {
                player.sendMessage(ChatColor.RED + "Constitution is already at its cap (" + MAX_CON + ").");
            } else {
                profile.setConPoints(profile.getConPoints() + 1);
                changed = true;
            }
        } else if (slot == SLOT_STR) {
            if (profile.getStrPoints() >= MAX_STR) {
                player.sendMessage(ChatColor.RED + "Strength is already at its cap (" + MAX_STR + ").");
            } else {
                profile.setStrPoints(profile.getStrPoints() + 1);
                changed = true;
            }
        } else if (slot == SLOT_TEC) {
            if (profile.getTecPoints() >= MAX_TEC) {
                player.sendMessage(ChatColor.RED + "Technique is already at its cap (" + MAX_TEC + ").");
            } else {
                profile.setTecPoints(profile.getTecPoints() + 1);
                changed = true;
            }
        } else if (slot == SLOT_DEX) {
            if (profile.getDexPoints() >= MAX_DEX) {
                player.sendMessage(ChatColor.RED + "Dexterity is already at its cap (" + MAX_DEX + ").");
            } else {
                profile.setDexPoints(profile.getDexPoints() + 1);
                changed = true;
            }
        }

        if (changed) {
            profile.setSkillPoints(profile.getSkillPoints() - 1);
            statsManager.recalculateStats(player);
            player.sendMessage(ChatColor.GREEN + "Stat point allocated.");
            SkillsGui.open(plugin, player);
        }
    }
}
