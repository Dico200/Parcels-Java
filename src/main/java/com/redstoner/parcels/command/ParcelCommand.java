package com.redstoner.parcels.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.redstoner.command.CommandException;
import com.redstoner.command.LambdaCommand;
import com.redstoner.command.SenderType;
import com.redstoner.utils.Values;

import org.bukkit.entity.Player;

public class ParcelCommand extends LambdaCommand {

	public ParcelCommand(String command, ParcelRequirement requirement,
			BiFunction<Player, ParcelScape, String> executor,
			BiFunction<Player, ParcelScape, List<String>> tabCompleter) {
		super(command, (sender, scape) -> {
				Player user = (Player) sender;
				return executor.apply(user, new ParcelScape(user, scape, requirement));
			}, (sender, scape) -> {
				Player user = (Player) sender;
				try {
					return tabCompleter.apply(user, new ParcelScape(user, scape, requirement));
				} catch (CommandException e) {
					return new ArrayList<>();
				}
			}
		);
		Values.validate(executor != null && tabCompleter != null && requirement != null, "tabCompleter, executor and requirement may not be null");
		setSenderType(SenderType.PLAYER);
	}
	
	public ParcelCommand(String command, ParcelRequirement requirement,
			BiFunction<Player, ParcelScape, String> executor) {
		this(command, requirement, executor, (sender, scape) -> scape.proposals());
	}

}
