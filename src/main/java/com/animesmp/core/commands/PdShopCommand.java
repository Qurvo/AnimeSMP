package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PdShopCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;

    public PdShopCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the PD shop.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getPdShopGuiManager().open(player);
        return true;
    }
}
