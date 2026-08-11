USE kyc_db;

-- Clear existing data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE risk_classification;
TRUNCATE TABLE document;
TRUNCATE TABLE onboarding_case;
TRUNCATE TABLE client_address;
TRUNCATE TABLE client;
TRUNCATE TABLE document_type;
TRUNCATE TABLE compliance_officer;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Insert Compliance Officers
INSERT INTO compliance_officer (officer_id, full_name, email) VALUES
(1, 'John Smith', 'john.smith@bank.com'),
(2, 'Anna Novak', 'anna.novak@bank.com'),
(3, 'Robert Taylor', 'robert.taylor@bank.com');

-- 2. Insert Document Types
INSERT INTO document_type (doc_type_id, doc_type_name, required_for_individual, required_for_corporate) VALUES
(1, 'PASSPORT', TRUE, FALSE),
(2, 'DRIVING_LICENCE', TRUE, FALSE),
(3, 'UTILITY_BILL', TRUE, TRUE),
(4, 'COMPANY_REGISTRATION', FALSE, TRUE);

-- 3. Insert 10 Clients
INSERT INTO client (client_id, full_name, client_type, nationality, date_of_birth, tax_id, status) VALUES
(1, 'Michael Brown', 'INDIVIDUAL', 'UK', '1985-04-12', 'TAX-UK-001', 'ACTIVE'),
(2, 'TechCorp Solutions Ltd', 'CORPORATE', 'UK', NULL, 'GB123456789', 'PENDING'),
(3, 'Elena Rostova', 'INDIVIDUAL', 'DE', '1992-09-25', 'TAX-DE-882', 'PENDING'),
(4, 'Alpha Trading Group', 'CORPORATE', 'PL', NULL, 'PL987654321', 'ACTIVE'),
(5, 'David Wilson', 'INDIVIDUAL', 'US', '1978-11-03', 'TAX-US-441', 'SUSPENDED'),
(6, 'Global Import Export LLC', 'CORPORATE', 'US', NULL, 'US554433221', 'REJECTED'),
(7, 'Sophie Dubois', 'INDIVIDUAL', 'FR', '1995-01-30', 'TAX-FR-901', 'PENDING'),
(8, 'Nordic Logistics AB', 'CORPORATE', 'SE', NULL, 'SE112233445', 'PENDING'),
(9, 'James Miller', 'INDIVIDUAL', 'UK', '1988-06-18', 'TAX-UK-773', 'ACTIVE'),
(10, 'Innovate AI Sp. z o.o.', 'CORPORATE', 'PL', NULL, 'PL555444333', 'PENDING');

-- 4. Insert Client Addresses
INSERT INTO client_address (address_id, client_id, address_type, street, city, country, postcode, effective_date) VALUES
(1, 1, 'REGISTERED', '10 Downing Street', 'London', 'UK', 'SW1A 2AA', '2024-01-01'),
(2, 2, 'REGISTERED', '100 High Street', 'Manchester', 'UK', 'M1 1AD', '2024-01-15'),
(3, 3, 'MAILING', 'Berliner Strasse 45', 'Berlin', 'DE', '10115', '2024-02-01'),
(4, 4, 'REGISTERED', 'Aleje Jerozolimskie 50', 'Warsaw', 'PL', '00-024', '2024-02-10'),
(5, 5, 'MAILING', '5th Avenue 12', 'New York', 'US', '10001', '2024-03-01');

-- 5. Insert Onboarding Cases
INSERT INTO onboarding_case (case_id, client_id, opened_date, status, assigned_officer_id, due_date, completed_date) VALUES
(1, 1, '2026-07-01 09:00:00', 'COMPLETED', 1, '2026-07-15', '2026-07-10 14:30:00'),
(2, 2, '2026-08-01 10:00:00', 'AWAITING_DOCUMENTS', 2, '2026-08-25', NULL),
(3, 3, '2026-08-05 11:30:00', 'OPEN', 1, '2026-08-20', NULL),
(4, 4, '2026-06-10 08:45:00', 'COMPLETED', 3, '2026-06-25', '2026-06-20 16:00:00'),
(5, 5, '2026-07-15 13:15:00', 'REJECTED', 2, '2026-07-30', '2026-07-28 11:00:00');

-- 6. Insert Documents
INSERT INTO document (doc_id, case_id, doc_type_id, submission_date, verified_flag, verified_by, verified_at, expiry_date, rejection_reason) VALUES
(1, 1, 1, '2026-07-02 10:00:00', TRUE, 1, '2026-07-03 12:00:00', '2026-08-30', NULL),
(2, 2, 4, '2026-08-02 14:20:00', FALSE, NULL, NULL, '2027-08-02', NULL),
(3, 4, 3, '2026-06-12 09:10:00', TRUE, 3, '2026-06-13 15:00:00', '2026-09-01', NULL),
(4, 5, 1, '2026-07-16 16:00:00', FALSE, 2, '2026-07-28 11:00:00', '2023-01-01', 'Document expired');

-- 7. Insert Risk Classifications
INSERT INTO risk_classification (classification_id, case_id, risk_level, classification_date, assessed_by, rationale, next_review_date) VALUES
(1, 1, 'LOW', '2026-07-03 12:30:00', 1, 'Standard individual client with valid identity document', '2027-07-03'),
(2, 2, 'MEDIUM', '2026-08-03 09:00:00', 2, 'Corporate entity operating in high volume sector', '2027-08-03'),
(3, 5, 'HIGH', '2026-07-28 11:15:00', 2, 'Expired identification document provided', '2026-10-28');