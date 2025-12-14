package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.combat.DamageTagUtil;
import com.animesmp.core.combat.UltimateBarManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Charges ultimate bar from PvP damage and kill events.
 */
public class UltimateChargeListener implements Listener {

    private final AnimeSMPPlugin plugin;

    public UltimateChargeListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        UltimateBarManager ub = plugin.getUltimateBarManager();
        if (ub == null || !ub.isEnabled()) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        if (!(damager instanceof Player attacker)) return;
        if (!(victim instanceof Player target)) return;

        // If this damage originated from an ability call, ignore it here
        if (DamageTagUtil.isAbilityDamage(target)) return;

        double dmg = event.getFinalDamage();
        if (dmg <= 0.0) return;

        ub.onMeleeDamage(attacker, target, dmg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        UltimateBarManager ub = plugin.getUltimateBarManager();
        if (ub == null || !ub.isEnabled()) return;

        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer != null) {
            ub.onPlayerKill(killer);
        }
    }
}
