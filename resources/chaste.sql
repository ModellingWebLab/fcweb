-- phpMyAdmin SQL Dump
-- version 3.4.10.1deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Dec 09, 2013 at 06:25 PM
-- Server version: 5.5.34
-- PHP Version: 5.3.10-1ubuntu3.8

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `chaste`
--

-- --------------------------------------------------------

--
-- Table structure for table `experiments`
--

CREATE TABLE IF NOT EXISTS `experiments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `model` int(11) NOT NULL,
  `protocol` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `author` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `model_2` (`model`,`protocol`),
  KEY `model` (`model`),
  KEY `protocol` (`protocol`),
  KEY `author` (`author`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `experimentversions`
--

CREATE TABLE IF NOT EXISTS `experimentversions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `author` int(11) NOT NULL,
  `experiment` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `filepath` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `status` enum('RUNNING','SUCCESS','FAILED','INAPPRORIATE') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'RUNNING',
  `returnmsg` text COLLATE utf8_unicode_ci NOT NULL,
  `visibility` enum('PRIVATE','RESTRICTED','PUBLIC') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'PUBLIC',
  PRIMARY KEY (`id`),
  KEY `author` (`author`),
  KEY `experiment` (`experiment`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `experiment_files`
--

CREATE TABLE IF NOT EXISTS `experiment_files` (
  `experiment` int(11) NOT NULL,
  `file` int(11) NOT NULL,
  UNIQUE KEY `result` (`experiment`,`file`),
  KEY `file` (`file`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `files`
--

CREATE TABLE IF NOT EXISTS `files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `relpath` varchar(150) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type` text COLLATE utf8_unicode_ci NOT NULL,
  `author` int(11) NOT NULL,
  `size` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `author` (`author`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `models`
--

CREATE TABLE IF NOT EXISTS `models` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `author` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `author` (`author`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `modelversions`
--

CREATE TABLE IF NOT EXISTS `modelversions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `author` int(11) NOT NULL,
  `model` int(11) NOT NULL,
  `version` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `filepath` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `visibility` enum('PRIVATE','RESTRICTED','PUBLIC') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'RESTRICTED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `filepath` (`filepath`),
  UNIQUE KEY `model_2` (`model`,`version`),
  KEY `author` (`author`),
  KEY `model` (`model`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `model_files`
--

CREATE TABLE IF NOT EXISTS `model_files` (
  `model` int(11) NOT NULL,
  `file` int(11) NOT NULL,
  UNIQUE KEY `model` (`model`,`file`),
  KEY `file` (`file`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `protocols`
--

CREATE TABLE IF NOT EXISTS `protocols` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `author` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `author` (`author`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `protocolversions`
--

CREATE TABLE IF NOT EXISTS `protocolversions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `author` int(11) NOT NULL,
  `protocol` int(11) NOT NULL,
  `version` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `filepath` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `visibility` enum('PRIVATE','RESTRICTED','PUBLIC') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'RESTRICTED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `filepath` (`filepath`),
  UNIQUE KEY `protocol` (`protocol`,`version`),
  KEY `author` (`author`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `protocol_files`
--

CREATE TABLE IF NOT EXISTS `protocol_files` (
  `protocol` int(11) NOT NULL,
  `file` int(11) NOT NULL,
  UNIQUE KEY `protocol` (`protocol`,`file`),
  KEY `file` (`file`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

CREATE TABLE IF NOT EXISTS `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mail` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `acronym` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `institution` text COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `role` enum('ADMIN','GUEST','READER','MODELER') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'GUEST',
  `cookie` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `givenName` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `familyName` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `sendMails` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `mail` (`mail`),
  UNIQUE KEY `acronym` (`acronym`),
  UNIQUE KEY `cookie` (`cookie`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `experiments`
--
ALTER TABLE `experiments`
  ADD CONSTRAINT `experiments_ibfk_1` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `experiments_ibfk_2` FOREIGN KEY (`model`) REFERENCES `modelversions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `experiments_ibfk_3` FOREIGN KEY (`protocol`) REFERENCES `protocolversions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `experimentversions`
--
ALTER TABLE `experimentversions`
  ADD CONSTRAINT `experimentversions_ibfk_1` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `experimentversions_ibfk_2` FOREIGN KEY (`experiment`) REFERENCES `experiments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `experiment_files`
--
ALTER TABLE `experiment_files`
  ADD CONSTRAINT `experiment_files_ibfk_2` FOREIGN KEY (`file`) REFERENCES `files` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `experiment_files_ibfk_3` FOREIGN KEY (`experiment`) REFERENCES `experimentversions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `files`
--
ALTER TABLE `files`
  ADD CONSTRAINT `files_ibfk_1` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `models`
--
ALTER TABLE `models`
  ADD CONSTRAINT `models_ibfk_1` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `modelversions`
--
ALTER TABLE `modelversions`
  ADD CONSTRAINT `modelversions_ibfk_1` FOREIGN KEY (`model`) REFERENCES `models` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `userconst` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `model_files`
--
ALTER TABLE `model_files`
  ADD CONSTRAINT `model_files_ibfk_1` FOREIGN KEY (`model`) REFERENCES `modelversions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `model_files_ibfk_2` FOREIGN KEY (`file`) REFERENCES `files` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `protocols`
--
ALTER TABLE `protocols`
  ADD CONSTRAINT `protocols_ibfk_1` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `protocolversions`
--
ALTER TABLE `protocolversions`
  ADD CONSTRAINT `protocolversions_ibfk_1` FOREIGN KEY (`protocol`) REFERENCES `protocols` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `userconst2` FOREIGN KEY (`author`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `protocol_files`
--
ALTER TABLE `protocol_files`
  ADD CONSTRAINT `protocol_files_ibfk_1` FOREIGN KEY (`protocol`) REFERENCES `protocolversions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `protocol_files_ibfk_2` FOREIGN KEY (`file`) REFERENCES `files` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;



-- create root user:
INSERT INTO `chaste`.`user` (`mail` ,`password` ,`acronym` ,`institution` ,`created` ,`role` ,`cookie` ,`givenName` ,`familyName` ,`sendMails`) VALUES ('root@localhost', MD5( 'admin' ) , 'root', '', CURRENT_TIMESTAMP , 'ADMIN', UUID(), '', '', '0');


/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
