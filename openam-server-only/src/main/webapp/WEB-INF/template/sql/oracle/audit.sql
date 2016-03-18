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


-- -----------------------------------------------------
-- Table am_auditaccess
-- -----------------------------------------------------
PROMPT Creating Table am_auditaccess ...
CREATE TABLE am_auditaccess (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  server_ip VARCHAR2(40 CHAR),
  server_port VARCHAR2(5 CHAR),
  client_host VARCHAR2(255 CHAR),
  client_ip VARCHAR2(40 CHAR),
  client_port VARCHAR2(5 CHAR),
  request_protocol VARCHAR2(255 CHAR) NULL ,
  request_operation VARCHAR2(255 CHAR) NULL ,
  request_detail CLOB NULL ,
  http_request_secure VARCHAR2(255 CHAR) NULL ,
  http_request_method VARCHAR2(7 CHAR) NULL ,
  http_request_path VARCHAR2(255 CHAR) NULL ,
  http_request_queryparameters CLOB NULL ,
  http_request_headers CLOB NULL ,
  http_request_cookies CLOB NULL ,
  http_response_headers CLOB NULL ,
  response_status VARCHAR2(10 CHAR) NULL ,
  response_statuscode VARCHAR2(255 CHAR) NULL ,
  response_detail CLOB NULL ,
  response_elapsedtime VARCHAR2(255 CHAR) NULL ,
  response_elapsedtimeunits VARCHAR2(255 CHAR) NULL ,
  component VARCHAR2(50 CHAR) NULL ,
  realm VARCHAR2(255 CHAR) NULL
);


COMMENT ON COLUMN am_auditaccess.timestamp_ IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_ACCESS on table am_auditaccess ...
ALTER TABLE am_auditaccess
ADD CONSTRAINT PRIMARY_ACCESS PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_am_auditaccess_txid on am_auditaccess ...
CREATE INDEX idx_am_auditaccess_txid ON am_auditaccess
(
  transactionid
)
;

-- -----------------------------------------------------
-- Table am_auditauthentication
-- -----------------------------------------------------
PROMPT Creating TABLE am_auditauthentication ...
CREATE TABLE am_auditauthentication (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  result VARCHAR2(255 CHAR),
  principals CLOB,
  context CLOB,
  entries CLOB ,
  component VARCHAR2(50 CHAR) NULL ,
  realm VARCHAR2(255 CHAR) NULL
);

COMMENT ON COLUMN am_auditauthentication.timestamp_ IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating PRIMARY KEY CONSTRAINT PRIMARY_AUTHENTICATION ON TABLE am_auditauthentication ...
ALTER TABLE am_auditauthentication
ADD CONSTRAINT PRIMARY_AUTHENTICATION PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_am_auditauthn_txid on am_auditauthentication ...
CREATE INDEX idx_am_auditauthn_txid ON am_auditauthentication
(
  transactionid
)
;


-- -----------------------------------------------------
-- Table am_auditactivity
-- -----------------------------------------------------
PROMPT Creating Table am_auditactivity ...
CREATE TABLE am_auditactivity (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  objectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  beforeObject CLOB,
  afterObject CLOB,
  changedfields VARCHAR2(255 CHAR),
  rev VARCHAR2(255 CHAR) ,
  component VARCHAR2(50 CHAR) NULL ,
  realm VARCHAR2(255 CHAR) NULL
);


COMMENT ON COLUMN am_auditactivity.timestamp_ IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_ACTIVITY on table am_auditactivity ...
ALTER TABLE am_auditactivity
ADD CONSTRAINT PRIMARY_ACTIVITY PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_activity_txid on am_auditactivity ...
CREATE INDEX idx_activity_txid ON am_auditactivity
(
  transactionid
)
;

-- -----------------------------------------------------
-- Table am_auditconfig
-- -----------------------------------------------------
PROMPT Creating Table am_auditconfig ...
CREATE TABLE am_auditconfig (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  objectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  beforeObject CLOB,
  afterObject CLOB,
  changedfields VARCHAR2(255 CHAR),
  rev VARCHAR2(255 CHAR) ,
  component VARCHAR2(50 CHAR) NULL ,
  realm VARCHAR2(255 CHAR) NULL
);


COMMENT ON COLUMN am_auditconfig.timestamp_ IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_CONFIG on table am_auditconfig ...
ALTER TABLE am_auditconfig
ADD CONSTRAINT PRIMARY_CONFIG PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_am_config_txid on am_auditconfig ...
CREATE INDEX idx_am_config_txid ON am_auditconfig
(
  transactionid
)
;

COMMIT;