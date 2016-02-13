package com.redstoner.parcels;

import java.util.HashMap;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.DuoObject.Entry;
import com.redstoner.utils.Maps;

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
	
	public ParcelsPlugin() {
		assert plugin == null;
		plugin = this;
		
		boolean x = true;
		assert x: "Message: x is true";
		
		getConfig().addDefaults(Maps.putAll(new HashMap<>(), new Entry<>("worlds", Maps.putAll(
				new HashMap<>(), new Entry<>("example", WorldManager.DEFAULT_WORLD_SETTINGS)))));
		
		ParcelsPlugin.plugin.getConfig().getValues(true).forEach((key, value) -> {
			ParcelsPlugin.debug(String.format("CONFIG: '%s' = %s", key, value));
		});
		
		worldManager = new WorldManager(this);
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
		return worldManager.getGenerator(world);
	}
	
	@Override
	public void onEnable() {

	}
	
	private WorldManager worldManager;
	
	public WorldManager getWorldManager() {
		return worldManager;
	}
	
}
