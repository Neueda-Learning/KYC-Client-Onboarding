USE kyc_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `risk_classification`;
TRUNCATE TABLE `document`;
TRUNCATE TABLE `onboarding_case`;
TRUNCATE TABLE `client_address`;
TRUNCATE TABLE `client`;
TRUNCATE TABLE `document_type`;
TRUNCATE TABLE `compliance_officer`;
TRUNCATE TABLE `admin_officer`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Insert Compliance Officers
-- password_hash values are PBKDF2-HMAC-SHA256 (see util.PasswordHasher); plaintext passwords are not stored here.
INSERT INTO compliance_officer (officer_id, full_name, email, username, password_hash) VALUES
(1, 'John Smith', 'john.smith@bank.com', 'john.smith', 'pbkdf2_sha256$210000$ac1rG6uAi30eexRN6DPn2A==$fRT14v48H0So1gsLkeudf/DEnJOnT/NjcdaA5PCntsk='),
(2, 'Anna Novak', 'anna.novak@bank.com', 'anna.novak', 'pbkdf2_sha256$210000$mFcYynSFXo+XREKGrt+kgg==$989wv/ruCm/n5J7pXJpGUthcCCTLaY2c4MrM4XvJIEM='),
(3, 'Robert Taylor', 'robert.taylor@bank.com', 'robert.taylor', 'pbkdf2_sha256$210000$CPBUc30gW8vpSrlyerO0vA==$CqNCnSSJGm+tN7iz+esWEFvHbymraXoa3vlTDhyvSls='),
(4, 'Priya Kapoor', 'priya.kapoor@bank.com', 'priya.kapoor', 'pbkdf2_sha256$210000$tBJhhl5o77EGg7amtrvt5w==$sN0HnkkfytXSfPUth0UeAoEOyqDhG1lUBWhKTZ+3DRw='),
(5, 'Lucas Meyer', 'lucas.meyer@bank.com', 'lucas.meyer', 'pbkdf2_sha256$210000$TKQbRBbjN6UuzP1ePR1OUQ==$V5HDTpdobniv7184dH3j7b35NtoMwzwWLDm1xvE+L3c=');

-- 1b. Insert Admin Compliance Officer (separate table; sees and can assign every case)
INSERT INTO admin_officer (admin_id, full_name, email, username, password_hash) VALUES
(1, 'Grace Whitman', 'grace.whitman@bank.com', 'grace.whitman', 'pbkdf2_sha256$210000$eT9sS87Oc2DmjeT85+SryA==$L+GUbGbPbiGTy18LQcLHL+Xrv9yuOT0JQorGG2+1zMM=');

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

