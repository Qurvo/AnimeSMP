package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.player.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AbilitiesCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;

    public AbilitiesCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        AbilityRegistry registry = plugin.getAbilityRegistry();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        List<Ability> all = registry.getAllAbilities()
                .stream()
                .sorted(Comparator.comparing(a -> a.getType().name()))
                .collect(Collectors.toList());

        player.sendMessage(ChatColor.DARK_AQUA + "==== Your Abilities ====");

        for (Ability ab : all) {
            String id = ab.getId().toLowerCase();
            boolean unlocked = profile.hasUnlockedAbility(id);

            String cleanName = ChatColor.stripColor(ab.getDisplayName());
            if (cleanName == null || cleanName.isEmpty()) {
                cleanName = id;
            }

            String typeLabel = switch (ab.getType()) {
                case MOVEMENT -> "Movement";
                case DAMAGE_LIGHT -> "Damage (Light)";
                case DAMAGE_HEAVY -> "Damage (Heavy)";
                case DEFENSE -> "Defense";
                case UTILITY -> "Utility";
                case ULTIMATE -> "Ultimate";
            };

            String rarityLabel = ab.getTier().getColoredRarityLabel();

            ChatColor statusColor = unlocked ? ChatColor.GREEN : ChatColor.DARK_RED;
            String statusText = unlocked ? "Unlocked" : "Locked";

            String line = statusColor + (unlocked ? "✔" : "✖") + " "
                    + ChatColor.AQUA + cleanName
                    + ChatColor.GRAY + " ["
                    + rarityLabel + ChatColor.GRAY + " | "
                    + ChatColor.YELLOW + typeLabel + ChatColor.GRAY + "] "
                    + statusColor + "(" + statusText + ")";

            player.sendMessage(line);
        }

        return true;
    }
}
