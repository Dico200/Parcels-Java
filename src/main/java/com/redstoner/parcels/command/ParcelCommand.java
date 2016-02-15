package com.redstoner.parcels.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.entity.Player;

import com.redstoner.command.LambdaCommand;
import com.redstoner.command.SenderType;
import com.redstoner.command.Validate;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.Bool;
import com.redstoner.utils.Optional;

public class ParcelCommand extends LambdaCommand {
	
	private static final WorldManager MANAGER = ParcelsPlugin.getInstance().getWorldManager();

	public ParcelCommand(String command, boolean requiresOwner,
			BiFunction<Player, ParcelScape, String> executor,
			BiFunction<Player, ParcelScape, List<String>> tabCompleter) {
		super(command, (sender, scape) -> {
				Player user = (Player) sender;
				Parcel parcel = Validate.returnIfPresent(MANAGER.getParcelAt(user.getLocation()), "You're not on a parcel");
				Validate.isTrue(user.hasPermission("parcels.command.setowner.any") 
						|| !requiresOwner || parcel.isOwner(user), "You must own this parcel to perform that command");
				return executor.apply(user, new ParcelScape(scape, parcel));}, 
			(sender, scape) -> {
				Player user = (Player) sender;
				Optional<Parcel> parcel = MANAGER.getParcelAt(user.getLocation());
				if (!parcel.isPresent() || (!user.hasPermission("parcels.command.setowner.any") && requiresOwner && !parcel.get().isOwner(user)))
					return new ArrayList<>();
				return tabCompleter.apply(user, new ParcelScape(scape, parcel.get()));}
		);
		Bool.validate(executor != null && tabCompleter != null, "tabCompleter and executor may not be null");
		setSenderType(SenderType.PLAYER);
	}
	
	public ParcelCommand(String command, boolean requiresOwner, BiFunction<Player, ParcelScape, String> executor) {
		this(command, requiresOwner, executor, (sender, scape) -> scape.proposals());
	}
	
	public ParcelCommand(String command, 
			BiFunction<Player, ParcelScape, String> executor,
			BiFunction<Player, ParcelScape, List<String>> tabCompleter) {
		this(command, false, executor, tabCompleter);
	}
	
	public ParcelCommand(String command, BiFunction<Player, ParcelScape, String> executor) {
		this(command, false, executor);
	}

}
