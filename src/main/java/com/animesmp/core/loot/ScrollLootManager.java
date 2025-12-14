package com.animesmp.core.loot;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ScrollLootManager {

    private final AnimeSMPPlugin plugin;
    private final Random random = new Random();

    private boolean enabled;
    private double baseChance;   // chance this chest even rolls for scrolls
    private int minAmount;
    private int maxAmount;

    // per-tier rarity chances per “roll”
    private double chanceScroll;   // SCROLL  (Common)
    private double chanceVendor;   // VENDOR  (Uncommon / Rare)
    private double chanceTrainer;  // TRAINER (Rare / Epic)
    private double chancePd;       // PD      (Legendary)

    public ScrollLootManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("loot.scrolls.enabled", true);

        // Chance that a chest gets *any* scroll rolls at all
        this.baseChance = cfg.getDouble("loot.scrolls.base-chance", 0.40); // 40% by default
        this.minAmount = cfg.getInt("loot.scrolls.min-amount", 1);
        this.maxAmount = cfg.getInt("loot.scrolls.max-amount", 2);

        if (minAmount < 0) minAmount = 0;
        if (maxAmount < minAmount) maxAmount = minAmount;

        // Defaults based on what you asked for:
        // Common  ~20%, Uncommon 10%, Rare 5%, Epic 1%, Legendary 0.5%
        // We map tiers -> rough “rarities”:
        //   SCROLL  -> Common
        //   VENDOR  -> Uncommon
        //   TRAINER -> Rare/Epic
        //   PD      -> Legendary
        this.chanceScroll = 0.20;
        this.chanceVendor = 0.10;
        this.chanceTrainer = 0.05;
        this.chancePd = 0.005; // 0.5%

        ConfigurationSection rs = cfg.getConfigurationSection("loot.scrolls.rarity-chances");
        if (rs != null) {
            // Optional overrides in config:
            // loot.scrolls.rarity-chances:
            //   SCROLL: 0.20
            //   VENDOR: 0.10
            //   TRAINER: 0.05
            //   PD: 0.005
            if (rs.isDouble("SCROLL")) {
                this.chanceScroll = rs.getDouble("SCROLL");
            }
            if (rs.isDouble("VENDOR")) {
                this.chanceVendor = rs.getDouble("VENDOR");
            }
            if (rs.isDouble("TRAINER")) {
                this.chanceTrainer = rs.getDouble("TRAINER");
            }
            if (rs.isDouble("PD")) {
                this.chancePd = rs.getDouble("PD");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void maybeAddScrolls(LootGenerateEvent event) {
        if (!enabled) return;

        // Only containers (chests, barrels, etc.), not mob drops/fishing
        if (event.getInventoryHolder() == null) {
            return;
        }

        // Chest-level roll: does this chest get scrolls at all?
        // PD should be 2x for everything, including scroll spawn rates.
        double chance = baseChance;
        var pd = plugin.getPdEventManager();
        if (pd != null && pd.isActive()) {
            chance = Math.min(1.0, chance * pd.getXpMultiplier());
        }

        if (random.nextDouble() > chance) {
            return;
        }

        // Split all abilities by tier
        List<Ability> scrollList = plugin.getAbilityRegistry().getAllAbilities()
                .stream()
                .filter(a -> a.getTier() == AbilityTier.SCROLL)
                .collect(Collectors.toList());

        List<Ability> vendorList = plugin.getAbilityRegistry().getAllAbilities()
                .stream()
                .filter(a -> a.getTier() == AbilityTier.VENDOR)
                .collect(Collectors.toList());

        List<Ability> trainerList = plugin.getAbilityRegistry().getAllAbilities()
                .stream()
                .filter(a -> a.getTier() == AbilityTier.TRAINER)
                .collect(Collectors.toList());

        List<Ability> pdList = plugin.getAbilityRegistry().getAllAbilities()
                .stream()
                .filter(a -> a.getTier() == AbilityTier.PD)
                .collect(Collectors.toList());

        // Nothing to drop
        if (scrollList.isEmpty() && vendorList.isEmpty() &&
                trainerList.isEmpty() && pdList.isEmpty()) {
            return;
        }

        int rolls;
        if (minAmount == maxAmount) {
            rolls = minAmount;
        } else {
            rolls = minAmount + random.nextInt(maxAmount - minAmount + 1);
        }

        if (rolls <= 0) return;

        List<ItemStack> loot = event.getLoot();
        if (loot == null) {
            loot = new ArrayList<>();
            event.setLoot(loot);
        }

        for (int i = 0; i < rolls; i++) {
            Ability chosen = rollOneAbility(scrollList, vendorList, trainerList, pdList);
            if (chosen == null) continue;

            ItemStack scroll = plugin.getAbilityManager().createAbilityScroll(chosen, 1);
            if (scroll != null) {
                loot.add(scroll);
            }
        }
    }

    /**
     * One “rarity roll” that may return an ability, or null (no scroll this roll).
     */
    private Ability rollOneAbility(List<Ability> scrollList,
                                   List<Ability> vendorList,
                                   List<Ability> trainerList,
                                   List<Ability> pdList) {

        double roll = random.nextDouble();

        double thresholdCommon   = chanceScroll;
        double thresholdVendor   = thresholdCommon + chanceVendor;
        double thresholdTrainer  = thresholdVendor + chanceTrainer;
        double thresholdPd       = thresholdTrainer + chancePd;

        // Nothing hit – no scroll on this roll
        if (roll > thresholdPd) {
            return null;
        }

        if (roll <= thresholdCommon && !scrollList.isEmpty()) {
            return scrollList.get(random.nextInt(scrollList.size()));
        }

        if (roll <= thresholdVendor && !vendorList.isEmpty()) {
            return vendorList.get(random.nextInt(vendorList.size()));
        }

        if (roll <= thresholdTrainer && !trainerList.isEmpty()) {
            return trainerList.get(random.nextInt(trainerList.size()));
        }

        if (!pdList.isEmpty()) {
            return pdList.get(random.nextInt(pdList.size()));
        }

        return null;
    }
}
