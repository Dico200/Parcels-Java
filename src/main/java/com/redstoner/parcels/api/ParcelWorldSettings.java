package com.redstoner.parcels.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.BlockType;
import com.redstoner.utils.Maps.CastingMap;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.Optional;
import com.redstoner.utils.Values;

public class ParcelWorldSettings {
	
	public static final CastingMap<String, Object> DEFAULT_WORLD_SETTINGS = new CastingMap<String, Object>() {

		private static final long serialVersionUID = 1L;
		
		{
			put("generator.wall-type", BlockType.fromString("44"));
			put("generator.floor-type", BlockType.fromString("155"));
			put("generator.fill-type", BlockType.fromString("1"));
			put("generator.path-main-type", BlockType.fromString("24"));
			put("generator.path-edge-type", BlockType.fromString("152"));
			put("generator.parcel-size", 101);
			put("generator.path-size", 8);
			put("generator.floor-height", 63);
			put("generator.offset-x", 0);
			put("generator.offset-z", 0);
			put("parcel-axis-limit", 10);
			put("static-time-day", true);
			put("static-weather-clear", true);
			put("interaction.disable-explosions", true);
			put("interaction.block-portal-creation", true);
			put("interaction.block-mob-spawning", true);
			put("interaction.items-blocked", new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
				{
					add("FLINT_AND_STEEL");
				}
			});
		}
		
	};
	
	public final BlockType wall, floor, fill, pathMain, pathEdge;
	public final int parcelSize, floorHeight, xOffset, zOffset, sectionSize, pathOffset;
	public final int axisLimit;
	public final boolean staticTimeDay, staticWeatherClear, disableExplosions, blockPortalCreation, blockMobSpawning;
	public final List<Material> itemsBlocked;
	
	public ParcelWorldSettings(BlockType wall, BlockType floor, BlockType fill, BlockType pathMain, BlockType pathEdge, 
			int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ, int axisLimit, boolean disableExplosions, 
			boolean staticTimeDay, boolean staticWeatherClear, boolean blockPortalCreation, boolean blockMobSpawning, 
			List<Material> itemsBlocked) {	
		
		this.axisLimit = axisLimit;
		this.staticTimeDay = staticTimeDay;
		this.staticWeatherClear = staticWeatherClear;
		
		// GENERATOR
		this.wall = wall;
		this.floor = floor;
		this.fill = fill;
		this.pathMain = pathMain;
		this.pathEdge = pathEdge;
		this.parcelSize = parcelSize;
		this.floorHeight = floorHeight;
		this.xOffset = offsetX;
		this.zOffset = offsetZ;
		this.sectionSize = parcelSize + pathSize;
		this.pathOffset = ((pathSize % 2 == 0)? pathSize + 2 : pathSize + 1) / 2;
	
		// INTERACTION
		this.disableExplosions = disableExplosions;
		this.blockPortalCreation = blockPortalCreation;
		this.blockMobSpawning = blockMobSpawning;
		this.itemsBlocked = itemsBlocked;
	}
	
	public ParcelWorldSettings(CastingMap<String, Object> settings) {
		this(
			settings.getCasted("generator.wall-type"), 
			settings.getCasted("generator.floor-type"), 
			settings.getCasted("generator.fill-type"), 
			settings.getCasted("generator.path-main-type"), 
			settings.getCasted("generator.path-edge-type"), 
			settings.getCasted("generator.parcel-size"), 
			settings.getCasted("generator.path-size"), 
			settings.getCasted("generator.floor-height"),
			settings.getCasted("generator.offset-x"),
			settings.getCasted("generator.offset-z"),
			settings.getCasted("parcel-axis-limit"),
			settings.getCasted("static-time-day"),
			settings.getCasted("static-weather-clear"),
			settings.getCasted("interaction.disable-explosions"),
			settings.getCasted("interaction.block-portal-creation"),
			settings.getCasted("interaction.block-mob-spawning"),
			settings.getCasted("interaction.items-blocked")
		);
		 
	}
	
	@SuppressWarnings("unchecked")
	public static Optional<ParcelWorldSettings> parseSettings(ConfigurationSection worlds, String world, MultiRunner errorPrinter) {
		BiConsumer<String, String> errorAdder = (key, worldname) -> {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Option '%s' for world '%s' could not be parsed. Aborting generator.", key, worldname)));
		};
		
		if (worlds.isConfigurationSection(world)) {
			Map<String, Object> input = worlds.getConfigurationSection(world).getValues(true);
			Values.validate(input != null, "getValues() (input) null");
			CastingMap<String, Object> settings = new CastingMap<>();
			
			for (Entry<String, Object> entry : DEFAULT_WORLD_SETTINGS.entrySet()) {
				
				String key = entry.getKey();
				ParcelsPlugin.debug("Default key: " + key);
				if (!input.containsKey(key)) {
					errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Option '%s' is missing from your settings. Aborting generator.", key)));
					continue;
				}
				
				Object value = null;
				Object inputValue = input.get(key);
				if (inputValue instanceof String) {
					ParcelsPlugin.debug("Found string, parsing BlockType");
					try {
						value = BlockType.fromString((String) inputValue);
					} catch (NumberFormatException e) {
						errorAdder.accept(key, world);
					}
				} else if (key.equals("interaction.items-blocked")) {
					try {
						value = ((List<String>) inputValue).stream().map(v -> Material.getMaterial(v)).collect(Collectors.toList());
					} catch (ClassCastException e) {
						errorAdder.accept(key, world);
					}
				} else {
					ParcelsPlugin.debug("Value directly assigned");
					value = inputValue;
				}
				
				settings.put(key, value);
			}
			
			if (!errorPrinter.willRun()) {
				return Optional.of(new ParcelWorldSettings(settings));
			}
			
			input.keySet().stream().filter(key -> !DEFAULT_WORLD_SETTINGS.containsKey(key)).forEach(key -> {
				errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Just FYI: Key '%s' isn't an option (Ignoring).", key)));
			});	
			
		} else {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  A world must be configured as a ConfigurationSection (a map).")));
		}
		if (errorPrinter.willRun()) {
			errorPrinter.addFirst(() -> ParcelsPlugin.log(String.format("Exception(s) occurred while loading settings for world '%s':", world)));
		}
		errorPrinter.runAll();
		errorPrinter.reset();
		return Optional.empty();
	}
}
