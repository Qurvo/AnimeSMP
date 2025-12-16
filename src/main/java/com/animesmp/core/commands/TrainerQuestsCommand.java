package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TrainerQuestsCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;

    public TrainerQuestsCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player p = (Player) sender;
        PlayerProfile profile = plugin.getProfileManager().getProfile(p);

        // 1) Try to read active quest from PlayerProfile first (most reliable in your codebase)
        String active = readActiveQuestFromProfile(profile);

        // 2) Fallback: try quest manager getters if profile doesn't have it
        if (active == null || active.isEmpty()) {
            active = readActiveQuestFromManager(p.getUniqueId());
        }

        if (active == null || active.isEmpty()) {
            p.sendMessage(ChatColor.GRAY + "No active quests.");
            return true;
        }

        p.sendMessage(ChatColor.GOLD + "Active Quest:");
        p.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + active);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    private String readActiveQuestFromProfile(PlayerProfile profile) {
        if (profile == null) return null;

        // Try common field getters that exist across your versions
        for (String mName : new String[]{
                "getActiveTrainerQuest",
                "getActiveQuest",
                "getCurrentQuest",
                "getTrainerQuest",
                "getActiveTrainerQuestId",
                "getActiveQuestId"
        }) {
            try {
                Method m = profile.getClass().getMethod(mName);
                Object r = m.invoke(profile);
                if (r != null) return r.toString();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private String readActiveQuestFromManager(UUID uuid) {
        Object tqm = plugin.getTrainerQuestManager();
        if (tqm == null) return null;

        for (String mName : new String[]{
                "getActiveQuest",
                "getCurrentQuest",
                "getActiveQuestFor",
                "getQuestFor",
                "getPlayerActiveQuest",
                "getActiveTrainerQuest"
        }) {
            // UUID signature
            try {
                Method m = tqm.getClass().getMethod(mName, UUID.class);
                Object r = m.invoke(tqm, uuid);
                if (r != null) return r.toString();
            } catch (Throwable ignored) {}

            // Player signature (some managers use Player)
            try {
                Method m = tqm.getClass().getMethod(mName, org.bukkit.entity.Player.class);
                // can't call without player here; handled in profile path
            } catch (Throwable ignored) {}
        }

        return null;
    }
}
