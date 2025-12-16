package com.animesmp.core.gui;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityRegistry;
import com.animesmp.core.ability.AbilityType;
import com.animesmp.core.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AbilityLoadoutGui {

    public static final String TITLE = ChatColor.DARK_AQUA + "Ability Loadout";
    private final AnimeSMPPlugin plugin;
    private final AbilityRegistry registry;

    private final NamespacedKey abilityIdKey;

    public AbilityLoadoutGui(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getAbilityRegistry();
        this.abilityIdKey = new NamespacedKey(plugin, "loadout_ability_id");
    }

    public NamespacedKey getAbilityIdKey() {
        return abilityIdKey;
    }

    public void open(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        Inventory inv = Bukkit.createInventory(new AbilityLoadoutHolder(), 54, TITLE);

        // top 36 slots: unlocked abilities
        List<String> unlocked = new ArrayList<>(profile.getUnlockedAbilities());
        unlocked.sort(String::compareToIgnoreCase);

        int slot = 0;
        for (String id : unlocked) {
            Ability a = registry.getAbility(id);
            if (a == null) continue;

            ItemStack item = buildAbilityItem(a);
            inv.setItem(slot, item);
            slot++;
            if (slot >= 36) break;
        }

        // bottom loadout slots 1..5 at 45..49
        for (int i = 1; i <= 5; i++) {
            inv.setItem(44 + i, buildSlotPlaceholder(i));
            String boundId = profile.getBoundAbilityId(i);
            if (boundId != null) {
                Ability a = registry.getAbility(boundId);
                if (a != null) inv.setItem(44 + i, buildAbilityItem(a));
            }
        }

        // Save button
        inv.setItem(53, buildSaveButton());

        player.openInventory(inv);
    }

    private ItemStack buildAbilityItem(Ability a) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return book;

        meta.setDisplayName(ChatColor.AQUA + a.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + a.getType().getDisplayName());
        lore.add(ChatColor.GRAY + "Rarity: " + ChatColor.WHITE + a.getTier().name());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Drag into slots 1–5.");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(abilityIdKey, PersistentDataType.STRING, a.getId().toLowerCase(Locale.ROOT));
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack buildSlotPlaceholder(int slot) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta == null) return pane;

        meta.setDisplayName(ChatColor.DARK_GRAY + "Slot " + ChatColor.YELLOW + slot);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Drop an ability here"));
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack buildSaveButton() {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta meta = emerald.getItemMeta();
        if (meta == null) return emerald;

        meta.setDisplayName(ChatColor.GREEN + "Save Loadout");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Enforces:",
                ChatColor.YELLOW + "• 1 Movement",
                ChatColor.YELLOW + "• 2 Damage",
                ChatColor.YELLOW + "• 1 Utility/Defense",
                ChatColor.YELLOW + "• 1 Ultimate",
                "",
                ChatColor.GRAY + "No duplicates."
        ));
        emerald.setItemMeta(meta);
        return emerald;
    }

    public boolean validateAndApply(Player player, Inventory inv) {
        // bottom slots 45..49 represent slots 1..5
        Map<Integer, String> chosen = new HashMap<>();
        Set<String> dedupe = new HashSet<>();

        int movement = 0, damage = 0, util = 0, ultimate = 0;

        for (int i = 1; i <= 5; i++) {
            ItemStack it = inv.getItem(44 + i);
            String id = readAbilityId(it);
            if (id == null) {
                chosen.put(i, null);
                continue;
            }

            if (!dedupe.add(id)) {
                player.sendMessage(ChatColor.RED + "Duplicate ability detected: " + id);
                return false;
            }

            Ability a = registry.getAbility(id);
            if (a == null) {
                player.sendMessage(ChatColor.RED + "Invalid ability: " + id);
                return false;
            }

            AbilityType t = a.getType();
            if (t == AbilityType.MOVEMENT) movement++;
            else if (t == AbilityType.ULTIMATE) ultimate++;
            else if (t == AbilityType.UTILITY || t == AbilityType.DEFENSE) util++;
            else damage++; // DAMAGE_LIGHT / DAMAGE_HEAVY

            chosen.put(i, id);
        }

        if (movement > 1 || ultimate > 1 || util > 1 || damage > 2) {
            player.sendMessage(ChatColor.RED + "Loadout limits exceeded.");
            player.sendMessage(ChatColor.GRAY + "Movement=" + movement + " Damage=" + damage + " Utility/Defense=" + util + " Ultimate=" + ultimate);
            return false;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        for (int i = 1; i <= 5; i++) {
            profile.setBoundAbilityId(i, chosen.get(i));
        }
        plugin.getProfileManager().saveProfile(profile);

        player.sendMessage(ChatColor.GREEN + "Loadout saved.");
        return true;
    }

    public String readAbilityId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(abilityIdKey, PersistentDataType.STRING);
    }
}
