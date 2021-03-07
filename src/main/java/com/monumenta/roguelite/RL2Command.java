package com.monumenta.roguelite;

import java.util.ArrayList;

import com.monumenta.roguelite.objects.Dungeon;
import com.monumenta.roguelite.objects.DungeonReader;
import com.monumenta.roguelite.objects.Room;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RL2Command implements CommandExecutor {

    private ArrayList<Room> rooms;
    private Plugin plugin;

    RL2Command(Plugin p) {
        this.plugin = p;
        this.rooms = FileParser.loadFiles(p, null);
    }

    // displays help message for the command /rl2
    private void rl2Help(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "rl2 - Roguelite dungeon plugin\n" +
                "avaiable sub-commands:\n" +
                "/rl2 generate | Dungeon generation (WARNING: STARTS A DUNGEON GENERATION WITHOUT WARNING. IT IS NOT UNDOABLE)\n" +
                "/rl2 reload | reloads internal data files\n" +
                "/rl2 savestructure | save a in-game structure into internal data files"));
    }

    private static Location getSenderLocation(CommandSender sender) {
        if (sender instanceof BlockCommandSender){
            return ((BlockCommandSender) sender).getBlock().getLocation();
        }
        if (sender instanceof Player) {
            return ((Player) sender).getLocation();
        }
        //should not happen. hopefully
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof BlockCommandSender)) {
            sender.sendMessage("you cannot use this command as something else than a player or a command block.");
        }
        if (args.length == 0) {
            this.rl2Help(sender);
            return false;
        }
        Location loc = getSenderLocation(sender);
        switch (args[0]) {
            case "help":
                this.rl2Help(sender);
                return true;
            case "generate":
                Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
                    Dungeon dungeon = new Dungeon(this.rooms, loc, this.plugin, true);
                    dungeon.calculateWithRetries(5);
                    dungeon.spawn();
                }, 0);

                return true;
            case "savestructure":
                new StructureParser(this.plugin, loc, sender, args).startParser();
                return true;
            case "reload":
                this.rooms = FileParser.loadFiles(this.plugin, sender);
                sender.sendMessage("" + this.rooms.size() + " Files reloaded");
                return true;
            case "stats":

                if (args.length < 2) {
                    sender.sendMessage("Failed. you need to specify the amounts on dungeons to be calculated");
                    return false;
                }
                boolean force = false;
                if (args.length >= 3 && args[2].equals("confirm")) {
                    force = true;
                }
                boolean finalForce = force;
                Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
                    DungeonReader reader = new DungeonReader(this.rooms, this.plugin, sender, loc);
                    reader.read(Integer.parseInt(args[1]), finalForce);
                    reader.output();
                }, 0);
                return true;
        }
        return false;
    }
}
