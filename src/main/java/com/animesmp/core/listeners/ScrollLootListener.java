package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

public class ScrollLootListener implements Listener {

    private final AnimeSMPPlugin plugin;

    public ScrollLootListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        plugin.getScrollLootManager().maybeAddScrolls(event);
    }
}
