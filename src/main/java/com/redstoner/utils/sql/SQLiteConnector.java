package com.redstoner.utils.sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnector extends SQLConnector {

    private final File sqlFile;

    public SQLiteConnector(File sqlFile) {
        if (!sqlFile.exists()) {
            try {
                sqlFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.sqlFile = sqlFile;
        super.openConn();
    }

    @Override
    public Connection createConnection() throws SQLException {
        DriverManager.registerDriver(new org.sqlite.JDBC());
        return DriverManager.getConnection("jdbc:sqlite:" + sqlFile.getAbsolutePath());
    }

    @Override
    public SQLType getType() {
        return SQLType.SQLite;
    }

}
