package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.tutorial.TutorialManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;

    public TutorialCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        Player p = (Player) sender;

        TutorialManager tm = plugin.getTutorialManager();
        if (tm == null) {
            p.sendMessage(ChatColor.RED + "Tutorial system is not loaded.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) {
            p.sendMessage(ChatColor.RED + "Tutorial is disabled in config.yml (tutorial.enabled=false).");
            return true;
        }

        // Force start (so /tutorial always does something)
        tm.start(p, true);
        p.sendMessage(ChatColor.GREEN + "Tutorial started.");
        return true;
    }
}
