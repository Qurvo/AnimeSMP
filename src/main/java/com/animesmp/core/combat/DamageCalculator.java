package com.animesmp.core.combat;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.combat.HitFeedbackUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DamageCalculator {

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;

    public DamageCalculator(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
    }

    /**
     * Core ability damage calculation.
     *
     * Base: ability.getBaseDamageHearts() hearts → HP
     * Scaling:
     *  - STR: +1.2% per point
     *  - DEX: buffs scaling by +0.5% per point
     *  - Defender CON: 0.5% damage reduction per point
     *  - Defender DEX: buffs reduction by +0.5% per point
     */
    public double calculateAbilityDamage(Player attacker,
                                         LivingEntity target,
                                         Ability ability) {
        if (ability == null) return 0.0;

        // --- Base from ability (hearts → hp) ---
        double baseHearts = ability.getBaseDamageHearts();
        if (baseHearts <= 0) {
            baseHearts = 4.0; // fallback: 4 hearts = 8 HP
        }
        double baseHp = baseHearts * 2.0;

        // --- Offense: STR + DEX from attacker ---
        PlayerProfile atkProfile = profiles.getProfile(attacker.getUniqueId());
        int atkStr = atkProfile.getStrPoints();
        int atkDex = atkProfile.getDexPoints();

        double offenseMult = 1.0;
        offenseMult += atkStr * 0.012;         // +1.2% per STR
        offenseMult *= (1.0 + atkDex * 0.005); // DEX boosts scaling by 0.5% per point

        // --- Defense: CON + DEX from defender (if player) ---
        double reduction = 0.0;
        if (target instanceof Player defPlayer) {
            PlayerProfile defProfile = profiles.getProfile(defPlayer.getUniqueId());
            int defCon = defProfile.getConPoints();
            int defDex = defProfile.getDexPoints();

            double baseReduction = defCon * 0.005;             // 0.5% per CON
            double reductionBoost = 1.0 + (defDex * 0.005);    // DEX boosts that
            reduction = baseReduction * reductionBoost;
        }

        // clamp reduction so it never fully negates hits
        if (reduction < 0.0) reduction = 0.0;
        if (reduction > 0.65) reduction = 0.65;

        double finalDamage = baseHp * offenseMult * (1.0 - reduction);

        // safety clamp
        if (finalDamage < 0.5) finalDamage = 0.5;

        return finalDamage;
    }

    /**
     * Convenience wrapper: calculate & directly apply ability damage.
     */
    public void applyAbilityDamage(Player attacker,
                                   LivingEntity target,
                                   Ability ability) {
        if (attacker == null || target == null || ability == null) return;

        double damage = calculateAbilityDamage(attacker, target, ability);

        // PvP in Prot4 otherwise makes many abilities feel like they do nothing.
        // We split damage into an "armor-mitigated" portion and a "true" portion
        // (health/absorption reduction) so abilities remain meaningful.
        double trueFrac = switch (ability.getTier()) {
            case SCROLL -> 0.25;
            case VENDOR -> 0.30;
            case TRAINER -> 0.35;
            case PD -> 0.40;
        };
        // Slight bump for ultimates.
        if (ability.getType() == com.animesmp.core.ability.AbilityType.ULTIMATE) {
            trueFrac = Math.min(0.50, trueFrac + 0.10);
        }

        double trueDamage = damage * trueFrac;
        double vanillaDamage = Math.max(0.0, damage - trueDamage);

        if (vanillaDamage > 0.0) {
            target.damage(vanillaDamage, attacker);
        } else {
            // ensure combat attribution
            target.damage(0.0, attacker);
        }

        if (trueDamage > 0.0) {
            applyTrueDamage(attacker, target, trueDamage);
        }

        if (attacker != null) {
            HitFeedbackUtil.onAbilityHit(attacker, target);
        }


    }

    /**
     * Directly reduce absorption/health, bypassing armor and most vanilla reductions.
     * If attacker is null, the damage is treated as environmental.
     */
    public void applyTrueDamage(Player attacker, LivingEntity target, double amountHp) {
        if (target == null) return;
        if (amountHp <= 0.0) return;

        // Absorption first for players.
        if (target instanceof Player p) {
            double abs = p.getAbsorptionAmount();
            if (abs > 0) {
                double used = Math.min(abs, amountHp);
                p.setAbsorptionAmount(abs - used);
                amountHp -= used;
            }
        }

        if (amountHp <= 0.0) return;

        double newHp = Math.max(0.0, target.getHealth() - amountHp);
        target.setHealth(newHp);

        // Ensure combat attribution so kill credit works.
        if (attacker != null) {
            target.setLastDamage(amountHp);
            target.damage(0.0, attacker);
        }
    }

    /**
     * Get nearby enemies around a player within radius.
     * Currently: all LivingEntities except the player themself.
     */
    public Collection<LivingEntity> getNearbyEnemies(Player player, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return result;

        for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.equals(player)) continue;
            result.add(le);
        }
        return result;
    }

    /**
     * Ray-like scan in front of the player to find the first LivingEntity hit.
     *
     * @param player   source
     * @param maxRange max distance
     * @param radius   hit radius around the line
     */
    public LivingEntity findFirstTargetInLine(Player player, double maxRange, double radius) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) return null;

        Vector dir = eye.getDirection().normalize();
        double step = 0.5;

        for (double d = 0.0; d <= maxRange; d += step) {
            Location point = eye.clone().add(dir.clone().multiply(d));

            Collection<Entity> nearby = world.getNearbyEntities(
                    point,
                    radius,
                    radius,
                    radius
            );

            LivingEntity best = null;
            double bestDistSq = Double.MAX_VALUE;

            for (Entity e : nearby) {
                if (!(e instanceof LivingEntity le)) continue;
                if (le.equals(player)) continue;

                double distSq = le.getLocation().distanceSquared(point);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = le;
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }
}
