package com.animesmp.core.trainer;

/**
 * Simple data holder for a trainer quest tied to a specific ability.
 * TrainerQuestManager and the trainer listeners use this.
 */
public class TrainerQuest {

    public enum Type {
        MOB_KILL_LIGHT,     // e.g. kill mobs with light/damage abilities
        DAMAGE_FIRE,        // fire-type damage contribution
        DISTANCE_SPRINT,    // sprint distance
        PLAYER_KILL_MELEE   // melee player kills
    }

    private final String abilityId;
    private final String requirementText;
    private final int requiredAmount;
    private final Type type;

    public TrainerQuest(String abilityId, String requirementText, int requiredAmount, Type type) {
        this.abilityId = abilityId;
        this.requirementText = requirementText;
        this.requiredAmount = requiredAmount;
        this.type = type;
    }

    public String getAbilityId() {
        return abilityId;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public Type getType() {
        return type;
    }
}