-- 3. Insert Clients (Updated to match new schema columns)
-- password_hash values are PBKDF2-HMAC-SHA256 (see util.PasswordHasher); plaintext passwords are not stored here.
INSERT INTO client (client_id, full_name, client_type, nationality, date_of_birth, country_of_birth, tax_residency, occupation, employer, main_source_of_funds, annual_income_band, status, is_active, username, password_hash) VALUES
(1, 'Michael Brown', 'INDIVIDUAL', 'UK', '1985-04-12', 'UK', 'UK', 'Software Engineer', 'Tech Corp', 'Employment Income', '50-100K', 'ACTIVE', TRUE, 'michael.brown', 'pbkdf2_sha256$210000$NwBbN/p/LDqdIpSuFfidAA==$zfB5osfx1gaK+jVhP9e5gCL0yFTu7t8SZx0VdWMEVqI='),
(2, 'TechCorp Solutions Ltd', 'CORPORATE', 'UK', '2010-01-01', 'UK', 'UK', NULL, NULL, 'Business Revenue', '250K+', 'PENDING', TRUE, 'techcorp.admin', 'pbkdf2_sha256$210000$gjVYeq40FVFm1xtW5nRzDQ==$lfrXi2BYuPq/DK8XQjAbFHKuB44GN5rzIntThjF7NE0='),
(3, 'Elena Rostova', 'INDIVIDUAL', 'DE', '1992-09-25', 'DE', 'DE', 'Financial Analyst', 'Bank DE', 'Employment Income', '50-100K', 'PENDING', TRUE, 'elena.rostova', 'pbkdf2_sha256$210000$fujgtFRl6CzqwJ9Af+vrVA==$y6knjfZdXEmdhacXNUAG//EVSwd4z8Hj3epeU8ai2OY='),
(4, 'The Sterling Family Trust', 'TRUST', 'UK', '2018-06-15', 'UK', 'UK', NULL, NULL, 'Investment Portfolio', '250K+', 'ACTIVE', TRUE, 'sterling.trust', 'pbkdf2_sha256$210000$LDfepZTUkh+JlOyvCDsHJQ==$L9rDyxUATLXy7LjHgiAPp/hWPCSoKksR8pjXMhjMCGw='),
(5, 'Senator David Wilson', 'POLITICAL', 'US', '1978-11-03', 'US', 'US', 'Senator', 'US Senate', 'Government Salary', '100-250K', 'SUSPENDED', FALSE, 'david.wilson', 'pbkdf2_sha256$210000$xTNjvuzCFII+LQv0FEiXKg==$Vo0JG2ao7wfepYMSgATnrHmuHkGu8JqKmIeN8bLhDZ0='),
(6, 'Global Import Export LLC', 'CORPORATE', 'US', '2018-03-20', 'US', 'US', NULL, NULL, 'Trade Operations', '250K+', 'REJECTED', FALSE, 'global.importexport', 'pbkdf2_sha256$210000$7oHeK/WwqIIr/KaaCXH0rw==$HIGwWQEPJedj861W2niFBpDKqwJYvAEQ7NjVP6oYIGE='),
(7, 'Sophie Dubois', 'INDIVIDUAL', 'FR', '1995-01-30', 'FR', 'FR', 'Marketing Manager', 'Creative Agency', 'Employment Income', '25-50K', 'PENDING', TRUE, 'sophie.dubois', 'pbkdf2_sha256$210000$bK2BVUZbFv9DYseb6Nsu+Q==$Wnw7N9uMKoRlv5IeIqm1/OGIXWb2oyyBE1CEf73ssL4='),
(8, 'Nordic Logistics AB', 'CORPORATE', 'SE', '2012-08-14', 'SE', 'SE', NULL, NULL, 'Logistics Revenue', '250K+', 'PENDING', TRUE, 'nordic.logistics', 'pbkdf2_sha256$210000$HeXKZuyT0+GZPWCRVXAVNQ==$QGZlvoK/aMTGQPiEn1/vqs5+5tz/7WPsQvQsOsU6V24='),
(9, 'Vanguard Heritage Foundation Trust', 'TRUST', 'CH', '2020-02-10', 'CH', 'CH', NULL, NULL, 'Trust Assets', '250K+', 'ACTIVE', TRUE, 'vanguard.trust', 'pbkdf2_sha256$210000$GwEFJjCQsZjglgiPu3gjnA==$1f0OyBcvtDu3WcwGAT9gRTxJUngba4HSqdB20pRo5Ng='),
(10, 'Minister Alexander Vance', 'POLITICAL', 'PL', '1970-03-18', 'PL', 'PL', 'Minister', 'Government of Poland', 'Government Salary', '100-250K', 'PENDING', TRUE, 'alexander.vance', 'pbkdf2_sha256$210000$7gl5yK9W7Iz6Z8ZBoQsXmw==$Rj3bmJqKrVb9GsJBJP9DpHbyyQchmFnGscmbBCDoFO4='),
(11, 'Laura Bennett', 'INDIVIDUAL', 'GB', '1988-05-22', 'GB', 'GB', 'Teacher', 'City Council', 'Employment Income', '25-50K', 'PENDING', TRUE, 'laura.bennett', 'pbkdf2_sha256$210000$Gpk+r/bjbLFZgm8uyDZiCA==$jqzzPqRQRrZCp7/CZzim2eHWM09J5oZBuAiKPMPUBlI='),
(12, 'Skyline Retail Group Ltd', 'CORPORATE', 'US', '2015-09-01', 'US', 'US', NULL, NULL, 'Retail Revenue', '250K+', 'PENDING', TRUE, 'skyline.retail', 'pbkdf2_sha256$210000$HVgh994ho4wSJrzdJif+Vg==$OjuAlP4A6OnhVMwzDbjX1zYqwQCkJc+tLCGR6b3JWQc='),
(13, 'Hiroshi Tanaka', 'INDIVIDUAL', 'JP', '1990-12-03', 'JP', 'JP', 'Architect', 'Tanaka Design', 'Employment Income', '50-100K', 'PENDING', TRUE, 'hiroshi.tanaka', 'pbkdf2_sha256$210000$NQGfjnUjt+uZMtvgDzD01A==$ILakRxcxoeOHFDEuzfAsdNfXUPQ5h3mPHbl/wqfrsw8='),
(14, 'The Whitfield Trust', 'TRUST', 'GB', '2019-04-11', 'GB', 'GB', NULL, NULL, 'Trust Assets', '250K+', 'PENDING', TRUE, 'whitfield.trust', 'pbkdf2_sha256$210000$CIlYaD8YZ9J18IZEVt3gXw==$BtazvsdvP3Q6pzfqA9SZ5BawN5AJTjFRsCPnB5Uwvgo='),
(15, 'Ambassador Maria Santos', 'POLITICAL', 'ES', '1975-07-19', 'ES', 'ES', 'Ambassador', 'Government of Spain', 'Government Salary', '100-250K', 'ACTIVE', TRUE, 'maria.santos', 'pbkdf2_sha256$210000$q7YX/Q5GuIEr7ULaXpO6Cg==$sW8sEL+wkLKj0khCgbSrd+tW2Y8rH50t5nNdsgxnZGg=');

