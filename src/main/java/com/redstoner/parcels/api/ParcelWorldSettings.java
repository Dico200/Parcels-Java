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
			put("wall-type", BlockType.fromString("44"));
			put("floor-type", BlockType.fromString("155"));
			put("fill-type", BlockType.fromString("1"));
			put("path-main-type", BlockType.fromString("24"));
			put("path-edge-type", BlockType.fromString("152"));
			put("parcel-size", 101);
			put("path-size", 8);
			put("floor-height", 63);
			put("offset-x", 0);
			put("offset-z", 0);
			put("parcel-axis-limit", 10);
			put("disable-explosions", true);
			put("items-blocked", new ArrayList<String>(){
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
	public final boolean disableExplosions;
	public final List<Material> itemsBlocked;
	
	public ParcelWorldSettings(BlockType wall, BlockType floor, BlockType fill, BlockType pathMain, BlockType pathEdge, 
			int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ, int axisLimit, boolean disableExplosions, 
			List<Material> itemsBlocked) {	
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
		
		this.axisLimit = axisLimit;
		
		this.disableExplosions = disableExplosions;
		
		this.itemsBlocked = itemsBlocked;
	}
	
	public ParcelWorldSettings(CastingMap<String, Object> settings) {
		this(
			settings.getCasted("wall-type"), 
			settings.getCasted("floor-type"), 
			settings.getCasted("fill-type"), 
			settings.getCasted("path-main-type"), 
			settings.getCasted("path-edge-type"), 
			settings.getCasted("parcel-size"), 
			settings.getCasted("path-size"), 
			settings.getCasted("floor-height"),
			settings.getCasted("offset-x"),
			settings.getCasted("offset-z"),
			settings.getCasted("parcel-axis-limit"),
			settings.getCasted("disable-explosions"),
			settings.getCasted("items-blocked")
		);
		 
	}
	
	@SuppressWarnings("unchecked")
	public static Optional<ParcelWorldSettings> parseSettings(ConfigurationSection worlds, String world, MultiRunner errorPrinter) {
		BiConsumer<String, String> errorAdder = (key, worldname) -> {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Option '%s' for world '%s' could not be parsed. Aborting generator.", key, worldname)));
		};
		
		if (worlds.isConfigurationSection(world)) {
			Map<String, Object> input = worlds.getConfigurationSection(world).getValues(false);
			Values.validate(input != null, "getValues() (input) null");
			CastingMap<String, Object> settings = new CastingMap<>();
			
			for (Entry<String, Object> entry : DEFAULT_WORLD_SETTINGS.entrySet()) {
				
				String key = entry.getKey();
				if (!input.containsKey(key)) {
					errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Option '%s' is missing from your settings. Aborting generator.", key)));
					continue;
				}
				
				Object value = null;
				Object inputValue = input.get(key);
				if (inputValue instanceof String) {
					try {
						value = BlockType.fromString((String) inputValue);
					} catch (NumberFormatException e) {
						errorAdder.accept(key, world);
					}
				} else if (key.equals("items-blocked")) {
					try {
						value = ((List<String>) inputValue).stream().map(v -> Material.getMaterial(v)).collect(Collectors.toList());
					} catch (ClassCastException e) {
						errorAdder.accept(key, world);
					}
				} else {
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
