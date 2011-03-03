/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Migrate.java,v 1.5 2008/07/23 17:57:48 kenwho Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Updates <code>sunAMIdentityRepository</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "sunIdentityRepositoryService";
    final static String SERVICE_DIR = "99_sunIdentityRepositoryService/20_30";
    final static String SCHEMA_FILE = "idRepoService_addAttrs.xml";
    final static String SCHEMA_FILE1 = "idRepoService_addUserAttrs.xml";
    final static String SCHEMA_FILE2 = "idRepoService_addLDAPv3Attrs.xml";
    final static String SCHEMA_FILE3 = "idRepoService_addLDAPv3ForADAttrs.xml";
    final static String ATTR_CONFIG_USER_OC =
            "sun-idrepo-ldapv3-config-user-objectclass";
    final static String ATTR_CONFIG_USER_PC =
            "sun-idrepo-ldapv3-config-people-container-name";
    final static String ATTR_CONFIG_USER_PC_VAL =
            "sun-idrepo-ldapv3-config-people-container-value";
    final static String ATTR_CONFIG_USER_AUTH_TYPE =
            "sun-idrepo-ldapv3-config-authenticatable-type";
    final static String ATTR_CONFIG_USER =
            "sun-idrepo-ldapv3-config-user-attributes";
    final static String ATTR3 =
            "sun-idrepo-ldapv3-config-people-container-name";
    final static String ATTR4 = "sun-idrepo-ldapv3-config-agent-attributes";
    final static String ATTR5 = "sun-idrepo-ldapv3-config-isactive";
    final static String ATTR6 = "sun-idrepo-ldapv3-config-active";
    final static String ATTR7 = "sun-idrepo-ldapv3-config-inactive";
    final static String ATTR8 = "sun-idrepo-ldapv3-config-agent-attributes";
    final static String ATTR9 = "sunFilesObjectClasses";
    final static String ORG_ATTR1 = "sunOrganizationAliases";
    final static String GLOBAL_SCHEMA_TYPE = "Global";
    final static String ORG_SCHEMA_TYPE = "Organization";
    final static String USERS_SUBSCHEMA = "users";
    final static String USER = "user";
    final static String AGENT_DEFAULT_VALUE = "agent=read,create,edit,delete";
    final static String AGENT = "Agent";
    final static String ATTR_IDREPO_SUPPORTED_OP =
            "sunIdRepoSupportedOperations";
    final static String ATTR_INET_USER = "inetUser";
    final static String ATTR_FEDMGR_DATA_STORE =
            "sunFederationManagerDataStore";
    final static String ATTR_LIBERTY_PP_SERVICE =
            "sunIdentityServerLibertyPPService";
    final static String ATTR_SAML2_NAME_ID = "sunFMSAML2NameIdentifier";
    final static String ATTR_ADMIN_ROLE = "adminRole";
    final static String ATTR_AUTH_REVO_LIST = "authorityRevocationList";
    final static String ATTR_CA_CERT = "caCertificate";
    final static String ATTR_DNAME = "distinguishedName";
    final static String ATTR_INET_USER_HTTP_URL = "inetUserHttpURL";
    final static String ATTR_INET_USER_STATUS = "inetUserStatus";
    final static String ATTR_AUTH_CONFIG = "iplanet-am-auth-configuration";
    final static String ATTR_AUTH_MODULES = "iplanet-am-user-auth-modules";
    final static String ATTR_SESSION_LISTENER =
            "iplanet-am-session-add-session-listener-on-all-sessions";
    final static String ATTR_DESTROY_SESSIONS =
            "iplanet-am-session-destroy-sessions";
    final static String ATTR_VALID_SESSIONS =
            "iplanet-am-session-get-valid-sessions";
    final static String ATTR_SESS_MAX_CACHE_TIME =
            "iplanet-am-session-max-caching-time";
    final static String ATTR_SESS_MAX_IDLE_TIME =
            "iplanet-am-session-max-idle-time";
    final static String ATTR_SESS_MAX_TIME =
            "iplanet-am-session-max-session-time";
    final static String ATTR_SESS_QUOTA_LIMI =
            "iplanet-am-session-quota-limit";
    final static String ATTR_SESS_SERVICE_STATUS =
            "iplanet-am-session-service-status";
    final static String ATTR_USER_ADM_START_DN =
            "iplanet-am-user-admin-start-dn";
    final static String ATTR_USER_ACCT_LIFE =
            "iplanet-am-user-account-life";
    final static String ATTR_USER_ALIAS_LIST =
            "iplanet-am-user-alias-list";
    final static String ATTR_USER_AUTH_CONFIG =
            "iplanet-am-user-auth-config";
    final static String ATTR_USER_FAILURE_URL =
            "iplanet-am-user-failure-url";
    final static String ATTR_USER_LOGIN_STATUS =
            "iplanet-am-user-login-status";
    final static String ATTR_USER_PASS_FORCE_RESET =
            "iplanet-am-user-password-reset-force-reset";
    final static String ATTR_PASS_RESET_OPT =
            "iplanet-am-user-password-reset-options";
    final static String ATTR_PASS_RESET_QUES_ANS =
            "iplanet-am-user-password-reset-question-answer";
    final static String ATTR_USER_SUCCESS_URL = "iplanet-am-user-success-url";
    final static String ATTR_STATIC_GRP_DN = "iplanet-am-static-group-dn";
    final static String ATTR_MAIL = "mail";
    final static String ATTR_MANAGER = "manager";
    final static String ATTR_MEM_OF = "memberOf";
    final static String ATTR_OC = "objectClass";
    final static String ATTR_POSTAL_ADDR = "postalAddress";
    final static String ATTR_PREF_LANG = "preferredlanguage";
    final static String ATTR_PREF_LOCALE = "preferredLocale";
    final static String ATTR_PREF_TIMEZONE = "preferredtimezone";
    final static String ATTR_SN = "sn";
    final static String ATTR_CN = "cn";
    final static String ATTR_AUTH_INVALID_DATA =
            "sunAMAuthInvalidAttemptsData";
    final static String ATTR_AUTH_ACCT_LOCK = "sunAMAuthAccountLockout";
    final static String ATTR_MSISDN_NUM = "sunIdentityMSISDNNumber";
    final static String ATTR_USER_CERT = "userCertificate";
    final static String ATTR_FED_INFO_KEY =
            "iplanet-am-user-federation-info-key";
    final static String ATTR_FED_INFO = "iplanet-am-user-federation-info";
    final static String ATTR_DISCO_ENTRIES = "sunIdentityServerDiscoEntries";
    final static String ATTR_PP_CN = "sunIdentityServerPPCommonNameCN";
    final static String ATTR_PP_FN = "sunIdentityServerPPCommonNameFN";
    final static String ATTR_PP_SN = "sunIdentityServerPPCommonNameSN";
    final static String ATTR_PP_MN = "sunIdentityServerPPCommonNameMN";
    final static String ATTR_ALT_CN = "sunIdentityServerPPCommonNameAltCN";
    final static String ATTR_CN_PT = "sunIdentityServerPPCommonNamePT";
    final static String ATTR_PP_INF_NAME = "sunIdentityServerPPInformalName";
    final static String ATTR_PP_LEGAL_NAME =
            "sunIdentityServerPPLegalIdentityLegalName";
    final static String ATTR_PP_LEGAL_ID_DOB =
            "sunIdentityServerPPLegalIdentityDOB";
    final static String ATTR_PP_LEGAL_ID_MS =
            "sunIdentityServerPPLegalIdentityMaritalStatus";
    final static String ATTR_PP_LEGAL_ID_GENDER =
            "sunIdentityServerPPLegalIdentityGender";
    final static String ATTR_PP_LEGAL_ID_ALT_TYPE =
            "sunIdentityServerPPLegalIdentityAltIdType";
    final static String ATTR_PP_LEGAL_ALT_VAL =
            "sunIdentityServerPPLegalIdentityAltIdValue";
    final static String ATTR_PP_LEGAL_VAL_ID_TYPE =
            "sunIdentityServerPPLegalIdentityVATIdType";
    final static String ATTR_PP_LEGAL_VAT_ID_VALUE =
            "sunIdentityServerPPLegalIdentityVATIdValue";
    final static String ATTR_PP_ID_JOB_TITLE =
            "sunIdentityServerPPEmploymentIdentityJobTitle";
    final static String ATTR_PP_ID_ORG =
            "sunIdentityServerPPEmploymentIdentityOrg";
    final static String ATTR_PP_ID_ALTO =
            "sunIdentityServerPPEmploymentIdentityAltO";
    final static String ATTR_PP_ADDR_CARD =
            "sunIdentityServerPPAddressCard";
    final static String ATTR_PP_MSG_CT = "sunIdentityServerPPMsgContact";
    final static String ATTR_PP_FACADE_MS = "sunIdentityServerPPFacadeMugShot";
    final static String ATTR_PP_FACADE_WS = "sunIdentityServerPPFacadeWebSite";
    final static String ATTR_PP_FACADE_NP =
            "sunIdentityServerPPFacadeNamePronounced";
    final static String ATTR_PP_FACADE_GS =
            "sunIdentityServerPPFacadeGreetSound";
    final static String ATTR_PP_FACADE_GMS =
            "sunIdentityServerPPFacadegreetmesound";
    final static String ATTR_PP_DEMO_DL =
            "sunIdentityServerPPDemographicsDisplayLanguage";
    final static String ATTR_PP_DEMO_LANG =
            "sunIdentityServerPPDemographicsLanguage";
    final static String ATTR_PP_DEMO_AGE =
            "sunIdentityServerPPDemographicsAge";
    final static String ATTR_PP_DEMO_BD =
            "sunIdentityServerPPDemographicsBirthDay";
    final static String ATTR_PP_DEMO_TZ =
            "sunIdentityServerPPDemographicsTimeZone";
    final static String ATTR_PP_SIGN_KEY = "sunIdentityServerPPSignKey";
    final static String ATTR_PP_ENC_PT_KEY = "sunIdentityServerPPEncryPTKey";
    final static String ATTR_PP_EC = "sunIdentityServerPPEmergencyContact";
    final static String ATTR_SAML2_INFO_KEY = "sun-fm-saml2-nameid-infokey";
    final static String ATTR_SAML2_NID_INFO = "sun-fm-saml2-nameid-info";
    final static String ATTR_AGENT_SEARCH =
            "sun-idrepo-ldapv3-config-agent-search-attribute";
    final static String ATTR_AGENT_NAME =
            "sun-idrepo-ldapv3-config-agent-container-name";
    final static String ATTR_AGENT_VALUE =
            "sun-idrepo-ldapv3-config-agent-container-value";
    final static String ATTR_AGENT_SEARCH_FILTER =
            "sun-idrepo-ldapv3-config-agent-search-filter";
    final static String ATTR_AGENT_OC =
            "sun-idrepo-ldapv3-config-agent-objectclass";
    final static String ATTR_AGENT_ATTRS =
            "sun-idrepo-ldapv3-config-agent-attributes";
    final static String ATTR_IS_ACTIVE = "sun-idrepo-ldapv3-config-isactive";
    final static String ATTR_IDREPO_ATTR_MAPPING = "sunIdRepoAttributeMapping";
    final static String OU = "ou";
    final static String PEOPLE = "people";
    final static String ATTR_GIVEN_NAME = "givenName";
    final static String ATTR_DISPLAY_NAME = "displayName";
    final static String ATTR_OBJECT_GUID = "objectGUID";
    final static String ATTR_ACCT_NAME = "sAMAccountName";
    final static String ATTR_PRINCIPAL_NAME = "userPrincipalname";
    final static String ATTR_NAME = "name";
    final static String USER_ACCT_CTRL = "userAccountControl";
    final static String UNICODE_PASS = "unicodePwd";
    final static String CONFIG_ACTIVE_VALUE = "544";
    final static String ATTR_CONFIG_ACTIVE = "sun-idrepo-ldapv3-config-active";
    final static String CONFIG_INACTIVE_VALUE = "546";
    final static String ATTR_CONFIG_INACTIVE =
            "sun-idrepo-ldapv3-config-inactive";
    final static String ATTR_ORG_ALIAS = "sunOrganizationAliases";
    final static String ATTR_TELEPHONE_NUM = "telephoneNumber";
    final static String ATTR_EMPLOYEE_NUM = "employeeNumber";
    final static String ATTR_OP = "organizationalPerson";
    final static String ATTR_L_OP = "organizationalperson";
    final static String ATTR_INET_ADMIN = "inetadmin";
    final static String ATTR_INET_ORG_PERSON = "inetorgperson";
    final static String ATTR_L_INET_USER = "inetuser";
    final static String EMPTY_VALUE = "";
    final static String USER_AMADMIN = "amAdmin";
    final static String USER_ANON = "anonymous";
    final static String SUBCONFIG_AMADMIN = "/users/amAdmin";
    final static String SUBCONFIG_ANON = "/users/anonymous";
    final static String SUPPORTED_IDS = "SupportedIdentities";
    final static String ATTR_AGENT = "agent";
    final static String SC_AGENT = "agent";
    final static String AGENT_GROUP = "agentgroup";
    final static String SUBCONFIG_AGENT_GROUP = "agentgroup";
    final static String SUBCONFIG_AGENT_ONLY = "agentonly";
    final static String AGENT_SERVICE = "AgentService";
    final static String ATTR_SERVICE_NAME = "servicename";
    final static String FILTERED_ROLE_SERVICE =
            "sunIdentityFilteredRoleService";
    final static String FILTERED_ROLE = "filteredrole";
    final static String CAN_HAVE_MEMBERS = "canHaveMembers";
    final static String CAN_ADD_MEMBERS = "canAddMembers";
    final static String CAN_BE_MEM_OF = "canBeMemberOf";
    final static String LDAPv3 = "LDAPv3";
    final static String LDAPv3ForAMDS = "LDAPv3ForAMDS";
    final static String LDAPv3ForAD = "LDAPv3ForAD";
    final static String FILES = "files";

    /**
     * Updates the <code>sunIdentityRepositoryService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        String classMethod = "20_30/sunIdentityRepositoryService";
        try {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                        "Adding attributes to user subschema /users/user"
                        + SCHEMA_FILE1);
            }
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE1);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, USERS_SUBSCHEMA,
                    USER, GLOBAL_SCHEMA_TYPE, fileName);
            // remove agent default value from sunIdRepoSupportedOperations
            // from LDAPv3 , LDAPv3ForAMDS , LDAPv3ForAD

            Set defaultVal = new HashSet();
            defaultVal.add(AGENT_DEFAULT_VALUE);
            Set sunServiceID = new HashSet();
            sunServiceID.add(LDAPv3);
            sunServiceID.add(LDAPv3ForAMDS);
            sunServiceID.add(LDAPv3ForAD);

            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                        "Remove default value from " +
                        "sunIdRepoSupportedOoperations" + defaultVal);
            }
            // remove from  all instances of  the data store
            UpgradeUtils.removeAttrDefaultValueSubConfig(
                    SERVICE_NAME, sunServiceID,
                    ATTR_IDREPO_SUPPORTED_OP, defaultVal);
            // remove default value from subschema
            Iterator i = sunServiceID.iterator();
            while (i.hasNext()) {
                String subSchemaName = (String) i.next();
                if (UpgradeUtils.debug.messageEnabled()) {
                    UpgradeUtils.debug.message(classMethod +
                            "Removing default Val from subSchema" +
                            subSchemaName);
                }
                UpgradeUtils.removeAttributeDefaultValues(
                        SERVICE_NAME, ORG_SCHEMA_TYPE,
                        ATTR_IDREPO_SUPPORTED_OP, defaultVal, subSchemaName);
            }

            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                        "Remove user default " +
                        "value from LDAPv3 config-user-objectclass");
            }
            // LDAPV3 only - remove user default value
            // from sun-idrepo-ldapv3-config-user-objectclass
            sunServiceID.remove(LDAPv3ForAMDS);
            sunServiceID.remove(LDAPv3ForAD);
            defaultVal.clear();
            defaultVal.add(USER);
            defaultVal.add(ATTR_INET_ADMIN);
            defaultVal.add(ATTR_INET_ORG_PERSON);
            defaultVal.add(ATTR_L_OP);
            defaultVal.add(ATTR_L_INET_USER);
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod + "Remove default " +
                        "values from LDAPv3 config-user-objectclass" +
                        defaultVal + " from subconfig " + sunServiceID);
            }
            UpgradeUtils.removeAttrDefaultValueSubConfig(
                    SERVICE_NAME, sunServiceID,
                    ATTR_CONFIG_USER_OC, defaultVal);
            // remove attribute default value from subschema 
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                        " Removing default value for attribute " +
                        ATTR_CONFIG_USER_OC + " from subschema : " +
                        defaultVal);
            }
            UpgradeUtils.removeAttributeDefaultValues(
                    SERVICE_NAME, ORG_SCHEMA_TYPE,
                    ATTR_CONFIG_USER_OC, defaultVal, LDAPv3);

            // add default values for sun-idrepo-ldapv3-config-user-objectclass
            defaultVal.clear();
            defaultVal.add(ATTR_OP);
            defaultVal.add(ATTR_INET_USER);
            defaultVal.add(ATTR_FEDMGR_DATA_STORE);
            defaultVal.add(ATTR_LIBERTY_PP_SERVICE);
            defaultVal.add(ATTR_SAML2_NAME_ID);

            // add default value to attribute schema.
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod + 
                        "Adding default values " +
                        defaultVal + "to attribute in subschema " +
                        ATTR_CONFIG_USER_OC);
            }
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER_OC, defaultVal);

            // add attribute to sub configuration , instances of the datastore.
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceID,
                    ATTR_CONFIG_USER_OC, defaultVal);

            // update schema for LDAPv3ForAMDS
            Set sunServiceIdDS = new HashSet();
            sunServiceIdDS.add(LDAPv3ForAMDS);
            defaultVal.remove(ATTR_INET_USER);
            defaultVal.remove(ATTR_OP);

            defaultVal.add(ATTR_L_OP);
            defaultVal.add(ATTR_AUTH_ACCT_LOCK);
            // add default values to the subschema for AM DS
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAMDS,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER_OC, defaultVal);
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceIdDS,
                    ATTR_CONFIG_USER_OC, defaultVal);

            // update AD .
            defaultVal.add(ATTR_OP);
            defaultVal.remove(ATTR_L_OP);
            HashSet sunServiceIdAD = new HashSet();
            sunServiceIdAD.add(LDAPv3ForAD);
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER_OC, defaultVal);
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceIdAD,
                    ATTR_CONFIG_USER_OC, defaultVal);

            // add default values for sun-idrepo-ldapv3-config-user-attributes
            defaultVal.clear();
            defaultVal.add(ATTR_ADMIN_ROLE);
            defaultVal.add(ATTR_AUTH_REVO_LIST);
            defaultVal.add(ATTR_CA_CERT);
            defaultVal.add(ATTR_DNAME);
            defaultVal.add(ATTR_INET_USER_HTTP_URL);
            defaultVal.add(ATTR_INET_USER_STATUS);
            defaultVal.add(ATTR_AUTH_CONFIG);
            defaultVal.add(ATTR_AUTH_MODULES);
            defaultVal.add(ATTR_SESSION_LISTENER);
            defaultVal.add(ATTR_DESTROY_SESSIONS);
            defaultVal.add(ATTR_VALID_SESSIONS);
            defaultVal.add(ATTR_SESS_MAX_CACHE_TIME);
            defaultVal.add(ATTR_SESS_MAX_IDLE_TIME);
            defaultVal.add(ATTR_SESS_MAX_TIME);
            defaultVal.add(ATTR_SESS_QUOTA_LIMI);
            defaultVal.add(ATTR_SESS_SERVICE_STATUS);
            defaultVal.add(ATTR_USER_ADM_START_DN);
            defaultVal.add(ATTR_USER_ACCT_LIFE);
            defaultVal.add(ATTR_USER_ALIAS_LIST);
            defaultVal.add(ATTR_USER_AUTH_CONFIG);
            defaultVal.add(ATTR_USER_FAILURE_URL);
            defaultVal.add(ATTR_USER_LOGIN_STATUS);
            defaultVal.add(ATTR_USER_PASS_FORCE_RESET);
            defaultVal.add(ATTR_PASS_RESET_OPT);
            defaultVal.add(ATTR_PASS_RESET_QUES_ANS);
            defaultVal.add(ATTR_USER_SUCCESS_URL);
            defaultVal.add(ATTR_STATIC_GRP_DN);
            defaultVal.add(ATTR_MAIL);
            defaultVal.add(ATTR_MANAGER);
            defaultVal.add(ATTR_MEM_OF);
            defaultVal.add(ATTR_OC);
            defaultVal.add(ATTR_POSTAL_ADDR);
            defaultVal.add(ATTR_PREF_LANG);
            defaultVal.add(ATTR_PREF_LOCALE);
            defaultVal.add(ATTR_PREF_TIMEZONE);
            defaultVal.add(ATTR_SN);
            defaultVal.add(ATTR_AUTH_INVALID_DATA);
            defaultVal.add(ATTR_MSISDN_NUM);
            defaultVal.add(ATTR_USER_CERT);
            defaultVal.add(ATTR_FED_INFO_KEY);
            defaultVal.add(ATTR_FED_INFO);
            defaultVal.add(ATTR_DISCO_ENTRIES);
            defaultVal.add(ATTR_PP_CN);
            defaultVal.add(ATTR_PP_FN);
            defaultVal.add(ATTR_PP_SN);
            defaultVal.add(ATTR_PP_MN);
            defaultVal.add(ATTR_ALT_CN);
            defaultVal.add(ATTR_CN_PT);
            defaultVal.add(ATTR_PP_INF_NAME);
            defaultVal.add(ATTR_PP_LEGAL_NAME);
            defaultVal.add(ATTR_PP_LEGAL_ID_DOB);
            defaultVal.add(ATTR_PP_LEGAL_ID_MS);
            defaultVal.add(ATTR_PP_LEGAL_ID_GENDER);
            defaultVal.add(ATTR_PP_LEGAL_ID_ALT_TYPE);
            defaultVal.add(ATTR_PP_LEGAL_ALT_VAL);
            defaultVal.add(ATTR_PP_LEGAL_VAL_ID_TYPE);
            defaultVal.add(ATTR_PP_LEGAL_VAT_ID_VALUE);
            defaultVal.add(ATTR_PP_ID_JOB_TITLE);
            defaultVal.add(ATTR_PP_ID_ORG);
            defaultVal.add(ATTR_PP_ID_ALTO);
            defaultVal.add(ATTR_PP_ADDR_CARD);
            defaultVal.add(ATTR_PP_MSG_CT);
            defaultVal.add(ATTR_PP_FACADE_MS);
            defaultVal.add(ATTR_PP_FACADE_WS);
            defaultVal.add(ATTR_PP_FACADE_NP);
            defaultVal.add(ATTR_PP_FACADE_GS);
            defaultVal.add(ATTR_PP_FACADE_GMS);
            defaultVal.add(ATTR_PP_DEMO_DL);
            defaultVal.add(ATTR_PP_DEMO_LANG);
            defaultVal.add(ATTR_PP_DEMO_AGE);
            defaultVal.add(ATTR_PP_DEMO_BD);
            defaultVal.add(ATTR_PP_DEMO_TZ);
            defaultVal.add(ATTR_PP_SIGN_KEY);
            defaultVal.add(ATTR_PP_ENC_PT_KEY);
            defaultVal.add(ATTR_PP_EC);
            defaultVal.add(ATTR_SAML2_INFO_KEY);
            defaultVal.add(ATTR_SAML2_NID_INFO);

            // add to organization schema.
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod + "Attribute Name" +
                        ATTR_CONFIG_USER);
                UpgradeUtils.debug.message(classMethod +
                        "Add values to attribute in subschema: " + defaultVal);
                UpgradeUtils.debug.message(classMethod + "Add to subconfigs :" +
                        sunServiceID);
            }
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER, defaultVal);
            // add to all sub configs.
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceID,
                    ATTR_CONFIG_USER, defaultVal);

            // modify schema for datastore LDAPv3ForAMDS 
            defaultVal.remove(ATTR_INET_USER_STATUS);
            defaultVal.remove(ATTR_USER_ACCT_LIFE);
            defaultVal.remove(ATTR_USER_ALIAS_LIST);
            defaultVal.remove(ATTR_USER_AUTH_CONFIG);
            defaultVal.remove(ATTR_USER_FAILURE_URL);
            defaultVal.remove(ATTR_USER_SUCCESS_URL);
            defaultVal.remove(ATTR_STATIC_GRP_DN);
            defaultVal.remove(ATTR_MAIL);
            defaultVal.remove(ATTR_OC);
            defaultVal.remove(ATTR_POSTAL_ADDR);
            defaultVal.remove(ATTR_PREF_LOCALE);
            defaultVal.remove(ATTR_SN);
            defaultVal.remove(ATTR_AUTH_INVALID_DATA);
            defaultVal.remove(ATTR_MSISDN_NUM);

            sunServiceIdDS.remove(LDAPv3ForAD);
            // add to organization schema.
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAMDS,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER, defaultVal);
            // add to all instances.
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceIdDS,
                    ATTR_CONFIG_USER, defaultVal);

            // update default values for LDAPv3ForAD
            defaultVal.remove(ATTR_GIVEN_NAME);

            defaultVal.add(ATTR_DISPLAY_NAME);
            defaultVal.add(ATTR_OC);
            defaultVal.add(ATTR_MAIL);
            defaultVal.add(ATTR_USER_ACCT_LIFE);
            defaultVal.add(ATTR_USER_ALIAS_LIST);
            defaultVal.add(ATTR_USER_AUTH_CONFIG);
            defaultVal.add(ATTR_USER_FAILURE_URL);
            defaultVal.add(ATTR_USER_SUCCESS_URL);
            defaultVal.add(ATTR_DISPLAY_NAME);
            defaultVal.add(ATTR_OBJECT_GUID);
            defaultVal.add(ATTR_ACCT_NAME);
            defaultVal.add(ATTR_PRINCIPAL_NAME);
            defaultVal.add(ATTR_NAME);
            defaultVal.add(UNICODE_PASS);
            defaultVal.add(USER_ACCT_CTRL);

            // add to organization schema.
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER, defaultVal);
            // add to all instances.
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceIdAD,
                    ATTR_CONFIG_USER, defaultVal);

	    // update sunIdRepoAttributeMapping
	    defaultVal.clear();
	    defaultVal.add("userPassword=unicodePwd");
            UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_IDREPO_ATTR_MAPPING , defaultVal);
            // add to all instances.
            UpgradeUtils.addAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceIdAD,
                    ATTR_IDREPO_ATTR_MAPPING, defaultVal);

            // add attribute to schema
            fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE2);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, LDAPv3,
                    ORG_SCHEMA_TYPE, fileName);

            fileName = UpgradeUtils.getAbsolutePath(SERVICE_DIR,SCHEMA_FILE);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, LDAPv3,
                    ORG_SCHEMA_TYPE, fileName);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, LDAPv3ForAMDS,
                    ORG_SCHEMA_TYPE, fileName);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, fileName);

            // add attribute for LDAPv3ForAD subschema
            fileName = UpgradeUtils.getAbsolutePath(SERVICE_DIR,SCHEMA_FILE3);
            UpgradeUtils.addAttributeToSubSchema(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, fileName);

            // remove default value of attribute : 
            // sun-idrepo-ldapv3-config-people-container-name
            // from all instances of the plugin
            defaultVal.clear();
            defaultVal.add(OU);
            UpgradeUtils.removeAttrDefaultValueSubConfig(SERVICE_NAME, 
                    sunServiceID,
                    ATTR_CONFIG_USER_PC, defaultVal);

            // remove from schema.
            UpgradeUtils.removeAllAttributeDefaultValues(SERVICE_NAME,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER_PC, LDAPv3);

            // remove default value from attribute :
            //sun-idrepo-ldapv3-config-people-container-value
            defaultVal.clear();
            defaultVal.add(PEOPLE);
            // remove from all instances of the plugin
            UpgradeUtils.removeAttrDefaultValueSubConfig(SERVICE_NAME,
                    sunServiceID,
                    ATTR_CONFIG_USER_PC_VAL, defaultVal);

            // remove from schema.
            UpgradeUtils.removeAllAttributeDefaultValues(SERVICE_NAME,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_USER_PC_VAL, LDAPv3);

            // remove agent attributes from  : LDAPv3,LDAPv3forAMDS,LDAPv3forAD
            ArrayList attrList = new ArrayList();
            attrList.add(ATTR_AGENT_SEARCH);
            attrList.add(ATTR_AGENT_NAME);
            attrList.add(ATTR_AGENT_VALUE);
            attrList.add(ATTR_AGENT_SEARCH_FILTER);
            attrList.add(ATTR_AGENT_OC);
            attrList.add(ATTR_AGENT_ATTRS);

            sunServiceID.add(LDAPv3ForAMDS);
            sunServiceID.add(LDAPv3ForAD);

            UpgradeUtils.removeAttrFromSubConfig(
                    SERVICE_NAME, sunServiceID, attrList);
            i = sunServiceID.iterator();
            while (i.hasNext()) {
                String subSchema = (String) i.next();
                UpgradeUtils.removeAttributeSchemas(SERVICE_NAME, 
                        ORG_SCHEMA_TYPE,
                        attrList, subSchema);
            }

            // remove default value from attribute :
            // sun-idrepo-ldapv3-config-authenticatable-type
            defaultVal.clear();
            defaultVal.add(AGENT);
            // remove from all instances of the plugin
            UpgradeUtils.removeAttrDefaultValueSubConfig(SERVICE_NAME,
                    sunServiceID,
                    ATTR_CONFIG_USER_AUTH_TYPE, defaultVal);

            i = sunServiceID.iterator();
            while (i.hasNext()) {
                String subSchema = (String) i.next();
                // remove default value from schema.
                if (UpgradeUtils.debug.messageEnabled()) {
                    UpgradeUtils.debug.message("Removing " + defaultVal +
                            " from subschema : " + subSchema);
                }
                UpgradeUtils.removeAttributeDefaultValues(SERVICE_NAME,
                        ORG_SCHEMA_TYPE,
                        ATTR_CONFIG_USER_AUTH_TYPE, defaultVal, subSchema);

                // remove choice value from Schema
                if (UpgradeUtils.debug.messageEnabled()) {
                    UpgradeUtils.debug.message("Removing choice " + defaultVal +
                            " from subschema : " + subSchema);
                }
                UpgradeUtils.removeAttributeChoiceValues(
                        SERVICE_NAME, ORG_SCHEMA_TYPE,
                        ATTR_CONFIG_USER_AUTH_TYPE, defaultVal, subSchema);
            }

            // update for LDAPv3ForAD
            defaultVal.clear();
            defaultVal.add(USER_ACCT_CTRL);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_IS_ACTIVE, defaultVal);

            //change default value of ldapv3-config-active for AD
            // from Active to 544
            defaultVal.clear();
            defaultVal.add(CONFIG_ACTIVE_VALUE);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_ACTIVE, defaultVal);

            //change default value of sun-idrepo-ldapv3-config-inactive
            //from InActive to 546
            defaultVal.clear();
            defaultVal.add(CONFIG_INACTIVE_VALUE);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, LDAPv3ForAD,
                    ORG_SCHEMA_TYPE, ATTR_CONFIG_INACTIVE, defaultVal);

            // remove default value for sunOrganizationAliases if it is
            // serverhost.
            // this attribute is in OrganizationAttributeSchema
            // NOTE: Currently not working due to issue # 2336
            String serverHost = UpgradeUtils.getServerHost();
            Set valSet = UpgradeUtils.getAttributeValue(SERVICE_NAME, 
                    ATTR_ORG_ALIAS,
                    ORG_SCHEMA_TYPE, true);
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("valSet is :" + valSet);
                UpgradeUtils.debug.message("serverHost is :" + serverHost);
            }
            if ((valSet != null) && (valSet.contains(serverHost))) {
                defaultVal.clear();
                defaultVal.add(serverHost);
                UpgradeUtils.removeAttributeDefaultValues(SERVICE_NAME,
                        ORG_SCHEMA_TYPE, ATTR_ORG_ALIAS, defaultVal, true);
            }

            // add subconfiguration.
            defaultVal.clear();
            Map attrMap = new HashMap();
            defaultVal.add(AGENT_SERVICE);
            attrMap.put(ATTR_SERVICE_NAME, defaultVal);
            Set attrSet = new HashSet();
            attrSet.add(ATTR_AGENT);
            attrMap.put(CAN_HAVE_MEMBERS, attrSet);
            attrMap.put(CAN_ADD_MEMBERS, attrSet);
            UpgradeUtils.addSubConfiguration(SERVICE_NAME, null,
                    SUBCONFIG_AGENT_GROUP, SUPPORTED_IDS, attrMap, 0);

            defaultVal.clear();
            attrMap.clear();
            defaultVal.add(AGENT_SERVICE);
            attrMap.put(ATTR_SERVICE_NAME, defaultVal);
            UpgradeUtils.addSubConfiguration(SERVICE_NAME, null, 
                    SUBCONFIG_AGENT_ONLY,
                    SUPPORTED_IDS, attrMap, 0);

            // add attribute to agent subconfig
            attrMap.clear();
            defaultVal.clear();
            defaultVal.add(AGENT_GROUP);
            attrMap.put(CAN_BE_MEM_OF, defaultVal);
            UpgradeUtils.addAttributeToSubConfiguration(
                    SERVICE_NAME, SC_AGENT, attrMap);

            // set attribute values for amAdmin

            attrMap.clear();
            defaultVal.clear();
            defaultVal.add(USER_AMADMIN);
            attrMap.put(ATTR_SN, defaultVal);
            attrMap.put(ATTR_CN, defaultVal);
            attrMap.put(ATTR_GIVEN_NAME, defaultVal);
            defaultVal.clear();
            defaultVal.add(EMPTY_VALUE);
            attrMap.put(ATTR_EMPLOYEE_NUM, defaultVal);
            attrMap.put(ATTR_USER_ALIAS_LIST, defaultVal);
            attrMap.put(ATTR_USER_SUCCESS_URL, defaultVal);
            attrMap.put(ATTR_USER_FAILURE_URL, defaultVal);
            attrMap.put(ATTR_MAIL, defaultVal);
            attrMap.put(ATTR_POSTAL_ADDR, defaultVal);
            attrMap.put(ATTR_MSISDN_NUM, defaultVal);
            attrMap.put(ATTR_TELEPHONE_NUM, defaultVal);

            UpgradeUtils.addAttributeToSubConfiguration(
                    SERVICE_NAME, SUBCONFIG_AMADMIN, attrMap);

            // set attribute values for user anonymous
            defaultVal.clear();
            defaultVal.add(USER_ANON);
            attrMap.put(ATTR_SN, defaultVal);
            attrMap.put(ATTR_CN, defaultVal);
            attrMap.put(ATTR_GIVEN_NAME, defaultVal);
            UpgradeUtils.addAttributeToSubConfiguration(
                    SERVICE_NAME, SUBCONFIG_ANON, attrMap);

            // set empty i18NKey value for SubSchema files
            if (UpgradeUtils.debug.messageEnabled()) {
               UpgradeUtils.debug.message(classMethod + "Modify I18N Key" );
            }
            UpgradeUtils.modifyI18NKeyInSubSchema(SERVICE_NAME,
                    FILES, ORG_SCHEMA_TYPE, EMPTY_VALUE);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }
}
