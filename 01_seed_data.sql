USE kyc_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `risk_classification`;
TRUNCATE TABLE `document`;
TRUNCATE TABLE `onboarding_case`;
TRUNCATE TABLE `client_address`;
TRUNCATE TABLE `client`;
TRUNCATE TABLE `document_type`;
TRUNCATE TABLE `compliance_officer`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Insert Compliance Officers
INSERT INTO compliance_officer (officer_id, full_name, email, password) VALUES
(1, 'John Smith', 'john.smith@bank.com', 'hashed_pass_1'),
(2, 'Anna Novak', 'anna.novak@bank.com', 'hashed_pass_2'),
(3, 'Robert Taylor', 'robert.taylor@bank.com', 'hashed_pass_3');

-- 2. Insert Document Types
INSERT INTO `document_type` (
    `doc_type_name`, 
    `required_for_individual`, 
    `required_for_corporate`, 
    `required_for_trust`, 
    `required_for_political`
) VALUES 
('PASSPORT', TRUE, TRUE, TRUE, TRUE),
('DRIVING_LICENCE', TRUE, TRUE, TRUE, TRUE),
('NATIONAL_ID', TRUE, TRUE, TRUE, TRUE),
('UTILITY_BILL', TRUE, TRUE, TRUE, TRUE),
('BANK_STATEMENT', TRUE, TRUE, TRUE, TRUE),
('COUNCIL_TAX_BILL', TRUE, FALSE, FALSE, FALSE),
('KYC_APPLICATION_FORM', TRUE, TRUE, TRUE, TRUE),
('TAX_SELF_CERT_FATCA_CRS', TRUE, TRUE, TRUE, TRUE),
('PAY_SLIP_TAX_RETURN', TRUE, FALSE, FALSE, TRUE),
('SHARE_PURCHASE_AGREEMENT', TRUE, FALSE, FALSE, TRUE),
('PROBATE_WILL', TRUE, FALSE, TRUE, FALSE),
('DEED_OF_SALE', TRUE, FALSE, FALSE, TRUE),
('ACCOUNTANT_NET_ASSET_DECLARATION', TRUE, FALSE, FALSE, TRUE),
('OFFICIAL_GOVT_APPOINTMENT_LETTER', FALSE, FALSE, FALSE, TRUE),
('PEP_BUSINESS_RATIONALE_STMT', FALSE, FALSE, FALSE, TRUE),
('CERTIFICATE_OF_INCORPORATION', FALSE, TRUE, FALSE, FALSE),
('MEMORANDUM_ARTICLES_ASSOCIATION', FALSE, TRUE, FALSE, FALSE),
('COMMERCIAL_REGISTER_EXTRACT', FALSE, TRUE, FALSE, FALSE),
('CERTIFICATE_OF_GOOD_STANDING', FALSE, TRUE, FALSE, FALSE),
('CORPORATE_GROUP_OWNERSHIP_CHART', FALSE, TRUE, FALSE, FALSE),
('UBO_DECLARATION_FORM', FALSE, TRUE, FALSE, FALSE),
('BOARD_RESOLUTION_ACCOUNT_OPENING', FALSE, TRUE, FALSE, FALSE),
('AUTHORIZED_SIGNATORY_LIST', FALSE, TRUE, TRUE, FALSE),
('AUDITED_FINANCIAL_STATEMENTS', FALSE, TRUE, FALSE, FALSE),
('TRUST_DEED', FALSE, FALSE, TRUE, FALSE),
('DEED_OF_VARIATION', FALSE, FALSE, TRUE, FALSE),
('LETTER_OF_WISHES', FALSE, FALSE, TRUE, FALSE);

-- 3. Insert 10 Clients (Updated to match new schema columns)
INSERT INTO client (client_id, full_name, client_type, nationality, date_of_birth, country_of_birth, tax_residency, occupation, employer, main_source_of_funds, annual_income_band, status, is_active) VALUES
(1, 'Michael Brown', 'INDIVIDUAL', 'UK', '1985-04-12', 'UK', 'UK', 'Software Engineer', 'Tech Corp', 'Employment Income', '50-100K', 'ACTIVE', TRUE),
(2, 'TechCorp Solutions Ltd', 'CORPORATE', 'UK', '2010-01-01', 'UK', 'UK', NULL, NULL, 'Business Revenue', '250K+', 'PENDING', TRUE),
(3, 'Elena Rostova', 'INDIVIDUAL', 'DE', '1992-09-25', 'DE', 'DE', 'Financial Analyst', 'Bank DE', 'Employment Income', '50-100K', 'PENDING', TRUE),
(4, 'The Sterling Family Trust', 'TRUST', 'UK', '2018-06-15', 'UK', 'UK', NULL, NULL, 'Investment Portfolio', '250K+', 'ACTIVE', TRUE),
(5, 'Senator David Wilson', 'POLITICAL', 'US', '1978-11-03', 'US', 'US', 'Senator', 'US Senate', 'Government Salary', '100-250K', 'SUSPENDED', FALSE),
(6, 'Global Import Export LLC', 'CORPORATE', 'US', '2018-03-20', 'US', 'US', NULL, NULL, 'Trade Operations', '250K+', 'REJECTED', FALSE),
(7, 'Sophie Dubois', 'INDIVIDUAL', 'FR', '1995-01-30', 'FR', 'FR', 'Marketing Manager', 'Creative Agency', 'Employment Income', '25-50K', 'PENDING', TRUE),
(8, 'Nordic Logistics AB', 'CORPORATE', 'SE', '2012-08-14', 'SE', 'SE', NULL, NULL, 'Logistics Revenue', '250K+', 'PENDING', TRUE),
(9, 'Vanguard Heritage Foundation Trust', 'TRUST', 'CH', '2020-02-10', 'CH', 'CH', NULL, NULL, 'Trust Assets', '250K+', 'ACTIVE', TRUE),
(10, 'Minister Alexander Vance', 'POLITICAL', 'PL', '1970-03-18', 'PL', 'PL', 'Minister', 'Government of Poland', 'Government Salary', '100-250K', 'PENDING', TRUE);

