package com.redstoner.parcels.api.storage;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.GlobalTrusted;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.ErrorPrinter;
import com.redstoner.utils.sql.SQLConnector;

import static com.google.common.base.Preconditions.checkArgument;

public class SqlManager {
	
	private static final String
		PARCELS_QUERY = "SELECT `id`, `px`, `pz`, `owner`, `allow_interact_inputs`, `allow_interact_inventory` FROM `parcels` WHERE `world` = ?;",
		PARCEL_ADDED_QUERY = "SELECT `player`, `allowed` FROM `parcels_added` WHERE `id` = ?;",
		PARCEL_ID_QUERY = "SELECT `id` FROM `parcels` WHERE `world` = ? AND `px` = ? AND `pz` = ?;",
		GLOBAL_ADDED_QUERY = "SELECT `player`, `added`, `allowed` FROM `global_added`;",
		CREATE_TABLE_PARCELS = "CREATE TABLE IF NOT EXISTS `parcels` ("
				+ "`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"
				+ "`world` VARCHAR(32) NOT NULL,"
				+ "`px` INTEGER NOT NULL,"
				+ "`pz` INTEGER NOT NULL,"
				+ "`owner` BINARY(16),"
				+ "`allow_interact_inputs` TINYINT(1) NOT NULL DEFAULT 0,"
				+ "`allow_interact_inventory` TINYINT(1) NOT NULL DEFAULT 0,"
				+ "UNIQUE KEY location(`world`, `px`, `pz`)"
				+ ");",
		CREATE_TABLE_PARCELS_ADDED = "CREATE TABLE IF NOT EXISTS `parcels_added` ("
				+ "`id` INTEGER NOT NULL,"
				+ "`player` BINARY(16) NOT NULL,"
				+ "`allowed` TINYINT(1) NOT NULL,"
				+ "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
				+ "UNIQUE KEY added(`id`, `player`)"
				+ ");",
		CREATE_TABLE_GLOBAL_ADDED = "CREATE TABLE IF NOT EXISTS `global_added` ("
				+ "`player` BINARY(16) NOT NULL,"
				+ "`added` BINARY(16) NOT NULL,"
				+ "`allowed` TINYINT(1) NOT NULL,"
				+ "UNIQUE KEY pair(`player`, `added`)"
				+ ");",
		DROP_TABLES = "DROP TABLE IF EXISTS `parcels_added`;"
				+ "DROP TABLE IF EXISTS `parcels`;",
		SET_ALLOW_INTERACT_INPUTS = "UPDATE `parcels` SET `allow_interact_inputs` = ? WHERE `id` = ?;",
		SET_ALLOW_INTERACT_INVENTORY = "UPDATE `parcels` SET `allow_interact_inventory` = ? WHERE `id` = ?;",
		SET_OWNER_UPDATE = "UPDATE `parcels` SET `owner` = ? WHERE `id` = ?;",
		PARCEL_ADD_PLAYER_UPDATE = "REPLACE `parcels_added` (`id`, `player`, `allowed`) VALUES (?, ?, ?);",
		PARCEL_REMOVE_PLAYER_UPDATE = "DELETE FROM `parcels_added` WHERE `id` = ? AND `player` = ?;",
		PARCEL_CLEAR_PLAYERS_UPDATE = "DELETE FROM `parcels_added` WHERE `id` = ?;",
		GLOBAL_ADD_PLAYER_UPDATE = "REPLACE `global_added` (`player`, `added`, `allowed`) VALUES (?, ?, ?);",
		GLOBAL_REMOVE_PLAYER_UPDATE = "DELETE FROM `global_added` WHERE `player` = ? AND `added` = ?;",
		GLOBAL_CLEAR_PLAYERS_UPDATE = "DELETE FROM `global_added` WHERE `player` = ?;",
		ADD_PARCEL_UPDATE = "INSERT IGNORE `parcels` (`world`, `px`, `pz`) VALUES (?, ?, ?);";
	
	public static SQLConnector CONNECTOR = null;
	
