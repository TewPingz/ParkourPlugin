package me.tewpingz.parkourplugin;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class ParkourListener implements Listener {

    private final ParkourPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getParkourScoreboard().addBoard(event.getPlayer());
        this.plugin.getProfileManager().addProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if the player moved block positions, otherwise no point checking.
        if (from.getBlockY() == to.getBlockY() && from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        ParkourRoute regionRoute = this.plugin.getParkourManager().getRouteByRegion(event.getTo());
        ParkourProfileManager.ParkourProfile parkourProfile = this.plugin.getProfileManager().getProfile(player.getUniqueId());

        // Check if region is null, it either means they were never in one or left one
        if (regionRoute == null) {
            // If they have a start time, and they left the region then alert them and remove them from the list
            if (parkourProfile.getStartTime() != -1) {
                parkourProfile.setStartTime(-1);
                parkourProfile.setExpectedCheckpoint(null);
                player.sendMessage(ChatColor.RED + "Due to the fact you left the region for the route, your time has been cancelled.");
            }
            return;
        }

        // Get the first point of the route
        ParkourRoute.ParkourCheckpoint startPoint = regionRoute.getFirstCheckpoint();

        // Check if the point is null
        if (startPoint == null) {
            return;
        }

        // Get the next point of the route
        ParkourRoute.ParkourCheckpoint secondCheckpoint = regionRoute.getNextCheckpoint(startPoint);

        /*  Check if the point is null, if it is it means the route is not setup,
            and we should not let it through because then. We will have a parkour that can be finished instantly.
        */
        if (secondCheckpoint == null) {
            return;
        }

        // Check if the start time exists
        if (parkourProfile.getStartTime() != -1) {
            // Get the expected point the player is supposed to be at
            ParkourRoute.ParkourCheckpoint checkpoint = parkourProfile.getExpectedCheckpoint();
            // Check if they are at that point
            if (checkpoint.checkLocation(to)) {
                // Check if the current point they are at has a next checkpoint
                ParkourRoute.ParkourCheckpoint nextCheckpoint = regionRoute.getNextCheckpoint(checkpoint);
                if (nextCheckpoint == null) {
                    // If it doesn't it means they have finished.
                    long endTime = System.currentTimeMillis();
                    parkourProfile.setExpectedCheckpoint(null);
                    long startTime = parkourProfile.getStartTime();
                    parkourProfile.setStartTime(-1);
                    double timeTaken = (endTime - startTime) / 1000D;
                    player.sendMessage(ChatColor.GREEN + "You have completed the parkour route named %s in %s seconds.".formatted(regionRoute.getName(), timeTaken));
                    ParkourRoute.ParkourLeaderboardEntry entry = new ParkourRoute.ParkourLeaderboardEntry(player.getUniqueId(), timeTaken);
                    regionRoute.addLeaderboardEntry(entry);
                } else {
                    // If it does then add it to the next point and tell them where to go.
                    parkourProfile.setExpectedCheckpoint(nextCheckpoint);
                    player.sendMessage(" ");
                    player.sendMessage(ChatColor.GOLD + "You hit a checkpoint good-job!");
                    player.sendMessage(ChatColor.YELLOW + "The next checkpoint is at %s,%s,%s".formatted(nextCheckpoint.getX(), nextCheckpoint.getY(), nextCheckpoint.getZ()));
                    player.sendMessage(" ");
                }
                return;
            }
        }

        // If they have not started or have come back to the point after going to another checkpoint then reset them.
        if (startPoint.checkLocation(to)) {
            if (parkourProfile.getStartTime() != -1) {
                player.sendMessage(ChatColor.GOLD + "You have just re-started the %s regionRoute".formatted(regionRoute.getName()));
            } else {
                player.sendMessage(ChatColor.GOLD + "You have just started the %s regionRoute".formatted(regionRoute.getName()));
            }
            parkourProfile.setStartTime(System.currentTimeMillis());
            parkourProfile.setExpectedCheckpoint(secondCheckpoint);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getProfileManager().removeProfile(player.getUniqueId());
        this.plugin.getParkourScoreboard().removeBoard(player.getUniqueId());
    }
}
