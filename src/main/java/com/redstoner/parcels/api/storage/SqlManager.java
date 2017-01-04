package com.redstoner.parcels.api.storage;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.GlobalTrusted;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.ErrorPrinter;
import com.redstoner.utils.UUIDUtil;
import com.redstoner.utils.sql.SQLConnector;
import com.redstoner.utils.sql.control.ExceptionHandler;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

import static com.redstoner.utils.UUIDUtil.*;

public class SqlManager {
    private static final String GET_PARCELS;
    private static final String GET_PARCELS_ADDED;
    private static final String GET_PARCEL_ID;
    private static final String GET_GLOBAL_ADDED;
    private static final String CREATE_PARCELS_TABLE;
    private static final String CREATE_PARCELS_ADDED_TABLE;
    private static final String CREATE_GLOBAL_ADDED_TABLE;
    private static final String SET_ALLOWINTERACT_INPUTS;
    private static final String SET_ALLOWINTERACT_INVENTORY;
    private static final String SET_OWNER;
    private static final String ADD_PLAYER;
    private static final String REMOVE_PLAYER;
    private static final String CLEAR_PLAYERS;
    private static final String GLOBAL_ADD_PLAYER;
    private static final String GLOBAL_REMOVE_PLAYER;
    private static final String GLOBAL_CLEAR_PLAYERS;
    private static final String ADD_PARCEL;
    private static final String DELETE_PARCEL;

    static {
        GET_PARCELS = "SELECT `id`, `px`, `pz`, hex(`owner`), `allow_interact_inputs`, `allow_interact_inventory` FROM `parcels` WHERE `world` = ?;";
        GET_PARCELS_ADDED = "SELECT hex(`player`), `allowed` FROM `parcels_added` WHERE `id` = ?;";
        GET_PARCEL_ID = "SELECT `id` FROM `parcels` WHERE `world` = ? AND `px` = ? AND `pz` = ?;";
        GET_GLOBAL_ADDED = "SELECT hex(`player`), hex(`added`), `allowed` FROM `global_added`;";
        CREATE_PARCELS_TABLE = "CREATE TABLE IF NOT EXISTS `parcels` ("
                + "`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`world` VARCHAR(32) NOT NULL,"
                + "`px` INTEGER NOT NULL,"
                + "`pz` INTEGER NOT NULL,"
                + "`owner` CHAR(16),"
                + "`allow_interact_inputs` TINYINT(1) NOT NULL DEFAULT 0,"
                + "`allow_interact_inventory` TINYINT(1) NOT NULL DEFAULT 0,"
                + "UNIQUE KEY location(`world`, `px`, `pz`)"
                + ");";
        CREATE_PARCELS_ADDED_TABLE = "CREATE TABLE IF NOT EXISTS `parcels_added` ("
                + "`id` INTEGER NOT NULL,"
                + "`player` CHAR(16) NOT NULL,"
                + "`allowed` TINYINT(1) NOT NULL,"
                + "FOREIGN KEY (`id`) REFERENCES `parcels`(`id`) ON DELETE CASCADE,"
                + "UNIQUE KEY added(`id`, `player`)"
                + ");";
        CREATE_GLOBAL_ADDED_TABLE = "CREATE TABLE IF NOT EXISTS `global_added` ("
                + "`player` CHAR(16) NOT NULL,"
                + "`added` CHAR(16) NOT NULL,"
                + "`allowed` TINYINT(1) NOT NULL,"
                + "UNIQUE KEY pair(`player`, `added`)"
                + ");";
        SET_ALLOWINTERACT_INPUTS = "UPDATE `parcels` SET `allow_interact_inputs` = ? WHERE `id` = ?;";
        SET_ALLOWINTERACT_INVENTORY = "UPDATE `parcels` SET `allow_interact_inventory` = ? WHERE `id` = ?;";
        SET_OWNER = "UPDATE `parcels` SET `owner` = unhex(?) WHERE `id` = ?;";
        ADD_PLAYER = "REPLACE `parcels_added` (`id`, `player`, `allowed`) VALUES (?, unhex(?), ?);";
        REMOVE_PLAYER = "DELETE FROM `parcels_added` WHERE `id` = ? AND `player` = unhex(?);";
        CLEAR_PLAYERS = "DELETE FROM `parcels_added` WHERE `id` = ?;";
        GLOBAL_ADD_PLAYER = "REPLACE `global_added` (`player`, `added`, `allowed`) VALUES (unhex(?), unhex(?), ?);";
        GLOBAL_REMOVE_PLAYER = "DELETE FROM `global_added` WHERE `player` = unhex(?) AND `added` = unhex(?);";
        GLOBAL_CLEAR_PLAYERS = "DELETE FROM `global_added` WHERE `player` = unhex(?);";
        DELETE_PARCEL = "DELETE FROM `parcels` WHERE `id` = ? LIMIT 1;";
        ADD_PARCEL = "INSERT IGNORE `parcels` (`world`, `px`, `pz`) VALUES (?, ?, ?);";
    }

