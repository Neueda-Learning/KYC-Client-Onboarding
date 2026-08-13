USE kyc_db;

-- ==========================================
-- 1. VIEWS
-- ==========================================

-- View 1: Cases with status OPEN or AWAITING_DOCUMENTS ordered by due_date
CREATE OR REPLACE VIEW pending_cases_vw AS
SELECT 
    case_id, 
    client_id, 
    opened_date, 
    product_type,
    case_status, 
    assigned_officer_id, 
    due_date
FROM onboarding_case
WHERE case_status IN ('OPEN', 'AWAITING_DOCUMENTS')
ORDER BY due_date ASC;

-- View 2: Documents expiring within 60 days across all active clients
CREATE OR REPLACE VIEW expiring_documents_vw AS
SELECT 
    d.doc_id, 
    d.case_id, 
    dt.doc_type_name, 
    d.expiry_date, 
    c.client_id, 
    c.full_name
FROM document d
JOIN onboarding_case oc ON d.case_id = oc.case_id
JOIN client c ON oc.client_id = c.client_id
JOIN document_type dt ON d.doc_type_id = dt.doc_type_id
WHERE c.is_active = TRUE 
  AND d.expiry_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL 60 DAY);


-- ==========================================
-- 2. STORED PROCEDURES
-- ==========================================

DELIMITER //

-- Procedure 1: Create new client and onboarding case; auto-assign required document checklist
DROP PROCEDURE IF EXISTS open_case //
CREATE PROCEDURE open_case(
    -- Client parameters
    IN p_full_name VARCHAR(255),
    IN p_client_type VARCHAR(255),
    IN p_nationality CHAR(2),
    IN p_date_of_birth DATE,
    IN p_country_of_birth CHAR(2),
    IN p_tax_residency CHAR(2),
    IN p_occupation VARCHAR(80),
    IN p_employer VARCHAR(80),
    IN p_main_source_of_funds VARCHAR(80),
    IN p_annual_income_band VARCHAR(80),
    -- Case parameters
    IN p_product_type VARCHAR(255),
    IN p_officer_id INT,
    IN p_due_date DATE
)
BEGIN
    DECLARE v_client_id INT;
    DECLARE v_case_id INT;

    -- 1. Insert Client record
    INSERT INTO client (
        full_name, client_type, nationality, date_of_birth, country_of_birth, 
        tax_residency, occupation, employer, main_source_of_funds, 
        annual_income_band, status, is_active
    ) VALUES (
        p_full_name, p_client_type, p_nationality, p_date_of_birth, p_country_of_birth, 
        p_tax_residency, p_occupation, p_employer, p_main_source_of_funds, 
        p_annual_income_band, 'PENDING', TRUE
    );
    
    SET v_client_id = LAST_INSERT_ID();

    -- 2. Create Onboarding Case
    INSERT INTO onboarding_case (
        client_id, opened_date, product_type, case_status, assigned_officer_id, due_date
    ) VALUES (
        v_client_id, NOW(), p_product_type, 'OPEN', p_officer_id, p_due_date
    );

    SET v_case_id = LAST_INSERT_ID();

    -- 3. Auto-assign required document checklist based on client_type
    INSERT INTO document (case_id, doc_type_id, submission_date, verified_flag)
    SELECT v_case_id, dt.doc_type_id, NOW(), FALSE
    FROM document_type dt
    WHERE (p_client_type = 'INDIVIDUAL' AND dt.required_for_individual = TRUE)
       OR (p_client_type = 'CORPORATE'  AND dt.required_for_corporate = TRUE)
       OR (p_client_type = 'TRUST'      AND dt.required_for_trust = TRUE)
       OR (p_client_type = 'POLITICAL'  AND dt.required_for_political = TRUE);
END //


-- Procedure 2: Record document submission; update case step status
DROP PROCEDURE IF EXISTS submit_document //
CREATE PROCEDURE submit_document(
    IN p_case_id INT,
    IN p_doc_type_id INT,
    IN p_expiry_date DATE
)
BEGIN
    -- Record or update submission details on the document
    IF EXISTS (SELECT 1 FROM document WHERE case_id = p_case_id AND doc_type_id = p_doc_type_id) THEN
        UPDATE document 
        SET submission_date = NOW(),
            expiry_date = p_expiry_date,
            verified_flag = FALSE
        WHERE case_id = p_case_id AND doc_type_id = p_doc_type_id;
    ELSE
        INSERT INTO document (case_id, doc_type_id, submission_date, verified_flag, expiry_date)
        VALUES (p_case_id, p_doc_type_id, NOW(), FALSE, p_expiry_date);
    END IF;

    -- Update case status to PENDING
    UPDATE onboarding_case 
    SET case_status = 'PENDING' 
    WHERE case_id = p_case_id;
END //


-- Procedure 3: Apply risk classification to a case; update client risk band
DROP PROCEDURE IF EXISTS classify_risk //
CREATE PROCEDURE classify_risk(
    IN p_case_id INT,
    IN p_risk_level VARCHAR(255),
    IN p_assessed_by INT,
    IN p_rationale TEXT,
    IN p_next_review_date DATE
)
BEGIN
    -- Record classification
    INSERT INTO risk_classification (
        case_id, risk_level, classification_date, assessed_by, rationale, next_review_date
    )
    VALUES (
        p_case_id, p_risk_level, NOW(), p_assessed_by, p_rationale, p_next_review_date
    );
END //


-- Procedure 4: Mark case complete; activate client; record completion timestamp
DROP PROCEDURE IF EXISTS complete_onboarding //
CREATE PROCEDURE complete_onboarding(
    IN p_case_id INT
)
BEGIN
    DECLARE v_client_id INT;

    -- Retrieve associated client ID
    SELECT client_id INTO v_client_id 
    FROM onboarding_case 
    WHERE case_id = p_case_id;

    -- 1. Mark case complete and set timestamp
    UPDATE onboarding_case 
    SET case_status = 'CLOSED', 
        completed_date = NOW() 
    WHERE case_id = p_case_id;

    -- 2. Activate client
    UPDATE client 
    SET status = 'ACTIVE',
        is_active = TRUE
    WHERE client_id = v_client_id;
END //

DELIMITER ;