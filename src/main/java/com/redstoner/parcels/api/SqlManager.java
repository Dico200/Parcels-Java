package com.redstoner.parcels.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.utils.Optional;
import com.redstoner.utils.SqlConnector;

public class SqlManager {
	
	public static SqlConnector CONNECTOR = null;
	
	public static void initialise(SqlConnector connector) {
		runAsync(() -> {
			try {
				Thread.sleep(4000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			ParcelsPlugin.debug("Retrieving SQL data");
			SqlManager.CONNECTOR = connector;
			try {
				Connection conn = CONNECTOR.getConnection();
				Statement stm = conn.createStatement();
				Statement pstm = conn.createStatement(); // for parcels result set
				
				createTables(stm);
				
				for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String name = entry.getKey();
					ParcelWorld world = entry.getValue();
					ResultSet parcels = pstm.executeQuery("SELECT `id`, `px`, `pz`, `owner` FROM `parcels` WHERE `world` = '" + name + "'");
					while (parcels.next()) {
						
						int px = parcels.getInt(2);
						int pz = parcels.getInt(3);
						String owner = parcels.getString(4);
						
						Optional<Parcel> mParcel = world.getParcelAtID(px, pz);
						if (!mParcel.isPresent()) {
							parcels.deleteRow();
							ParcelsPlugin.debug("Deleted parcel at " + px + "," + pz + " from database");
							continue;
						}
						
						Parcel p = mParcel.get();
						
						if (owner != null)
							p.setOwnerIgnoreSQL(toPlayer(owner));
						
						int id = parcels.getInt(1);
						SqlPlayerMap<Boolean> addedPlayers = (SqlPlayerMap<Boolean>) p.getAdded();
						ResultSet added = stm.executeQuery(String.format("SELECT `player`, `allowed` FROM `parcels_added` WHERE `id` = %s;", id));
						while (added.next()) {
							addedPlayers.addIgnoreSQL(toPlayer(added.getString(1)), added.getInt(2) != 0);
						}
						added.close();
					}
					parcels.close();
				}
				pstm.close();
				stm.close();
				CONNECTOR.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	private static OfflinePlayer toPlayer(String uuid) {
		return Bukkit.getOfflinePlayer(UUID.fromString(uuid));
	}
	
	public static void setOwner(String world, int px, int pz, OfflinePlayer owner) {
		runAsync(() -> {
			CONNECTOR.executeUpdate(String.format("UPDATE `parcels` SET `owner` = '%s' WHERE `id` = %s;", 
					owner.getUniqueId().toString(), getId(world, px, pz)));
		});
	}
	
	public static void delOwner(String world, int px, int pz) {
		runAsync(() -> {
			CONNECTOR.executeUpdate(String.format("UPDATE `parcels` SET `owner` = NULL WHERE `id` = %s;", 
					getId(world, px, pz)));
		});
	}
	
	public static void addPlayer(String world, int px, int pz, OfflinePlayer player, boolean allowed) {
		runAsync(() -> {
			CONNECTOR.executeUpdate(String.format("REPLACE `parcels_added` (`id`, `player`, `allowed`) VALUES (%s, '%s', %s);", 
					getId(world, px, pz), player.getUniqueId().toString(), allowed? 1:0));
		});
	}
	
	public static void removePlayer(String world, int px, int pz, OfflinePlayer player) {
		runAsync(() -> {
			CONNECTOR.executeUpdate(String.format("DELETE FROM `parcels_added` WHERE `id` = %s AND `player` = '%s';", 
					getId(world, px, pz), player.getUniqueId().toString()));
		});
	}
	
	public static void removeAllPlayers(String world, int px, int pz) {
		runAsync(() -> {
			CONNECTOR.executeUpdate(String.format("DELETE FROM `parcels_added` WHERE `id` = %s;",
					getId(world, px, pz)));
		});
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
		
		stm.executeUpdate("CREATE TABLE IF NOT EXISTS `parcels_added` ("
				+ "`id` INTEGER NOT NULL,"
				+ "`player` VARCHAR(36) NOT NULL,"
				+ "`allowed` TINYINT(1) NOT NULL DEFAULT 1,"
				+ "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
				+ "UNIQUE KEY added(`id`, `player`)"
				+ ");"
		);
	}
	
	private static void dropTables(Statement stm) throws SQLException {
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels_allowed`;");
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels_denied`;");
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels_added`;");
		stm.executeUpdate("DROP TABLE IF EXISTS `parcels`;");
	}
	
	static void saveAll(SqlConnector connector) {
		runAsync(() -> {
			try {
				SqlManager.CONNECTOR = connector;
				Connection conn = CONNECTOR.getConnection();
				Statement stm = conn.createStatement();
				dropTables(stm);
				createTables(stm);
				StringBuilder updateOwnersStatement = new StringBuilder("UPDATE `parcels` SET `owner` = CASE `id` ");
				StringBuilder insertAddedStatement = new StringBuilder("INSERT");
				for (Entry<String, ParcelWorld> entry : WorldManager.getWorlds().entrySet()) {
					String name = entry.getKey();
					ParcelWorld world = entry.getValue();
					for (Parcel parcel : world.getParcels().getAll()) {
						Optional<OfflinePlayer> owner = parcel.getOwner();
						Map<OfflinePlayer, Boolean> added = parcel.getAdded().getMap();
						if (owner.isPresent() || added.size() > 0) {
							int id = getId(name, parcel.getX(), parcel.getZ());
							updateOwnersStatement.append(String.format("WHEN %s THEN '%s' ", id, owner.get().getUniqueId().toString()));
							added.forEach((player, banned) -> {
								String uuid = player.getUniqueId().toString();
								insertAddedStatement.append(String.format(" INTO `parcels_added` (`id`, `player`, `allowed`) VALUES (%s, '%s', %s)", id, uuid, banned));
							});
						}
						
					}
				}
				updateOwnersStatement.append("END;");
				insertAddedStatement.append(";");
				ParcelsPlugin.debug("INSERTING OWNERS: " + updateOwnersStatement.toString());
				ParcelsPlugin.debug("INSERTING ADDED PLAYERS: " + insertAddedStatement.toString());
				stm.executeUpdate(updateOwnersStatement.toString());
				stm.executeUpdate(insertAddedStatement.toString());
				CONNECTOR.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}
	
	private static void runAsync(Runnable toRun) {
		new Thread(toRun).start();
	}
}
