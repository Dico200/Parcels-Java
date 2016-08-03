package com.redstoner.parcels;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.redstoner.parcels.api.WorldManager;
import com.redstoner.parcels.api.storage.StorageManager;
import com.redstoner.parcels.command.ParcelCommands;

public class ParcelsPlugin extends JavaPlugin {
	
	private static ParcelsPlugin plugin = null;
	private static boolean debugging = true;
	
	public static void log(String s) {
		if (plugin != null)
			plugin.getLogger().info(s);
		else
			System.out.println("[Parcels]: " + s);
	}
	
	public static void debug(String s) {
		if (debugging) {
			if (plugin != null)
				plugin.getLogger().info("[DEBUG] " + s);
			else
				System.out.println("[Parcels]: [DEBUG]" + s);
		}
	}
	
	public static ParcelsPlugin getInstance() {
		return plugin;
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
		return WorldManager.getWorld(world).map(w -> w.getGenerator()).orElse(null);
	}
	
	public ParcelsPlugin() {
		plugin = this;
	}
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		
		WorldManager.loadSettingsFromConfig();
		StorageManager.initialise();
		ParcelCommands.register();
		ParcelListener.register();
		
		Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEdit != null) {
			WorldEditListener.register(worldEdit);
		}
		
	}
	
}
