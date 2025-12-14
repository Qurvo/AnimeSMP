package com.animesmp.core.ability.effects;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.combat.DamageCalculator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Generic fallback effect:
 * - Does an anime-style forward slash with particles.
 * - Uses DamageCalculator to deal scaled damage to the first target in front.
 * Any ability ID without a custom effect will use this.
 */
public class GenericAbilityEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {
        if (caster == null || ability == null) return;

        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null || plugin.getDamageCalculator() == null) {
            // Failsafe: just play a sound/particles if for some reason damage calc isn't ready.
            simpleVisualOnly(caster);
            return;
        }

        DamageCalculator calc = plugin.getDamageCalculator();

        // --- 1) Visual slash line in front of the player ---
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        double maxRange = 8.0;
        int steps = 8;

        for (int i = 1; i <= steps; i++) {
            Location point = eye.clone().add(dir.clone().multiply((maxRange / steps) * i));
            caster.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    point,
                    1,
                    0.1, 0.1, 0.1,
                    0.0
            );
        }

        // --- 2) Find first target in line and apply ability damage ---
        LivingEntity target = calc.findFirstTargetInLine(caster, maxRange, 1.5);
        if (target != null) {
            calc.applyAbilityDamage(caster, target, ability);
            caster.getWorld().playSound(
                    target.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                    1.0f,
                    1.2f
            );
            caster.sendMessage(ChatColor.GREEN + "You hit " +
                    ChatColor.YELLOW + target.getName() +
                    ChatColor.GREEN + " with " +
                    ChatColor.AQUA + ability.getDisplayName() + ChatColor.GREEN + "!");
        } else {
            // Miss feedback
            caster.getWorld().playSound(
                    caster.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                    0.7f,
                    0.8f
            );
            caster.sendMessage(ChatColor.GRAY + "Your " +
                    ChatColor.AQUA + ability.getDisplayName() +
                    ChatColor.GRAY + " missed.");
        }
    }

    private void simpleVisualOnly(Player caster) {
        Location loc = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 8, 0.5, 0.5, 0.5, 0.01);
        caster.getWorld().playSound(
                loc,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                0.8f,
                1.0f
        );
    }
}
