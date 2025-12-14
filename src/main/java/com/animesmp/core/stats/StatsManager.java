package com.animesmp.core.stats;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.util.AttributeUtil;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class StatsManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;

    public StatsManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
    }

    public void recalculateStats(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int con = profile.getConPoints();
        int dex = profile.getDexPoints();

        // (dexMultiplier currently unused, keeping your logic intact)
        double dexMultiplier = 1.0 + (dex * 0.005);

        // Max HP: base 10 hearts (20 HP), +0.25 hearts per CON (up to +10 hearts)
        double extraHearts = con * 0.25;
        if (extraHearts > 10.0) {
            extraHearts = 10.0;
        }
        double maxHearts = 10.0 + extraHearts;
        double maxHealth = maxHearts * 2.0;

        AttributeInstance maxHpAttr = player.getAttribute(AttributeUtil.maxHealth());
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(maxHealth);
        }

        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }

        // Stamina handled by StaminaManager using trainingLevel
    }

    public double getAbilityDamageMultiplier(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int str = profile.getStrPoints();
        int dex = profile.getDexPoints();
        double base = 1.0 + (str * 0.012);
        return base * (1.0 + dex * 0.005);
    }

    public double getAbilityDamageReduction(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int con = profile.getConPoints();
        int dex = profile.getDexPoints();
        double reduction = (con * 0.005) * (1.0 + dex * 0.005);
        if (reduction > 0.8) {
            reduction = 0.8;
        }
        return reduction;
    }

    public double getCooldownMultiplier(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int tec = profile.getTecPoints();
        int dex = profile.getDexPoints();
        double base = 1.0 - (tec * 0.0075);
        if (base < 0.6) base = 0.6;

        base *= (1.0 - dex * 0.005);
        if (base < 0.5) base = 0.5;

        return base;
    }

    public double getStaminaCostMultiplier(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int tec = profile.getTecPoints();
        int dex = profile.getDexPoints();
        double base = 1.0 - (tec * 0.0075);
        if (base < 0.6) base = 0.6;

        base *= (1.0 - dex * 0.005);
        if (base < 0.5) base = 0.5;

        return base;
    }
}