	public static void initialise(SQLConnector parcelsConnector, boolean load) {
		CONNECTOR = parcelsConnector;
		CONNECTOR.asyncConn(conn -> {
			try {
				Statement stm = conn.createStatement();
				stm.executeUpdate(CREATE_TABLE_PARCELS);
				stm.executeUpdate(CREATE_TABLE_PARCELS_ADDED);
				stm.executeUpdate(CREATE_TABLE_GLOBAL_ADDED);
				stm.close();
			} catch (SQLException e) {
				logSqlExc("An error occurred while creating the tables", e);
			}
			if (load) {
				loadAllFromDatabase(conn);
			}
		});
	}
	
	public static void loadAllFromDatabase(Connection conn) {
		for (String worldName : WorldManager.getWorlds().keySet()) {
			loadFromDatabase(conn, worldName, false);
		}
		
		loadGlobalAddedFromDatabase(conn);
	}
	
	private static void loadFromDatabase(Connection conn, String worldName, boolean resetContainer) {
		ParcelWorld world = WorldManager.getWorld(worldName).orElse(null);
		if (world == null) {
			ParcelsPlugin.debug(String.format("Couldn't find ParcelWorld instance for world by name '%s'", worldName));
			return;
		}
		
		if (resetContainer) {
			world.refreshParcels();
		}
		
		try {
			PreparedStatement query = conn.prepareStatement(PARCELS_QUERY);
			query.setString(1, worldName);
			ResultSet parcels = query.executeQuery();
			
			while (parcels.next()) {
				
				int px = parcels.getInt(2);
				int pz = parcels.getInt(3);				
				Parcel parcel = world.getParcelAtID(px, pz).orElse(null);
				
				if (parcel == null) {
					parcels.deleteRow();
					ParcelsPlugin.debug(String.format("Deleted parcel at %s,%s from database", px, pz));
					continue;
				}
				
				byte[] owner = parcels.getBytes(4);
				if (owner != null) {
					parcel.setOwnerIgnoreSQL(toUUID(owner));
				}
				
				parcel.getSettings().setAllowsInteractInputsIgnoreSQL(parcels.getInt(5) != 0);
				parcel.getSettings().setAllowsInteractInventoryIgnoreSQL(parcels.getInt(6) != 0);
				
				PreparedStatement query2 = conn.prepareStatement(PARCEL_ADDED_QUERY);
				query2.setInt(1, parcels.getInt(1));
				ResultSet added = query2.executeQuery();
				
				Map<UUID, Boolean> addedPlayers = parcel.getAdded().getMap();
				while (added.next()) {
					addedPlayers.put(toUUID(added.getBytes(1)), added.getInt(2) != 0);
				}
				added.close();
			}
			parcels.close();
			
		} catch (SQLException e) {
			logSqlExc(String.format("[SEVERE] Error occurred while loading data for world '%s'", worldName), e);
		}
	}
	
	private static void loadGlobalAddedFromDatabase(Connection conn) {
		try {
			
			Statement stm = conn.createStatement();
			ResultSet addedSet = stm.executeQuery(GLOBAL_ADDED_QUERY);			
			while (addedSet.next()) {
				GlobalTrusted.addPlayerIgnoreSQL(addedSet.getString(1), addedSet.getString(2), addedSet.getInt(3) != 0);			
			}
			
		} catch (SQLException e) {
			logSqlExc("An exception occurred while retrieving globally added players", e);
		}
	}
	
    static byte[] toBytes(UUID uuid) {
    	return ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }
	
	private static UUID toUUID(byte[] arr) {
		checkArgument(arr.length == 16, "Illegal byte[] length");
		ByteBuffer buffer = ByteBuffer.wrap(arr);
		return new UUID(buffer.getLong(), buffer.getLong());
	}
	
	protected static void logSqlExc(String header, SQLException e) {
		ParcelsPlugin.log(header);
		ParcelsPlugin.log("Error code: " + e.getErrorCode());
		ParcelsPlugin.log("SQL State: " + e.getSQLState());
		ParcelsPlugin.log("Details: " + e.getMessage());
		ParcelsPlugin.log("---------------- Start Stack ----------------");
		e.printStackTrace();
		ParcelsPlugin.log("----------------  End Stack  ----------------");
	}
	
