package com.animesmp.core.player;

import com.animesmp.core.ability.Ability;

import java.util.*;

public class PlayerProfile {

    private final UUID uuid;

    // Core progression
    private int level;
    private int xp;

    private int skillPoints;
    private int conPoints;
    private int strPoints;
    private int tecPoints;
    private int dexPoints;

    // Training
    private int trainingLevel;
    private int trainingXp;

    // Stamina
    private int staminaCap;
    private double staminaCurrent;
    private double staminaRegenPerSecond;

    // Currency
    private int yen;
    private int pdTokens;

    // Equipped abilities (legacy)
    private Ability movementAbility;
    private Ability damageAbility1;
    private Ability damageAbility2;
    private Ability defenseAbility;
    private Ability supportAbility;

    // Trainer quests
    private String activeTrainerQuestId;
    private int activeTrainerQuestProgress;
    private final Set<String> completedTrainerQuests = new HashSet<>();

    // Tutorial
    private boolean tutorialCompleted = false;

    // Action bar labels
    private final Map<Integer, String> actionBarLabels = new HashMap<>();

    // Daily rotating vendor purchases (legacy, still saved)
    private final Set<String> purchasedToday = new HashSet<>();

    // Bound abilities (slot → abilityId)
    private final Map<Integer, String> boundAbilityIds = new HashMap<>();
    private int selectedSlot = 1;

    // Permanently unlocked abilities
    private final Set<String> unlockedAbilities = new HashSet<>();

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;

        this.level = 1;
        this.xp = 0;

        // IMPORTANT: start with 1 skill point at level 1
        this.skillPoints = 1;
        this.conPoints = 0;
        this.strPoints = 0;
        this.tecPoints = 0;
        this.dexPoints = 0;

        this.trainingLevel = 0;
        this.trainingXp = 0;

        this.staminaCap = 100;
        this.staminaCurrent = staminaCap;
        this.staminaRegenPerSecond = 6.0;

        this.yen = 0;
        this.pdTokens = 0;
    }

    public UUID getUuid() { return uuid; }

    // Tutorial
    public boolean isTutorialCompleted() { return tutorialCompleted; }
    public void setTutorialCompleted(boolean tutorialCompleted) { this.tutorialCompleted = tutorialCompleted; }

    // Action bar labels
    public String getActionBarLabel(int slot) {
        return actionBarLabels.get(slot);
    }

    public void setActionBarLabel(int slot, String text) {
        if (slot < 1 || slot > 5) return;
        if (text == null || text.trim().isEmpty()) actionBarLabels.remove(slot);
        else actionBarLabels.put(slot, text);
    }

    public Map<Integer, String> getActionBarLabels() { return actionBarLabels; }

    // Trainer Quests
    public String getActiveTrainerQuestId() { return activeTrainerQuestId; }
    public void setActiveTrainerQuestId(String id) { this.activeTrainerQuestId = id; }

    public int getActiveTrainerQuestProgress() { return activeTrainerQuestProgress; }
    public void setActiveTrainerQuestProgress(int progress) { this.activeTrainerQuestProgress = progress; }

    public Set<String> getCompletedTrainerQuests() { return completedTrainerQuests; }

    public boolean hasCompletedTrainerQuest(String id) {
        return id != null && completedTrainerQuests.contains(id.toLowerCase());
    }

    public void addCompletedTrainerQuest(String id) {
        if (id != null && !id.isEmpty()) completedTrainerQuests.add(id.toLowerCase());
    }

    // Level / XP
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    // Skill Points
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int value) { skillPoints = value; }

    public int getConPoints() { return conPoints; }
    public void setConPoints(int value) { conPoints = value; }

    public int getStrPoints() { return strPoints; }
    public void setStrPoints(int value) { strPoints = value; }

    public int getTecPoints() { return tecPoints; }
    public void setTecPoints(int value) { tecPoints = value; }

    public int getDexPoints() { return dexPoints; }
    public void setDexPoints(int value) { dexPoints = value; }

    // Training
    public int getTrainingLevel() { return trainingLevel; }
    public void setTrainingLevel(int level) { trainingLevel = level; }

    public int getTrainingXp() { return trainingXp; }
    public void setTrainingXp(int xp) { trainingXp = xp; }

    // Stamina
    public int getStaminaCap() { return staminaCap; }
    public void setStaminaCap(int cap) { staminaCap = cap; }

    public double getStaminaCurrent() { return staminaCurrent; }
    public void setStaminaCurrent(double value) { staminaCurrent = value; }

    public double getStaminaRegenPerSecond() { return staminaRegenPerSecond; }
    public void setStaminaRegenPerSecond(double value) { staminaRegenPerSecond = value; }

    // Currency
    public int getYen() { return yen; }
    public void setYen(int value) { yen = value; }

    public int getPdTokens() { return pdTokens; }
    public void setPdTokens(int value) { pdTokens = value; }

    // Equipped Abilities (legacy)
    public Ability getMovementAbility() { return movementAbility; }
    public void setMovementAbility(Ability a) { movementAbility = a; }

    public Ability getDamageAbility1() { return damageAbility1; }
    public void setDamageAbility1(Ability a) { damageAbility1 = a; }

    public Ability getDamageAbility2() { return damageAbility2; }
    public void setDamageAbility2(Ability a) { damageAbility2 = a; }

    public Ability getDefenseAbility() { return defenseAbility; }
    public void setDefenseAbility(Ability a) { defenseAbility = a; }

    public Ability getSupportAbility() { return supportAbility; }
    public void setSupportAbility(Ability a) { supportAbility = a; }

    // Ability Binding
    public Map<Integer, String> getBoundAbilityIds() { return boundAbilityIds; }

    public String getBoundAbilityId(int slot) { return boundAbilityIds.get(slot); }

    public void setBoundAbilityId(int slot, String id) {
        if (id == null || id.isEmpty()) boundAbilityIds.remove(slot);
        else boundAbilityIds.put(slot, id.toLowerCase());
    }

    public int getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(int slot) {
        if (slot < 1 || slot > 5) slot = 1;
        selectedSlot = slot;
    }

    // Daily Vendor Purchases (legacy)
    public Set<String> getPurchasedToday() { return purchasedToday; }

    public void clearDailyPurchases() { purchasedToday.clear(); }

    public boolean hasPurchasedToday(String id) {
        return id != null && purchasedToday.contains(id.toLowerCase());
    }

    public void markPurchased(String id) {
        if (id != null && !id.isEmpty()) purchasedToday.add(id.toLowerCase());
    }

    // Unlocked Abilities
    public Set<String> getUnlockedAbilities() { return unlockedAbilities; }

    public boolean hasUnlockedAbility(String id) {
        return id != null && unlockedAbilities.contains(id.toLowerCase());
    }

    public void unlockAbility(String id) {
        if (id != null && !id.isEmpty()) unlockedAbilities.add(id.toLowerCase());
    }
}
