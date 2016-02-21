package com.redstoner.utils.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.function.Consumer;

import com.redstoner.parcels.ParcelsPlugin;

public class SqlConnector {
	
	private final String host, username, password, database;
	private Connection conn;
	private final ThreadGroup threadGroup;
	
	public SqlConnector(String host, String database, String username, String password) {
		this.host = host;
		this.database = database;
		this.username = username;
		this.password = password;
		this.threadGroup = new ThreadGroup("SqlConnector");
		threadGroup.setDaemon(false);
		
		openConn();

	}
	
	private void openConn() {
		try {
			this.conn = DriverManager.getConnection("jdbc:mysql://" + host, username, password);
			conn.createStatement().executeUpdate(String.format("USE `%s`", database));
			SqlUtil.executeUpdate(conn, String.format("CREATE DATABASE IF NOT EXISTS `%s`", database));
		} catch (SQLException e) {
			if (e instanceof SQLTimeoutException) {
				ParcelsPlugin.log("[ERROR] No response from the MySQL server");
			} else {
				ParcelsPlugin.log("[ERROR] While connecting to the MySQL server:");
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
			ParcelsPlugin.log("Failed to close database connection.");
		}
	}
}
