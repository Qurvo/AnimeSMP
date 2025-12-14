package com.animesmp.core.ability.effects.movement;

import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.util.DashUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FlashstepEffect implements AbilityEffect {

    @Override
    public void execute(Player player, Ability ability) {
        // 8 blocks over 8 ticks = dash feel (not a teleport)
        DashUtil.dash(player, 10.0, 7, 0.05);
    }
}
