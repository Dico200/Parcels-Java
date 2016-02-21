package com.redstoner.parcels.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bukkit.configuration.ConfigurationSection;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.mysql.SqlConnector;

public class StorageManager {
	
	public static boolean useMySQL = true;
	
	public static void initialise() {
		
		if (useMySQL) {
			
			ParcelsPlugin.log("Using MySQL database for storage. Initialising.");
			
			SqlManager.initialise(getConnector());
			
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
	
	public static void save() {
		save(StorageManager.useMySQL);
	}
	
	public static void save(boolean useMySQL) {
		if (useMySQL != StorageManager.useMySQL) {
			ParcelsPlugin.getInstance().getConfig().set("MySQL.enabled", useMySQL);
			if (useMySQL) {
				ParcelsPlugin.log("Converting all storage to MySQL Database. This might take a little while.");
				
				SqlManager.saveAll(getConnector());
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
	
	private static SqlConnector getConnector() {
		ConfigurationSection conf = ParcelsPlugin.getInstance().getConfig().getConfigurationSection("MySQL");
		if (conf == null)
			throw new UnsupportedOperationException();
		
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
