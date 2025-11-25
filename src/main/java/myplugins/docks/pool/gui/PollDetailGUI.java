package myplugins.docks.pool.gui;

import myplugins.docks.pool.model.Poll;
import myplugins.docks.pool.model.PollOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PollDetailGUI {

    private static final int[] OPTION_SLOTS = {10, 11, 12, 14, 15, 16};

    public Inventory build(Player viewer, Poll poll) {
        int size = 27;

        DetailHolder holder = new DetailHolder(poll.getId());
        Inventory inv = Bukkit.createInventory(holder, size,
                ChatColor.DARK_AQUA + "Poll #" + poll.getId());
        holder.setInventory(inv);

        int i = 0;
        for (PollOption option : poll.getOptions().values()) {
            if (i >= OPTION_SLOTS.length) break;
            int slot = OPTION_SLOTS[i];

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&b" + option.getText()));
                List<String> lore = new ArrayList<>();
                lore.add(color("&7Votes: &f" + option.getVotes()));
                lore.add("");
                lore.add(color("&eClick to vote for this option"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            i++;
        }

        return inv;
    }

    public int getOptionIndexBySlot(int slot) {
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            if (OPTION_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
