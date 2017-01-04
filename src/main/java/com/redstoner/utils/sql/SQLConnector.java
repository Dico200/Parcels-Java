package com.redstoner.utils.sql;

import com.redstoner.utils.sql.control.UnsafeConsumer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SQLConnector {
    private static final List<SQLConnector> connectors = new ArrayList<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void closeAllConnections() {
        connectors.forEach(SQLConnector::closeConn);
    }

    private Connection conn = null;
    private boolean connected = false;

    public SQLConnector() {
        connectors.add(this);
    }

    public abstract SQLType getType();

    protected abstract Connection createConnection() throws SQLException;

    public boolean isConnected() {
        return connected;
    }

    protected void openConn() {
        try {
            this.conn = createConnection();
            this.connected = true;
        } catch (SQLTimeoutException ex) {
            System.out.println("[ERROR] No response from the MySQL server");
        } catch (SQLException ex) {
            System.out.println("[ERROR] While connecting to the MySQL server:");
            ex.printStackTrace();
        }
    }

    public void asyncConn(UnsafeConsumer<Connection> toRun) {
        executor.submit(() -> syncConn(toRun));
    }

    public void syncConn(UnsafeConsumer<Connection> toRun) {
        if (conn == null) {
            openConn();
        }
        toRun.accept(conn);
    }

    public void closeConn() {
        try {
            if (!conn.getAutoCommit())
                conn.commit();
            conn.close();
            conn = null;
            connected = false;
        } catch (SQLException e) {
            System.out.println("Failed to close database connection.");
        }
    }

}
