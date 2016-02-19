package com.redstoner.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;

import com.redstoner.parcels.ParcelsPlugin;

public class SqlConnector {
	
	private final String host, username, password, database;
	
	private Connection conn;
	private boolean connected;
	
	public SqlConnector(String host, String database, String username, String password) {
		this.host = host;
		this.database = database;
		this.username = username;
		this.password = password;
		this.connected = false;
		
		executeUpdate(String.format("CREATE DATABASE IF NOT EXISTS `%s`", database));

	}
	
	public boolean open() {
		if (!connected) {
			try {
				this.conn = DriverManager.getConnection("jdbc:mysql://" + host, username, password);
				conn.createStatement().executeUpdate(String.format("USE `%s`", database));
			} catch (SQLTimeoutException e) {
				ParcelsPlugin.log("ERROR: Failed to connect to MySQL server.");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			this.connected = true;
			return true;
		}
		return false;
	}
	
	public boolean close() {
		if (connected) {
			try {
				this.conn.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			this.conn = null;
			this.connected = false;
			return true;
		}
		return false;
	}
	
	public void execute(Consumer<Statement> cons) {
		try {
			boolean close = open();
			Statement stm = conn.createStatement();
			cons.accept(stm);
			stm.close();
			if (close)
				close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ResultSet execute(String sql) {
		try {
			boolean close = open();
			Statement stm = conn.createStatement();
			ResultSet res = stm.executeQuery(sql);
			stm.close();
			if (close)
				close();
			return res;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void executeUpdate(String sql) {
		try {
			boolean close = open();
			Statement stm = conn.createStatement();
			stm.executeUpdate(sql);
			stm.close();
			if (close)
				close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Connection getConnection() {
		open();
		return conn;
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public <T> T executeSet(String sql, Function<ResultSet, T> function) {
		try {
			boolean close = open();
			Statement stm = conn.createStatement();
			T value = function.apply(stm.executeQuery(sql));
			stm.close();
			if (close)
				close();
			return value;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
