package com.redstoner.utils.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class SQLConnector {
	
	public abstract Connection createConnection() throws SQLException;
	public abstract SQLType getType();
	
	private static final List<SQLConnector> connectors = new ArrayList<>();
	
	public static void closeAllConnections() {
		connectors.forEach(c -> {
			try {
				if (!c.conn.isClosed()) c.conn.close();
			} catch (SQLException e) {
				System.out.println("[com.redstoner.utils.SQLConnector] Failed to close a database connection");
				e.printStackTrace();
			}
		});
	}
	
	private static final ThreadGroup threadGroup = new ThreadGroup("SqlConnector");
	
	private Connection conn;
	
	public SQLConnector() {
		connectors.add(this);
	}
	
	protected void openConn() {
		try {
			this.conn = createConnection();
		} catch (SQLException e) {
			if (e instanceof SQLTimeoutException) {
				System.out.println("[ERROR] No response from the MySQL server");
			} else {
				System.out.println("[ERROR] While connecting to the MySQL server:");
				e.printStackTrace();
			}
		}
	}
	
	private void runAsync(Runnable toRun) {
		Thread thread = new Thread(threadGroup, toRun);
		thread.setDaemon(false);
		thread.start();
	}
	
	public void asyncConn(Consumer<Connection> toRun) {
		runAsync(() -> {
			syncConn(toRun);
		});
	}
	
	public void syncConn(Consumer<Connection> toRun) {
		toRun.accept(conn);
	}
	
	public void closeConn() {
		try {
			conn.close();
			conn = null;
		} catch (SQLException e) {
			System.out.println("Failed to close database connection.");
		}
	}
	
	static {
		threadGroup.setDaemon(false);
	}

}
