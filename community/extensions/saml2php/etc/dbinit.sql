DROP TABLE IF EXISTS users;
CREATE TABLE users (
	userid varchar(80) NOT NULL, 
	password varchar(80) NOT NULL, 
	username varchar(80) NOT NULL,
	PRIMARY KEY (userid)
);

INSERT INTO users VALUES ('johns', '123', 'John Smith');
INSERT INTO users VALUES ('admin', '456', 'Administrator');

DROP TABLE IF EXISTS nameidmapping;

CREATE TABLE nameidmapping (
	idp varchar(80) NOT NULL,
	sp varchar(80) NOT NULL,
	nameid varchar(80) NOT NULL,
	localid varchar(80) NOT NULL,
	PRIMARY KEY (idp,sp,localid)
);

INSERT INTO nameidmapping VALUES ('openssodemo.idp.company.com', 'openssodemo.idp.company.com', 'YgolvKBPsL4ABSrdOpilovLnVq+X', 'johns');
INSERT INTO nameidmapping VALUES ('openssodemo.idp.company.com', 'openssodemo.idp.company.com', 'GsIcQLU2JvgDJ0ov2+SXVf29ncGF', 'admin');
