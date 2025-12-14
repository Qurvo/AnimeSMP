package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.training.TrainingManager;
import com.animesmp.core.training.TrainingTokenTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AnimeSmpTestCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final TrainingManager trainingManager;

    public AnimeSmpTestCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.trainingManager = plugin.getTrainingManager();
    }

    private boolean checkPerm(CommandSender sender) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPerm(sender)) return true;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "setlevel":
                cmdSetLevel(sender, args);
                break;
            case "addxp":
                cmdAddXp(sender, args);
                break;
            case "addskillpoints":
                cmdAddSkillPoints(sender, args);
                break;
            case "setstat":
                cmdSetStat(sender, args);
                break;
            case "setyen":
                cmdSetYen(sender, args);
                break;
            case "addyen":
                cmdAddYen(sender, args);
                break;
            case "setpdtokens":
                cmdSetPdTokens(sender, args);
                break;
            case "addpdtokens":
                cmdAddPdTokens(sender, args);
                break;
            case "givetrainingtoken":
                cmdGiveTrainingToken(sender, args);
                break;
            case "resetplayer":
                cmdResetPlayer(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "AnimeSMP Test Tools" + ChatColor.GOLD + " =====");
        sender.sendMessage(ChatColor.AQUA + "/asptest setlevel <player> <level>");
        sender.sendMessage(ChatColor.AQUA + "/asptest addxp <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest addskillpoints <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest setstat <player> <con|str|tec|dex> <value>");
        sender.sendMessage(ChatColor.AQUA + "/asptest setyen <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest addyen <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest setpdtokens <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest addpdtokens <player> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest givetrainingtoken <player> <tier> <amount>");
        sender.sendMessage(ChatColor.AQUA + "/asptest resetplayer <player>");
    }

    // ---------- helpers ----------

    private Player requirePlayer(CommandSender sender, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + name);
        }
        return p;
    }

    private Integer parseInt(CommandSender sender, String raw, String what) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid " + what + ": " + raw);
            return null;
        }
    }

    // ---------- subcommands ----------

    private void cmdSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest setlevel <player> <level>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer level = parseInt(sender, args[2], "level");
        if (level == null) return;

        if (level < 1) level = 1;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setLevel(level);
        profile.setXp(0);

        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set level of " + target.getName() + " to " + level);
        target.sendMessage(ChatColor.YELLOW + "Your level has been set to " + level + " for testing.");
    }

    private void cmdAddXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest addxp <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be > 0.");
            return;
        }

        plugin.getLevelManager().addXp(target, amount);
        sender.sendMessage(ChatColor.GREEN + "Added " + amount + " XP to " + target.getName());
    }

    private void cmdAddSkillPoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest addskillpoints <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setSkillPoints(profile.getSkillPoints() + Math.max(0, amount));
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " skill points to " + target.getName());
    }

    private void cmdSetStat(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest setstat <player> <con|str|tec|dex> <value>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        String stat = args[2].toLowerCase(Locale.ROOT);
        Integer value = parseInt(sender, args[3], "value");
        if (value == null) return;
        if (value < 0) value = 0;

        PlayerProfile profile = profiles.getProfile(target);

        switch (stat) {
            case "con":
                profile.setConPoints(value);
                break;
            case "str":
                profile.setStrPoints(value);
                break;
            case "tec":
                profile.setTecPoints(value);
                break;
            case "dex":
                profile.setDexPoints(value);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown stat: " + stat + " (use con/str/tec/dex)");
                return;
        }

        profiles.saveProfile(profile);
        sender.sendMessage(ChatColor.GREEN + "Set " + stat + " of " + target.getName() + " to " + value);
    }

    private void cmdSetYen(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest setyen <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;
        if (amount < 0) amount = 0;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setYen(amount);
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set yen of " + target.getName() + " to " + amount);
    }

    private void cmdAddYen(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest addyen <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setYen(profile.getYen() + amount);
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Added " + amount + " yen to " + target.getName());
    }

    private void cmdSetPdTokens(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest setpdtokens <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;
        if (amount < 0) amount = 0;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setPdTokens(amount);
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set PD tokens of " + target.getName() + " to " + amount);
    }

    private void cmdAddPdTokens(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest addpdtokens <player> <amount>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        Integer amount = parseInt(sender, args[2], "amount");
        if (amount == null) return;

        PlayerProfile profile = profiles.getProfile(target);
        profile.setPdTokens(profile.getPdTokens() + amount);
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Added " + amount + " PD tokens to " + target.getName());
    }

    private void cmdGiveTrainingToken(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest givetrainingtoken <player> <tier> <amount>");
            sender.sendMessage(ChatColor.GRAY + "Tiers: BASIC, INTERMEDIATE, ADVANCED, EXPERT, MASTER");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        String tierRaw = args[2].toUpperCase(Locale.ROOT);
        TrainingTokenTier tier;
        try {
            tier = TrainingTokenTier.valueOf(tierRaw);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Unknown tier: " + tierRaw);
            return;
        }

        Integer amount = parseInt(sender, args[3], "amount");
        if (amount == null) return;
        if (amount <= 0) amount = 1;

        ItemStack token = trainingManager.createToken(tier, amount);
        target.getInventory().addItem(token);

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + tier.name() + " training token(s) to " + target.getName());
    }

    private void cmdResetPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /asptest resetplayer <player>");
            return;
        }
        Player target = requirePlayer(sender, args[1]);
        if (target == null) return;

        PlayerProfile profile = profiles.getProfile(target);
        UUID id = profile.getUuid();

        // Reset core progression & stats
        profile.setLevel(1);
        profile.setXp(0);
        profile.setSkillPoints(0);
        profile.setConPoints(0);
        profile.setStrPoints(0);
        profile.setTecPoints(0);
        profile.setDexPoints(0);

        // Training
        profile.setTrainingLevel(0);
        profile.setTrainingXp(0);

        // Stamina
        profile.setStaminaCap(100);
        profile.setStaminaCurrent(100.0);
        profile.setStaminaRegenPerSecond(6.0);

        // Currency
        profile.setYen(0);
        profile.setPdTokens(0);

        // Abilities
        profile.getUnlockedAbilities().clear();
        profile.getBoundAbilityIds().clear();
        profile.setSelectedSlot(1);

        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GOLD + "Reset player profile for " + target.getName());
        target.sendMessage(ChatColor.RED + "Your profile has been reset for testing.");
    }

    // ---------- tab complete ----------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("animesmp.admin")) return out;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            String[] subs = {
                    "setlevel", "addxp", "addskillpoints",
                    "setstat", "setyen", "addyen",
                    "setpdtokens", "addpdtokens",
                    "givetrainingtoken", "resetplayer"
            };
            for (String s : subs) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setstat")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (String s : new String[]{"con", "str", "tec", "dex"}) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givetrainingtoken")) {
            String prefix = args[2].toUpperCase(Locale.ROOT);
            for (String s : new String[]{"BASIC", "INTERMEDIATE", "ADVANCED", "EXPERT", "MASTER"}) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        return out;
    }
}
