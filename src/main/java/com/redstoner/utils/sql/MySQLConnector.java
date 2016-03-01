package com.redstoner.utils.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnector extends SQLConnector {
	
	private final String hostname, database, username, password;
	
	public MySQLConnector(String hostname, String database, String username, String password) {
		this.hostname = hostname;
		this.database = database;
		this.username = username;
		this.password = password;
		super.openConn();
	}

	@Override
	public Connection createConnection() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:mysql://" + hostname, username, password);
		Statement stm = conn.createStatement();
		stm.executeUpdate(String.format("CREATE DATABASE IF NOT EXISTS `%s`", database));
		stm.executeUpdate(String.format("USE `%s`", database));
		stm.close();
		return conn;
	}
	
	@Override
	public SQLType getType() {
		return SQLType.MySQL;
	}
}
