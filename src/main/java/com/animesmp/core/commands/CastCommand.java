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

public class CastCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final AbilityRegistry registry;
    private final AbilityManager abilityManager;

    private static final String UNLOCK_BYPASS_PERM = "animesmp.unlock.bypass";

    public CastCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.registry = plugin.getAbilityRegistry();
        this.abilityManager = plugin.getAbilityManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        String name = command.getName().toLowerCase();

        int slot;
        switch (name) {
            case "cast1": slot = 1; break;
            case "cast2": slot = 2; break;
            case "cast3": slot = 3; break;
            case "cast4": slot = 4; break;
            case "cast5": slot = 5; break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown cast command.");
                return true;
        }

        PlayerProfile profile = profiles.getProfile(player);
        String abilityId = profile.getBoundAbilityId(slot);

        if (abilityId == null || abilityId.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No ability is bound to slot " + slot + ".");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/bind " + slot + " <abilityId>" +
                    ChatColor.GRAY + " to bind one.");
            return true;
        }

        Ability ability = registry.getAbility(abilityId);
        if (ability == null) {
            player.sendMessage(ChatColor.RED + "The ability bound to slot " + slot + " (" + abilityId + ") no longer exists.");
            player.sendMessage(ChatColor.GRAY + "Bind a new one with " + ChatColor.YELLOW + "/bind " + slot + " <abilityId>" + ChatColor.GRAY + ".");
            return true;
        }

        // Hard unlock enforcement at cast time too (admins do NOT bypass)
        if (!profile.hasUnlockedAbility(ability.getId()) && !player.hasPermission(UNLOCK_BYPASS_PERM)) {
            player.sendMessage(ChatColor.RED + "You have not unlocked " + ChatColor.YELLOW + ability.getDisplayName() + ChatColor.RED + " yet.");
            return true;
        }

        // Use the ability (handles cooldown, stamina, etc.)
        abilityManager.useAbility(player, ability);
        return true;
    }
}