-- 4. Insert Client Addresses
INSERT INTO client_address (address_id, client_id, address_type, line1, city, country, postcode, is_current) VALUES
(1, 1, 'REGISTERED', '10 Downing Street', 'London', 'UK', 'SW1A 2AA', 'TRUE'),
(2, 2, 'REGISTERED', '100 High Street', 'Manchester', 'UK', 'M1 1AD', 'TRUE'),
(3, 3, 'MAILING', 'Berliner Strasse 45', 'Berlin', 'DE', '10115', 'TRUE'),
(4, 4, 'REGISTERED', '12 Wealth Way', 'Edinburgh', 'UK', 'EH1 1YZ', 'TRUE'),
(5, 5, 'MAILING', '5th Avenue 12', 'New York', 'US', '10001', 'TRUE'),
(6, 9, 'REGISTERED', 'Paradeplatz 8', 'Zurich', 'CH', '8001', 'TRUE'),
(7, 10, 'MAILING', 'Wiejska 4/6', 'Warsaw', 'PL', '00-902', 'TRUE'),
(8, 11, 'REGISTERED', '22 Baker Street', 'London', 'GB', 'NW1 6XE', 'TRUE'),
(9, 12, 'REGISTERED', '500 Market Street', 'Chicago', 'US', '60601', 'TRUE'),
(10, 13, 'MAILING', '3-2 Marunouchi', 'Tokyo', 'JP', '100-0005', 'TRUE'),
(11, 14, 'REGISTERED', '7 Whitfield Gardens', 'Bristol', 'GB', 'BS1 4ND', 'TRUE'),
(12, 15, 'MAILING', 'Calle Serrano 20', 'Madrid', 'ES', '28001', 'TRUE');

