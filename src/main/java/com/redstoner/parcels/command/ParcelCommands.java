package com.redstoner.parcels.command;

import java.util.Optional;

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
		
		CommandManager.register(new LambdaCommand("parcel", (sender, scape) -> "EXEC:CommandAction.DISPLAY_HELP") {
			{
				setPermission("parcels.command.$COMMAND$");
				setDescription("manages your parcels");
				setAliases("plot", "p");
				setOnSyntaxRequest(CommandAction.CONTINUE);
				setHelpInformation("The command for anything parcel-related");
			}
		});
		
		CommandManager.register(new ParcelCommand("parcel info", (sender, scape) -> {
			return "info";
		}){
			{
				setAliases("i");
				setSenderType(SenderType.PLAYER);
			}
		});
		
		CommandManager.register(new ParcelCommand("parcel setowner", (sender, scape) -> {
			OfflinePlayer owner = scape.get("owner");
			Validate.isTrue(owner != null && (owner.hasPlayedBefore() || owner.isOnline()), "That player could not be found");
			Validate.isTrue(scape.getParcelAt().setOwner(owner, sender), "You're not allowed to manage this plot");
			return "Set this plot's owner on your request";
		}){
			{
				setDescription("sets the owner of this parcel");
				setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
				setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
				setSenderType(SenderType.PLAYER);
			}
		});
		
		/*
		CommandManager.register(new LambdaCommand("", (sender, scape) -> {
			return "";
		}){
			{
				
			}
		});
		
		CommandManager.register(new LambdaCommand("", (sender, scape) -> "") {
			{

			}
		});
		*/
	}
	
	private ParcelCommands() {}
}
