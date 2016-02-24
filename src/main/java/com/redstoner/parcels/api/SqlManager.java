package com.redstoner.parcels.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.redstoner.parcels.ParcelsPlugin;
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
				+ "`allow_interact_inputs` TINYINT(1) NOT NULL DEFAULT 0, "
				+ "`allow_interact_inventory` TINYINT(1) NOT NULL DEFAULT 0, "
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
	
	public static void initialise(SqlConnector connector) {
		CONNECTOR = connector;
		CONNECTOR.asyncConn(conn -> {
			SqlUtil.executeUpdate(conn, CREATE_TABLE_PARCELS, CREATE_TABLE_PARCELS_ADDED);
			
			try {
				
				for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String worldName = entry.getKey();
					ParcelWorld world = entry.getValue();
					
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
				}
			} catch (SQLException e) {
				ParcelsPlugin.log("[ERROR] While querying the MySQL database:");
				e.printStackTrace();
			}// catch (InterruptedException e) {
				//e.printStackTrace();
			//}
			
		});
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
				setOwner(conn, world, px, pz, owner);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	/* Setting template
	public static void setAllowInteract(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, world, px, pz, SET_ALLOW_INTERACT_, enabled);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	*/
	
	public static void setAllowInteractInputs(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, world, px, pz, SET_ALLOW_INTERACT_INPUTS, enabled);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void setAllowInteractInventory(String world, int px, int pz, boolean enabled) {
		CONNECTOR.asyncConn(conn -> {
			try {
				setBooleanParcelSetting(conn, world, px, pz, SET_ALLOW_INTERACT_INVENTORY, enabled);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void addPlayer(String world, int px, int pz, UUID player, boolean allowed) {
		CONNECTOR.asyncConn(conn -> {
			try {
				addPlayer(conn, world, px, pz, player, allowed);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void removePlayer(String world, int px, int pz, UUID player) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removePlayer(conn, world, px, pz, player);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void removeAllPlayers(String world, int px, int pz) {
		CONNECTOR.asyncConn(conn -> {
			try {
				removeAllPlayers(conn, world, px, pz);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	private static void setOwner(Connection conn, String world, int px, int pz, UUID owner) throws SQLException {
		PreparedStatement update = conn.prepareStatement(SET_OWNER_UPDATE);
		update.setString(1, owner == null ? null : owner.toString());
		update.setInt(2, getId(conn, world, px, pz));
		update.executeUpdate();
	}
	
	private static void setBooleanParcelSetting(Connection conn, String world, int px, int pz, String query, boolean enabled) throws SQLException {
		PreparedStatement update = conn.prepareStatement(query);
		update.setBoolean(1, enabled);
		update.setInt(2, getId(conn, world, px, pz));
		update.executeUpdate();
	}
	
	private static void addPlayer(Connection conn, String world, int px, int pz, UUID player, boolean allowed) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_ADD_PLAYER_UPDATE);
		update.setInt(1, getId(conn, world, px, pz));
		update.setString(2, player.toString());
		update.setBoolean(3, allowed);
		update.executeUpdate();
	}
	
	private static void removePlayer(Connection conn, String world, int px, int pz, UUID player) throws SQLException {
		PreparedStatement update = conn.prepareStatement(PARCEL_REMOVE_PLAYER_UPDATE);
		update.setInt(1, getId(conn, world, px, pz));
		update.setString(2, player.toString());
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
				
				for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String worldName = entry.getKey();
					ParcelWorld world = entry.getValue();
					for (Parcel parcel : world.getParcels().getAll()) {
						int x = parcel.getX();
						int z = parcel.getZ();
						if (parcel.getOwner().isPresent()) {
							setOwner(conn, worldName, x, z, parcel.getOwner().get());
						}
						for (Entry<UUID, Boolean> added : parcel.getAdded().getMap().entrySet()) {
							addPlayer(conn, worldName, x, z, added.getKey(), added.getValue());
						}
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
}

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
