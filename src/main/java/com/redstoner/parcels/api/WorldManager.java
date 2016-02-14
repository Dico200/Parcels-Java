package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.CastingMap;
import com.redstoner.utils.DuoObject.BlockType;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.Bool;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class WorldManager {
	
	public static final String WORLD_SETTINGS_FILE = "worlds.yml";
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
		}
		
	};
	
	public WorldManager(ParcelsPlugin plugin) {
		this.plugin = plugin;
		ParcelsPlugin.log("WorldManager initialized");
		loadSettingsFromConfig();
	}

	public boolean resize(String world, int axisLimit) {
		return exec(world, w -> w.resize(axisLimit));
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
		ParcelWorld w = getWorld(world);
		if (w == null) {
			ParcelsPlugin.debug(String.format("World '%s' doesn't exist.", world));
			worlds.forEach((key, value) -> {
				ParcelsPlugin.debug(String.format("World '%s' does exist: %s", key, value));
			});
		}
		return (w == null)? null : function.apply(w);
	}
	
	private boolean exec(String world, Consumer<ParcelWorld> function) {
		ParcelWorld w = getWorld(world);
		if (w == null)
			return false;
		function.accept(w);
		return true;
	}
	
	private ParcelsPlugin plugin;
	private HashMap<String, ParcelWorld> worlds = new HashMap<>();
	
	public ParcelWorld getWorld(String world) {
		return this.worlds.get(world);
	}
	
	private void loadSettingsFromConfig() {
		MultiRunner printErrors = new MultiRunner(() -> {
			ParcelsPlugin.log("##########################################################");
		}, () -> {
			ParcelsPlugin.log("##########################################################");
		});
		
		boolean useDefaultIfMissing = true; //TODO
		
		ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
		Bool.validate(worlds != null, "worlds section null");
		Bool.validate(worlds.getKeys(false) != null, "getKeys() null");
		
		for (String world : worlds.getKeys(false)) {
			
			if (worlds.isConfigurationSection(world)) {
				Map<String, Object> input = worlds.getConfigurationSection(world).getValues(false);
				Bool.validate(input != null, "getValues() (input) null");
				CastingMap<String, Object> settings = new CastingMap<>();
				
				for (Entry<String, Object> entry : DEFAULT_WORLD_SETTINGS.entrySet()) {
					String key = entry.getKey();
					if (!input.containsKey(key)) {
						printErrors.add(() -> ParcelsPlugin.log(String.format("  Option '%s' is missing from your settings. Aborting generator.", key)));
						continue;
					}
					Object value;
					try {
						Object inputValue = input.get(key);
						if (inputValue instanceof String)
							value = BlockType.fromString((String) inputValue);
						else
							value = inputValue;
					} catch (ClassCastException e) {
						if (!useDefaultIfMissing) {
							printErrors.add(() -> ParcelsPlugin.log(String.format("  Option '%s' must be an integer. Aborting generator.", key)));
							continue;
						}
						value = entry.getValue();
					}
					settings.put(key, value);
				}
				
				if (!printErrors.willRun()) {
					this.worlds.put(world, new ParcelWorld(world, settings));
				}
				input.keySet().stream().filter(key -> !DEFAULT_WORLD_SETTINGS.containsKey(key)).forEach(key -> {
					printErrors.add(() -> ParcelsPlugin.log(String.format("  Just FYI: Key '%s' isn't an option (Ignoring).", key)));
				});	
			} else {
				printErrors.add(() -> ParcelsPlugin.log(String.format("  A world must be configured as a ConfigurationSection (a map).")));
			}
			if (printErrors.willRun()) {
				printErrors.addFirst(() -> ParcelsPlugin.log(String.format("Exception(s) occurred while loading settings for world '%s':", world)));
			}
			printErrors.runAll();
			printErrors.reset();
		}
	}

}
