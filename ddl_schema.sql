CREATE TABLE `compliance_officer` (
  `officer_id` integer PRIMARY KEY AUTO_INCREMENT,
  `full_name` varchar(255),
  `email` varchar(255)
);

CREATE TABLE `document_type` (
  `doc_type_id` integer PRIMARY KEY AUTO_INCREMENT,
  `doc_type_name` varchar(255) COMMENT 'PASSPORT, DRIVING_LICENCE, UTILITY_BILL, COMPANY_REG',
  `required_for_individual` boolean,
  `required_for_corporate` boolean
);

CREATE TABLE `client` (
  `client_id` integer PRIMARY KEY AUTO_INCREMENT,
  `full_name` varchar(255),
  `client_type` varchar(255) COMMENT 'INDIVIDUAL / CORPORATE',
  `nationality` varchar(255),
  `date_of_birth` date,
  `tax_id` varchar(255),
  `status` varchar(255) COMMENT 'PENDING / ACTIVE / SUSPENDED / REJECTED'
);

CREATE TABLE `client_address` (
  `address_id` integer PRIMARY KEY AUTO_INCREMENT,
  `client_id` integer,
  `address_type` varchar(255) COMMENT 'REGISTERED / MAILING',
  `street` varchar(255),
  `city` varchar(255),
  `country` varchar(255),
  `postcode` varchar(255),
  `effective_date` date
);

CREATE TABLE `onboarding_case` (
  `case_id` integer PRIMARY KEY AUTO_INCREMENT,
  `client_id` integer,
  `opened_date` timestamp,
  `status` varchar(255),
  `assigned_officer_id` integer,
  `due_date` date,
  `completed_date` timestamp
);

CREATE TABLE `document` (
  `doc_id` integer PRIMARY KEY AUTO_INCREMENT,
  `case_id` integer,
  `doc_type_id` integer,
  `submission_date` timestamp,
  `verified_flag` boolean,
  `verified_by` integer,
  `verified_at` timestamp,
  `expiry_date` date,
  `rejection_reason` text
);

CREATE TABLE `risk_classification` (
  `classification_id` integer PRIMARY KEY AUTO_INCREMENT,
  `case_id` integer,
  `risk_level` varchar(255) COMMENT 'LOW / MEDIUM / HIGH',
  `classification_date` timestamp,
  `assessed_by` integer,
  `rationale` text,
  `next_review_date` date
);

ALTER TABLE `client_address` ADD FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`);

ALTER TABLE `onboarding_case` ADD FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`);

ALTER TABLE `onboarding_case` ADD FOREIGN KEY (`assigned_officer_id`) REFERENCES `compliance_officer` (`officer_id`);

ALTER TABLE `document` ADD FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`);

ALTER TABLE `document` ADD FOREIGN KEY (`doc_type_id`) REFERENCES `document_type` (`doc_type_id`);

ALTER TABLE `document` ADD FOREIGN KEY (`verified_by`) REFERENCES `compliance_officer` (`officer_id`);

ALTER TABLE `risk_classification` ADD FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`);

ALTER TABLE `risk_classification` ADD FOREIGN KEY (`assessed_by`) REFERENCES `compliance_officer` (`officer_id`);