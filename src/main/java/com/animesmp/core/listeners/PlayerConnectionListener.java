package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.stats.StatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatsManager stats;

    public PlayerConnectionListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.stats = plugin.getStatsManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profiles.handleJoin(event.getPlayer());
        stats.recalculateStats(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.handleQuit(event.getPlayer());
    }
}
