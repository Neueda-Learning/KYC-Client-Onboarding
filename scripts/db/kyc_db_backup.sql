-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: kyc_db
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `client_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) DEFAULT NULL,
  `client_type` varchar(255) DEFAULT NULL COMMENT 'INDIVIDUAL / CORPORATE',
  `nationality` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `tax_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL COMMENT 'PENDING / ACTIVE / SUSPENDED / REJECTED',
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client`
--

LOCK TABLES `client` WRITE;
/*!40000 ALTER TABLE `client` DISABLE KEYS */;
/*!40000 ALTER TABLE `client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `client_address`
--

DROP TABLE IF EXISTS `client_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_address` (
  `address_id` int NOT NULL AUTO_INCREMENT,
  `client_id` int DEFAULT NULL,
  `address_type` varchar(255) DEFAULT NULL COMMENT 'REGISTERED / MAILING',
  `street` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `postcode` varchar(255) DEFAULT NULL,
  `effective_date` date DEFAULT NULL,
  PRIMARY KEY (`address_id`),
  KEY `client_id` (`client_id`),
  CONSTRAINT `client_address_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client_address`
--

LOCK TABLES `client_address` WRITE;
/*!40000 ALTER TABLE `client_address` DISABLE KEYS */;
/*!40000 ALTER TABLE `client_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `compliance_officer`
--

DROP TABLE IF EXISTS `compliance_officer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compliance_officer` (
  `officer_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`officer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `compliance_officer`
--

LOCK TABLES `compliance_officer` WRITE;
/*!40000 ALTER TABLE `compliance_officer` DISABLE KEYS */;
/*!40000 ALTER TABLE `compliance_officer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document`
--

DROP TABLE IF EXISTS `document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document` (
  `doc_id` int NOT NULL AUTO_INCREMENT,
  `case_id` int DEFAULT NULL,
  `doc_type_id` int DEFAULT NULL,
  `submission_date` timestamp NULL DEFAULT NULL,
  `verified_flag` tinyint(1) DEFAULT NULL,
  `verified_by` int DEFAULT NULL,
  `verified_at` timestamp NULL DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `rejection_reason` text,
  PRIMARY KEY (`doc_id`),
  KEY `case_id` (`case_id`),
  KEY `doc_type_id` (`doc_type_id`),
  KEY `verified_by` (`verified_by`),
  CONSTRAINT `document_ibfk_1` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`),
  CONSTRAINT `document_ibfk_2` FOREIGN KEY (`doc_type_id`) REFERENCES `document_type` (`doc_type_id`),
  CONSTRAINT `document_ibfk_3` FOREIGN KEY (`verified_by`) REFERENCES `compliance_officer` (`officer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document`
--

LOCK TABLES `document` WRITE;
/*!40000 ALTER TABLE `document` DISABLE KEYS */;
/*!40000 ALTER TABLE `document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_type`
--

DROP TABLE IF EXISTS `document_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_type` (
  `doc_type_id` int NOT NULL AUTO_INCREMENT,
  `doc_type_name` varchar(255) DEFAULT NULL COMMENT 'PASSPORT, DRIVING_LICENCE, UTILITY_BILL, COMPANY_REG',
  `required_for_individual` tinyint(1) DEFAULT NULL,
  `required_for_corporate` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`doc_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_type`
--

LOCK TABLES `document_type` WRITE;
/*!40000 ALTER TABLE `document_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `document_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `expiring_documents_vw`
--

DROP TABLE IF EXISTS `expiring_documents_vw`;
/*!50001 DROP VIEW IF EXISTS `expiring_documents_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `expiring_documents_vw` AS SELECT 
 1 AS `doc_id`,
 1 AS `case_id`,
 1 AS `doc_type_name`,
 1 AS `expiry_date`,
 1 AS `client_id`,
 1 AS `full_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `onboarding_case`
--

DROP TABLE IF EXISTS `onboarding_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `onboarding_case` (
  `case_id` int NOT NULL AUTO_INCREMENT,
  `client_id` int DEFAULT NULL,
  `opened_date` timestamp NULL DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `assigned_officer_id` int DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `completed_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`case_id`),
  KEY `client_id` (`client_id`),
  KEY `assigned_officer_id` (`assigned_officer_id`),
  CONSTRAINT `onboarding_case_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
  CONSTRAINT `onboarding_case_ibfk_2` FOREIGN KEY (`assigned_officer_id`) REFERENCES `compliance_officer` (`officer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `onboarding_case`
--

LOCK TABLES `onboarding_case` WRITE;
/*!40000 ALTER TABLE `onboarding_case` DISABLE KEYS */;
/*!40000 ALTER TABLE `onboarding_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `pending_cases_vw`
--

DROP TABLE IF EXISTS `pending_cases_vw`;
/*!50001 DROP VIEW IF EXISTS `pending_cases_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `pending_cases_vw` AS SELECT 
 1 AS `case_id`,
 1 AS `client_id`,
 1 AS `opened_date`,
 1 AS `status`,
 1 AS `assigned_officer_id`,
 1 AS `due_date`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `risk_classification`
--

DROP TABLE IF EXISTS `risk_classification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `risk_classification` (
  `classification_id` int NOT NULL AUTO_INCREMENT,
  `case_id` int DEFAULT NULL,
  `risk_level` varchar(255) DEFAULT NULL COMMENT 'LOW / MEDIUM / HIGH',
  `classification_date` timestamp NULL DEFAULT NULL,
  `assessed_by` int DEFAULT NULL,
  `rationale` text,
  `next_review_date` date DEFAULT NULL,
  PRIMARY KEY (`classification_id`),
  KEY `case_id` (`case_id`),
  KEY `assessed_by` (`assessed_by`),
  CONSTRAINT `risk_classification_ibfk_1` FOREIGN KEY (`case_id`) REFERENCES `onboarding_case` (`case_id`),
  CONSTRAINT `risk_classification_ibfk_2` FOREIGN KEY (`assessed_by`) REFERENCES `compliance_officer` (`officer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `risk_classification`
--

LOCK TABLES `risk_classification` WRITE;
/*!40000 ALTER TABLE `risk_classification` DISABLE KEYS */;
/*!40000 ALTER TABLE `risk_classification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `expiring_documents_vw`
--

/*!50001 DROP VIEW IF EXISTS `expiring_documents_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `expiring_documents_vw` AS select `d`.`doc_id` AS `doc_id`,`d`.`case_id` AS `case_id`,`dt`.`doc_type_name` AS `doc_type_name`,`d`.`expiry_date` AS `expiry_date`,`c`.`client_id` AS `client_id`,`c`.`full_name` AS `full_name` from (((`document` `d` join `onboarding_case` `oc` on((`d`.`case_id` = `oc`.`case_id`))) join `client` `c` on((`oc`.`client_id` = `c`.`client_id`))) join `document_type` `dt` on((`d`.`doc_type_id` = `dt`.`doc_type_id`))) where (`d`.`expiry_date` between curdate() and (curdate() + interval 60 day)) */;
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
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `pending_cases_vw` AS select `onboarding_case`.`case_id` AS `case_id`,`onboarding_case`.`client_id` AS `client_id`,`onboarding_case`.`opened_date` AS `opened_date`,`onboarding_case`.`status` AS `status`,`onboarding_case`.`assigned_officer_id` AS `assigned_officer_id`,`onboarding_case`.`due_date` AS `due_date` from `onboarding_case` where (`onboarding_case`.`status` in ('OPEN','AWAITING_DOCUMENTS')) order by `onboarding_case`.`due_date` */;
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

-- Dump completed on 2026-08-10 17:04:54