    public static SQLConnector CONNECTOR = null;

    private static void handleException(String header, Exception ex) {
        ExceptionHandler.log(ParcelsPlugin.getInstance()::error, header).handle(ex);
    }

    public static void initialise(SQLConnector parcelsConnector, boolean load) {
        if (CONNECTOR == null) {
            CONNECTOR = parcelsConnector;
        }

        CONNECTOR.asyncConn(conn -> {
            try (Statement sm = conn.createStatement()) {
                sm.execute(CREATE_PARCELS_TABLE);
                sm.execute(CREATE_PARCELS_ADDED_TABLE);
                sm.execute(CREATE_GLOBAL_ADDED_TABLE);
            }
            if (load) {
                loadAllFromDatabase(conn);
            }
        });
    }

    private static void loadAllFromDatabase(Connection conn) {
        for (String worldName : WorldManager.getWorlds().keySet()) {
            loadFromDatabase(conn, worldName);
        }

        loadGlobalAddedFromDatabase(conn);
    }

    private static void loadFromDatabase(Connection conn, String worldName) {
        ParcelWorld world = WorldManager.getWorld(worldName).orElse(null);
        if (world == null) {
            ParcelsPlugin.getInstance().debug(String.format("Couldn't find ParcelWorld instance for world by name '%s'", worldName));
            return;
        }

        try (PreparedStatement sm = conn.prepareStatement(GET_PARCELS)) {
            sm.setString(1, worldName);
            try (ResultSet row = sm.executeQuery()) {
                while (row.next()) try {
                    int id = row.getInt(1);
                    int px = row.getInt(2);
                    int pz = row.getInt(3);
                    Parcel parcel = world.getParcelByID(px, pz);

                    if (parcel == null) {
                        try (PreparedStatement update = conn.prepareStatement(DELETE_PARCEL)) {
                            update.setInt(1, id);
                            update.executeUpdate();
                        }
                        return;
                    }

                    parcel.setUniqueId(id);

                    String owner = row.getString(4);
                    if (owner != null) try {
                        parcel.setOwnerIgnoreSQL(UUIDFromString(owner));
                    } catch (IllegalArgumentException ignored) {
                    }

                    parcel.getSettings().setAllowsInteractInputsIgnoreSQL(row.getInt(5) != 0);
                    parcel.getSettings().setAllowsInteractInventoryIgnoreSQL(row.getInt(6) != 0);

                    Map<UUID, Boolean> addedPlayers = parcel.getAdded().getMap();
                    try (PreparedStatement addedQuery = conn.prepareStatement(GET_PARCELS_ADDED)) {
                        addedQuery.setInt(1, id);
                        try (ResultSet added = addedQuery.executeQuery()) {
                            while (added.next()) try {
                                addedPlayers.put(UUIDFromString(added.getString(1)), added.getInt(2) != 0);
                            } catch (Exception ex) {
                                handleException("loading an adder player for a parcel in " + worldName + " from database", ex);
                            }
                        }
                    }
                } catch (Exception ex) {
                    handleException("loading a parcel for " + worldName + " from database", ex);
                }
            }
        } catch (SQLException ex) {
            handleException("loading world " + worldName + " from database", ex);
        }
    }

    private static void loadGlobalAddedFromDatabase(Connection conn) {
        /*
        try (PreparedStatement psm = conn.prepareStatement()) {
            try (ResultSet row = psm.executeQuery()) {
                while (row.next()) try {

                } catch (Exception ex) {
                    handleException("", ex);
                }
            }

        } catch (Exception ex) {
            handleException("", ex);
        }
        */

        try (Statement sm = conn.createStatement();
             ResultSet row = sm.executeQuery(GET_GLOBAL_ADDED)) {
            while (row.next()) try {
                GlobalTrusted.addPlayerIgnoreSQL(UUIDUtil.UUIDFromString(row.getString(1)),
                        UUIDUtil.UUIDFromString(row.getString(2)), row.getInt(3) != 0);
            } catch (Exception ex) {
                handleException("loading a globally added player from database", ex);
            }
        } catch (Exception ex) {
            handleException("loading all globally added players from database", ex);
        }
    }

    public static void setOwner(Parcel parcel, UUID owner) {
        CONNECTOR.asyncConn(conn -> setOwner(conn, getId(conn, parcel), owner));
    }

