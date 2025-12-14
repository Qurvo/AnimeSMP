package com.animesmp.core.daily;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Handles:
 *  - Daily login streak rewards (Yen)
 *  - Three daily missions per-player (Easy / Medium / Hard)
 *
 * Data is persisted to daily_data.yml.
 */
public class DailyRewardManager {

    public enum MissionDifficulty {
        EASY(ChatColor.GREEN, "Easy"),
        MEDIUM(ChatColor.YELLOW, "Medium"),
        HARD(ChatColor.RED, "Hard");

        private final ChatColor color;
        private final String label;

        MissionDifficulty(ChatColor color, String label) {
            this.color = color;
            this.label = label;
        }

        public ChatColor getColor() {
            return color;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum MissionType {
        KILL_MOBS,
        UNIQUE_PLAYER_KILLS,
        DISTANCE_TRAVELLED
    }

    public static class DailyMission {
        private final MissionType type;
        private final MissionDifficulty difficulty;
        private final int target;
        private final int rewardYen;
        private final String description;

        private int progress;

        public DailyMission(MissionType type,
                            MissionDifficulty difficulty,
                            int target,
                            int rewardYen,
                            String description,
                            int progress) {
            this.type = type;
            this.difficulty = difficulty;
            this.target = target;
            this.rewardYen = rewardYen;
            this.description = description;
            this.progress = progress;
        }

        public MissionType getType() {
            return type;
        }

        public MissionDifficulty getDifficulty() {
            return difficulty;
        }

        public int getTarget() {
            return target;
        }

        public int getRewardYen() {
            return rewardYen;
        }

        public String getDescription() {
            return description;
        }

        public int getProgress() {
            return progress;
        }

        public boolean isCompleted() {
            return progress >= target;
        }

        public void addProgress(int amount) {
            if (isCompleted()) return;
            this.progress = Math.min(target, this.progress + amount);
        }
    }

    private final AnimeSMPPlugin plugin;
    private final EconomyManager economy;

    private final File dataFile;
    private FileConfiguration data;

    // Per-player
    private final Map<UUID, LocalDate> lastLoginDate = new HashMap<>();
    private final Map<UUID, Integer> loginStreak = new HashMap<>();

    private final Map<UUID, LocalDate> missionsDate = new HashMap<>();
    private final Map<UUID, List<DailyMission>> missions = new HashMap<>();

    // For UNIQUE_PLAYER_KILLS mission
    private final Map<UUID, Set<UUID>> uniqueKillsToday = new HashMap<>();

    // For DISTANCE_TRAVELLED mission (buffer fractional blocks)
    private final Map<UUID, Double> distanceBuffer = new HashMap<>();

    public DailyRewardManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomyManager();
        this.dataFile = new File(plugin.getDataFolder(), "daily_data.yml");
        reload();
    }

    public void reload() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);

        lastLoginDate.clear();
        loginStreak.clear();
        missionsDate.clear();
        missions.clear();
        uniqueKillsToday.clear();
        distanceBuffer.clear();

        ConfigurationSection playersSec = data.getConfigurationSection("players");
        if (playersSec == null) return;

