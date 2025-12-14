package com.animesmp.core.training;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TrainingManager {

    private static final int MAX_TRAINING_LEVEL = 25;

    private final AnimeSMPPlugin plugin;
    private final PlayerProfileManager profiles;
    private final NamespacedKey tokenKey;
    private final Map<UUID, TrainingSession> sessions = new HashMap<>();

    // OSU-style: slots where the button can appear (center-ish region of a 27-slot inventory)
    private static final int[] ACTIVE_SLOTS = {
            10, 11, 12,
            13, 14, 15,
            16
    };

    private final Random random = new Random();

    public TrainingManager(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.profiles = plugin.getProfileManager();
        this.tokenKey = new NamespacedKey(plugin, "training_token_tier");
    }

    public NamespacedKey getTokenKey() {
        return tokenKey;
    }

    // ------------------------------------------------------------------------
    // TOKEN CREATION / PARSING
    // ------------------------------------------------------------------------

    public ItemStack createTokenItem(TrainingTokenTier tier) {
        return createTokenItem(tier, 1);
    }

    public ItemStack createTokenItem(TrainingTokenTier tier, int amount) {
        if (amount <= 0) amount = 1;

        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();

        String name = ChatColor.GOLD + tier.getDisplayName();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Use while holding to start a");
        lore.add(ChatColor.GRAY + "training minigame and earn TL XP.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Recommended TL: " +
                ChatColor.AQUA + tier.getMinLevel() + "–" + tier.getMaxLevel());
        lore.add(ChatColor.DARK_AQUA + "XP Range: " +
                ChatColor.GREEN + tier.getXpMin() + ChatColor.GRAY + " to " +
                ChatColor.GREEN + tier.getXpMax());
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Tier: " + tier.name());
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tokenKey, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }

    public boolean isTrainingToken(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!stack.hasItemMeta()) return false;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.has(tokenKey, PersistentDataType.STRING);
    }

    public TrainingTokenTier getTokenTier(ItemStack stack) {
        if (!isTrainingToken(stack)) return null;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(tokenKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return TrainingTokenTier.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return TrainingTokenTier.fromString(raw);
        }
    }

    // ------------------------------------------------------------------------
    // TOKEN USE → START SESSION
    // ------------------------------------------------------------------------

    public void handleTokenUse(Player player, ItemStack stack) {
        TrainingTokenTier tier = getTokenTier(stack);
        if (tier == null) return;

        PlayerProfile profile = profiles.getProfile(player);
        int trainingLevel = profile.getTrainingLevel();

        if (trainingLevel < tier.getMinLevel()) {
            player.sendMessage(ChatColor.RED + "Your Training Level is too low to use this token.");
            player.sendMessage(ChatColor.GRAY + "Required TL: " + ChatColor.AQUA + tier.getMinLevel() +
                    ChatColor.GRAY + " (you are TL " + trainingLevel + ").");
            return;
        }

        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in a training session.");
            return;
        }

        consumeOne(stack);
        startTrainingSession(player, tier);
    }

    private void consumeOne(ItemStack stack) {
        int amt = stack.getAmount();
        if (amt <= 1) {
            stack.setAmount(0);
        } else {
            stack.setAmount(amt - 1);
        }
    }

    // ------------------------------------------------------------------------
    // TRAINING SESSION / GUI
    // ------------------------------------------------------------------------

    private static class TrainingSession {
        final TrainingTokenTier tier;
        final UUID playerId;
        final Inventory inventory;
        final int noteCount;

        int notesDone;
        int score;           // aggregate score
        int phaseIndex;      // 0..6
        int taskId;
        int currentSlot;     // where the button currently is
        int combo;           // current success streak

        TrainingSession(TrainingTokenTier tier, UUID playerId, Inventory inv, int noteCount, int initialSlot) {
            this.tier = tier;
            this.playerId = playerId;
            this.inventory = inv;
            this.noteCount = noteCount;
            this.currentSlot = initialSlot;
            this.combo = 0;
        }
    }

    private void startTrainingSession(Player player, TrainingTokenTier tier) {
        UUID id = player.getUniqueId();
        if (sessions.containsKey(id)) {
            player.sendMessage(ChatColor.RED + "You are already in a training session.");
            return;
        }

        int baseNotes = getBaseNoteCountForTier(tier);
        // small randomization of note count (±2, but not below 8)
        int offset = random.nextInt(5) - 2; // -2..+2
        int notes = Math.max(8, baseNotes + offset);

        Inventory inv = Bukkit.createInventory(
                player,
                27,
                ChatColor.DARK_AQUA + "Training: " + tier.getDisplayName()
        );

        // Background filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Initial slot: center-ish (13)
        int startSlot = 13;
        updateTimingItemAtSlot(inv, startSlot, 0);

        TrainingSession session = new TrainingSession(tier, id, inv, notes, startSlot);
        sessions.put(id, session);

        // Tick speed per tier (lower = faster)
        long periodTicks = getTickPeriodForTier(tier);

        // Open GUI
        player.openInventory(inv);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> tickSession(session),
                periodTicks,
                periodTicks
        );
        session.taskId = taskId;

        player.sendMessage(ChatColor.AQUA + "Training started: " + ChatColor.GOLD + tier.getDisplayName());
        player.sendMessage(ChatColor.GRAY + "Click the moving orb when it is " +
                ChatColor.GREEN + "bright green" + ChatColor.GRAY + " for best results.");
    }

    private int getBaseNoteCountForTier(TrainingTokenTier tier) {
        switch (tier) {
            case BASIC:
                return 12;
            case INTERMEDIATE:
                return 14;
            case ADVANCED:
                return 16;
            case EXPERT:
            case ELITE:
                return 18;
            case MASTER:
            case LEGENDARY:
                return 20;
            default:
                return 12;
        }
    }

    private long getTickPeriodForTier(TrainingTokenTier tier) {
        switch (tier) {
            case BASIC:
                return 12L; // slower
            case INTERMEDIATE:
                return 11L;
            case ADVANCED:
                return 10L;
            case EXPERT:
                return 9L;
            case ELITE:
                return 8L;
            case MASTER:
                return 7L;
            case LEGENDARY:
                return 6L;  // fastest
            default:
                return 10L;
        }
    }

    private void tickSession(TrainingSession session) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null || !player.isOnline()) {
            endSession(session, true);
            return;
        }

        if (session.notesDone >= session.noteCount) {
            endSession(session, false);
            return;
        }

        // Advance timing phase
        session.phaseIndex++;
        if (session.phaseIndex > 6) {
            // note expired → count as miss & move button
            session.notesDone++;
            session.phaseIndex = 0;

            // MISS → reset combo & play dull sound
            session.combo = 0;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.7f);

            if (session.notesDone >= session.noteCount) {
                endSession(session, false);
                return;
            }

            moveButtonToNewSlot(session);
        } else {
            // just update color on current slot
            updateTimingItemAtSlot(session.inventory, session.currentSlot, session.phaseIndex);
        }

        if (player.getOpenInventory() != null &&
                player.getOpenInventory().getTopInventory().equals(session.inventory)) {
            player.updateInventory();
        }
    }

    private void updateTimingItemAtSlot(Inventory inv, int slot, int phaseIndex) {
        Material mat;
        ChatColor color;
        String label;

        switch (phaseIndex) {
            case 0:
            case 6:
                mat = Material.RED_STAINED_GLASS_PANE;
                color = ChatColor.RED;
                label = "Too early / too late";
                break;
            case 1:
            case 5:
                mat = Material.ORANGE_STAINED_GLASS_PANE;
                color = ChatColor.GOLD;
                label = "Getting closer";
                break;
            case 2:
            case 4:
                mat = Material.YELLOW_STAINED_GLASS_PANE;
                color = ChatColor.YELLOW;
                label = "Almost there";
                break;
            case 3:
            default:
                mat = Material.LIME_STAINED_GLASS_PANE;
                color = ChatColor.GREEN;
                label = "CLICK!";
                break;
        }

        ItemStack orb = new ItemStack(mat);
        ItemMeta meta = orb.getItemMeta();
        meta.setDisplayName(color + label);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click when this is " + ChatColor.GREEN + "bright green");
        lore.add(ChatColor.GRAY + "to earn more training XP.");
        meta.setLore(lore);
        orb.setItemMeta(meta);

        inv.setItem(slot, orb);
    }

    private void moveButtonToNewSlot(TrainingSession session) {
        Inventory inv = session.inventory;

        // Reset old slot to filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        inv.setItem(session.currentSlot, filler);

        // Pick a new slot from ACTIVE_SLOTS, different from current
        int newSlot = session.currentSlot;
        int tries = 0;
        while (newSlot == session.currentSlot && tries < 10) {
            newSlot = ACTIVE_SLOTS[random.nextInt(ACTIVE_SLOTS.length)];
            tries++;
        }

        session.currentSlot = newSlot;
        session.phaseIndex = 0;
        updateTimingItemAtSlot(inv, session.currentSlot, session.phaseIndex);
    }

    // ------------------------------------------------------------------------
    // CLICK / CLOSE HANDLING
    // ------------------------------------------------------------------------

    public void handleGuiClick(Player player, int slot) {
        UUID id = player.getUniqueId();
        TrainingSession session = sessions.get(id);
        if (session == null) return;

        // Only count clicks on the active button slot
        if (slot != session.currentSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.25f, 0.7f);
            return;
        }

        int phase = session.phaseIndex;
        int delta = Math.abs(phase - 3); // distance from "perfect"

        int gainedScore;
        String msg;
        float basePitch;

        if (delta == 0) {
            gainedScore = 15;
            msg = ChatColor.GREEN + "Perfect timing!";
            basePitch = 1.4f;
        } else if (delta == 1) {
            gainedScore = 12;
            msg = ChatColor.GREEN + "Great timing!";
            basePitch = 1.2f;
        } else if (delta == 2) {
            gainedScore = 8;
            msg = ChatColor.YELLOW + "Good timing.";
            basePitch = 1.0f;
        } else {
            gainedScore = 2;
            msg = ChatColor.GRAY + "Off timing.";
            basePitch = 0.85f;
        }

        session.score += gainedScore;
        session.notesDone++;
        session.phaseIndex = 0;

        // Combo handling
        if (delta <= 2) {
            session.combo++;
        } else {
            session.combo = 0;
        }

        // Combo-based pitch (slightly rising with combo, capped)
        int cappedCombo = Math.min(session.combo, 10);
        float pitch = basePitch + (cappedCombo * 0.03f);

        player.sendMessage(msg);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, pitch);

        // Move button after each successful click
        if (session.notesDone >= session.noteCount) {
            endSession(session, false);
        } else {
            moveButtonToNewSlot(session);
        }
    }

    public void handleGuiClose(Player player) {
        TrainingSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        endSession(session, true);
    }

    private void endSession(TrainingSession session, boolean closedEarly) {
        sessions.remove(session.playerId);

        if (session.taskId != 0) {
            Bukkit.getScheduler().cancelTask(session.taskId);
        }

        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null) return;

        if (closedEarly && session.notesDone == 0) {
            player.sendMessage(ChatColor.RED + "Training cancelled.");
            return;
        }

        int maxScore = session.noteCount * 15;
        int score = session.score;
        double ratio = maxScore <= 0 ? 0.0 : (score / (double) maxScore);

        int xpMin = session.tier.getXpMin();
        int xpMax = session.tier.getXpMax();
        int xpGain = xpMin + (int) Math.round((xpMax - xpMin) * ratio);

        if (xpGain <= 0) {
            player.sendMessage(ChatColor.RED + "You gained no training XP.");
            return;
        }

        addTrainingXp(player, xpGain);

        player.sendMessage(ChatColor.AQUA + "Training complete! " +
                ChatColor.GREEN + "+" + xpGain + " Training XP" +
                ChatColor.GRAY + " (" + session.tier.getDisplayName() + ")");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
    }

    // ------------------------------------------------------------------------
    // TRAINING XP / TL
    // ------------------------------------------------------------------------

    public void addTrainingXp(Player player, int amount) {
        if (amount <= 0) return;

        PlayerProfile profile = profiles.getProfile(player);
        int tl = profile.getTrainingLevel();
        int xp = profile.getTrainingXp();

        int remaining = amount;

        while (remaining > 0 && tl < MAX_TRAINING_LEVEL) {
            int needed = getXpNeededForNext(tl);
            int canFill = needed - xp;

            if (remaining >= canFill) {
                remaining -= canFill;
                xp = 0;
                tl++;

                profile.setTrainingLevel(tl);
                profile.setTrainingXp(xp);

                player.sendMessage(ChatColor.GOLD + "Your Training Level increased to " +
                        ChatColor.YELLOW + tl + ChatColor.GOLD + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);

                recalcStaminaFromTraining(profile, player);
            } else {
                xp += remaining;
                remaining = 0;
                profile.setTrainingXp(xp);
            }
        }

        profiles.saveProfile(profile);
    }

    private int getXpNeededForNext(int tl) {
        switch (tl) {
            case 0:  return 120;
            case 1:  return 140;
            case 2:  return 160;
            case 3:  return 180;
            case 4:  return 200;
            case 5:  return 230;
            case 6:  return 260;
            case 7:  return 290;
            case 8:  return 320;
            case 9:  return 350;
            case 10: return 380;
            case 11: return 420;
            case 12: return 460;
            case 13: return 500;
            case 14: return 540;
            case 15: return 600;
            case 16: return 650;
            case 17: return 700;
            case 18: return 750;
            case 19: return 800;
            case 20: return 900;
            case 21: return 1000;
            case 22: return 1100;
            case 23: return 1200;
            case 24: return 1300;
            default:
                return 1300;
        }
    }

    private void recalcStaminaFromTraining(PlayerProfile profile, Player player) {
        int tl = profile.getTrainingLevel();

        int baseCap = 100 + (tl * 4);
        double baseRegen = 6.0 + (tl * 0.1);

        profile.setStaminaCap(baseCap);

        double current = profile.getStaminaCurrent();
        if (current > baseCap) current = baseCap;
        profile.setStaminaCurrent(current);

        profile.setStaminaRegenPerSecond(baseRegen);

        player.sendMessage(ChatColor.DARK_AQUA + "Your stamina has improved with training!");
    }

    // ------------------------------------------------------------------------
    // COMPAT HELPERS
    // ------------------------------------------------------------------------

    public ItemStack createToken(TrainingTokenTier tier, int amount) {
        return createTokenItem(tier, amount);
    }

    public void sendTrainingInfo(Player player) {
        PlayerProfile profile = profiles.getProfile(player);
        int tl = profile.getTrainingLevel();
        int xp = profile.getTrainingXp();
        int needed = (tl < MAX_TRAINING_LEVEL) ? getXpNeededForNext(tl) : 0;

        player.sendMessage(ChatColor.GOLD + "===== Training Info =====");
        player.sendMessage(ChatColor.AQUA + "Training Level: " +
                ChatColor.YELLOW + tl +
                (tl >= MAX_TRAINING_LEVEL ? ChatColor.GREEN + " (MAX)" : ""));
        if (tl < MAX_TRAINING_LEVEL) {
            player.sendMessage(ChatColor.AQUA + "Training XP: " +
                    ChatColor.YELLOW + xp +
                    ChatColor.GRAY + " / " +
                    ChatColor.YELLOW + needed);
        }
        player.sendMessage(ChatColor.GRAY + "Buy a Training Token from a trainer/vendor");
        player.sendMessage(ChatColor.GRAY + "and " + ChatColor.GREEN + "right-click it" +
                ChatColor.GRAY + " to start the minigame.");
    }

    public void startTrainingWithToken(Player player) {
        player.sendMessage(ChatColor.RED + "Training is now done via Training Tokens.");
        player.sendMessage(ChatColor.GRAY + "Purchase a token and " +
                ChatColor.GREEN + "right-click it" +
                ChatColor.GRAY + " to begin training.");
    }
}
