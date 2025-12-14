package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.economy.EconomyManager;
import com.animesmp.core.level.LevelManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /animesmp root command.
 *
 * Player subcommands are proxied to the existing command executors so you can use
 * "/animesmp <command> ..." for everything.
 */

public class AnimeSmpAdminCommand implements CommandExecutor, TabCompleter {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final LevelManager levelManager;
    private final EconomyManager economy;

    // Player-friendly routing: /animesmp <sub> ... proxies to /<sub> ...
    // (keeps the original commands working too, so you don't break muscle memory).
    private static final Map<String, String> PLAYER_PROXY = new HashMap<>();
    static {
        // Core player info / progression
        PLAYER_PROXY.put("stats", "stats");
        PLAYER_PROXY.put("skills", "skills");
        PLAYER_PROXY.put("abilities", "abilities");

        // Ability binding + casting
        PLAYER_PROXY.put("bind", "bind");
        PLAYER_PROXY.put("cast1", "cast1");
        PLAYER_PROXY.put("cast2", "cast2");
        PLAYER_PROXY.put("cast3", "cast3");
        PLAYER_PROXY.put("cast4", "cast4");
        PLAYER_PROXY.put("cast5", "cast5");
        PLAYER_PROXY.put("castselected", "castselected");
        PLAYER_PROXY.put("cyclenext", "cyclenext");
        PLAYER_PROXY.put("cycleprevious", "cycleprevious");

        // Shops + training
        PLAYER_PROXY.put("vendor", "vendor");
        PLAYER_PROXY.put("pdshop", "pdshop");
        PLAYER_PROXY.put("train", "train");

        // Quests + missions
        PLAYER_PROXY.put("mission", "mission");
        PLAYER_PROXY.put("quests", "quests");
        PLAYER_PROXY.put("trainer", "trainer");

        // Currency
        PLAYER_PROXY.put("yen", "yen");
        PLAYER_PROXY.put("pdtokens", "pdtokens");
    }

