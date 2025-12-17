package com.animesmp.core.tutorial;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityTier;
import com.animesmp.core.player.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TutorialManager {

    public enum Step {
        STATS("tutorial.messages.stats", "Tutorial: Run /stats"),
        SKILLS("tutorial.messages.skills", "Tutorial: Open /skills menu"),
        ABILITY_VENDOR("tutorial.messages.abilityVendor", "Tutorial: Interact with Ability Vendor"),
        PD_VENDOR("tutorial.messages.pdVendor", "Tutorial: Interact with PD Vendor"),
        TRAINER("tutorial.messages.trainer", "Tutorial: Interact with Trainer"),
        LEARN_SCROLL("tutorial.messages.learnScroll", "Tutorial: Learn the Ability Scroll"),
        BIND("tutorial.messages.bind", "Tutorial: Run /bind"),
        CAST("tutorial.messages.cast", "Tutorial: Use /cast1..5"),
        COMPLETE(null, "Tutorial complete!");

        final String messagePath;
        final String bossbarText;

        Step(String messagePath, String bossbarText) {
            this.messagePath = messagePath;
            this.bossbarText = bossbarText;
        }
    }

    private final AnimeSMPPlugin plugin;

    private final Map<UUID, Step> progress = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossbars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastNoticeMs = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> announceTask = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> trailTask = new ConcurrentHashMap<>();

    public TutorialManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // COMPATIBILITY SHIM (called by NPC listeners)
    // -------------------------------------------------------------------------

    public void handleNpcInteraction(Player player, String npcTypeOrId) {
        if (player == null || npcTypeOrId == null) return;

        String t = npcTypeOrId.trim().toLowerCase(Locale.ROOT);
        if (t.contains("trainer")) {
            notifyTrainerInteracted(player, npcTypeOrId);
            return;
        }
        if (t.contains("pd")) {
            notifyShopNpcInteracted(player, "PD");
            return;
        }
        notifyShopNpcInteracted(player, "ABILITY");
    }

    // -------------------------------------------------------------------------
    // START API
    // -------------------------------------------------------------------------

    public void start(Player player) {
        start(player, false);
    }

    public void start(Player player, boolean force) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) return;

        PlayerProfile prof = plugin.getProfileManager().getProfile(player);
        boolean completed = (prof != null && prof.isTutorialCompleted());
        if (completed && !force) return;

        startInternal(player);
    }

    private void startInternal(Player player) {
        if (player == null) return;

        handleQuit(player);

        progress.put(player.getUniqueId(), Step.STATS);
        ensureBossbar(player);
        updateBossbar(player);

        showAnnouncement(player, "tutorial.messages.welcome");
        showAnnouncement(player, Step.STATS.messagePath);
        playNotice(player);

        refreshRepeatingForStep(player);
    }

    // -------------------------------------------------------------------------
    // JOIN / QUIT
    // -------------------------------------------------------------------------

    public void handleJoin(Player player) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) return;

        boolean auto = plugin.getConfig().getBoolean("tutorial.auto-start-on-first-join", true);
        if (!auto) return;

        PlayerProfile prof = plugin.getProfileManager().getProfile(player);
        if (prof != null && prof.isTutorialCompleted()) return;

        UUID id = player.getUniqueId();
        if (progress.containsKey(id)) return;

        Bukkit.getScheduler().runTask(plugin, () -> start(player, false));
    }

    public void handleQuit(Player player) {
        if (player == null) return;

        stopRepeatingAnnouncement(player);
        stopTrail(player);

        BossBar bar = bossbars.remove(player.getUniqueId());
        if (bar != null) bar.removePlayer(player);
    }

    // -------------------------------------------------------------------------
    // ADVANCE / FINISH
    // -------------------------------------------------------------------------

    private void advance(Player player, Step next) {
        if (player == null) return;

        progress.put(player.getUniqueId(), next);
        ensureBossbar(player);
        updateBossbar(player);

        playNotice(player);

        if (next.messagePath != null) showAnnouncement(player, next.messagePath);

        if (next == Step.LEARN_SCROLL) {
            giveRandomCommonScroll(player);
        }

        refreshRepeatingForStep(player);
    }

    private void finish(Player player) {
        if (player == null) return;

        UUID id = player.getUniqueId();

        stopRepeatingAnnouncement(player);
        stopTrail(player);

        progress.remove(id);

        PlayerProfile prof = plugin.getProfileManager().getProfile(player);
        if (prof != null) {
            prof.setTutorialCompleted(true);
            plugin.getProfileManager().saveProfile(prof);
        }

        BossBar bar = bossbars.remove(id);
        if (bar != null) bar.removePlayer(player);

        int bindDelay = Math.max(0, plugin.getConfig().getInt("tutorial.notice-delays.bind-mod-seconds", 3));
        int linksDelay = Math.max(0, plugin.getConfig().getInt("tutorial.notice-delays.completion-links-seconds", 4));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) sendBindModMessage(player);
        }, 20L * bindDelay);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) sendDiscordAndTrello(player);
        }, 20L * (bindDelay + linksDelay));
    }

    // -------------------------------------------------------------------------
    // COMMAND HANDLING
    // -------------------------------------------------------------------------

    public void handleCommandStep(Player player, String raw) {
        if (player == null || raw == null) return;
        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        String cmd = raw.trim().toLowerCase(Locale.ROOT);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String label = cmd.split("\\s+")[0];

        if (step == Step.STATS && label.equals("stats")) {
            advance(player, Step.SKILLS);
            return;
        }
        if (step == Step.SKILLS && (label.equals("skills") || label.equals("skill"))) {
            advance(player, Step.ABILITY_VENDOR);
        }
    }

    public void handleBindOrCastUsed(Player player, String lowerMsg) {
        if (player == null || lowerMsg == null) return;
        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        if (step == Step.BIND && lowerMsg.startsWith("/bind")) {
            advance(player, Step.CAST);
            return;
        }

        if (step == Step.CAST) {
            if (lowerMsg.startsWith("/cast1") || lowerMsg.startsWith("/cast2") || lowerMsg.startsWith("/cast3")
                    || lowerMsg.startsWith("/cast4") || lowerMsg.startsWith("/cast5")) {
                advance(player, Step.COMPLETE);
                finish(player);
            }
        }
    }

    // -------------------------------------------------------------------------
    // NPC INTERACTION HOOKS
    // -------------------------------------------------------------------------

    public void notifyShopNpcInteracted(Player player, String shopType) {
        if (player == null || shopType == null) return;
        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        String t = shopType.trim().toUpperCase(Locale.ROOT);

        if (step == Step.ABILITY_VENDOR && t.contains("ABILITY")) {
            advance(player, Step.PD_VENDOR);
            return;
        }

        if (step == Step.PD_VENDOR && t.contains("PD")) {
            advance(player, Step.TRAINER);
        }
    }

    public void notifyTrainerInteracted(Player player, String trainerId) {
        if (player == null) return;
        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        if (step == Step.TRAINER) {
            advance(player, Step.LEARN_SCROLL);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                Step now = progress.get(player.getUniqueId());
                if (now == Step.LEARN_SCROLL) {
                    advance(player, Step.BIND);
                }
            }, 20L * 4);
        }
    }

    // -------------------------------------------------------------------------
    // BOSSBAR
    // -------------------------------------------------------------------------

    private void ensureBossbar(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bossbars.get(id);

        if (bar == null) {
            String colorName = plugin.getConfig().getString("tutorial.bossbar.color", "RED");
            String styleName = plugin.getConfig().getString("tutorial.bossbar.style", "SOLID");

            BarColor color = BarColor.RED;
            BarStyle style = BarStyle.SOLID;
            try { color = BarColor.valueOf(colorName.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
            try { style = BarStyle.valueOf(styleName.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}

            bar = Bukkit.createBossBar("Tutorial", color, style);
            bar.addPlayer(player);
            bar.setVisible(true);
            bossbars.put(id, bar);
        } else if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
            bar.setVisible(true);
        }
    }

    private void updateBossbar(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bossbars.get(id);
        if (bar == null) return;

        Step step = progress.get(id);
        if (step == null) return;

        bar.setTitle(color("&c&l" + step.bossbarText));
        bar.setProgress(stepProgress(step));
    }

    private double stepProgress(Step step) {
        switch (step) {
            case STATS: return 0.10;
            case SKILLS: return 0.20;
            case ABILITY_VENDOR: return 0.35;
            case PD_VENDOR: return 0.50;
            case TRAINER: return 0.65;
            case LEARN_SCROLL: return 0.75;
            case BIND: return 0.85;
            case CAST: return 0.95;
            default: return 1.0;
        }
    }

    // -------------------------------------------------------------------------
    // CONSTANT ANNOUNCEMENTS + AUTO TRAIL
    // -------------------------------------------------------------------------

    private void refreshRepeatingForStep(Player player) {
        stopRepeatingAnnouncement(player);
        stopTrail(player);

        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        startRepeatingAnnouncement(player, step);

        if (step == Step.ABILITY_VENDOR) {
            startAutoTrail(player, AutoTrailTarget.ABILITY_VENDOR);
        } else if (step == Step.PD_VENDOR) {
            startAutoTrail(player, AutoTrailTarget.PD_VENDOR);
        } else if (step == Step.TRAINER) {
            startAutoTrail(player, AutoTrailTarget.TRAINER);
        }
    }

    private void startRepeatingAnnouncement(Player player, Step step) {
        if (player == null || step == null || step.messagePath == null) return;

        int periodTicks = Math.max(5, plugin.getConfig().getInt("tutorial.actionbar.period-ticks", 20));
        periodTicks = Math.min(periodTicks, 20);

        UUID id = player.getUniqueId();
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) {
                stopRepeatingAnnouncement(player);
                return;
            }
            Step now = progress.get(id);
            if (now != step) return;

            showAnnouncement(player, step.messagePath);
        }, 0L, periodTicks);

        announceTask.put(id, taskId);
    }

    private void stopRepeatingAnnouncement(Player player) {
        if (player == null) return;
        Integer task = announceTask.remove(player.getUniqueId());
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    private enum AutoTrailTarget { ABILITY_VENDOR, PD_VENDOR, TRAINER }

    private void startAutoTrail(Player player, AutoTrailTarget target) {
        if (player == null || target == null) return;

        if (!plugin.getConfig().getBoolean("tutorial.trail.enabled", true)) return;

        int period = Math.max(1, plugin.getConfig().getInt("tutorial.trail.period-ticks", 3));
        double maxDist = plugin.getConfig().getDouble("tutorial.trail.max-distance", 160.0);

        UUID id = player.getUniqueId();
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) {
                stopTrail(player);
                return;
            }

            Location dest = findNearestNpcLocation(player, target, maxDist);
            if (dest == null) return;

            Location from = player.getLocation().clone().add(0, 1.0, 0);
            Location to = dest.clone().add(0, 1.0, 0);

            drawLine(from, to);

        }, 0L, period);

        trailTask.put(id, taskId);
    }

    private void stopTrail(Player player) {
        if (player == null) return;
        Integer task = trailTask.remove(player.getUniqueId());
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    private Location findNearestNpcLocation(Player player, AutoTrailTarget target, double maxDist) {
        try {
            Class<?> citizensApi = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Method getNpcRegistry = citizensApi.getMethod("getNPCRegistry");
            Object registry = getNpcRegistry.invoke(null);

            Method iteratorMethod = registry.getClass().getMethod("iterator");
            @SuppressWarnings("unchecked")
            Iterator<Object> it = (Iterator<Object>) iteratorMethod.invoke(registry);

            Location best = null;
            double bestDist = Double.MAX_VALUE;

            while (it.hasNext()) {
                Object npc = it.next();
                if (npc == null) continue;

                String name = safeNpcName(npc);
                if (!matchesTargetByConfig(name, target)) continue;

                Location loc = safeNpcLocation(npc);
                if (loc == null || loc.getWorld() == null) continue;
                if (!loc.getWorld().equals(player.getWorld())) continue;

                double d = loc.distanceSquared(player.getLocation());
                if (d > (maxDist * maxDist)) continue;

                if (d < bestDist) {
                    bestDist = d;
                    best = loc;
                }
            }

            return best;

        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Config-driven matching:
     * - Reads tutorial.trail.npc-targets.<key>.match (preferred)
     * - Falls back to tutorial.particles.npc-targets.<key>.match
     * - Compares case-insensitive, color-stripped, exact OR substring match
     */
    private boolean matchesTargetByConfig(String npcNameRaw, AutoTrailTarget target) {
        String npcName = (npcNameRaw == null ? "" : npcNameRaw);
        npcName = ChatColor.stripColor(npcName);
        String n = npcName.trim().toLowerCase(Locale.ROOT);

        String key;
        switch (target) {
            case PD_VENDOR: key = "pd-vendor"; break;
            case TRAINER: key = "trainer"; break;
            case ABILITY_VENDOR:
            default: key = "ability-vendor"; break;
        }

        List<String> matches = plugin.getConfig().getStringList("tutorial.trail.npc-targets." + key + ".match");
        if (matches == null || matches.isEmpty()) {
            matches = plugin.getConfig().getStringList("tutorial.particles.npc-targets." + key + ".match");
        }

        if (matches != null && !matches.isEmpty()) {
            for (String m : matches) {
                if (m == null) continue;
                String mm = ChatColor.stripColor(m).trim().toLowerCase(Locale.ROOT);
                if (mm.isEmpty()) continue;

                // exact OR contains
                if (n.equals(mm) || n.contains(mm)) return true;
            }
            return false;
        }

        // Hard fallback if config missing (old behavior)
        switch (target) {
            case PD_VENDOR:
                return n.equals("pd_vendor") || n.contains("pd vendor") || n.contains("pd_vendor");
            case TRAINER:
                return n.contains("trainer");
            case ABILITY_VENDOR:
            default:
                return n.equals("ability_vendor") || n.contains("ability vendor") || n.contains("ability_vendor");
        }
    }

    private String safeNpcName(Object npc) {
        try {
            Method getName = npc.getClass().getMethod("getName");
            Object o = getName.invoke(npc);
            return o == null ? "" : String.valueOf(o);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private Location safeNpcLocation(Object npc) {
        // Prefer spawned entity location
        try {
            Method isSpawned = npc.getClass().getMethod("isSpawned");
            Object spawned = isSpawned.invoke(npc);
            if (spawned instanceof Boolean && (Boolean) spawned) {
                Method getEntity = npc.getClass().getMethod("getEntity");
                Object ent = getEntity.invoke(npc);
                if (ent instanceof Entity) {
                    return ((Entity) ent).getLocation();
                }
            }
        } catch (Throwable ignored) { }

        // Fallback: stored location
        try {
            Method getStoredLocation = npc.getClass().getMethod("getStoredLocation");
            Object loc = getStoredLocation.invoke(npc);
            if (loc instanceof Location) return (Location) loc;
        } catch (Throwable ignored) { }

        // Older fallback
        try {
            Method getEntity = npc.getClass().getMethod("getEntity");
            Object ent = getEntity.invoke(npc);
            if (ent instanceof Entity) return ((Entity) ent).getLocation();
        } catch (Throwable ignored) { }

        return null;
    }

    private void drawLine(Location from, Location to) {
        if (from == null || to == null) return;
        if (from.getWorld() == null || to.getWorld() == null) return;
        if (!from.getWorld().equals(to.getWorld())) return;

        Vector dir = to.toVector().subtract(from.toVector());
        double dist = dir.length();
        if (dist < 0.25) return;

        dir.normalize();

        int points = Math.max(30, plugin.getConfig().getInt("tutorial.trail.points", 22));
        double step = dist / points;
        if (step <= 0) step = 0.25;

        int r = plugin.getConfig().getInt("tutorial.trail.color.r", 255);
        int g = plugin.getConfig().getInt("tutorial.trail.color.g", 35);
        int b = plugin.getConfig().getInt("tutorial.trail.color.b", 35);
        float size = (float) plugin.getConfig().getDouble("tutorial.trail.color.size", 1.8);

        Color c = Color.fromRGB(clamp255(r), clamp255(g), clamp255(b));
        Particle.DustOptions dust = new Particle.DustOptions(c, size);

        World w = from.getWorld();
        Vector base = from.toVector();

        for (int i = 0; i <= points; i++) {
            Vector pos = base.clone().add(dir.clone().multiply(step * i));
            w.spawnParticle(Particle.DUST, pos.getX(), pos.getY(), pos.getZ(),
                    6, 0.03, 0.03, 0.03, 0, dust);
        }
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // -------------------------------------------------------------------------
    // SCROLL REWARD
    // -------------------------------------------------------------------------

    private void giveRandomCommonScroll(Player player) {
        if (player == null) return;

        try {
            List<Ability> commons = new ArrayList<>();
            for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
                if (a == null || a.getTier() == null) continue;

                if (a.getTier() == AbilityTier.SCROLL || a.getTier().name().toUpperCase(Locale.ROOT).contains("COMMON")) {
                    commons.add(a);
                }
            }
            if (commons.isEmpty()) return;

            Ability pick = commons.get(new Random().nextInt(commons.size()));
            player.getInventory().addItem(plugin.getAbilityManager().createAbilityScroll(pick, 1));

            player.sendMessage(color("&aYou received a random Common ability scroll."));
            player.sendMessage(color("&7Hold it and &fRight-Click&7 to learn it."));

        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // ANNOUNCEMENTS / LINKS
    // -------------------------------------------------------------------------

    private void playNotice(Player player) {
        long now = System.currentTimeMillis();
        long last = lastNoticeMs.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 600L) return;
        lastNoticeMs.put(player.getUniqueId(), now);

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.35f);
        } catch (Throwable ignored) {
        }
    }

    private void showAnnouncement(Player player, String path) {
        if (player == null || path == null) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;

        String title = color(sec.getString("title", ""));
        String subtitle = color(sec.getString("subtitle", ""));

        player.sendTitle(title, subtitle, 0, 20, 0);
    }

    private Component clickableLine(String word, String url) {
        return Component.text(word + " ", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url))
                .append(Component.text("(CLICK HERE)", NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url)));
    }

    private void sendBindModMessage(Player player) {
        String url = plugin.getConfig().getString("tutorial.links.bind-mod", "");
        if (url == null || url.isBlank()) return;

        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("BINDING TIP", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Install BindCommands mod for easier casting:", NamedTextColor.GRAY));
        player.sendMessage(clickableLine("BindCommands", url));
        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
    }

    private void sendDiscordAndTrello(Player player) {
        String discord = plugin.getConfig().getString("tutorial.links.discord", "");
        String trello = plugin.getConfig().getString("tutorial.links.trello", "");

        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("NEXT STEPS", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        if (discord != null && !discord.isBlank()) player.sendMessage(clickableLine("Discord", discord));
        if (trello != null && !trello.isBlank()) player.sendMessage(clickableLine("Trello", trello));
        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
