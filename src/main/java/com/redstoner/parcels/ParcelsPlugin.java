package com.redstoner.parcels;

import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
		return WorldManager.getWorld(world).map(w -> w.getGenerator()).orElse(null);
	}
	
	public ParcelsPlugin() {
		plugin = this;
		
		getConfig().options().copyDefaults(true);
		saveConfig();
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {

		this.newUseMySQL = true;
		
		StorageManager.initialise();
		ParcelCommands.register();
		ParcelListener.register();
		
		Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEdit != null) {
			WorldEditListener.register(worldEdit);
		}
		
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
	
}
