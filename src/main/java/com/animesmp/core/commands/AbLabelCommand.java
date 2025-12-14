package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /ablabel <slot 1-5> <text|off>
 *
 * Lets players replace the HUD icon placeholder with a small custom label (max 3 chars).
 */
public class AbLabelCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;

    public AbLabelCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage(ChatColor.GOLD + "---- Action Bar Labels ----");
            for (int s = 1; s <= 5; s++) {
                String v = profile.getActionBarLabel(s);
                player.sendMessage(ChatColor.YELLOW + "Slot " + s + ": " + (v == null ? ChatColor.DARK_GRAY + "(default)" : ChatColor.AQUA + v));
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <slot 1-5> <text|off>");
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Slot must be a number 1-5.");
            return true;
        }
        if (slot < 1 || slot > 5) {
            player.sendMessage(ChatColor.RED + "Slot must be between 1 and 5.");
            return true;
        }

        String raw = args[1];
        if (raw.equalsIgnoreCase("off") || raw.equalsIgnoreCase("clear")) {
            profile.setActionBarLabel(slot, null);
            player.sendMessage(ChatColor.YELLOW + "Cleared action bar label for slot " + slot + ".");
            return true;
        }

        // Sanitize: strip color codes and enforce 3 chars
        String cleaned = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', raw));
        if (cleaned == null) cleaned = "";
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) {
            profile.setActionBarLabel(slot, null);
            player.sendMessage(ChatColor.YELLOW + "Cleared action bar label for slot " + slot + ".");
            return true;
        }
        if (cleaned.length() > 3) cleaned = cleaned.substring(0, 3);

        profile.setActionBarLabel(slot, cleaned);
        player.sendMessage(ChatColor.GREEN + "Set slot " + ChatColor.YELLOW + slot + ChatColor.GREEN + " label to " + ChatColor.AQUA + cleaned + ChatColor.GREEN + ".");
        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "---- Action Bar Labels ----");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " <slot 1-5> <text>" + ChatColor.GRAY + " - Set a 3-char label.");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " <slot 1-5> off" + ChatColor.GRAY + " - Clear a label.");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - View your labels.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player)) return out;

        if (args.length == 1) {
            String p = args[0].toLowerCase();
            if ("list".startsWith(p)) out.add("list");
            if ("help".startsWith(p)) out.add("help");
            for (int i = 1; i <= 5; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(p)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String p = args[1].toLowerCase();
            if ("off".startsWith(p)) out.add("off");
            if ("clear".startsWith(p)) out.add("clear");
        }
        return out;
    }
}
