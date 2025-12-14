package com.animesmp.core.pd;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.level.LevelManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Random;


/**
 * Global Perma Death manager.
 *
 * - When active:
 *   * All XP is multiplied by getXpMultiplier()
 *   * Yen kill share uses PD / non-PD multipliers in CombatRewardListener
 *   * Any player with level >= minWipeLevel is wiped on ANY death.
 *   * Bossbar + title are shown.
 *
 * - No /pdjoin: PD is GLOBAL.
 *
 * - Admin:
 *   * /pdadmin start → start PD now for a configured duration
 *   * /pdadmin stop  → stop PD immediately
 */
public class PdEventManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final LevelManager levelManager;

    // Current PD state
    private boolean active = false;
    private long endTimeMs = 0L;

    // Config-driven knobs
    private final int minWipeLevel;
    private final double xpMultiplier;
    private final int defaultDurationMinutes;

    // Simple daily scheduling (optional)
    private final boolean autoEnabled;
    private final int autoMinHour;            // inclusive
    private final int autoMaxHour;            // inclusive
    private final int autoMinDurationMinutes; // for auto events
    private final int autoMaxDurationMinutes;
    private final int autoMinPlayers;

    // Persistence
    private final File stateFile;
    private final FileConfiguration stateCfg;
    private final Random random = new Random();


    // Visuals
    private BossBar bossBar;
    private int schedulerTaskId = -1;

    // For "once per day" auto events
    private LocalDate lastAutoDay = null;

    public PdEventManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.levelManager = plugin.getLevelManager();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.stateFile = new File(dataFolder, "pd_state.yml");
        this.stateCfg = YamlConfiguration.loadConfiguration(stateFile);

        FileConfiguration cfg = plugin.getConfig();

        // Core PD config
        this.minWipeLevel = cfg.getInt("pd.min-wipe-level", 20);
        this.xpMultiplier = cfg.getDouble("pd.xp-multiplier", 2.0);
        this.defaultDurationMinutes = cfg.getInt("pd.default-duration-minutes", 90);

        // Auto scheduling (1 event per day, within a window)
        this.autoEnabled = cfg.getBoolean("pd.auto.enabled", true);
        this.autoMinHour = cfg.getInt("pd.auto.min-hour", 18); // 18:00
        this.autoMaxHour = cfg.getInt("pd.auto.max-hour", 23); // 23:00
        this.autoMinDurationMinutes = cfg.getInt("pd.auto.min-duration-minutes", 60);
        this.autoMaxDurationMinutes = cfg.getInt("pd.auto.max-duration-minutes", 120);
        this.autoMinPlayers = cfg.getInt("pd.auto.min-players", 4);

        loadState();
        restoreIfActive();
        startAutoTask();
    }

    // ------------------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------------------

    public boolean isActive() {
        return active;
    }

    /**
     * Used by LevelManager: XP is multiplied by this.
     */
    public double getXpMultiplier() {
        return active ? xpMultiplier : 1.0;
    }

    /**
     * Should this player be wiped on death while PD is active?
     * (Only level >= minWipeLevel.)
     */
    public boolean shouldWipeOnDeath(Player player) {
        if (!active) return false;
        int level = levelManager.getLevel(player);
        return level >= minWipeLevel;
    }

    /**
     * Admin manual start: start PD now using default duration.
     */
    public void adminStartNow() {
        int minutes = Math.max(10, defaultDurationMinutes);
        startPdWindow(minutes * 60_000L, true);
    }

    /**
     * Admin manual stop.
     */
    public void adminStopNow() {
        if (!active) {
            Bukkit.broadcastMessage(ChatColor.RED + "[PD] Perma Death is not active.");
            return;
        }
        Bukkit.broadcastMessage(ChatColor.RED + "[PD] Perma Death has been forcefully disabled by an admin.");
        stopPdInternal(false);
    }

    /**
     * Called from PdEventListener when a player dies.
     * Only handles PD wipe logic & small PD messaging.
     * XP/yen rewards & kill cooldown are handled elsewhere.
     */
    public void handleDeath(Player victim, Player killer) {
        if (!active) return;

        // Wipe check
        if (shouldWipeOnDeath(victim)) {
            wipePlayer(victim);

            // Kick with message
            victim.kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                    "You died during Perma Death.\n" +
                    ChatColor.RED + "Rejoin to start a new adventure.");
        }

        // Optional PD broadcast
        if (killer != null) {
            Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[PD] " +
                    ChatColor.GOLD + killer.getName() +
                    ChatColor.LIGHT_PURPLE + " has slain " +
                    ChatColor.RED + victim.getName() +
                    ChatColor.LIGHT_PURPLE + " during Perma Death.");
        } else {
            Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[PD] " +
                    ChatColor.RED + victim.getName() +
                    ChatColor.LIGHT_PURPLE + " has fallen during Perma Death.");
        }
    }

    /**
     * Called from PdEventListener on quit. Currently no special logic,
     * but we keep it for compatibility / future use.
     */
    public void handleQuit(Player player) {
        // No-op for now (PD is global, not per-participant).
    }

    /**
     * For PlayerConnectionListener: if PD is active when a player joins,
     * show them the same intro message.
     */
    public void handleJoin(Player player) {
        if (!active) return;
        showIntroTo(player);
        addPlayerToBossbar(player);
    }

    // For commands
    public int getMinWipeLevel() {
        return minWipeLevel;
    }

    public long getRemainingMs() {
        if (!active) return 0L;
        long now = System.currentTimeMillis();
        return Math.max(0L, endTimeMs - now);
    }

    // ------------------------------------------------------------------------
    // INTERNAL: START / STOP
    // ------------------------------------------------------------------------

    private void startPdWindow(long durationMs, boolean manual) {
        if (active) {
            // already active
            return;
        }

        active = true;
        long now = System.currentTimeMillis();
        endTimeMs = now + durationMs;

        saveState();

        // Intro for all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            showIntroTo(p);
        }

        // Boss bar
        createOrUpdateBossbar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerToBossbar(p);
        }

        String src = manual ? "by an admin" : "automatically";
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                "[PD] Permanent Death has begun (" + src + ")!");
        Bukkit.broadcastMessage(ChatColor.RED + "All XP & gains are boosted. " +
                "Players level " + ChatColor.YELLOW + minWipeLevel + ChatColor.RED +
                " or higher will be wiped on death!");

        // Per-tick updater
        startEndWatcherTask();
    }

    private void stopPdInternal(boolean fromAutoEnd) {
        active = false;
        endTimeMs = 0L;

        saveState();

        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }

        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
            schedulerTaskId = -1;
        }

        if (fromAutoEnd) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                    "[PD] Permanent Death has ended.");
        }
    }

    private void showIntroTo(Player p) {
        p.sendTitle(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Permanent Death is ON",
                ChatColor.RED + "Everything is 2x. " + ChatColor.DARK_RED + "Dying gets you wiped.",
                10, 60, 10
        );
        p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                "[PD] " + ChatColor.RED +
                "Permanent Death is active. Level " + ChatColor.YELLOW + minWipeLevel +
                ChatColor.RED + "+ players are wiped on death.");
    }

    // ------------------------------------------------------------------------
    // BOSSBAR
    // ------------------------------------------------------------------------

    private void createOrUpdateBossbar() {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "PERMA DEATH ACTIVE",
                    BarColor.RED,
                    BarStyle.SOLID
            );
        } else {
            bossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "PERMA DEATH ACTIVE");
        }
        bossBar.setVisible(true);
        bossBar.setProgress(1.0); // we don't show the timer, so just full.
    }

    private void addPlayerToBossbar(Player p) {
        if (bossBar == null) return;
        if (!bossBar.getPlayers().contains(p)) {
            bossBar.addPlayer(p);
        }
    }

    private void startEndWatcherTask() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
        }

        schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> {
                    if (!active) return;
                    long remaining = getRemainingMs();
                    if (remaining <= 0L) {
                        stopPdInternal(true);
                    }
                    // we keep bossbar progress at 1.0 on purpose:
                    // no visible timer so players can't meta-time PD.
                },
                20L, 20L
        );
    }

    // ------------------------------------------------------------------------
    // WIPE LOGIC
    // ------------------------------------------------------------------------

    private void wipePlayer(Player player) {
        UUID id = player.getUniqueId();
        PlayerProfile profile = profiles.getProfile(player);

        // Reset profile to default fresh state
        PlayerProfile fresh = new PlayerProfile(id);

        // Copy over the new fresh profile
        // Easiest: manually copy fields you care about OR overwrite file.
        // We'll just save the fresh profile and replace in manager map.

        profiles.saveProfile(fresh);
        // Overwrite the in-memory cache
        // (the manager uses computeIfAbsent + map, but we don't have direct map access.
        //  simplest approach: set core fields on existing profile)
        profile.setLevel(fresh.getLevel());
        profile.setXp(fresh.getXp());
        profile.setSkillPoints(fresh.getSkillPoints());
        profile.setConPoints(fresh.getConPoints());
        profile.setStrPoints(fresh.getStrPoints());
        profile.setTecPoints(fresh.getTecPoints());
        profile.setDexPoints(fresh.getDexPoints());
        profile.setTrainingLevel(fresh.getTrainingLevel());
        profile.setTrainingXp(fresh.getTrainingXp());
        profile.setStaminaCap(fresh.getStaminaCap());
        profile.setStaminaCurrent(fresh.getStaminaCurrent());
        profile.setStaminaRegenPerSecond(fresh.getStaminaRegenPerSecond());
        profile.setYen(fresh.getYen());
        profile.setPdTokens(fresh.getPdTokens());
        profile.getUnlockedAbilities().clear();
        profile.getBoundAbilityIds().clear();
        profile.setSelectedSlot(1);

        profiles.saveProfile(profile);
    }

    // ------------------------------------------------------------------------
    // STATE PERSISTENCE
    // ------------------------------------------------------------------------

    private void loadState() {
        this.active = stateCfg.getBoolean("active", false);
        this.endTimeMs = stateCfg.getLong("endTimeMs", 0L);

        String lastDay = stateCfg.getString("lastAutoDay", null);
        if (lastDay != null) {
            try {
                lastAutoDay = LocalDate.parse(lastDay);
            } catch (Exception ignored) {
            }
        }
    }

    private void saveState() {
        stateCfg.set("active", active);
        stateCfg.set("endTimeMs", endTimeMs);
        if (lastAutoDay != null) {
            stateCfg.set("lastAutoDay", lastAutoDay.toString());
        }
        try {
            stateCfg.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pd_state.yml");
            e.printStackTrace();
        }
    }

    private void restoreIfActive() {
        if (!active) return;
        long now = System.currentTimeMillis();
        if (endTimeMs <= now) {
            // Expired while server was offline
            active = false;
            endTimeMs = 0L;
            saveState();
            return;
        }

        long remaining = endTimeMs - now;
        plugin.getLogger().info("[PD] Restoring active Perma Death window (" + (remaining / 1000) + "s left).");

        // Recreate visuals & watcher
        createOrUpdateBossbar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerToBossbar(p);
            showIntroTo(p);
        }
        startEndWatcherTask();
    }

    // ------------------------------------------------------------------------
    // SIMPLE AUTO SCHEDULER (1 event per day max)
    // ------------------------------------------------------------------------

    private void startAutoTask() {
        if (!autoEnabled) return;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active) return; // already running

            LocalDate today = LocalDate.now();
            if (lastAutoDay != null && lastAutoDay.equals(today)) {
                return; // already did an auto PD today
            }

            int hour = java.time.LocalTime.now().getHour();
            if (hour < autoMinHour || hour > autoMaxHour) return;

            if (Bukkit.getOnlinePlayers().size() < autoMinPlayers) return;

            long durationMin = autoMinDurationMinutes;
            long durationMax = autoMaxDurationMinutes;
            if (durationMax < durationMin) durationMax = durationMin;

            long durationMinutes;
            if (durationMax == durationMin) {
                durationMinutes = durationMin;
            } else {
                long diff = durationMax - durationMin;
                durationMinutes = durationMin + random.nextInt((int) diff + 1);
            }

            lastAutoDay = today;
            saveState();

            startPdWindow(durationMinutes * 60_000L, false);


        }, 20L * 60L, 20L * 60L); // check every 60s
    }
}
