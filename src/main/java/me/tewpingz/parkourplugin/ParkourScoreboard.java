package me.tewpingz.parkourplugin;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourScoreboard implements Runnable {
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public ParkourScoreboard(ParkourPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, 2L, 2L);
    }

    public void addBoard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(ChatColor.RED + "Parkour");
        this.boards.put(player.getUniqueId(), board);
    }

    public void removeBoard(UUID uuid) {
        this.boards.remove(uuid);
    }

    private Collection<String> lines(Player player) {
        Collection<String> lines = new ArrayList<>();
        ParkourRoute regionRoute = ParkourPlugin.getInstance().getParkourManager().getRouteByRegion(player.getLocation());

        if (regionRoute == null) {
            lines.add(ChatColor.GOLD + "You are not in a parkour region!");
            return lines;
        }

        lines.add(ChatColor.GOLD + "Route name: " + ChatColor.WHITE + regionRoute.getName());
        lines.add("");

        // Current attempt
        ParkourProfileManager.ParkourProfile parkourProfile = ParkourPlugin.getInstance().getProfileManager().getProfile(player.getUniqueId());
        long startTime = parkourProfile.getStartTime();
        if (startTime != -1) {
            long different = System.currentTimeMillis() - startTime;
            double time = different / 1000D;
            lines.add(ChatColor.GOLD + "Current attempt: " + ChatColor.WHITE + time);
            lines.add(" ");
        }

        // Best attempt
        double bestAttempt = regionRoute.getBestAttempt(player.getUniqueId());
        String bestAttemptDisplay = String.valueOf(bestAttempt);
        if (bestAttempt == -1) {
            bestAttemptDisplay = "N/A";
        }
        lines.add(ChatColor.GOLD + "Best time: " + ChatColor.WHITE + bestAttemptDisplay);

        lines.add("");
        lines.add(ChatColor.GOLD + "Leaderboard:");
        for (int i = 0; i < Math.min(5, regionRoute.getAttempts()); i++) {
            ParkourRoute.ParkourLeaderboardEntry entry = regionRoute.getEntryByIndex(i);
            String name = Bukkit.getOfflinePlayer(entry.getPlayerId()).getName();
            lines.add(ChatColor.GOLD + " #" + (i + 1) + " - " + name + " - " + entry.getTime());
        }

        return lines;
    }

    @Override
    public void run() {
        this.boards.forEach((uuid, fastBoard) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                fastBoard.updateLines(this.lines(player));
            }
        });
    }
}
