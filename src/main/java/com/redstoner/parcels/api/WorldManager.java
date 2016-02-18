package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.Values;
import com.redstoner.utils.Optional;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

public enum WorldManager {
	
	INSTANCE;
	
	
	public static HashMap<String, ParcelWorld> getWorlds() {
		return INSTANCE.worlds;
	}
	
	public static Optional<ParcelWorld> getWorld(World w) {
		return INSTANCE.getWorld(w.getName());
	}
	
	public static Optional<ParcelWorld> getWorld(Block b) {
		return getWorld(b.getWorld());
	}
	
	public static Optional<Parcel> getParcel(Block b) {
		return getWorld(b.getWorld()).flatMap(w -> w.getParcelAt(b.getX(), b.getZ()));
	}
	
	public static Optional<Parcel> getParcel(Location loc) {
		return getParcel(loc.getWorld().getBlockAt(loc));
	}
	
	public static void ifWorldPresent(Block b, BiConsumer<ParcelWorld, Optional<Parcel>> present) {
		getWorld(b).ifPresent(w -> present.accept(w, w.getParcelAt(b.getX(), b.getZ())));
	}
	
	public static void ifWorldPresent(Location loc, BiConsumer<ParcelWorld, Optional<Parcel>> present) {
		ifWorldPresent(loc.getWorld().getBlockAt(loc), present);
	}
	
	WorldManager() {
		this.plugin = ParcelsPlugin.getInstance();
		ParcelsPlugin.log("WorldManager initialized");
		loadSettingsFromConfig();
	}
	
	public Optional<Parcel> getParcelAt(Location loc) {
		return get(loc.getWorld().getName(), w -> w.getParcelAt(loc.getBlockX(), loc.getBlockZ()));
	}
	
	public ParcelWorldSettings getSettings(String world) {
		return get(world, w -> w.getSettings());
	}
	
	public ParcelGenerator getGenerator(String world) {
		return get(world, w -> w.getGenerator());
	}
	
	private <T> T get(String world, Function<ParcelWorld, T> function) {
		Optional<ParcelWorld> w = getWorld(world);
		return w.isPresent()? function.apply(w.get()) : null;
	}
	
	@SuppressWarnings("unused")
	private boolean exec(String world, Consumer<ParcelWorld> function) {
		Optional<ParcelWorld> w = getWorld(world);
		w.ifPresent(function::accept);
		return w.isPresent();
	}
	
	private ParcelsPlugin plugin;
	private HashMap<String, ParcelWorld> worlds = new HashMap<>();

	
	public Optional<ParcelWorld> getWorld(String world) {
		return Optional.ofNullable(this.worlds.get(world));
	}
	
	private void loadSettingsFromConfig() {
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
				this.worlds.put(world, new ParcelWorld(world, pws));
			});
		});
	}
}
