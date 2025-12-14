package com.animesmp.core.trainer;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks a trainer GUI inventory and stores which TrainerType it belongs to.
 */
public class TrainerGuiHolder implements InventoryHolder {

    private final TrainerType type;

    public TrainerGuiHolder(TrainerType type) {
        this.type = type;
    }

    public TrainerType getTrainerType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        // Not used; Bukkit fills this in for us.
        return null;
    }
}
