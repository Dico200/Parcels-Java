package com.redstoner.parcels.api;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.Optional;
import com.redstoner.utils.Values;

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
		MultiRunner printErrors = new MultiRunner(() -> {
			ParcelsPlugin.log("##########################################################");
		}, () -> {
			ParcelsPlugin.log("##########################################################");
		});
		
		ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
		Values.validate(worlds != null, "worlds section null");
		Values.validate(worlds.getKeys(false) != null, "getKeys() null");
		
		worlds.getKeys(false).forEach(world -> {
			ParcelWorldSettings.parseSettings(worlds, world, printErrors).ifPresent(pws -> {
				WorldManager.worlds.put(world, new ParcelWorld(world, pws));
			});
		});
	}
	
	static {
		plugin = ParcelsPlugin.getInstance();
		loadSettingsFromConfig();
	}
}