	public static void setOwner(String world, int px, int pz, UUID owner) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setOwner(conn, getId(conn, world, px, pz), owner == null ? null : toBytes(owner));
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while setting owner for a parcel", e);
			}
		});
	}
	
	/* Setting template
	public static void setAllowInteract(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, world, px, pz, SET_ALLOW_INTERACT_, enabled);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while setting setAllowInteract for a parcel", e);
			}
		});
	}
	*/
	
	public static void setAllowInteractInputs(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, getId(conn, world, px, pz), SET_ALLOW_INTERACT_INPUTS, enabled);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while setting allowInteractInputs for a parcel", e);
			}
		});
	}
	
	public static void setAllowInteractInventory(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, getId(conn, world, px, pz), SET_ALLOW_INTERACT_INVENTORY, enabled);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while setting allowInteractInventory for a parcel", e);
			}
		});
	}
	
	public static void addPlayer(String world, int px, int pz, UUID player, boolean allowed) {
		CONNECTOR.asyncConn(conn -> {
			try {
				addPlayer(conn, getId(conn, world, px, pz), toBytes(player), allowed);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while adding a player to a parcel", e);
			}
		});
	}
	
	public static void removePlayer(String world, int px, int pz, UUID player) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removePlayer(conn, getId(conn, world, px, pz), toBytes(player));
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while removing a player from a parcel", e);
			}
		});
	}
	
	public static void removeAllPlayers(String world, int px, int pz) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removeAllPlayers(conn, world, px, pz);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while removing all players from a parcel", e);
			}
		});
	}
	
	public static void addGlobalPlayer(UUID player, UUID added, boolean allowed) {
		CONNECTOR.asyncConn(conn -> {
			try {
				addGlobalPlayer(conn, toBytes(player), toBytes(added), allowed);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while globally adding a player to someone's parcels", e);
			}
		});
	}
	
	public static void removeGlobalPlayer(UUID player, UUID removed) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removeGlobalPlayer(conn, toBytes(player), toBytes(removed));
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while removing a globally added player from someone's parcels", e);
			}
		});
	}
	
	public static void removeAllGlobalPlayers(UUID player) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removeAllGlobalPlayers(conn, toBytes(player));
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while removing all globally added players from someone's parcels", e);
			}
		});
	}
	
	private static void setOwner(Connection conn, int id, byte[] owner) throws SQLException {
		PreparedStatement update = conn.prepareStatement(SET_OWNER_UPDATE);
		update.setBytes(1, owner);
		update.setInt(2, id);
		update.executeUpdate();
		update.close();
	}
	
	private static void setBooleanParcelSetting(Connection conn, int id, String query, boolean enabled) throws SQLException {
		PreparedStatement update = conn.prepareStatement(query);
		update.setBoolean(1, enabled);
		update.setInt(2, id);
		update.executeUpdate();
		update.close();
	}
	
	private static void addPlayer(Connection conn, int id, byte[] player, boolean allowed) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_ADD_PLAYER_UPDATE);
		update.setInt(1, id);
		update.setBytes(2, player);
		update.setBoolean(3, allowed);
		update.executeUpdate();
		update.close();
	}
	
	private static void removePlayer(Connection conn, int id, byte[] player) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_REMOVE_PLAYER_UPDATE);
		update.setInt(1, id);
		update.setBytes(2, player);
		update.executeUpdate();
		update.close();
	}
	
	private static void removeAllPlayers(Connection conn, String world, int px, int pz) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_CLEAR_PLAYERS_UPDATE);
		update.setInt(1, getId(conn, world, px, pz));
		update.executeUpdate();
	}
	
	private static void addGlobalPlayer(Connection conn, byte[] player, byte[] added, boolean allowed) throws SQLException {
		PreparedStatement update = conn.prepareStatement(GLOBAL_ADD_PLAYER_UPDATE);
		update.setBytes(1, player);
		update.setBytes(2, added);
		update.setInt(3, allowed ? 1 : 0);
		update.executeUpdate();
		update.close();
	}
	
	private static void removeGlobalPlayer(Connection conn, byte[] player, byte[] removed) throws SQLException {
		PreparedStatement update = conn.prepareStatement(GLOBAL_REMOVE_PLAYER_UPDATE);
		update.setBytes(1, player);
		update.setBytes(2, removed);
		update.executeUpdate();
		update.close();
	}
	
	private static void removeAllGlobalPlayers(Connection conn, byte[] player) throws SQLException {
		PreparedStatement update = conn.prepareStatement(GLOBAL_CLEAR_PLAYERS_UPDATE);
		update.setBytes(1, player);
		update.executeUpdate();
		update.close();
	}
	
	private static int getId(Connection conn, String world, int px, int pz) throws SQLException {
		
		PreparedStatement query = conn.prepareStatement(PARCEL_ID_QUERY);
		query.setString(1, world);
		query.setInt(2, px);
		query.setInt(3, pz);
		ResultSet set = query.executeQuery();
		
		int ret;
		if (set.next()) {
			ret = set.getInt(1);
		} else {
			
			PreparedStatement update = conn.prepareStatement(ADD_PARCEL_UPDATE);
			update.setString(1, world);
			update.setInt(2, px);
			update.setInt(3, pz);
			update.executeUpdate();
			update.close();
			
			ret = getId(conn, world, px, pz);
		}
		
		query.close();
		return ret;
	}
	
	static void saveAll(SQLConnector sqlConnector) {
		CONNECTOR = sqlConnector;
		CONNECTOR.asyncConn(conn -> {
			try {
				Statement stm = conn.createStatement();
				stm.executeUpdate(DROP_TABLES);
				stm.executeUpdate(CREATE_TABLE_PARCELS);
				stm.executeUpdate(CREATE_TABLE_PARCELS_ADDED);
				stm.close();
				
				for (Map.Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String worldName = entry.getKey();
					ParcelWorld world = entry.getValue();
					for (Parcel parcel : world.getParcels().getAll()) {
						int x = parcel.getX();
						int z = parcel.getZ();
						if (parcel.getOwner().isPresent()) {
							setOwner(conn, getId(conn, worldName, x, z), toBytes(parcel.getOwner().get()));
						}
						for (Map.Entry<UUID, Boolean> added : parcel.getAdded().getMap().entrySet()) {
							addPlayer(conn, getId(conn, worldName, x, z), toBytes(added.getKey()), added.getValue());
						}
					}
				}
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while saving all parcel data", e);
			}
		});
	}
	
	public static void importFromPlotMe(SQLConnector plotMeConnector, String worldNameFrom, String worldNameTo, ErrorPrinter errorPrinter) {
		
		ParcelWorld world = WorldManager.getWorld(worldNameTo).orElse(null);
		if (world == null) {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Couldn't find parcel world '%s' while preparing to convert plotme database", worldNameTo)));
			return;
		}
		
		plotMeConnector.asyncConn(plotMeConn -> {			
			CONNECTOR.syncConn(parcelsConn -> {
				
				PlotMeTableSetup setup = PlotMeTableSetup.getSetup(plotMeConn);
				
				if (setup == null) {
					ParcelsPlugin.log("[ERROR] Didn't find PlotMe's MySQL tables to import from");
					loadFromDatabase(parcelsConn, worldNameTo, false);
					return;
				}
			
				try {					
					
					ResultSet plotSet = setup.getPlots(plotMeConn, worldNameFrom);
					
					if (!plotSet.isBeforeFirst()) {
						ParcelsPlugin.log(String.format("[ERROR] No PlotMe data found for world by name '%s' (but the table exists)", worldNameFrom));
						loadFromDatabase(parcelsConn, worldNameTo, false);
						return;
					}
					
					Statement parcelsSmt = parcelsConn.createStatement();
					parcelsSmt.executeUpdate("DELETE FROM `parcels_added`;");
					parcelsSmt.executeUpdate("DELETE FROM `parcels`;");
					parcelsSmt.close();
					
					while (plotSet.next()) {

						int parcelId = getId(parcelsConn, worldNameTo, plotSet.getInt(1), plotSet.getInt(2));	
						setOwner(parcelsConn, parcelId, setup.getBytesFromIndex(plotSet, 3));
						
						// Import allowed players
						ResultSet allowedSet = setup.getAllowed(plotMeConn, plotSet);				
						while (allowedSet.next()) {
							addPlayer(parcelsConn, parcelId, setup.getBytesFromIndex(allowedSet, 1), true);
						}
						allowedSet.close();
						
						// Import denied players
						ResultSet deniedSet = setup.getDenied(plotMeConn, plotSet);					
						while (deniedSet.next()) {
							addPlayer(parcelsConn, parcelId, setup.getBytesFromIndex(deniedSet, 1), false);
						}
						deniedSet.close();
						
					}
					plotSet.close();
					
				} catch (SQLException e) {
					logSqlExc("[SEVERE] Error occurred while importing from PlotMe database", e);
				}
				
				loadFromDatabase(parcelsConn, worldNameTo, true);
				
			});
		});		
	}
	
}

