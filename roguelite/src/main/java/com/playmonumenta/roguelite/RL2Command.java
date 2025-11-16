package com.playmonumenta.roguelite;

import com.playmonumenta.roguelite.objects.Dungeon;
import com.playmonumenta.roguelite.objects.DungeonReader;
import com.playmonumenta.roguelite.objects.Room;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RL2Command {

	private List<Room> mRooms;
	private final Plugin mPlugin;

	RL2Command(Plugin p) {
		this.mPlugin = p;
		this.mRooms = FileParser.loadRooms(p, null);

		CommandAPICommand helpCmd = new CommandAPICommand("help")
			.executes((sender, args) -> {
				rl2Help(sender);
			});

		CommandAPICommand generateCmd = new CommandAPICommand("generate")
			.executes((sender, args) -> {
				accessCheck(sender);
				Location loc = getSenderLocation(sender);
				Bukkit.getServer().getScheduler().runTaskAsynchronously(this.mPlugin, () -> {
					Dungeon dungeon = new Dungeon(this.mRooms, loc, this.mPlugin, true);
					dungeon.calculateWithRetries(5);
					dungeon.spawn();
				});
			});

		StringArgument roomIdArg = new StringArgument("Room ID");
		LocationArgument corner1 = new LocationArgument("Corner 1", LocationType.BLOCK_POSITION);
		LocationArgument corner2 = new LocationArgument("Corner 2", LocationType.BLOCK_POSITION);
		CommandAPICommand saveStructureCommand = new CommandAPICommand("savestructure")
			.withArguments(roomIdArg, corner1, corner2)
			.executes((sender, args) -> {
				accessCheck(sender);
				Location senderLoc = getSenderLocation(sender);
				String roomId = Objects.requireNonNull(args.getByArgument(roomIdArg));
				Location minLoc = Objects.requireNonNull(args.getByArgument(corner1));
				Location maxLoc = Objects.requireNonNull(args.getByArgument(corner2));
				new StructureParser(this.mPlugin, senderLoc, sender, roomId, minLoc, maxLoc).startParser();
			});

		CommandAPICommand reloadCommand = new CommandAPICommand("reload")
			.executes((sender, args) -> {
				accessCheck(sender);
				this.mRooms = FileParser.loadRooms(this.mPlugin, sender);
				sender.sendMessage(this.mRooms.size() + " Files reloaded");
			});

		IntegerArgument runCountArg = new IntegerArgument("Run Count");
		MultiLiteralArgument forceArg = new MultiLiteralArgument("Force", "confirm");
		CommandAPICommand statsCommand = new CommandAPICommand("stats")
			.withArguments(runCountArg)
			.withOptionalArguments(forceArg)
			.executes((sender, args) -> {
				accessCheck(sender);

				int runCount = args.getByArgumentOrDefault(runCountArg, 1);
				boolean force = args.getByArgumentOrDefault(forceArg, null) != null;

				if (runCount > 10000 && !force) {
					throw CommandAPI.failWithString(String.format(
						"Warning: big number chosen. the command is expected to run for approximately %d seconds.\n" +
							"enter 'confirm' as the third argument to use that amount.", (int)(runCount * 0.0005)
					));
				}

				Location loc = getSenderLocation(sender);
				Bukkit.getServer().getScheduler().runTaskAsynchronously(this.mPlugin, () -> {
					DungeonReader reader = new DungeonReader(this.mRooms, this.mPlugin, sender, loc);
					reader.read(runCount);
					reader.output();
				});
			});

		new CommandAPICommand("roguelite")
			.withPermission(CommandPermission.fromString("monumenta.roguelite"))
			.withSubcommand(helpCmd)
			.withSubcommand(generateCmd)
			.withSubcommand(saveStructureCommand)
			.withSubcommand(reloadCommand)
			.withSubcommand(statsCommand)
			.register();
	}

	private void accessCheck(CommandSender sender) throws WrapperCommandSyntaxException {
		if (!(sender instanceof Player) && !(sender instanceof BlockCommandSender)) {
			throw CommandAPI.failWithString("you cannot use this command as something else than a player or a command block.");
		}
	}

	// Displays help message for the command /rl2
	private void rl2Help(CommandSender sender) {
		sender.sendMessage(Component.text("rl2 - Roguelite dungeon plugin", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("available sub-commands:", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("/rl2 help | Shows this message", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("/rl2 generate | Dungeon generation (WARNING: STARTS A DUNGEON GENERATION WITHOUT WARNING. IT IS NOT UNDOABLE)", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("/rl2 reload | reloads internal data files", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("/rl2 savestructure | save a in-game structure into internal data files", NamedTextColor.GREEN));
		sender.sendMessage(Component.text("/rl2 stats | shows the stats of various details over a lot of dungeons", NamedTextColor.GREEN));
	}

	private static Location getSenderLocation(CommandSender sender) throws WrapperCommandSyntaxException {
		if (sender instanceof BlockCommandSender) {
			return ((BlockCommandSender) sender).getBlock().getLocation();
		}
		if (sender instanceof Player) {
			return ((Player) sender).getLocation();
		}
		throw CommandAPI.failWithString("You can only run this command as a command block or a player.");
	}
}
