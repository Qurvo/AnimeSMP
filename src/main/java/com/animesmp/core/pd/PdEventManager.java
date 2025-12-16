package com.animesmp.core.pd;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PD event manager:
 * - Randomly schedules 1-2 PD windows per day, each 1-2 hours.
 * - Shows title + bossbar (red) + sound on start, and on join while active.
 * - Level 20+ players are wiped on death during PD (profile reset) and kicked.
 * - Rewards:
 *    - 1 PD token per eligible kill (victim level >= 20), anti-farm: same victim within 15s doesn't count
 *    - 1 PD token for "surviving" PD if online >= 45 minutes during the PD window
 *    - +3 PD tokens for top 3 PD kills
 * - Sidebar scoreboard during PD: top 3 + your kills.
 *
 * Persistence:
 * - Persists PD active/activeUntil and today's planned schedule to pd_state.yml.
 * - Kill/participation tracking is in-memory; if server restarts mid-PD, tracking restarts.
 */
public class PdEventManager {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File stateFile;

    // PD active state
    private boolean active = false;
    private long activeUntilMs = 0L;

    // Config
    private int minWipeLevel = 20;
    private double xpMultiplier = 2.0;
    private double yenMultiplier = 2.0;

    private boolean scheduleEnabled = true;
    private int minEventsPerDay = 1;
    private int maxEventsPerDay = 2;
    private int minDurationMinutes = 60;
    private int maxDurationMinutes = 120;

    // Scheduling zone (reuse vendor-reset.zone)
    private ZoneId zone = ZoneId.systemDefault();

    // Daily plan (persisted)
    private LocalDate plannedDate;
    private final List<Instant> plannedStarts = new ArrayList<>();
    private final List<Integer> plannedDurations = new ArrayList<>();

    // UI
    private BossBar pdBossBar;

    // PD kill tracking (in-memory)
    private final Map<UUID, Integer> pdKills = new HashMap<>();
    private final Map<String, Long> lastKillPairMs = new HashMap<>(); // "killerUUID:victimUUID" -> ms
    private final Map<UUID, Long> onlineSinceMs = new HashMap<>();    // for participation during current PD only
    private long pdStartMs = 0L;

    // Scoreboard
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<>();
    private ScoreboardManager sbManager;
    private int scoreboardTaskId = -1;
    private int bossbarTaskId = -1;

    public PdEventManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.stateFile = new File(plugin.getDataFolder(), "pd_state.yml");

