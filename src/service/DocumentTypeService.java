package service;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DocumentTypeRepository;

/**
 * Business logic for document type lookups.
 */
public class DocumentTypeService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeService.class);
    private final DocumentTypeRepository documentTypeRepository;

    public DocumentTypeService() {
        this(new DocumentTypeRepository());
    }

    public DocumentTypeService(DocumentTypeRepository documentTypeRepository) {
        this.documentTypeRepository = documentTypeRepository;
    }

    /**
     * Lists all known document types.
     *
     * @return JSON array of document types
     * @throws SQLException when the query fails
     */
    public String listDocumentTypes() throws SQLException {
        java.util.List<String> types = documentTypeRepository.listDocumentTypes();
        logger.debug("Listed document types: count={}", types.size());
        return "[\n" + String.join(",\n", types) + "\n]";
    }
}