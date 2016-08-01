package com.redstoner.parcels.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.redstoner.command.CommandAction;
import com.redstoner.command.CommandException;
import com.redstoner.command.CommandManager;
import com.redstoner.command.LambdaCommand;
import com.redstoner.command.Messaging;
import com.redstoner.command.Parameter;
import com.redstoner.command.ParameterType;
import com.redstoner.command.Validate;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.GlobalTrusted;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.Optional;
import com.redstoner.utils.UUIDUtil;

public final class ParcelCommands {
	
	private static final String PREFIX = "Parcels";
	
	private static final Map<Player, DuoObject<Parcel, Long>> clearQueue = new HashMap<>();
	private static final long clearRequestExpireTime = 30000;
	
	private static final ParameterType<Coord> PARCEL_TYPE = new ParameterType<Coord>("Parcel", "the ID of a parcel") {

		@Override
		protected Coord handle(String input) {
			String[] both = input.split(":");
			Validate.isTrue(both.length == 2, exceptionMessage());
			int x, z;
			try {
				x = Integer.parseInt(both[0]);
				z = Integer.parseInt(both[1]);
			} catch (NumberFormatException e) {
				throw new CommandException(exceptionMessage());
			}
			return Coord.of(x, z);
		}
			
	};
	
	public static void register() {	
		
		CommandManager.register(PREFIX, new LambdaCommand("parcel", (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP") {{
			setPermission("parcels.command");
			setDescription("manages your parcels");
			setAliases("plot", "p");
			setOnSyntaxRequest(CommandAction.CONTINUE);
			setHelpInformation("The command for anything parcel-related");
		}});
		
		CommandManager.register(new ParcelCommand("parcel auto", ParcelRequirement.IN_WORLD,
				(sender, scape) -> {
					ParcelWorld w = scape.getWorld();
					Validate.isTrue(w.getOwned(sender).length < Permissions.getParcelLimit(sender), "You have enough plots for now");
					Optional<Parcel> p = scape.getWorld().getNextUnclaimed();
					Validate.isTrue(p.isPresent(), "This world is full, please ask an admin to upsize it");
					p.get().setOwner(sender.getUniqueId());
					w.teleport(sender, p.get());
					return "Enjoy your new parcel!";
				}){{
			setDescription("sets you up with a fresh, unclaimed parcel");
			setHelpInformation("Finds the unclaimed parcel nearest to origin,", "and gives it to you");
		}});
		
		CommandManager.register(new ParcelCommand("parcel info", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					return scape.getParcel().getInfo();
				}){{
			setDescription("displays information about this parcel");
			setHelpInformation("Displays general information", "about the parcel you're on");
			setAliases("i");
		}});
		
