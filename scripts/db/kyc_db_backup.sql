-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: kyc_db
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `client`
--

DROP TABLE IF EXISTS `client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `client` (
  `client_id` int(11) NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) NOT NULL,
  `client_type` varchar(255) DEFAULT NULL COMMENT 'INDIVIDUAL / CORPORATE / TRUST / POLITICAL',
  `nationality` char(2) NOT NULL,
  `date_of_birth` date NOT NULL,
  `country_of_birth` char(2) NOT NULL,
  `tax_residency` char(2) NOT NULL,
  `occupation` varchar(80) DEFAULT NULL,
  `employer` varchar(80) DEFAULT NULL,
  `main_source_of_funds` varchar(80) DEFAULT NULL,
  `annual_income_band` varchar(80) DEFAULT NULL COMMENT '<25K / 25-50K  / 50-100K / 100-250K / 250K+',
  `status` varchar(255) DEFAULT NULL COMMENT 'PENDING / ACTIVE / SUSPENDED / REJECTED',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`client_id`),
  KEY `idx_client_status` (`status`),
  KEY `idx_client_tax_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client`
--

LOCK TABLES `client` WRITE;
/*!40000 ALTER TABLE `client` DISABLE KEYS */;
INSERT INTO `client` VALUES (1,'Michael Brown','INDIVIDUAL','UK','1985-04-12','UK','UK','Software Engineer','Tech Corp','Employment Income','50-100K','ACTIVE',1),(2,'TechCorp Solutions Ltd','CORPORATE','UK','2010-01-01','UK','UK',NULL,NULL,'Business Revenue','250K+','PENDING',1),(3,'Elena Rostova','INDIVIDUAL','DE','1992-09-25','DE','DE','Financial Analyst','Bank DE','Employment Income','50-100K','PENDING',1),(4,'The Sterling Family Trust','TRUST','UK','2018-06-15','UK','UK',NULL,NULL,'Investment Portfolio','250K+','ACTIVE',1),(5,'Senator David Wilson','POLITICAL','US','1978-11-03','US','US','Senator','US Senate','Government Salary','100-250K','SUSPENDED',0),(6,'Global Import Export LLC','CORPORATE','US','2018-03-20','US','US',NULL,NULL,'Trade Operations','250K+','REJECTED',0),(7,'Sophie Dubois','INDIVIDUAL','FR','1995-01-30','FR','FR','Marketing Manager','Creative Agency','Employment Income','25-50K','PENDING',1),(8,'Nordic Logistics AB','CORPORATE','SE','2012-08-14','SE','SE',NULL,NULL,'Logistics Revenue','250K+','PENDING',1),(9,'Vanguard Heritage Foundation Trust','TRUST','CH','2020-02-10','CH','CH',NULL,NULL,'Trust Assets','250K+','ACTIVE',1),(10,'Minister Alexander Vance','POLITICAL','PL','1970-03-18','PL','PL','Minister','Government of Poland','Government Salary','100-250K','PENDING',1);
/*!40000 ALTER TABLE `client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `client_address`
--

DROP TABLE IF EXISTS `client_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `client_address` (
  `address_id` int(11) NOT NULL AUTO_INCREMENT,
  `client_id` int(11) NOT NULL,
  `address_type` varchar(255) DEFAULT NULL COMMENT 'REGISTERED / MAILING ',
  `line1` varchar(255) NOT NULL,
  `line2` varchar(255) DEFAULT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `state` varchar(255) DEFAULT NULL,
  `postcode` varchar(255) DEFAULT NULL,
  `is_current` varchar(255) NOT NULL DEFAULT '1',
  PRIMARY KEY (`address_id`),
  KEY `idx_client_address_client_id` (`client_id`),
  CONSTRAINT `fk_address_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client_address`
--

LOCK TABLES `client_address` WRITE;
/*!40000 ALTER TABLE `client_address` DISABLE KEYS */;
INSERT INTO `client_address` VALUES (1,1,'REGISTERED','10 Downing Street',NULL,'London','UK',NULL,'SW1A 2AA','TRUE'),(2,2,'REGISTERED','100 High Street',NULL,'Manchester','UK',NULL,'M1 1AD','TRUE'),(3,3,'MAILING','Berliner Strasse 45',NULL,'Berlin','DE',NULL,'10115','TRUE'),(4,4,'REGISTERED','12 Wealth Way',NULL,'Edinburgh','UK',NULL,'EH1 1YZ','TRUE'),(5,5,'MAILING','5th Avenue 12',NULL,'New York','US',NULL,'10001','TRUE'),(6,9,'REGISTERED','Paradeplatz 8',NULL,'Zurich','CH',NULL,'8001','TRUE'),(7,10,'MAILING','Wiejska 4/6',NULL,'Warsaw','PL',NULL,'00-902','TRUE');
/*!40000 ALTER TABLE `client_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `compliance_officer`
--

