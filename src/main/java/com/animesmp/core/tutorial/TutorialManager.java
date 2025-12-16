package com.animesmp.core.tutorial;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TutorialManager {

    public enum Step {
        STATS(null, "tutorial.messages.stats", "Tutorial: Run /stats"),
        SKILLS(null, "tutorial.messages.skills", "Tutorial: Run /skills"),
        ABILITY_VENDOR(TargetKind.ABILITY_VENDOR, "tutorial.messages.abilityVendor", "Tutorial: Find the Ability Vendor"),
        PD_VENDOR(TargetKind.PD_VENDOR, "tutorial.messages.pdVendor", "Tutorial: Find the PD Vendor"),
        TRAINER(TargetKind.TRAINER, "tutorial.messages.trainer", "Tutorial: Talk to a Trainer"),
        BIND(null, "tutorial.messages.bind", "Tutorial: Bind an ability (/bind)"),
        CAST(null, "tutorial.messages.cast", "Tutorial: Cast an ability (/cast1..5)"),
        COMPLETE(null, null, "Tutorial complete!");

        final TargetKind targetKind;
        final String messagePath;
        final String bossbarText;

        Step(TargetKind targetKind, String messagePath, String bossbarText) {
            this.targetKind = targetKind;
            this.messagePath = messagePath;
            this.bossbarText = bossbarText;
        }
    }

    private enum TargetKind {
        ABILITY_VENDOR,
        PD_VENDOR,
        TRAINER
    }

    private final AnimeSMPPlugin plugin;

    private final Map<UUID, Step> progress = new ConcurrentHashMap<>();
    private final Set<UUID> completed = ConcurrentHashMap.newKeySet();

    private final Map<UUID, BossBar> bossbars = new ConcurrentHashMap<>();

    private final Set<UUID> actionbarRunning = ConcurrentHashMap.newKeySet();
    private BukkitRunnable actionbarTask;

    private BukkitRunnable trailTask;

    public TutorialManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        startTrailTask();
        startActionbarTask();
    }

    // -----------------------------------------------------------------------
    // START API
    // -----------------------------------------------------------------------

    /** Backwards compatible */
    public void start(Player player) {
        start(player, false);
    }

    public void start(Player player, boolean force) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) return;

        UUID id = player.getUniqueId();
        boolean allowReplay = plugin.getConfig().getBoolean("tutorial.allow-replay", true);

        if (completed.contains(id) && !(allowReplay && force)) return;

        progress.put(id, Step.STATS);

        ensureBossbar(player);
        updateBossbar(player);

        startActionbar(player);

        showAnnouncement(player, "tutorial.messages.welcome");
        showAnnouncement(player, Step.STATS.messagePath);
        playNotice(player);
    }

    public void handleJoin(Player player) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) return;

        boolean auto = plugin.getConfig().getBoolean("tutorial.auto-start-on-first-join", true);
        UUID id = player.getUniqueId();

        if (auto && !completed.contains(id) && !progress.containsKey(id)) {
            Bukkit.getScheduler().runTask(plugin, () -> start(player, false));
        }
    }

    public boolean isInTutorial(Player player) {
        return player != null && progress.containsKey(player.getUniqueId());
    }

    // -----------------------------------------------------------------------
    // COMPATIBILITY METHODS (WHAT YOUR LISTENERS EXPECT)
    // -----------------------------------------------------------------------

    public void handleCommandStep(Player player, String commandLabel) {
        if (player == null || commandLabel == null) return;
        handlePlayerCommand(player, commandLabel);
    }

    public void handleBindOrCastUsed(Player player) {
        if (player == null) return;

        Step step = progress.get(player.getUniqueId());
        if (step == null) return;

        if (step == Step.BIND) {
            advance(player, Step.CAST);
            return;
        }

        if (step == Step.CAST) {
            advance(player, Step.COMPLETE);
            finish(player);
        }
    }

    public void handleNpcInteraction(Player player, String npcType) {
        if (player == null || npcType == null) return;

        String t = npcType.trim().toUpperCase(Locale.ROOT);

        if (t.contains("TRAINER")) {
            notifyTrainerInteracted(player, "");
            return;
        }

        notifyShopNpcInteracted(player, t);
    }

    // -----------------------------------------------------------------------
    // INTERNAL HANDLERS
    // -----------------------------------------------------------------------

    public void handlePlayerCommand(Player player, String rawCommandLabelOrCommand) {
        if (player == null || rawCommandLabelOrCommand == null) return;

        UUID id = player.getUniqueId();
        Step step = progress.get(id);
        if (step == null) return;

        String cmd = rawCommandLabelOrCommand.trim().toLowerCase(Locale.ROOT);
        if (cmd.isEmpty()) return;

        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        String label = cmd.split("\\s+")[0];

        if (step == Step.STATS && label.equals("stats")) {
            advance(player, Step.SKILLS);
            return;
        }

        if (step == Step.SKILLS && label.equals("skills")) {
            advance(player, Step.ABILITY_VENDOR);
            return;
        }

        if (step == Step.BIND && label.equals("bind")) {
            advance(player, Step.CAST);
            return;
        }

        if (step == Step.CAST) {
            if (label.matches("cast[1-5]") || label.equals("castselected")) {
                advance(player, Step.COMPLETE);
                finish(player);
            }
        }
    }

    public void notifyShopNpcInteracted(Player player, String shopType) {
        if (player == null || shopType == null) return;

        UUID id = player.getUniqueId();
        Step step = progress.get(id);
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

        UUID id = player.getUniqueId();
        Step step = progress.get(id);
        if (step == null) return;

        if (step == Step.TRAINER) {
            advance(player, Step.BIND);

            int delaySec = plugin.getConfig().getInt("tutorial.notice-delays.bind-mod-seconds", 3);
            long delayTicks = Math.max(0, delaySec) * 20L;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (progress.get(player.getUniqueId()) != Step.BIND) return;
                playNotice(player);
                sendBindModMessage(player);
            }, delayTicks);
        }
    }

    // -----------------------------------------------------------------------
    // ADVANCE / FINISH
    // -----------------------------------------------------------------------

    private void advance(Player player, Step next) {
        progress.put(player.getUniqueId(), next);

        ensureBossbar(player);
        updateBossbar(player);

        playNotice(player);

        if (next.messagePath != null) {
            showAnnouncement(player, next.messagePath);
        }
    }

    private void finish(Player player) {
        UUID id = player.getUniqueId();

        progress.remove(id);
        completed.add(id);

        stopActionbar(player);

        BossBar bar = bossbars.remove(id);
        if (bar != null) bar.removePlayer(player);

        if (plugin.getConfig().getBoolean("tutorial.reward.give-random-common", true)) {
            giveRandomCommonScroll(player);
        }

        int delaySec = plugin.getConfig().getInt("tutorial.notice-delays.completion-links-seconds", 4);
        long delayTicks = Math.max(0, delaySec) * 20L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            playNotice(player);
            sendCompletionLinks(player);
        }, delayTicks);
    }

    private void giveRandomCommonScroll(Player player) {
        try {
            List<Ability> commons = new ArrayList<>();
            for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
                if (a == null) continue;
                if (a.getTier() == AbilityTier.SCROLL) commons.add(a);
            }
            if (commons.isEmpty()) return;

            Ability pick = commons.get(new Random().nextInt(commons.size()));
            player.getInventory().addItem(plugin.getAbilityManager().createAbilityScroll(pick, 1));
        } catch (Throwable ignored) {}
    }

    // -----------------------------------------------------------------------
    // ANNOUNCEMENTS + NOTICE
    // -----------------------------------------------------------------------

    private void playNotice(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.35f);
        } catch (Throwable ignored) {}
    }

    private void showAnnouncement(Player player, String path) {
        if (player == null || path == null) return;

        String mode = plugin.getConfig().getString("tutorial.messages.mode", "TITLE").toUpperCase(Locale.ROOT);

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;

        String title = color(sec.getString("title", ""));
        String subtitle = color(sec.getString("subtitle", ""));

        if ((mode.equals("TITLE") || mode.equals("BOTH")) && (!title.isEmpty() || !subtitle.isEmpty())) {
            player.sendTitle(title, subtitle, 10, 60, 10);
        }

        if (mode.equals("CHAT") || mode.equals("BOTH")) {
            if (!title.isEmpty()) player.sendMessage(title);
            if (!subtitle.isEmpty()) player.sendMessage(subtitle);
        }
    }

    private Component bigLinkLine(String label, String url) {
        return Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text(label, NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url)));
    }

    private void sendBindModMessage(Player player) {
        String url = plugin.getConfig().getString("tutorial.links.bind-mod", "");
        if (url == null || url.isBlank()) return;

        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("KEYBINDS RECOMMENDED", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Install this mod for easier casting:", NamedTextColor.GRAY));
        player.sendMessage(bigLinkLine("Bind Commands Mod (click)", url));
        player.sendMessage(Component.text("Tip: bind /cast1..5 or /castselected to keys.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
    }

    private void sendCompletionLinks(Player player) {
        String discord = plugin.getConfig().getString("tutorial.links.discord", "");
        String trello = plugin.getConfig().getString("tutorial.links.trello", "");

        String raw = plugin.getConfig().getString(
                "tutorial.completion.chat-message",
                "&aGreat job! Tutorial complete. Don't forget to join the &bDiscord&a and check the &bTrello&a for more information."
        );
        player.sendMessage(color(raw));

        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("NEXT STEPS", NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

        if (discord != null && !discord.isBlank()) {
            player.sendMessage(bigLinkLine("Join the Discord", discord));
        } else {
            player.sendMessage(Component.text("» Discord link not set.", NamedTextColor.DARK_GRAY));
        }

        if (trello != null && !trello.isBlank()) {
            player.sendMessage(bigLinkLine("Open the Trello", trello));
        } else {
            player.sendMessage(Component.text("» Trello link not set.", NamedTextColor.DARK_GRAY));
        }

        player.sendMessage(Component.text("────────────────────────────────", NamedTextColor.DARK_GRAY));
    }

    // -----------------------------------------------------------------------
    // BOSSBAR
    // -----------------------------------------------------------------------

    private void ensureBossbar(Player player) {
        UUID id = player.getUniqueId();
        if (bossbars.containsKey(id)) return;

        BarColor color = BarColor.RED;
        String c = plugin.getConfig().getString("tutorial.bossbar.color", "RED");
        try { color = BarColor.valueOf(c.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}

        BarStyle style = BarStyle.SOLID;
        String s = plugin.getConfig().getString("tutorial.bossbar.style", "SOLID");
        try { style = BarStyle.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}

        BossBar bar = Bukkit.createBossBar("", color, style);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        bossbars.put(id, bar);
    }

    private void updateBossbar(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bossbars.get(id);
        if (bar == null) return;

        Step step = progress.get(id);
        if (step == null) return;

        bar.setTitle(step.bossbarText);
        bar.setProgress(1.0);
    }

    // -----------------------------------------------------------------------
    // ACTIONBAR LOOP
    // -----------------------------------------------------------------------

    private void startActionbarTask() {
        if (actionbarTask != null) actionbarTask.cancel();

        int period = plugin.getConfig().getInt("tutorial.actionbar.period-ticks", 20);

        actionbarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("tutorial.actionbar.enabled", true)) return;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!actionbarRunning.contains(p.getUniqueId())) continue;

                    Step step = progress.get(p.getUniqueId());
                    if (step == null) continue;

                    String msg = switch (step) {
                        case STATS -> "Run /stats";
                        case SKILLS -> "Run /skills";
                        case ABILITY_VENDOR -> "Find & interact with the Ability Vendor";
                        case PD_VENDOR -> "Find & interact with the PD Vendor";
                        case TRAINER -> "Find & talk to a Trainer";
                        case BIND -> "Bind an ability: /bind 1 <abilityId>";
                        case CAST -> "Cast: /cast1..5 (or /castselected)";
                        default -> "";
                    };

                    if (!msg.isEmpty()) {
                        p.sendActionBar(Component.text(msg, NamedTextColor.YELLOW));
                    }
                }
            }
        };

        actionbarTask.runTaskTimer(plugin, 20L, Math.max(1, period));
    }

    private void startActionbar(Player player) {
        actionbarRunning.add(player.getUniqueId());
    }

    private void stopActionbar(Player player) {
        actionbarRunning.remove(player.getUniqueId());
    }

    // -----------------------------------------------------------------------
    // PARTICLE TRAIL LOOP
    // -----------------------------------------------------------------------

    private void startTrailTask() {
        if (trailTask != null) trailTask.cancel();

        int period = plugin.getConfig().getInt("tutorial.trail.period-ticks", 5);

        trailTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("tutorial.trail.enabled", true)) return;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    Step step = progress.get(p.getUniqueId());
                    if (step == null) continue;
                    if (step.targetKind == null) continue;

                    Location target = resolveTargetLocation(p, step.targetKind);
                    if (target == null) continue;

                    drawTrail(p, target);
                }
            }
        };

        trailTask.runTaskTimer(plugin, 20L, Math.max(1, period));
    }

    private Location resolveTargetLocation(Player player, TargetKind kind) {
        double maxSearch = plugin.getConfig().getDouble("tutorial.particles.max-search-distance", 200.0);

        if (kind == TargetKind.ABILITY_VENDOR || kind == TargetKind.PD_VENDOR) {
            String want = (kind == TargetKind.ABILITY_VENDOR) ? "ABILITY_VENDOR" : "PD_VENDOR";

            Location best = null;
            double bestDist = Double.MAX_VALUE;

            for (Entity e : player.getNearbyEntities(maxSearch, maxSearch, maxSearch)) {
                PersistentDataContainer pdc = e.getPersistentDataContainer();
                String type = pdc.get(plugin.getShopNpcKey(), PersistentDataType.STRING);
                if (type == null) continue;
                if (!type.equalsIgnoreCase(want)) continue;

                double d = e.getLocation().distanceSquared(player.getLocation());
                if (d < bestDist) {
                    bestDist = d;
                    best = e.getLocation();
                }
            }
            return best;
        }

        if (kind == TargetKind.TRAINER) {
            Location best = null;
            double bestDist = Double.MAX_VALUE;

            for (Entity e : player.getNearbyEntities(maxSearch, maxSearch, maxSearch)) {
                PersistentDataContainer pdc = e.getPersistentDataContainer();
                String trainerId = pdc.get(plugin.getTrainerManager().getTrainerKey(), PersistentDataType.STRING);
                if (trainerId == null) continue;

                double d = e.getLocation().distanceSquared(player.getLocation());
                if (d < bestDist) {
                    bestDist = d;
                    best = e.getLocation();
                }
            }
            return best;
        }

        return null;
    }

    private void drawTrail(Player player, Location target) {
        World w = player.getWorld();

        double maxDist = plugin.getConfig().getDouble("tutorial.trail.max-distance", 160.0);
        if (player.getLocation().distance(target) > maxDist) return;

        int points = plugin.getConfig().getInt("tutorial.trail.points", 22);
        points = Math.max(6, Math.min(points, 120));

        ConfigurationSection col = plugin.getConfig().getConfigurationSection("tutorial.trail.color");
        int r = col != null ? col.getInt("r", 255) : 255;
        int g = col != null ? col.getInt("g", 35) : 35;
        int b = col != null ? col.getInt("b", 35) : 35;
        float size = col != null ? (float) col.getDouble("size", 1.8) : 1.8f;

        Location start = player.getLocation().add(0, 1.0, 0);
        Location end = target.clone().add(0, 1.0, 0);

        Vector dir = end.toVector().subtract(start.toVector());
        if (dir.lengthSquared() < 0.0001) return;

        Vector step = dir.clone().multiply(1.0 / points);

        for (int i = 0; i <= points; i++) {
            Location pointLoc = start.clone().add(step.clone().multiply(i));
            w.spawnParticle(
                    Particle.DUST,
                    pointLoc,
                    1,
                    0, 0, 0,
                    0,
                    new Particle.DustOptions(Color.fromRGB(clamp(r), clamp(g), clamp(b)), size)
            );
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private String color(String s) {
        if (s == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
