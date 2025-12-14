package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.gui.SkillsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;

    public SkillsCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        SkillsGui.open(plugin, player);
        return true;
    }
}
