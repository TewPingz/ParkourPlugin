package me.tewpingz.parkourplugin;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
public class ParkourRoute {

    private final String name;
    private final List<ParkourCheckpoint> checkpoints;
    private final List<ParkourLeaderboardEntry> leaderboardEntries;

    public ParkourRoute(String name) {
        this.name = name;
        this.checkpoints = new ArrayList<>();
        this.leaderboardEntries = new ArrayList<>();
    }

    public ProtectedRegion getRegion(World world) {
        Objects.requireNonNull(world);
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        Objects.requireNonNull(bukkitWorld);
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(bukkitWorld);
        Objects.requireNonNull(regionManager);
        return regionManager.getRegion(this.name);
    }

    public ParkourCheckpoint getFirstCheckpoint() {
        if (this.checkpoints.isEmpty()) {
            return null;
        }
        return this.checkpoints.get(0);
    }

    public ParkourCheckpoint getNextCheckpoint(ParkourCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint);
        int index = this.checkpoints.indexOf(checkpoint);
        try {
            return this.checkpoints.get(index + 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void addCheckpoint(ParkourCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint);
        int index = this.checkpoints.size();
        this.checkpoints.add(checkpoint);
        ParkourPlugin.getInstance().getParkourManager().getPersistence().getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insert = connection.prepareStatement("INSERT INTO ROUTE_CHECKPOINTS (CHECKPOINT_INDEX, ROUTE_NAME, WORLD_NAME, X, Y, Z) VALUES(?,?,?,?,?,?)");
                insert.setInt(1, index);
                insert.setString(2, this.name);
                insert.setString(3, checkpoint.getWorldName());
                insert.setInt(4, checkpoint.getX());
                insert.setInt(5, checkpoint.getY());
                insert.setInt(6, checkpoint.getZ());
                insert.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void loadCheckpoints(Connection connection) throws SQLException {
        Objects.requireNonNull(connection);
        PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM ROUTE_CHECKPOINTS WHERE ROUTE_NAME=?");
        selectStatement.setString(1, this.name);
        ResultSet resultSet = selectStatement.executeQuery();
        while (resultSet.next()) {
            int checkpointId = resultSet.getInt("CHECKPOINT_INDEX");
            String worldName = resultSet.getString("WORLD_NAME");
            int x = resultSet.getInt("X");
            int y = resultSet.getInt("Y");
            int z = resultSet.getInt("Z");
            ParkourCheckpoint checkpoint = new ParkourCheckpoint(worldName, x, y, z);
            this.checkpoints.add(checkpointId, checkpoint);
        }
    }

    public void addLeaderboardEntry(ParkourLeaderboardEntry entry) {
        Objects.requireNonNull(entry);
        this.leaderboardEntries.add(entry);
        this.leaderboardEntries.sort(ParkourLeaderboardEntry::compareTo);
        ParkourPlugin.getInstance().getParkourManager().getPersistence().getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insert = connection.prepareStatement("INSERT INTO ROUTE_LEADERBOARD_ENTRIES (ROUTE_NAME, PLAYER_UUID, TIME) VALUES(?,?,?)");
                insert.setString(1, this.name);
                insert.setString(2, entry.getPlayerId().toString());
                insert.setDouble(3, entry.getTime());
                insert.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void loadParkourLeaderboard(Connection connection) throws SQLException {
        PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM ROUTE_LEADERBOARD_ENTRIES WHERE ROUTE_NAME=?");
        selectStatement.setString(1, this.name);
        ResultSet resultSet = selectStatement.executeQuery();
        while (resultSet.next()) {
            UUID playerId = UUID.fromString(resultSet.getString("PLAYER_UUID"));
            double time = resultSet.getDouble("TIME");
            ParkourLeaderboardEntry leaderboardEntry = new ParkourLeaderboardEntry(playerId, time);
            this.leaderboardEntries.add(leaderboardEntry);
        }
        this.leaderboardEntries.sort(ParkourLeaderboardEntry::compareTo);
    }

    public double getBestAttempt(UUID uuid) {
        // Since the list is sorted just go down until you fine one with the user
        for (ParkourLeaderboardEntry leaderboardEntry : this.leaderboardEntries) {
            if (leaderboardEntry.getPlayerId().equals(uuid)) {
                return leaderboardEntry.getTime();
            }
        }
        return -1;
    }

    public ParkourLeaderboardEntry getEntryByIndex(int index) {
        return this.leaderboardEntries.get(index);
    }

    public int getAttempts() {
        return this.leaderboardEntries.size();
    }

    @Data
    @RequiredArgsConstructor
    public static class ParkourCheckpoint {
        private final String worldName;
        private final int x, y, z;

        public ParkourCheckpoint(Location location) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        public boolean checkLocation(Location location) {
            return location.getWorld().getName().equalsIgnoreCase(this.worldName)
                    && location.getBlockX() == x
                    && location.getBlockY() == y
                    && location.getBlockZ() == z;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ParkourCheckpoint) {
                ParkourCheckpoint checkpoint = (ParkourCheckpoint) o;
                return this.x == checkpoint.getX()
                        && this.y == checkpoint.getY()
                        && this.z == checkpoint.getZ()
                        && checkpoint.getWorldName().equals(this.worldName);
            }
            return false;
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class ParkourLeaderboardEntry implements Comparable<ParkourLeaderboardEntry> {
        private final UUID playerId;
        private final double time;

        @Override
        public int compareTo(ParkourLeaderboardEntry o) {
            return Double.compare(this.time, o.getTime());
        }
    }
}
