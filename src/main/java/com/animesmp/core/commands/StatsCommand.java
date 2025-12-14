package com.animesmp.core.commands;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.level.LevelManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.stamina.StaminaManager;
import com.animesmp.core.stats.StatsManager;
import com.animesmp.core.training.TrainingManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final StatsManager statsManager;
    private final TrainingManager trainingManager;
    private final StaminaManager staminaManager;
    private final LevelManager levelManager;

    public StatsCommand(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.statsManager = plugin.getStatsManager();
        this.trainingManager = plugin.getTrainingManager();
        this.staminaManager = plugin.getStaminaManager();
        this.levelManager = plugin.getLevelManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = profiles.getProfile(player);

        int level = profile.getLevel();
        int xp = profile.getXp();
        int xpToNext = level >= 50 ? 0 : levelManager.getXpToNextLevel(player);
        int skillPoints = profile.getSkillPoints();

        int con = profile.getConPoints();
        int str = profile.getStrPoints();
        int tec = profile.getTecPoints();
        int dex = profile.getDexPoints();

        int trainingLevel = profile.getTrainingLevel();
        int trainingXp = profile.getTrainingXp();

        int staminaCap = profile.getStaminaCap();
        double staminaCurrent = profile.getStaminaCurrent();
        double staminaRegen = profile.getStaminaRegenPerSecond();

        double dmgMult = statsManager.getAbilityDamageMultiplier(player);
        double dmgRed = statsManager.getAbilityDamageReduction(player);
        double cdMult = statsManager.getCooldownMultiplier(player);
        double costMult = statsManager.getStaminaCostMultiplier(player);

        player.sendMessage(ChatColor.DARK_GRAY + "------------------------------");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " AnimeSMP Stats");
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + level +
                ChatColor.GRAY + "  | Skill Points: " + ChatColor.AQUA + skillPoints);
        if (level >= 50) {
            player.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.GREEN + xp +
                    ChatColor.DARK_GRAY + " (MAX LEVEL)");
        } else {
            player.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.GREEN + xp +
                    ChatColor.GRAY + "  | To next: " + ChatColor.AQUA + xpToNext);
        }
        player.sendMessage("");

        player.sendMessage(ChatColor.YELLOW + "Stats:");
        player.sendMessage(ChatColor.RED + "  CON: " + con +
                ChatColor.GRAY + "  STR: " + str +
                ChatColor.AQUA + "  TEC: " + tec +
                ChatColor.GREEN + "  DEX: " + dex);

        player.sendMessage(ChatColor.YELLOW + "Training:");
        player.sendMessage(ChatColor.GRAY + "  Training Level: " + ChatColor.AQUA + trainingLevel +
                ChatColor.GRAY + "  | Training XP: " + ChatColor.AQUA + trainingXp);

        player.sendMessage(ChatColor.YELLOW + "Stamina:");
        player.sendMessage(ChatColor.GRAY + "  Current: " + ChatColor.AQUA +
                String.format("%.1f", staminaCurrent) + ChatColor.GRAY +
                " / " + ChatColor.AQUA + staminaCap);
        player.sendMessage(ChatColor.GRAY + "  Regen/sec: " + ChatColor.AQUA +
                String.format("%.2f", staminaRegen));

        player.sendMessage(ChatColor.YELLOW + "Ability Scaling:");
        player.sendMessage(ChatColor.GRAY + "  Damage Multiplier: " + ChatColor.AQUA +
                String.format("%.2fx", dmgMult));
        player.sendMessage(ChatColor.GRAY + "  Damage Reduction: " + ChatColor.AQUA +
                String.format("%.0f%%", dmgRed * 100.0));
        player.sendMessage(ChatColor.GRAY + "  Cooldown Multiplier: " + ChatColor.AQUA +
                String.format("%.2fx", cdMult));
        player.sendMessage(ChatColor.GRAY + "  Stamina Cost Multiplier: " + ChatColor.AQUA +
                String.format("%.2fx", costMult));

        player.sendMessage(ChatColor.DARK_GRAY + "Use " + ChatColor.YELLOW + "/skills" +
                ChatColor.DARK_GRAY + " to allocate your stat points.");
        player.sendMessage(ChatColor.DARK_GRAY + "------------------------------");
        return true;
    }
}