enum PlotMeTableSetup {
	
	FIRST {
		
		@Override
		public String getPlotMeTableName() {
			return "plotmecore_plots";
		}
		
		@Override
		public ResultSet getPlots(Connection conn, String worldName) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `plotX`, `plotZ`, `ownerID`, `plot_id` FROM `plotmecore_plots` WHERE `world` = ?;");
			query.setString(1, worldName);
			return query.executeQuery();
		}
		
		@Override
		public ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `player` FROM `plotmecore_allowed` WHERE `plot_id` = ?;");
			query.setInt(1, plotSet.getInt(4));
			return query.executeQuery();
		}
		
		@Override
		public ResultSet getDenied(Connection conn, ResultSet plotSet) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `player` FROM `plotmecore_denied` WHERE `plot_id` = ?;");
			query.setInt(1, plotSet.getInt(4));
			return query.executeQuery();
		}

		@Override
		public byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException {
			return SqlManager.toBytes(UUID.fromString(set.getString(index)));
		}
		
	},
	
	SECOND {
		
		@Override
		public String getPlotMeTableName() {
			return "plotmeplots";
		}
		
		@Override
		public ResultSet getPlots(Connection conn, String worldName) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `idZ`, `idX`, `ownerId`, `world` FROM `plotmeplots` WHERE `world` = ?;");
			query.setString(1, worldName);
			return query.executeQuery();
		}
		
		@Override
		public ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmeallowed` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
			query.setString(1, plotSet.getString(4));
			query.setInt(2, plotSet.getInt(1));
			query.setInt(3, plotSet.getInt(2));
			return query.executeQuery();
		}
		
		@Override
		public ResultSet getDenied(Connection conn, ResultSet plotSet) throws SQLException {
			PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmedenied` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
			query.setString(1, plotSet.getString(4));
			query.setInt(2, plotSet.getInt(1));
			query.setInt(3, plotSet.getInt(2));
			return query.executeQuery();
		}
		
		@Override
		public byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException {
			byte[] arr = set.getBytes(index);
			checkArgument(arr.length == 16, "Illegal byte[] length");
		    return arr;
		}
		
	};
	
	public abstract String getPlotMeTableName();
	
	public abstract ResultSet getPlots(Connection conn, String worldName) throws SQLException;
	
	public abstract ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException;
	
	public abstract ResultSet getDenied(Connection conn, ResultSet plotSet) throws SQLException;
	
	public abstract byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException;
	
	public boolean isCase(Connection conn) {
		ResultSet set = null;
		boolean ret = false;
		try {
			ret = (set = conn.getMetaData().getTables(null, null, getPlotMeTableName(), null)).next();
		} catch (SQLException e) {
			SqlManager.logSqlExc("SQL Failed Table Check:", e);
		} finally {
			if (set != null) {
				try {
					set.close();
				} catch (SQLException e) {
					SqlManager.logSqlExc("SQL Exception on closing:", e);
				}
			}
		}
		return ret;
	}
	
	public static PlotMeTableSetup getSetup(Connection conn) {
		for (PlotMeTableSetup setup : values()) {
			if (setup.isCase(conn)) {
				return setup;
			}
		}
		return null;
	}
	
}
