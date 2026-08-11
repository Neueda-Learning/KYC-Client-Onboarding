SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `risk_classification`;
DROP TABLE IF EXISTS `document`;
DROP TABLE IF EXISTS `onboarding_case`;
DROP TABLE IF EXISTS `client_address`;
DROP TABLE IF EXISTS `compliance_officer`;
DROP TABLE IF EXISTS `document_type`;
DROP TABLE IF EXISTS `client`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `compliance_officer` (
  `officer_id` integer PRIMARY KEY AUTO_INCREMENT,
  `full_name` varchar(255),
  `email` varchar(255),
  `password` varchar(255)
);

CREATE TABLE `document_type` (
  `doc_type_id` integer PRIMARY KEY AUTO_INCREMENT,
  `doc_type_name` varchar(255) NOT NULL COMMENT 'PASSPORT, DRIVING_LICENCE, NATIONAL_ID, UTILITY_BILL, BANK_STATEMENT, COUNCIL_TAX_BILL, KYC_APPLICATION_FORM, TAX_SELF_CERT_FATCA_CRS, PAY_SLIP_TAX_RETURN, SHARE_PURCHASE_AGREEMENT, PROBATE_WILL, DEED_OF_SALE, ACCOUNTANT_NET_ASSET_DECLARATION, OFFICIAL_GOVT_APPOINTMENT_LETTER, PEP_BUSINESS_RATIONALE_STMT, CERTIFICATE_OF_INCORPORATION, MEMORANDUM_ARTICLES_ASSOCIATION, COMMERCIAL_REGISTER_EXTRACT, CERTIFICATE_OF_GOOD_STANDING, CORPORATE_GROUP_OWNERSHIP_CHART, UBO_DECLARATION_FORM, BOARD_RESOLUTION_ACCOUNT_OPENING, AUTHORIZED_SIGNATORY_LIST, AUDITED_FINANCIAL_STATEMENTS, TRUST_DEED, DEED_OF_VARIATION, LETTER_OF_WISHES',
  `required_for_individual` boolean NOT NULL,
  `required_for_corporate` boolean NOT NULL,
  `required_for_trust` boolean NOT NULL,
  `required_for_political` boolean NOT NULL
);

CREATE TABLE `client` (
  `client_id` integer PRIMARY KEY AUTO_INCREMENT,
  `full_name` varchar(255) NOT NULL,
  `client_type` varchar(255) COMMENT 'INDIVIDUAL / CORPORATE / TRUST / POLITICAL',
  `nationality` char(2) NOT NULL,
  `date_of_birth` date NOT NULL,
  `country_of_birth` char(2) NOT NULL,
  `tax_residency` char(2) NOT NULL,
  `occupation` varchar(80),
  `employer` varchar(80),
  `main_source_of_funds` varchar(80),
  `annual_income_band` varchar(80) COMMENT '<25K / 25-50K  / 50-100K / 100-250K / 250K+',
  `status` varchar(255) COMMENT 'PENDING / ACTIVE / SUSPENDED / REJECTED',
  `is_active` boolean NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_client_status ON client (status);
CREATE INDEX idx_client_tax_id ON client (client_id);

CREATE TABLE `client_address` (
  `address_id` integer PRIMARY KEY AUTO_INCREMENT,
  `client_id` integer NOT NULL,
  `address_type` varchar(255) COMMENT 'REGISTERED / MAILING ',
  `line1` varchar(255) NOT NULL,
  `line2` varchar(255),
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `state` varchar(255),
  `postcode` varchar(255),
  `is_current` varchar(255) NOT NULL DEFAULT TRUE,
  CONSTRAINT `fk_address_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`) ON DELETE CASCADE
);
CREATE INDEX idx_client_address_client_id ON client_address (client_id);

CREATE TABLE `onboarding_case` (
  `case_id` INT PRIMARY KEY AUTO_INCREMENT,
  `client_id` INT NOT NULL,
  `opened_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `product_type` VARCHAR(255) NOT NULL,
  `case_status` VARCHAR(20) NOT NULL COMMENT 'OPEN / PENDING / CLOSED',
  `assigned_officer_id` INT,
  `due_date` DATE,
  `completed_date` TIMESTAMP NULL DEFAULT NULL,
  `rejection_reason` VARCHAR(200),
  CONSTRAINT `fk_case_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
  CONSTRAINT `fk_case_officer` FOREIGN KEY (`assigned_officer_id`) REFERENCES `compliance_officer` (`officer_id`)
);

CREATE INDEX idx_onboarding_case_client_id ON onboarding_case (client_id);
CREATE INDEX idx_onboarding_case_assigned_officer_id ON onboarding_case (assigned_officer_id);
CREATE INDEX idx_onboarding_case_status ON onboarding_case (case_status);

CREATE TABLE `document` (
  `doc_id` integer PRIMARY KEY AUTO_INCREMENT,
  `case_id` integer,
  `doc_type_id` integer NOT NULL,
  `submission_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `verified_flag` boolean NOT NULL,
  `verified_by` integer,
  `verified_at` timestamp NULL DEFAULT NULL,
  `expiry_date` date,
  `rejection_reason` text,
  CONSTRAINT `fk_doc_case` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_doc_type` FOREIGN KEY (`doc_type_id`) REFERENCES `document_type` (`doc_type_id`),
  CONSTRAINT `fk_doc_verifier` FOREIGN KEY (`verified_by`) REFERENCES `compliance_officer` (`officer_id`)
);

CREATE INDEX idx_document_case_id ON document (case_id);
CREATE INDEX idx_document_verified_by ON document (verified_by);

CREATE TABLE `risk_classification` (
  `classification_id` integer PRIMARY KEY AUTO_INCREMENT,
  `case_id` integer,
  `risk_level` varchar(255) COMMENT 'LOW / MEDIUM / HIGH',
  `classification_date` timestamp,
  `assessed_by` integer,
  `rationale` text,
  `next_review_date` date,
  CONSTRAINT `fk_risk_case` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_risk_assessor` FOREIGN KEY (`assessed_by`) REFERENCES `compliance_officer` (`officer_id`)
);
CREATE INDEX idx_risk_classification_case_id ON risk_classification (case_id);
CREATE INDEX idx_risk_classification_assessed_by ON risk_classification (assessed_by);