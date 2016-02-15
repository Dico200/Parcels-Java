package com.redstoner.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class InputHandler extends org.bukkit.command.Command {
	
	private Command parent;
	private org.bukkit.command.Command other;
	private boolean takePriority;
	
	protected InputHandler(Command parent) {
		super(parent.getId(), parent.getDescription(), new String(), parent.getAliases());
		this.parent = parent;
		this.other = null;
		this.takePriority = false;
	}
	
	public void setOther(org.bukkit.command.Command other) {
		this.other = other;
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (!takePriority && other != null)
			return other.execute(sender, label, args);
		
		Command handler = parent.instanceAt(args, true);
		assert handler != null;
		args = Arrays.copyOfRange(args, handler.getLayer() - 1, args.length);
		
		String message;
		try {
			message = "&a" + handler.acceptCall(sender, args);
		} catch (CommandException e) {
			message = "&c" + e.getMessage();
		} catch (ArgumentException e) {
			Bukkit.getLogger().severe(String.format("Command '%s' threw ArgumentException: '%s'", parent.getId(), e.getMessage()));
			e.printStackTrace();
			return true;
		}
		
		if (!(message == null || message.isEmpty())) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		}
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		Command handler = parent.instanceAt(args, true);
		args = Arrays.copyOfRange(args, handler.getLayer(), args.length);
		return handler.acceptTabComplete(sender, args);
	}
}
