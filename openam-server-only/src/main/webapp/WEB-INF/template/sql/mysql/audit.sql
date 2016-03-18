-- -----------------------------------------------------
-- The contents of this file are subject to the terms of the Common Development and
-- Distribution License (the License). You may not use this file except in compliance with the
-- License.
--
-- You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
-- specific language governing permission and limitations under the License.
--
-- When distributing Covered Software, include this CDDL Header Notice in each file and include
-- the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
-- Header, with the fields enclosed by brackets [] replaced by your own identifying
-- information: "Portions copyright [year] [name of copyright owner]".
--
-- Copyright 2016 ForgeRock AS.
-- Copyright 2016 Nomura Research Institute, Ltd.
-- -----------------------------------------------------
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `audit` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;
USE `audit` ;

-- -----------------------------------------------------
-- Table `audit`.`am_auditauthentication`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit`.`am_auditauthentication` (
  `id` VARCHAR(56) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(50) NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` MEDIUMTEXT,
  `result` VARCHAR(255) NULL ,
  `principals` MEDIUMTEXT ,
  `context` MEDIUMTEXT ,
  `entries` MEDIUMTEXT ,
  `component` VARCHAR(50) NULL ,
  `realm` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_am_auditauthentication_transactionid` (`transactionid` ASC)
)
  ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `audit`.`am_auditactivity`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`am_auditactivity` (
  `id` VARCHAR(56) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(255) NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` MEDIUMTEXT,
  `runas` VARCHAR(255) NULL ,
  `objectid` VARCHAR(255) NULL ,
  `operation` VARCHAR(255) NULL ,
  `beforeObject` MEDIUMTEXT NULL ,
  `afterObject` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `rev` VARCHAR(255) NULL ,
  `component` VARCHAR(50) NULL ,
  `realm` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_am_auditactivity_transactionid` (`transactionid` ASC)
)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `audit`.`am_auditaccess`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`am_auditaccess` (
  `id` VARCHAR(56) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(255) ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` MEDIUMTEXT,
  `server_ip` VARCHAR(40) ,
  `server_port` VARCHAR(5) ,
  `client_host` VARCHAR(255) ,
  `client_ip` VARCHAR(40) ,
  `client_port` VARCHAR(5) ,
  `request_protocol` VARCHAR(255) NULL ,
  `request_operation` VARCHAR(255) NULL ,
  `request_detail` TEXT NULL ,
  `http_request_secure` BOOLEAN NULL ,
  `http_request_method` VARCHAR(7) NULL ,
  `http_request_path` VARCHAR(255) NULL ,
  `http_request_queryparameters` MEDIUMTEXT NULL ,
  `http_request_headers` MEDIUMTEXT NULL ,
  `http_request_cookies` MEDIUMTEXT NULL ,
  `http_response_headers` MEDIUMTEXT NULL ,
  `response_status` VARCHAR(10) NULL ,
  `response_statuscode` VARCHAR(255) NULL ,
  `response_detail` TEXT NULL ,
  `response_elapsedtime` VARCHAR(255) NULL ,
  `response_elapsedtimeunits` VARCHAR(255) NULL ,
  `component` VARCHAR(50) NULL ,
  `realm` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_am_auditaccess_transactionid` (`transactionid` ASC) ,
  INDEX `idx_am_auditaccess_status` (`response_status` ASC) ,
  INDEX `idx_am_auditaccess_userid` (`userid` ASC)
)
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `audit`.`am_auditconfig`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`am_auditconfig` (
  `id` VARCHAR(56) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(255) NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` MEDIUMTEXT,
  `runas` VARCHAR(255) NULL ,
  `objectid` VARCHAR(255) NULL ,
  `operation` VARCHAR(255) NULL ,
  `beforeObject` MEDIUMTEXT NULL ,
  `afterObject` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `rev` VARCHAR(255) NULL,
  `component` VARCHAR(50) NULL ,
  `realm` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_am_auditconfig_transactionid` (`transactionid` ASC)
)
ENGINE = InnoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -------------------------------------------
-- audit database user
-- ------------------------------------------
GRANT ALL PRIVILEGES on audit.* TO audit IDENTIFIED BY 'audit';
GRANT ALL PRIVILEGES on audit.* TO audit@'%' IDENTIFIED BY 'audit';
GRANT ALL PRIVILEGES on audit.* TO audit@localhost IDENTIFIED BY 'audit';