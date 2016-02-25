package com.redstoner.parcels.api;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.Optional;

public class WorldManager {
	
	public static HashMap<String, ParcelWorld> getWorlds() {
		return worlds;
	}
	
	public static Optional<ParcelWorld> getWorld(String world) {
		return Optional.ofNullable(worlds.get(world));
	}
	
	public static Optional<ParcelWorld> getWorld(World w) {
		return getWorld(w.getName());
	}
	
	public static void ifWorldPresent(Block b, BiConsumer<ParcelWorld, Optional<Parcel>> present) {
		getWorld(b.getWorld()).ifPresent(w -> present.accept(w, w.getParcelAt(b.getX(), b.getZ())));
	}
	
	public static Optional<Parcel> getParcelAt(Location loc) {
		return get(loc.getWorld().getName(), w -> w.getParcelAt(loc.getBlockX(), loc.getBlockZ()));
	}

	
	private WorldManager() {
		throw new RuntimeException();
	}
	
	private static <T> T get(String world, Function<ParcelWorld, T> function) {
		Optional<ParcelWorld> w = getWorld(world);
		return w.isPresent()? function.apply(w.get()) : null;
	}
	
	private static ParcelsPlugin plugin;
	private static HashMap<String, ParcelWorld> worlds = new HashMap<>();
	
	private static void loadSettingsFromConfig() {
		
		ConfigurationSection worldsConfig = plugin.getConfig().getConfigurationSection("worlds");
		if (worldsConfig == null) {
			ParcelsPlugin.log("Failed to find your world's settings in config.");
			return;
		}
		
		worldsConfig.getKeys(false).forEach(worldName -> {
			ParcelWorldSettings.parseSettings(worldsConfig, worldName).ifPresent(pws -> {
				worlds.put(worldName, new ParcelWorld(worldName, pws));
			});
		});
	}
	
	static {
		try {
			plugin = ParcelsPlugin.getInstance();
			loadSettingsFromConfig();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
}
