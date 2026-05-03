package com.campuseateries.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Centralizes JDBC lifecycle and classpath configuration for the persistence layer (DAO collaborators).
 */
public final class DbConnectionManager {

    private static final DbConnectionManager INSTANCE = new DbConnectionManager();

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private DbConnectionManager() {
        Properties props = loadProperties("/db.properties");
        this.jdbcUrl = props.getProperty("jdbc.url");
        this.username = props.getProperty("jdbc.username");
        this.password = props.getProperty("jdbc.password");
        if (jdbcUrl == null || username == null || password == null) {
            throw new IllegalStateException(
                    "jdbc.url, jdbc.username, jdbc.password must be declared in classpath db.properties.");
        }
    }

    public static DbConnectionManager getInstance() {
        return INSTANCE;
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static Properties loadProperties(String classpathLocation) {
        try (InputStream in = DbConnectionManager.class.getResourceAsStream(classpathLocation)) {
            Objects.requireNonNull(in, "Missing classpath resource: " + classpathLocation);
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read JDBC configuration from " + classpathLocation, e);
        }
    }
}
