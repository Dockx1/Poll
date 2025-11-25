package myplugins.docks.pool.command;

import myplugins.docks.pool.Pool;
import myplugins.docks.pool.gui.CreationSession;
import myplugins.docks.pool.gui.PollCreationGUI;
import myplugins.docks.pool.service.PollManager;
import myplugins.docks.pool.util.DurationParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreatePollCommand implements CommandExecutor {

    private final Pool plugin;
    private final PollManager pollManager;

    public CreatePollCommand(Pool plugin, PollManager pollManager) {
        this.plugin = plugin;
        this.pollManager = pollManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return true;
        }

        if (!player.hasPermission("polls.create")) {
            player.sendMessage(color("&cYou do not have permission to create polls."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /createpoll <duration> <question>"));
            return true;
        }

        String durationArg = args[0];
        long millis = DurationParser.parseToMillis(durationArg);
        if (millis <= 0) {
            player.sendMessage(color("&cInvalid duration. Example: &e1h30m &cor &e20m"));
            return true;
        }

        String question = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        PollCreationGUI gui = plugin.getGuiListener().getCreationGUI();
        CreationSession existing = gui.getSession(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(color("&cYou are already creating a poll. Finish or cancel it first."));
            return true;
        }

        gui.startSession(player, millis, question);
        player.sendMessage(color("&aStarted poll creation for question: &f" + question));
        player.sendMessage(color("&7Click the option slots to set up to 6 answers."));
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
