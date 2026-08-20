USE kyc_db;

-- ============================================================================
-- EXPANDED ML/ANALYTICS DATASET (500 NEW RECORDS: ID 16 TO 515)
-- Run after 01_seed_data.sql
-- ============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS GenerateAnalysisDataset$$
CREATE PROCEDURE GenerateAnalysisDataset()
BEGIN
    DECLARE i INT DEFAULT 16;
    DECLARE max_id INT DEFAULT 515;

    -- Attributes
    DECLARE v_name VARCHAR(255);
    DECLARE v_client_type VARCHAR(50);
    DECLARE v_country CHAR(2);
    DECLARE v_dob DATE;
    DECLARE v_age INT;
    DECLARE v_occupation VARCHAR(80);
    DECLARE v_employer VARCHAR(80);
    DECLARE v_income VARCHAR(80);
    DECLARE v_funds VARCHAR(80);
    DECLARE v_status VARCHAR(50);
    DECLARE v_product VARCHAR(50);
    DECLARE v_case_status VARCHAR(20);
    DECLARE v_officer_id INT;
    DECLARE v_opened_date TIMESTAMP;
    DECLARE v_completed_date TIMESTAMP;
    DECLARE v_rej_reason VARCHAR(200);
    DECLARE v_risk_level VARCHAR(20);
    DECLARE v_risk_rationale TEXT;

    -- Statistical Drift Factors
    DECLARE v_risk_score INT;
    DECLARE v_rand FLOAT;
    DECLARE v_is_suspicious_name BOOLEAN;
    DECLARE v_is_high_risk_country BOOLEAN;
    DECLARE v_is_young BOOLEAN;
    DECLARE v_is_low_income BOOLEAN;
    DECLARE v_docs_unverified BOOLEAN;

    -- Seeded static lookup arrays helper
    WHILE i <= max_id DO
        SET v_risk_score = 0;
        SET v_rand = RAND();

        -- 1. Determine Geography & Controversy
        IF (i % 17 = 0 OR i % 29 = 0) THEN
            -- Controversial / Sanctions watchlist jurisdictions
            SET v_country = ELT(1 + (i % 5), 'RU', 'IR', 'KP', 'SY', 'VE');
            SET v_is_high_risk_country = TRUE;
            SET v_risk_score = v_risk_score + 35;
        ELSE
            -- Standard jurisdictions
            SET v_country = ELT(1 + (i % 10), 'GB', 'US', 'DE', 'FR', 'ES', 'IT', 'NL', 'SE', 'JP', 'CA');
            SET v_is_high_risk_country = FALSE;
        END IF;

        -- 2. Determine Name & Controversy
        IF (i % 23 = 0) THEN
            -- PEP / Watchlist-adjacent profiles
            SET v_name = CONCAT(ELT(1 + (i % 4), 'Viktor', 'Tariq', 'Igor', 'Bashar'), ' ', ELT(1 + (i % 4), 'Petrov', 'Al-Mansoor', 'Volkov', 'Haddad'));
            SET v_is_suspicious_name = TRUE;
            SET v_risk_score = v_risk_score + 25;
        ELSE
            SET v_name = CONCAT(
                ELT(1 + ((i * 3) % 15), 'James', 'Oliver', 'Emma', 'Lucas', 'Liam', 'Sofia', 'Ethan', 'Chloe', 'Noah', 'Mia', 'Alexander', 'Hannah', 'Daniel', 'Zoe', 'Marcus'),
                ' ',
                ELT(1 + ((i * 7) % 15), 'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson')
            );
            SET v_is_suspicious_name = FALSE;
        END IF;

        -- 3. Determine Client Type & Demographics
        IF (i % 8 = 0) THEN
            SET v_client_type = 'CORPORATE';
            SET v_dob = DATE_SUB('2026-01-01', INTERVAL (2 + (i % 15)) YEAR);
            SET v_occupation = NULL;
            SET v_employer = NULL;
            SET v_funds = 'Business Revenue';
            SET v_is_young = FALSE;
        ELSEIF (i % 19 = 0) THEN
            SET v_client_type = 'POLITICAL';
            SET v_dob = DATE_SUB('2026-01-01', INTERVAL (45 + (i % 25)) YEAR);
            SET v_occupation = 'Diplomatic Attaché';
            SET v_employer = 'Foreign Affairs';
            SET v_funds = 'Government Salary';
            SET v_risk_score = v_risk_score + 20;
            SET v_is_young = FALSE;
        ELSE
            SET v_client_type = 'INDIVIDUAL';
            -- Inject youth bias (< 21 years old)
            IF (i % 11 = 0) THEN
                SET v_dob = DATE_SUB('2026-01-01', INTERVAL (18 + (i % 3)) YEAR);
                SET v_is_young = TRUE;
                SET v_risk_score = v_risk_score + 20;
            ELSE
                SET v_dob = DATE_SUB('2026-01-01', INTERVAL (23 + (i % 45)) YEAR);
                SET v_is_young = FALSE;
            END IF;
            
            SET v_occupation = ELT(1 + (i % 6), 'Engineer', 'Consultant', 'Designer', 'Student', 'Retail Associate', 'Contractor');
            SET v_employer = ELT(1 + (i % 6), 'Apex Tech', 'Global Advisory', 'Freelance', 'University', 'Metro Retail', 'Self-Employed');
            SET v_funds = 'Employment Income';
        END IF;

        -- 4. Financial Status
        IF (v_is_young OR (i % 9 = 0)) THEN
            SET v_income = '<25K';
            SET v_is_low_income = TRUE;
            SET v_risk_score = v_risk_score + 15;
        ELSE
            SET v_income = ELT(1 + (i % 4), '25-50K', '50-100K', '100-250K', '250K+');
            SET v_is_low_income = FALSE;
        END IF;

        -- 5. Document Verification Drift
        IF (v_risk_score >= 35 OR (i % 5 = 0)) THEN
            SET v_docs_unverified = TRUE;
            SET v_risk_score = v_risk_score + 30;
        ELSE
            SET v_docs_unverified = FALSE;
        END IF;

        -- 6. Officer Assignment & Decision Outcome Logic
        SET v_officer_id = 1 + (i % 5);
        SET v_opened_date = TIMESTAMP(DATE_SUB('2026-07-30', INTERVAL (i % 60) DAY), '09:00:00');

        -- Decision threshold with noise injection
        IF ((v_risk_score >= 50 AND v_rand > 0.12) OR (v_risk_score < 50 AND v_rand < 0.08)) THEN
            -- REJECTED
            SET v_status = 'REJECTED';
            SET v_case_status = 'CLOSED';
            SET v_completed_date = TIMESTAMPADD(DAY, 3 + (i % 5), v_opened_date);
            SET v_risk_level = IF(v_risk_score > 60, 'HIGH', 'MEDIUM');
            
            SET v_rej_reason = CASE 
                WHEN v_docs_unverified THEN 'Required documents missing or failed authenticity checks'
                WHEN v_is_high_risk_country THEN 'Country of origin subject to enhanced sanctions screening'
                WHEN v_is_suspicious_name THEN 'Direct match on international PEP/sanctions watchlists'
                WHEN v_is_low_income AND v_income = '<25K' THEN 'Income band fails product affordability thresholds'
                ELSE 'Compliance criteria not met under enhanced due diligence'
            END;
            SET v_risk_rationale = CONCAT('Adverse indicators flagged during review. Rejection driven by: ', v_rej_reason);
        ELSE
            -- APPROVED / ACTIVE
            SET v_status = 'ACTIVE';
            SET v_case_status = 'CLOSED';
            SET v_completed_date = TIMESTAMPADD(DAY, 2 + (i % 4), v_opened_date);
            SET v_rej_reason = NULL;
            SET v_risk_level = IF(v_risk_score > 30, 'MEDIUM', 'LOW');
            SET v_risk_rationale = 'All baseline KYC checks completed and verified. Risk is within operational thresholds.';
        END IF;

        SET v_product = ELT(1 + (i % 4), 'RETAIL_BANKING', 'CORPORATE_ACCOUNT', 'PRIVATE_BANKING', 'WEALTH_MANAGEMENT');

        -- ====================================================================
        -- INSERTS
        -- ====================================================================

        -- Client
        INSERT INTO client (
            client_id, full_name, client_type, nationality, date_of_birth,
            country_of_birth, tax_residency, occupation, employer,
            main_source_of_funds, annual_income_band, status, is_active,
            username, password_hash
        ) VALUES (
            i, v_name, v_client_type, v_country, v_dob,
            v_country, v_country, v_occupation, v_employer,
            v_funds, v_income, v_status, (v_status = 'ACTIVE'),
            CONCAT('user_', i), 'pbkdf2_sha256$210000$ac1rG6uAi30eexRN6DPn2A==$fRT14v48H0So1gsLkeudf/DEnJOnT/NjcdaA5PCntsk='
        );

        -- Address
        INSERT INTO client_address (
            address_id, client_id, address_type, line1, city, country, postcode, is_current
        ) VALUES (
            i, i, 'REGISTERED', CONCAT(10 + (i % 90), ' Boulevard Way'), 'Metropolis', v_country, CONCAT('POST-', i), 'TRUE'
        );

        -- Case
        INSERT INTO onboarding_case (
            case_id, client_id, opened_date, product_type, case_status,
            assigned_officer_id, due_date, completed_date, rejection_reason
        ) VALUES (
            i, i, v_opened_date, v_product, v_case_status,
            v_officer_id, DATE_ADD(DATE(v_opened_date), INTERVAL 14 DAY), v_completed_date, v_rej_reason
        );

        -- Primary Document (e.g., Passport / ID)
        INSERT INTO document (
            doc_id, case_id, doc_type_id, submission_date, verified_flag,
            verified_by, verified_at, expiry_date, rejection_reason
        ) VALUES (
            (i * 2) - 1, i, IF(v_client_type = 'CORPORATE', 16, 1),
            v_opened_date,
            NOT v_docs_unverified,
            IF(NOT v_docs_unverified, v_officer_id, NULL),
            IF(NOT v_docs_unverified, TIMESTAMPADD(HOUR, 4, v_opened_date), NULL),
            IF(v_docs_unverified, '2024-01-01', '2031-12-31'),
            IF(v_docs_unverified, 'Document unverified, illegible, or expired', NULL)
        );

        -- Secondary Document (Proof of Address / KYC form)
        INSERT INTO document (
            doc_id, case_id, doc_type_id, submission_date, verified_flag,
            verified_by, verified_at, expiry_date, rejection_reason
        ) VALUES (
            (i * 2), i, IF(v_client_type = 'CORPORATE', 21, 7),
            TIMESTAMPADD(MINUTE, 30, v_opened_date),
            NOT v_docs_unverified,
            IF(NOT v_docs_unverified, v_officer_id, NULL),
            IF(NOT v_docs_unverified, TIMESTAMPADD(HOUR, 5, v_opened_date), NULL),
            NULL,
            IF(v_docs_unverified, 'Incomplete verification checks', NULL)
        );

        -- Risk Assessment Classification
        INSERT INTO risk_classification (
            classification_id, case_id, risk_level, classification_date,
            assessed_by, rationale, next_review_date
        ) VALUES (
            i, i, v_risk_level, v_completed_date,
            v_officer_id, v_risk_rationale, DATE_ADD(DATE(v_completed_date), INTERVAL 1 YEAR)
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

-- Execute batch generator
CALL GenerateAnalysisDataset();

-- Clean up helper procedure
DROP PROCEDURE IF EXISTS GenerateAnalysisDataset;