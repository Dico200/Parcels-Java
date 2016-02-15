package com.redstoner.parcels.command;

import com.redstoner.utils.Optional;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import com.redstoner.command.CommandAction;
import com.redstoner.command.CommandManager;
import com.redstoner.command.LambdaCommand;
import com.redstoner.command.Parameter;
import com.redstoner.command.ParameterType;
import com.redstoner.command.SenderType;
import com.redstoner.command.Validate;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;

@SuppressWarnings("unused")
public class ParcelCommands {
	
	public static void register() {
		
		ParcelsPlugin.debug("Registering parcel commands");
		
		CommandManager.register(new LambdaCommand("parcel", (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP") {{
			setPermission("parcels.command");
			setDescription("manages your parcels");
			setAliases("plot", "p");
			setOnSyntaxRequest(CommandAction.CONTINUE);
			setHelpInformation("The command for anything parcel-related");
		}});
		
		CommandManager.register(new ParcelCommand("parcel info", ParcelRequirement.IN_PARCEL, 
				(sender, scape) -> {
			return Validate.returnIfPresent(scape.getMaybeParcel().map(Parcel::getInfo), "You're not on a parcel");
		}){{
			setAliases("i");
		}});
		
		CommandManager.register(new ParcelCommand("parcel setowner", ParcelRequirement.IS_ADMIN, 
				(sender, scape) -> {
			Validate.isTrue(scape.getParcel().setOwner(scape.get("owner")), "That player already owns this parcel");
			return "Set this plot's owner on your request";
		}){{
			setDescription("sets the owner of this parcel");
			setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
			setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel add", ParcelRequirement.IN_OWNED,
				(sender, scape) -> {
			Validate.isTrue(scape.getParcel().getFriends().add(scape.get("friend")), "That person is already a friend on this parcel");
			return "Added friend to this parcel on your request";
		}){{
			setDescription("adds a friend");
			setHelpInformation("Adds a friend to this parcel,", "who will be able to build on it");
			setParameters(new Parameter<OfflinePlayer>("friend", ParameterType.OFFLINE_PLAYER, "the friend to add"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel remove", ParcelRequirement.IN_OWNED, 
				(sender, scape) -> {
			Validate.isTrue(scape.getParcel().getFriends().remove(scape.get("friend")), "That person was not a friend on this parcel");
			return "Removed friend from this parcel on your request";
		}){{
			setDescription("removes a friend");
			setHelpInformation("Removes a friend from this parcel,", "they won't be able to build anymore");
			setParameters(new Parameter<OfflinePlayer>("friend", ParameterType.OFFLINE_PLAYER, "the friend to remove"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel auto", ParcelRequirement.IN_WORLD,
				(sender, scape) -> {
			ParcelWorld w = scape.getWorld();
			Validate.isTrue(w.getOwned(sender).length < getPlotLimit(sender), "You have enough plots for now");
			Optional<Parcel> p = scape.getWorld().getNextUnclaimed();
			Validate.isTrue(p.isPresent(), "This world is full, please ask an admin to upsize it");
			p.get().setOwner(sender);
			w.teleport(sender, p.get());
			return "Enjoy your new plot!";
		}){{
			
		}});
		
		CommandManager.register(new ParcelCommand("parcel home", ParcelRequirement.IN_WORLD, 
				(sender, scape) -> {
			int number = scape.<Integer>getOptional("number").orElse(0);
			Parcel[] owned = scape.getWorld().getOwned(sender);
			Validate.isTrue(number < owned.length, "That parcel id does not exist, you have " + owned.length + " parcels");
			scape.getWorld().teleport(sender, owned[number]);
			return null;
		}){{
			setParameters(new Parameter<Integer>("id", ParameterType.INTEGER, "the id of your parcel", false));
		}});
		
		/*
		CommandManager.register(new ParcelCommand("parcel ", (sender, scape) -> {
			return null;
		}){{
			
		}});
		*/
	}
	
	private static int getPlotLimit(Player user) {
		if (user.hasPermission("parcels.limit.*"))
			return -1;
		for (int i = 0; i < 256; i++)
			if (user.hasPermission("parcels.limit." + i))
				return i;
		return 0;
	}
	
	private ParcelCommands() {}
}
