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

public class ParcelCommand extends LambdaCommand {
	
	private static final WorldManager MANAGER = ParcelsPlugin.getInstance().getWorldManager();

	public ParcelCommand(String command,
			BiFunction<Player, ParcelScape, String> executor,
			BiFunction<Player, ParcelScape, List<String>> tabCompleter) {
		super(command, (sender, scape) -> {
				Player user = (Player) sender;
				Parcel parcel = Validate.returnIfPresent(MANAGER.getParcelAt(user.getLocation()), "You're not on a parcel");
				return executor.apply(user, new ParcelScape(scape, parcel));}, 
			(sender, scape) -> {
				Player user = (Player) sender;
				Parcel parcel = Validate.returnIfPresent(MANAGER.getParcelAt(user.getLocation()), "You're not on a parcel");
				return tabCompleter.apply(user, new ParcelScape(scape, parcel));}
		);
		Bool.validate(executor != null && tabCompleter != null, "tabCompleter and executor may not be null");
		setSenderType(SenderType.PLAYER);
	}
	
	public ParcelCommand(String command, BiFunction<Player, ParcelScape, String> executor) {
		this(command, executor, (sender, scape) -> new ArrayList<String>());
	}

}
