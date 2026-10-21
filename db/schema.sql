CREATE DATABASE  IF NOT EXISTS `bulitipper2` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */;
USE `bulitipper2`;
-- MySQL dump 10.13  Distrib 8.0.12, for Win64 (x86_64)
--
-- Host: localhost    Database: bulitipper2
-- ------------------------------------------------------
-- Server version	8.0.12

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `community`
--

DROP TABLE IF EXISTS `community`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `community` (
                             `created_on` datetime(6) DEFAULT NULL,
                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `description` varchar(255) DEFAULT NULL,
                             `name` varchar(255) DEFAULT NULL,
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=854 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_membership`
--

DROP TABLE IF EXISTS `community_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `community_membership` (
                                        `created_on` datetime(6) DEFAULT NULL,
                                        `fk_comm_id` bigint(20) NOT NULL,
                                        `fk_tipper_id` bigint(20) NOT NULL,
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                        PRIMARY KEY (`id`),
                                        KEY `FK_COMM_MEMB_TO_COMMUNITY` (`fk_comm_id`),
                                        KEY `FK_COMM_MEMB_TO_TIPPER` (`fk_tipper_id`),
                                        CONSTRAINT `FK_COMM_MEMB_TO_COMMUNITY` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`),
                                        CONSTRAINT `FK_COMM_MEMB_TO_TIPPER` FOREIGN KEY (`fk_tipper_id`) REFERENCES `tipper` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=235 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition`
--

DROP TABLE IF EXISTS `competition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition` (
                               `remis_multiplicator` int(11) NOT NULL,
                               `win_multiplicator` int(11) NOT NULL,
                               `competition_family_id` bigint(20) NOT NULL,
                               `created_on` datetime(6) NOT NULL,
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `description` varchar(255) DEFAULT NULL,
                               `name` varchar(255) NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FK_COMP_TO_FAM` (`competition_family_id`),
                               CONSTRAINT `FK_COMP_TO_FAM` FOREIGN KEY (`competition_family_id`) REFERENCES `competition_family` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1292 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_family`
--

DROP TABLE IF EXISTS `competition_family`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition_family` (
                                      `has_clubs` bit(1) NOT NULL,
                                      `has_liga_modus` bit(1) NOT NULL,
                                      `created_on` datetime(6) NOT NULL,
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `country` varchar(255) DEFAULT NULL,
                                      `description` varchar(255) DEFAULT NULL,
                                      `name` varchar(255) NOT NULL,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `UKekh2mj9mpxak23erwxk1f7di3` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_group`
--

DROP TABLE IF EXISTS `competition_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition_group` (
                                     `group_number` int(11) NOT NULL,
                                     `created_on` datetime(6) NOT NULL,
                                     `fk_comp_round` bigint(20) NOT NULL,
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `name` varchar(255) NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `FKm73wgq2rqv9w4is76fmbq7htn` (`fk_comp_round`),
                                     CONSTRAINT `FKm73wgq2rqv9w4is76fmbq7htn` FOREIGN KEY (`fk_comp_round`) REFERENCES `competition_round` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_membership`
--

DROP TABLE IF EXISTS `competition_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition_membership` (
                                          `created_on` datetime(6) DEFAULT NULL,
                                          `fk_comm_id` bigint(20) NOT NULL,
                                          `fk_comp_id` bigint(20) NOT NULL,
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                          PRIMARY KEY (`id`),
                                          KEY `FK_COMP_MEMB_TO_COMMUNITY` (`fk_comm_id`),
                                          KEY `FK_COMP_MEMB_TO_COMP` (`fk_comp_id`),
                                          CONSTRAINT `FK_COMP_MEMB_TO_COMMUNITY` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`),
                                          CONSTRAINT `FK_COMP_MEMB_TO_COMP` FOREIGN KEY (`fk_comp_id`) REFERENCES `competition` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=244 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_round`
--

DROP TABLE IF EXISTS `competition_round`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition_round` (
                                     `first_matchday` int(11) NOT NULL,
                                     `has_groups` bit(1) NOT NULL,
                                     `matchdays_size` int(11) NOT NULL,
                                     `round_number` int(11) NOT NULL,
                                     `teams_size` int(11) NOT NULL,
                                     `created_on` datetime(6) NOT NULL,
                                     `fk_comp` bigint(20) NOT NULL,
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `name` varchar(255) NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `FK_ROUND_TO_COMP` (`fk_comp`),
                                     CONSTRAINT `FK_ROUND_TO_COMP` FOREIGN KEY (`fk_comp`) REFERENCES `competition` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=502 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_team`
--

DROP TABLE IF EXISTS `competition_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `competition_team` (
                                    `created_on` datetime(6) NOT NULL,
                                    `fk_comp` bigint(20) NOT NULL,
                                    `fk_team` bigint(20) NOT NULL,
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                    PRIMARY KEY (`id`),
                                    KEY `FK_COMP_TEAM_TO_COMP` (`fk_comp`),
                                    KEY `FK_COMP_TEAM_TO_TEAM` (`fk_team`),
                                    CONSTRAINT `FK_COMP_TEAM_TO_COMP` FOREIGN KEY (`fk_comp`) REFERENCES `competition` (`id`),
                                    CONSTRAINT `FK_COMP_TEAM_TO_TEAM` FOREIGN KEY (`fk_team`) REFERENCES `team` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hibernate_sequences`
--

DROP TABLE IF EXISTS `hibernate_sequences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `hibernate_sequences` (
                                       `next_val` bigint(20) DEFAULT NULL,
                                       `sequence_name` varchar(255) NOT NULL,
                                       PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `role` (
                        `created_on` datetime(6) DEFAULT NULL,
                        `fk_comm_id` bigint(20) DEFAULT NULL,
                        `fk_comp_id` bigint(20) DEFAULT NULL,
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `role_types` varchar(31) NOT NULL,
                        `description` varchar(255) DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        KEY `FK_COMM_ROLE_TO_COMM` (`fk_comm_id`),
                        KEY `FK_COMP_ROLE_TO_COMP` (`fk_comp_id`),
                        CONSTRAINT `FK_COMM_ROLE_TO_COMM` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`),
                        CONSTRAINT `FK_COMP_ROLE_TO_COMP` FOREIGN KEY (`fk_comp_id`) REFERENCES `competition` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1884 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spiel`
--

DROP TABLE IF EXISTS `spiel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `spiel` (
                         `gast_tore` int(11) NOT NULL,
                         `heim_tore` int(11) NOT NULL,
                         `spiel_number` int(11) NOT NULL,
                         `stattgefunden` bit(1) NOT NULL,
                         `anpfiffdate` datetime(6) DEFAULT NULL,
                         `competition_group_id` bigint(20) DEFAULT NULL,
                         `created_on` datetime(6) NOT NULL,
                         `fk_gast_team_id` bigint(20) DEFAULT NULL,
                         `fk_heim_team_id` bigint(20) DEFAULT NULL,
                         `fk_spieltag_id` bigint(20) DEFAULT NULL,
                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
                         PRIMARY KEY (`id`),
                         KEY `FKtfy938hfi3odo51a6gfsaip2o` (`competition_group_id`),
                         KEY `FK_SPIEL_TO_GAST_TEAM` (`fk_gast_team_id`),
                         KEY `FK_SPIEL_TO_HEIM_TEAM` (`fk_heim_team_id`),
                         KEY `FK_SPIEL_TO_SPIELTAG` (`fk_spieltag_id`),
                         CONSTRAINT `FK_SPIEL_TO_GAST_TEAM` FOREIGN KEY (`fk_gast_team_id`) REFERENCES `team` (`id`),
                         CONSTRAINT `FK_SPIEL_TO_HEIM_TEAM` FOREIGN KEY (`fk_heim_team_id`) REFERENCES `team` (`id`),
                         CONSTRAINT `FK_SPIEL_TO_SPIELTAG` FOREIGN KEY (`fk_spieltag_id`) REFERENCES `spieltag` (`id`),
                         CONSTRAINT `FKtfy938hfi3odo51a6gfsaip2o` FOREIGN KEY (`competition_group_id`) REFERENCES `competition_group` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spiel_formula`
--

DROP TABLE IF EXISTS `spiel_formula`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `spiel_formula` (
                                 `diff_tore` int(11) NOT NULL,
                                 `gast_tore` int(11) NOT NULL,
                                 `heim_tore` int(11) NOT NULL,
                                 `is_heim_team` bit(1) NOT NULL,
                                 `lost` int(11) NOT NULL,
                                 `points` int(11) NOT NULL,
                                 `remis` int(11) NOT NULL,
                                 `won` int(11) NOT NULL,
                                 `created_on` datetime(6) NOT NULL,
                                 `fk_spiel_id` bigint(20) NOT NULL,
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `team_name` varchar(255) NOT NULL,
                                 `team_name_acronym` varchar(255) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `FK_FORMULA_TO_SPIEL` (`fk_spiel_id`),
                                 CONSTRAINT `FK_FORMULA_TO_SPIEL` FOREIGN KEY (`fk_spiel_id`) REFERENCES `spiel` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2149 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spieltag`
--

DROP TABLE IF EXISTS `spieltag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `spieltag` (
                            `spieltag_number` int(11) NOT NULL,
                            `created_on` datetime(6) NOT NULL,
                            `fk_comp_round_id` bigint(20) NOT NULL,
                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `start_date` datetime(6) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            KEY `FK_SPIELTAG_TO_ROUND` (`fk_comp_round_id`),
                            CONSTRAINT `FK_SPIELTAG_TO_ROUND` FOREIGN KEY (`fk_comp_round_id`) REFERENCES `competition_round` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=686 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `team` (
                        `has_club` bit(1) NOT NULL,
                        `created_on` datetime(6) NOT NULL,
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `acronym` varchar(255) NOT NULL,
                        `name` varchar(255) NOT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `UKmqe3y99p5v0r98d8wnjcolgcl` (`acronym`),
                        UNIQUE KEY `UKg2l9qqsoeuynt4r5ofdt1x2td` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=964 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipp`
--

DROP TABLE IF EXISTS `tipp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipp` (
                        `gast_tipp` int(11) DEFAULT NULL,
                        `heim_tipp` int(11) DEFAULT NULL,
                        `remis_tipp` int(11) DEFAULT NULL,
                        `win_points` int(11) DEFAULT NULL,
                        `created_on` datetime(6) NOT NULL,
                        `fk_comm_memb_id` bigint(20) NOT NULL,
                        `fk_spiel_id` bigint(20) NOT NULL,
                        `fk_tipp_modus_id` bigint(20) NOT NULL,
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        PRIMARY KEY (`id`),
                        KEY `FK_TIPP_TO_COMP_MEMB` (`fk_comm_memb_id`),
                        KEY `FK_TIPP_TO_GAME` (`fk_spiel_id`),
                        CONSTRAINT `FK_TIPP_TO_COMP_MEMB` FOREIGN KEY (`fk_comm_memb_id`) REFERENCES `community_membership` (`id`),
                        CONSTRAINT `FK_TIPP_TO_GAME` FOREIGN KEY (`fk_spiel_id`) REFERENCES `spiel` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=190 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipp_config`
--

DROP TABLE IF EXISTS `tipp_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipp_config` (
                               `created_on` datetime(6) NOT NULL,
                               `fk_comp_memb` bigint(20) NOT NULL,
                               `fk_matchday` bigint(20) NOT NULL,
                               `fk_tipp_modus` bigint(20) NOT NULL,
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               PRIMARY KEY (`id`),
                               KEY `FK_TIPP_CONFIG_TO_MATCHDAY` (`fk_comp_memb`),
                               KEY `FK_TIPP_CONFIG_TO_COMP_MEMB` (`fk_matchday`),
                               CONSTRAINT `FK_TIPP_CONFIG_TO_COMP_MEMB` FOREIGN KEY (`fk_matchday`) REFERENCES `spieltag` (`id`),
                               CONSTRAINT `FK_TIPP_CONFIG_TO_MATCHDAY` FOREIGN KEY (`fk_comp_memb`) REFERENCES `competition_membership` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=168 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipp_modus_point`
--

DROP TABLE IF EXISTS `tipp_modus_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipp_modus_point` (
                                    `deadline` int(11) DEFAULT NULL,
                                    `total_points` int(11) DEFAULT NULL,
                                    `created_on` datetime(6) NOT NULL,
                                    `fk_comm_id` bigint(20) NOT NULL,
                                    `id` bigint(20) NOT NULL,
                                    `name` varchar(255) NOT NULL,
                                    `type` varchar(255) DEFAULT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `UniqueNameAndCommunity` (`name`,`fk_comm_id`),
                                    KEY `FKllyx78r54qm4tqu4migk8fagt` (`fk_comm_id`),
                                    CONSTRAINT `FKllyx78r54qm4tqu4migk8fagt` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipp_modus_result`
--

DROP TABLE IF EXISTS `tipp_modus_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipp_modus_result` (
                                     `bonus_points` int(11) DEFAULT NULL,
                                     `deadline` int(11) DEFAULT NULL,
                                     `tendency_points` int(11) DEFAULT NULL,
                                     `created_on` datetime(6) NOT NULL,
                                     `fk_comm_id` bigint(20) NOT NULL,
                                     `id` bigint(20) NOT NULL,
                                     `name` varchar(255) NOT NULL,
                                     `type` varchar(255) DEFAULT NULL,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `UniqueNameAndCommunity` (`name`,`fk_comm_id`),
                                     KEY `FKgtv83qb48yjkwcjdjqu29ff8q` (`fk_comm_id`),
                                     CONSTRAINT `FKgtv83qb48yjkwcjdjqu29ff8q` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipp_modus_toto`
--

DROP TABLE IF EXISTS `tipp_modus_toto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipp_modus_toto` (
                                   `deadline` int(11) DEFAULT NULL,
                                   `created_on` datetime(6) NOT NULL,
                                   `fk_comm_id` bigint(20) NOT NULL,
                                   `id` bigint(20) NOT NULL,
                                   `name` varchar(255) NOT NULL,
                                   `type` varchar(255) DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `UniqueNameAndCommunity` (`name`,`fk_comm_id`),
                                   KEY `FK6499pav01d3vq0psrgi6amo17` (`fk_comm_id`),
                                   CONSTRAINT `FK6499pav01d3vq0psrgi6amo17` FOREIGN KEY (`fk_comm_id`) REFERENCES `community` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipper`
--

DROP TABLE IF EXISTS `tipper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipper` (
                          `is_community_admin` bit(1) NOT NULL,
                          `is_competition_admin` bit(1) NOT NULL,
                          `default_community_id` bigint(20) DEFAULT NULL,
                          `default_competition_id` bigint(20) DEFAULT NULL,
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `email` varchar(255) DEFAULT NULL,
                          `firstname` varchar(255) NOT NULL,
                          `lastname` varchar(255) NOT NULL,
                          `passwort` varchar(255) NOT NULL,
                          `passwort_hint` varchar(255) DEFAULT NULL,
                          `username` varchar(255) NOT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKhork5clv1ekoyi0nb28vmsn0m` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=529 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipper_role`
--

DROP TABLE IF EXISTS `tipper_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
SET character_set_client = utf8mb4 ;
CREATE TABLE `tipper_role` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `rolle_id` bigint(20) NOT NULL,
                               `tipper_id` bigint(20) NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FK_TIPPER_ROLE_TO_ROLE` (`rolle_id`),
                               KEY `FK_TIPPER_ROLE_TO_TIPPER` (`tipper_id`),
                               CONSTRAINT `FK_TIPPER_ROLE_TO_ROLE` FOREIGN KEY (`rolle_id`) REFERENCES `role` (`id`),
                               CONSTRAINT `FK_TIPPER_ROLE_TO_TIPPER` FOREIGN KEY (`tipper_id`) REFERENCES `tipper` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-21 14:47:27