-- 5. Insert Onboarding Cases
INSERT INTO onboarding_case (case_id, client_id, opened_date, product_type, case_status, assigned_officer_id, due_date, completed_date) VALUES
(1, 1, '2026-07-01 09:00:00', 'RETAIL_BANKING', 'CLOSED', 1, '2026-07-15', '2026-07-10 14:30:00'),
(2, 2, '2026-08-01 10:00:00', 'CORPORATE_ACCOUNT', 'PENDING', 2, '2026-08-25', NULL),
(3, 3, '2026-08-10 11:30:00', 'RETAIL_BANKING', 'OPEN', 1, '2026-08-24', NULL),
(4, 4, '2026-06-10 08:45:00', 'WEALTH_MANAGEMENT', 'CLOSED', 3, '2026-06-25', '2026-06-20 16:00:00'),
(5, 5, '2026-07-15 13:15:00', 'WEALTH_MANAGEMENT', 'CLOSED', 2, '2026-07-30', '2026-07-28 11:00:00'),
(6, 9, '2026-08-09 14:00:00', 'TRUST_CUSTODY', 'OPEN', 3, '2026-08-23', NULL),
(7, 10, '2026-08-02 16:20:00', 'PRIVATE_BANKING', 'PENDING', 1, '2026-08-16', NULL),
(8, 11, '2026-08-05 09:30:00', 'RETAIL_BANKING', 'OPEN', 4, '2026-09-05', NULL),
(9, 12, '2026-08-06 10:15:00', 'CORPORATE_ACCOUNT', 'PENDING', 5, '2026-09-10', NULL),
(10, 13, '2026-08-07 13:00:00', 'PRIVATE_BANKING', 'OPEN', 1, '2026-09-01', NULL),
(11, 14, '2026-08-08 15:45:00', 'TRUST_CUSTODY', 'PENDING', 2, '2026-09-15', NULL),
(12, 15, '2026-07-20 08:00:00', 'PRIVATE_BANKING', 'CLOSED', 3, '2026-08-20', '2026-08-18 12:00:00');

-- 6. Insert Documents
INSERT INTO document (doc_id, case_id, doc_type_id, submission_date, verified_flag, verified_by, verified_at, expiry_date, rejection_reason) VALUES
(1, 1, 1, '2026-07-02 10:00:00', TRUE, 1, '2026-07-03 12:00:00', '2030-08-30', NULL),
(2, 2, 16, '2026-08-02 14:20:00', FALSE, NULL, NULL, NULL, NULL),
(3, 3, 3, '2026-08-10 12:00:00', FALSE, NULL, NULL, '2028-05-15', NULL),
(4, 4, 25, '2026-06-12 09:10:00', TRUE, 3, '2026-06-13 15:00:00', '2026-08-31', NULL),
(5, 5, 14, '2026-07-16 16:00:00', FALSE, 2, '2026-07-28 11:00:00', '2023-01-01', 'Document expired'),
(6, 6, 27, '2026-08-09 15:30:00', FALSE, NULL, NULL, NULL, NULL),
(7, 7, 15, '2026-08-03 11:00:00', TRUE, 1, '2026-08-04 09:45:00', NULL, NULL),
(8, 8, 1, '2026-08-05 10:00:00', FALSE, NULL, NULL, '2029-04-01', NULL),
(9, 9, 16, '2026-08-06 11:00:00', FALSE, NULL, NULL, NULL, NULL),
(10, 10, 3, '2026-08-07 14:00:00', TRUE, 1, '2026-08-08 09:00:00', '2027-12-03', NULL),
(11, 11, 25, '2026-08-08 16:30:00', FALSE, NULL, NULL, NULL, NULL),
(12, 12, 15, '2026-07-21 09:00:00', TRUE, 3, '2026-07-22 10:00:00', NULL, NULL);

-- 7. Insert Risk Classifications
INSERT INTO risk_classification (classification_id, case_id, risk_level, classification_date, assessed_by, rationale, next_review_date) VALUES
(1, 1, 'LOW', '2026-07-03 12:30:00', 1, 'Standard individual client with valid identity document', '2027-07-03'),
(2, 2, 'MEDIUM', '2026-08-03 09:00:00', 2, 'Corporate entity operating in high volume sector', '2027-08-03'),
(3, 4, 'LOW', '2026-06-13 15:30:00', 3, 'Established trust structure with clear beneficiaries', '2027-06-13'),
(4, 5, 'HIGH', '2026-07-28 11:15:00', 2, 'PEP client provided expired documentation', '2026-10-28'),
(5, 7, 'HIGH', '2026-08-04 10:00:00', 1, 'PEP status requires enhanced due diligence', '2027-02-04'),
(6, 10, 'LOW', '2026-08-08 09:15:00', 1, 'Standard individual client with valid identity document', '2027-08-08'),
(7, 12, 'HIGH', '2026-07-22 10:15:00', 3, 'Ambassador status requires enhanced due diligence', '2027-01-22');