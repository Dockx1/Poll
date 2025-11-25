package myplugins.docks.pool.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class BrowseHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Integer> slotToPollId = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setPollIdForSlot(int slot, int pollId) {
        slotToPollId.put(slot, pollId);
    }

    public int getPollIdForSlot(int slot) {
        return slotToPollId.getOrDefault(slot, -1);
    }
}
