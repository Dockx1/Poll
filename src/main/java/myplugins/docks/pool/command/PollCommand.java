package myplugins.docks.pool.command;

import myplugins.docks.pool.Pool;
import myplugins.docks.pool.gui.PollBrowseGUI;
import myplugins.docks.pool.model.Poll;
import myplugins.docks.pool.model.PollOption;
import myplugins.docks.pool.service.PollManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PollCommand implements CommandExecutor, TabCompleter {

    private final Pool plugin;
    private final PollManager pollManager;

    public PollCommand(Pool plugin, PollManager pollManager) {
        this.plugin = plugin;
        this.pollManager = pollManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /poll -> open GUI of active polls
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&cOnly players can open the poll GUI."));
                return true;
            }

            List<Poll> active = pollManager.getActivePolls();
            if (active.isEmpty()) {
                player.sendMessage(color("&eThere are no active polls."));
                return true;
            }

            PollBrowseGUI browseGUI = plugin.getGuiListener().getBrowseGUI();
            player.openInventory(browseGUI.build(player, active));
            return true;
        }

        // management subcommands: close, remove, results
        if (!sender.hasPermission("polls.manage")) {
            sender.sendMessage(color("&cYou do not have permission to manage polls."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("close")) {
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid poll id."));
                return true;
            }

            boolean ok = pollManager.closePoll(id);
            sender.sendMessage(ok
                    ? color("&aClosed poll #" + id)
                    : color("&cPoll not found."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("remove")) {
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid poll id."));
                return true;
            }

            boolean ok = pollManager.removePoll(id);
            sender.sendMessage(ok
                    ? color("&aRemoved poll #" + id)
                    : color("&cPoll not found."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("results")) {
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid poll id."));
                return true;
            }

            Poll poll = pollManager.getPoll(id);
            if (poll == null) {
                sender.sendMessage(color("&cPoll not found."));
                return true;
            }

            sendResults(sender, poll);
            return true;
        }

        sender.sendMessage(color("&cUsage: /poll"));
        sender.sendMessage(color("&cUsage: /poll close <id>"));
        sender.sendMessage(color("&cUsage: /poll remove <id>"));
        sender.sendMessage(color("&cUsage: /poll results <id>"));
        return true;
    }

    private void sendResults(CommandSender sender, Poll poll) {
        int total = poll.getTotalVotes();
        sender.sendMessage(color("&8&m----------------------------------------"));
        sender.sendMessage(color("&bPoll #" + poll.getId() + " &7- &f" + poll.getQuestion()));
        sender.sendMessage(color("&7Status: " + (poll.isClosed()
                ? "&cClosed"
                : "&aOpen")));
        sender.sendMessage(color("&7Total votes: &f" + total));
        sender.sendMessage(color("&7Results:"));

        for (Map.Entry<Integer, PollOption> entry : poll.getOptions().entrySet()) {
            PollOption opt = entry.getValue();
            int votes = opt.getVotes();
            double percent = total == 0 ? 0.0 : (votes * 100.0 / total);
            String pctStr = String.format("%.1f", percent);

            sender.sendMessage(color("  &b[" + pctStr + "%] &f" + opt.getText()
                    + " &7- &e" + votes + " vote" + (votes == 1 ? "" : "s")));
        }
        sender.sendMessage(color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("polls.manage")) {
                String a = args[0].toLowerCase();
                if ("close".startsWith(a)) result.add("close");
                if ("remove".startsWith(a)) result.add("remove");
                if ("results".startsWith(a)) result.add("results");
            }
        } else if (args.length == 2 &&
                sender.hasPermission("polls.manage") &&
                (args[0].equalsIgnoreCase("close")
                        || args[0].equalsIgnoreCase("remove")
                        || args[0].equalsIgnoreCase("results"))) {

            for (Integer id : pollManager.getAllPollIds()) {
                result.add(String.valueOf(id));
            }
        }

        return result;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
