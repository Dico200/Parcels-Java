package com.redstoner.parcels.api.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.ParcelContainer;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.mysql.SqlConnector;

public class StorageManager {
	
	public static boolean useMySQL = true;
	
	public static void initialise() {
		
		if (useMySQL) {
			
			ParcelsPlugin.log("Using MySQL database for storage. Initialising.");
			
			FileConfiguration config = ParcelsPlugin.getInstance().getConfig();
			SqlConnector parcelsConnector = getConnector(config.getConfigurationSection("MySQL"));
			if (parcelsConnector == null) {
				ParcelsPlugin.log("[SEVERE] Failed to connect to parcels database. Aborting connection.");
				return;
			}
			
			if (config.getBoolean("import-plotme-settings.enabled")) {
				config.set("import-plotme-settings.enabled", false);
				ParcelsPlugin.getInstance().saveConfig();
				SqlManager.initialise(parcelsConnector, false);
				importPlotMeSettings(parcelsConnector);
			} else {
				SqlManager.initialise(parcelsConnector, true);
			}
			
		} else {
			
			WorldManager.getWorlds().forEach((name, world) -> {
				File file = new File(worldSaveFile(name));
				if (file.exists()) {
					try {
						ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(worldSaveFile(name))));
						ParcelContainer parcels = (ParcelContainer) ois.readObject();
						world.setParcels(parcels);
						ois.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	private static boolean importPlotMeSettings(SqlConnector parcelsConnector) {
		
		MultiRunner errorPrinter = new MultiRunner(() -> {
			ParcelsPlugin.log("Error occurred while attempting to import plotme settings:");
		}, () -> {
			ParcelsPlugin.log("Next time you try to import plotme settings, make sure to set import-plotme to true again.");
		});
		
		ConfigurationSection importSettings = ParcelsPlugin.getInstance().getConfig().getConfigurationSection("import-plotme-settings");
		if (importSettings == null) {
			errorPrinter.add(() -> ParcelsPlugin.log("  Couldn't find import-plotme-settings in config"));
			errorPrinter.runAll();
			return false;
		}
		
		SqlConnector plotMeConnector = getConnector(importSettings.getConfigurationSection("MySQL"));
		if (plotMeConnector == null) {
			errorPrinter.add(() -> ParcelsPlugin.log("  Couldn't find import-plotme-settings.MySQL in config"));
			errorPrinter.runAll();
			return false;
		}
		
		ConfigurationSection worlds = importSettings.getConfigurationSection("worlds");
		if (worlds == null) {
			errorPrinter.add(() -> ParcelsPlugin.log("  Couldn't find import-plotme-settings.worlds in config"));
			errorPrinter.runAll();
			return false;
		}
		
		ParcelsPlugin.log("Starting import from plotme database. This may take a while. Preferably, don't use the parcel world in the meantime.");
		SqlManager.initialise(parcelsConnector, false);
		worlds.getValues(false).forEach((worldFrom, obj) -> {
			
			if (!(obj instanceof String)) {
				errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Failed to find parcel world associated with plotme world '%s'", worldFrom)));
			}
			
			String worldTo = (String) obj;
			SqlManager.ImportFromPlotMe(plotMeConnector, worldFrom, worldTo, errorPrinter);
			
		});
		errorPrinter.runAll();
		return true;		
	}
	
	public static void save() {
		save(StorageManager.useMySQL);
	}
	
	public static void save(boolean useMySQL) {
		if (useMySQL != StorageManager.useMySQL) {
			ParcelsPlugin.getInstance().getConfig().set("MySQL.enabled", useMySQL);
			if (useMySQL) {
				ParcelsPlugin.log("Converting all storage to MySQL Database. This might take a little while.");
				
				SqlManager.saveAll(getConnector(ParcelsPlugin.getInstance().getConfig().getConfigurationSection("MySQL")));
			}
		}
		if (!useMySQL) {
			WorldManager.getWorlds().forEach((name, world) -> {
				try {
					File file = new File(worldSaveFile(name));
					if (!file.exists()) {
						
					}
					ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
					oos.writeObject(world.getParcels());
					oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}
	
	private static SqlConnector getConnector(ConfigurationSection conf) {
		//ConfigurationSection conf = ParcelsPlugin.getInstance().getConfig().getConfigurationSection("MySQL");
		if (conf == null)
			return null;
		
		String hostname = (hostname = conf.getString("hostname")) == null ? "localhost:3306" : hostname;
		String database = (database = conf.getString("database")) == null ? "redstoner" : database;
		String username = (username = conf.getString("username")) == null ? "root" : username;
		String password = (password = conf.getString("password")) == null ? "" : password;
		
		return new SqlConnector(hostname, database, username, password);
	}
	
	private static String worldSaveFile(String worldName) {
		return ParcelsPlugin.getInstance().getDataFolder() + File.separator + "worlds" + File.separator + worldName + ".dat";
	}
	
	// -------------------------------------------------------------------

}
