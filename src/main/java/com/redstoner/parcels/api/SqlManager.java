package com.redstoner.parcels.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.Optional;
import com.redstoner.utils.SqlConnector;

public class SqlManager {
	
	public static SqlConnector CONNECTOR = null;
	
	public static void initialise(SqlConnector connector) {
		ParcelsPlugin.log("INITIALISING SQL MANAGER");
		SqlManager.CONNECTOR = connector;
		try {
			Connection conn = CONNECTOR.getConnection();
			Statement stm = conn.createStatement();
			Statement pstm = conn.createStatement(); // for parcels result set
			
			createTables(stm);
			
			for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
				String name = entry.getKey();
				ParcelWorld world = entry.getValue();
				ResultSet parcels = pstm.executeQuery("SELECT * FROM `parcels` WHERE `world` = '" + name + "'");
				while (parcels.next()) {
					ParcelsPlugin.debug("Found an SQL parcel in world " + name);
					
					int id = parcels.getInt("id");
					int px = parcels.getInt("px");
					int pz = parcels.getInt("pz");
					String owner = parcels.getString("owner");
					
					Optional<Parcel> mParcel = world.getParcelAtID(px, pz);
					if (!mParcel.isPresent()) {
						parcels.deleteRow();
						ParcelsPlugin.debug("Deleted parcel at " + px + "," + pz + " from database");
						continue;
					}
					
					Parcel p = mParcel.get();
					
					ParcelsPlugin.debug("Owner: " + owner);
					if (owner != null)
						p.setOwnerIgnoreSQL(toPlayer(owner));
					
					SqlPlayerList friends = (SqlPlayerList) p.getFriends();
					for (ResultSet allowed = stm.executeQuery("SELECT `player` FROM `parcels_allowed` WHERE `id` = " + id); allowed.next();) {
						friends.addIgnoreSQL(toPlayer(allowed.getString(1)));
					}

					SqlPlayerList enemies = (SqlPlayerList) p.getDenied();
					for (ResultSet denied = stm.executeQuery("SELECT `player` FROM `parcels_denied` WHERE `id` = " + id); denied.next();) {
						enemies.addIgnoreSQL(toPlayer(denied.getString(1)));
					}
				}
				parcels.close();
			}
			ParcelsPlugin.debug("Closing stm");
			pstm.close();
			stm.close();
			CONNECTOR.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static OfflinePlayer toPlayer(String uuid) {
		ParcelsPlugin.debug(uuid);
		UUID result = UUID.fromString(uuid);
		return result == null ? null : Bukkit.getOfflinePlayer(result);
	}
	
	public static void setOwner(int id, String owner) {
		ParcelsPlugin.debug(String.format("UPDATE `parcels` SET `owner` = '%s' WHERE `id` = %s;", owner, id));
		CONNECTOR.executeUpdate(String.format("UPDATE `parcels` SET `owner` = '%s' WHERE `id` = %s;", owner, id));
	}
	
	public static void delOwner(int id) {
		CONNECTOR.executeUpdate(String.format("UPDATE `parcels` SET `owner` = NULL WHERE `id` = %s;", id));
	}
	
	public static void addFriend(int id, String friend) {
		ParcelsPlugin.debug("Adding friend " + String.format("INSERT INTO `parcels_allowed` (`id`, `player`) VALUES (%s, '%s');", id, friend));
		CONNECTOR.executeUpdate(String.format("INSERT IGNORE `parcels_allowed` (`id`, `player`) VALUES (%s, '%s');", id, friend));
	}
	
	public static void addDenied(int id, String denied) {
		CONNECTOR.executeUpdate(String.format("INSERT IGNORE `parcels_denied` (`id`, `player`) VALUES (%s, '%s');", id, denied));
	}
	
	public static void removeFriend(int id, String friend) {
		CONNECTOR.executeUpdate(String.format("DELETE FROM `parcels_allowed` WHERE `id` = %s AND `player` = '%s';", id, friend));
	}
	
	public static void removeDenied(int id, String denied) {
		CONNECTOR.executeUpdate(String.format("DELETE FROM `parcels_denied` WHERE `id` = %s AND `player` = '%s';", id, denied));
	}
	
	public static int getId(String world, int px, int pz) {
		insertParcel(world, px, pz);
		Integer id = CONNECTOR.executeSet(String.format("SELECT `id` FROM `parcels` WHERE `world` = '%s' AND `px` = %s AND `pz` = %s;", world, px, pz),
				set -> {
					try {
						return set.next()? set.getInt(1) : null;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				});
		if (id == null)
			throw new RuntimeException();
		return id;
		
	}
	
	private static void insertParcel(String world, int px, int pz) {
		CONNECTOR.executeUpdate(String.format("INSERT IGNORE `parcels` (`world`, `px`, `pz`) VALUES ('%s', %s, %s);", world, px, pz));
	}
	
	private static void createTables(Statement stm) throws SQLException {
		stm.executeUpdate("CREATE TABLE IF NOT EXISTS `parcels` ("
				+ "`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"
				+ "`world` VARCHAR(32) NOT NULL,"
				+ "`px` INTEGER NOT NULL,"
				+ "`pz` INTEGER NOT NULL,"
				+ "`owner` VARCHAR(36),"
				+ "UNIQUE KEY location(`world`, `px`, `pz`)"
				+ ");"
		);
		
		stm.executeUpdate("CREATE TABLE IF NOT EXISTS `parcels_allowed` ("
				+ "`id` INTEGER NOT NULL,"
				+ "`player` VARCHAR(36) NOT NULL,"
				+ "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
				+ "UNIQUE KEY allowed(`id`, `player`)"
				+ ");"
		);
		
		stm.executeUpdate("CREATE TABLE IF NOT EXISTS `parcels_denied` ("
				+ "`id` INTEGER NOT NULL,"
				+ "`player` VARCHAR(36) NOT NULL,"
				+ "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
				+ "UNIQUE KEY denied(`id`, `player`)"
				+ ");"
		);
	}
	
	private static void dropTables(Statement stm) throws SQLException {
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels_allowed`;");
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels_denied`;");
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels`;");
	}
	
	static void saveAll(SqlConnector connector) {
		try {
			SqlManager.CONNECTOR = connector;
			Connection conn = CONNECTOR.getConnection();
			Statement stm = conn.createStatement();
			dropTables(stm);
			createTables(stm);
			StringBuilder updateOwnersStatement = new StringBuilder("UPDATE `parcels` SET `owner` = CASE `id` ");
			StringBuilder insertAllowedStatement = new StringBuilder("INSERT");
			StringBuilder insertDeniedStatement = new StringBuilder("INSERT");
			for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
				String name = entry.getKey();
				ParcelWorld world = entry.getValue();
				for (Parcel parcel : world.getParcels().getAll()) {
					Optional<OfflinePlayer> owner = parcel.getOwner();
					List<String> friends = parcel.getFriends().stream().map(player -> player.getUniqueId().toString()).collect(Collectors.toList());
					List<String> denied = parcel.getDenied().stream().map(player -> player.getUniqueId().toString()).collect(Collectors.toList());
					if (owner.isPresent() || friends.size() > 0 || denied.size() > 0) {
						int id = getId(name, parcel.getX(), parcel.getZ());
						updateOwnersStatement.append(String.format("WHEN %s THEN '%s' ", id, owner.get().getUniqueId().toString()));
						for (String uuid : friends)
							insertAllowedStatement.append(String.format(" INTO `parcels_allowed` (`id`, `player`) VALUES (%s, '%s')", id, uuid));
						for (String uuid : denied)
							insertDeniedStatement.append(String.format(" INTO `parcels_denied` (`id`, `player`) VALUES (%s, '%s')", id, uuid));
					}
					
				}
			}
			updateOwnersStatement.append("END;");
			insertAllowedStatement.append(";");
			insertDeniedStatement.append(";");
			ParcelsPlugin.debug("INSERTING OWNERS: " + updateOwnersStatement.toString());
			ParcelsPlugin.debug("INSERTING FRIENDS: " + insertAllowedStatement.toString());
			ParcelsPlugin.debug("INSERTING DENIED: " + insertDeniedStatement.toString());
			stm.executeUpdate(updateOwnersStatement.toString());
			stm.executeUpdate(insertAllowedStatement.toString());
			stm.executeUpdate(insertDeniedStatement.toString());
			CONNECTOR.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