    private static void setOwner(Connection conn, int id, UUID owner) {
        /*
        try (PreparedStatement psm = conn.prepareStatement()) {

            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("", ex);
        }
        */
        try (PreparedStatement psm = conn.prepareStatement(SET_OWNER)) {
            psm.setString(1, UUIDToString(owner));
            psm.setInt(2, id);
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("", ex);
        }
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

    public static void setAllowInteractInputs(Parcel parcel, boolean enabled) {
        CONNECTOR.asyncConn(conn -> setBooleanParcelSetting(conn, SET_ALLOWINTERACT_INPUTS, parcel, enabled));
    }

    public static void setAllowInteractInventory(Parcel parcel, boolean enabled) {
        CONNECTOR.asyncConn(conn -> setBooleanParcelSetting(conn, SET_ALLOWINTERACT_INVENTORY, parcel, enabled));
    }

    private static void setBooleanParcelSetting(Connection conn, String update, Parcel parcel, boolean enabled) {
        try (PreparedStatement psm = conn.prepareStatement(update)) {
            psm.setBoolean(1, enabled);
            psm.setInt(2, getId(conn, parcel));
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("setting an allow-interact flag for a parcel", ex);
        }
    }

    public static void addPlayer(Parcel parcel, UUID player, boolean allowed) {
        CONNECTOR.asyncConn(conn -> addPlayer(conn, parcel, player, allowed));
    }

    private static void addPlayer(Connection conn, Parcel parcel, UUID player, boolean allowed) {
        try (PreparedStatement psm = conn.prepareStatement(ADD_PLAYER)) {
            psm.setInt(1, getId(conn, parcel));
            psm.setString(2, UUIDToString(player));
            psm.setBoolean(3, allowed);
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("adding a player to a parcel", ex);
        }
    }

    public static void removePlayer(Parcel parcel, UUID player) {
        CONNECTOR.asyncConn(conn -> removePlayer(conn, parcel, player));
    }

    private static void removePlayer(Connection conn, Parcel parcel, UUID player) {
        try (PreparedStatement psm = conn.prepareStatement(REMOVE_PLAYER)) {
            psm.setInt(1, getId(conn, parcel));
            psm.setString(2, UUIDToString(player));
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("removing a player from a parcel", ex);
        }
    }

    public static void removeAllPlayers(Parcel parcel) {
        CONNECTOR.asyncConn(conn -> removeAllPlayers(conn, parcel));
    }

    private static void removeAllPlayers(Connection conn, Parcel parcel) {
        try (PreparedStatement psm = conn.prepareStatement(CLEAR_PLAYERS)) {
            psm.setInt(1, getId(conn, parcel));
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("removing all players from a parcel", ex);
        }
    }

    public static void addGlobalPlayer(UUID player, UUID added, boolean allowed) {
        CONNECTOR.asyncConn(conn -> addGlobalPlayer(conn, player, added, allowed));
    }

    private static void addGlobalPlayer(Connection conn, UUID player, UUID added, boolean allowed) {
        try (PreparedStatement psm = conn.prepareStatement(GLOBAL_ADD_PLAYER)) {
            psm.setString(1, UUIDToString(player));
            psm.setString(2, UUIDToString(added));
            psm.setBoolean(3, allowed);
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("adding a global player", ex);
        }
    }

    public static void removeGlobalPlayer(UUID player, UUID removed) {
        CONNECTOR.asyncConn(conn -> removeGlobalPlayer(conn, player, removed));
    }

    private static void removeGlobalPlayer(Connection conn, UUID player, UUID removed) {
        try (PreparedStatement psm = conn.prepareStatement(GLOBAL_REMOVE_PLAYER)) {
            psm.setString(1, UUIDToString(player));
            psm.setString(2, UUIDToString(removed));
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("removing a global player", ex);
        }
    }

    public static void removeAllGlobalPlayers(UUID player) {
        CONNECTOR.asyncConn(conn -> removeAllGlobalPlayers(conn, player));
    }

    private static void removeAllGlobalPlayers(Connection conn, UUID player) {
        try (PreparedStatement psm = conn.prepareStatement(GLOBAL_CLEAR_PLAYERS)) {
            psm.setString(1, UUIDToString(player));
            psm.executeUpdate();
        } catch (SQLException ex) {
            handleException("removing all globally added players for a player", ex);
        }
    }

    private static int getId(Connection conn, Parcel parcel) throws SQLException {
        if (parcel.getUniqueId() > 0) {
            return parcel.getUniqueId();
        }

        String world = parcel.getWorld().getName();
        int x = parcel.getX();
        int z = parcel.getZ();
        try (PreparedStatement psm = conn.prepareStatement(GET_PARCEL_ID)) {
            psm.setString(1, world);
            psm.setInt(2, x);
            psm.setInt(3, z);
            try (ResultSet set = psm.executeQuery()) {
                if (set.next()) {
                    return set.getInt(1);
                }
            }
        }

        try (PreparedStatement psm = conn.prepareStatement(ADD_PARCEL, Statement.RETURN_GENERATED_KEYS)) {
            psm.setString(1, world);
            psm.setInt(2, x);
            psm.setInt(3, z);
            psm.executeUpdate();
            try (ResultSet generated = psm.getGeneratedKeys()) {
                if (generated.next()) {
                    int id = generated.getInt(1);
                    parcel.setUniqueId(id);
                    return id;
                }
                throw new RuntimeException("Failed to get the generated key");
            }
        }
    }

    static void saveAll(SQLConnector sqlConnector) {
        CONNECTOR = sqlConnector;
        for (ParcelWorld world : WorldManager.getWorlds().values()) {
            Parcel[] parcels = world.getParcels().getAll();
            for (Parcel parcel : parcels) {
                setOwner(parcel, parcel.getOwner().orElse(null));
                removeAllPlayers(parcel);
                for (Map.Entry<UUID, Boolean> added : parcel.getAdded().getMap().entrySet()) {
                    addPlayer(parcel, added.getKey(), added.getValue());
                }
            }
        }
    }

    public static void importFromPlotMe(SQLConnector plotMeConnector, String worldNameFrom, String worldNameTo, ErrorPrinter errorPrinter) {
        ParcelWorld world = WorldManager.getWorld(worldNameTo).orElse(null);
        if (world == null) {
            errorPrinter.add(() -> ParcelsPlugin.getInstance().error(String.format("  Couldn't find parcel world '%s' while preparing to convert plotme database", worldNameTo)));
            return;
        }

        initialise(CONNECTOR, false);
        CONNECTOR.asyncConn(parcelsConn -> {
            loadFromDatabase(parcelsConn, worldNameTo);
            plotMeConnector.syncConn(plotMeConn -> {
                PlotMeTableSetup setup = PlotMeTableSetup.getSetup(plotMeConn);

                if (setup == null) {
                    ParcelsPlugin.getInstance().error("Didn't find PlotMe's MySQL tables to import from");
                    loadFromDatabase(parcelsConn, worldNameTo);
                    return;
                }

                ParcelsPlugin.getInstance().info("Plotme Table setup found: " + setup.name());

                try (ResultSet plotSet = setup.getPlots(plotMeConn, worldNameFrom)) {
                    if (!plotSet.isBeforeFirst()) {
                        ParcelsPlugin.getInstance().error(String.format("No PlotMe data found for world by name '%s' (but the table exists)", worldNameFrom));
                        loadFromDatabase(parcelsConn, worldNameTo);
                        return;
                    }

                    while (plotSet.next()) try {
                        int px = plotSet.getInt(1) - 1;
                        int pz = plotSet.getInt(2) - 1;
                        Parcel parcel = world.getParcelByID(px, pz);
                        if (parcel == null) {
                            continue;
                        }

                        UUID owner = UUIDFromBytes(setup.getBytesFromIndex(plotSet, 3));
                        parcel.setOwner(owner);

                        // Import allowed players
                        try (ResultSet allowedSet = setup.getAllowed(plotMeConn, plotSet)) {
                            UUID player;
                            while (allowedSet.next()) try {
                                player = UUIDFromBytes(setup.getBytesFromIndex(allowedSet, 1));
                                parcel.getAdded().add(player, true);
                            } catch (Exception ex) {
                                ExceptionHandler.log(errorPrinter::add, "reading an added player from plotme").handle(ex);
                            }
                        }

                        try (ResultSet bannedSet = setup.getAllowed(plotMeConn, plotSet)) {
                            UUID player;
                            while (bannedSet.next()) try {
                                player = UUIDFromBytes(setup.getBytesFromIndex(bannedSet, 1));
                                parcel.getAdded().add(player, false);
                            } catch (Exception ex) {
                                ExceptionHandler.log(errorPrinter::add, "reading a banned player from plotme").handle(ex);
                            }
                        }
                    } catch (Exception ex) {
                        ExceptionHandler.log(errorPrinter::add, "reading a plot from plotme");
                    }

                } catch (Exception ex) {
                    ExceptionHandler.log(errorPrinter::add, "importing from PlotMe database").handle(ex);
                }

                // Load imported world data
                ParcelsPlugin.getInstance().info("Finished PlotMe import for Parcels world " + worldNameTo);
            });
        });

    }

}


