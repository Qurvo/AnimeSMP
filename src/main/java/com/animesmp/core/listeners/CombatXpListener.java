package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.level.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CombatXpListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final LevelManager levelManager;

    public CombatXpListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getLevelManager();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        EntityType type = event.getEntityType();
        int xp = getXpForEntity(type);

        if (xp <= 0) return;

        levelManager.giveXp(killer, xp);

        // Show XP gain in chat instead of action bar
        killer.sendMessage(ChatColor.GREEN + "+" + xp + " XP");
    }

    private int getXpForEntity(EntityType type) {
        switch (type) {
            // Basic hostile mobs
            case ZOMBIE:
            case SKELETON:
            case SPIDER:
            case CAVE_SPIDER:
            case HUSK:
            case STRAY:
            case DROWNED:
            case SLIME:
            case PHANTOM:
                return 8;

            case CREEPER:
            case ENDERMAN:
            case WITCH:
            case BLAZE:
            case SILVERFISH:
            case VEX:
            case GUARDIAN:
                return 12;

            // Nether / stronger mobs
            case WITHER_SKELETON:
            case PIGLIN_BRUTE:
            case HOGLIN:
            case ZOGLIN:
                return 16;

            // Bosses / mini-bosses
            case WITHER:
                return 250;
            case ENDER_DRAGON:
                return 400;

            // Players - PvP bonus
            case PLAYER:
                return 40;

            // Passive mobs (small XP)
            case COW:
            case SHEEP:
            case PIG:
            case CHICKEN:
            case RABBIT:
            case COD:
            case SALMON:
            case TROPICAL_FISH:
            case SQUID:
            case GLOW_SQUID:
                return 3;

            default:
                return 0;
        }
    }
}
