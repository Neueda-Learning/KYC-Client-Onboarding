package com.kyc.repository;

import com.kyc.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    // 1. Status update query (matches original updateStatus method)
    @Modifying
    @Query("UPDATE Client c SET c.status = :status WHERE c.clientId = :clientId")
    int updateStatus(@Param("clientId") Integer clientId, @Param("status") String status);

    // 2. Complex join query without custom DTOs.
    // Returning Map<String, Object> automatically captures columns as key-value pairs
    // and serializes into valid JSON in controllers without needing extra classes.
    @Query(value = """
        SELECT DISTINCT 
            c.client_id AS client_id,
            c.full_name AS full_name,
            c.client_type AS client_type,
            d.doc_id AS doc_id,
            dt.doc_type_name AS doc_type,
            d.expiry_date AS expiry_date
        FROM document d
        JOIN document_type dt ON d.doc_type_id = dt.doc_type_id
        JOIN onboarding_case oc ON d.case_id = oc.case_id
        JOIN client c ON oc.client_id = c.client_id
        WHERE d.expiry_date IS NOT NULL
          AND d.expiry_date BETWEEN :today AND :endDate
        """, nativeQuery = true)
    List<Map<String, Object>> findExpiringDocuments(
        @Param("today") LocalDate today, 
        @Param("endDate") LocalDate endDate
    );
}