package myplugins.docks.pool;

import myplugins.docks.pool.command.CreatePollCommand;
import myplugins.docks.pool.command.PollCommand;
import myplugins.docks.pool.gui.GUIListener;
import myplugins.docks.pool.service.PollManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Pool extends JavaPlugin {

    private static Pool instance;

    private PollManager pollManager;
    private GUIListener guiListener;

    public static Pool getInstance() {
        return instance;
    }

    public PollManager getPollManager() {
        return pollManager;
    }

    public GUIListener getGuiListener() {
        return guiListener;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Ensure plugin folder exists (no config.yml needed)
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Poll manager (uses SQLite database now)
        pollManager = new PollManager(this);
        pollManager.load(); // no-op but keeps API

        // GUI + chat listener
        guiListener = new GUIListener(this, pollManager);
        Bukkit.getPluginManager().registerEvents(guiListener, this);

        // Register commands
        if (getCommand("createpoll") != null) {
            getCommand("createpoll").setExecutor(new CreatePollCommand(this, pollManager));
        }

        if (getCommand("poll") != null) {
            PollCommand pollCommand = new PollCommand(this, pollManager);
            getCommand("poll").setExecutor(pollCommand);
            getCommand("poll").setTabCompleter(pollCommand);
        }

        // Expire polls every 30 seconds
        Bukkit.getScheduler().runTaskTimer(this, pollManager::expirePolls, 20L, 20L * 30);

        getLogger().info("Pool plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (pollManager != null) {
            pollManager.close(); // closes DB connection
        }
        getLogger().info("Pool plugin disabled.");
    }
}
