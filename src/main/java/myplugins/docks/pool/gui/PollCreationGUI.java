package myplugins.docks.pool.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PollCreationGUI {

    public static final String TITLE = ChatColor.DARK_AQUA + "Create Poll Options";

    private static final int[] OPTION_SLOTS = {10, 11, 12, 14, 15, 16};

    private final Map<UUID, CreationSession> sessions = new HashMap<>();

    public CreationSession startSession(Player player, long durationMillis, String question) {
        CreationSession session = new CreationSession(player.getUniqueId(), question, durationMillis);
        sessions.put(player.getUniqueId(), session);
        openFor(player, session);
        return session;
    }

    public CreationSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public void removeSession(UUID playerId) {
        sessions.remove(playerId);
    }

    public void openFor(Player player, CreationSession session) {
        CreationHolder holder = new CreationHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            int slot = OPTION_SLOTS[i];
            String text = session.getOptions().get(i);
            inv.setItem(slot, buildOptionItem(i, text));
        }

        inv.setItem(22, buildFinishItem(session.hasAtLeastTwoOptions()));
        inv.setItem(26, buildCancelItem());

        player.openInventory(inv);
    }

    public int getOptionIndexBySlot(int slot) {
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            if (OPTION_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack buildOptionItem(int index, String text) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (text == null || text.isBlank()) {
                meta.setDisplayName(color("&eSet option " + (index + 1)));
                meta.setLore(Collections.singletonList(color("&7Click to type the option text")));
            } else {
                meta.setDisplayName(color("&aOption " + (index + 1) + ": &f" + text));
                meta.setLore(Collections.singletonList(color("&7Click to edit this option")));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildFinishItem(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(enabled
                    ? "&aFinish & Create Poll"
                    : "&cNeed at least 2 options"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&cCancel"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
