package com.animesmp.core.ability.util;

import com.animesmp.core.AnimeSMPPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class DangerTelegraphUtil {

    private DangerTelegraphUtil() {}

    public static void telegraph(Player caster, int ticks) {
        if (caster == null) return;
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        Location origin = caster.getLocation().clone();

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (!caster.isOnline() || caster.isDead()) { cancel(); return; }
                if (i++ >= ticks) { cancel(); return; }

                caster.getWorld().spawnParticle(Particle.DUST_PLUME, origin.clone().add(0, 0.15, 0), 18, 1.0, 0.05, 1.0, 0.0);
                caster.getWorld().spawnParticle(Particle.FLAME, origin.clone().add(0, 0.25, 0), 6, 0.8, 0.05, 0.8, 0.0);
                caster.getWorld().playSound(origin, Sound.UI_BUTTON_CLICK, 0.25f, 0.6f);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
