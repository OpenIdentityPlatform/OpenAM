/* The contents of this file are subject to the terms
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
 * $Id: TestConstants.java,v 1.37 2009/05/29 16:20:13 rmisra Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.common;

public interface TestConstants {
    
    /**
     * Property name for AMConfig.properties file
     */
    String TEST_PROPERTY_AMCONFIG = "AMConfig";
    
    /**
     * Version number of OpenAM being used
     */
    String KEY_ATT_AM_VERSION = "openam_version";
    
    /**
     * Property name for logging level.
     */
    String KEY_ATT_LOG_LEVEL = "log_level";
    
    /**
     * Property key for <code>amadmin</code> user.
     */
    String KEY_ATT_AMADMIN_USER = "amadmin_username";
    
    /**
     * Property key for <code>amadmin</code> user password.
     */
    String KEY_ATT_AMADMIN_PASSWORD = "amadmin_password";
    
    /**
     * Property key for <code>com.iplanet.am.defaultOrg</code>.
     */
    String KEY_AMC_BASEDN = "com.iplanet.am.defaultOrg";
    
    /**
     * Property key for <code>planet.am.server.protocol</code>.
     */
    String KEY_AMC_PROTOCOL = "com.iplanet.am.server.protocol";
    
    /**
     * Property key for <code>com.iplanet.am.server.host</code>.
     */
    String KEY_AMC_HOST = "com.iplanet.am.server.host";
    
    /**
     * Property key for <code>com.iplanet.am.server.port</code>.
     */
    String KEY_AMC_PORT = "com.iplanet.am.server.port";
    
    /**
     * Property key for
     * <code>com.iplanet.am.services.deploymentDescriptor</code>.
     */
    String KEY_AMC_URI = "com.iplanet.am.services.deploymentDescriptor";
    
    /**
     * Property key for <code>com.iplanet.am.naming.url</code>.
     */
    String KEY_AMC_NAMING_URL = "com.iplanet.am.naming.url";
    
    /**
     * Property key for <code>com.iplanet.am.service.password</code>.
     */
    String KEY_AMC_SERVICE_PASSWORD = "com.iplanet.am.service.password";
    
    /**
     * Property key for <code>com.sun.identity.liberty.ws.wsc.certalias</code>.
     */
    String KEY_AMC_WSC_CERTALIAS = "com.sun.identity.liberty.ws.wsc.certalias";
    
    /**
     * Property key for <code>com.sun.identity.saml.xmlsig.keystore</code>.
     */
    String KEY_AMC_KEYSTORE = "com.sun.identity.saml.xmlsig.keystore";
    
    /**
     * Property key for <code>com.sun.identity.saml.xmlsig.keypass</code>.
     */
    String KEY_AMC_KEYPASS = "com.sun.identity.saml.xmlsig.keypass";
    
    /**
     * Property key for <code>com.sun.identity.saml.xmlsig.storepass</code>.
     */
    String KEY_AMC_STOREPASS = "com.sun.identity.saml.xmlsig.storepass";
    
    /**
     * Property key for <code>com.sun.identity.saml.xmlsig.certalias</code>.
     */
    String KEY_AMC_XMLSIG_CERTALIAS = "com.sun.identity.saml.xmlsig.certalias";
    
    /**
     * Property key for <code>com.sun.identity.idm.cache.enabled</code>.
     */
    String KEY_AMC_IDM_CACHE_ENABLED = "com.sun.identity.idm.cache.enabled";
    
    /**
     * Property key for <code>com.sun.identity.liberty.authnsvc.url</code>.
     */
    String KEY_AMC_AUTHNSVC_URL = "com.sun.identity.liberty.authnsvc.url";
    
    /**
     * Property key for <code>com.iplanet.am.notification.url</code>.
     */
    String KEY_AMC_NOTIFICATION_URL =
            "com.sun.identity.client.notification.url";
    
    /**
     * Property key for <code>com.sun.identity.agents.app.username</code>.
     */
    String KEY_AMC_AGENTS_APP_USERNAME = "com.sun.identity.agents.app.username";
    
    /**
     * Property key for <code>realm</code>.
     */
    String KEY_ATT_REALM = "realm";
    
    /**
     * Property key for <code>execution_realm</code>.
     */
    String KEY_ATT_EXECUTION_REALM = "execution_realm";
    
    /**
     * Property key for <code>subrealm_recursive_delete</code>.
     */
    String KEY_ATT_SUBREALM_RECURSIVE_DELETE = "subrealm_recursive_delete";

    /**
     * Property key for <code>testservername</code>.
     */
    String KEY_ATT_SERVER_NAME = "testservername";
    
    /**
     * Property key for <code>cookiedomain</code>.
     */
    String KEY_ATT_COOKIE_DOMAIN = "cookiedomain";
    
    /**
     * Property key for <code>config_dir</code>.
     */
    String KEY_ATT_CONFIG_DIR = "config_dir";
    
    /**
     * Property key for <code>am.encryption.pwd</code>.
     */
    String KEY_ATT_AM_ENC_PWD = "am.encryption.pwd";
    
    /**
     * Property key for <code>datastore</code>.
     */
    String KEY_ATT_CONFIG_DATASTORE = "datastore";
    
    /**
     * Property key for <code>directory_server</code>.
     */
    String KEY_ATT_DIRECTORY_SERVER = "directory_server";
    
    /**
     * Property key for <code>directory_port</code>.
     */
    String KEY_ATT_DIRECTORY_PORT = "directory_port";
    
    
    /**
     * Property key for <code>ds_adminport</code>.
     */
    String KEY_ATT_DS_ADMINPORT = "directory_admin_port";
    
    
    /**
     * Property key for <code>ds_jmxport</code>.
     */
    String KEY_ATT_DS_JMXPORT = "directory_jmx_port";
    
    /**
     * Property key for <code>config_root_suffix</code>.
     */
    String KEY_ATT_CONFIG_ROOT_SUFFIX = "config_root_suffix";
    
    /**
     * Property key for <code>ds_dirmgrdn</code>.
     */
    String KEY_ATT_DS_DIRMGRDN = "ds_dirmgrdn";
    
    /**
     * Property key for <code>ds_dirmgrpasswd</code>.
     */
    String KEY_ATT_DS_DIRMGRPASSWD = "ds_dirmgrpasswd";
    
    /**
     * Property key for <code>load_ums</code>.
     */
    String KEY_ATT_LOAD_UMS = "load_ums";
    
    /**
     * Property key for <code>umdatastore</code>.
     */
    String KEY_ATT_CONFIG_UMDATASTORE = "umdatastore";
    
    /**
     * Property key for <code>defaultorg</code>.
     */
    String KEY_ATT_DEFAULTORG = "defaultorg";
    
    /**
     * Property key for <code>productSetupResult</code>.
     */
    String KEY_ATT_PRODUCT_SETUP_RESULT = "productSetupResult";
    
    /**
     * Property key for <code>metaalias</code>.
     */
    String KEY_ATT_METAALIAS = "metaalias";
    
    /**
     * Property key for <code>entity_name</code>.
     */
    String KEY_ATT_ENTITY_NAME = "entity_name";
    
    /**
     * Property key for <code>cot</code>.
     */
    String KEY_ATT_COT = "cot";
    
    /**
     * Property key for <code>certalias</code>.
     */
    String KEY_ATT_CERTALIAS = "certalias";
    
    /**
     * Property key for <code>multiprotocol_enabled</code>
     */
    String KEY_ATT_MULTIPROTOCOL_ENABLED="multiprotocol_enabled";
    
    /**
     * Property key for <code>idff_sp</code>
     */
    String KEY_ATT_IDFF_SP="idff_sp";
    
    /**
     * Property key for <code>samlv2_sp</code>
     */
    String KEY_ATT_SAMLV2_SP="samlv2_sp";
    
    /**
     * Property key for <code>wsfed_sp</code>
     */
    String KEY_ATT_WSFED_SP="wsfed_sp";
    
    /**
     * Property key for <code>notification_sleep</code>.
     */
    String KEY_ATT_NOTIFICATION_SLEEP = "notification_sleep";
    
    /**
     * Property key for <code>internal.webapp.uri</code>.
     */
    String KEY_INTERNAL_WEBAPP_URI = "internal.webapp.uri";

    /**
     * Property key for <code>debug_dir</code>.
     */
    String KEY_DEBUG_DIR = "debug_dir";

    /**
     * Property key for <code>config_result</code>.
     */
    String KEY_CONFIG_RESULT = "config_result";

    /**
     * Property key for <code>client_txt</code>.
     */
    String KEY_CLIENT_TXT = "client_txt";

    /**
     * SAMLv2, IDFF SP related constants
     * Property key for <code>sp_host</code>
     */
    String KEY_SP_HOST = "sp_com.iplanet.am.server.host";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_protocol</code>
     */
    String KEY_SP_PROTOCOL = "sp_com.iplanet.am.server.protocol";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_port</code>
     */
    String KEY_SP_PORT = "sp_com.iplanet.am.server.port";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_deployment_uri</code>
     */
    String KEY_SP_DEPLOYMENT_URI =
            "sp_com.iplanet.am.services.deploymentDescriptor";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_metaalias</code>
     */
    String KEY_SP_METAALIAS = "sp_metaalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_entity_name</code>
     */
    String KEY_SP_ENTITY_NAME = "sp_entity_name";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_realm</code>
     */
    String KEY_SP_REALM = "sp_realm";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_execution_realm</code>
     */
    String KEY_SP_EXECUTION_REALM = "sp_execution_realm";
    
    /**
     * Property key for <code>sp_subrealm_recursive_delete</code>.
     */
    String KEY_SP_SUBREALM_RECURSIVE_DELETE = "sp_subrealm_recursive_delete";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_cot</code>
     */
    String KEY_SP_COT = "sp_cot";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_certalias</code>
     */
    String KEY_SP_CERTALIAS = "sp_certalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_config_dir</code>
     */
    String KEY_SP_CONFIG_DIR = "sp_config_dir";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_encryption_key</code>
     */
    String KEY_SP_ENC_KEY = "sp_encryption_key";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_datastore</code>
     */
    String KEY_SP_DATASTORE = "sp_datastore";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_amadmin_username</code>
     */
    String KEY_SP_AMADMIN_USER = "sp_amadmin_username";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_amadmin_password</code>
     */
    String KEY_SP_AMADMIN_PASSWORD = "sp_amadmin_password";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_directory_server</code>
     */
    String KEY_SP_DIRECTORY_SERVER = "sp_directory_server";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_directory_port</code>
     */
    String KEY_SP_DIRECTORY_PORT = "sp_directory_port";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_config_root_suffix</code>
     */
    String KEY_SP_CONFIG_ROOT_SUFFIX = "sp_config_root_suffix";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_ds_dirmgrdn</code>
     */
    String KEY_SP_DS_DIRMGRDN = "sp_ds_dirmgrdn";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_ds_dirmgrpasswd</code>
     */
    String KEY_SP_DS_DIRMGRPASSWORD = "sp_ds_dirmgrpasswd";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_load_ums</code>
     */
    String KEY_SP_LOAD_UMS = "sp_load_ums";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_cookiedomain</code>
     */
    String KEY_SP_COOKIE_DOMAIN = "sp_cookiedomain";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_user</code>
     */
    String KEY_SP_USER = "sp_user";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_userpw</code>
     */
    String KEY_SP_USER_PASSWORD = "sp_userpw";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_mail</code>
     */
    String KEY_SP_USER_MAIL = "sp_mail";
    
    
    /**
     * SAMLv2, IDFF Property key for <code>KEY_SP_GIVEN_NAME</code>
     */
    String KEY_SP_GIVEN_NAME = "sp_givenname";
    
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_employeenumber</code>
     */
    String KEY_SP_USER_EMPLOYEE = "sp_employeenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_postaladdress</code>
     */
    String KEY_SP_USER_POSTAL = "sp_post";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_telephonenumber</code>
     */
    String KEY_SP_USER_TELE = "sp_telephonenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_homephone</code>
     */
    String KEY_SP_USER_HOMEPHONE = "sp_homephone";
    /**
     * SAMLv2, IDFF Property key for <code>sp_homepostaladdress</code>
     */
    String KEY_SP_USER_HOMEPOSTAL = "sp_homepostaladdress";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_mobile</code>
     */
    String KEY_SP_USER_MOBILE = "sp_mobile";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_telephonenumber</code>
     */
    String KEY_SP_USER_TELEPHONE = "sp_telephonenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_pager</code>
     */
    String KEY_SP_USER_PAGER = "sp_pager";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_title</code>
     */
    String KEY_SP_USER_TITLE = "sp_title";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_sunIdentityMSISDNNumber</code>
     */
    String KEY_SP_USER_MSISDN = "sp_sunIdentityMSISDNNumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_server_alias</code>
     * This is server alias used for server alias configuration file under
     * resources directory
     */
    String KEY_IDP_SERVER_ALIAS = "idp_server_alias";

    /**
     * SAMLv2, IDFF Property key for <code>idp_title</code>
     */
    String KEY_IDP_USER_TITLE = "idp_title";

    /**
     * SAMLv2, IDFF Property key for <code>idp_employeenumber</code>
     */
    String KEY_IDP_USER_EMPLOYEE = "idp_employeenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_mail</code>
     */
    String KEY_IDP_GIVEN_NAME = "idp_givenname";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_postaladdress</code>
     */
    String KEY_IDP_USER_POSTAL = "idp_post";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_telephonenumber</code>
     */
    String KEY_IDP_USER_TELE = "idp_telephonenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_homephone</code>
     */
    String KEY_IDP_USER_HOMEPHONE = "idp_homephone";
    /**
     * SAMLv2, IDFF Property key for <code>idp_homepostaladdress</code>
     */
    String KEY_IDP_USER_HOMEPOSTAL = "idp_homepostaladdress";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_mobile</code>
     */
    String KEY_IDP_USER_MOBILE = "idp_mobile";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_telephonenumber</code>
     */
    String KEY_IDP_USER_TELEPHONE = "idp_telephonenumber";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_pager</code>
     */
    String KEY_IDP_USER_PAGER = "idp_pager";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_multiattributes</code>
     */
    String KEY_IDP_USER_MULTIATTRIBUTES = "idp_multiattributes";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_nsrole</code>
     */
    String KEY_IDP_USER_NSROLE = "idp_nsrole";
    
    /**
     * SAMLv2, IDFF Property key for <code>sp_sunIdentityMSISDNNumber</code>
     */
    String KEY_IDP_USER_MSISDN = "idp_sunIdentityMSISDNNumber";
    
    /**
     * SAMLv1 NameIDFormat key for <code>nameidformatkey</code>
     */
    String KEY_NAMEIDFORMAT_KEY = "nameidformatkey";
    
    /**
     * SAMLv1 NameIDFormat keyvalue for <code>nameidformatkeyvalue</code>
     */
    String KEY_NAMEIDFORMAT_KEYVALUE = "nameidformatkeyvalue";   
    
    /**
     * SAMLv1 attribute map for <code>attrmap</code>
     */
    String KEY_ATTRMAP = "attrmap";    
    
    /**
     * SAMLv1 attribute map for sp user multivalue attribute for
     * <code>sp_user_attribute</code>
     */
    String KEY_SP_USER_MULTIVALUE_ATTRIBUTE = "sp_user_attribute";
    
    /**
     * SAMLv1 attribute map for idp user multivalue attribute for
     * <code>idp_user_attribute</code>
     */
    String KEY_IDP_USER_MULTIVALUE_ATTRIBUTE = "idp_user_attribute";

    /**
     * Fedlet attribute map for <code>fedletidp_user</code>
     */
    String KEY_FEDLETIDP_USER = "fedletidp_user";

    /**
     * Fedlet attribute map for <code>fedletidp_entity_name</code>
     */
    String KEY_FEDLETIDP_ENTITY_NAME = "fedletidp_entity_name";

    /**
     * Fedlet attribute map for <code>fedletidp_userpw</code>
     */
    String KEY_FEDLETIDP_USER_PASSWORD = "fedletidp_userpw";

    /**
     * Fedlet attribute map for <code>fedlet_cot</code>
     */
    String KEY_FEDLET_COT = "fedlet_cot";    

    /**
     * Fedlet attribute map for <code>fedlet_name</code>
     */
    String KEY_FEDLET_NAME = "fedlet_name";  
    
    /**
     * Fedlet attribute map for <code>fedlet_attributes_mapping</code>
     */
    String KEY_FEDLET_ATT_MAP = "fedlet_attributes_mapping"; 
    
    /**
     * Fedlet attribute map for <code>fedlet_war_type</code>
     */
    String KEY_FEDLET_WAR_TYPE = "fedlet_war_type"; 
    
    /**
     * Fedlet attribute map for <code>fedlet_war_location</code>
     */
    String KEY_FEDLET_WAR_LOCATION = "fedlet_war_location";    
    
    /**
     * SAMLv2, IDFF Property key for <code>attrqprovider</code>
     */
    String KEY_SP_ATTRQPROVIDER = "attrqprovider";
    
    /**
     * SAMLv2, IDFF Property key for <code>spscertalias</code>
     */
    String KEY_SP_SCERTALIAS = "spscertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>attrqscertalias</code>
     */
    String KEY_SP_ATTRQ_SCERTALIAS = "attrqscertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>specertalias</code>
     */
    String KEY_SP_ECERTALIAS = "specertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>attrqecertalias</code>
     */
    String KEY_SP_ATTRQECERTALIAS = "attrqecertalias";
    
    /**
     * SAMLv2, IDFF IDP related constants
     * Property key for <code>idp_host</code>
     */
    String KEY_IDP_HOST = "idp_com.iplanet.am.server.host";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_port</code>
     */
    String KEY_IDP_PORT = "idp_com.iplanet.am.server.port";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_protocol</code>
     */
    String KEY_IDP_PROTOCOL = "idp_com.iplanet.am.server.protocol";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_deployment_uri</code>
     */
    String KEY_IDP_DEPLOYMENT_URI =
            "idp_com.iplanet.am.services.deploymentDescriptor";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_metaalias</code>
     */
    String KEY_IDP_METAALIAS = "idp_metaalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_entity_name</code>
     */
    String KEY_IDP_ENTITY_NAME = "idp_entity_name";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_realm</code>
     */
    String KEY_IDP_REALM = "idp_realm";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_execution_realm</code>
     */
    String KEY_IDP_EXECUTION_REALM = "idp_execution_realm";
    
    /**
     * Property key for <code>idp_subrealm_recursive_delete</code>.
     */
    String KEY_IDP_SUBREALM_RECURSIVE_DELETE = "idp_subrealm_recursive_delete";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_cot</code>
     */
    String KEY_IDP_COT = "idp_cot";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_encryption_key</code>
     */
    String KEY_IDP_ENC_KEY = "idp_encryption_key";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_certalias</code>
     */
    String KEY_IDP_CERTALIAS = "idp_certalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_config_dir</code>
     */
    String KEY_IDP_CONFIG_DIR = "idp_config_dir";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_datastore</code>
     */
    String KEY_IDP_DATASTORE = "idp_datastore";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_amadmin_username</code>
     */
    String KEY_IDP_AMADMIN_USER = "idp_amadmin_username";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_amadmin_password</code>
     */
    String KEY_IDP_AMADMIN_PASSWORD = "idp_amadmin_password";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_directory_server</code>
     */
    String KEY_IDP_DIRECTORY_SERVER = "idp_directory_server";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_directory_port</code>
     */
    String KEY_IDP_DIRECTORY_PORT = "idp_directory_port";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_config_root_suffix</code>
     */
    String KEY_IDP_CONFIG_ROOT_SUFFIX = "idp_config_root_suffix";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_ds_dirmgrdn</code>
     */
    String KEY_IDP_DS_DIRMGRDN = "idp_ds_dirmgrdn";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_ds_dirmgrpasswd</code>
     */
    String KEY_IDP_DS_DIRMGRPASSWORD = "idp_ds_dirmgrpasswd";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_load_ums</code>
     */
    String KEY_IDP_LOAD_UMS = "idp_load_ums";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_cookiedomain</code>
     */
    String KEY_IDP_COOKIE_DOMAIN = "idp_cookiedomain";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_user</code>
     */
    String KEY_IDP_USER = "idp_user";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_userpw</code>
     */
    String KEY_IDP_USER_PASSWORD = "idp_userpw";
    
    /**
     * SAMLv2, IDFF Property key for <code>idp_mail</code>
     */
    String KEY_IDP_USER_MAIL = "idp_mail";
    
    /**
     * SAMLv2, IDFF Property key for <code>attrauthority</code>
     */
    String KEY_IDP_ATTRAUTHOIRTY  = "attrauthority";
    
    /**
     * SAMLv2, IDFF Property key for <code>authnauthority</code>
     */
    String KEY_IDP_AUTHNAUTHORITY = "authnauthority";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpscertalias</code>
     */
    String KEY_IDP_IDPSCERTALIAS = "idpscertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>attrascertalias</code>
     */
    String KEY_IDP_ATTRASCERTALIAS = "attrascertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>authnascertalias</code>
     */
    String KEY_IDP_AUTHNASCERTALIAS = "authnascertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpecertalias</code>
     */
    String KEY_IDP_IDPECERTALIAS = "idpecertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>attraecertalias</code>
     */
    String KEY_IDP_ATTRAECERTALIAS = "attraecertalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>authnaecertalias</code>
     */
    String KEY_IDP_AUTHNAECERTALIAS = "authnaecertalias";
    
    /**
     * SAMLv2, IDFF IDP related constants
     * Property key for <code>idpProxy_host</code>
     */
    String KEY_IDP_PROXY_HOST = "idpProxy_com.iplanet.am.server.host";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_port</code>
     */
    String KEY_IDP_PROXY_PORT = "idpProxy_com.iplanet.am.server.port";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_protocol</code>
     */
    String KEY_IDP_PROXY_PROTOCOL = "idpProxy_com.iplanet.am.server.protocol";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_deployment_uri</code>
     */
    String KEY_IDP_PROXY_DEPLOYMENT_URI =
            "idpProxy_com.iplanet.am.services.deploymentDescriptor";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_metaalias</code>
     */
    String KEY_IDP_PROXY_METAALIAS = "idpProxy_metaalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_sp_metaalias</code>
     */
    String KEY_IDP_PROXY_SP_METAALIAS = "idpProxy_sp_metaalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_idp_metaalias</code>
     */
    String KEY_IDP_PROXY_IDP_METAALIAS = "idpProxy_idp_metaalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_entity_name</code>
     */
    String KEY_IDP_PROXY_ENTITY_NAME = "idpProxy_entity_name";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_realm</code>
     */
    String KEY_IDP_PROXY_REALM = "idpProxy_realm";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_execution_realm</code>
     */
    String KEY_IDP_PROXY_EXECUTION_REALM = "idpProxy_execution_realm";
    
    /**
     * Property key for <code>idpProxy_subrealm_recursive_delete</code>.
     */
    String KEY_IDP_PROXY_SUBREALM_RECURSIVE_DELETE =
            "idpProxy_subrealm_recursive_delete";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_cot</code>
     */
    String KEY_IDP_PROXY_COT = "idpProxy_cot";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_encryption_key</code>
     */
    String KEY_IDP_PROXY_ENC_KEY = "idpProxy_encryption_key";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_certalias</code>
     */
    String KEY_IDP_PROXY_CERTALIAS = "idpProxy_certalias";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_config_dir</code>
     */
    String KEY_IDP_PROXY_CONFIG_DIR = "idpProxy_config_dir";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_datastore</code>
     */
    String KEY_IDP_PROXY_DATASTORE = "idpProxy_datastore";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_amadmin_username</code>
     */
    String KEY_IDP_PROXY_AMADMIN_USER = "idpProxy_amadmin_username";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_amadmin_password</code>
     */
    String KEY_IDP_PROXY_AMADMIN_PASSWORD = "idpProxy_amadmin_password";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_directory_server</code>
     */
    String KEY_IDP_PROXY_DIRECTORY_SERVER = "idpProxy_directory_server";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_directory_port</code>
     */
    String KEY_IDP_PROXY_DIRECTORY_PORT = "idpProxy_directory_port";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_config_root_suffix</code>
     */
    String KEY_IDP_PROXY_CONFIG_ROOT_SUFFIX = "idpProxy_config_root_suffix";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_ds_dirmgrdn</code>
     */
    String KEY_IDP_PROXY_DS_DIRMGRDN = "idpProxy_ds_dirmgrdn";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_ds_dirmgrpasswd</code>
     */
    String KEY_IDP_PROXY_DS_DIRMGRPASSWORD = "idpProxy_ds_dirmgrpasswd";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_load_ums</code>
     */
    String KEY_IDP_PROXY_LOAD_UMS = "idpProxy_load_ums";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_user</code>
     */
    String KEY_IDP_PROXY_USER = "idpProxy_user";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_userpw</code>
     */
    String KEY_IDP_PROXY_USER_PASSWORD = "idpProxy_userpw";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpProxy_mail</code>
     */
    String KEY_IDP_PROXY_USER_MAIL = "idpProxy_mail";
    
    /**
     * SAMLv2, IDFF Property key for <code>ssoresult</code>
     */
    String KEY_SSO_RESULT = "ssoresult";
    
    /**
     * SAMLv2, IDFF Property key for <code>ssoinitresult</code>
     */
    String KEY_SSO_INIT_RESULT = "ssoinitresult";
    
    /**
     * SAMLv2, IDFF Property key for <code>spsloresult</code>
     */
    String KEY_SP_SLO_RESULT = "spsloresult";
    
    /**
     * SAMLv2, IDFF Property key for <code>terminateresult</code>
     */
    String KEY_TERMINATE_RESULT = "terminateresult";
    
    /**
     * SAMLv2, IDFF Property key for <code>idpsloresult</code>
     */
    String KEY_IDP_SLO_RESULT = "idpsloresult";
    
    /**
     * SAMLv2, IDFF Property key for <code>loginresult</code>
     */
    String KEY_LOGIN_RESULT = "loginresult";     
    
    /**
     * IDFF Property key for <code>nameregresult</code>
     */
    String KEY_NAME_REG_RESULT = "nameregresult";
    
    /**
     * IDFF Property key for <code>SSO Artifact Profile</code>
     */
    String SSO_BROWSER_ARTIFACT_VALUE =
            "<Value>http://projectliberty.org/profiles/brws-art</Value>";
    
    /**
     * IDFF Property key for <code>SSO Post Profile</code>
     */
    String SSO_BROWSER_POST_VALUE =
            "<Value>http://projectliberty.org/profiles/brws-post</Value>";
    
    /**
     * IDFF Property key for <code>SLO HTTP Profile</code>
     */
    String SLO_HTTP_PROFILE_VALUE =
            "<SingleLogoutProtocolProfile>http://projectliberty.org/profiles/" +
            "slo-idp-http</SingleLogoutProtocolProfile>";
    
    /**
     * IDFF Property key for <code>SLO SOAP Profile</code>
     */
    String SLO_SOAP_PROFILE_VALUE =
            "<SingleLogoutProtocolProfile>http://projectliberty.org/profiles/" +
            "slo-idp-soap</SingleLogoutProtocolProfile>";
    
    /**
     * IDFF Property key for <code>Registration HTTP Profile</code>
     */
    String REG_HTTP_PROFILE_VALUE =
            "<RegisterNameIdentifierProtocolProfile>http://" +
            "projectliberty.org/profiles/rni-idp-http" +
            "</RegisterNameIdentifierProtocolProfile>";
    
    /**
     * IDFF Property key for <code>Registration SOAP Profile</code>
     */
    String REG_SOAP_PROFILE_VALUE =
            "<RegisterNameIdentifierProtocolProfile>http://" +
            "projectliberty.org/profiles/rni-idp-soap" +
            "</RegisterNameIdentifierProtocolProfile>";
    
    /**
     * IDFF Property key for <code>Termination HTTP Profile</code>
     */
    String TERMIATION_HTTP_PROFILE_VALUE =
            "<FederationTerminationNotificationProtocolProfile>" +
            "http://projectliberty.org/profiles/fedterm-idp-http" +
            "</FederationTerminationNotificationProtocolProfile>";
    
    /**
     * IDFF Property key for <code>Termination SOAP Profile</code>
     */
    String TERMIATION_SOAP_PROFILE_VALUE =
            "<FederationTerminationNotificationProtocolProfile>" +
            "http://projectliberty.org/profiles/fedterm-idp-soap" +
            "</FederationTerminationNotificationProtocolProfile>";
    
    /**
     * MultiProtocol Property key for <code>idff_sp_user</code>
     */
    String KEY_IDFF_SP_USER = "idff_sp_user";
    
    /**
     * MultiProtocol Property key for <code>idff_sp_userpw</code>
     */
    String KEY_IDFF_SP_USER_PASSWORD = "idff_sp_userpw";
    
    /**
     * MultiProtocol Property key for <code>samlv2_sp_user</code>
     */
    String KEY_SAMLv2_SP_USER = "samlv2_sp_user";
    
    /**
     * MultiProtocol Property key for <code>samlv2_sp_userpw</code>
     */
    String KEY_SAMLv2_SP_USER_PASSWORD = "samlv2_sp_userpw";
    
    /**
     * MultiProtocol Property key for <code>wsfed_sp_user</code>
     */
    String KEY_WSFed_SP_USER = "wsfed_sp_user";
    
    /**
     * MultiProtocol Property key for <code>wsfed_sp_userpw</code>
     */
    String KEY_WSFed_SP_USER_PASSWORD = "wsfed_sp_userpw";
    
    /**
     * list-cot-members message for no entites present in the COT
     */
    String KEY_LIST_COT_NO_ENTITIES = "There are no trusted entities in the " +
            "circle of trust";
    
    /**
     * Authentication Property key for <code>dist_auth_enabled</code>
     */
    String KEY_DIST_AUTH_ENABLED = "dist_auth_enabled";
    
    /**
     * Authentication Property key for
     * <code>dist_auth_notification_service</code>
     */
    String KEY_DIST_AUTH_NOTIFICATION_SVC = "dist_auth_notification_service";

    /**
     * ID-WSF key for
     * <code>attribute_container</code>
     */
    String KEY_ATTRIBUTE_CONTAINER = "attribute_container";

    /**
     * ID-WSF key for
     * <code>attribute_name</code>
     */
    String KEY_ATTRIBUTE_NAME = "attribute_name";

    /**
     * ID-WSF key for
     * <code>attribute_value</code>
     */
    String KEY_ATTRIBUTE_VALUE = "attribute_value";

    /**
     * ID-WSF PP Modify success result text
     */
    String KEY_PP_MODIFY_RESULT = "pp:OK";

    /**
     * ID-WSF Sample configuration successful message. 
     */
    String KEY_WSC_CONFIG_SUCCESS_RESULT = "WSC Sample is configured";

}
