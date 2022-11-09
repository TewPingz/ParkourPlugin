package me.tewpingz.parkourplugin;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class ParkourPersistence {

    private Connection connection;

    private final ParkourPlugin plugin;

    private final String host, database, username, password;
    private final int port;

    public ParkourPersistence(ParkourPlugin plugin) throws ClassNotFoundException {
        plugin.getConfig().options().copyDefaults();
        plugin.saveDefaultConfig();
        FileConfiguration configuration = plugin.getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("mysql");
        this.plugin = plugin;
        this.host = section.getString("host");
        this.port = section.getInt("port");
        this.database = section.getString("database");
        this.username = section.getString("username");
        this.password = section.getString("password");
    }

    public CompletableFuture<Connection> getConnectionAsync() {
        return CompletableFuture.supplyAsync(this::attemptToGetConnection);
    }

    public void shutdown() throws SQLException {
        this.connection.close();
    }

    private Connection attemptToGetConnection() {
        try {
            if (!this.isConnectionValid()) {
                this.connect();
            }
        } catch (Exception e) {
            this.connect();
            e.printStackTrace();
        }
        return this.connection;
    }

    private boolean isConnectionValid() throws SQLException {
        return this.connection != null && !this.connection.isClosed() && this.connection.isValid(10);
    }

    private void connect() {
        try {
            this.plugin.getLogger().info("Attempting to connect to MySQL database...");

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUser(this.username);
            dataSource.setPassword(this.password);
            dataSource.setAutoReconnect(true);
            dataSource.setUrl("jdbc:mysql://%s:%s/%s".formatted(this.host, this.port, this.database));
            this.connection = dataSource.getConnection();

            String routeQuery = "CREATE TABLE if not exists ROUTES("
                    + "ROUTE_NAME VARCHAR(16) NOT NULL,"
                    + "PRIMARY KEY (ROUTE_NAME)"
                    + ")";

            String checkpointQuery = "CREATE TABLE if not exists ROUTE_CHECKPOINTS("
                    + "CHECKPOINT_ID INTEGER  NOT NULL AUTO_INCREMENT,"
                    + "CHECKPOINT_INDEX INTEGER NOT NULL,"
                    + "ROUTE_NAME VARCHAR(16) NOT NULL,"
                    + "WORLD_NAME VARCHAR(16) NOT NULL,"
                    + "X INTEGER NOT NULL,"
                    + "Y INTEGER NOT NULL,"
                    + "Z INTEGER NOT NULL,"
                    + "PRIMARY KEY (CHECKPOINT_ID)"
                    + ")";

            String leaderboardQuery = "create table if not exists ROUTE_LEADERBOARD_ENTRIES("
                    + "LEADERBOARD_ID INTEGER  NOT NULL AUTO_INCREMENT,"
                    + "ROUTE_NAME VARCHAR(16) NOT NULL,"
                    + "PLAYER_UUID VARCHAR(36) NOT NULL,"
                    + "TIME DOUBLE NOT NULL,"
                    + "PRIMARY KEY (LEADERBOARD_ID)"
                    + ")";

            this.connection.prepareStatement(routeQuery).executeUpdate();
            this.connection.prepareStatement(checkpointQuery).executeUpdate();
            this.connection.prepareStatement(leaderboardQuery).executeUpdate();
            this.plugin.getLogger().info("Connected to MySQL database");
        } catch (SQLException e) {
            this.plugin.getLogger().info("Failed to make a connection to the MySQL database.");
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getServer().shutdown());
            e.printStackTrace();
        }
    }
}
