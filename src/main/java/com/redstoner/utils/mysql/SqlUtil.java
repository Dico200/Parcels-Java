package com.redstoner.utils.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;

public class SqlUtil {
	
	public static void execute(Connection conn, Consumer<Statement> cons) {
		try {
			Statement stm = conn.createStatement();
			cons.accept(stm);
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ResultSet execute(Connection conn, String query) {
		try {
			Statement stm = conn.createStatement();
			ResultSet res = stm.executeQuery(query);
			stm.close();
			return res;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void executeUpdate(Connection conn, String... updates) {
		if (updates == null || updates.length == 0)
			return;
		try {
			Statement stm = conn.createStatement();
			for (String update : updates)
				stm.executeUpdate(update);
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static <T> T executeSet(Connection conn, String query, Function<ResultSet, T> function) {
		try {
			Statement stm = conn.createStatement();
			T value = function.apply(stm.executeQuery(query));
			stm.close();
			return value;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static PreparedStatement prepareStatement(Connection conn, String query, Consumer<PreparedStatement> preparer) {
		try {
			PreparedStatement pstm = conn.prepareStatement(query);
			preparer.accept(pstm);
			return pstm;
		} catch (SQLException e) {
			throw new RuntimeException(e);		
		}
	}

}
