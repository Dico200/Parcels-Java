package com.redstoner.command;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

public class CommandManager {
	
	private static final CommandMap COMMAND_MAP;
	private static final Command ROOT;
	
	static Command getROOT() {
		return ROOT;
	}
	
	public static void register(Command handler) {
		ROOT.addChild(handler);
	}
	
	private static void dispatchToMap(Command command) {
		assert COMMAND_MAP != null : new AssertionError("Command Map wasn't retrieved, unable to register commands!");
		assert command != null : new AssertionError("Dispatched command is null!");
		Bukkit.getLogger().info("Registering command " + command.getId() + " to command map.");
		
		InputHandler handler = new InputHandler(command);
		String id = command.getId();

		handler.setOther(COMMAND_MAP.getCommand(id));
		COMMAND_MAP.register(id, handler);
	}
	
	static {
		
		PluginManager pm = Bukkit.getPluginManager();
		CommandMap map;
		try {
			Field f = pm.getClass().getDeclaredField("commandMap");
			f.setAccessible(true);
			map = (CommandMap) f.get(pm);
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
			Bukkit.getLogger().severe("An error occured while retrieving the command map! See below.");
			e.printStackTrace();
			map = null;
		}
		
		COMMAND_MAP = map;
		
		ROOT = new Command() {

			@Override
			protected String execute(CommandSender sender, CommandScape scape) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			protected List<String> tabComplete(CommandSender sender, CommandScape scape) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			protected boolean addChild(String key, Hierarchy<Command> child) {
				if (super.addChild(key, child)) {
					Bukkit.getLogger().info("Dispatching, layer: " + child.getLayer());
					dispatchToMap(child.getInstance());
					return true;
				}
				return false;
			}
			
		};
		
	}
	
}
