package myplugins.docks.pool.gui;

import myplugins.docks.pool.model.Poll;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PollBrowseGUI {

    public static final String TITLE = ChatColor.DARK_AQUA + "Active Polls";

    public Inventory build(Player viewer, List<Poll> polls) {
        int size = ((polls.size() - 1) / 9 + 1) * 9;
        size = Math.max(size, 9);

        BrowseHolder holder = new BrowseHolder();
        Inventory inv = Bukkit.createInventory(holder, size, TITLE);
        holder.setInventory(inv);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        int slot = 0;
        for (Poll poll : polls) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&bPoll #" + poll.getId()));
                List<String> lore = new ArrayList<>();
                lore.add(color("&f" + shorten(poll.getQuestion(), 40)));
                lore.add("");
                lore.add(color("&7Created: &f" + fmt.format(new Date(poll.getCreatedAt()))));
                lore.add(color("&7Ends: &f" + fmt.format(new Date(poll.getEndsAt()))));
                lore.add(color("&7Votes: &f" + poll.getTotalVotes()));
                lore.add("");
                lore.add(color("&eClick to open"));
                meta.setLore(lore);
                paper.setItemMeta(meta);
            }
            inv.setItem(slot, paper);
            holder.setPollIdForSlot(slot, poll.getId());
            slot++;
        }

        return inv;
    }

    private String shorten(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