        for (String key : playersSec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection sec = playersSec.getConfigurationSection(key);
                if (sec == null) continue;

                String lastLoginStr = sec.getString("lastLogin");
                if (lastLoginStr != null && !lastLoginStr.isEmpty()) {
                    lastLoginDate.put(uuid, LocalDate.parse(lastLoginStr));
                }
                loginStreak.put(uuid, sec.getInt("streak", 0));

                String missionsDateStr = sec.getString("missionsDate");
                if (missionsDateStr != null && !missionsDateStr.isEmpty()) {
                    missionsDate.put(uuid, LocalDate.parse(missionsDateStr));
                }

                List<DailyMission> missionList = new ArrayList<>();
                ConfigurationSection missionSec = sec.getConfigurationSection("missions");
                if (missionSec != null) {
                    for (String id : missionSec.getKeys(false)) {
                        ConfigurationSection m = missionSec.getConfigurationSection(id);
                        if (m == null) continue;
                        try {
                            MissionType type = MissionType.valueOf(m.getString("type"));
                            MissionDifficulty diff = MissionDifficulty.valueOf(m.getString("difficulty"));
                            int target = m.getInt("target", 0);
                            int reward = m.getInt("rewardYen", 0);
                            int progress = m.getInt("progress", 0);
                            String desc = m.getString("description", type.name());
                            missionList.add(new DailyMission(type, diff, target, reward, desc, progress));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (!missionList.isEmpty()) {
                    missions.put(uuid, missionList);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveAll() {
        FileConfiguration out = new YamlConfiguration();
        ConfigurationSection playersSec = out.createSection("players");

        for (UUID uuid : getKnownPlayers()) {
            ConfigurationSection sec = playersSec.createSection(uuid.toString());

            LocalDate last = lastLoginDate.get(uuid);
            if (last != null) {
                sec.set("lastLogin", last.toString());
            }
            sec.set("streak", loginStreak.getOrDefault(uuid, 0));

            LocalDate mDate = missionsDate.get(uuid);
            if (mDate != null) {
                sec.set("missionsDate", mDate.toString());
            }

            List<DailyMission> list = missions.get(uuid);
            if (list != null && !list.isEmpty()) {
                ConfigurationSection mSec = sec.createSection("missions");
                int idx = 0;
                for (DailyMission m : list) {
                    ConfigurationSection ms = mSec.createSection("m" + (idx++));
                    ms.set("type", m.getType().name());
                    ms.set("difficulty", m.getDifficulty().name());
                    ms.set("target", m.getTarget());
                    ms.set("rewardYen", m.getRewardYen());
                    ms.set("progress", m.getProgress());
                    ms.set("description", m.getDescription());
                }
            }
        }

        try {
            out.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<UUID> getKnownPlayers() {
        Set<UUID> uuids = new HashSet<>();
        uuids.addAll(lastLoginDate.keySet());
        uuids.addAll(loginStreak.keySet());
        uuids.addAll(missionsDate.keySet());
        uuids.addAll(missions.keySet());
        return uuids;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    // ------------------------------------------------------------------------
    // Login rewards
    // ------------------------------------------------------------------------

    public void handleLogin(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDate today = today();
        LocalDate last = lastLoginDate.get(uuid);

        // If already logged in today, do nothing except ensure missions exist
        if (last != null && last.equals(today)) {
            ensureMissionsFor(uuid);
            return;
        }

        int streak;
        if (last == null || last.isBefore(today.minusDays(1))) {
            streak = 1;
        } else {
            streak = loginStreak.getOrDefault(uuid, 0) + 1;
        }

        lastLoginDate.put(uuid, today);
        loginStreak.put(uuid, streak);

        // Daily login reward: start at 500, +250 per streak, cap at 3000
        int reward = 500 + (streak - 1) * 250;
        if (reward > 3000) reward = 3000;

        economy.addYen(player, reward);
        player.sendMessage(ChatColor.GOLD + "[Daily Login] " + ChatColor.GREEN +
                "Day " + streak + " streak! You received " + ChatColor.AQUA + reward +
                ChatColor.GREEN + " Yen.");

        // Ensure missions for today are generated
        ensureMissionsFor(uuid);
    }

    // ------------------------------------------------------------------------
    // Missions
    // ------------------------------------------------------------------------

    private void ensureMissionsFor(UUID uuid) {
        LocalDate today = today();
        LocalDate mDate = missionsDate.get(uuid);
        if (mDate != null && mDate.equals(today) && missions.containsKey(uuid)) {
            return; // already generated
        }

        // New day: reset unique kills and distance buffer
        uniqueKillsToday.remove(uuid);
        distanceBuffer.remove(uuid);

        List<DailyMission> generated = new ArrayList<>();

        // Easy: kill mobs
        generated.add(generateMission(MissionDifficulty.EASY, MissionType.KILL_MOBS));

        // Medium: distance travel
        generated.add(generateMission(MissionDifficulty.MEDIUM, MissionType.DISTANCE_TRAVELLED));

        // Hard: unique player kills
        generated.add(generateMission(MissionDifficulty.HARD, MissionType.UNIQUE_PLAYER_KILLS));

        missionsDate.put(uuid, today);
        missions.put(uuid, generated);
    }

    private DailyMission generateMission(MissionDifficulty difficulty, MissionType forcedType) {
        switch (forcedType) {
            case KILL_MOBS -> {
                int target = switch (difficulty) {
                    case EASY -> 15;
                    case MEDIUM -> 30;
                    case HARD -> 45;
                };
                int reward = baseMissionReward(difficulty, target);
                String desc = "Kill " + target + " mobs.";
                return new DailyMission(MissionType.KILL_MOBS, difficulty, target, reward, desc, 0);
            }
            case UNIQUE_PLAYER_KILLS -> {
                int target = switch (difficulty) {
                    case EASY -> 2;
                    case MEDIUM -> 3;
                    case HARD -> 4;
                };
                int reward = baseMissionReward(difficulty, target * 10);
                String desc = "Kill " + target + " different players.";
                return new DailyMission(MissionType.UNIQUE_PLAYER_KILLS, difficulty, target, reward, desc, 0);
            }
            case DISTANCE_TRAVELLED -> {
                int targetBlocks = switch (difficulty) {
                    case EASY -> 250;
                    case MEDIUM -> 500;
                    case HARD -> 800;
                };
                int reward = baseMissionReward(difficulty, targetBlocks / 10);
                String desc = "Travel " + targetBlocks + " blocks on foot.";
                return new DailyMission(MissionType.DISTANCE_TRAVELLED, difficulty, targetBlocks, reward, desc, 0);
            }
            default -> throw new IllegalArgumentException("Unsupported mission type: " + forcedType);
        }
    }

    private int baseMissionReward(MissionDifficulty difficulty, int scale) {
        double mult = switch (difficulty) {
            case EASY -> 8.0;
            case MEDIUM -> 10.0;
            case HARD -> 12.0;
        };
        return (int) Math.max(100, Math.round(scale * mult));
    }

    public List<DailyMission> getMissions(Player player) {
        UUID uuid = player.getUniqueId();
        ensureMissionsFor(uuid);
        return missions.getOrDefault(uuid, Collections.emptyList());
    }

    public List<String> buildMissionLines(Player player) {
        List<String> lines = new ArrayList<>();
        List<DailyMission> list = getMissions(player);
        if (list.isEmpty()) {
            lines.add(ChatColor.GRAY + "You have no missions today.");
            return lines;
        }

        for (DailyMission m : list) {
            ChatColor col = m.getDifficulty().getColor();
            String diffLabel = m.getDifficulty().getLabel();
            String status = m.isCompleted()
                    ? ChatColor.AQUA + "Completed"
                    : ChatColor.YELLOW + (m.getProgress() + "/" + m.getTarget());
            lines.add(col + "[" + diffLabel + "] " +
                    ChatColor.WHITE + m.getDescription() +
                    ChatColor.GRAY + " | Progress: " + status +
                    ChatColor.GRAY + " | Reward: " + ChatColor.AQUA + m.getRewardYen() + "¥");
        }

        return lines;
    }

    // Progress update APIs ----------------------------------------------------

    public void onMobKill(Player killer) {
        UUID uuid = killer.getUniqueId();
        ensureMissionsFor(uuid);
        List<DailyMission> list = missions.get(uuid);
        if (list == null) return;

        boolean anyCompletedNow = false;

        for (DailyMission m : list) {
            if (m.getType() == MissionType.KILL_MOBS && !m.isCompleted()) {
                m.addProgress(1);
                if (m.isCompleted()) {
                    payMissionReward(killer, m);
                    anyCompletedNow = true;
                }
            }
        }

        if (anyCompletedNow) {
            killer.sendMessage(ChatColor.GOLD + "[Daily Mission] " + ChatColor.GREEN +
                    "You completed a mission! Use /mission to check your rewards.");
        }
    }

    public void onPlayerKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();
        ensureMissionsFor(uuid);

        Set<UUID> seen = uniqueKillsToday.computeIfAbsent(uuid, u -> new HashSet<>());
        if (!seen.add(victim.getUniqueId())) {
            // Already counted this victim today
            return;
        }

        List<DailyMission> list = missions.get(uuid);
        if (list == null) return;

        boolean anyCompletedNow = false;

        for (DailyMission m : list) {
            if (m.getType() == MissionType.UNIQUE_PLAYER_KILLS && !m.isCompleted()) {
                m.addProgress(1);
                if (m.isCompleted()) {
                    payMissionReward(killer, m);
                    anyCompletedNow = true;
                }
            }
        }

        if (anyCompletedNow) {
            killer.sendMessage(ChatColor.GOLD + "[Daily Mission] " + ChatColor.GREEN +
                    "You completed a mission! Use /mission to check your rewards.");
        }
    }

    public void onTravel(Player player, double horizontalDistance) {
        if (horizontalDistance <= 0) return;
        UUID uuid = player.getUniqueId();
        ensureMissionsFor(uuid);

        double buffer = distanceBuffer.getOrDefault(uuid, 0.0);
        buffer += horizontalDistance;
        int wholeBlocks = (int) buffer;
        if (wholeBlocks <= 0) {
            distanceBuffer.put(uuid, buffer);
            return;
        }
        buffer -= wholeBlocks;
        distanceBuffer.put(uuid, buffer);

        List<DailyMission> list = missions.get(uuid);
        if (list == null) return;

        boolean anyCompletedNow = false;

        for (DailyMission m : list) {
            if (m.getType() == MissionType.DISTANCE_TRAVELLED && !m.isCompleted()) {
                m.addProgress(wholeBlocks);
                if (m.isCompleted()) {
                    payMissionReward(player, m);
                    anyCompletedNow = true;
                }
            }
        }

        if (anyCompletedNow) {
            player.sendMessage(ChatColor.GOLD + "[Daily Mission] " + ChatColor.GREEN +
                    "You completed a mission! Use /mission to check your rewards.");
        }
    }

    private void payMissionReward(Player player, DailyMission mission) {
        economy.addYen(player, mission.getRewardYen());
        player.sendMessage(ChatColor.GOLD + "[Daily Mission] " + ChatColor.GREEN +
                "Completed: " + ChatColor.WHITE + mission.getDescription() +
                ChatColor.GREEN + ". You earned " + ChatColor.AQUA +
                mission.getRewardYen() + ChatColor.GREEN + " Yen!");
    }
}