    public AnimeSmpAdminCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.levelManager = plugin.getLevelManager();
        this.economy = plugin.getEconomyManager();
    }

    private boolean noPerm(CommandSender sender) {
        if (!sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "======== " + ChatColor.YELLOW + "AnimeSMP" + ChatColor.GOLD + " ========");
        sender.sendMessage(ChatColor.AQUA + "/animesmp help" + ChatColor.GRAY + " - Show this help.");

        sender.sendMessage(ChatColor.YELLOW + "Player Commands:");
        sender.sendMessage(ChatColor.AQUA + "/animesmp stats" + ChatColor.GRAY + " - View your stats.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp skills" + ChatColor.GRAY + " - Allocate skill points.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp abilities" + ChatColor.GRAY + " - View unlocked abilities.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp bind <slot> <abilityId>" + ChatColor.GRAY + " - Bind abilities to slots 1–5.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp cast1..cast5" + ChatColor.GRAY + " - Cast bound ability.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp cyclenext|cycleprevious|castselected" + ChatColor.GRAY + " - Cycle/cast.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp vendor" + ChatColor.GRAY + " - Daily ability vendor.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp pdshop" + ChatColor.GRAY + " - PD vendor shop.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp train" + ChatColor.GRAY + " - Training vendor.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp mission" + ChatColor.GRAY + " - Daily missions.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp quests" + ChatColor.GRAY + " - Active trainer quest.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp trainer" + ChatColor.GRAY + " - Trainer help.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp yen" + ChatColor.GRAY + " - View/pay yen.");
        sender.sendMessage(ChatColor.AQUA + "/animesmp pdtokens" + ChatColor.GRAY + " - View/pay PD tokens.");

        if (sender.hasPermission("animesmp.admin")) {
            sender.sendMessage(ChatColor.RED + "Admin Commands:");
            sender.sendMessage(ChatColor.AQUA + "/animesmp inspect <player>" + ChatColor.GRAY + " - View full profile.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp setlevel <player> <level>" + ChatColor.GRAY + " - Set level.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp addxp <player> <amount>" + ChatColor.GRAY + " - Add XP.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp setstat <player> <con|str|tec|dex> <value>" + ChatColor.GRAY + " - Set stat.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp addstat <player> <con|str|tec|dex> <amount>" + ChatColor.GRAY + " - Add to stat.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp giveyen <player> <amount>" + ChatColor.GRAY + " - Give Yen.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp setyen <player> <amount>" + ChatColor.GRAY + " - Set Yen.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp givepdtokens <player> <amount>" + ChatColor.GRAY + " - Give PD tokens.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp setpdtokens <player> <amount>" + ChatColor.GRAY + " - Set PD tokens.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp resetplayer <player>" + ChatColor.GRAY + " - Full wipe/reset.");
            sender.sendMessage(ChatColor.AQUA + "/animesmp refresh <vendor|pdshop|all>" + ChatColor.GRAY + " - Force shop refresh.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Player command proxy: /animesmp <sub> ... -> /<mapped> ...
        String mapped = PLAYER_PROXY.get(sub);
        if (mapped != null) {
            return proxyTo(sender, mapped, Arrays.copyOfRange(args, 1, args.length));
        }

        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;

            case "inspect":
                if (noPerm(sender)) return true;
                handleInspect(sender, args);
                return true;

            case "setlevel":
                if (noPerm(sender)) return true;
                handleSetLevel(sender, args);
                return true;

            case "addxp":
                if (noPerm(sender)) return true;
                handleAddXp(sender, args);
                return true;

            case "setstat":
                if (noPerm(sender)) return true;
                handleSetStat(sender, args);
                return true;

            case "addstat":
                if (noPerm(sender)) return true;
                handleAddStat(sender, args);
                return true;

            case "giveyen":
                if (noPerm(sender)) return true;
                handleGiveYen(sender, args);
                return true;

            case "setyen":
                if (noPerm(sender)) return true;
                handleSetYen(sender, args);
                return true;

            case "givepdtokens":
                if (noPerm(sender)) return true;
                handleGivePdTokens(sender, args);
                return true;

            case "setpdtokens":
                if (noPerm(sender)) return true;
                handleSetPdTokens(sender, args);
                return true;

            case "resetplayer":
                if (noPerm(sender)) return true;
                handleResetPlayer(sender, args);
                return true;

            case "refresh":
                if (noPerm(sender)) return true;
                handleRefresh(sender, args);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean proxyTo(CommandSender sender, String commandName, String[] forwardedArgs) {
        PluginCommand cmd = plugin.getCommand(commandName);
        if (cmd == null || cmd.getExecutor() == null) {
            sender.sendMessage(ChatColor.RED + "Command not available: /" + commandName);
            return true;
        }
        return cmd.getExecutor().onCommand(sender, cmd, commandName, forwardedArgs);
    }

    // ---------- helpers ----------

    private Player requireOnlinePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + name);
        }
        return target;
    }

    private Integer parseInt(CommandSender sender, String raw, String label) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid " + label + ": " + raw);
            return null;
        }
    }

    // ---------- /animesmp inspect ----------

    private void handleInspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp inspect <player>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        PlayerProfile profile = profiles.getProfile(target);

        int level = profile.getLevel();
        int xp = profile.getXp();

        int requiredForNext = levelManager.getXpToNextLevel(target);
        int xpToNext = requiredForNext - xp;
        if (xpToNext < 0) xpToNext = 0;

        int yen = economy.getYen(target);
        int pdTokens = economy.getPdTokens(target);

        sender.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Inspect: " + target.getName() + ChatColor.GOLD + " ======");
        sender.sendMessage(ChatColor.AQUA + "Level: " + ChatColor.YELLOW + level +
                ChatColor.GRAY + " | XP: " + ChatColor.GREEN + xp +
                ChatColor.GRAY + " (to next: " + ChatColor.GREEN + xpToNext + ChatColor.GRAY + ")");
        sender.sendMessage(ChatColor.AQUA + "Training Level: " + ChatColor.YELLOW + profile.getTrainingLevel() +
                ChatColor.GRAY + " | Training XP: " + ChatColor.GREEN + profile.getTrainingXp());

        sender.sendMessage(ChatColor.AQUA + "Stats: " +
                ChatColor.GRAY + "CON=" + ChatColor.YELLOW + profile.getConPoints() +
                ChatColor.GRAY + ", STR=" + ChatColor.YELLOW + profile.getStrPoints() +
                ChatColor.GRAY + ", TEC=" + ChatColor.YELLOW + profile.getTecPoints() +
                ChatColor.GRAY + ", DEX=" + ChatColor.YELLOW + profile.getDexPoints() +
                ChatColor.GRAY + " | Skill Points=" + ChatColor.LIGHT_PURPLE + profile.getSkillPoints());

        sender.sendMessage(ChatColor.AQUA + "Stamina: " +
                ChatColor.YELLOW + (int) profile.getStaminaCurrent() +
                ChatColor.GRAY + " / " +
                ChatColor.YELLOW + profile.getStaminaCap() +
                ChatColor.GRAY + " | Regen/s=" +
                ChatColor.GREEN + profile.getStaminaRegenPerSecond());

        sender.sendMessage(ChatColor.AQUA + "Currency: " +
                ChatColor.YELLOW + yen + ChatColor.GRAY + " Yen" +
                ChatColor.GRAY + " | PD Tokens: " +
                ChatColor.LIGHT_PURPLE + pdTokens);

        sender.sendMessage(ChatColor.AQUA + "Unlocked Abilities: " +
                ChatColor.YELLOW + profile.getUnlockedAbilities().size());
    }

    // ---------- /animesmp setlevel ----------

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp setlevel <player> <level>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer levelInt = parseInt(sender, args[2], "level");
        if (levelInt == null) return;

        int level = Math.max(1, Math.min(50, levelInt));
        PlayerProfile profile = profiles.getProfile(target);
        profile.setLevel(level);
        profile.setXp(0);
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set level of " +
                ChatColor.YELLOW + target.getName() +
                ChatColor.GREEN + " to " + ChatColor.GOLD + level);
        target.sendMessage(ChatColor.YELLOW + "Your level was set to " +
                ChatColor.GOLD + level + ChatColor.YELLOW + " by an admin.");
    }

    // ---------- /animesmp addxp ----------

    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp addxp <player> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer amountInt = parseInt(sender, args[2], "amount");
        if (amountInt == null) return;

        int amount = Math.max(0, amountInt);
        if (amount == 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be > 0.");
            return;
        }

        levelManager.giveXp(target, amount);
        sender.sendMessage(ChatColor.GREEN + "Gave " +
                ChatColor.GOLD + amount + ChatColor.GREEN + " XP to " +
                ChatColor.YELLOW + target.getName());
    }

    // ---------- Stat helpers ----------

    private String normalizeStatKey(String raw) {
        if (raw == null) return "";
        raw = raw.toLowerCase(Locale.ROOT);
        switch (raw) {
            case "con":
            case "constitution":
                return "con";
            case "str":
            case "strength":
                return "str";
            case "tec":
            case "tech":
            case "technique":
                return "tec";
            case "dex":
            case "dexterity":
                return "dex";
            default:
                return "";
        }
    }

    // ---------- /animesmp setstat ----------

    private void handleSetStat(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp setstat <player> <con|str|tec|dex> <value>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        String key = normalizeStatKey(args[2]);
        if (key.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown stat: " + args[2]);
            return;
        }

        Integer valInt = parseInt(sender, args[3], "value");
        if (valInt == null) return;

        int value = Math.max(0, valInt);

        PlayerProfile profile = profiles.getProfile(target);
        switch (key) {
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
        }
        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + key.toUpperCase() +
                ChatColor.GREEN + " of " + ChatColor.YELLOW + target.getName() +
                ChatColor.GREEN + " to " + ChatColor.GOLD + value);
    }

    // ---------- /animesmp addstat ----------

    private void handleAddStat(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp addstat <player> <con|str|tec|dex> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        String key = normalizeStatKey(args[2]);
        if (key.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown stat: " + args[2]);
            return;
        }

        Integer amtInt = parseInt(sender, args[3], "amount");
        if (amtInt == null) return;
        int amount = amtInt;

        PlayerProfile profile = profiles.getProfile(target);

        switch (key) {
            case "con":
                profile.setConPoints(Math.max(0, profile.getConPoints() + amount));
                break;
            case "str":
                profile.setStrPoints(Math.max(0, profile.getStrPoints() + amount));
                break;
            case "tec":
                profile.setTecPoints(Math.max(0, profile.getTecPoints() + amount));
                break;
            case "dex":
                profile.setDexPoints(Math.max(0, profile.getDexPoints() + amount));
                break;
        }

        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Adjusted " + ChatColor.YELLOW + key.toUpperCase() +
                ChatColor.GREEN + " of " + ChatColor.YELLOW + target.getName() +
                ChatColor.GREEN + " by " + ChatColor.GOLD + amount);
    }

    // ---------- /animesmp giveyen ----------

    private void handleGiveYen(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp giveyen <player> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer amtInt = parseInt(sender, args[2], "amount");
        if (amtInt == null) return;

        int amount = Math.max(0, amtInt);
        if (amount == 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be > 0.");
            return;
        }

        economy.addYen(target, amount);

        sender.sendMessage(ChatColor.GREEN + "Gave " +
                ChatColor.GOLD + amount + ChatColor.GREEN + " Yen to " +
                ChatColor.YELLOW + target.getName());
        target.sendMessage(ChatColor.YELLOW + "You received " +
                ChatColor.GOLD + amount + ChatColor.YELLOW + " Yen from an admin.");
    }

    // ---------- /animesmp setyen ----------

    private void handleSetYen(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp setyen <player> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer amtInt = parseInt(sender, args[2], "amount");
        if (amtInt == null) return;

        int amount = Math.max(0, amtInt);
        economy.setYen(target, amount);

        sender.sendMessage(ChatColor.GREEN + "Set Yen of " +
                ChatColor.YELLOW + target.getName() +
                ChatColor.GREEN + " to " + ChatColor.GOLD + amount);
        target.sendMessage(ChatColor.YELLOW + "Your Yen was set to " +
                ChatColor.GOLD + amount + ChatColor.YELLOW + " by an admin.");
    }

    // ---------- /animesmp givepdtokens ----------

    private void handleGivePdTokens(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp givepdtokens <player> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer amtInt = parseInt(sender, args[2], "amount");
        if (amtInt == null) return;

        int amount = Math.max(0, amtInt);
        if (amount == 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be > 0.");
            return;
        }

        economy.addPdTokens(target, amount);

        sender.sendMessage(ChatColor.GREEN + "Gave " +
                ChatColor.GOLD + amount + ChatColor.GREEN + " PD Tokens to " +
                ChatColor.YELLOW + target.getName());
        target.sendMessage(ChatColor.LIGHT_PURPLE + "You received " +
                ChatColor.GOLD + amount + ChatColor.LIGHT_PURPLE + " PD Tokens from an admin.");
    }

    // ---------- /animesmp setpdtokens ----------

    private void handleSetPdTokens(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp setpdtokens <player> <amount>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        Integer amtInt = parseInt(sender, args[2], "amount");
        if (amtInt == null) return;

        int amount = Math.max(0, amtInt);
        economy.setPdTokens(target, amount);

        sender.sendMessage(ChatColor.GREEN + "Set PD Tokens of " +
                ChatColor.YELLOW + target.getName() +
                ChatColor.GREEN + " to " + ChatColor.GOLD + amount);
        target.sendMessage(ChatColor.LIGHT_PURPLE + "Your PD Tokens were set to " +
                ChatColor.GOLD + amount + ChatColor.LIGHT_PURPLE + " by an admin.");
    }

    // ---------- /animesmp resetplayer ----------

    private void handleResetPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp resetplayer <player>");
            return;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return;

        PlayerProfile profile = profiles.getProfile(target);

        profile.setLevel(1);
        profile.setXp(0);
        profile.setSkillPoints(0);
        profile.setConPoints(0);
        profile.setStrPoints(0);
        profile.setTecPoints(0);
        profile.setDexPoints(0);

        profile.setTrainingLevel(0);
        profile.setTrainingXp(0);

        profile.setStaminaCap(100);
        profile.setStaminaCurrent(100.0);
        profile.setStaminaRegenPerSecond(6.0);

        profile.setYen(0);
        profile.setPdTokens(0);

        profile.getBoundAbilityIds().clear();
        profile.setSelectedSlot(1);
        profile.getUnlockedAbilities().clear();

        profiles.saveProfile(profile);

        sender.sendMessage(ChatColor.RED + "Player " + ChatColor.YELLOW + target.getName() +
                ChatColor.RED + " has been fully reset.");
        target.sendMessage(ChatColor.RED + "" + ChatColor.BOLD +
                "Your profile has been reset by an admin.");
    }

    // ---------- /animesmp refresh ----------

    private void handleRefresh(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /animesmp refresh <vendor|pdshop|all>");
            return;
        }

        String which = args[1].toLowerCase(Locale.ROOT);

        switch (which) {
            case "vendor" -> {
                plugin.getRotatingVendorManager().forceRefreshNow();
                sender.sendMessage(ChatColor.GREEN + "Refreshed Daily Ability Vendor rotation now.");
            }
            case "pdshop", "pd" -> {
                plugin.getPdStockManager().forceResetNow();
                sender.sendMessage(ChatColor.GREEN + "Reset PD shop stock now.");
            }
            case "all" -> {
                plugin.getRotatingVendorManager().forceRefreshNow();
                plugin.getPdStockManager().forceResetNow();
                sender.sendMessage(ChatColor.GREEN + "Refreshed vendor rotation + reset PD stock now.");
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown refresh target. Use: vendor, pdshop, all");
        }
    }

    // ---------- Tab Complete ----------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("animesmp.admin");

        // Delegate to proxied command completers when possible
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String mapped = PLAYER_PROXY.get(sub);
            if (mapped != null) {
                PluginCommand pc = plugin.getCommand(mapped);
                if (pc != null && pc.getTabCompleter() != null) {
                    String[] forwarded = Arrays.copyOfRange(args, 1, args.length);
                    List<String> res = pc.getTabCompleter().onTabComplete(sender, pc, mapped, forwarded);
                    return (res != null) ? res : out;
                }
            }
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);

            // Player subcommands
            for (String s : PLAYER_PROXY.keySet()) {
                if (s.startsWith(prefix)) out.add(s);
            }

            // Admin subcommands
            if (isAdmin) {
                for (String s : Arrays.asList(
                        "help", "inspect",
                        "setlevel", "addxp",
                        "setstat", "addstat",
                        "giveyen", "setyen",
                        "givepdtokens", "setpdtokens",
                        "resetplayer",
                        "refresh"
                )) {
                    if (s.startsWith(prefix)) out.add(s);
                }
            } else {
                if ("help".startsWith(prefix)) out.add("help");
            }

            return out;
        }

        if (!isAdmin) {
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("refresh")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                for (String s : Arrays.asList("vendor", "pdshop", "all")) {
                    if (s.startsWith(prefix)) out.add(s);
                }
                return out;
            }
            if (Arrays.asList(
                    "inspect", "setlevel", "addxp",
                    "setstat", "addstat",
                    "giveyen", "setyen",
                    "givepdtokens", "setpdtokens",
                    "resetplayer"
            ).contains(sub)) {
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
            if (sub.equals("setstat") || sub.equals("addstat")) {
                String prefix = args[2].toLowerCase(Locale.ROOT);
                for (String s : Arrays.asList("con", "str", "tec", "dex")) {
                    if (s.startsWith(prefix)) {
                        out.add(s);
                    }
                }
            }
            return out;
        }

        return out;
    }
}