-- 4. Insert Client Addresses
INSERT INTO client_address (address_id, client_id, address_type, line1, city, country, postcode, is_current) VALUES
(1, 1, 'REGISTERED', '10 Downing Street', 'London', 'UK', 'SW1A 2AA', 'TRUE'),
(2, 2, 'REGISTERED', '100 High Street', 'Manchester', 'UK', 'M1 1AD', 'TRUE'),
(3, 3, 'MAILING', 'Berliner Strasse 45', 'Berlin', 'DE', '10115', 'TRUE'),
(4, 4, 'REGISTERED', '12 Wealth Way', 'Edinburgh', 'UK', 'EH1 1YZ', 'TRUE'),
(5, 5, 'MAILING', '5th Avenue 12', 'New York', 'US', '10001', 'TRUE'),
(6, 9, 'REGISTERED', 'Paradeplatz 8', 'Zurich', 'CH', '8001', 'TRUE'),
(7, 10, 'MAILING', 'Wiejska 4/6', 'Warsaw', 'PL', '00-902', 'TRUE');

-- 5. Insert Onboarding Cases
INSERT INTO onboarding_case (case_id, client_id, opened_date, product_type, case_status, assigned_officer_id, due_date, completed_date) VALUES
(1, 1, '2026-07-01 09:00:00', 'RETAIL_BANKING', 'CLOSED', 1, '2026-07-15', '2026-07-10 14:30:00'),
(2, 2, '2026-08-01 10:00:00', 'CORPORATE_ACCOUNT', 'PENDING', 2, '2026-08-25', NULL),
(3, 3, '2026-08-10 11:30:00', 'RETAIL_BANKING', 'OPEN', 1, '2026-08-24', NULL),
(4, 4, '2026-06-10 08:45:00', 'WEALTH_MANAGEMENT', 'CLOSED', 3, '2026-06-25', '2026-06-20 16:00:00'),
(5, 5, '2026-07-15 13:15:00', 'WEALTH_MANAGEMENT', 'CLOSED', 2, '2026-07-30', '2026-07-28 11:00:00'),
(6, 9, '2026-08-09 14:00:00', 'TRUST_CUSTODY', 'OPEN', 3, '2026-08-23', NULL),
(7, 10, '2026-08-02 16:20:00', 'PRIVATE_BANKING', 'PENDING', 1, '2026-08-16', NULL);

-- 6. Insert Documents
INSERT INTO document (doc_id, case_id, doc_type_id, submission_date, verified_flag, verified_by, verified_at, expiry_date, rejection_reason) VALUES
(1, 1, 1, '2026-07-02 10:00:00', TRUE, 1, '2026-07-03 12:00:00', '2030-08-30', NULL),
(2, 2, 16, '2026-08-02 14:20:00', FALSE, NULL, NULL, NULL, NULL),
(3, 3, 3, '2026-08-10 12:00:00', FALSE, NULL, NULL, '2028-05-15', NULL),
(4, 4, 25, '2026-06-12 09:10:00', TRUE, 3, '2026-06-13 15:00:00', NULL, NULL),
(5, 5, 14, '2026-07-16 16:00:00', FALSE, 2, '2026-07-28 11:00:00', '2023-01-01', 'Document expired'),
(6, 6, 27, '2026-08-09 15:30:00', FALSE, NULL, NULL, NULL, NULL),
(7, 7, 15, '2026-08-03 11:00:00', TRUE, 1, '2026-08-04 09:45:00', NULL, NULL);

-- 7. Insert Risk Classifications
INSERT INTO risk_classification (classification_id, case_id, risk_level, classification_date, assessed_by, rationale, next_review_date) VALUES
(1, 1, 'LOW', '2026-07-03 12:30:00', 1, 'Standard individual client with valid identity document', '2027-07-03'),
(2, 2, 'MEDIUM', '2026-08-03 09:00:00', 2, 'Corporate entity operating in high volume sector', '2027-08-03'),
(3, 4, 'LOW', '2026-06-13 15:30:00', 3, 'Established trust structure with clear beneficiaries', '2027-06-13'),
(4, 5, 'HIGH', '2026-07-28 11:15:00', 2, 'PEP client provided expired documentation', '2026-10-28'),
(5, 7, 'HIGH', '2026-08-04 10:00:00', 1, 'PEP status requires enhanced due diligence', '2027-02-04');