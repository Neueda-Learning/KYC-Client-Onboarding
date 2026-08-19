package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access layer for login credential lookups across client, compliance_officer
 * and admin_officer tables.
 */
public class AuthRepository {

    /**
     * A credential row looked up by username, independent of which table it came from.
     */
    public static class Credential {
        public final int id;
        public final String fullName;
        public final String username;
        public final String passwordHash;

        Credential(int id, String fullName, String username, String passwordHash) {
            this.id = id;
            this.fullName = fullName;
            this.username = username;
            this.passwordHash = passwordHash;
        }
    }

    /**
     * Looks up a client's credential row by username.
     *
     * @param username login username
     * @return credential row, or null when no client has this username
     * @throws SQLException when the query fails
     */
    public Credential findClientByUsername(String username) throws SQLException {
        return findByUsername("client", "client_id", username);
    }

    /**
     * Looks up a compliance officer's credential row by username.
     *
     * @param username login username
     * @return credential row, or null when no officer has this username
     * @throws SQLException when the query fails
     */
    public Credential findOfficerByUsername(String username) throws SQLException {
        return findByUsername("compliance_officer", "officer_id", username);
    }

    /**
     * Looks up an admin compliance officer's credential row by username.
     *
     * @param username login username
     * @return credential row, or null when no admin officer has this username
     * @throws SQLException when the query fails
     */
    public Credential findAdminByUsername(String username) throws SQLException {
        return findByUsername("admin_officer", "admin_id", username);
    }

    private Credential findByUsername(String table, String idColumn, String username) throws SQLException {
        String sql = "SELECT " + idColumn + ", full_name, username, password_hash FROM " + table + " WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Credential(rs.getInt(idColumn), rs.getString("full_name"),
                        rs.getString("username"), rs.getString("password_hash"));
            }
        }
    }
}
