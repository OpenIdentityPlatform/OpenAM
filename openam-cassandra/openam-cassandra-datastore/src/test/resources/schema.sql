CREATE KEYSPACE msisdn WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '2'}  AND durable_writes = true;

use target_space;

create table realm (
"uid" text PRIMARY KEY,
"serviceName" set<text>,
"objectClass" set<text>,
"cospriority" set<text>,
"iplanet-am-user-federation-info" set<text>,
"iplanet-am-user-federation-info-key" set<text>,
"sunIdentityServerDiscoEntries" set<text>,

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
"iplanet-am-user-login-status" set<text>,
"iplanet-am-user-password-reset-force-reset" set<text>,
"iplanet-am-user-password-reset-options" set<text>,
"iplanet-am-user-password-reset-question-answer" set<text>,
"iplanet-am-user-success-url" set<text>
);
CREATE INDEX serviceName ON realm ("serviceName");
CREATE INDEX objectClass2 ON realm ("objectClass");

GRANT SELECT ON realm TO openam;
GRANT MODIFY ON realm TO openam;

create table user (
"uid"  text PRIMARY KEY,
"adminRole" set<text>,
"assignedDashboard" set<text>,
"authorityRevocationList" set<text>,
"caCertificate" set<text>,
"cn" set<text>,
"devicePrintProfiles" set<text>,
"distinguishedName" set<text>,
"dn" set<text>,
"employeeNumber" set<text>,
"givenName" set<text>,
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
"mail" set<text>,
"manager" set<text>,
"memberOf" set<text>,
"modifyTimestamp" set<text>,
"objectClass" set<text>,
"postalAddress" set<text>,
"preferredlanguage" set<text>,
"preferredLocale" set<text>,
"preferredtimezone" set<text>,
"sn" set<text>,
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
"sunIdentityServerPPFacadegreetmesound" set<text>,
"sunIdentityServerPPFacadeGreetSound" set<text>,
"sunIdentityServerPPFacadeMugShot" set<text>,
"sunIdentityServerPPFacadeNamePronounced" set<text>,
"sunIdentityServerPPFacadeWebSite" set<text>,
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
"telephoneNumber" set<text>,
"userCertificate" set<text>,
"userPassword" set<text>
) WITH compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
AND default_time_to_live = 31536000;

CREATE INDEX cn ON user ("cn");
CREATE INDEX "givenName" ON user ("givenName");
CREATE INDEX "iplanet_am_user_alias_list" ON user ("iplanet-am-user-alias-list");
CREATE INDEX "iplanet_am_user_federation_info_key" ON user ("iplanet-am-user-federation-info-key");
CREATE INDEX memberof ON user ("memberOf");
CREATE INDEX objectClass ON user ("objectClass");
CREATE INDEX "sun_fm_saml2_nameid_infokey" ON user ("sun-fm-saml2-nameid-infokey");
CREATE INDEX sunIdentityMSISDNNumber ON user ("sunIdentityMSISDNNumber");
CREATE INDEX telephoneNumber ON user ("telephoneNumber");

CREATE TABLE rowindexschema (
    id int PRIMARY KEY,
    name text
) ;

CREATE TABLE rowindexdata (
    id int,
    value text,
    key text,
    time timestamp,
    PRIMARY KEY (id, value, key)
) WITH default_time_to_live = 0;

GRANT SELECT ON user TO openam;
GRANT MODIFY ON user TO openam;