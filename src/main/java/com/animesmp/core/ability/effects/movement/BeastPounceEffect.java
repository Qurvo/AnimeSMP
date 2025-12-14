package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BeastPounceEffect implements AbilityEffect {

    @Override
    public void execute(Player caster, Ability ability) {

        // Leap forward
        Vector vel = caster.getLocation().getDirection().normalize().multiply(0.9);
        vel.setY(0.8);
        caster.setVelocity(vel);

        caster.getWorld().playSound(
                caster.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                0.9f
        );

        // After short delay, do small AoE slam where they land
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("AnimeSMP"),
                () -> {
                    Location loc = caster.getLocation().add(0, 0.5, 0);

                    loc.getWorld().spawnParticle(
                            Particle.CRIT,
                            loc,
                            10,
                            0.6, 0.3, 0.6,
                            0.0
                    );
                    loc.getWorld().playSound(
                            loc,
                            Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                            0.8f,
                            0.8f
                    );

                    double radius = 3.0;
                    double damageHearts = 1.0; // config value
                    double damage = damageHearts * 2.0;

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                        if (!(e instanceof LivingEntity)) continue;
                        if (e == caster) continue;

                        LivingEntity le = (LivingEntity) e;
                        le.damage(damage, caster);

                        Vector kb = le.getLocation().toVector()
                                .subtract(loc.toVector())
                                .normalize().multiply(0.4);
                        kb.setY(0.25);
                        le.setVelocity(le.getVelocity().add(kb));
                    }
                },
                10L // ~0.5s
        );
    }
}
