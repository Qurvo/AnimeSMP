package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.combat.StatusEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles PvP-facing mechanics that aren't cleanly expressed as vanilla potion effects.
 */
public class CombatStatusListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final StatusEffectManager status;

    public CombatStatusListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.status = plugin.getStatusEffectManager();
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (status.hasFallImmunity(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Entity damager = e.getDamager();
        if (!(damager instanceof Player attacker)) return;

        // Water Parry window: cancel incoming damage and punish attacker.
        if (status.isParrying(victim)) {
            e.setCancelled(true);

            // Stun attacker
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9, false, false, true));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 250, false, false, true));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 2, false, false, true));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, true));

            // True "chip" damage (1 heart) bypassing armor.
            plugin.getDamageCalculator().applyTrueDamage(victim, attacker, 2.0);

            victim.getWorld().spawnParticle(Particle.SPLASH, victim.getLocation().add(0, 1.0, 0), 25, 0.6, 0.4, 0.6, 0.05);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.8f);

            attacker.sendMessage(ChatColor.AQUA + "You were parried!");
            victim.sendMessage(ChatColor.AQUA + "Parry successful.");
            return;
        }

        // Flame Cloak retaliation: apply a true burn to attacker.
        if (status.hasFlameCloak(victim)) {
            applyTrueBurn(attacker, 5, 1.0);
        }
    }

    @EventHandler
    public void onBreakBarrier(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (status.isKiBarrierBlock(b.getLocation())) {
            e.setCancelled(true);
        }
    }

    private void applyTrueBurn(Player target, int seconds, double heartsTotal) {
        // Visual fire + DOT that bypasses fire resistance.
        target.setFireTicks(Math.max(target.getFireTicks(), seconds * 20));

        int ticks = seconds * 20;
        int period = 20;
        int runs = Math.max(1, ticks / period);
        double totalHp = heartsTotal * 2.0;
        double perTick = totalHp / runs;

        for (int i = 0; i < runs; i++) {
            int delay = i * period;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline()) return;
                plugin.getDamageCalculator().applyTrueDamage(null, target, perTick);
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1.0, 0), 8, 0.4, 0.5, 0.4, 0.02);
            }, delay);
        }
    }
}
