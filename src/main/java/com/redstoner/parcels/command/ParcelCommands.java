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

@SuppressWarnings("unused")
public class ParcelCommands {
	
	public static void register(WorldManager manager) {
		
		ParcelsPlugin.debug("Registering parcel commands");
		
		CommandManager.register(new LambdaCommand("parcel", (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP") {{
			setPermission("parcels.command");
			setDescription("manages your parcels");
			setAliases("plot", "p");
			setOnSyntaxRequest(CommandAction.CONTINUE);
			setHelpInformation("The command for anything parcel-related");
		}});
		
		CommandManager.register(new ParcelCommand("parcel info", (sender, scape) -> {
			return scape.getParcelAt().getInfo();
		}){{
			setAliases("i");
		}});
		
		CommandManager.register(new ParcelCommand("parcel setowner", true, (sender, scape) -> {
			OfflinePlayer owner = scape.get("owner");
			Validate.isTrue(scape.getParcelAt().setOwner(owner, sender), "You're not allowed to manage this plot");
			manager.getWorld(sender.getWorld().getName()).get().getParcels().print();
			return "Set this plot's owner on your request";
		}, (sender, scape) -> {
			return scape.proposals();
		}){{
			setDescription("sets the owner of this parcel");
			setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
			setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
		}});
		
		CommandManager.register(new ParcelCommand("parcel add", true, (sender, scape) -> {
			Validate.isTrue(scape.getParcelAt().addFriend(scape.get("friend")), "That person is already a friend on this parcel");
			return "Added friend to this parcel on your request";
		}, (sender, scape) -> {
			return scape.proposals();
		}){{
			setAliases("+");
			setDescription("adds a friend");
			setHelpInformation("Adds a friend to this parcel,", "who will be able to build on it");
			setParameters(new Parameter<OfflinePlayer>("friend", ParameterType.OFFLINE_PLAYER, "the friend to add"));
		}});
		
		/*
		CommandManager.register(new ParcelCommand("parcel ", (sender, scape) -> {
			return null;
		}){{
			
		}});
		*/
	}
	
	private ParcelCommands() {}
}