DROP TABLE IF EXISTS `compliance_officer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `compliance_officer` (
  `officer_id` int(11) NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`officer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `compliance_officer`
--

LOCK TABLES `compliance_officer` WRITE;
/*!40000 ALTER TABLE `compliance_officer` DISABLE KEYS */;
INSERT INTO `compliance_officer` VALUES (1,'John Smith','john.smith@bank.com','hashed_pass_1'),(2,'Anna Novak','anna.novak@bank.com','hashed_pass_2'),(3,'Robert Taylor','robert.taylor@bank.com','hashed_pass_3');
/*!40000 ALTER TABLE `compliance_officer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document`
--

DROP TABLE IF EXISTS `document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document` (
  `doc_id` int(11) NOT NULL AUTO_INCREMENT,
  `case_id` int(11) DEFAULT NULL,
  `doc_type_id` int(11) NOT NULL,
  `submission_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `verified_flag` tinyint(1) NOT NULL,
  `verified_by` int(11) DEFAULT NULL,
  `verified_at` timestamp NULL DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `rejection_reason` text DEFAULT NULL,
  PRIMARY KEY (`doc_id`),
  KEY `fk_doc_type` (`doc_type_id`),
  KEY `idx_document_case_id` (`case_id`),
  KEY `idx_document_verified_by` (`verified_by`),
  CONSTRAINT `fk_doc_case` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_doc_type` FOREIGN KEY (`doc_type_id`) REFERENCES `document_type` (`doc_type_id`),
  CONSTRAINT `fk_doc_verifier` FOREIGN KEY (`verified_by`) REFERENCES `compliance_officer` (`officer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document`
--

LOCK TABLES `document` WRITE;
/*!40000 ALTER TABLE `document` DISABLE KEYS */;
INSERT INTO `document` VALUES (1,1,1,'2026-07-02 08:00:00',1,1,'2026-07-03 10:00:00','2030-08-30',NULL),(2,2,16,'2026-08-02 12:20:00',0,NULL,NULL,NULL,NULL),(3,3,3,'2026-08-10 10:00:00',0,NULL,NULL,'2028-05-15',NULL),(4,4,25,'2026-06-12 07:10:00',1,3,'2026-06-13 13:00:00',NULL,NULL),(5,5,14,'2026-07-16 14:00:00',0,2,'2026-07-28 09:00:00','2023-01-01','Document expired'),(6,6,27,'2026-08-09 13:30:00',0,NULL,NULL,NULL,NULL),(7,7,15,'2026-08-03 09:00:00',1,1,'2026-08-04 07:45:00',NULL,NULL);
/*!40000 ALTER TABLE `document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_type`
--

DROP TABLE IF EXISTS `document_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document_type` (
  `doc_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `doc_type_name` varchar(255) NOT NULL COMMENT 'PASSPORT, DRIVING_LICENCE, NATIONAL_ID, UTILITY_BILL, BANK_STATEMENT, COUNCIL_TAX_BILL, KYC_APPLICATION_FORM, TAX_SELF_CERT_FATCA_CRS, PAY_SLIP_TAX_RETURN, SHARE_PURCHASE_AGREEMENT, PROBATE_WILL, DEED_OF_SALE, ACCOUNTANT_NET_ASSET_DECLARATION, OFFICIAL_GOVT_APPOINTMENT_LETTER, PEP_BUSINESS_RATIONALE_STMT, CERTIFICATE_OF_INCORPORATION, MEMORANDUM_ARTICLES_ASSOCIATION, COMMERCIAL_REGISTER_EXTRACT, CERTIFICATE_OF_GOOD_STANDING, CORPORATE_GROUP_OWNERSHIP_CHART, UBO_DECLARATION_FORM, BOARD_RESOLUTION_ACCOUNT_OPENING, AUTHORIZED_SIGNATORY_LIST, AUDITED_FINANCIAL_STATEMENTS, TRUST_DEED, DEED_OF_VARIATION, LETTER_OF_WISHES',
  `required_for_individual` tinyint(1) NOT NULL,
  `required_for_corporate` tinyint(1) NOT NULL,
  `required_for_trust` tinyint(1) NOT NULL,
  `required_for_political` tinyint(1) NOT NULL,
  PRIMARY KEY (`doc_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_type`
--

LOCK TABLES `document_type` WRITE;
/*!40000 ALTER TABLE `document_type` DISABLE KEYS */;
INSERT INTO `document_type` VALUES (1,'PASSPORT',1,1,1,1),(2,'DRIVING_LICENCE',1,1,1,1),(3,'NATIONAL_ID',1,1,1,1),(4,'UTILITY_BILL',1,1,1,1),(5,'BANK_STATEMENT',1,1,1,1),(6,'COUNCIL_TAX_BILL',1,0,0,0),(7,'KYC_APPLICATION_FORM',1,1,1,1),(8,'TAX_SELF_CERT_FATCA_CRS',1,1,1,1),(9,'PAY_SLIP_TAX_RETURN',1,0,0,1),(10,'SHARE_PURCHASE_AGREEMENT',1,0,0,1),(11,'PROBATE_WILL',1,0,1,0),(12,'DEED_OF_SALE',1,0,0,1),(13,'ACCOUNTANT_NET_ASSET_DECLARATION',1,0,0,1),(14,'OFFICIAL_GOVT_APPOINTMENT_LETTER',0,0,0,1),(15,'PEP_BUSINESS_RATIONALE_STMT',0,0,0,1),(16,'CERTIFICATE_OF_INCORPORATION',0,1,0,0),(17,'MEMORANDUM_ARTICLES_ASSOCIATION',0,1,0,0),(18,'COMMERCIAL_REGISTER_EXTRACT',0,1,0,0),(19,'CERTIFICATE_OF_GOOD_STANDING',0,1,0,0),(20,'CORPORATE_GROUP_OWNERSHIP_CHART',0,1,0,0),(21,'UBO_DECLARATION_FORM',0,1,0,0),(22,'BOARD_RESOLUTION_ACCOUNT_OPENING',0,1,0,0),(23,'AUTHORIZED_SIGNATORY_LIST',0,1,1,0),(24,'AUDITED_FINANCIAL_STATEMENTS',0,1,0,0),(25,'TRUST_DEED',0,0,1,0),(26,'DEED_OF_VARIATION',0,0,1,0),(27,'LETTER_OF_WISHES',0,0,1,0);
/*!40000 ALTER TABLE `document_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary table structure for view `expiring_documents_vw`
--

DROP TABLE IF EXISTS `expiring_documents_vw`;
/*!50001 DROP VIEW IF EXISTS `expiring_documents_vw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `expiring_documents_vw` AS SELECT
 1 AS `doc_id`,
  1 AS `case_id`,
  1 AS `doc_type_name`,
  1 AS `expiry_date`,
  1 AS `client_id`,
  1 AS `full_name` */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `onboarding_case`
