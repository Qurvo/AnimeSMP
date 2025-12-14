package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CycleCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final AbilityRegistry registry;
    private final AbilityManager abilityManager;

    public CycleCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.registry = plugin.getAbilityRegistry();
        this.abilityManager = plugin.getAbilityManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        String name = command.getName().toLowerCase();
        PlayerProfile profile = profiles.getProfile(player);

        switch (name) {
            case "cyclenext":
                cycle(player, profile, true);
                break;
            case "cycleprevious":
                cycle(player, profile, false);
                break;
            case "castselected":
                castSelected(player, profile);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown cycle command.");
                break;
        }

        return true;
    }

    private void cycle(Player player, PlayerProfile profile, boolean forward) {
        // Check if any bound abilities exist at all
        boolean anyBound = false;
        for (int i = 1; i <= 5; i++) {
            if (profile.getBoundAbilityId(i) != null) {
                anyBound = true;
                break;
            }
        }
        if (!anyBound) {
            player.sendMessage(ChatColor.RED + "You have no abilities bound. Use " +
                    ChatColor.YELLOW + "/bind <slot> <abilityId>" + ChatColor.RED + " first.");
            return;
        }

        int current = profile.getSelectedSlot();
        int loops = 0;

        while (loops < 5) {
            current = forward ? (current % 5) + 1 : (current == 1 ? 5 : current - 1);
            String id = profile.getBoundAbilityId(current);
            if (id != null) {
                profile.setSelectedSlot(current);
                Ability ability = registry.getAbility(id);
                String name = (ability != null) ? ability.getDisplayName() : id;

                player.sendMessage(ChatColor.AQUA + "Selected Slot " + current + ": " +
                        ChatColor.YELLOW + name + ChatColor.GRAY + " (" + id + ")");

                // Later we’ll put this on the action bar instead of chat
                return;
            }
            loops++;
        }

        player.sendMessage(ChatColor.RED + "Could not find any valid bound abilities to cycle to.");
    }

    private void castSelected(Player player, PlayerProfile profile) {
        int slot = profile.getSelectedSlot();
        String abilityId = profile.getBoundAbilityId(slot);

        if (abilityId == null || abilityId.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No ability is bound to your selected slot (" + slot + ").");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/bind " + slot + " <abilityId>" +
                    ChatColor.GRAY + " to bind one.");
            return;
        }

        Ability ability = registry.getAbility(abilityId);
        if (ability == null) {
            player.sendMessage(ChatColor.RED + "The ability bound to slot " + slot + " (" + abilityId + ") no longer exists.");
            player.sendMessage(ChatColor.GRAY + "Bind a new one with " +
                    ChatColor.YELLOW + "/bind " + slot + " <abilityId>" + ChatColor.GRAY + ".");
            return;
        }

        abilityManager.useAbility(player, ability);
    }
}
