package com.redstoner.parcels;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.redstoner.parcels.api.WorldManager;
import com.redstoner.parcels.command.ParcelCommands;

public class ParcelsPlugin extends JavaPlugin {
	
	private static ParcelsPlugin plugin = null;
	private static boolean debugging = true;
	
	public static void log(String s) {
		plugin.getLogger().info(s);
	}
	
	public static void debug(String s) {
		if (debugging) {
			plugin.getLogger().info("[DEBUG] " + s);
		}
	}
	
	public static ParcelsPlugin getInstance() {
		return plugin;
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
		return worldManager.getGenerator(world);
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		getConfig().getValues(true).forEach((key, value) -> {
			ParcelsPlugin.debug(String.format("CONFIG: '%s' = %s", key, value));
		});
		
		worldManager = new WorldManager(this);
		
		ParcelCommands.register(worldManager);
		getServer().getPluginManager().registerEvents(new ParcelListener(), this);
	}
	
	@Override
	public void onDisable() {
		saveConfig();
	}
	
	private WorldManager worldManager;
	
	public WorldManager getWorldManager() {
		return worldManager;
	}
	
}
