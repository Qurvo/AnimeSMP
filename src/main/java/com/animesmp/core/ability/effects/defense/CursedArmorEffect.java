package com.animesmp.core.ability.effects.defense;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.util.AttributeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class CursedArmorEffect implements AbilityEffect {

    private static final UUID ARMOR_MOD_UUID = UUID.fromString("a9bda5b7-0d1e-4c45-b77e-4fd1a1b93d55");
    private static final int DURATION_TICKS = 12 * 20;

    @Override
    public void execute(Player player, Ability ability) {
        AttributeInstance maxHealth = player.getAttribute(AttributeUtil.maxHealth());
        if (maxHealth != null) {
            maxHealth.getModifiers().stream()
                    .filter(m -> m.getUniqueId().equals(ARMOR_MOD_UUID))
                    .forEach(maxHealth::removeModifier);

            // +4 hearts (8 health)
            AttributeModifier mod = new AttributeModifier(
                    ARMOR_MOD_UUID,
                    "cursed_armor_bonus",
                    8.0,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            maxHealth.addTransientModifier(mod);
        }

        double newHealth = Math.min(player.getHealth() + 4.0, player.getMaxHealth());
        player.setHealth(newHealth);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, DURATION_TICKS, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION_TICKS, 0, false, true, true));

        player.getWorld().spawnParticle(
                Particle.SMOKE,
                player.getLocation().add(0, 1.0, 0),
                40,
                0.8, 1.3, 0.8,
                0.01
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                0.7f,
                0.6f
        );

        player.sendMessage(ChatColor.DARK_PURPLE + "A cursed armor wraps around you, trading mobility for power.");

        Bukkit.getScheduler().runTaskLater(AnimeSMPPlugin.getInstance(), () -> {
            AttributeInstance mh = player.getAttribute(AttributeUtil.maxHealth());
            if (mh != null) {
                mh.getModifiers().stream()
                        .filter(m -> m.getUniqueId().equals(ARMOR_MOD_UUID))
                        .forEach(mh::removeModifier);

                if (player.getHealth() > mh.getValue()) {
                    player.setHealth(mh.getValue());
                }
            }
            player.sendMessage(ChatColor.DARK_GRAY + "The cursed armor fades away.");
        }, DURATION_TICKS);
    }
}
