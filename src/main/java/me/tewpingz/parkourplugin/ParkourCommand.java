package me.tewpingz.parkourplugin;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("parkour")
@Description("A command that allows your to add, and remove checkpoints")
public class ParkourCommand extends BaseCommand {
    @Default
    @HelpCommand
    public void doHelp(Player player, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("createroute")
    public void createRoute(Player player, String name) {
        ParkourRoute route = ParkourPlugin.getInstance().getParkourManager().getRoute(name);

        // Check if route already exists if it does then do not allow the player to create another route
        if (route != null) {
            player.sendMessage(ChatColor.RED + "There is already a route with that name.");
            return;
        }

        ParkourPlugin.getInstance().getParkourManager().addRoute(new ParkourRoute(name));
        player.sendMessage(ChatColor.GREEN + "Created a route named %s".formatted(name));
    }

    @Subcommand("removeroute")
    public void removeRoute(Player player, ParkourRoute route) {
        ParkourPlugin.getInstance().getParkourManager().removeRoute(route.getName());
        player.sendMessage(ChatColor.GREEN + "Removed a route named %s".formatted(route.getName()));
    }

    @Subcommand("addcheckpoint")
    public void addCheckpoint(Player player, ParkourRoute route) {
        route.addCheckpoint(new ParkourRoute.ParkourCheckpoint(player.getLocation()));
        player.sendMessage(ChatColor.GREEN + "Added a checkpoint at your location for the route named %s".formatted(route.getName()));
    }
}