		CommandManager.register(new ParcelCommand("parcel home", ParcelRequirement.IN_WORLD, 
				(sender, scape) -> {
					int number = scape.<Integer>getOptional("id").orElse(0);
					OfflinePlayer owner = scape.<OfflinePlayer>getOptional("player").orElse(sender);
					Validate.isTrue(owner == sender || sender.hasPermission(Permissions.PARCEL_HOME_OTHERS), "You do not have permission to teleport to other people's parcels");
					Parcel[] owned = scape.getWorld().getOwned(owner);
					Validate.isTrue(number < owned.length, "That parcel id does not exist, they have " + owned.length + " parcels");
					scape.getWorld().teleport(sender, owned[number]);
					return String.format("Teleported you to %s's parcel %s", owner.getName(), number);
				}){{
			setDescription("teleports you to parcels");
			setHelpInformation("Teleports you to your parcels,", "unless another player was specified.", "You can specify an index number if you have", "more than one parcel");
			setAliases("h");
			setParameters(new Parameter<Integer>("id", ParameterType.INTEGER, "the home id of your parcel", false),
					new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player whose parcels to teleport to", false));
		}});
		
		CommandManager.register(new ParcelCommand("parcel claim", ParcelRequirement.IN_PARCEL,
				(sender, scape) -> {
					Parcel p = scape.getParcel();
					Validate.isTrue(!p.isClaimed(), "This parcel is not available");		
					ParcelWorld w = scape.getWorld();
					Validate.isTrue(w.getOwned(sender).length < Permissions.getParcelLimit(sender), "You have enough plots for now");
					p.setOwner(sender.getUniqueId());
					return "Enjoy your new parcel!";
				}){{
			setDescription("claims this parcel");
			setHelpInformation("If this parcel is unowned, makes you the owner");
		}});
		
		CommandManager.register(new ParcelCommand("parcel option", ParcelRequirement.IN_WORLD, (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP"){{
			setDescription("changes interaction options for this parcel");
			setHelpInformation("Sets whether players who are not allowed to", "build here can interact with certain things.");
			setOnSyntaxRequest(CommandAction.CONTINUE);
		}});
		
		CommandManager.register(new ParcelCommand("parcel option inputs", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					Boolean enabled = scape.get("enabled");
					Parcel p = scape.getParcel();
					if (enabled == null) {
						String word = p.getSettings().allowsInteractInputs()? "" : "not ";
						return "This parcel does " + word + "allow using levers, buttons, etc.";
					}
					Validate.isTrue(sender.hasPermission(Permissions.ADMIN_MANAGE) || p.isOwner(sender), "You must own this parcel to change its options");
					String word = enabled? "enabled" : "disabled";
					Validate.isTrue(scape.getParcel().getSettings().setAllowsInteractInputs(enabled), "That option was already " + word);
					return "That option is now " + word;
				}){{
			setDescription("allows using inputs");
			setHelpInformation("Sets whether players who are not allowed to", "build here can use levers, buttons," + "pressure plates, tripwire or redstone ore");
			setParameters(new Parameter<Boolean>("enabled", ParameterType.BOOLEAN, "whether the option is enabled", false));
		}});
		
