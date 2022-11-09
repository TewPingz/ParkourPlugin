package me.tewpingz.parkourplugin;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

@Getter
public final class ParkourPlugin extends JavaPlugin {

    @Getter
    private static ParkourPlugin instance;

    private ParkourProfileManager profileManager;
    private ParkourManager parkourManager;
    private ParkourScoreboard parkourScoreboard;

    @Override
    public void onEnable() {
        instance = this;

        // Register profile manager
        this.profileManager = new ParkourProfileManager();

        // Register database
        try {
            this.parkourManager = new ParkourManager(this);
        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            this.getLogger().severe("Failed to load MYSQL drivers");
            this.getServer().shutdown();
            throw new RuntimeException(e);
        }

        // Register our aikar command manager
        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.getCommandContexts().registerContext(ParkourRoute.class, context -> {
            String name = context.popFirstArg();
            ParkourRoute route = this.parkourManager.getRoute(name);

            if (route == null) {
                throw new InvalidCommandArgument("That route does not exist.");
            }

            return route;
        });
        commandManager.registerCommand(new ParkourCommand());

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        // Register scoreboard
        this.parkourScoreboard = new ParkourScoreboard(this);
    }

    @Override
    public void onDisable() {
        try {
            this.getLogger().info("Attempting to close MySQL peacefully");
            this.parkourManager.getPersistence().shutdown();
            this.getLogger().info("Successfully closed MySQL");
        } catch (SQLException e) {
            this.getLogger().info("Failed to close MySQL");
            throw new RuntimeException(e);
        }
    }
}
