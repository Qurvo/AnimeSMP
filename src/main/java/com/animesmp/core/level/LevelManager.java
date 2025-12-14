package com.animesmp.core.level;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.animesmp.core.pd.PdEventManager;


public class LevelManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;

    // Cached XP curve: level -> xp needed for next level
    private final Map<Integer, Integer> xpCurve = new HashMap<>();

    // max level per GDD
    private static final int MAX_LEVEL = 50;

    public LevelManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        buildXpCurve();
    }

    private void buildXpCurve() {
        // Simple increasing curve: base 100 + 25 * (level-1)
        for (int lvl = 1; lvl < MAX_LEVEL; lvl++) {
            int required = 100 + (lvl - 1) * 25;
            xpCurve.put(lvl, required);
        }
    }

    public int getXpForNextLevel(int level) {
        if (level >= MAX_LEVEL) return Integer.MAX_VALUE;
        return xpCurve.getOrDefault(level, 100);
    }

    public int getLevel(Player player) {
        return profiles.getProfile(player).getLevel();
    }

    public int getXp(Player player) {
        return profiles.getProfile(player).getXp();
    }

    // -------- NEW: compatibility for StatsCommand --------
    public int getXpToNextLevel(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int level = profile.getLevel();
        if (level >= MAX_LEVEL) return 0;

        int needed = getXpForNextLevel(level);
        int currentXp = profile.getXp();
        int remaining = needed - currentXp;
        return Math.max(0, remaining);
    }

    /**
     * Main XP entry used by new code.
     */
    public void addXp(Player player, int amount) {
        if (amount <= 0) return;

        // PD 2x multiplier (or whatever is set in config)
        PdEventManager pd = plugin.getPdEventManager();
        if (pd != null && pd.isActive()) {
            double mult = pd.getXpMultiplier();
            if (mult > 0) {
                amount = (int) Math.round(amount * mult);
            }
        }

        PlayerProfile profile = profiles.getProfile(player);
        applyXp(profile, player, amount);
    }


    // -------- NEW: backwards-compatible alias for old code --------
    public void giveXp(Player player, int amount) {
        addXp(player, amount);
    }

    /**
     * If you ever want to add XP to an offline player by UUID.
     */
    public void addXp(UUID uuid, int amount) {
        if (amount <= 0) return;
        PlayerProfile profile = profiles.getProfile(uuid);
        Player online = plugin.getServer().getPlayer(uuid);
        applyXp(profile, online, amount);
    }

    private void applyXp(PlayerProfile profile, Player player, int amount) {
        int level = profile.getLevel();
        int xp = profile.getXp();

        if (level >= MAX_LEVEL) {
            // still store XP, but no level up
            profile.setXp(xp + amount);
            profiles.saveProfile(profile);
            return;
        }

        xp += amount;
        boolean leveled = false;

        while (level < MAX_LEVEL) {
            int needed = getXpForNextLevel(level);
            if (xp < needed) break;

            xp -= needed;
            level++;
            profile.setSkillPoints(profile.getSkillPoints() + 1);
            leveled = true;
        }

        profile.setLevel(level);
        profile.setXp(xp);

        profiles.saveProfile(profile);

        if (player != null && leveled) {
            player.sendMessage(ChatColor.GOLD + "You leveled up! " +
                    ChatColor.YELLOW + "Level " + level +
                    ChatColor.GRAY + " | " +
                    ChatColor.AQUA + "Skill Points: " + profile.getSkillPoints());
        }
    }
}
