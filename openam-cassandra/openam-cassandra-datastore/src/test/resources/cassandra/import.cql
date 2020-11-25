CREATE KEYSPACE test_b2c_users WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}   AND durable_writes = true;

CREATE TABLE test_b2c_users.rowindexdata (
    id int,
    value text,
    key text,
    time timestamp,
    PRIMARY KEY (id, value, key)
) WITH CLUSTERING ORDER BY (value ASC, key ASC)
    AND bloom_filter_fp_chance = 0.01
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';

CREATE TABLE test_b2c_users.realm (
    uid text PRIMARY KEY,
    cospriority set<text>,
    "iplanet-am-session-add-session-listener-on-all-sessions" set<text>,
    "iplanet-am-session-destroy-sessions" set<text>,
    "iplanet-am-session-get-valid-sessions" set<text>,
    "iplanet-am-session-max-caching-time" set<text>,
    "iplanet-am-session-max-idle-time" set<text>,
    "iplanet-am-session-max-session-time" set<text>,
    "iplanet-am-session-quota-limit" set<text>,
    "iplanet-am-session-service-status" set<text>,
    "iplanet-am-user-account-life" set<text>,
    "iplanet-am-user-admin-start-dn" set<text>,
    "iplanet-am-user-alias-list" set<text>,
    "iplanet-am-user-auth-config" set<text>,
    "iplanet-am-user-auth-modules" set<text>,
    "iplanet-am-user-failure-url" set<text>,
    "iplanet-am-user-federation-info" set<text>,
    "iplanet-am-user-federation-info-key" set<text>,
    "iplanet-am-user-login-status" set<text>,
    "iplanet-am-user-password-reset-force-reset" set<text>,
    "iplanet-am-user-password-reset-options" set<text>,
    "iplanet-am-user-password-reset-question-answer" set<text>,
    "iplanet-am-user-success-url" set<text>,
    "objectClass" set<text>,
    "serviceName" set<text>,
    "sunIdentityServerDiscoEntries" set<text>
) WITH bloom_filter_fp_chance = 0.01
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';
CREATE INDEX servicename ON test_b2c_users.realm ("serviceName");
CREATE INDEX objectclass2 ON test_b2c_users.realm ("objectClass");