--

DROP TABLE IF EXISTS `onboarding_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `onboarding_case` (
  `case_id` int(11) NOT NULL AUTO_INCREMENT,
  `client_id` int(11) NOT NULL,
  `opened_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `product_type` varchar(255) NOT NULL,
  `case_status` varchar(20) NOT NULL COMMENT 'OPEN / PENDING / CLOSED',
  `assigned_officer_id` int(11) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `completed_date` timestamp NULL DEFAULT NULL,
  `rejection_reason` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`case_id`),
  KEY `idx_onboarding_case_client_id` (`client_id`),
  KEY `idx_onboarding_case_assigned_officer_id` (`assigned_officer_id`),
  KEY `idx_onboarding_case_status` (`case_status`),
  CONSTRAINT `fk_case_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
  CONSTRAINT `fk_case_officer` FOREIGN KEY (`assigned_officer_id`) REFERENCES `compliance_officer` (`officer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `onboarding_case`
--

LOCK TABLES `onboarding_case` WRITE;
/*!40000 ALTER TABLE `onboarding_case` DISABLE KEYS */;
INSERT INTO `onboarding_case` VALUES (1,1,'2026-07-01 07:00:00','RETAIL_BANKING','CLOSED',1,'2026-07-15','2026-07-10 12:30:00',NULL),(2,2,'2026-08-01 08:00:00','CORPORATE_ACCOUNT','PENDING',2,'2026-08-25',NULL,NULL),(3,3,'2026-08-10 09:30:00','RETAIL_BANKING','OPEN',1,'2026-08-24',NULL,NULL),(4,4,'2026-06-10 06:45:00','WEALTH_MANAGEMENT','CLOSED',3,'2026-06-25','2026-06-20 14:00:00',NULL),(5,5,'2026-07-15 11:15:00','WEALTH_MANAGEMENT','CLOSED',2,'2026-07-30','2026-07-28 09:00:00',NULL),(6,9,'2026-08-09 12:00:00','TRUST_CUSTODY','OPEN',3,'2026-08-23',NULL,NULL),(7,10,'2026-08-02 14:20:00','PRIVATE_BANKING','PENDING',1,'2026-08-16',NULL,NULL);
/*!40000 ALTER TABLE `onboarding_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary table structure for view `pending_cases_vw`
--

DROP TABLE IF EXISTS `pending_cases_vw`;
/*!50001 DROP VIEW IF EXISTS `pending_cases_vw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `pending_cases_vw` AS SELECT
 1 AS `case_id`,
  1 AS `client_id`,
  1 AS `opened_date`,
  1 AS `product_type`,
  1 AS `case_status`,
  1 AS `assigned_officer_id`,
  1 AS `due_date` */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `risk_classification`
