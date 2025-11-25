package myplugins.docks.pool.gui;

import myplugins.docks.pool.Pool;
import myplugins.docks.pool.model.Poll;
import myplugins.docks.pool.service.PollManager;
import myplugins.docks.pool.service.PollManager.VoteResult;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final Pool plugin;
    private final PollManager pollManager;

    private final PollCreationGUI creationGUI = new PollCreationGUI();
    private final PollBrowseGUI browseGUI = new PollBrowseGUI();
    private final PollDetailGUI detailGUI = new PollDetailGUI();

    public GUIListener(Pool plugin, PollManager pollManager) {
        this.plugin = plugin;
        this.pollManager = pollManager;
    }

    public PollCreationGUI getCreationGUI() {
        return creationGUI;
    }

    public PollBrowseGUI getBrowseGUI() {
        return browseGUI;
    }

    public PollDetailGUI getDetailGUI() {
        return detailGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inventory = event.getInventory();
        if (inventory == null) return;

        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof CreationHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null ||
                    event.getClickedInventory().getHolder() != holder) {
                return;
            }
            handleCreationClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (holder instanceof BrowseHolder browseHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null ||
                    event.getClickedInventory().getHolder() != holder) {
                return;
            }
            handleBrowseClick(player, browseHolder, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (holder instanceof DetailHolder detailHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null ||
                    event.getClickedInventory().getHolder() != holder) {
                return;
            }
            handleDetailClick(player, detailHolder, event.getSlot(), event.getCurrentItem());
        }
    }

    private void handleCreationClick(Player player, int slot, ItemStack current) {
        CreationSession session = creationGUI.getSession(player.getUniqueId());
        if (session == null) return;

        int optIndex = creationGUI.getOptionIndexBySlot(slot);
        if (optIndex != -1) {
            session.setAwaitingOption(optIndex);
            player.closeInventory();
            player.sendMessage(color("&eType the text for option " + (optIndex + 1) + " in chat."));
            player.sendMessage(color("&7(Only your next message will be used. Type 'cancel' to cancel.)"));
            return;
        }

        if (slot == 22) {
            if (!session.hasAtLeastTwoOptions()) {
                player.sendMessage(color("&cYou need at least 2 options to create a poll."));
                return;
            }
            Poll poll = pollManager.createPoll(
                    session.getQuestion(),
                    session.getDurationMillis(),
                    session.getOptions(),
                    player.getUniqueId() // store creator in DB
            );
            creationGUI.removeSession(player.getUniqueId());
            player.closeInventory();
            if (poll != null) {
                player.sendMessage(color("&aCreated poll &f#" + poll.getId()
                        + " &awith " + poll.getOptions().size() + " options."));
            } else {
                player.sendMessage(color("&cAn error occurred while creating the poll. Check console."));
            }
            return;
        }

        if (slot == 26) {
            creationGUI.removeSession(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(color("&cPoll creation cancelled."));
        }
    }

    private void handleBrowseClick(Player player, BrowseHolder holder, int slot, ItemStack current) {
        if (current == null) return;
        int pollId = holder.getPollIdForSlot(slot);
        if (pollId <= 0) return;

        Poll poll = pollManager.getPoll(pollId);
        if (poll == null || poll.isClosed()) {
            player.sendMessage(color("&cThis poll is no longer active."));
            player.closeInventory();
            return;
        }

        player.openInventory(detailGUI.build(player, poll));
    }

    private void handleDetailClick(Player player, DetailHolder holder, int slot, ItemStack current) {
        if (current == null) return;

        int pollId = holder.getPollId();
        Poll poll = pollManager.getPoll(pollId);
        if (poll == null) {
            player.sendMessage(color("&cPoll not found."));
            player.closeInventory();
            return;
        }

        int optionIndex = detailGUI.getOptionIndexBySlot(slot);
        if (optionIndex == -1) return;

        VoteResult result = pollManager.vote(poll, player.getUniqueId(), optionIndex);
        switch (result) {
            case SUCCESS:
                player.sendMessage(color("&aYour vote has been recorded."));
                player.closeInventory();
                break;
            case ALREADY_VOTED:
                player.sendMessage(color("&cYou have already voted on this poll."));
                break;
            case POLL_CLOSED:
                player.sendMessage(color("&cThis poll is closed."));
                player.closeInventory();
                break;
            case INVALID_OPTION:
                player.sendMessage(color("&cThat option is no longer available."));
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // You can clear sessions here if you want to force them to restart,
        // but right now we leave them so they can reopen the GUI.
    }

    @EventHandler
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        CreationSession session = creationGUI.getSession(player.getUniqueId());
        if (session == null) return;

        int awaiting = session.getAwaitingOption();
        if (awaiting == -1) return;

        String msg = event.getMessage();
        event.setCancelled(true);

        if (msg.equalsIgnoreCase("cancel")) {
            session.setAwaitingOption(-1);
            player.sendMessage(color("&cCancelled editing this option."));
            return;
        }

        session.getOptions().put(awaiting, msg);
        session.setAwaitingOption(-1);

        // Reopen GUI on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage(color("&aSet option " + (awaiting + 1) + " to: &f" + msg));
            creationGUI.openFor(player, session);
        });
    }

    // New: join message for active polls
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int active = pollManager.getActivePolls().size();
        if (active <= 0) return;

        String line = color("&8&m----------------------------------------");
        player.sendMessage(line);
        if (active == 1) {
            player.sendMessage(color("&aThere is &b1 &aactive poll."));
        } else {
            player.sendMessage(color("&aThere are &b" + active + " &aactive polls."));
        }
        player.sendMessage(color("&7Use &e/poll &7to view and vote."));
        player.sendMessage(line);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
