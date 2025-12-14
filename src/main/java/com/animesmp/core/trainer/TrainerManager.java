package com.animesmp.core.trainer;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class TrainerManager {

    private final AnimeSMPPlugin plugin;
    private final NamespacedKey trainerKey;

    // One spawn per TrainerType
    private final Map<TrainerType, TrainerSpawn> spawns = new EnumMap<>(TrainerType.class);

    private File trainersFile;
    private FileConfiguration trainersCfg;

    public TrainerManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.trainerKey = new NamespacedKey(plugin, "trainer_id");
        initConfig();
        loadSpawns();
    }

    // -------------------------------------------------
    // Config load/save
    // -------------------------------------------------

    private void initConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.trainersFile = new File(dataFolder, "trainers.yml");
        if (!trainersFile.exists()) {
            try {
                trainersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create trainers.yml");
                e.printStackTrace();
            }
        }
        this.trainersCfg = YamlConfiguration.loadConfiguration(trainersFile);
    }

    public void loadSpawns() {
        spawns.clear();

        if (!trainersCfg.isConfigurationSection("trainers")) {
            return;
        }

        for (String key : trainersCfg.getConfigurationSection("trainers").getKeys(false)) {
            TrainerType type = TrainerType.fromId(key);
            if (type == null) continue;

            String base = "trainers." + key + ".";
            String worldName = trainersCfg.getString(base + "world");
            double x = trainersCfg.getDouble(base + "x");
            double y = trainersCfg.getDouble(base + "y");
            double z = trainersCfg.getDouble(base + "z");
            float yaw = (float) trainersCfg.getDouble(base + "yaw");
            float pitch = (float) trainersCfg.getDouble(base + "pitch");

            spawns.put(type, new TrainerSpawn(worldName, x, y, z, yaw, pitch));
        }
    }

    public void saveSpawns() {
        trainersCfg.set("trainers", null);

        for (Map.Entry<TrainerType, TrainerSpawn> entry : spawns.entrySet()) {
            TrainerType type = entry.getKey();
            TrainerSpawn spawn = entry.getValue();
            String base = "trainers." + type.getId() + ".";
            trainersCfg.set(base + "world", spawn.worldName);
            trainersCfg.set(base + "x", spawn.x);
            trainersCfg.set(base + "y", spawn.y);
            trainersCfg.set(base + "z", spawn.z);
            trainersCfg.set(base + "yaw", spawn.yaw);
            trainersCfg.set(base + "pitch", spawn.pitch);
        }

        try {
            trainersCfg.save(trainersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trainers.yml");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------
    // Startup: respawn trainers
    // -------------------------------------------------

    /**
     * Called on enable after loadSpawns():
     * ensures each saved trainer has an NPC at its location.
     */
    public void spawnAllFromConfig() {
        for (Map.Entry<TrainerType, TrainerSpawn> entry : spawns.entrySet()) {
            TrainerType type = entry.getKey();
            TrainerSpawn spawn = entry.getValue();
            ensureTrainerEntity(type, spawn);
        }
    }

    private void ensureTrainerEntity(TrainerType type, TrainerSpawn spawn) {
        World world = Bukkit.getWorld(spawn.worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + spawn.worldName + " for trainer " + type.getId() + " not found.");
            return;
        }

        Location loc = new Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);

        // If a trainer with this ID is already nearby, do nothing.
        for (Entity e : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity) {
                PersistentDataContainer pdc = e.getPersistentDataContainer();
                String id = pdc.get(trainerKey, PersistentDataType.STRING);
                if (id != null && id.equalsIgnoreCase(type.getId())) {
                    return;
                }
            }
        }

        spawnTrainerEntity(type, loc);
    }

    private void spawnTrainerEntity(TrainerType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Villager villager = world.spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setCustomNameVisible(true);
            v.setCustomName(type.getDisplayName());
            v.setProfession(Villager.Profession.LIBRARIAN);
            v.setCollidable(false);
        });

        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(trainerKey, PersistentDataType.STRING, type.getId());

        plugin.getLogger().info("Spawned trainer " + type.getId() + " at " +
                loc.getWorld().getName() + " (" +
                loc.getBlockX() + ", " +
                loc.getBlockY() + ", " +
                loc.getBlockZ() + ")");
    }

    // -------------------------------------------------
    // Manual admin spawn/remove
    // -------------------------------------------------

    /**
     * Spawn (or move) a trainer of the given type at the player's location.
     * Overwrites any previous saved spawn for that type.
     */
    public boolean spawnTrainerAt(Player admin, TrainerType type) {
        Location loc = admin.getLocation();

        // Remove any existing NPC of that trainer type
        removeTrainerEntity(type);

        spawns.put(type, new TrainerSpawn(
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        ));
        saveSpawns();

        spawnTrainerEntity(type, loc);
        return true;
    }

    /**
     * Remove trainer of this type (NPC + saved spawn).
     */
    public boolean removeTrainer(TrainerType type) {
        TrainerSpawn removed = spawns.remove(type);
        saveSpawns();
        boolean hadNpc = removeTrainerEntity(type);
        return removed != null || hadNpc;
    }

    private boolean removeTrainerEntity(TrainerType type) {
        boolean removed = false;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity)) continue;
                PersistentDataContainer pdc = e.getPersistentDataContainer();
                String id = pdc.get(trainerKey, PersistentDataType.STRING);
                if (id != null && id.equalsIgnoreCase(type.getId())) {
                    e.remove();
                    removed = true;
                }
            }
        }
        return removed;
    }

    public Map<TrainerType, TrainerSpawn> getSpawns() {
        return Collections.unmodifiableMap(spawns);
    }

    public NamespacedKey getTrainerKey() {
        return trainerKey;
    }

    // -------------------------------------------------
    // Aura task (visual + sound hint)
    // -------------------------------------------------

    public void startAuraTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (!(e instanceof LivingEntity)) continue;
                    PersistentDataContainer pdc = e.getPersistentDataContainer();
                    String id = pdc.get(trainerKey, PersistentDataType.STRING);
                    if (id == null) continue;

                    Location loc = e.getLocation().add(0, 1.8, 0);

                    // Particles above head
                    world.spawnParticle(
                            Particle.ENCHANT,
                            loc,
                            10,
                            0.3, 0.5, 0.3,
                            0.0
                    );

                    // Soft beacon-like sound for nearby players
                    for (Player p : world.getPlayers()) {
                        if (p.getLocation().distanceSquared(e.getLocation()) <= 12 * 12) {
                            p.playSound(
                                    e.getLocation(),
                                    Sound.BLOCK_BEACON_AMBIENT,
                                    0.35f,
                                    1.6f
                            );
                        }
                    }
                }
            }
        }, 40L, 40L); // every 2 seconds
    }

    // -------------------------------------------------
    // Inner class
    // -------------------------------------------------

    public static class TrainerSpawn {
        public final String worldName;
        public final double x, y, z;
        public final float yaw, pitch;

        public TrainerSpawn(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Location toLocation() {
            World w = Bukkit.getWorld(worldName);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }
}
