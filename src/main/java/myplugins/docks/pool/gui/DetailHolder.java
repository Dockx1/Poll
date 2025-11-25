package myplugins.docks.pool.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class DetailHolder implements InventoryHolder {

    private Inventory inventory;
    private final int pollId;

    public DetailHolder(int pollId) {
        this.pollId = pollId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getPollId() {
        return pollId;
    }
}