		CommandManager.register(new ParcelCommand("parcel option inventory", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					Boolean enabled = scape.get("enabled");
					Parcel p = scape.getParcel();
					if (enabled == null) {
						String word = p.getSettings().allowsInteractInventory()? "" : "not ";
						return "This parcel does " + word + "allow interaction with inventories";
					}
					Validate.isTrue(sender.hasPermission(Permissions.ADMIN_MANAGE) || p.isOwner(sender), "You must own this parcel to change its options");
					String word = enabled? "enabled" : "disabled";
					Validate.isTrue(scape.getParcel().getSettings().setAllowsInteractInventory(enabled), "That option was already " + word);
					return "That option is now " + word;
				}){{
			setDescription("allows editing inventories");
			setHelpInformation("Sets whether players who are not allowed to", "build here can interact with inventories");
			setParameters(new Parameter<Boolean>("enabled", ParameterType.BOOLEAN, "whether the option is enabled", false));
		}});
		
		CommandManager.register(new ParcelCommand("parcel allow", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
					OfflinePlayer allowed = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().add(allowed.getUniqueId(), true) &&
							!scape.getParcel().getOwner().filter(owner -> owner.equals(allowed.getUniqueId())).isPresent(),
							allowed.getName() + " is already allowed to build on this parcel");
					return allowed.getName() + " is now allowed to build on this parcel";
				}){{
			setDescription("allows a player to build on this parcel");
			setHelpInformation("Allows a player to build on this parcel");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to allow"));
			setAliases("add", "permit");
		}});
		
		CommandManager.register(new ParcelCommand("parcel disallow", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					OfflinePlayer forbidden = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().remove(forbidden.getUniqueId(), true),
							forbidden.getName() + " wasn't allowed to build on this parcel");
					return forbidden.getName() + " is no longer allowed to build on this parcel";
				}){{
			setDescription("disallows a player to build on this parcel");
			setHelpInformation("Disallows a player to build on this parcel,", "they won't be allowed to anymore");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to disallow"));
			setAliases("remove", "forbid");
		}});
		
		CommandManager.register(new ParcelCommand("parcel ban", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
					OfflinePlayer banned = scape.get("player");
					Validate.isTrue(!scape.getParcel().getOwner().filter(owner -> owner.equals(banned.getUniqueId())).isPresent(),
							"The owner of this parcel cannot be banned from it");
					Validate.isTrue(scape.getParcel().getAdded().add(banned.getUniqueId(), false), banned.getName() + " is already banned from this parcel");
					return banned.getName() + " is now banned from this parcel";
				}){{
			setDescription("bans a player from this parcel");
			setHelpInformation("Bans a player from this parcel,", "making them unable to enter");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to ban"));
			setAliases("deny");
		}});
		
		CommandManager.register(new ParcelCommand("parcel unban", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					OfflinePlayer unbanned = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().remove(unbanned.getUniqueId(), false), unbanned.getName() + " wasn't banned from this parcel");
					return unbanned.getName() + " is no longer banned from this parcel";
				}){{
			setDescription("unbans a player from this parcel");
			setHelpInformation("Unbans a player from this parcel,", "they will be able to enter it again");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to unban"));
			setAliases("undeny");
		}});
		
		CommandManager.register(new ParcelCommand("parcel global", ParcelRequirement.NONE, (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP"){{
			setDescription("manages your globally added players");
			setHelpInformation("Manages the players who you trust or want", "banned from all the parcels you own.");
			setOnSyntaxRequest(CommandAction.CONTINUE);
			setAliases("g");
		}});
		
		CommandManager.register(new ParcelCommand("parcel global allow", ParcelRequirement.NONE,
				(sender, scape) -> {
					OfflinePlayer allowed = scape.get("player");
					Validate.isTrue(GlobalTrusted.addPlayer(sender.getUniqueId(), allowed.getUniqueId(), true), 
							allowed.getName() + " is already globally allowed to build on your parcels");
					return allowed.getName() + " is now globally allowed to build on your parcels";
				}){{
			setDescription("Globally allows a player to build on your parcels");
			setHelpInformation("Globally allows a player to build on all", "the parcels that you own.");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to allow globally"));
			setAliases("add", "permit");
		}});
		
		CommandManager.register(new ParcelCommand("parcel global disallow", ParcelRequirement.NONE,
				(sender, scape) -> {
					OfflinePlayer forbidden = scape.get("player");
					Validate.isTrue(GlobalTrusted.removePlayer(sender.getUniqueId(), forbidden.getUniqueId(), true), 
							forbidden.getName() + " was not globally allowed to build on your parcels");
					return forbidden.getName() + " is no longer globally allowed to build on your parcels";
				}){{
			setDescription("Globally disallows a player to build on your parcels");
			setHelpInformation("Globally disallows a player to build on", "the parcels that you own.",
					"If the player is allowed to build on specific", "parcels, they can still build there.");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to disallow globally"));
			setAliases("remove", "forbid");
		}});
		
		CommandManager.register(new ParcelCommand("parcel global ban", ParcelRequirement.NONE,
				(sender, scape) -> {
					OfflinePlayer banned = scape.get("player");
					Validate.isTrue(GlobalTrusted.addPlayer(sender.getUniqueId(), banned.getUniqueId(), false), 
							banned.getName() + " is already globally banned from your parcels");
					return banned.getName() + " is now globally banned from your parcels";
				}){{
			setDescription("Globally bans a player from your parcels");
			setHelpInformation("Globally bans a player from all the parcels", "that you own, making them unable to enter.");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to ban globally"));
			setAliases("deny");
		}});
		
		CommandManager.register(new ParcelCommand("parcel global unban", ParcelRequirement.NONE,
				(sender, scape) -> {
					OfflinePlayer unbanned = scape.get("player");
					Validate.isTrue(GlobalTrusted.removePlayer(sender.getUniqueId(), unbanned.getUniqueId(), false), 
							unbanned.getName() + " was not globally banned from your parcels");
					return unbanned.getName() + " is no longer globally banned from your parcels";
				}){{
			setDescription("Globally unbans a player from your parcels");
			setHelpInformation("Globally unbans a player from all the parcels", "that you own, they can enter again.",
					"If the player is banned from specific parcels,", "they will still be banned there.");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to unban globally"));
			setAliases("undeny");
		}});
		
		CommandManager.register(new ParcelCommand("parcel setowner", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					UUID uuid = scape.<OfflinePlayer>get("owner").getUniqueId();
					Parcel parcel = scape.getParcel();
					
					Validate.isTrue(scape.getParcel().setOwner(uuid), "That player already owns this parcel");
					
					if (parcel.getAdded().get(uuid) != null) {
						parcel.getAdded().remove(uuid);
					}
									
					return scape.<OfflinePlayer>get("owner").getName() + " now owns this parcel";
				}){{
			setDescription("sets the owner of this parcel");
			setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
			setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel dispose", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					scape.getParcel().dispose();
					return "This parcel no longer has any data";
				}){{
			setDescription("removes any data about this parcel");
			setHelpInformation("removes any data about this parcel, it will be", "unowned, and noone will be allowed or banned.",
					"This command will not clear this parcel");
		}});
		
		CommandManager.register(new ParcelCommand("parcel reset", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
					Messaging.send(sender, "Parcels", Formatting.BLUE, "Resetting this parcel, hang tight...");
					scape.getWorld().reset(scape.getParcel());
					return "This parcel was reset successfully";
				}){{
			setDescription("clears and disposes this parcel");
			setHelpInformation("Clears and disposes this parcel,", "see /parcel clear and /parcel dispose");
		}});
		
		CommandManager.register(new ParcelCommand("parcel tp", ParcelRequirement.IN_WORLD, 
				(sender, scape) -> {
					ParcelWorld w = scape.getWorld();
					Coord xz = scape.get("parcel");
					Parcel p = Validate.returnIfPresent(w.getParcelAtID(xz.getX(), xz.getZ()), "That ID is not within this world's boundaries");
					Player target = scape.<Player>getOptional("target").orElse(sender);
					w.teleport(target, p);
					String format = "%s teleported %s to the " + p.toString();
					if (target == sender)
						return String.format(format, "You", "yourself");
					Messaging.send(target, PREFIX, Messaging.SUCCESS, String.format(format, sender.getName(), "you"));
					return String.format(format, "you", target.getName());
				}){{
			setDescription("teleports to a parcel");
			setHelpInformation("Teleports you or a target player", "to the parcel you specify by ID");
			setAliases("teleport");
			setParameters(new Parameter<Coord>("parcel", PARCEL_TYPE, "the parcel to teleport to"),
					new Parameter<Player>("target", ParameterType.PLAYER, "the player to teleport", false));
		}});
		
		CommandManager.register(new ParcelCommand("parcel clear", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					clearQueue.put(sender, new DuoObject<Parcel, Long>(scape.getParcel(), System.currentTimeMillis()));
					return "Are you sure you want to clear your parcel? Use /pconfirm";
				}){{
			setDescription("clears this parcel");
			setHelpInformation("Clears this parcel, resetting all of its blocks", "and removing all entities inside");
		}});
		
		ParcelsPlugin.getInstance().getServer().getPluginManager().registerEvents(new Listener() {
			
			@EventHandler
			public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
				
				String msg = event.getMessage();
				if (msg.startsWith("/"))
					msg = msg.substring(1);
				String[] split = msg.split(" ");
				if (split.length > 0 && split[0].equalsIgnoreCase("pconfirm")) {
					
					Player sender = event.getPlayer();
					DuoObject<Parcel, Long> entry = clearQueue.get(sender);
					if (entry != null) {
						clearQueue.remove(sender);
						
						Parcel parcel = entry.v1();
						ParcelWorld world = WorldManager.getWorld(parcel.getWorld().getName()).orElse(null);
						long currentTime = System.currentTimeMillis();
						String message;
						
						if (currentTime - entry.v2() > clearRequestExpireTime) {
							message = "Something weird happened";
						} else if (world == null) {
							message = "Your parcel clear request has expired";
						} else {
							Messaging.send(sender, "Parcels", Formatting.BLUE, "Clearing this parcel, hang tight...");
							world.clearBlocks(parcel);
							world.removeEntities(parcel);
							message = String.format("Cleared this parcel successfully, %.2fs elapsed", (System.currentTimeMillis() - currentTime) / 1000D);					
						}
						Messaging.send(sender, PREFIX, Formatting.GREEN, message);
						event.setCancelled(true);
					}
				}
			}
			
		}, ParcelsPlugin.getInstance());
		
		CommandManager.register(new ParcelCommand("parcel swap", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					Coord coord = scape.get("parcel");
					Parcel parcel2 = Validate.returnIfPresent(scape.getWorld().getParcelAtID(coord.getX(), coord.getZ()), "The target parcel does not exist");
					Messaging.send(sender, "Parcels", Formatting.BLUE, "Swapping these parcels, hang tight...");
					long time = System.currentTimeMillis();
					scape.getWorld().swap(scape.getParcel(), parcel2);
					return String.format("Swapped these parcels successfully, %.2fs elapsed", (System.currentTimeMillis() - time) / 1000D);
				}){{
			setDescription("swaps this parcel and its blocks with another");
			setHelpInformation("Swaps this parcel's data and any other contents,", "such as blocks and entities, with the target parcel");
			setParameters(new Parameter<Coord>("parcel", PARCEL_TYPE, "the parcel to swap with"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel random", ParcelRequirement.IN_WORLD, 
				(sender, scape) -> {
					Stream<Parcel> ownedParcels = scape.getWorld().getParcels().stream();
					ownedParcels.filter(p -> p.getOwner().isPresent());
					ownedParcels.skip((long) (Math.random() * ownedParcels.count()));
					Parcel target = ownedParcels.findFirst().orElse(null);
					Validate.notNull(target, "There is no owned parcel to teleport to in this world");
					scape.getWorld().teleport(sender, target);
					return String.format("Teleported to the %s, it is owned by %s", target.toString(), UUIDUtil.getName(target.getOwner().get()));
				}){{
			setDescription("teleports you to a random parcel");
			setHelpInformation("Teleports you to a random parcel in your current world", "to check it out");		
		}});
		
		String allBiomes = String.join(", ", Arrays.stream(Biome.values()).map(biome -> biome.toString().toLowerCase().replaceAll("_", " ")).toArray(CharSequence[]::new));
		
		CommandManager.register(new ParcelCommand("parcel setbiome", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					List<String> args = scape.get("biome");
					String biomeName = String.join("_", args.toArray(new CharSequence[args.size()])).toUpperCase();
					Biome biome;
					try {
						biome = Biome.valueOf(biomeName);
					} catch (IllegalArgumentException e) {
						throw new CommandException("That biome could not be found. All biomes: " + allBiomes);
					}
					scape.getWorld().setBiome(scape.getParcel(), biome);
					return "Set the biome to " + biomeName.toLowerCase().replaceAll("_", " ");
				}){{
			setDescription("changes the biome of this parcel");
			setHelpInformation("Changes the biome of this parcel to the requested one,");
			setParameters(true, new Parameter<String>("biome", ParameterType.STRING, "the biome to set"));
		}});
		
		/* Template
		CommandManager.register(new ParcelCommand("parcel ", ParcelRequirement.NONE, 
				(sender, scape) -> {
					return null;
				}){{
			
		}});
		*/		
	}
	
	private ParcelCommands() {}
}
