package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AbilityAdminCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;
    private final PlayerProfileManager profiles;

    public AbilityAdminCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.profiles = plugin.getProfileManager();
    }

    // ----------------- Helpers -----------------

    private String humanize(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        raw = raw.replace('_', ' ');
        String[] parts = raw.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1).toLowerCase(Locale.ROOT))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    private String cleanDisplayName(Ability ability) {
        String name = ability.getDisplayName();
        if (name == null || name.isEmpty()) {
            // fallback to ID
            return humanize(ability.getId());
        }
        // Translate & codes, then strip all colors so we show a clean name
        String translated = ChatColor.translateAlternateColorCodes('&', name);
        String stripped = ChatColor.stripColor(translated);
        return stripped;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "======== " + ChatColor.YELLOW + "Ability Admin" + ChatColor.GOLD + " ========");
        sender.sendMessage(ChatColor.AQUA + "/abilityadmin list" + ChatColor.GRAY + " - List all abilities.");
        sender.sendMessage(ChatColor.AQUA + "/abilityadmin giveability <player> <id>" + ChatColor.GRAY + " - Unlock ability for player.");
        sender.sendMessage(ChatColor.AQUA + "/abilityadmin removeability <player> <id>" + ChatColor.GRAY + " - Remove unlocked ability.");
        sender.sendMessage(ChatColor.AQUA + "/abilityadmin inspect <player>" + ChatColor.GRAY + " - Show unlocked abilities.");
    }

    // ----------------- Command -----------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list":
                handleList(sender);
                return true;

            case "giveability":
                handleGiveAbility(sender, args);
                return true;

            case "removeability":
                handleRemoveAbility(sender, args);
                return true;

            case "inspect":
                handleInspect(sender, args);
                return true;

            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    // /abilityadmin list
    private void handleList(CommandSender sender) {
        List<Ability> abilities = new ArrayList<>(registry.getAllAbilities());
        abilities.sort(Comparator.comparing(Ability::getId));

        if (abilities.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No abilities are registered.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Registered Abilities" + ChatColor.GOLD + " =====");

        for (Ability ability : abilities) {
            String niceName = cleanDisplayName(ability);
            String typeName = humanize(ability.getType().name());
            String rarityColored = ability.getTier().getColoredRarityLabel();  // e.g. "§7Common", "§9Rare"

            sender.sendMessage(
                    ChatColor.YELLOW + "- " +
                            ChatColor.AQUA + niceName +
                            ChatColor.GRAY + " | Rarity: " +
                            rarityColored +
                            ChatColor.GRAY + " | Type: " +
                            ChatColor.GREEN + typeName +
                            ChatColor.DARK_GRAY + " (" + ability.getId().toLowerCase() + ")"
            );
        }
    }

    // /abilityadmin giveability <player> <id>
    private void handleGiveAbility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /abilityadmin giveability <player> <abilityId>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String id = args[2].toLowerCase(Locale.ROOT);
        Ability ab = registry.getAbility(id);
        if (ab == null) {
            sender.sendMessage(ChatColor.RED + "No ability with ID '" + id + "' exists.");
            return;
        }

        PlayerProfile profile = profiles.getProfile(target);
        if (profile.hasUnlockedAbility(id)) {
            sender.sendMessage(ChatColor.YELLOW + "Player already has ability: " + ChatColor.AQUA + cleanDisplayName(ab));
            return;
        }

        profile.unlockAbility(id);
        sender.sendMessage(ChatColor.GREEN + "Gave ability " +
                ChatColor.AQUA + cleanDisplayName(ab) +
                ChatColor.GREEN + " to " +
                ChatColor.YELLOW + target.getName());

        target.sendMessage(ChatColor.GREEN + "You have unlocked a new ability: " +
                ChatColor.AQUA + cleanDisplayName(ab));
    }

    // /abilityadmin removeability <player> <id>
    private void handleRemoveAbility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /abilityadmin removeability <player> <abilityId>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String id = args[2].toLowerCase(Locale.ROOT);
        Ability ab = registry.getAbility(id);
        if (ab == null) {
            sender.sendMessage(ChatColor.RED + "No ability with ID '" + id + "' exists.");
            return;
        }

        PlayerProfile profile = profiles.getProfile(target);
        if (!profile.hasUnlockedAbility(id)) {
            sender.sendMessage(ChatColor.YELLOW + "Player does not have that ability unlocked.");
            return;
        }

        profile.getUnlockedAbilities().remove(id.toLowerCase(Locale.ROOT));

        sender.sendMessage(ChatColor.GREEN + "Removed ability " +
                ChatColor.AQUA + cleanDisplayName(ab) +
                ChatColor.GREEN + " from " +
                ChatColor.YELLOW + target.getName());

        target.sendMessage(ChatColor.RED + "Ability removed: " +
                ChatColor.AQUA + cleanDisplayName(ab));
    }

    // /abilityadmin inspect <player>
    private void handleInspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /abilityadmin inspect <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        PlayerProfile profile = profiles.getProfile(target);

        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Abilities for " + target.getName() + ChatColor.GOLD + " =====");

        if (profile.getUnlockedAbilities().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No abilities unlocked.");
            return;
        }

        for (String id : profile.getUnlockedAbilities()) {
            Ability ab = registry.getAbility(id);
            if (ab == null) {
                sender.sendMessage(ChatColor.DARK_GRAY + "- " + id + " (missing in registry)");
                continue;
            }
            String niceName = cleanDisplayName(ab);
            String rarityColored = ab.getTier().getColoredRarityLabel();

            sender.sendMessage(ChatColor.YELLOW + "- " +
                    ChatColor.AQUA + niceName +
                    ChatColor.GRAY + " | Rarity: " +
                    rarityColored +
                    ChatColor.DARK_GRAY + " (" + ab.getId().toLowerCase() + ")");
        }
    }

    // ----------------- Tab Complete -----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (!sender.hasPermission("animesmp.admin")) {
            return out;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : new String[]{"list", "giveability", "removeability", "inspect", "help"}) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("giveability") || sub.equals("removeability") || sub.equals("inspect")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(p.getName());
                    }
                }
            }
            return out;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("giveability") || sub.equals("removeability")) {
                String prefix = args[2].toLowerCase(Locale.ROOT);
                for (Ability ability : registry.getAllAbilities()) {
                    String id = ability.getId().toLowerCase(Locale.ROOT);
                    if (id.startsWith(prefix)) {
                        out.add(id);
                    }
                }
            }
        }

        return out;
    }
}
