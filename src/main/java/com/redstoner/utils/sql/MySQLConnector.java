package com.redstoner.utils.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
		PreparedStatement pstm = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS ?; USE ?;");
		pstm.setString(1, database);
		pstm.setString(2, database);
		pstm.executeUpdate();
		pstm.close();
		return conn;
	}
	
	@Override
	public SQLType getType() {
		return SQLType.MySQL;
	}
}
