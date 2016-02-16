package com.redstoner.command;

import org.bukkit.command.CommandSender;

public enum CommandAction {
	
	CONTINUE {

		@Override
		String execute(Command handler, CommandSender sender, String[] args) {
			String message = handler.invokeExecutor(sender, args);
			if (message != null)
				for (CommandAction action : values())
					if (action != this && message.equals(action.messageId()))
						return action.execute(handler, sender, args);
			return message;
		}
		
	},
	
	DISPLAY_ERROR {

		@Override
		String execute(Command handler, CommandSender sender, String[] args) {
			return "Invalid syntax, please try again.";
		}
		
	},
	
	DISPLAY_SYNTAX {

		@Override
		String execute(Command handler, CommandSender sender, String[] args) {
			return "Syntax: " + handler.getSyntaxMessage();
		}
		
	},
	
	DISPLAY_HELP {

		@Override
		String execute(Command handler, CommandSender sender, String[] args) {
			return handler.getHelpMessage(sender);
		}
		
	};
	
	abstract String execute(Command handler, CommandSender sender, String[] args);
	
	public String toString(Command handler, CommandSender sender, String[] args) {
		if (CONTINUE != this)
		return execute(handler, sender, args);
		throw new UnsupportedOperationException();
	}
	
	private String messageId() {
		return "EXEC:CommandAction." + name();
	};

}
