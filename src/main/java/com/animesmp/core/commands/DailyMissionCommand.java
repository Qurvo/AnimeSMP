package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.daily.DailyRewardManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DailyMissionCommand implements CommandExecutor, TabCompleter {

    private final DailyRewardManager daily;

    public DailyMissionCommand(AnimeSMPPlugin plugin) {
        this.daily = plugin.getDailyRewardManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Daily Missions" + ChatColor.GOLD + " =====");
        for (String line : daily.buildMissionLines(player)) {
            player.sendMessage(line);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
