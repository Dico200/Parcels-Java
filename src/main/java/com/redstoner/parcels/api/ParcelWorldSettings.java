package com.redstoner.parcels.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
	
	public final int axisLimit;
	public final boolean staticTimeDay;
	public final boolean staticWeatherClear;
	
	public final boolean disableExplosions;
	public final boolean blockPortalCreation;
	public final boolean blockMobSpawning;
	public final List<Material> blockedItems;
	
	public final BlockType wallType;
	public final BlockType floorType; 
	public final BlockType fillType; 
	public final BlockType pathMainType; 
	public final BlockType pathEdgeType;
	public final int parcelSize;
	public final int floorHeight;
	public final int offsetX;
	public final int offsetZ;
	public final int sectionSize;
	public final int pathOffset;
	
	private ParcelWorldSettings(int axisLimit, boolean staticTimeDay, boolean staticWeatherClear,
			
			boolean disableExplosions, boolean blockPortalCreation, boolean blockMobSpawning, List<Material> itemsBlocked,
			
			BlockType wallType, BlockType floorType, BlockType fillType, BlockType pathMainType, BlockType pathEdgeType, 
			int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ) {
		
		this.axisLimit = axisLimit;
		this.staticTimeDay = staticTimeDay;
		this.staticWeatherClear = staticWeatherClear;
		
		// INTERACTION
		this.disableExplosions = disableExplosions;
		this.blockPortalCreation = blockPortalCreation;
		this.blockMobSpawning = blockMobSpawning;
		this.blockedItems = itemsBlocked;
		
		// GENERATOR
		this.wallType = wallType;
		this.floorType = floorType;
		this.fillType = fillType;
		this.pathMainType = pathMainType;
		this.pathEdgeType = pathEdgeType;
		this.parcelSize = parcelSize;
		this.floorHeight = floorHeight;
		this.offsetX = offsetX;
		this.offsetZ = offsetZ;
		this.sectionSize = parcelSize + pathSize;
		this.pathOffset = ((pathSize % 2 == 0)? pathSize + 2 : pathSize + 1) / 2;
	}
	
	private ParcelWorldSettings(CastingMap<String, Object> settings) {
		this(
			settings.getCasted("parcel-axis-limit"),
			settings.getCasted("static-time-day"),
			settings.getCasted("static-weather-clear"),
			
			settings.getCasted("interaction.disable-explosions"),
			settings.getCasted("interaction.block-portal-creation"),
			settings.getCasted("interaction.block-mob-spawning"),
			settings.getCasted("interaction.items-blocked"),
			
			settings.getCasted("generator.wall-type"), 
			settings.getCasted("generator.floor-type"), 
			settings.getCasted("generator.fill-type"), 
			settings.getCasted("generator.path-main-type"), 
			settings.getCasted("generator.path-edge-type"), 
			settings.getCasted("generator.parcel-size"), 
			settings.getCasted("generator.path-size"), 
			settings.getCasted("generator.floor-height"),
			settings.getCasted("generator.offset-x"),
			settings.getCasted("generator.offset-z")
		);
		 
	}
	
	public static ParcelWorldSettings parseMap(String worldName, Map<String, Object> input, MultiRunner errorPrinter) {
		
		ParcelWorldSettings parsed = null;
		
		Values.validate(input != null, "getValues() (input) null");
		CastingMap<String, Object> settings = new CastingMap<>();
		
		WORLD_SETTINGS_PARSER.forEach((key, function) -> {
			
			String excPrefix = "  Option '" + key + "' ";
			if (!input.containsKey(key)) {
				errorPrinter.add(() -> ParcelsPlugin.log(excPrefix + "is missing from your settings. Aborting generator."));
			} else {			
				try {
					settings.put(key, function.apply(input.get(key)));
				} catch (SettingParseException e) {
					errorPrinter.add(() -> ParcelsPlugin.log(excPrefix + "could not be parsed. Aborting generator."));
					errorPrinter.add(() -> ParcelsPlugin.log("    Cause: " + e.getMessage()));
				}
			}
			
		});
		
		if (!errorPrinter.willRun()) {
			parsed = new ParcelWorldSettings(settings);
		}
		
		input.keySet().stream()
		.filter(key -> !WORLD_SETTINGS_PARSER.containsKey(key))
		.filter(key -> !key.equals("interaction") && !key.equals("generator"))
		.forEach(key -> errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Just FYI: Key '%s' isn't an option (Ignoring).", key))));
		
		return parsed;
	}
	
	public static Optional<ParcelWorldSettings> parseSettings(ConfigurationSection worlds, String worldName) {
		MultiRunner errorPrinter = new MultiRunner(() -> {
			ParcelsPlugin.log("##########################################################");
			ParcelsPlugin.log(String.format("Exception(s) occurred while loading settings for world '%s':", worldName));
		}, () -> {
			ParcelsPlugin.log("##########################################################");
		});
		
		ParcelWorldSettings parsed = null;
		
		if (worlds.isConfigurationSection(worldName)) {

			parsed = parseMap(worldName, worlds.getConfigurationSection(worldName).getValues(true), errorPrinter);
			
		} else {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  A world must be configured as a ConfigurationSection (a map).")));
		}
		
		errorPrinter.runAll();
		return Optional.ofNullable(parsed);
	}
	
	private static final Map<String, Function<Object, Object>> WORLD_SETTINGS_PARSER = new HashMap<String, Function<Object, Object>>() {
		private static final long serialVersionUID = 1L;
		{
			Function<Object, Object> parseBlockType = obj -> {
				if (obj instanceof String) {
					try {
						return BlockType.fromString((String) obj);
					} catch (NumberFormatException e) {
						throw new SettingParseException("You have a number formatted incorrectly in your BlockType; " + e.getMessage());
					} catch (NullPointerException e) {
						throw new SettingParseException("A BlockType cannot be null");
					}
				} else {
					throw new SettingParseException("A BlockType must be formatted 'ID:DATA'");
				}
			};
			
			Function<Object, Object> checkInteger = obj -> {
				if (obj instanceof Integer) {
					return obj;
				} else {
					throw new SettingParseException("This must be an integer");
				}
			};
			
			Function<Object, Object> checkBoolean = obj -> {
				if (obj instanceof Boolean) {
					return obj;
				} else {
					throw new SettingParseException("This must be a boolean value");
				}
			};
			
			@SuppressWarnings("unchecked")
			Function<Object, Object> parseMaterialList = obj -> {
				try {
					return ((List<String>) obj).stream().map(v -> {
						Material type = Material.getMaterial(v);
						Values.checkNotNull(type, new SettingParseException(String.format("Material %s could not be parsed. "
								+ "See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html", v)));
						return type;
					}).collect(Collectors.toList());
				} catch (ClassCastException e) {
					throw new SettingParseException("This must be a list of material names");
				}
			};
			
			put("parcel-axis-limit", checkInteger);
			put("static-time-day", checkBoolean);
			put("static-weather-clear", checkBoolean);
			
			put("interaction.disable-explosions", checkBoolean);
			put("interaction.block-portal-creation", checkBoolean);
			put("interaction.block-mob-spawning", checkBoolean);
			put("interaction.items-blocked", parseMaterialList);
			
			put("generator.wall-type", parseBlockType);
			put("generator.floor-type", parseBlockType);
			put("generator.fill-type", parseBlockType);
			put("generator.path-main-type", parseBlockType);
			put("generator.path-edge-type", parseBlockType);
			put("generator.parcel-size", checkInteger);
			put("generator.path-size", checkInteger);
			put("generator.floor-height", checkInteger);
			put("generator.offset-x", checkInteger);
			put("generator.offset-z", checkInteger);
		}
	};
	
}

class SettingParseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SettingParseException(String message) {
		super(message);
	}
	
}
