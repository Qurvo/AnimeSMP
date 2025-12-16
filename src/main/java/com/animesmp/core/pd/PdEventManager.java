package com.animesmp.core.pd;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PdEventManager {

    private final AnimeSMPPlugin plugin;
    private final File stateFile;

    private boolean active = false;
    private long activeUntilMs = 0L;
    private long lastEndMs = 0L;

    // config cache
    private int minWipeLevel = 20;
    private double xpMultiplier = 2.0;
    private double yenMultiplier = 2.0;

    private boolean scheduleEnabled = true;
    private int minEventsPerDay = 1;
    private int maxEventsPerDay = 2;
    private int minDurationMinutes = 60;
    private int maxDurationMinutes = 120;
    private int minGapMinutes = 60;
    private int maxGapMinutes = 180;

    private ZoneId zone = ZoneId.systemDefault();

    // daily plan (persisted)
    private LocalDate plannedDate;
    private final List<Instant> plannedStarts = new ArrayList<>();
    private final List<Integer> plannedDurations = new ArrayList<>();

    public PdEventManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
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
        this.minGapMinutes = plugin.getConfig().getInt("pd-event.schedule.min-gap-minutes", 60);
        this.maxGapMinutes = plugin.getConfig().getInt("pd-event.schedule.max-gap-minutes", 180);

        String zoneId = plugin.getConfig().getString("vendor-reset.zone", "Europe/Lisbon");
        try { this.zone = ZoneId.of(zoneId); } catch (Exception e) { this.zone = ZoneId.systemDefault(); }
    }

    private void startTasks() {
        // every 30 seconds: handle scheduling + end
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L * 30L);
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("pd-event.enabled", true)) return;

        // end if needed
        if (active && System.currentTimeMillis() >= activeUntilMs) {
            stopInternal(false);
        }

        if (!scheduleEnabled) return;
        if (active) return;

        ensureTodayPlanned();

        long now = System.currentTimeMillis();
        int idx = nextPlannedIndex(now);
        if (idx == -1) return;

        long startMs = plannedStarts.get(idx).toEpochMilli();
        if (now >= startMs) {
            int durMin = plannedDurations.get(idx);
            startInternal(durMin * 60_000L, false);

            // remove the one we just used so it doesn't re-trigger if tick runs again quickly
            plannedStarts.remove(idx);
            plannedDurations.remove(idx);
            saveState();
        }
    }

    private void ensureTodayPlanned() {
        LocalDate today = ZonedDateTime.now(zone).toLocalDate();
        if (plannedDate != null && plannedDate.equals(today) && !plannedStarts.isEmpty()) return;

        // if plannedDate is today but list empty, we don't need new ones
        if (plannedDate != null && plannedDate.equals(today) && plannedStarts.isEmpty()) return;

        planToday(today);
        saveState();
    }

    private void planToday(LocalDate day) {
        plannedDate = day;
        plannedStarts.clear();
        plannedDurations.clear();

        int events = rand(minEventsPerDay, maxEventsPerDay);

        ZonedDateTime startOfDay = day.atStartOfDay(zone);

        // Pick first start somewhere in the day, leaving headroom for duration+gap+duration
        int maxHeadroom = Math.max(0, (24 * 60) - (maxDurationMinutes + maxGapMinutes + maxDurationMinutes));
        int firstStartMin = rand(0, maxHeadroom);
        ZonedDateTime cur = startOfDay.plusMinutes(firstStartMin);

        for (int i = 0; i < events; i++) {
            int dur = rand(minDurationMinutes, Math.max(minDurationMinutes, maxDurationMinutes));

            plannedStarts.add(cur.toInstant());
            plannedDurations.add(dur);

            if (i < events - 1) {
                int gap = rand(minGapMinutes, Math.max(minGapMinutes, maxGapMinutes));
                cur = cur.plusMinutes(dur + gap);

                // prevent scheduling beyond end of day
                if (cur.toLocalTime().isAfter(LocalTime.of(23, 30))) break;
            }
        }
    }

    private int nextPlannedIndex(long nowMs) {
        // If we missed a start during downtime, start the most recent one (immediately).
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
     * Returns the next PD start time (epoch millis) that the scheduler would be willing to trigger.
     *
     * <p>This exists primarily for admin/status output (e.g., /pdadmin status). The PD scheduler
     * in this build is driven by the daily planned schedule (pd-event.schedule.*). We therefore
     * expose the next planned start (or the most-recent missed start, if the server was offline),
     * which matches the behaviour of {@link #tick()}.
     *
     * @return epoch millis of the next eligible start, or -1 if no schedule is available.
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

    // -------------------- START / STOP --------------------

    private void startInternal(long durationMs, boolean silent) {
        if (active) return;

        active = true;
        activeUntilMs = System.currentTimeMillis() + Math.max(10_000L, durationMs);

        saveState();

        if (!silent) {
            broadcast(ChatColor.DARK_RED + "====== " + ChatColor.RED + "PERMA DEATH ACTIVE" + ChatColor.DARK_RED + " ======");
            broadcast(ChatColor.GRAY + "If you die during this window, you lose everything.");
            broadcast(ChatColor.AQUA + "Rewards: " + ChatColor.YELLOW + xpMultiplier + "x XP, " + yenMultiplier + "x Yen");
        }
    }

    private void stopInternal(boolean silent) {
        if (!active) return;

        active = false;
        activeUntilMs = 0L;
        lastEndMs = System.currentTimeMillis();

        saveState();

        if (!silent) {
            broadcast(ChatColor.DARK_RED + "====== " + ChatColor.RED + "PERMA DEATH ENDED" + ChatColor.DARK_RED + " ======");
            broadcast(ChatColor.GRAY + "Perma Death is now inactive.");
        }
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
            this.lastEndMs = yml.getLong("lastEndMs", 0L);

            String dateStr = yml.getString("plannedDate", null);
            this.plannedDate = (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);

            plannedStarts.clear();
            plannedDurations.clear();

            List<String> starts = yml.getStringList("plannedStarts");
            List<Integer> durs = yml.getIntegerList("plannedDurations");

            for (String s : starts) {
                try { plannedStarts.add(Instant.parse(s)); } catch (Exception ignored) {}
            }
            plannedDurations.addAll(durs);

            // If PD expired while server offline, clean it
            if (active && System.currentTimeMillis() >= activeUntilMs) {
                active = false;
                activeUntilMs = 0L;
            }

            // Ensure we have a plan for today
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
            yml.set("lastEndMs", lastEndMs);

            yml.set("plannedDate", plannedDate != null ? plannedDate.toString() : null);

            List<String> starts = new ArrayList<>();
            for (Instant i : plannedStarts) starts.add(i.toString());

            yml.set("plannedStarts", starts);
            yml.set("plannedDurations", plannedDurations);

            yml.save(stateFile);
        } catch (IOException ignored) {}
    }

    // ---------------------------------------------------------------------
    // Listener compatibility (PdEventListener expects these methods)
    // ---------------------------------------------------------------------

    public void handleDeath(Player dead, Player killer) {
        if (dead == null) return;
        if (!isActive()) return;

        // TODO: hook into your existing PD penalty logic.
        // Keep empty for now so it compiles without guessing your wipe system.
    }

    public void handleQuit(Player player) {
        // Optional: participation tracking if you have it
    }

    public void handleJoin(Player player) {
        // Optional: notify on join if active
        if (player != null && isActive()) {
            player.sendMessage(ChatColor.RED + "Perma Death is currently ACTIVE.");
        }
    }

    // -------------------- UTIL --------------------

    private void broadcast(String msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
        Bukkit.getLogger().info(ChatColor.stripColor(msg));
    }
}
