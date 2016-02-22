package com.redstoner.parcels;

import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.redstoner.parcels.api.ParcelWorldSettings;
import com.redstoner.parcels.api.StorageManager;
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
	
	public ParcelsPlugin() {
		plugin = this;
		
		getConfig().set("worlds.Parcels.items-blocked", ParcelWorldSettings.DEFAULT_WORLD_SETTINGS.get("items-blocked"));
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		worldManager = WorldManager.INSTANCE;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {

		this.newUseMySQL = true;
		
		StorageManager.initialise();
		ParcelCommands.register();
		ParcelListener.register();
		
		if (!StorageManager.useMySQL) {
			int interval = getConfig().getInt("MySQL.save-interval-no-mysql");
			if (interval < 10)
				interval = 30;
			Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {
				StorageManager.save();
			}, 20 * interval, 20 * interval);
		}
	}
	
	public boolean newUseMySQL;
	
	@Override
	public void onDisable() {
		StorageManager.save(newUseMySQL);
	}
	
	private WorldManager worldManager;
	
	public WorldManager getWorldManager() {
		return worldManager;
	}
	
}
