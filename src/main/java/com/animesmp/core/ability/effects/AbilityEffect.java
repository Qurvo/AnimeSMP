package com.animesmp.core.ability.effects;

import com.animesmp.core.ability.Ability;
import org.bukkit.entity.Player;

public interface AbilityEffect {

    /**
     * Execute the visual / gameplay effect of this ability.
     * NOTE: Cooldown & stamina (cost) are already handled by AbilityManager.
     */
    void execute(Player player, Ability ability);
}
