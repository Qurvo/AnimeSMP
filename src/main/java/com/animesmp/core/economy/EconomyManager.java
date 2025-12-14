package com.animesmp.core.economy;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;

    public EconomyManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
    }

    // --- Yen ---

    public int getYen(Player player) {
        return profiles.getProfile(player).getYen();
    }

    public void setYen(Player player, int amount) {
        PlayerProfile profile = profiles.getProfile(player);
        profile.setYen(amount);
        profiles.saveProfile(profile);
    }

    public void addYen(Player player, int amount) {
        if (amount == 0) return;

        // PD is intended to be 2x gains for everything (XP is handled in LevelManager).
        // Apply the PD multiplier to positive Yen gains from systems that use EconomyManager.
        // (Combat yen-steal is handled separately and already uses 25% -> 50% during PD.)
        if (amount > 0) {
            var pd = plugin.getPdEventManager();
            if (pd != null && pd.isActive()) {
                double mult = pd.getXpMultiplier(); // reuse PD multiplier (defaults to 2.0)
                if (mult > 0) {
                    amount = (int) Math.round(amount * mult);
                }
            }
        }

        PlayerProfile profile = profiles.getProfile(player);
        int newVal = Math.max(0, profile.getYen() + amount);
        profile.setYen(newVal);
        profiles.saveProfile(profile);
    }

    public boolean trySpendYen(Player player, int amount) {
        if (amount <= 0) return true;
        PlayerProfile profile = profiles.getProfile(player);
        int current = profile.getYen();
        if (current < amount) return false;
        profile.setYen(current - amount);
        profiles.saveProfile(profile);
        return true;
    }

    // --- PD Tokens ---

    public int getPdTokens(Player player) {
        return profiles.getProfile(player).getPdTokens();
    }

    public void setPdTokens(Player player, int amount) {
        PlayerProfile profile = profiles.getProfile(player);
        profile.setPdTokens(amount);
        profiles.saveProfile(profile);
    }

    public void addPdTokens(Player player, int amount) {
        if (amount == 0) return;
        PlayerProfile profile = profiles.getProfile(player);
        int newVal = Math.max(0, profile.getPdTokens() + amount);
        profile.setPdTokens(newVal);
        profiles.saveProfile(profile);
    }

    public boolean trySpendPdTokens(Player player, int amount) {
        if (amount <= 0) return true;
        PlayerProfile profile = profiles.getProfile(player);
        int current = profile.getPdTokens();
        if (current < amount) return false;
        profile.setPdTokens(current - amount);
        profiles.saveProfile(profile);
        return true;
    }
}
