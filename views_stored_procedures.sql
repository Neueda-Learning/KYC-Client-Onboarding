USE kyc_db;

-- ==========================================
-- 1. VIEWS
-- ==========================================

-- View 1: Cases with status OPEN or AWAITING_DOCUMENTS ordered by due_date
CREATE OR REPLACE VIEW pending_cases_vw AS
SELECT case_id, client_id, opened_date, status, assigned_officer_id, due_date
FROM onboarding_case
WHERE status IN ('OPEN', 'AWAITING_DOCUMENTS')
ORDER BY due_date ASC;

-- View 2: Documents expiring within 60 days across all active clients
CREATE OR REPLACE VIEW expiring_documents_vw AS
SELECT d.doc_id, d.case_id, dt.doc_type_name, d.expiry_date, c.client_id, c.full_name
FROM document d
JOIN onboarding_case oc ON d.case_id = oc.case_id
JOIN client c ON oc.client_id = c.client_id
JOIN document_type dt ON d.doc_type_id = dt.doc_type_id
WHERE d.expiry_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL 60 DAY);


-- ==========================================
-- 2. STORED PROCEDURES
-- ==========================================

DELIMITER //

-- Procedure 1: Create a new onboarding case
CREATE PROCEDURE open_case(
    IN p_client_id INT,
    IN p_officer_id INT,
    IN p_due_date DATE
)
BEGIN
    INSERT INTO onboarding_case (client_id, opened_date, status, assigned_officer_id, due_date)
    VALUES (p_client_id, NOW(), 'OPEN', p_officer_id, p_due_date);
END //

-- Procedure 2: Record document submission and update case status
CREATE PROCEDURE submit_document(
    IN p_case_id INT,
    IN p_doc_type_id INT,
    IN p_expiry_date DATE
)
BEGIN
    INSERT INTO document (case_id, doc_type_id, submission_date, verified_flag, expiry_date)
    VALUES (p_case_id, p_doc_type_id, NOW(), FALSE, p_expiry_date);
    
    UPDATE onboarding_case 
    SET status = 'AWAITING_DOCUMENTS' 
    WHERE case_id = p_case_id;
END //

-- Procedure 3: Apply risk classification to a case
CREATE PROCEDURE classify_risk(
    IN p_case_id INT,
    IN p_risk_level VARCHAR(20),
    IN p_assessed_by INT,
    IN p_rationale TEXT,
    IN p_next_review_date DATE
)
BEGIN
    INSERT INTO risk_classification (case_id, risk_level, classification_date, assessed_by, rationale, next_review_date)
    VALUES (p_case_id, p_risk_level, NOW(), p_assessed_by, p_rationale, p_next_review_date);
END //

-- Procedure 4: Mark case complete and activate client
CREATE PROCEDURE complete_onboarding(
    IN p_case_id INT
)
BEGIN
    DECLARE v_client_id INT;
    
    UPDATE onboarding_case 
    SET status = 'COMPLETED', completed_date = NOW() 
    WHERE case_id = p_case_id;
    
    SELECT client_id INTO v_client_id FROM onboarding_case WHERE case_id = p_case_id;
    
    UPDATE client 
    SET status = 'ACTIVE' 
    WHERE client_id = v_client_id;
END //

DELIMITER ;