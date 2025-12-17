package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.stats.StatsManager;
import com.animesmp.core.tutorial.TutorialManager;
import org.bukkit.entity.Player;
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
        Player p = event.getPlayer();

        profiles.handleJoin(p);
        stats.recalculateStats(p);

        if (plugin.getUltimateBarManager() != null) {
            plugin.getUltimateBarManager().handleJoin(p);
        }

        // IMPORTANT: tutorial auto-start hook
        TutorialManager tm = plugin.getTutorialManager();
        if (tm != null) {
            tm.handleJoin(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        profiles.handleQuit(p);

        if (plugin.getUltimateBarManager() != null) {
            plugin.getUltimateBarManager().handleQuit(p);
        }

        // clean up tutorial tasks/bars
        TutorialManager tm = plugin.getTutorialManager();
        if (tm != null) {
            tm.handleQuit(p);
        }
    }
}
