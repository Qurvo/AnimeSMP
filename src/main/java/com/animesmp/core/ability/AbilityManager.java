package com.animesmp.core.ability;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.effects.AbilityEffect;
import com.animesmp.core.ability.effects.GenericAbilityEffect;
import com.animesmp.core.ability.util.DangerTelegraphUtil;

// Effects
import com.animesmp.core.ability.effects.damage.*;
import com.animesmp.core.ability.effects.defense.*;
import com.animesmp.core.ability.effects.movement.*;
import com.animesmp.core.ability.effects.utility.*;
import com.animesmp.core.ability.effects.ultimate.*;

import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.stamina.StaminaManager;
import com.animesmp.core.stats.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AbilityManager {

    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;
    private final CooldownManager cooldowns = new CooldownManager();
    private final PlayerProfileManager profiles;
    private final StaminaManager stamina;
    private final StatsManager stats;
    private final NamespacedKey scrollKey;

    private final Map<String, AbilityEffect> effects = new HashMap<>();
    private final AbilityEffect defaultEffect;

    public AbilityManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.profiles = plugin.getProfileManager();
        this.stamina = plugin.getStaminaManager();
        this.stats = plugin.getStatsManager();

        this.defaultEffect = new GenericAbilityEffect();
        this.scrollKey = new NamespacedKey(plugin, "ability_scroll_id");
        registerDefaultEffects();
    }

    // ------------------------------------------------------------------------
    // Effect registration
    // ------------------------------------------------------------------------

    private void registerDefaultEffects() {
        // MOVEMENT
        registerEffect("flashstep", new FlashstepEffect());
        registerEffect("thunderclap_dash", new ThunderDashEffect());
        registerEffect("soru_step", new SoruStepEffect());
        registerEffect("sky_walk", new SkyWalkEffect());
        registerEffect("shadow_slip", new ShadowSlipEffect());
        registerEffect("beast_pounce", new BeastPounceEffect());
        registerEffect("burst_step", new BurstStepEffect());

        // DAMAGE (LIGHT / HEAVY) – core
        registerEffect("fire_fist", new FireFistEffect(plugin));
        registerEffect("shockwave", new ShockwaveEffect(plugin));

        // Light / single-target / projectiles
        registerEffect("spirit_bullet", new SpiritBulletEffect());
        registerEffect("spider_thread_cutter", new SpiderThreadCutterEffect());
        registerEffect("dark_slash", new DarkSlashEffect());
        registerEffect("exploding_blood_burst", new ExplodingBloodBurstEffect());
        registerEffect("ki_blast_barrage", new KiBlastBarrageEffect());
        registerEffect("thunder_pierce", new ThunderPierceEffect());
        registerEffect("blood_sickle_slash", new BloodSickleSlashEffect());
        registerEffect("gomu_rocket_punch", new GomuRocketPunchEffect());
        registerEffect("abyss_cutter", new AbyssCutterEffect());

        // AoE / beams / specials
        registerEffect("shockwave_roar", new ShockwaveRoarEffect());
        registerEffect("thunder_beam", new ThunderBeamEffect());
        registerEffect("cursed_black_flash", new CursedBlackFlashEffect());
        registerEffect("crescent_typhoon_slash", new CrescentTyphoonSlashEffect());

        // Arcane / Rankyaku / Rumble
        registerEffect("arcane_spear", new ArcaneBurstEffect());
        registerEffect("arcane_burst", new ArcaneBurstEffect());
        registerEffect("rankyaku_slice", new RankyakuSliceEffect());
        registerEffect("rumble_claw", new RumbleClawEffect());

        // Moon / Getsu
        registerEffect("crescent_moon_arc", new CrescentMoonArcEffect());
        registerEffect("getsu_wave", new GetsuWaveEffect());

        // Wind-style
        registerEffect("wind_typhoon_slash", new WindTyphoonSlashEffect());

        // DEFENSIVE
        registerEffect("flame_cloak", new FlameCloakEffect());
        registerEffect("ki_barrier", new KiBarrierEffect());
        registerEffect("water_parry", new WaterParryEffect());
        registerEffect("harden_skin", new HardenSkinEffect());
        registerEffect("iron_body", new IronBodyEffect());
        registerEffect("shadow_guard", new ShadowGuardEffect());
        registerEffect("cursed_armor", new CursedArmorEffect());

        // UTILITY / SUPPORT
        registerEffect("beast_instinct", new BeastInstinctEffect());
        registerEffect("breathing_focus", new BreathingFocusEffect());
        registerEffect("chakra_renewal", new ChakraRenewalEffect());
        registerEffect("cursed_pulse", new CursedPulseEffect());
        registerEffect("gale_zone", new GaleZoneEffect());
        registerEffect("observation_sense", new ObservationSenseEffect());
        registerEffect("shadow_bind", new ShadowBindEffect());
        registerEffect("spider_trap", new SpiderTrapEffect());

        // ULTIMATE
        registerEffect("inferno_nova", new InfernoNovaEffect());
        registerEffect("judgment_bolt", new JudgmentBoltEffect());
        registerEffect("annihilation_punch", new AnnihilationPunchEffect());
        registerEffect("dragon_gods_fist", new DragonGodsFistEffect());
        registerEffect("full_moon_typhoon", new FullMoonTyphoonEffect());
        registerEffect("moon_sever", new MoonSeverEffect());

    }

    public void registerEffect(String id, AbilityEffect effect) {
        if (id == null || effect == null) return;
        effects.put(id.toLowerCase(Locale.ROOT), effect);
    }

    // ------------------------------------------------------------------------
    // Ability usage
    // ------------------------------------------------------------------------

    public boolean useAbility(Player player, Ability ability) {
        if (ability == null) return false;

        // Hard CC: silence blocks ability casts.
        if (plugin.getStatusEffectManager() != null && plugin.getStatusEffectManager().isSilenced(player)) {
            long left = plugin.getStatusEffectManager().silenceRemainingMs(player);
            player.sendMessage(ChatColor.RED + "You are silenced for " + ChatColor.YELLOW + String.format(Locale.US, "%.1f", left / 1000.0) + "s");
            return false;
        }

        PlayerProfile profile = profiles.getProfile(player);
        String abilityId = ability.getId().toLowerCase(Locale.ROOT);

        // Unlock check (admins do NOT bypass; only explicit bypass perm)
        if (!profile.hasUnlockedAbility(abilityId) && !player.hasPermission("animesmp.unlock.bypass")) {
            player.sendMessage(ChatColor.RED + "You have not unlocked this ability yet.");
            return false;
        }


        // Global cooldown
        if (plugin.getGlobalCooldownManager().isOnGlobalCooldown(player)) {
            long left = plugin.getGlobalCooldownManager().getRemaining(player);
            player.sendMessage(ChatColor.RED + "Global Cooldown: " +
                    ChatColor.YELLOW + String.format(Locale.US, "%.2f", left / 1000.0) + "s");
            return false;
        }

        // Ability cooldown
        if (cooldowns.isOnCooldown(player, ability)) {
            long ms = cooldowns.getRemaining(player, ability);
            player.sendMessage(ChatColor.RED + "Ability on cooldown for " +
                    ChatColor.YELLOW + String.format(Locale.US, "%.1f", ms / 1000.0) + "s");
            return false;
        }

        // Stamina cost
        int baseCost = ability.getStaminaCost();
        double costMultiplier = stats.getStaminaCostMultiplier(player);
        int cost = (int) Math.round(baseCost * costMultiplier);

        if (!stamina.hasStamina(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough stamina. Need " +
                    ChatColor.AQUA + cost + ChatColor.RED + ".");
            return false;
        }

        stamina.consumeStamina(player, cost);

        // Apply global CD
        plugin.getGlobalCooldownManager().applyGlobalCooldown(player);

        // Use per-ability cooldown from config (core balance lever).
        double cdMult = stats.getCooldownMultiplier(player);
        long cdMillis = (long) ((ability.getCooldownSeconds() * cdMult) * 1000L);
        cooldowns.setCooldown(player, ability, cdMillis);

        // Telegraph for high-danger abilities (placeholder until custom VFX/SFX)
        if (ability.getType() == AbilityType.DAMAGE_HEAVY || ability.getType() == AbilityType.ULTIMATE) {
            DangerTelegraphUtil.telegraph(player, 6);
        }

        // Execute effect
        AbilityEffect effect = effects.getOrDefault(abilityId, defaultEffect);
        effect.execute(player, ability);

        return true;
    }

    public long getRemainingCooldown(Player player, String abilityId) {
        if (abilityId == null) return 0;

        Ability ability = registry.getAbility(abilityId.toLowerCase(Locale.ROOT));
        if (ability == null) return 0;

        if (!cooldowns.isOnCooldown(player, ability)) {
            return 0;
        }
        long remainingMs = cooldowns.getRemaining(player, ability);
        return Math.max(0, remainingMs);
    }

    // ------------------------------------------------------------------------
    // Scroll creation
    // ------------------------------------------------------------------------

    private String rarityLabel(AbilityTier tier) {
        if (tier == null) return "Common";
        switch (tier) {
            case SCROLL:
                return "Common";
            case VENDOR:
                return "Rare";
            case TRAINER:
                return "Epic";
            case PD:
                return "Legendary";
            default:
                return "Common";
        }
    }

    private ChatColor rarityColor(AbilityTier tier) {
        if (tier == null) return ChatColor.GRAY;
        switch (tier) {
            case SCROLL:   // common
                return ChatColor.GRAY;
            case VENDOR:   // rare
                return ChatColor.BLUE;
            case TRAINER:  // epic
                return ChatColor.DARK_PURPLE;
            case PD:       // legendary
                return ChatColor.GOLD;
            default:
                return ChatColor.GRAY;
        }
    }

    private String typeLabel(AbilityType type) {
        if (type == null) return "Unknown";
        switch (type) {
            case MOVEMENT:
                return "Movement";
            case DAMAGE_LIGHT:
                return "Light Damage";
            case DAMAGE_HEAVY:
                return "Heavy Damage";
            case DEFENSE:
                return "Defense";
            case UTILITY:
                return "Utility";
            case ULTIMATE:
                return "Ultimate";
            default:
                return "Unknown";
        }
    }

    private String typeDescription(Ability ability) {
        if (ability == null || ability.getType() == null) {
            return "A special technique from the AnimeSMP world.";
        }
        switch (ability.getType()) {
            case MOVEMENT:
                return "Fast movement technique to reposition or chase.";
            case DAMAGE_LIGHT:
                return "Quick offensive technique with reliable damage.";
            case DAMAGE_HEAVY:
                return "Heavy-hitting finisher that deals big damage.";
            case DEFENSE:
                return "Protective technique to reduce or avoid damage.";
            case UTILITY:
                return "Support technique that grants buffs or control.";
            case ULTIMATE:
                return "Extremely powerful ultimate technique with a long cooldown.";
            default:
                return "A special technique from the AnimeSMP world.";
        }
    }

    public ItemStack createAbilityScroll(Ability ability, int amount) {
        if (ability == null) return null;
        if (amount <= 0) amount = 1;

        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, amount);
        ItemMeta meta = item.getItemMeta();

        // Display name
        String display = ChatColor.LIGHT_PURPLE + "Ability Scroll: " +
                ChatColor.AQUA + ability.getDisplayName();
        meta.setDisplayName(display);

        // Lore
        List<String> lore = new ArrayList<>();

        // Short description
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + typeDescription(ability));
        lore.add("");

        // Rarity & type
        AbilityTier tier = ability.getTier();
        String rarityText = rarityLabel(tier);
        ChatColor rarityCol = rarityColor(tier);
        String typeText = typeLabel(ability.getType());

        lore.add(ChatColor.DARK_GRAY + "Rarity: " + rarityCol + rarityText);
        lore.add(ChatColor.DARK_GRAY + "Type: " + ChatColor.GREEN + typeText);
        lore.add("");

        // Use info
        lore.add(ChatColor.YELLOW + "Right-click " + ChatColor.GRAY + "to learn this ability.");
        lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + ability.getId().toLowerCase(Locale.ROOT));

        meta.setLore(lore);

        // PDC tag
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(scrollKey, PersistentDataType.STRING, ability.getId().toLowerCase(Locale.ROOT));

        item.setItemMeta(meta);
        return item;
    }

    public Ability getAbilityFromScroll(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        if (!stack.hasItemMeta()) return null;

        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(scrollKey, PersistentDataType.STRING);
        if (id == null) return null;

        return registry.getAbility(id);
    }

    public AbilityRegistry getRegistry() {
        return registry;
    }
}
