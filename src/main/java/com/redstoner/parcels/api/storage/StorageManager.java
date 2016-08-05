package com.redstoner.parcels.api.storage;

import java.io.File;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.ErrorPrinter;
import com.redstoner.utils.sql.MySQLConnector;
import com.redstoner.utils.sql.SQLConnector;
import com.redstoner.utils.sql.SQLiteConnector;

public class StorageManager {
	
	public static boolean useMySQL = true;
	public static boolean connected = false;
	
	public static void initialise() {

		SQLConnector parcelsConnector = getParcelsConnector();
		if (parcelsConnector == null || !parcelsConnector.isConnected()) {
			ParcelsPlugin.log("[SEVERE] Failed to connect to parcels database. Aborting connection.");
			return;
		}
		connected = true;
		
		SqlManager.initialise(parcelsConnector, true, true);
		
		FileConfiguration config = ParcelsPlugin.getInstance().getConfig();
		if (config.getBoolean("import-plotme-settings.enabled")) {
			config.set("import-plotme-settings.enabled", false);
			ParcelsPlugin.getInstance().saveConfig();
			importPlotMeSettings(parcelsConnector);
		}
		
	}
	
	private static boolean importPlotMeSettings(SQLConnector parcelsConnector) {
		
		ErrorPrinter errorPrinter = new ErrorPrinter(s -> ParcelsPlugin.log(s),
				"Error occurred while attempting to import plotme settings:",
				"Next time you try to import plotme settings, make sure to set enabled to true again.");
		
		ConfigurationSection importSettings = ParcelsPlugin.getInstance().getConfig().getConfigurationSection("import-plotme-settings");
		SQLConnector plotMeConnector = getPlotMeConnector();
		if (plotMeConnector == null) {
			errorPrinter.add("Failed to connect to PlotMe database");
			errorPrinter.runAll();
			return false;
		}
		
		ConfigurationSection worlds = importSettings.getConfigurationSection("worlds");
		if (worlds == null) {
			errorPrinter.add("Couldn't find import-plotme-settings.worlds in config");
			errorPrinter.runAll();
			return false;
		}
		
		ParcelsPlugin.log("Starting import from plotme database. This may take a while. Preferably, don't use the parcel world in the meantime.");
		worlds.getValues(false).forEach((worldFrom, obj) -> {
			
			if (!(obj instanceof String)) {
				errorPrinter.add(String.format("Failed to find parcel world associated with plotme world '%s'", worldFrom));
				return;
			}
			
			String worldTo = (String) obj;
			SqlManager.importFromPlotMe(plotMeConnector, worldFrom, worldTo, errorPrinter);
			
		});
		errorPrinter.runAll();
		return true;		
	}
	
	public static void save() {	
		SqlManager.saveAll(getParcelsConnector());
	}
	
	private static SQLConnector getParcelsConnector() {
		ConfigurationSection conf = ParcelsPlugin.getInstance().getConfig().getConfigurationSection("storage");
		if (conf == null)
			return null;
			
		String hostname = (hostname = conf.getString("hostname")) == null ? "localhost:3306" : hostname;
		String database = (database = conf.getString("database")) == null ? "parcels" : database;
		String username = (username = conf.getString("username")) == null ? "root" : username;
		String password = (password = conf.getString("password")) == null ? "" : password;
		return new MySQLConnector(hostname, database, username, password);		
	}
	
	private static SQLConnector getPlotMeConnector() {
		Plugin plotMe = Bukkit.getPluginManager().getPlugin("PlotMe");
		if (plotMe == null)
			return null;
		
		ConfigurationSection conf = plotMe.getConfig();
		if (conf.getBoolean("usemySQL")) {
			String hostname;
			String database;
			String username;
			String password;
			
			hostname = conf.getString("mySQLconn");
			if (hostname == null)
				return null;
			if (hostname.toLowerCase().startsWith("jdbc:mysql://"))
				hostname = hostname.substring(13);			
			if (!(hostname.lastIndexOf('/') > -1)) {
				return null;
			}
			String[] split = hostname.split("/");
			database = split[split.length - 1];
			hostname = String.join("/", Arrays.copyOfRange(split, 0, split.length - 1));					
			username = (username = conf.getString("mySQLuname")) == null ? "root" : username;
			password = (password = conf.getString("mySQLpass")) == null ? "" : password;
			
			ParcelsPlugin.log("Connecting to PlotMe's MySQL database");
			return new MySQLConnector(hostname, database, username, password);
			
		} else {
			return new SQLiteConnector(new File(plotMe.getDataFolder(), "plots.db"));
		}
	}

}
