CREATE KEYSPACE IF NOT EXISTS realm_name WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'} ;
use realm_name;

CREATE TABLE IF NOT EXISTS values (type text, uid text, field text,value text,change timestamp, PRIMARY KEY ((type,uid),field,value));

/*---indexes for search:----*/
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_uid AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='uid' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_servicename AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='servicename' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_cn AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='cn' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_sn AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='sn' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_givenname AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='givenname' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_mail AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='mail' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_memberof AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='memberof' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_manager AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='manager' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_sunidentitymsisdnnumber AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='sunidentitymsisdnnumber' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_iplanet_am_user_federation_info_key AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='iplanet_am_user_federation_info_key' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_sun_fm_saml2_nameid_infokey AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='sun_fm_saml2_nameid_infokey' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_iplanet_am_user_alias_list AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='iplanet-am-user-alias-list' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);
CREATE MATERIALIZED VIEW IF NOT EXISTS ix_employeenumber AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='employeenumber' and value IS NOT NULL PRIMARY KEY ((type,field,value),uid);