package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class YenCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;

    public YenCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        // /yen
        if (args.length == 0) {
            PlayerProfile profile = profiles.getProfile(player);
            player.sendMessage(ChatColor.GOLD + "Your Yen: " + ChatColor.YELLOW + profile.getYen());
            return true;
        }

        // /yen give <player> <amount>
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            int amount;
            try { amount = Integer.parseInt(args[2]); }
            catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }

            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return true;
            }

            PlayerProfile pFrom = profiles.getProfile(player);
            PlayerProfile pTo = profiles.getProfile(target);

            if (pFrom.getYen() < amount) {
                player.sendMessage(ChatColor.RED + "You do not have enough yen.");
                return true;
            }

            pFrom.setYen(pFrom.getYen() - amount);
            pTo.setYen(pTo.getYen() + amount);

            player.sendMessage(ChatColor.GREEN + "You sent " + ChatColor.YELLOW + amount +
                    ChatColor.GREEN + " yen to " + ChatColor.AQUA + target.getName());

            target.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.GREEN +
                    " sent you " + ChatColor.YELLOW + amount + ChatColor.GREEN + " yen!");

            profiles.saveProfile(pFrom);
            profiles.saveProfile(pTo);

            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /yen OR /yen give <player> <amount>");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> list = new java.util.ArrayList<>();

        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase())) list.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
        }

        return list;
    }
}
