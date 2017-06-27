package com.redstoner.parcels.api.storage;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.sql.control.ExceptionHandler;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

enum PlotMeTableSetup {

    FIRST {
        @Override
        public String getPlotMeTableName() {
            return "plotmecore_plots";
        }

        @Override
        public String getPlotQuery() {
            return "SELECT `plotX`, `plotZ`, `ownerID`, `plot_id` FROM `plotmecore_plots` WHERE `world` = ?;";
        }

        @Override
        public boolean usesPlotId() {
            return true;
        }

        @Override
        public String getAllowedQuery() {
            return "SELECT `player` FROM `plotmecore_allowed` WHERE `plot_id` = ?;";
        }

        @Override
        public String getBannedQuery() {
            return "SELECT `player` FROM `plotmecore_denied` WHERE `plot_id` = ?;";
        }

        @Override
        public UUID getUUIDFromIndex(ResultSet set, int index) throws Exception {
            return UUID.fromString(set.getString(index));
        }

    },

    SECOND {
        @Override
        public String getPlotMeTableName() {
            return "plotmeplots";
        }

        @Override
        public String getPlotQuery() {
            return "SELECT `idX`, `idZ`, `ownerId` FROM `plotmeplots` WHERE `world` = ?;";
        }

        @Override
        public String getAllowedQuery() {
            return "SELECT `playerid` FROM `plotmeallowed` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;";
        }

        @Override
        public String getBannedQuery() {
            return "SELECT `playerid` FROM `plotmedenied` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;";
        }
    },

    THIRD {
        @Override
        public String getPlotMeTableName() {
            return "plotmePlots";
        }

        @Override
        public String getPlotQuery() {
            return "SELECT `idX`, `idZ`, `ownerid` FROM `plotmePlots` WHERE `world` = ?;";
        }

        @Override
        public String getAllowedQuery() {
            return "SELECT `playerid` FROM `plotmeAllowed` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;";
        }

        @Override
        public String getBannedQuery() {
            return "SELECT `playerid` FROM `plotmeDenied` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;";
        }
    };

    public abstract String getPlotMeTableName();

    public abstract String getPlotQuery();

    public boolean usesPlotId() {
        return false;
    }

    public abstract String getAllowedQuery();

    public abstract String getBannedQuery();

    public UUID getUUIDFromIndex(ResultSet set, int index) throws Exception {
        byte[] bytes = set.getBytes(index);
        if (bytes == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return new UUID(buf.getLong(), buf.getLong());
    }

    public Iterable<Plot> getPlots(Connection conn, String worldName) throws Exception {
        // "SELECT `idX`, `idZ`, hex(`ownerid`) FROM `plotmePlots` WHERE `world` = ?;"
        try (PreparedStatement psm = conn.prepareStatement(getPlotQuery())) {
            psm.setString(1, worldName);
            try (ResultSet set = psm.executeQuery()) {
                List<Plot> result = new LinkedList<>();
                while (set.next()) try {
                    UUID owner = getUUIDFromIndex(set, 3);
                    if (owner == null) {
                        continue;
                    }
                    int idX = set.getInt(1);
                    int idZ = set.getInt(2);
                    int plotId = usesPlotId() ? set.getInt(4) : -1;
                    result.add(new Plot(plotId, idX, idZ, worldName, owner));
                } catch (Exception ex) {
                    ExceptionHandler.log(ParcelsPlugin.getInstance()::error, "reading a plot from plotme database").handle(ex);
                }
                return result;
            }
        }
    }

    public Iterable<UUID> getAdded(Connection conn, Plot plot, boolean allowed) throws Exception {
        try (PreparedStatement psm = conn.prepareStatement(allowed ? getAllowedQuery() : getBannedQuery())) {
            if (usesPlotId()) {
                psm.setInt(1, plot.plotId);
            } else {
                psm.setString(1, plot.worldName);
                psm.setInt(2, plot.idX);
                psm.setInt(3, plot.idZ);
            }

            try (ResultSet set = psm.executeQuery()) {
                List<UUID> result = new LinkedList<>();
                while (set.next()) try {
                    result.add(getUUIDFromIndex(set, 1));
                } catch (Exception ex) {
                    ExceptionHandler.log(ParcelsPlugin.getInstance()::error, "reading a" + (allowed ? "n allowed" : " banned") + " player from plotme database").handle(ex);
                }
                return result;
            }
        }
    }

    public boolean isCase(Connection conn) {
        try (Statement sm = conn.createStatement()) {
            sm.execute("SELECT 1 FROM `" + getPlotMeTableName() + "` LIMIT 1;");
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    public static PlotMeTableSetup getSetup(Connection conn) {
        for (PlotMeTableSetup setup : values()) {
            if (setup.isCase(conn)) {
                return setup;
            }
        }
        return null;
    }

    static class Plot {
        final int plotId;
        final int idX, idZ;
        final String worldName;
        final UUID owner;

        public Plot(int plotId, int idX, int idZ, String worldName, UUID owner) {
            this.plotId = plotId;
            this.idX = idX;
            this.idZ = idZ;
            this.worldName = worldName;
            this.owner = owner;
        }

    }

}