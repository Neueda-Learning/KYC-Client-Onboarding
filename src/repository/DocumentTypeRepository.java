package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for document type lookups.
 */
public class DocumentTypeRepository {

    /**
     * Lists all known document types.
     *
     * @return JSON array entries of doc_type_id/doc_type_name pairs
     * @throws SQLException when the query fails
     */
    public List<String> listDocumentTypes() throws SQLException {
        String sql = "SELECT doc_type_id, doc_type_name FROM document_type ORDER BY doc_type_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add("{"
                        + "\"doc_type_id\":" + rs.getInt("doc_type_id") + ","
                        + "\"doc_type_name\":\"" + DatabaseConnection.escape(rs.getString("doc_type_name")) + "\""
                        + "}");
            }
        }
        return list;
    }
}