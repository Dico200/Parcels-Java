package com.redstoner.parcels.api.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.Optional;
import com.redstoner.utils.mysql.SqlConnector;
import com.redstoner.utils.mysql.SqlUtil;

public class SqlManager {
	
	@SuppressWarnings("unused")
	private static final String
		PARCELS_QUERY = "SELECT `id`, `px`, `pz`, `owner`, `allow_interact_inputs`, `allow_interact_inventory` FROM `parcels` WHERE `world` = ?;",
		//PARCEL_OWNER_QUERY = "SELECT `owner` FROM `parcels` WHERE `id` = ?;",
		PARCEL_ADDED_QUERY = "SELECT `player`, `allowed` FROM `parcels_added` WHERE `id` = ?;",
		PARCEL_ID_QUERY = "SELECT `id` FROM `parcels` WHERE `world` = ? AND `px` = ? AND `pz` = ?;",
		CREATE_TABLE_PARCELS = "CREATE TABLE IF NOT EXISTS `parcels` ("
				+ "`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"
				+ "`world` VARCHAR(32) NOT NULL,"
				+ "`px` INTEGER NOT NULL,"
				+ "`pz` INTEGER NOT NULL,"
				+ "`owner` VARCHAR(36),"
				+ "`allow_interact_inputs` TINYINT(1) NOT NULL DEFAULT 0,"
				+ "`allow_interact_inventory` TINYINT(1) NOT NULL DEFAULT 0,"
				+ "UNIQUE KEY location(`world`, `px`, `pz`)"
				+ ");",
		CREATE_TABLE_PARCELS_ADDED = "CREATE TABLE IF NOT EXISTS `parcels_added` ("
				+ "`id` INTEGER NOT NULL,"
				+ "`player` VARCHAR(36) NOT NULL,"
				+ "`allowed` TINYINT(1) NOT NULL,"
				+ "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
				+ "UNIQUE KEY added(`id`, `player`)"
				+ ");",
		CREATE_TABLE_GLOBAL_ADDED = "CREATE TABLE IF NOT EXISTS `global_added` ("
				+ "`player` VARCHAR(36) NOT NULL,"
				+ "`added` VARCHAR(36) NOT NULL,"
				+ "`allowed` TINYINT(1) NOT NULL,"
				+ "UNIQUE KEY pair(`player`, `friend`)"
				+ ");",
		DROP_TABLES = "DROP TABLE IF EXISTS `parcels_added`;"
				+ "DROP TABLE IF EXISTS `parcels`;",
		SET_ALLOW_INTERACT_INPUTS = "UPDATE `parcels` SET `allow_interact_inputs` = ? WHERE `id` = ?;",
		SET_ALLOW_INTERACT_INVENTORY = "UPDATE `parcels` SET `allow_interact_inventory` = ? WHERE `id` = ?;",
		SET_OWNER_UPDATE = "UPDATE `parcels` SET `owner` = ? WHERE `id` = ?;",
		PARCEL_ADD_PLAYER_UPDATE = "REPLACE `parcels_added` (`id`, `player`, `allowed`) VALUES (?, ?, ?);",
		PARCEL_REMOVE_PLAYER_UPDATE = "DELETE FROM `parcels_added` WHERE `id` = ? AND `player` = ?;",
		PARCEL_CLEAR_PLAYERS_UPDATE = "DELETE FROM `parcels_added` WHERE `id` = ?;",
		ADD_PARCEL_UPDATE = "INSERT IGNORE `parcels` (`world`, `px`, `pz`) VALUES (?, ?, ?);";
	
	public static SqlConnector CONNECTOR = null;
	
	public static void initialise(SqlConnector connector, boolean load) {
		CONNECTOR = connector;
		CONNECTOR.asyncConn(conn -> {
			SqlUtil.executeUpdate(conn, CREATE_TABLE_PARCELS, CREATE_TABLE_PARCELS_ADDED);
			
			if (load) {
				loadAllFromDatabase(conn);
			}
		});
	}
	
