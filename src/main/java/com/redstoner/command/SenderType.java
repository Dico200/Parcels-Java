package com.redstoner.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum SenderType {
	
	PLAYER(Player.class, "That command can only be used by players"),
	CONSOLE(ConsoleCommandSender.class, "That command can only be used by the console"),
	EITHER(CommandSender.class, null);
	
	private Class<?> type;
	private String message;
	SenderType(Class<?> type, String message) {
		this.type = type;
		this.message = message;
	}
	
	protected boolean correct(CommandSender sender) {
		return type.isInstance(sender);
	}
	
	protected String getMessage() {
		return message;
	}
	
	protected void check(CommandSender sender) {
		Validate.isTrue(correct(sender), getMessage());
	}

}
