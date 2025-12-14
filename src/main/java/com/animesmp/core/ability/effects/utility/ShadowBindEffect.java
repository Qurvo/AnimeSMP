package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ShadowBindEffect implements AbilityEffect {

    private static final double RANGE = 10.0;
    private static final int DURATION_TICKS = 4 * 20;

    @Override
    public void execute(Player player, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null) return;

        LivingEntity target = null;
        double closest = RANGE + 1;

        for (Entity e : player.getNearbyEntities(RANGE, RANGE, RANGE)) {
            if (e instanceof LivingEntity && e != player) {
                double dist = e.getLocation().distanceSquared(player.getLocation());
                if (dist < closest * closest) {
                    closest = Math.sqrt(dist);
                    target = (LivingEntity) e;
                }
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target to bind.");
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION_TICKS, 5, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, DURATION_TICKS, 128, false, false, false)); // basically no jumping
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));

        // Silence blocks the target from casting for the bind duration.
        if (target instanceof Player tp) {
            plugin.getStatusEffectManager().silence(tp, DURATION_TICKS * 50L);
        }

        target.getWorld().spawnParticle(
                Particle.SMOKE,
                target.getLocation().add(0, 1.0, 0),
                25,
                0.5, 1.0, 0.5,
                0.01
        );

        target.getWorld().playSound(
                target.getLocation(),
                Sound.ENTITY_ENDERMAN_STARE,
                0.9f,
                0.5f
        );

        player.sendMessage(ChatColor.DARK_GRAY + "You bind " + ChatColor.RED + target.getName() + ChatColor.DARK_GRAY + " in your shadow.");
    }
}
