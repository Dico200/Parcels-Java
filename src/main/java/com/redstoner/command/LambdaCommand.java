package com.redstoner.command;

import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.command.CommandSender;

import com.redstoner.utils.Values;

public class LambdaCommand extends Command {
	
	private BiFunction<CommandSender, CommandScape, String> executor;
	private BiFunction<CommandSender, CommandScape, List<String>> tabCompleter;
	
	public LambdaCommand(String command, BiFunction<CommandSender, CommandScape, String> executor) {
		this(command, executor, (sender, scape) -> scape.proposals());
	}
	
	public LambdaCommand(String command, BiFunction<CommandSender, CommandScape, String> executor, 
			BiFunction<CommandSender, CommandScape, List<String>> tabCompleter) {
		super(command);
		Values.validate(executor != null, "executor may not be null");
		Values.validate(tabCompleter != null, "tabCompleter may not be null");
		this.executor = executor;
		this.tabCompleter = tabCompleter;
	}
	
	@Override
	protected final String execute(CommandSender sender, CommandScape scape) {
		return executor.apply(sender, scape);
	}

	@Override
	protected final List<String> tabComplete(CommandSender sender, CommandScape scape) {
		return tabCompleter.apply(sender, scape);
	}

}
