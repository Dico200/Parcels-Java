package com.redstoner.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQL {
	
	@SuppressWarnings("unused")
	private static void printParcel(ResultSet set) throws SQLException {
		System.out.println("Parcel:");
		System.out.println(String.format("ID: %s, WORLD: %s, PX: %s, PZ: %s, OWNER: %s", set.getInt(1), set.getString(2), set.getInt(3), set.getInt(4), set.getString(5)));
	}
	
	public static void main(String[] args) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "");
		Statement stm = conn.createStatement();
		stm.executeUpdate("USE redstoner");
		
		stm.executeUpdate("DROP TABLE `parcels`");
		
		/*
		stm.executeUpdate("CREATE TABLE `parcels` ("
				+ "`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"
				+ "`world` VARCHAR(32) NOT NULL,"
				+ "`px` INTEGER NOT NULL,"
				+ "`pz` INTEGER NOT NULL,"
				+ "`owner` VARCHAR(50),"
				+ "UNIQUE KEY location(`world`, `px`, `pz`)"
				+ ");"
		);
		
		long nanos = -System.currentTimeMillis();
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 0, 0);");
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 0, 1);");
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 0, 2);");
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 0, 3);");
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 1, 1);");
		stm.executeUpdate("insert into parcels (world, px, pz) values ('world', 1, 1);");
		
		stm.executeUpdate("update parcels set owner='Dico' where px=0;");
		
		ResultSet set = stm.executeQuery("select * from parcels");
		while (set.next()) {
			printParcel(set);
		}
		
		for (set = stm.executeQuery("select * from parcels where owner='Dico' and (px = 2 or pz = 1)"); set.next();) {
			printParcel(set);
		}
		System.out.println("\nThat took millis " + (System.currentTimeMillis() + nanos));
		*/
	}
	
	private final String host, username, password;
	private Connection connection;
	private Statement statement;
	
	public SQL(String host, String username, String password) {
		this.host = host;
		this.username = username;
		this.password = password;
		this.connection = null;
		this.statement = null;
	}
	
	public Connection connect() {
		if (connection == null) try {
			this.connection = createConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public Statement createStatement() {
		connect();
		
		if (statement == null) try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return statement;
	}
	
	private Connection createConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://" + host, username, password);
	}
	
	public void closeStatement() {
		if (statement == null)
			return;
		try {
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection() {
		if (connection == null)
			return;
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
