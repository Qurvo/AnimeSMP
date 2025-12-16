package com.animesmp.core.tutorial;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class TutorialListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final TutorialManager tutorial;

    public TutorialListener(AnimeSMPPlugin plugin, TutorialManager tutorial) {
        this.plugin = plugin;
        this.tutorial = tutorial;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        tutorial.handleJoin(p);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage(); // e.g. "/skills", "/skills something"
        tutorial.handleCommandStep(p, msg);

        // Also detect bind/cast usage regardless of step (manager will decide)
        String lower = msg.toLowerCase();
        if (lower.startsWith("/bind") || lower.startsWith("/cast1") || lower.startsWith("/cast2")
                || lower.startsWith("/cast3") || lower.startsWith("/cast4") || lower.startsWith("/cast5")) {
            tutorial.handleBindOrCastUsed(p);
        }
    }
}
