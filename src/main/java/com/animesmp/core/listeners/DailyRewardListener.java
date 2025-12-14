package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.daily.DailyRewardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles daily login rewards via DailyRewardManager.
 */
public class DailyRewardListener implements Listener {

    private final DailyRewardManager daily;

    public DailyRewardListener(AnimeSMPPlugin plugin) {
        this.daily = plugin.getDailyRewardManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        daily.handleLogin(player);
    }
}
