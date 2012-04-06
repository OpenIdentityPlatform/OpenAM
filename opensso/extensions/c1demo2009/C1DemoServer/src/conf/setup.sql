#
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#  Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
#
#  The contents of this file are subject to the terms
#  of the Common Development and Distribution License
#  (the License). You may not use this file except in
#  compliance with the License.
#
#  You can obtain a copy of the License at
#  opensso/legal/CDDLv1.0.txt
#  See the License for the specific language governing
#  permission and limitations under the License.
#
#  When distributing Covered Code, include this CDDL
#  Header Notice in each file and include the License file
#  at opensso/legal/CDDLv1.0.txt.
#  If applicable, add the following below the CDDL Header,
#  with the fields enclosed by brackets [] replaced by
#  your own identifying information:
#  "Portions Copyrighted [year] [name of copyright owner]"
#
#  $Id: setup.sql,v 1.3 2009/06/18 00:55:54 superpat7 Exp $
#

#
# SQL Script file to set up tables and data for demo
# Import using 
# mysql -uroot -p<password> -Dj1demodb < setup.sql
#

# Pre-emptive cleanup!
DROP TABLE IF EXISTS account_request;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS call_log;
DROP TABLE IF EXISTS phone;
DROP TABLE IF EXISTS account;

CREATE TABLE account
    (account_number CHAR(20) NOT NULL,
    bill_to_address_line_1 VARCHAR(100) NOT NULL,
    bill_to_address_line_2 VARCHAR(100),
    bill_to_city VARCHAR(100) NOT NULL,
    bill_to_state VARCHAR(100) NOT NULL,
    bill_to_zip VARCHAR(100) NOT NULL,
    credit_card_number DECIMAL(16) NOT NULL,
    cvv DECIMAL(4) NOT NULL,
    plan_minutes INT,
    plan_id INT,
    challenge_question VARCHAR(100),
    challenge_answer VARCHAR(100),
    PRIMARY KEY (account_number)
    )
    ENGINE InnoDB;

CREATE TABLE phone
    (phone_number CHAR(10) NOT NULL,
    account_number CHAR(20) NOT NULL,
    user_name VARCHAR(100),
    password VARCHAR(100),
    allocated_minutes INT,
    head_of_household BIT,
    can_download_ringtones BIT,
    can_download_music BIT,
    can_download_video BIT,
    otp VARCHAR(200),
    inetuserstatus VARCHAR(10),
    PRIMARY KEY (phone_number),
    INDEX account_ind (account_number),
    FOREIGN KEY (account_number) REFERENCES account(account_number)
    )
    ENGINE InnoDB;
    
CREATE TABLE call_log
    (phone_number_from CHAR(10) NOT NULL,
    phone_number_to CHAR(10) NOT NULL,
    call_time TIMESTAMP NOT NULL,
    call_duration_secs INT NOT NULL,
    call_id INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (call_id),
    INDEX phone_from_ind (phone_number_from),
    FOREIGN KEY (phone_number_from) REFERENCES phone(phone_number)
    )
    ENGINE InnoDB;
    
CREATE TABLE notification
    (phone_number CHAR(10) NOT NULL,
    notification_time TIMESTAMP NOT NULL,
    message_text VARCHAR(2000) NOT NULL,
    notification_id INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (notification_id),
    INDEX phone_ind (phone_number),
    FOREIGN KEY (phone_number) REFERENCES phone(phone_number)
    )
    ENGINE InnoDB;
    
CREATE TABLE account_request
    (phone_number CHAR(10) NOT NULL,
    account_number CHAR(20) NOT NULL,
    request_time TIMESTAMP NOT NULL,
    message_text VARCHAR(2000) NOT NULL,
    request_id INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (request_id),
    INDEX phone_ind (phone_number),
    FOREIGN KEY (phone_number) REFERENCES phone(phone_number),
    INDEX account_ind (account_number),
    FOREIGN KEY (account_number) REFERENCES account(account_number)
    )
    ENGINE InnoDB;

INSERT INTO account VALUES('123456789012345', '123 Any St', DEFAULT, 'Santa Clara', 'CA', '95012', '1234567890123456', '1234', '1000', '1', DEFAULT, DEFAULT);

INSERT INTO phone VALUES('1234567890', '123456789012345', 'Frank Spencer', 'abc123', '250', '1', '1', '1', '1', DEFAULT, 'Active');

INSERT INTO phone VALUES('1112223333', '123456789012345', 'Billy Spencer', 'abc123', '250', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, 'Active');

INSERT INTO phone VALUES('1212121212', '123456789012345', 'Jane Spencer', 'abc123', '500', '1', '1', '1', '1', DEFAULT, 'Active');

INSERT INTO phone VALUES('1231231234', '123456789012345', 'Susan Spencer', 'abc123', '1000', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, 'Active');

# Some calls have been made
INSERT INTO call_log VALUES('1234567890', '1112223333', '2009-04-24 17:08:01', '123', DEFAULT);
INSERT INTO call_log VALUES('1234567890', '2223334444', '2009-04-25 21:09:01', '421', DEFAULT);
INSERT INTO call_log VALUES('1112223333', '3334445555', '2009-04-26 22:21:34', '236', DEFAULT);
INSERT INTO call_log VALUES('1212121212', '1231231234', '2009-04-28 20:34:35', '123', DEFAULT);
INSERT INTO call_log VALUES('1231231234', '1212121212', '2009-04-29 21:43:37', '321', DEFAULT);

# We've sent a notification
INSERT INTO notification VALUES('1234567890', '2009-04-24 20:28:02', 'Welcome to MyAccount! Have fun using it!', DEFAULT);
INSERT INTO notification VALUES('1112223333', '2009-04-24 20:28:02', 'Welcome to MyAccount! Have fun using it!', DEFAULT);
INSERT INTO notification VALUES('1212121212', '2009-04-24 20:28:02', 'Welcome to MyAccount! Have fun using it!', DEFAULT);
INSERT INTO notification VALUES('1231231234', '2009-04-24 20:28:02', 'Welcome to MyAccount! Have fun using it!', DEFAULT);

# And made a request
INSERT INTO account_request VALUES('1112223333', '123456789012345', '2009-05-24 20:28:02', 'Please add me to the account', DEFAULT);

COMMIT;
