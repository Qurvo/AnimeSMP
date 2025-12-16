package com.animesmp.core.shop.rotation;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

/**
 * Daily Ability Vendor rotation manager.
 *
 * ShopGuiManager expects: List<String> ability ids from getCurrentRotation().
 *
 * Pools:
 * - COMMON stock: by default includes tier COMMON AND (optionally) tier SCROLL if you want commons to appear.
 * - RARE stock:   tier VENDOR
 * - EPIC stock:   tier TRAINER (very low chance)
 */
public class RotatingVendorManager {

    private final AnimeSMPPlugin plugin;

    private LocalDate rotationDay;
    private final List<String> currentRotationIds = new ArrayList<>();

    public RotatingVendorManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        ensureCurrentRotation();
    }

    public void openDailyVendor(Player player) {
        // Your ShopNpcListener now opens directly, but keep method for legacy callers.
        ensureCurrentRotation();
        Object shopGui = plugin.getShopGuiManager();
        if (shopGui == null || player == null) return;
        invoke(shopGui, "openDailyAbilityVendor", player);
        invoke(shopGui, "openDailyVendor", player);
        invoke(shopGui, "openAbilityVendor", player);
    }

    public List<String> getCurrentRotation() {
        ensureCurrentRotation();
        return Collections.unmodifiableList(currentRotationIds);
    }

    public String getFormattedTimeRemaining() {
        Duration d = timeUntilNextReset();
        long seconds = Math.max(0, d.getSeconds());
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    public void forceRefreshNow() {
        rotationDay = null;
        currentRotationIds.clear();
        ensureCurrentRotation();
    }

    // -------------------------------------------------------------------------

    private void ensureCurrentRotation() {
        LocalDate today = LocalDate.now(getVendorZone());
        if (rotationDay != null && rotationDay.equals(today) && !currentRotationIds.isEmpty()) return;
        rotationDay = today;
        rollRotation();
    }

    private void rollRotation() {
        currentRotationIds.clear();

        // Config knobs
        int size = Math.max(6, plugin.getConfig().getInt("ability-vendor.stock-size", 9));
        int wCommon = plugin.getConfig().getInt("ability-vendor.weights.common", 85);
        int wRare   = plugin.getConfig().getInt("ability-vendor.weights.rare", 14);
        int wEpic   = plugin.getConfig().getInt("ability-vendor.weights.epic", 1);

        // IMPORTANT: Your "commons" in config are mostly tier: SCROLL.
        boolean includeScrollAsCommon = plugin.getConfig().getBoolean("ability-vendor.common-includes-scroll", true);

        // Guarantee at least N commons so the vendor always feels “common-heavy”
        int minCommons = Math.max(0, plugin.getConfig().getInt("ability-vendor.min-commons", 4));

        List<Ability> commons = new ArrayList<>();
        List<Ability> rares = new ArrayList<>();
        List<Ability> epics = new ArrayList<>();

        for (Ability a : plugin.getAbilityRegistry().getAllAbilities()) {
            if (a == null || a.getTier() == null) continue;

            String tier = a.getTier().name().toUpperCase(Locale.ROOT);

            if (tier.contains("COMMON")) {
                commons.add(a);
                continue;
            }

            if (includeScrollAsCommon && tier.contains("SCROLL")) {
                // Treat SCROLL as common for vendor rotation purposes
                commons.add(a);
                continue;
            }

            if (tier.contains("VENDOR")) {
                rares.add(a);
                continue;
            }

            if (tier.contains("TRAINER")) {
                epics.add(a);
            }
        }

        Random rng = new Random();
        Set<String> picked = new HashSet<>();

        // 1) Force minimum commons first
        int safety = 0;
        while (currentRotationIds.size() < size && currentRotationIds.size() < minCommons && safety++ < 300) {
            Ability a = commons.isEmpty() ? null : commons.get(rng.nextInt(commons.size()));
            if (a == null) break;
            if (!picked.add(a.getId())) continue;
            currentRotationIds.add(a.getId());
        }

        // 2) Fill remaining using weights
        int maxAttempts = 600;
        int attempts = 0;

        while (currentRotationIds.size() < size && attempts++ < maxAttempts) {
            Ability chosen = weightedPick(rng, commons, rares, epics, wCommon, wRare, wEpic);
            if (chosen == null) continue;
            if (!picked.add(chosen.getId())) continue;
            currentRotationIds.add(chosen.getId());
        }
    }

    private Ability weightedPick(Random rng,
                                 List<Ability> commons, List<Ability> rares, List<Ability> epics,
                                 int wCommon, int wRare, int wEpic) {

        int a = Math.max(0, wCommon);
        int b = Math.max(0, wRare);
        int c = Math.max(0, wEpic);
        int total = a + b + c;
        if (total <= 0) total = 1;

        int roll = rng.nextInt(total);

        if (roll < a && !commons.isEmpty()) return commons.get(rng.nextInt(commons.size()));
        roll -= a;

        if (roll < b && !rares.isEmpty()) return rares.get(rng.nextInt(rares.size()));
        roll -= b;

        if (roll < c && !epics.isEmpty()) return epics.get(rng.nextInt(epics.size()));

        // fallback
        if (!commons.isEmpty()) return commons.get(rng.nextInt(commons.size()));
        if (!rares.isEmpty()) return rares.get(rng.nextInt(rares.size()));
        if (!epics.isEmpty()) return epics.get(rng.nextInt(epics.size()));
        return null;
    }

    private Duration timeUntilNextReset() {
        ZoneId zone = getVendorZone();
        int resetHour = plugin.getConfig().getInt("vendor-reset.hour", 8);
        int resetMin  = plugin.getConfig().getInt("vendor-reset.minute", 0);

        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(resetHour).withMinute(resetMin).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);

        return Duration.between(now, next);
    }

    private ZoneId getVendorZone() {
        String zone = plugin.getConfig().getString("vendor-reset.zone", "Europe/Lisbon");
        try { return ZoneId.of(zone); } catch (Exception ignored) { return ZoneId.of("Europe/Lisbon"); }
    }

    private boolean invoke(Object target, String methodName, Player player) {
        try {
            Method m = target.getClass().getMethod(methodName, Player.class);
            m.invoke(target, player);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