--

DROP TABLE IF EXISTS `risk_classification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `risk_classification` (
  `classification_id` int(11) NOT NULL AUTO_INCREMENT,
  `case_id` int(11) DEFAULT NULL,
  `risk_level` varchar(255) DEFAULT NULL COMMENT 'LOW / MEDIUM / HIGH',
  `classification_date` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `assessed_by` int(11) DEFAULT NULL,
  `rationale` text DEFAULT NULL,
  `next_review_date` date DEFAULT NULL,
  PRIMARY KEY (`classification_id`),
  KEY `idx_risk_classification_case_id` (`case_id`),
  KEY `idx_risk_classification_assessed_by` (`assessed_by`),
  CONSTRAINT `fk_risk_assessor` FOREIGN KEY (`assessed_by`) REFERENCES `compliance_officer` (`officer_id`),
  CONSTRAINT `fk_risk_case` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `risk_classification`
--

LOCK TABLES `risk_classification` WRITE;
/*!40000 ALTER TABLE `risk_classification` DISABLE KEYS */;
INSERT INTO `risk_classification` VALUES (1,1,'LOW','2026-07-03 10:30:00',1,'Standard individual client with valid identity document','2027-07-03'),(2,2,'MEDIUM','2026-08-03 07:00:00',2,'Corporate entity operating in high volume sector','2027-08-03'),(3,4,'LOW','2026-06-13 13:30:00',3,'Established trust structure with clear beneficiaries','2027-06-13'),(4,5,'HIGH','2026-07-28 09:15:00',2,'PEP client provided expired documentation','2026-10-28'),(5,7,'HIGH','2026-08-04 08:00:00',1,'PEP status requires enhanced due diligence','2027-02-04');
/*!40000 ALTER TABLE `risk_classification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `expiring_documents_vw`
--

/*!50001 DROP VIEW IF EXISTS `expiring_documents_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = cp850 */;
/*!50001 SET character_set_results     = cp850 */;
/*!50001 SET collation_connection      = cp850_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `expiring_documents_vw` AS select `d`.`doc_id` AS `doc_id`,`d`.`case_id` AS `case_id`,`dt`.`doc_type_name` AS `doc_type_name`,`d`.`expiry_date` AS `expiry_date`,`c`.`client_id` AS `client_id`,`c`.`full_name` AS `full_name` from (((`document` `d` join `onboarding_case` `oc` on(`d`.`case_id` = `oc`.`case_id`)) join `client` `c` on(`oc`.`client_id` = `c`.`client_id`)) join `document_type` `dt` on(`d`.`doc_type_id` = `dt`.`doc_type_id`)) where `c`.`is_active` = 1 and `d`.`expiry_date` between curdate() and curdate() + interval 60 day */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `pending_cases_vw`
--

/*!50001 DROP VIEW IF EXISTS `pending_cases_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = cp850 */;
/*!50001 SET character_set_results     = cp850 */;
/*!50001 SET collation_connection      = cp850_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `pending_cases_vw` AS select `onboarding_case`.`case_id` AS `case_id`,`onboarding_case`.`client_id` AS `client_id`,`onboarding_case`.`opened_date` AS `opened_date`,`onboarding_case`.`product_type` AS `product_type`,`onboarding_case`.`case_status` AS `case_status`,`onboarding_case`.`assigned_officer_id` AS `assigned_officer_id`,`onboarding_case`.`due_date` AS `due_date` from `onboarding_case` where `onboarding_case`.`case_status` in ('OPEN','AWAITING_DOCUMENTS') order by `onboarding_case`.`due_date` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-12 10:53:41