CREATE TABLE test_b2c_users.user (
    uid text PRIMARY KEY,
    ad set<text>,
    "adminRole" set<text>,
    "assignedDashboard" set<text>,
    "authorityRevocationList" set<text>,
    balance set<text>,
    birthday set<text>,
    bonus set<text>,
    businesscategory set<text>,
    "caCertificate" set<text>,
    cn set<text>,
    "comstar-b2c-login" set<text>,
    "comstar-b2c-password" set<text>,
    customer set<text>,
    destinationindicator set<text>,
    "devicePrintProfiles" set<text>,
    displayname set<text>,
    "distinguishedName" set<text>,
    dn set<text>,
    "employeeNumber" set<text>,
    employeetype set<text>,
    envelope set<text>,
    generateid set<text>,
    "givenName" set<text>,
    h2o set<text>,
    imsi set<text>,
    "inetUserHttpURL" set<text>,
    "inetUserStatus" set<text>,
    "iplanet-am-auth-config" set<text>,
    "iplanet-am-session-add-session-listener-on-all-sessions" set<text>,
    "iplanet-am-session-destroy-sessions" set<text>,
    "iplanet-am-session-get-valid-sessions" set<text>,
    "iplanet-am-session-max-caching-time" set<text>,
    "iplanet-am-session-max-idle-time" set<text>,
    "iplanet-am-session-max-session-time" set<text>,
    "iplanet-am-session-quota-limit" set<text>,
    "iplanet-am-session-service-status" set<text>,
    "iplanet-am-user-account-life" set<text>,
    "iplanet-am-user-admin-start-dn" set<text>,
    "iplanet-am-user-alias-list" set<text>,
    "iplanet-am-user-auth-config" set<text>,
    "iplanet-am-user-auth-modules" set<text>,
    "iplanet-am-user-failure-url" set<text>,
    "iplanet-am-user-federation-info" set<text>,
    "iplanet-am-user-federation-info-key" set<text>,
    "iplanet-am-user-login-status" set<text>,
    "iplanet-am-user-password-reset-force-reset" set<text>,
    "iplanet-am-user-password-reset-options" set<text>,
    "iplanet-am-user-password-reset-question-answer" set<text>,
    "iplanet-am-user-success-url" set<text>,
    mail set<text>,
    manager set<text>,
    "memberOf" set<text>,
    "modifyTimestamp" set<text>,
    o set<text>,
    "objectClass" set<text>,
    orderid set<text>,
    ou set<text>,
    personalaccountnumber set<text>,
    po set<text>,
    "postalAddress" set<text>,
    "preferredLocale" set<text>,
    preferredlanguage set<text>,
    preferredtimezone set<text>,
    service set<text>,
    services set<text>,
    sn set<text>,
    "sun-fm-saml2-nameid-info" set<text>,
    "sun-fm-saml2-nameid-infokey" set<text>,
    "sunAMAuthInvalidAttemptsData" set<text>,
    "sunIdentityMSISDNNumber" set<text>,
    "sunIdentityServerDiscoEntries" set<text>,
    "sunIdentityServerPPAddressCard" set<text>,
    "sunIdentityServerPPCommonNameAltCN" set<text>,
    "sunIdentityServerPPCommonNameCN" set<text>,
    "sunIdentityServerPPCommonNameFN" set<text>,
    "sunIdentityServerPPCommonNameMN" set<text>,
    "sunIdentityServerPPCommonNamePT" set<text>,
    "sunIdentityServerPPCommonNameSN" set<text>,
    "sunIdentityServerPPDemographicsAge" set<text>,
    "sunIdentityServerPPDemographicsBirthDay" set<text>,
    "sunIdentityServerPPDemographicsDisplayLanguage" set<text>,
    "sunIdentityServerPPDemographicsLanguage" set<text>,
    "sunIdentityServerPPDemographicsTimeZone" set<text>,
    "sunIdentityServerPPEmergencyContact" set<text>,
    "sunIdentityServerPPEmploymentIdentityAltO" set<text>,
    "sunIdentityServerPPEmploymentIdentityJobTitle" set<text>,
    "sunIdentityServerPPEmploymentIdentityOrg" set<text>,
    "sunIdentityServerPPEncryPTKey" set<text>,
    "sunIdentityServerPPFacadeGreetSound" set<text>,
    "sunIdentityServerPPFacadeMugShot" set<text>,
    "sunIdentityServerPPFacadeNamePronounced" set<text>,
    "sunIdentityServerPPFacadeWebSite" set<text>,
    "sunIdentityServerPPFacadegreetmesound" set<text>,
    "sunIdentityServerPPInformalName" set<text>,
    "sunIdentityServerPPLegalIdentityAltIdType" set<text>,
    "sunIdentityServerPPLegalIdentityAltIdValue" set<text>,
    "sunIdentityServerPPLegalIdentityDOB" set<text>,
    "sunIdentityServerPPLegalIdentityGender" set<text>,
    "sunIdentityServerPPLegalIdentityLegalName" set<text>,
    "sunIdentityServerPPLegalIdentityMaritalStatus" set<text>,
    "sunIdentityServerPPLegalIdentityVATIdType" set<text>,
    "sunIdentityServerPPLegalIdentityVATIdValue" set<text>,
    "sunIdentityServerPPMsgContact" set<text>,
    "sunIdentityServerPPSignKey" set<text>,
    tariff set<text>,
    tariffid set<text>,
    tdid set<text>,
    tdn set<text>,
    "telephoneNumber" set<text>,
    terminal set<text>,
    "time-balance-actual" set<text>,
    "time-change-password" set<text>,
    "userCertificate" set<text>,
    "userPassword" set<text>,
    "confirmUseOAuth" set<text>,
    "oauth-token-access" set<text>,
    "oauth-token-refresh" set<text>,
    "session-success" set<text>,
    "session-failed" set<text>
) WITH bloom_filter_fp_chance = 0.01
     AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 5184000
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';
CREATE INDEX telephonenumber ON test_b2c_users.user ("telephoneNumber");
CREATE INDEX service2terminal ON test_b2c_users.user (terminal);
CREATE INDEX cn ON test_b2c_users.user (cn);
CREATE INDEX iplanet_am_user_federation_info_key ON test_b2c_users.user ("iplanet-am-user-federation-info-key");
CREATE INDEX objectclass ON test_b2c_users.user ("objectClass");
CREATE INDEX envelope ON test_b2c_users.user (envelope);
CREATE INDEX generateid ON test_b2c_users.user (generateid);
CREATE INDEX comstar_b2c_login ON test_b2c_users.user ("comstar-b2c-login");
CREATE INDEX iplanet_am_user_alias_list ON test_b2c_users.user ("iplanet-am-user-alias-list");
CREATE INDEX sunidentitymsisdnnumber ON test_b2c_users.user ("sunIdentityMSISDNNumber");
CREATE INDEX sun_fm_saml2_nameid_infokey ON test_b2c_users.user ("sun-fm-saml2-nameid-infokey");
CREATE INDEX memberof ON test_b2c_users.user ("memberOf");
CREATE INDEX givenName ON test_b2c_users.user ("givenName");
CREATE INDEX personalaccountnumber ON test_b2c_users.user (personalaccountnumber);
CREATE INDEX manager ON test_b2c_users.user ("manager");

CREATE TABLE test_b2c_users.rowindexschema (
    id int PRIMARY KEY,
    name text
) WITH bloom_filter_fp_chance = 0.01
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';