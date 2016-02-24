package com.redstoner.parcels.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.command.CommandAction;
import com.redstoner.command.CommandException;
import com.redstoner.command.CommandManager;
import com.redstoner.command.LambdaCommand;
import com.redstoner.command.Messaging;
import com.redstoner.command.Parameter;
import com.redstoner.command.ParameterType;
import com.redstoner.command.Validate;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.Optional;

public class ParcelCommands {
	
	private static final String PREFIX = "Parcels";
	
	public static void register() {
		
		ParameterType<Coord> PARCEL_TYPE = new ParameterType<Coord>("Parcel", "the ID of a parcel") {

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
		
		CommandManager.register(PREFIX, new LambdaCommand("parcel", (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP") {{
			setPermission("parcels.command");
			setDescription("manages your parcels");
			setAliases("plot", "p");
			setOnSyntaxRequest(CommandAction.CONTINUE);
			setHelpInformation("The command for anything parcel-related");
		}});
		
		CommandManager.register(new ParcelCommand("parcel info", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					return Validate.returnIfPresent(scape.getMaybeParcel().map(Parcel::getInfo), "You have to be in a parcel world to use that command");
				}){{
			setDescription("displays information about this parcel");
			setHelpInformation("Displays general information", "about the parcel you're on");
			setAliases("i");
		}});
		
		CommandManager.register(new ParcelCommand("parcel setowner", ParcelRequirement.IS_ADMIN, 
				(sender, scape) -> {
					Validate.isTrue(scape.getParcel().setOwner(scape.<OfflinePlayer>get("owner").getUniqueId()), "That player already owns this parcel");
					return "Set this plot's owner on your request";
				}){{
			setDescription("sets the owner of this parcel");
			setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
			setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel allow", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
					OfflinePlayer allowed = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().add(allowed.getUniqueId(), true), "That player is already allowed to build on this parcel");
					return allowed.getName() + " is now allowed to build on this parcel";
				}){{
			setDescription("allows a player to build");
			setHelpInformation("Allows a player to build on this parcel");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to allow"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel forbid", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					OfflinePlayer forbidden = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().remove(forbidden.getUniqueId(), true), "That player wasn't allowed to build on this parcel");
					return forbidden.getName() + " is no longer allowed to build on this parcel";
				}){{
			setDescription("forbids a player to build");
			setHelpInformation("Forbids a player to build on this parcel,", "they won't be allowed to anymore");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to forbid"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel ban", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
					OfflinePlayer banned = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().add(banned.getUniqueId(), false), "That player is already banned from this parcel");
					return banned.getName() + " is now banned from this parcel";
				}){{
			setDescription("bans a player from this parcel");
			setHelpInformation("Bans a player from this parcel,", "making them unable to enter");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to ban"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel unban", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
					OfflinePlayer unbanned = scape.get("player");
					Validate.isTrue(scape.getParcel().getAdded().remove(unbanned.getUniqueId(), false), "That player wasn't banned from this parcel");
					return unbanned.getName() + " is no longer banned from this parcel";
				}){{
			setDescription("unbans a player from this parcel");
			setHelpInformation("Unbans a player from this parcel,", "they will be able to enter it again");
			setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to unban"));
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
					Messaging.send(sender, "Parcels", Formatting.BLUE, "Clearing this parcel, hang tight...");
					long time = System.currentTimeMillis();
					scape.getWorld().clearBlocks(scape.getParcel());
					scape.getWorld().removeEntities(scape.getParcel());
					return String.format("Cleared this parcel successfully, %.2fs elapsed", (System.currentTimeMillis() - time) / 1000.0);
				}){{
			setDescription("clears this parcel");
			setHelpInformation("Clears this parcel, resetting all of its blocks", "and removing all entities inside");
		}});
		
		CommandManager.register(new ParcelCommand("parcel swap", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
					ParcelWorld world = scape.getWorld();
					Parcel parcel1 = scape.getParcel();
					Coord xz = scape.get("parcel");
					Parcel parcel2 = Validate.returnIfPresent(world.getParcelAtID(xz.getX(), xz.getZ()), "The target parcel does not exist");
					Messaging.send(sender, "Parcels", Formatting.BLUE, "Swapping these parcels, hang tight...");
					long time = System.currentTimeMillis();
					world.swap(parcel1, parcel2);
					return String.format("Swapped these parcels successfully, %.2fs elapsed", (System.currentTimeMillis() - time) / 1000.0);
				}){{
			setDescription("swaps this parcel and its blocks with another");
			setHelpInformation("Swaps this parcel's data and any other contents,", "such as blocks and their data, with the target parcel");
			setParameters(new Parameter<Coord>("parcel", PARCEL_TYPE, "the parcel to swap with"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel option", ParcelRequirement.IN_PARCEL, (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP"){{
			setDescription("changes interaction options for this parcel");
			setHelpInformation("Sets whether players who are not allowed to", "build here can interact with certain things.");
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
		
		/*
		CommandManager.register(new ParcelCommand("parcel ", ParcelRequirement.NONE, 
				(sender, scape) -> {
					return null;
				}){{
			
		}});
		*/
		
		
		/*
		 * This one is for storage saving.
		 */
		
		/*
		CommandManager.register(new LambdaCommand("parcel setusemysql", (sender, scape) -> {
			boolean current = ParcelsPlugin.getInstance().newUseMySQL;
			boolean newvalue = scape.get(0);
			String enabled = newvalue? "enabled" : "disabled";
			Validate.isTrue(current != newvalue, "Usage of MySQL is already " + enabled);
			ParcelsPlugin.getInstance().newUseMySQL = newvalue;
			return "MySQL will be " + enabled + " on shutdown, and Parcels will be saved accordingly";
		}){{
			setParameters(new Parameter<Boolean>("enabled", ParameterType.BOOLEAN, "a boolean value"));
		}});
		*/
		
	}
	
	private ParcelCommands() {}
}
