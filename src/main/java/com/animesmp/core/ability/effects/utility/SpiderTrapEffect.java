package com.animesmp.core.ability.effects.utility;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.combat.TargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpiderTrapEffect implements AbilityEffect {

    private static final int DURATION_TICKS = 6 * 20;

    @Override
    public void execute(Player caster, Ability ability) {
        AnimeSMPPlugin plugin = AnimeSMPPlugin.getInstance();
        if (plugin == null || caster == null) return;

        Location center = caster.getLocation();
        List<Block> placed = new ArrayList<>();

        LivingEntity target = TargetingUtil.findNearestLivingTarget(
                caster,
                10.0,
                true,
                true
        );

        if (target != null) {
            place2x2WebAtFeet(target.getLocation(), placed);
        } else {
            for (LivingEntity le : TargetingUtil.getLivingTargetsInRadius(caster, center, 4.0, true)) {
                place2x2WebAtFeet(le.getLocation(), placed);
            }
        }

        caster.getWorld().spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(0, 0.5, 0), 12, 1.0, 0.3, 1.0, 0.0);
        caster.getWorld().playSound(center, Sound.BLOCK_CROP_BREAK, 0.8f, 0.9f);

        caster.sendMessage(ChatColor.DARK_GREEN + "You cast Spider Trap!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Block b : placed) {
                if (b.getType() == Material.COBWEB) {
                    b.setType(Material.AIR, false);
                }
            }
        }, DURATION_TICKS);
    }

    private void place2x2WebAtFeet(org.bukkit.Location loc, java.util.List<org.bukkit.block.Block> placed) {
        org.bukkit.World w = loc.getWorld();
        if (w == null) return;

        int y = loc.getBlockY(); // feet level
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        int[][] offsets = { {0,0}, {1,0}, {0,1}, {1,1} };

        for (int[] o : offsets) {
            org.bukkit.block.Block b = w.getBlockAt(x + o[0], y, z + o[1]);
            if (b.getType() == org.bukkit.Material.COBWEB) continue;

            // Only place if not a solid block
            if (b.getType().isAir() || b.isPassable()) {
                b.setType(org.bukkit.Material.COBWEB, false);
                placed.add(b);
            }
        }
    }
}
