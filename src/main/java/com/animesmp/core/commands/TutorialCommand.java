package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
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
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        boolean force = args.length > 0 && args[0].equalsIgnoreCase("force");

        plugin.getTutorialManager().start(player, force);
        player.sendMessage(ChatColor.GREEN + "Tutorial started" + (force ? " (forced)" : "") + ".");

        return true;
    }
}
