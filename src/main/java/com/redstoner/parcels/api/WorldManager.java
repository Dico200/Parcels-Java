package com.redstoner.parcels.api;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.Optional;

public final class WorldManager {
	
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
	
	public static boolean isInOtherWorldOrInParcel(Location loc, Predicate<Parcel> parcelTest) {
		Optional<ParcelWorld> world = getWorld(loc.getWorld());
		return !world.isPresent() || world.get().getParcelAt(loc.getBlockX(), loc.getBlockZ()).filter(parcelTest).isPresent();
	}
	
	public static boolean isInOtherWorldOrInParcel(Block b, Predicate<Parcel> parcelTest) {
		Optional<ParcelWorld> world = getWorld(b.getWorld());
		return !world.isPresent() || world.get().getParcelAt(b.getX(), b.getZ()).filter(parcelTest).isPresent();
	}
	
	private WorldManager() {}
	
	private static <T> T get(String world, Function<ParcelWorld, T> function) {
		Optional<ParcelWorld> w = getWorld(world);
		return w.isPresent()? function.apply(w.get()) : null;
	}
	
	//private static ParcelsPlugin plugin;
	private static HashMap<String, ParcelWorld> worlds = new HashMap<>();
	
	private static void loadSettingsFromConfig() {
		ParcelsPlugin plugin = ParcelsPlugin.getInstance();
		if (plugin == null) {
			// Running test
			return;
		}
		
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
		loadSettingsFromConfig();
	}
}