	public static void loadAllFromDatabase(Connection conn) {
		for (String worldName : WorldManager.getWorlds().keySet()) {
			loadFromDatabase(conn, worldName, false);
		}
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
				String owner = parcels.getString(4);
				
				Optional<Parcel> parcel = world.getParcelAtID(px, pz);
				
				if (!parcel.isPresent()) {
					parcels.deleteRow();
					ParcelsPlugin.debug(String.format("Deleted parcel at %s,%s from database", px, pz));
					continue;
				}
				
				Parcel p = parcel.get();
				
				if (owner != null) {
					p.setOwnerIgnoreSQL(toUUID(owner));
				}
				
				p.getSettings().setAllowsInteractInputsIgnoreSQL(parcels.getInt(5) != 0);
				p.getSettings().setAllowsInteractInventoryIgnoreSQL(parcels.getInt(6) != 0);
				
				PreparedStatement query2 = conn.prepareStatement(PARCEL_ADDED_QUERY);
				query2.setInt(1, parcels.getInt(1));
				ResultSet added = query2.executeQuery();
				
				Map<UUID, Boolean> addedPlayers = p.getAdded().getMap();
				while (added.next()) {
					addedPlayers.put(toUUID(added.getString(1)), added.getInt(2) != 0);
				}
				added.close();
			}
			parcels.close();
			
		} catch (SQLException e) {
			logSqlExc(String.format("[SEVERE] Error occurred while loading data for world '%s'", worldName), e);
		}
	}
	
	private static UUID toUUID(String uuid) {
		return UUID.fromString(uuid);
	}
	
	/*
	private static final List<ParcelId> UPDATING = new ArrayList<>();
	
	public static void update(String world, int px, int pz) {
		ParcelId PID = ParcelId.of(world, px, pz);
		if (UPDATING.contains(PID))
			return;
		UPDATING.add(PID);
		CONNECTOR.asyncConn(conn -> {
			try {
				Parcel parcel = new Parcel(world, px, pz);
				
				int id = getId(conn, world, px, pz);
				
				PreparedStatement query1 = conn.prepareStatement(PARCEL_OWNER_QUERY);
				query1.setInt(1, id);
				ResultSet ownerSet = query1.executeQuery();
				if (ownerSet.next()) {
					parcel.setOwnerIgnoreSQL(toUUID(ownerSet.getString(1)));
				}
				
				PreparedStatement query2 = conn.prepareStatement(PARCEL_ADDED_QUERY);
				query2.setInt(1, id);
				ResultSet addedSet = query2.executeQuery();
				Map<UUID, Boolean> added = parcel.getAdded().getMap();
				while (addedSet.next()) {
					added.put(toUUID(addedSet.getString(1)), addedSet.getInt(2) != 0);
				}
				
				WorldManager.getWorld(Bukkit.getWorld(world)).get().getParcels().setParcelAt(px, pz, parcel);
				
				UPDATING.remove(PID);
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	*/
	
	public static void setOwner(String world, int px, int pz, UUID owner) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setOwner(conn, getId(conn, world, px, pz), owner.toString());
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
				addPlayer(conn, getId(conn, world, px, pz), player.toString(), allowed);
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while adding a player to a parcel", e);
			}
		});
	}
	
	public static void removePlayer(String world, int px, int pz, UUID player) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removePlayer(conn, getId(conn, world, px, pz), player.toString());
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
	
	private static void setOwner(Connection conn, int id, String owner) throws SQLException {
		PreparedStatement update = conn.prepareStatement(SET_OWNER_UPDATE);
		update.setString(1, owner == null ? null : owner);
		update.setInt(2, id);
		update.executeUpdate();
	}
	
	private static void setBooleanParcelSetting(Connection conn, int id, String query, boolean enabled) throws SQLException {
		PreparedStatement update = conn.prepareStatement(query);
		update.setBoolean(1, enabled);
		update.setInt(2, id);
		update.executeUpdate();
	}
	
	private static void addPlayer(Connection conn, int id, String player, boolean allowed) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_ADD_PLAYER_UPDATE);
		update.setInt(1, id);
		update.setString(2, player);
		update.setBoolean(3, allowed);
		update.executeUpdate();
	}
	
	private static void removePlayer(Connection conn, int id, String player) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_REMOVE_PLAYER_UPDATE);
		update.setInt(1, id);
		update.setString(2, player);
		update.executeUpdate();
	}
	
	private static void removeAllPlayers(Connection conn, String world, int px, int pz) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_CLEAR_PLAYERS_UPDATE);
		update.setInt(1, getId(conn, world, px, pz));
		update.executeUpdate();
	}
	
	private static int getId(Connection conn, String world, int px, int pz) throws SQLException {
		
		PreparedStatement query = conn.prepareStatement(PARCEL_ID_QUERY);
		query.setString(1, world);
		query.setInt(2, px);
		query.setInt(3, pz);
		ResultSet set = query.executeQuery();
		
		if (set.next()) {
			return set.getInt(1);
		} else {
			
			PreparedStatement update = conn.prepareStatement(ADD_PARCEL_UPDATE);
			update.setString(1, world);
			update.setInt(2, px);
			update.setInt(3, pz);
			update.executeUpdate();
			
			return getId(conn, world, px, pz);
		}
	}
	
	static void saveAll(SqlConnector connector) {
		CONNECTOR = connector;
		CONNECTOR.asyncConn(conn -> {
			try {
				SqlUtil.executeUpdate(conn, DROP_TABLES);
				SqlUtil.executeUpdate(conn, CREATE_TABLE_PARCELS, CREATE_TABLE_PARCELS_ADDED);
				
				for (Map.Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String worldName = entry.getKey();
					ParcelWorld world = entry.getValue();
					for (Parcel parcel : world.getParcels().getAll()) {
						int x = parcel.getX();
						int z = parcel.getZ();
						if (parcel.getOwner().isPresent()) {
							setOwner(conn, getId(conn, worldName, x, z), parcel.getOwner().get().toString());
						}
						for (Map.Entry<UUID, Boolean> added : parcel.getAdded().getMap().entrySet()) {
							addPlayer(conn, getId(conn, worldName, x, z), added.getKey().toString(), added.getValue());
						}
					}
				}
			} catch (SQLException e) {
				logSqlExc("[SEVERE] Error occurred while saving all parcel data", e);
			}
		});
	}
	
	public static void ImportFromPlotMe(SqlConnector plotMeConnector, String worldNameFrom, String worldNameTo, MultiRunner errorPrinter) {
		
		ParcelWorld world = WorldManager.getWorld(worldNameTo).orElse(null);
		if (world == null) {
			errorPrinter.add(() -> ParcelsPlugin.log(String.format("  Couldn't find parcel world '%s' while preparing to convert plotme database", worldNameTo)));
			return;
		}
		
		plotMeConnector.asyncConn(plotMeConn -> {
			CONNECTOR.syncConn(parcelsConn -> {
			
				try {			
					
					PreparedStatement plotQuery = plotMeConn.prepareStatement("SELECT `plot_id`, `plotX`, `plotZ`, `ownerID` FROM `plotmeplots` WHERE `world` = ?;");
					plotQuery.setString(1, worldNameFrom);
					ResultSet plotSet = plotQuery.executeQuery();
					if (!plotSet.isBeforeFirst()) {
						ParcelsPlugin.log(String.format("[ERROR] No plotme data found for world by name '%s'", worldNameFrom));
						loadFromDatabase(parcelsConn, worldNameTo, false);
						return;
					}
					
					Statement parcelsSmt = parcelsConn.createStatement();
					parcelsSmt.executeUpdate("DELETE FROM `parcels_added`;");
					parcelsSmt.executeUpdate("DELETE FROM `parcels`;");
					parcelsSmt.close();
					
					while (plotSet.next()) {
						int plotMeId = plotSet.getInt(1);
						int parcelsId = getId(parcelsConn, worldNameTo, plotSet.getInt(2), plotSet.getInt(3));						
						setOwner(parcelsConn, parcelsId, plotSet.getString(4));
						
						// PlotMe's AccessLevel thing appears to always be ALLOWED, where TRUSTED is not in use.
						// Import allowed players
						PreparedStatement allowedQuery = plotMeConn.prepareStatement("SELECT `player` FROM `plotmeallowed` WHERE `plot_id` = ?;");
						allowedQuery.setInt(1, plotMeId);
						ResultSet allowedSet = allowedQuery.executeQuery();						
						while (allowedSet.next()) {
							addPlayer(parcelsConn, parcelsId, allowedSet.getString(1), true);
						}
						allowedSet.close();
						
						// Import denied/banned players
						PreparedStatement deniedQuery = plotMeConn.prepareStatement("SELECT `player` FROM `plotmedenied` WHERE `plot_id` = ?;");
						deniedQuery.setInt(1, plotMeId);
						ResultSet deniedSet = deniedQuery.executeQuery();					
						while (deniedSet.next()) {
							addPlayer(parcelsConn, parcelsId, deniedSet.getString(1), false);
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
	
	private static void logSqlExc(String header, SQLException e) {
		ParcelsPlugin.log(header);
		ParcelsPlugin.log("Error code: " + e.getErrorCode());
		ParcelsPlugin.log("SQL State: " + e.getSQLState());
		ParcelsPlugin.log("Details: " + e.getMessage());
		ParcelsPlugin.log("---------------- Start Stack ----------------");
		e.printStackTrace();
		ParcelsPlugin.log("----------------  End Stack  ----------------");
	}
	
}
/*
class ParcelId {
	
	static ParcelId of(String world, int x, int z) {
		return new ParcelId(world, x, z);
	}
	
	String world;
	int x, z;
	
	private ParcelId(String world, int x, int z) {
		this.world = world;
		this.x = x;
		this.z = z;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ParcelId))
			return false;
		ParcelId otherId = (ParcelId) other;
		return world.equals(otherId.world) && x == otherId.x && z == otherId.z;
	}
	
}
*/
