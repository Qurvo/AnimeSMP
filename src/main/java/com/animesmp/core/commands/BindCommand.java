package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityType;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BindCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;

    // Only THIS permission bypasses type-lock
    private static final String TYPELOCK_BYPASS_PERM = "animesmp.bind.bypass";

    // Only THIS permission bypasses "must be unlocked"
    private static final String UNLOCK_BYPASS_PERM = "animesmp.unlock.bypass";

    public BindCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        PlayerProfileManager ppm = plugin.getProfileManager();
        AbilityRegistry registry = plugin.getAbilityRegistry();
        PlayerProfile profile = ppm.getProfile(player);

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        // /bind list
        if (args[0].equalsIgnoreCase("list")) {
            sendBindingsList(player, profile);
            return true;
        }

        // /bind clear <slot>
        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " clear <slot 1-5>");
                return true;
            }
            int slot;
            try {
                slot = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Slot must be a number 1-5.");
                return true;
            }

            if (slot < 1 || slot > 5) {
                player.sendMessage(ChatColor.RED + "Slot must be between 1 and 5.");
                return true;
            }

            profile.setBoundAbilityId(slot, null);
            player.sendMessage(ChatColor.YELLOW + "Cleared ability from slot " + slot + ".");
            return true;
        }

        // /bind <slot> <abilityId>
        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "First argument must be a slot number (1-5), or 'list'/'clear'.");
            return true;
        }

        if (slot < 1 || slot > 5) {
            player.sendMessage(ChatColor.RED + "Slot must be between 1 and 5.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <slot 1-5> <abilityId>");
            return true;
        }

        String abilityId = args[1].toLowerCase(Locale.ROOT);
        Ability ability = registry.getAbility(abilityId);
        if (ability == null) {
            player.sendMessage(ChatColor.RED + "Unknown ability: " + ChatColor.YELLOW + abilityId);
            return true;
        }

        // UNLOCK ENFORCEMENT (admins do NOT bypass anymore)
        if (!profile.hasUnlockedAbility(abilityId) && !player.hasPermission(UNLOCK_BYPASS_PERM)) {
            player.sendMessage(ChatColor.RED + "You have not unlocked " +
                    ChatColor.YELLOW + ability.getDisplayName() + ChatColor.RED + " yet.");
            return true;
        }

        // TYPE-LOCK ENFORCEMENT
        if (!player.hasPermission(TYPELOCK_BYPASS_PERM)) {
            String denyReason = validateTypeLock(profile, registry, slot, ability);
            if (denyReason != null) {
                player.sendMessage(ChatColor.RED + denyReason);
                player.sendMessage(ChatColor.GRAY + "Allowed loadout: " + ChatColor.YELLOW + "1 Movement" + ChatColor.GRAY
                        + ", " + ChatColor.YELLOW + "2 Damage" + ChatColor.GRAY
                        + ", " + ChatColor.YELLOW + "1 Utility/Defense" + ChatColor.GRAY
                        + ", " + ChatColor.YELLOW + "1 Ultimate");
                return true;
            }
        }

        profile.setBoundAbilityId(slot, abilityId);
        player.sendMessage(ChatColor.GREEN + "Bound " +
                ChatColor.AQUA + ability.getDisplayName() +
                ChatColor.GREEN + " to slot " + ChatColor.YELLOW + slot + ChatColor.GREEN + ".");

        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "---- Ability Binding ----");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " <slot 1-5> <abilityId> " +
                ChatColor.GRAY + "- Bind an unlocked ability to a slot.");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " clear <slot 1-5> " +
                ChatColor.GRAY + "- Clear a bound ability.");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " list " +
                ChatColor.GRAY + "- Show your current bindings.");
    }

    private void sendBindingsList(Player player, PlayerProfile profile) {
        player.sendMessage(ChatColor.GOLD + "---- Your Ability Binds ----");
        for (int slot = 1; slot <= 5; slot++) {
            String id = profile.getBoundAbilityId(slot);
            if (id == null || id.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Slot " + slot + ": " + ChatColor.DARK_GRAY + "(empty)");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Slot " + slot + ": " + ChatColor.AQUA + id);
            }
        }
    }

    /**
     * Enforces:
     * - 1x MOVEMENT
     * - 2x DAMAGE (DAMAGE_LIGHT + DAMAGE_HEAVY combined)
     * - 1x UTILITY/DEFENSE (UTILITY + DEFENSE combined)
     * - 1x ULTIMATE
     */
    private String validateTypeLock(PlayerProfile profile, AbilityRegistry registry, int targetSlot, Ability newAbility) {
        int movement = 0;
        int damage = 0;
        int util = 0;
        int ultimate = 0;

        for (int s = 1; s <= 5; s++) {
            if (s == targetSlot) continue;
            String id = profile.getBoundAbilityId(s);
            if (id == null || id.isEmpty()) continue;

            Ability a = registry.getAbility(id);
            if (a == null) continue;

            AbilityType t = a.getType();
            if (t == null) continue;

            switch (t) {
                case MOVEMENT -> movement++;
                case DAMAGE_LIGHT, DAMAGE_HEAVY -> damage++;
                case DEFENSE, UTILITY -> util++;
                case ULTIMATE -> ultimate++;
            }
        }

        AbilityType nt = newAbility.getType();
        if (nt != null) {
            switch (nt) {
                case MOVEMENT -> movement++;
                case DAMAGE_LIGHT, DAMAGE_HEAVY -> damage++;
                case DEFENSE, UTILITY -> util++;
                case ULTIMATE -> ultimate++;
            }
        }

        if (movement > 1) return "You can only bind 1 movement ability.";
        if (damage > 2) return "You can only bind 2 damage abilities.";
        if (util > 1) return "You can only bind 1 utility/defense ability.";
        if (ultimate > 1) return "You can only bind 1 ultimate ability.";

        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player)) return out;

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("list".startsWith(prefix)) out.add("list");
            if ("clear".startsWith(prefix)) out.add("clear");
            for (int i = 1; i <= 5; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            String prefix = args[1].toLowerCase();
            for (int i = 1; i <= 5; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        // /bind <slot> <abilityId> suggestions:
        // Only show unlocked abilities unless you have animesmp.unlock.bypass
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            boolean canSeeAll = player.hasPermission(UNLOCK_BYPASS_PERM);

            for (Ability ability : plugin.getAbilityRegistry().getAllAbilities()) {
                if (ability == null || ability.getId() == null) continue;
                String id = ability.getId().toLowerCase(Locale.ROOT);

                if (!canSeeAll && !profile.hasUnlockedAbility(id)) continue;

                if (id.startsWith(prefix)) out.add(id);
            }
            return out;
        }

        return out;
    }
}
