package com.animesmp.core;

import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.GlobalCooldownManager;
import com.animesmp.core.commands.*;
import com.animesmp.core.combat.DamageCalculator;
import com.animesmp.core.combat.KillCooldownManager;
import com.animesmp.core.combat.StatusEffectManager;
import com.animesmp.core.daily.DailyRewardManager;
import com.animesmp.core.economy.EconomyManager;
import com.animesmp.core.hud.StatusHudManager;
import com.animesmp.core.level.LevelManager;
import com.animesmp.core.listeners.*;
import com.animesmp.core.loot.ScrollLootManager;
import com.animesmp.core.pd.PdEventManager;
import com.animesmp.core.player.PlayerProfileManager;
import com.animesmp.core.shop.PdShopGuiManager;
import com.animesmp.core.shop.ShopGuiManager;
import com.animesmp.core.shop.rotation.PdStockManager;
import com.animesmp.core.shop.rotation.RotatingVendorManager;
import com.animesmp.core.stats.StatsManager;
import com.animesmp.core.stamina.StaminaManager;
import com.animesmp.core.training.TrainingManager;
import com.animesmp.core.trainer.TrainerAbilityManager;
import com.animesmp.core.trainer.TrainerManager;
import com.animesmp.core.trainer.TrainerQuestManager;
import com.animesmp.core.commands.TrainerQuestsCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class AnimeSMPPlugin extends JavaPlugin {

    private static AnimeSMPPlugin instance;

    // Core managers
    private PlayerProfileManager profileManager;
    private StatsManager statsManager;
    private StaminaManager staminaManager;
    private AbilityRegistry abilityRegistry;
    private AbilityManager abilityManager;
    private TrainingManager trainingManager;
    private LevelManager levelManager;
    private EconomyManager economyManager;
    private ShopGuiManager shopGuiManager;
    private StatusHudManager hudManager;
    private GlobalCooldownManager globalCooldownManager;
    private DamageCalculator damageCalculator;
    private KillCooldownManager killCooldownManager;
    private StatusEffectManager statusEffectManager;

    // Vendors / PD
    private PdStockManager pdStockManager;
    private PdShopGuiManager pdShopGuiManager;
    private RotatingVendorManager rotatingVendorManager;
    private NamespacedKey shopNpcKey;

    // Systems
    private TrainerQuestManager trainerQuestManager;
    private TrainerManager trainerManager;
    private ScrollLootManager scrollLootManager;
    private PdEventManager pdEventManager;

    // Trainer ability mapping
    private TrainerAbilityManager trainerAbilityManager;

    // Daily systems
    private DailyRewardManager dailyRewardManager;

    public static AnimeSMPPlugin getInstance() {
        return instance;
    }

    public NamespacedKey getShopNpcKey() {
        return shopNpcKey;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Core managers
        this.profileManager = new PlayerProfileManager(this);
        this.statsManager = new StatsManager(this);
        this.staminaManager = new StaminaManager(this);
        this.abilityRegistry = new AbilityRegistry(this);
        this.abilityManager = new AbilityManager(this);
        this.trainingManager = new TrainingManager(this);
        this.levelManager = new LevelManager(this);
        this.economyManager = new EconomyManager(this);
        this.shopGuiManager = new ShopGuiManager(this);
        this.hudManager = new StatusHudManager(this);
        this.globalCooldownManager = new GlobalCooldownManager();
        this.damageCalculator = new DamageCalculator(this);
        this.killCooldownManager = new KillCooldownManager();
        this.statusEffectManager = new StatusEffectManager(this);

        // Vendors / PD
        this.rotatingVendorManager = new RotatingVendorManager(this); // daily ability vendor
        this.pdStockManager = new PdStockManager(this);               // PD global stock
        this.pdShopGuiManager = new PdShopGuiManager(this);           // PD vendor GUI
        this.pdEventManager = new PdEventManager(this);               // PD events
        this.shopNpcKey = new NamespacedKey(this, "shop_npc_type");

        // Systems
        this.trainerQuestManager = new TrainerQuestManager(this);
        this.trainerManager = new TrainerManager(this);
        this.scrollLootManager = new ScrollLootManager(this);

        // Daily rewards & missions
        this.dailyRewardManager = new DailyRewardManager(this);

        // Load abilities from config
        abilityRegistry.loadAbilities();

        // Trainer ability mapping (which trainer teaches which abilities)
        this.trainerAbilityManager = new TrainerAbilityManager(this);

        // Commands
        registerCommands();

        // Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SkillsGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatXpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AbilityScrollListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatRewardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopNpcListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrainerQuestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ScrollLootListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrainerNpcListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PdEventListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrainingGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrainingTokenListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatStatusListener(this), this);

        // Unified vendor GUI listener (Ability, Training, PD)
        Bukkit.getPluginManager().registerEvents(new VendorListener(this), this);

        // Daily login rewards + missions
        Bukkit.getPluginManager().registerEvents(new DailyRewardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DailyMissionListener(this), this);

        // HUD
        hudManager.start();

        // Trainers: spawn from config + aura
        trainerManager.spawnAllFromConfig();
        trainerManager.startAuraTask();

        getLogger().info("AnimeSMP enabled.");
    }

    @Override
    public void onDisable() {
        if (profileManager != null) {
            profileManager.saveAll();
        }
        if (killCooldownManager != null) {
            killCooldownManager.clear();
        }
        if (dailyRewardManager != null) {
            dailyRewardManager.saveAll();
        }
        getLogger().info("AnimeSMP disabled.");
    }

    private void registerCommands() {
        // Player info / core
        registerExecutor("stats", new StatsCommand(this));
        registerExecutor("skills", new SkillsCommand(this));
        registerExecutor("abilities", new AbilitiesCommand(this));

        // PD admin (global PD toggle etc.)
        PluginCommand pdAdminCmd = getCommand("pdadmin");
        if (pdAdminCmd != null) {
            PdAdminCommand admin = new PdAdminCommand(this);
            pdAdminCmd.setExecutor(admin);
            pdAdminCmd.setTabCompleter(admin);
        }

        // /mission – view daily missions & progress
        PluginCommand missionCmd = getCommand("mission");
        if (missionCmd != null) {
            DailyMissionCommand dmc = new DailyMissionCommand(this);
            missionCmd.setExecutor(dmc);
            missionCmd.setTabCompleter(dmc);
        }

        // Test tools
        PluginCommand testCmd = getCommand("asptest");
        if (testCmd != null) {
            AnimeSmpTestCommand test = new AnimeSmpTestCommand(this);
            testCmd.setExecutor(test);
            testCmd.setTabCompleter(test);
        }

        // /traineradmin – trainer hub control
        PluginCommand trainerAdmin = getCommand("traineradmin");
        if (trainerAdmin != null) {
            trainerAdmin.setExecutor(new TrainerAdminCommand(this));
        }

        // /quests – view active trainer quests & progress
        PluginCommand questsCmd = getCommand("quests");
        if (questsCmd != null) {
            TrainerQuestsCommand tqc = new TrainerQuestsCommand(this);
            questsCmd.setExecutor(tqc);
            questsCmd.setTabCompleter(tqc);
        }

        // /train – info
        PluginCommand trainCmd = getCommand("train");
        if (trainCmd != null) {
            TrainCommand train = new TrainCommand(this);
            trainCmd.setExecutor(train);
            trainCmd.setTabCompleter(train);
        }

        // /traintoken – admin utility
        PluginCommand ttCmd = getCommand("traintoken");
        if (ttCmd != null) {
            TrainingTokenCommand tt = new TrainingTokenCommand(this);
            ttCmd.setExecutor(tt);
            ttCmd.setTabCompleter(tt);
        }

        // /abilityscroll – admin ability scrolls
        PluginCommand scrollCmd = getCommand("abilityscroll");
        if (scrollCmd != null) {
            AbilityScrollCommand asc = new AbilityScrollCommand(this);
            scrollCmd.setExecutor(asc);
            scrollCmd.setTabCompleter(asc);
        }

        // /yenadmin – currency admin
        PluginCommand yenAdminCmd = getCommand("yenadmin");
        if (yenAdminCmd != null) {
            YenAdminCommand yac = new YenAdminCommand(this);
            yenAdminCmd.setExecutor(yac);
            yenAdminCmd.setTabCompleter(yac);
        }

        // /yen – player yen view / pay
        PluginCommand yenCmd = getCommand("yen");
        if (yenCmd != null) {
            YenCommand yc = new YenCommand(this);
            yenCmd.setExecutor(yc);
            yenCmd.setTabCompleter(yc);
        }

        // /pdtokens – PD tokens view / pay
        PluginCommand pdTokensCmd = getCommand("pdtokens");
        if (pdTokensCmd != null) {
            PdTokensCommand pc = new PdTokensCommand(this);
            pdTokensCmd.setExecutor(pc);
            pdTokensCmd.setTabCompleter(pc);
        }

        // /shopnpc – create vendor NPCs (ability vendor, training vendor, PD vendor, etc.)
        PluginCommand shopNpcCmd = getCommand("shopnpc");
        if (shopNpcCmd != null) {
            ShopNpcCommand snc = new ShopNpcCommand(this);
            shopNpcCmd.setExecutor(snc);
            shopNpcCmd.setTabCompleter(snc);
        }

        // /trainer – player helper
        PluginCommand trainerCmd = getCommand("trainer");
        if (trainerCmd != null) {
            TrainerCommand tc = new TrainerCommand(this);
            trainerCmd.setExecutor(tc);
            trainerCmd.setTabCompleter(tc);
        }

        // /bind – bind abilities
        PluginCommand bindCmd = getCommand("bind");
        if (bindCmd != null) {
            BindCommand bind = new BindCommand(this);
            bindCmd.setExecutor(bind);
            bindCmd.setTabCompleter(bind);
        }

        // /abilityadmin – admin ability management
        PluginCommand abilityAdminCmd = getCommand("abilityadmin");
        if (abilityAdminCmd != null) {
            AbilityAdminCommand admin = new AbilityAdminCommand(this);
            abilityAdminCmd.setExecutor(admin);
            abilityAdminCmd.setTabCompleter(admin);
        }

        // /animesmp – central admin hub
        PluginCommand smpAdmin = getCommand("animesmp");
        if (smpAdmin != null) {
            AnimeSmpAdminCommand aac = new AnimeSmpAdminCommand(this);
            smpAdmin.setExecutor(aac);
            smpAdmin.setTabCompleter(aac);
        }

        // /cast1..5
        CastCommand cast = new CastCommand(this);
        for (String name : new String[]{"cast1", "cast2", "cast3", "cast4", "cast5"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(cast);
            }
        }

        // /cyclenext, /cycleprevious, /castselected
        CycleCommand cycle = new CycleCommand(this);
        for (String name : new String[]{"cyclenext", "cycleprevious", "castselected"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(cycle);
            }
        }
    }

    private void registerExecutor(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        }
    }

    // Getters

    public PlayerProfileManager getProfileManager() { return profileManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public StaminaManager getStaminaManager() { return staminaManager; }
    public AbilityRegistry getAbilityRegistry() { return abilityRegistry; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public TrainingManager getTrainingManager() { return trainingManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public ShopGuiManager getShopGuiManager() { return shopGuiManager; }
    public StatusHudManager getHudManager() { return hudManager; }
    public GlobalCooldownManager getGlobalCooldownManager() { return globalCooldownManager; }
    public DamageCalculator getDamageCalculator() { return damageCalculator; }
    public KillCooldownManager getKillCooldownManager() { return killCooldownManager; }

    public StatusEffectManager getStatusEffectManager() { return statusEffectManager; }

    public TrainerQuestManager getTrainerQuestManager() { return trainerQuestManager; }
    public TrainerManager getTrainerManager() { return trainerManager; }
    public ScrollLootManager getScrollLootManager() { return scrollLootManager; }

    public PdShopGuiManager getPdShopGuiManager() { return pdShopGuiManager; }
    public PdEventManager getPdEventManager() { return pdEventManager; }
    public RotatingVendorManager getRotatingVendorManager() { return rotatingVendorManager; }
    public PdStockManager getPdStockManager() { return pdStockManager; }

    public DailyRewardManager getDailyRewardManager() { return dailyRewardManager; }

    public TrainerAbilityManager getTrainerAbilityManager() { return trainerAbilityManager; }
}
