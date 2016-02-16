package com.redstoner.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Messaging {

	public static final char SUCCESS = 'a';
	public static final char EXCEPT = 'e';
	
	private static final String PREFIX_FORMAT = applyColours("&4[&c%s&4] ");
	private static final String MESSAGE_FORMAT = "%s%s%s";
	
	private static String formatPrefix(String prefix) {
		return (prefix == null || prefix.isEmpty())? "" : String.format(PREFIX_FORMAT, prefix);
	}
	
	private static String applyColours(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	private static String toColour(char color) {
		return applyColours("&" + color);
	}
	
	public static void send(CommandSender sender, String prefix, char color, String message) {
		sender.sendMessage(String.format(MESSAGE_FORMAT, formatPrefix(prefix), toColour(color), applyColours(message)));
	}
	
	public static void send(CommandSender recipient, char color, String message) {
		send(recipient, null, color, message);
	}
	
	public static class CommandWriter {
		
		private static final String HELPMESSAGE_ORDER = "$HEADER$$HELP_INFORMATION$$ALIASES$$SUBCOMMAND_HEADER$$SYNTAX$$SUBCOMMANDS$";
		
		private static final String HEADER_FORMAT 				= applyColours("&9Command &3/%s");
		private static final String HELP_INFORMATION_FORMAT 	= applyColours("\n &9%s");
		private static final String ALIASES_FORMAT 				= applyColours("\n&9Aliases: &3%s");
		private static final String SUBCOMMAND_HEADER_FORMAT 	= applyColours("\n&3/%s...\n");
		private static final String SYNTAX_FORMAT 				= applyColours("\n &a%s");
		private static final String SUBCOMMAND_FORMAT 			= applyColours("\n &3%s&9: %s");
		
		private static final String SYNTAXMESSAGE_FORMAT 		= applyColours("&3%s %s");
		
		private final String halfFinishedHelpMessage;
		
		private final String command, syntax, syntaxMessage;
		private Command handler;
		
		public CommandWriter(Command handler, String command, String[] helpInformation, List<String> aliases, String syntax) {
			String message = HELPMESSAGE_ORDER;
			
			message = message.replace("$HEADER$", String.format(HEADER_FORMAT, command));
			
			message = message.replace("$HELP_INFORMATION$", String.join("", (CharSequence[])Arrays.stream(helpInformation)
					.map(s -> String.format(HELP_INFORMATION_FORMAT, s)).toArray(size -> new String[size])));
			
			message = message.replace("$ALIASES$", (aliases.size() > 0)? 
					String.format(ALIASES_FORMAT, String.join(applyColours("&9, &3"), aliases)): "");
			
			this.halfFinishedHelpMessage = message;
			this.command = command;
			this.syntax = syntax;
			this.handler = handler;
			this.syntaxMessage = String.format(SYNTAXMESSAGE_FORMAT, command, syntax);
		}
		
		public String parseHelpMessage(CommandSender sender) {
			String result = halfFinishedHelpMessage;
			
			List<Command> children = handler.getChildren();
			
			boolean syntaxPresent = !syntax.isEmpty();
			boolean subsPresent = children.size() > 0;

			result = result.replace("$SUBCOMMAND_HEADER$", (syntaxPresent || subsPresent)? 
					String.format(SUBCOMMAND_HEADER_FORMAT, command) : "");
			
			result = result.replace("$SYNTAX$", syntaxPresent? String.format(SYNTAX_FORMAT, syntax) : "");
			
			int layer = handler.getLayer();
			
			String[] subcommands = children.stream().filter(cmd -> cmd.accepts(sender))
					.map(cmd -> String.format(SUBCOMMAND_FORMAT, cmd.collectPath(layer), cmd.getDescription()))
					.toArray(size -> new String[size]);
			
			result = result.replace("$SUBCOMMANDS$", String.join("\n", subcommands));
			
			return result;
		}
		
		public String getSyntaxMessage() {
			return syntaxMessage;
		}
	}
}

