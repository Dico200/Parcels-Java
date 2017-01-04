package com.redstoner.parcels.api.storage;

import java.sql.*;
import java.util.UUID;

import static com.redstoner.utils.UUIDUtil.UUIDToBytes;

enum PlotMeTableSetup {

    FIRST {
        @Override
        public String getPlotMeTableName() {
            return "plotmecore_plots";
        }

        @Override
        public ResultSet getPlots(Connection conn, String worldName) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `plotX`, `plotZ`, `ownerID`, `plot_id` FROM `plotmecore_plots` WHERE `world` = ?;");
            try {
                query.setString(1, worldName);
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `player` FROM `plotmecore_allowed` WHERE `plot_id` = ?;");
            try {
                query.setInt(1, plotSet.getInt(4));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getBanned(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `player` FROM `plotmecore_denied` WHERE `plot_id` = ?;");
            try {
                query.setInt(1, plotSet.getInt(4));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException {
            String uuid = set.getString(index);
            return UUIDToBytes(UUID.fromString(uuid));
        }

    },

    SECOND {
        @Override
        public String getPlotMeTableName() {
            return "plotmeplots";
        }

        @Override
        public ResultSet getPlots(Connection conn, String worldName) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `idX`, `idZ`, `ownerId`, `world` FROM `plotmeplots` WHERE `world` = ?;");
            try {
                query.setString(1, worldName);
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmeallowed` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
            try {
                query.setString(1, plotSet.getString(4));
                query.setInt(2, plotSet.getInt(1));
                query.setInt(3, plotSet.getInt(2));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getBanned(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmedenied` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
            try {
                query.setString(1, plotSet.getString(4));
                query.setInt(2, plotSet.getInt(1));
                query.setInt(3, plotSet.getInt(2));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException {
            return set.getBytes(index);
        }

    },

    THIRD {
        @Override
        public String getPlotMeTableName() {
            return "plotmePlots";
        }

        @Override
        public ResultSet getPlots(Connection conn, String worldName) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `idX`, `idZ`, `ownerid`, `world` FROM `plotmePlots` WHERE `world` = ?;");
            try {
                query.setString(1, worldName);
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmeAllowed` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
            try  {
                query.setString(1, plotSet.getString(4));
                query.setInt(2, plotSet.getInt(1));
                query.setInt(3, plotSet.getInt(2));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public ResultSet getBanned(Connection conn, ResultSet plotSet) throws SQLException {
            PreparedStatement query = conn.prepareStatement("SELECT `playerid` FROM `plotmeDenied` WHERE `world` = ? AND `idX` = ? AND `idZ` = ?;");
            try {
                query.setString(1, plotSet.getString(4));
                query.setInt(2, plotSet.getInt(1));
                query.setInt(3, plotSet.getInt(2));
                return query.executeQuery();
            } catch (Exception ex) {
                try {
                    query.close();
                } catch (SQLException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex;
                }
                throw ex;
            }
        }

        @Override
        public byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException {
            return set.getBytes(index);
        }

    };

    public abstract String getPlotMeTableName();

    public abstract ResultSet getPlots(Connection conn, String worldName) throws SQLException;

    public abstract ResultSet getAllowed(Connection conn, ResultSet plotSet) throws SQLException;

    public abstract ResultSet getBanned(Connection conn, ResultSet plotSet) throws SQLException;

    public abstract byte[] getBytesFromIndex(ResultSet set, int index) throws SQLException;

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

}