        reloadFromConfig();
        loadState();
        startTasks();
    }

    public void reloadFromConfig() {
        this.minWipeLevel = plugin.getConfig().getInt("pd-event.min-wipe-level", 20);
        this.xpMultiplier = plugin.getConfig().getDouble("pd-event.xp-multiplier", 2.0);
        this.yenMultiplier = plugin.getConfig().getDouble("pd-event.yen-multiplier", 2.0);

        this.scheduleEnabled = plugin.getConfig().getBoolean("pd-event.schedule.enabled", true);
        this.minEventsPerDay = plugin.getConfig().getInt("pd-event.schedule.min-events-per-day", 1);
        this.maxEventsPerDay = plugin.getConfig().getInt("pd-event.schedule.max-events-per-day", 2);
        this.minDurationMinutes = plugin.getConfig().getInt("pd-event.schedule.min-duration-minutes", 60);
        this.maxDurationMinutes = plugin.getConfig().getInt("pd-event.schedule.max-duration-minutes", 120);

        String zoneId = plugin.getConfig().getString("vendor-reset.zone", "Europe/Lisbon");
        try {
            this.zone = ZoneId.of(zoneId);
        } catch (Exception e) {
            this.zone = ZoneId.systemDefault();
        }
    }

    private void startTasks() {
        // Tick scheduling/end every 30s
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L * 30L);
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("pd-event.enabled", true)) return;

        if (active && System.currentTimeMillis() >= activeUntilMs) {
            stopInternal(false);
            return;
        }

        if (!scheduleEnabled) return;
        if (active) return;

        ensureTodayPlanned();
        if (plannedStarts.isEmpty()) return;

        long now = System.currentTimeMillis();
        int idx = nextPlannedIndex(now);
        if (idx == -1) return;

        long startMs = plannedStarts.get(idx).toEpochMilli();
        if (now >= startMs) {
            int durMin = plannedDurations.get(idx);
            startInternal(durMin * 60_000L, false);

            // remove used
            plannedStarts.remove(idx);
            plannedDurations.remove(idx);
            saveState();
        }
    }

    private void ensureTodayPlanned() {
        LocalDate today = ZonedDateTime.now(zone).toLocalDate();
        if (plannedDate != null && plannedDate.equals(today)) {
            // keep as-is (even if empty)
            return;
        }
        planToday(today);
        saveState();
    }

    private void planToday(LocalDate day) {
        plannedDate = day;
        plannedStarts.clear();
        plannedDurations.clear();

        int events = rand(minEventsPerDay, maxEventsPerDay);

        ZonedDateTime startOfDay = day.atStartOfDay(zone);

        // Choose random starts across the day; avoid ending after midnight by clamping
        for (int i = 0; i < events; i++) {
            int dur = rand(minDurationMinutes, Math.max(minDurationMinutes, maxDurationMinutes));
            int startMinute = rand(0, 24 * 60 - Math.min(dur, 120)); // keep space

            ZonedDateTime start = startOfDay.plusMinutes(startMinute);
            plannedStarts.add(start.toInstant());
            plannedDurations.add(dur);
        }

        // sort by start time
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < plannedStarts.size(); i++) order.add(i);
        order.sort(Comparator.comparing(plannedStarts::get));

        List<Instant> startsSorted = new ArrayList<>();
        List<Integer> dursSorted = new ArrayList<>();
        for (int idx : order) {
            startsSorted.add(plannedStarts.get(idx));
            dursSorted.add(plannedDurations.get(idx));
        }
        plannedStarts.clear();
        plannedDurations.clear();
        plannedStarts.addAll(startsSorted);
        plannedDurations.addAll(dursSorted);
    }

    private int nextPlannedIndex(long nowMs) {
        // If missed, choose most recent missed; else choose next future
        int missedIdx = -1;
        long missedStart = Long.MIN_VALUE;

        int futureIdx = -1;
        long futureStart = Long.MAX_VALUE;

        for (int i = 0; i < plannedStarts.size(); i++) {
            long s = plannedStarts.get(i).toEpochMilli();
            if (s <= nowMs) {
                if (s > missedStart) {
                    missedStart = s;
                    missedIdx = i;
                }
            } else {
                if (s < futureStart) {
                    futureStart = s;
                    futureIdx = i;
                }
            }
        }
        return missedIdx != -1 ? missedIdx : futureIdx;
    }

    private int rand(int min, int max) {
        if (max < min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    // -------------------- PUBLIC API --------------------

    public boolean isActive() {
        if (!active) return false;
        if (System.currentTimeMillis() >= activeUntilMs) {
            stopInternal(false);
            return false;
        }
        return true;
    }

    public long getRemainingMs() {
        if (!isActive()) return 0L;
        return Math.max(0L, activeUntilMs - System.currentTimeMillis());
    }

    public int getMinWipeLevel() {
        return minWipeLevel;
    }

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    public double getYenMultiplier() {
        return yenMultiplier;
    }

    /**
     * Used by /pdadmin status.
     * @return next planned start time in epoch ms, or -1 if none/disabled/active
     */
    public long getNextEligibleAutoStartMs() {
        if (!scheduleEnabled) return -1L;
        if (!plugin.getConfig().getBoolean("pd-event.enabled", true)) return -1L;
        if (active) return -1L;

        ensureTodayPlanned();
        if (plannedStarts.isEmpty()) return -1L;

        int idx = nextPlannedIndex(System.currentTimeMillis());
        if (idx < 0 || idx >= plannedStarts.size()) return -1L;

        return plannedStarts.get(idx).toEpochMilli();
    }

    public void adminStartNow() {
        int durMin = Math.max(1, minDurationMinutes);
        int durMax = Math.max(durMin, maxDurationMinutes);
        int pick = rand(durMin, durMax);
        startInternal(pick * 60_000L, false);
    }

    public void adminStopNow() {
        stopInternal(true);
    }

    // -------------------- LISTENER HOOKS --------------------

    public void handleJoin(Player player) {
        if (player == null) return;

        if (isActive()) {
            // show UI + sound on join
            sendPdStartUI(player, true);

            // participation tracking
            onlineSinceMs.put(player.getUniqueId(), System.currentTimeMillis());

            // scoreboard
            applyPdScoreboard(player);
        }
    }

    public void handleQuit(Player player) {
        if (player == null) return;
        if (!isActive()) return;

        // keep participation time by leaving their onlineSinceMs as-is (we only count continuous session time)
        // remove scoreboard
        restoreScoreboard(player);
    }

    public void handleDeath(Player dead, Player killer) {
        if (dead == null) return;
        if (!isActive()) return;

        PlayerProfile deadProfile = profiles.getProfile(dead);
        if (deadProfile.getLevel() < minWipeLevel) {
            // kills only count if victim is eligible (>= minWipeLevel)
            return;
        }

        // Count kill token if killer is eligible and anti-farm passes
        if (killer != null && !killer.getUniqueId().equals(dead.getUniqueId())) {
            String key = killer.getUniqueId() + ":" + dead.getUniqueId();
            long now = System.currentTimeMillis();
            long last = lastKillPairMs.getOrDefault(key, 0L);
            if (now - last >= 15_000L) {
                lastKillPairMs.put(key, now);
                pdKills.put(killer.getUniqueId(), pdKills.getOrDefault(killer.getUniqueId(), 0) + 1);

                // Reward 1 PD token per eligible kill
                PlayerProfile kp = profiles.getProfile(killer);
                kp.setPdTokens(kp.getPdTokens() + 1);
                profiles.saveProfile(kp);

                killer.sendMessage(ChatColor.RED + "[PD] " + ChatColor.YELLOW + "+1 PD Token " +
                        ChatColor.GRAY + "(Kill counted: " + ChatColor.WHITE + pdKills.get(killer.getUniqueId()) + ChatColor.GRAY + ")");
            }
        }

        // Wipe dead player + kick
        wipePlayerProfile(dead);
        Bukkit.getScheduler().runTask(plugin, () ->
                dead.kickPlayer(ChatColor.RED + "You have died during PD, join back to begin a new journey.")
        );
    }

    // -------------------- START / STOP --------------------

    private void startInternal(long durationMs, boolean silent) {
        if (active) return;

        this.active = true;
        this.pdStartMs = System.currentTimeMillis();
        this.activeUntilMs = pdStartMs + Math.max(60_000L, durationMs);

        // reset in-memory tracking for this PD window
        pdKills.clear();
        lastKillPairMs.clear();
        onlineSinceMs.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            onlineSinceMs.put(p.getUniqueId(), System.currentTimeMillis());
        }

        // UI
        ensureBossBar();
        ensureScoreboardManager();
        startBossbarTask();
        startScoreboardTask();

        // Apply scoreboard + bossbar to online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyPdScoreboard(p);
            pdBossBar.addPlayer(p);
        }

        saveState();

        if (!silent) {
            broadcastTitleToAll("PD IS NOW ACTIVE", "2x XP, 2x YEN, LVL 20+ players get wiped on death");
            playStartSoundAll();
        }
    }

    private void stopInternal(boolean silent) {
        if (!active) return;

        // Award survival + top3
        awardSurvivalAndTopKills();

        this.active = false;
        this.activeUntilMs = 0L;
        this.pdStartMs = 0L;

        saveState();

        // stop tasks
        stopScoreboardTask();
        stopBossbarTask();

        // clear UI
        if (pdBossBar != null) {
            pdBossBar.removeAll();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            restoreScoreboard(p);
        }

        if (!silent) {
            broadcastTitleToAll("PD is now over", "");
            playEndSoundAll();
        }
    }

    // -------------------- WIPING --------------------

    private void wipePlayerProfile(Player player) {
        PlayerProfile profile = profiles.getProfile(player);

        // Reset to "first join" style defaults used by PlayerProfile constructor,
        // but we do it explicitly to avoid replacing the instance.
        profile.setLevel(1);
        profile.setXp(0);
        profile.setSkillPoints(1);

        profile.setConPoints(0);
        profile.setStrPoints(0);
        profile.setTecPoints(0);
        profile.setDexPoints(0);

        profile.setTrainingLevel(0);
        profile.setTrainingXp(0);

        profile.setStaminaCap(100);
        profile.setStaminaCurrent(100);
        profile.setStaminaRegenPerSecond(6.0);

        profile.setYen(0);
        // PD tokens persist (do NOT wipe)
        // profile.setPdTokens(profile.getPdTokens());

        profile.setSelectedSlot(1);
        profile.getBoundAbilityIds().clear();
        profile.getUnlockedAbilities().clear();

        // trainer quest progress wipe
        profile.setActiveTrainerQuestId(null);
        profile.setActiveTrainerQuestProgress(0);
        profile.getCompletedTrainerQuests().clear();

        // daily purchases wipe
        profile.clearDailyPurchases();

        // tutorial should NOT rerun
        profile.setTutorialCompleted(true);

        profiles.saveProfile(profile);
    }

    // -------------------- REWARDS --------------------

    private void awardSurvivalAndTopKills() {
        // Survival token: online >= 45 minutes during PD
        long requiredMs = 45L * 60_000L;
        long now = System.currentTimeMillis();

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            Long since = onlineSinceMs.get(uuid);
            if (since == null) continue;

            long onlineMs = now - since;
            if (onlineMs >= requiredMs) {
                PlayerProfile prof = profiles.getProfile(uuid);
                prof.setPdTokens(prof.getPdTokens() + 1);
                profiles.saveProfile(prof);
                p.sendMessage(ChatColor.RED + "[PD] " + ChatColor.YELLOW + "+1 PD Token " + ChatColor.GRAY + "(Survived PD)");
            }
        }

        // Top 3 kills (by PD kills map)
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(pdKills.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int awards = Math.min(3, entries.size());
        for (int i = 0; i < awards; i++) {
            UUID winner = entries.get(i).getKey();
            int kills = entries.get(i).getValue();

            PlayerProfile prof = profiles.getProfile(winner);
            prof.setPdTokens(prof.getPdTokens() + 3);
            profiles.saveProfile(prof);

            Player online = Bukkit.getPlayer(winner);
            if (online != null) {
                online.sendMessage(ChatColor.RED + "[PD] " + ChatColor.GOLD + "+3 PD Tokens " +
                        ChatColor.GRAY + "(Top " + (i + 1) + ", " + kills + " kills)");
            }
        }
    }

    // -------------------- UI / SCOREBOARD --------------------

    private void ensureBossBar() {
        if (pdBossBar != null) return;
        pdBossBar = Bukkit.createBossBar("PD IS ACTIVE", BarColor.RED, BarStyle.SOLID);
        pdBossBar.setVisible(true);
    }

    private void startBossbarTask() {
        if (bossbarTaskId != -1) return;
        bossbarTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isActive()) return;

            long remaining = getRemainingMs();
            String timeLeft = formatHoursMinutes(remaining);

            String title = ChatColor.RED + "PD IS ACTIVE - " + ChatColor.WHITE + timeLeft;
            pdBossBar.setTitle(title);

            double total = Math.max(1.0, (double) (activeUntilMs - pdStartMs));
            double prog = Math.max(0.0, Math.min(1.0, remaining / total));
            pdBossBar.setProgress(prog);

        }, 0L, 20L); // update every second
    }

    private void stopBossbarTask() {
        if (bossbarTaskId == -1) return;
        Bukkit.getScheduler().cancelTask(bossbarTaskId);
        bossbarTaskId = -1;
    }

    private void ensureScoreboardManager() {
        if (sbManager != null) return;
        sbManager = Bukkit.getScoreboardManager();
    }

    private void startScoreboardTask() {
        if (scoreboardTaskId != -1) return;
        scoreboardTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isActive()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                updatePdSidebar(p);
            }
        }, 0L, 40L); // every 2 seconds
    }

    private void stopScoreboardTask() {
        if (scoreboardTaskId == -1) return;
        Bukkit.getScheduler().cancelTask(scoreboardTaskId);
        scoreboardTaskId = -1;
    }

    private void applyPdScoreboard(Player player) {
        if (player == null) return;
        if (sbManager == null) ensureScoreboardManager();
        if (sbManager == null) return;

        UUID uuid = player.getUniqueId();
        if (!previousBoards.containsKey(uuid)) {
            previousBoards.put(uuid, player.getScoreboard());
        }

        Scoreboard board = sbManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("pd", Criteria.DUMMY, ChatColor.RED + "" + ChatColor.BOLD + "PD KILLS");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(board);
        updatePdSidebar(player);
    }

    private void restoreScoreboard(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        Scoreboard prev = previousBoards.remove(uuid);
        if (prev != null) {
            player.setScoreboard(prev);
        }
    }

    private void updatePdSidebar(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("pd");
        if (obj == null) return;

        // Clear existing entries by resetting scores
        for (String entry : new HashSet<>(board.getEntries())) {
            board.resetScores(entry);
        }

        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(pdKills.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int rank = 1;
        int scoreLine = 6;

        obj.getScore(ChatColor.GRAY + " ").setScore(scoreLine--);

        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            UUID u = entries.get(i).getKey();
            int kills = entries.get(i).getValue();
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(u).getName()).orElse("Unknown");
            String line = ChatColor.WHITE + "" + rank + ". " + ChatColor.YELLOW + name + ChatColor.GRAY + " - " + ChatColor.RED + kills;
            obj.getScore(trimToScoreboard(line, i)).setScore(scoreLine--);
            rank++;
        }

        obj.getScore(ChatColor.GRAY + "  ").setScore(scoreLine--);

        int mine = pdKills.getOrDefault(player.getUniqueId(), 0);
        obj.getScore(ChatColor.WHITE + "Your kills: " + ChatColor.RED + mine).setScore(scoreLine--);
    }

    private String trimToScoreboard(String s, int salt) {
        // Scoreboard entries are limited; keep simple and unique
        if (s.length() <= 40) return s;
        String cut = s.substring(0, 38);
        return cut + ChatColor.values()[salt % ChatColor.values().length];
    }

    private void sendPdStartUI(Player player, boolean joiningMidEvent) {
        if (player == null) return;

        // bossbar
        ensureBossBar();
        pdBossBar.addPlayer(player);

        // title
        String title = ChatColor.RED + "" + ChatColor.BOLD + "PD IS NOW ACTIVE";
        String subtitle = ChatColor.GRAY + "2x XP, 2x YEN, LVL 20+ players get wiped on death";
        player.sendTitle(title, subtitle, 10, 60, 10);

        // sound
        player.playSound(player.getLocation(),
                joiningMidEvent ? Sound.ENTITY_WITHER_SPAWN : Sound.ENTITY_ENDER_DRAGON_GROWL,
                1.0f, 1.0f);
    }

    private void broadcastTitleToAll(String title, String subtitle) {
        String t = ChatColor.RED + "" + ChatColor.BOLD + title;
        String st = subtitle == null ? "" : ChatColor.GRAY + subtitle;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(t, st, 10, 60, 10);
        }
    }

    private void playStartSoundAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    private void playEndSoundAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f);
        }
    }

    private String formatHoursMinutes(long ms) {
        long totalSec = ms / 1000L;
        long hours = totalSec / 3600L;
        long minutes = (totalSec % 3600L) / 60L;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    // -------------------- PERSISTENCE --------------------

    private void loadState() {
        if (!stateFile.exists()) {
            ensureTodayPlanned();
            return;
        }

        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(stateFile);

            this.active = yml.getBoolean("active", false);
            this.activeUntilMs = yml.getLong("activeUntilMs", 0L);

            String dateStr = yml.getString("plannedDate", null);
            this.plannedDate = (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);

            plannedStarts.clear();
            plannedDurations.clear();

            List<String> starts = yml.getStringList("plannedStarts");
            List<Integer> durs = yml.getIntegerList("plannedDurations");

            for (String s : starts) {
                try {
                    plannedStarts.add(Instant.parse(s));
                } catch (Exception ignored) { }
            }
            plannedDurations.addAll(durs);

            // If PD expired while offline, clean it
            if (active && System.currentTimeMillis() >= activeUntilMs) {
                active = false;
                activeUntilMs = 0L;
            }

            ensureTodayPlanned();
            saveState();

        } catch (Exception e) {
            plannedDate = null;
            plannedStarts.clear();
            plannedDurations.clear();
            ensureTodayPlanned();
            saveState();
        }
    }

    public void saveState() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            YamlConfiguration yml = new YamlConfiguration();
            yml.set("active", active);
            yml.set("activeUntilMs", activeUntilMs);
            yml.set("plannedDate", plannedDate != null ? plannedDate.toString() : null);

            List<String> starts = new ArrayList<>();
            for (Instant i : plannedStarts) starts.add(i.toString());
            yml.set("plannedStarts", starts);
            yml.set("plannedDurations", plannedDurations);

            yml.save(stateFile);
        } catch (IOException ignored) { }
    }
}
