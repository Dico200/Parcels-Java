package com.redstoner.utils.sql;

import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

import java.sql.*;

public class MySQLConnector extends SQLConnector {

    private static final TCharSet allowedChars = new TCharHashSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_".toCharArray());
    private final String hostname, database, username, password;

    public MySQLConnector(String hostname, String database, String username, String password) {
        this.hostname = hostname;
        this.database = sanitise(database);
        this.username = username;
        this.password = password;
        super.openConn();
    }

    private static String sanitise(String string) {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!allowedChars.contains(chars[i])) {
                chars[i] = '_';
            }
        }
        return String.valueOf(chars);
    }

    @Override
    public Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://" + hostname, username, password);
        try (Statement sm = conn.createStatement()) {
            sm.execute("CREATE DATABASE IF NOT EXISTS " + database + "; USE " + database + ";");
        }
        return conn;
    }

    @Override
    public SQLType getType() {
        return SQLType.MySQL;
    }
}
