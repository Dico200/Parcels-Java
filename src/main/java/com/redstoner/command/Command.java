package com.redstoner.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public abstract class Command extends Hierarchy<Command> {

	final String acceptCall(CommandSender sender, String[] args) {
		doSenderChecks(sender);
		
		CommandAction action;
		if (args.length == 0) {
			action = CommandAction.CONTINUE;
		} else if (args[0].toLowerCase().equals("help")) {
			action = onHelpRequest;
		} else if (args[0].toLowerCase().equals("syntax")) {
			action = onSyntaxRequest;
		} else {
			action = CommandAction.CONTINUE;
		}
		return action.execute(this, sender, args);
	}
	
	final String invokeExecutor(CommandSender sender, String[] args) {
		return execute(sender, params.toScape(args));
	}
	
	protected abstract String execute(CommandSender sender, CommandScape scape);
	
	final List<String> acceptTabComplete(CommandSender sender, String[] args) {
		return accepts(sender) ? tabComplete(sender, params.toScape(args, params.complete(args))) : new ArrayList<>();
	}
	
	protected List<String> tabComplete(CommandSender sender, CommandScape scape) {
		return scape.proposals();
	}
	
	private final void doSenderChecks(CommandSender sender) {
		senderType.check(sender);
		
		if (permission != null && !permission.isEmpty()) {
			Validate.isAuthorized(sender, permission);
		}
	}
	
	protected Command() {
		super(Command.class);
		this.permission = "";
		this.senderType = null;
		this.description = null;
		this.helpInformation = null;
		this.aliases = null;
		this.params = null;
		this.onHelpRequest = null;
		this.onSyntaxRequest = null;
	}
	
	public Command(String command) {
		super(command, Command.class);
		this.command = command.toLowerCase();
		setPermission("$PARENT$.$COMMAND$");
		setSenderType(SenderType.EITHER);
		setDescription("");
		setHelpInformation(new String[]{});
		setAliases(new String[]{});
		setParameters(new Parameter<?>[]{});
		setOnHelpRequest(CommandAction.DISPLAY_HELP);
		setOnSyntaxRequest(CommandAction.DISPLAY_SYNTAX);
	}
	
	String command;
	private String permission; 
	private SenderType senderType;
	private String description;
	private String[] helpInformation;
	private List<String> aliases;
	private Parameters params;
	private CommandAction onHelpRequest;
	private CommandAction onSyntaxRequest;
	private Messaging.CommandWriter messager;
	
	@Override
	public final List<String> getAliases() {
		return aliases;
	}
	
	final String getDescription() {
		return description;
	}
	
	final boolean accepts(CommandSender sender) {
		try {
			doSenderChecks(sender);
			return true;
		} catch (CommandException e) {
			return false;
		}
	}
	
	protected final String collectPath(int layerFrom) {
		String[] prev = Arrays.copyOfRange(getPath(), layerFrom, getLayer() - 1);
		return String.format("%s %s", String.join(" ", prev), getId());
	}
	
	protected final String getSyntaxMessage() {
		return messager.getSyntaxMessage();
	}
	
	protected final String getHelpMessage(CommandSender sender) {
		return messager.parseHelpMessage(sender);
	}
	
	/**
	 * @param permission The permission which the sender should be checked against.
	 * Notes:
	 * "$PARENT$" is replaced with the permission node of the parent command (you probably want a . behind it).
	 * "$COMMAND$" is replaced with the (sub)command.
	 * Use an empty string to disable permission checking.
	 * @default "$PARENT$.$COMMAND$"
	 */
	protected final void setPermission(String permission) {
		assert permission != null;
		this.permission = permission;
	}
	
	/**
	 * @param senderType The type of CommandSender that should be accepted:
	 * Player, Console, or Either.
	 * @default Either
	 */
	protected final void setSenderType(SenderType senderType) {
		this.senderType = senderType;
	}
	
	/**
	 * @param description A brief description of what the command does
	 * @default empty string
	 */
	protected final void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @param helpInformation Lines to print when help is requested. Comes down to a very detailed description.
	 * @default empty string[]
	 */
	protected final void setHelpInformation(String... lines) {
		this.helpInformation = lines;
	}
	
	/**
	 * The aliases for the command. Defaults to none
	 * @default empty List<String>
	 */
	protected final void setAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
	}
	
	protected final void setParameters(boolean allowOverflow, Parameter<?>... params) {
		this.params = new Parameters(params, allowOverflow);
	};
	
	protected final void setParameters(Parameter<?>... params) {
		setParameters(false, params);
	}
	
	protected final void setOnHelpRequest(CommandAction action) {
		this.onHelpRequest = action;
	}
	
	protected final void setOnSyntaxRequest(CommandAction action) {
		this.onSyntaxRequest = action;
	}
	
	@Override
	public final Command getParent() {
		Hierarchy<Command> parent = super.getParent();
		while (parent.isPlaceHolder()) {
			parent = parent.getParent();
		}
		return parent.getInstance();
	}
	
	@Override
	final void setParent(Hierarchy<Command> parent) {
		super.setParent(parent);
		Command inst = getParent();
		if (inst.permission.isEmpty()) {
			this.permission = permission.replace("$PARENT$.", "").replace(".$PARENT$", "");
		}
		this.permission = permission.replace("$PARENT$", inst.permission).replace("$COMMAND$", getId());
		Bukkit.getLogger().info(String.format("Permission for command '%s' = '%s'", command, permission));
		
		this.messager = new Messaging.CommandWriter(this, command, helpInformation, aliases, params.syntax());
	}	

}
