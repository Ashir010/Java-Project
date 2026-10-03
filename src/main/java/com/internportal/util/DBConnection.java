package com.internportal.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens JDBC connections using the settings in db.properties. */
public final class DBConnection {

    private static Properties props;

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        Properties p = loadProps();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.user"),
                p.getProperty("db.password"));
    }

    private static synchronized Properties loadProps() throws SQLException {
        if (props == null) {
            Properties p = new Properties();
            try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
                if (in == null) {
                    throw new SQLException("db.properties not found in src/main/resources");
                }
                p.load(in);
            } catch (IOException e) {
                throw new SQLException("Could not read db.properties", e);
            }
            props = p;
        }
        return props;
    }
}
