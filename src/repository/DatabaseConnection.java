package repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides JDBC connections and shared JSON escaping helpers.
 */
public class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/kyc_db";
    private static final String DB_USER = System.getenv().getOrDefault("MYSQL_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", "");

    /**
     * Opens a new JDBC connection to the KYC database.
     *
     * @return an open connection
     * @throws SQLException when the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Escapes a string for safe inclusion in a JSON value.
     *
     * @param s source string
     * @return escaped string, or empty string when null
     */
    public static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Renders a string as a JSON value, quoting and escaping it, or "null" when absent.
     *
     * @param s source string
     * @return JSON literal
     */
    public static String jsonStringOrNull(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }
}