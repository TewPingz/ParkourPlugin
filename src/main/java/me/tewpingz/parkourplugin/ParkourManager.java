package me.tewpingz.parkourplugin;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourManager {

    @Getter
    private final ParkourPersistence persistence;
    private final Map<String, ParkourRoute> parkourRoutes;

    public ParkourManager(ParkourPlugin plugin) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.persistence = new ParkourPersistence(plugin);
        this.parkourRoutes = new ConcurrentHashMap<>();
        this.loadRoutes();
    }

    public ParkourRoute getRoute(String name) {
        Objects.requireNonNull(name);
        return this.parkourRoutes.get(name.toLowerCase());
    }

    public ParkourRoute getRouteByRegion(Location location) {
        Objects.requireNonNull(location);
        for (ParkourRoute value : this.parkourRoutes.values()) {
            ProtectedRegion region = value.getRegion(location.getWorld());
            if (region == null) {
                continue;
            }

            if (region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                return value;
            }
        }
        return null;
    }

    public void addRoute(ParkourRoute route) {
        Objects.requireNonNull(route);
        this.parkourRoutes.put(route.getName().toLowerCase(), route);
        this.persistence.getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insert = connection.prepareStatement("INSERT INTO ROUTES VALUES(?)");
                insert.setString(1, route.getName());
                insert.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void removeRoute(String routeName) {
        Objects.requireNonNull(routeName);
        ParkourRoute route = this.parkourRoutes.remove(routeName.toLowerCase());
        if (route != null) { // This means we actually deleted a route
            this.persistence.getConnectionAsync().thenAccept(connection -> {
                try {
                    PreparedStatement deleteRoute = connection.prepareStatement("DELETE FROM ROUTES WHERE ROUTE_NAME=?");
                    deleteRoute.setString(1, route.getName());
                    deleteRoute.executeUpdate();

                    PreparedStatement deleteLeaderboards = connection.prepareStatement("DELETE FROM ROUTE_LEADERBOARD_ENTRIES WHERE ROUTE_NAME=?");
                    deleteLeaderboards.setString(1, route.getName());
                    deleteLeaderboards.executeUpdate();

                    PreparedStatement deleteCheckpoints = connection.prepareStatement("DELETE FROM ROUTE_CHECKPOINTS WHERE ROUTE_NAME=?");
                    deleteCheckpoints.setString(1, route.getName());
                    deleteCheckpoints.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void loadRoutes() {
        this.persistence.getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM ROUTES");
                ResultSet resultSet = selectStatement.executeQuery();

                while (resultSet.next()) {
                    try {
                        String name = resultSet.getString("ROUTE_NAME");
                        ParkourRoute parkourRoute = new ParkourRoute(name);
                        parkourRoute.loadCheckpoints(connection);
                        parkourRoute.loadParkourLeaderboard(connection);
                        this.parkourRoutes.put(name.toLowerCase(), parkourRoute);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

                System.out.printf("Loaded %s routes%n", this.parkourRoutes.size());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
