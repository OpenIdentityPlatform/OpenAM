/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UpgradeUtils.java,v 1.18 2009/09/30 17:35:24 goodearth Exp $
 *
 */
package com.sun.identity.upgrade;

import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.shared.ldap.util.DN;
import com.iplanet.am.admin.cli.Main;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSMigration70;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPConstraints;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.LDIF;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException; 
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Subject;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.LDAPv3;

/**
 * This class contains utilities to upgrade the service schema
 * configuration to be compatible with OpenSSO.
 * 
 */
public class UpgradeUtils {

    static Properties configTags;
    final static String SCHEMA_TYPE_GLOBAL = "global";
    final static String SCHEMA_TYPE_ORGANIZATION = "organization";
    final static String SCHEMA_TYPE_DYNAMIC = "dynamic";
    final static String SCHEMA_TYPE_USER = "user";
    final static String SCHEMA_TYPE_POLICY = "policy";
    final static String AUTH_SERVICE_NAME = "iPlanetAMAuthService";
    final static String AUTH_ATTR_NAME = "iplanet-am-auth-authenticators";
    final static String ATTR_ADMIN_AUTH_MODULE =
            "iplanet-am-auth-admin-auth-module";
    final static String ATTR_ORG_AUTH_MODULE = "iplanet-am-auth-org-config";
    final static int AUTH_SUCCESS =
            com.sun.identity.authentication.internal.AuthContext.AUTH_SUCCESS;
    final static String ORG_NAMING_ATTR = "o";
    final static String OU = "ou";
    final static String SERVICE_DN = "ou=services";
    final static String COMMA = ",";
    final static String EQUAL = "=";
    final static String AUTH_CONFIG_SERVICE = "iPlanetAMAuthConfiguration";
    final static String CONFIG_DN =
            "ou=Configurations,ou=default,ou=OrganizationConfig,ou=1.0,";
    final static String NAMED_CONFIG = "Configurations";
    final static String SUB_NAMED_CONFIG = "NamedConfiguration";
    final static String AUTH_ATTR_PREFIX = "iplanet-am-auth";
    final static String ATTR_AUTH_CONFIG = "iplanet-am-auth-configuration";
    final static String ATTR_AUTH_SUCCESS_URL =
            "iplanet-am-auth-login-success-url";
    final static String ATTR_AUTH_FAIL_URL =
            "iplanet-am-auth-login-failure-url";
    final static String ATTR_AUTH_POST_CLASS =
            "iplanet-am-auth-post-login-process-class";
    final static String START_VALUE = "<Value>";
    final static String END_VALUE = "</Value>";
    final static String ATTR_START_VALUE = "<AttributeValuePair>";
    final static String ATTR_END_VALUE = "</AttributeValuePair>";
    final static String HIDDEN_REALM =
            "/sunamhiddenrealmdelegationservicepermissions";
    final static String IDREPO_SERVICE = "sunIdentityRepositoryService";
    final static String IDFF_PROVIDER_SERVICE = 
        "iPlanetAMProviderConfigService";
    final static String IDFF_SERVICE_VERSION = "1.1";
    final static String SERVER_HOST = "com.iplanet.am.server.host";
    final static String SERVER_PORT = "com.iplanet.am.server.port";
    final static String SERVER_PROTO = "com.iplanet.am.server.protocol";

    static SSOToken ssoToken;
    public static Debug debug = Debug.getInstance("famUpgrade");
    private static String dsHostName;
    private static int dsPort;
    private static String bindDN = null;
    private static String bindPasswd = null;
    private static String deployURI = null;
    private static String dsAdminPwd;
    private static LDAPConnection ld = null;
    private static String basedir;
    private static String stagingDir;
    private static String configDir;
    private static String rootSuffix = SMSEntry.getRootSuffix();
    public static ResourceBundle bundle;
    static Map entityDescriptors = new HashMap();
    static Map entityConfigs = new HashMap();
    // will be passed on from the main upgrade class
    static String adminDN = null;
    static String adminPasswd = null;
    static String instanceType = null;
    // the following value will be passed down from the Main Upgrade program.
    // default dsMnanager dn.
    static String dsManager = "cn=Directory Manager";
    static String RESOURCE_BUNDLE_NAME = "ssoUpgrade";
    static String PRINCIPAL = "Principal";
    static String REALM_MODE = "realmMode";
    static String SERVER_DEFAULTS_FILE = "serverdefaults.properties";
    static String AUTH_CONFIG_SEARCH_FILTER =
            "(&(objectclass=LDAPsubentry) (cn=iplanetamauthconfiguration))";
    static String serverNameURL = null;
    final static String COS_TEMPL_FILTER = "objectclass=costemplate";
    final static String DELEGATION_SERVICE = "sunAMDelegationService";
    final static String ORG_ADMIN_ROLE = "Organization Admin Role";
    final static String DELEGATION_SUBJECT = "delegation-subject";
    final static String POLICY_SERVICE = "iPlanetAMPolicyService";
    final static String ORG_POLICY_ADMIN_ROLE =
            "Organization Policy Admin Role";
    final static String REALM_SERVICE = "sunAMRealmService";
    final static String REALM_READ_ONLY = "RealmReadOnly";
    final static String DATA_STORE_READ_ONLY = "DatastoresReadOnly";
    final static String AM_ID_SUBJECT = "AMIdentitySubject";
    final static String ATTR_SERVER_CONFIG = "serverconfig";
    final static String ATTR_SERVER_CONFIG_XML = "serverconfigxml";
    final static String CONFIG_SERVER_DEFAULT = "server-default";
    final static String SUB_SCHEMA_SERVER = "server";
    final static String SERVER_CONFIG_XML = "serverconfig.xml";
    final static String BACKUP_SERVER_CONFIG_XML = "serverconfig.xml.bak";
    final static String BACKUP_AMCONFIG = "AMConfig.properties.bak";
    final static String ATTR_SERVER_ID = "serverid";
    final static String ATTR_SUNSERVICE_ID = "sunserviceid";
    final static String ATTR_SUN_KEY_VALUE = "sunkeyvalue";
    final static String DIR_UPGRADE = "upgrade";
    final static String DIR_CONFIG = "config";
    final static String APPLICATION_SERVICE = "sunAMAuthApplicationService";
    final static String POLICY_CONFIG_XML = "amPolicyConfig.xml";
    final static String POLICY_XML = "amPolicy.xml";
    final static String PASSWORD_RESET_XML = "amPasswordReset.xml";
    final static String USER_XML = "amUser.xml";
    final static String REPO_XML = "idRepoService.xml";
    final static String UMS_XML = "ums.xml";
    final static String UNIX_XML = "amAuthUnix.xml";
    final static String DAI_LDIF = "FM_DAI_ds_remote_schema.ldif";
    final static String INSTALL_LDIF = "FM_DAI_install.ldif";
    static Hashtable propertyFileMap = new Hashtable();
    static {
        bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        // TODO change this , the properties should be 
        // read from AMConfig.properties. Currently
        // properties from AMConfig.properties are not
        // being picked up.
        System.setProperty("com.iplanet.services.debug.level", "message");
        System.setProperty("com.iplanet.am.version", "8.0");
        System.setProperty("installTime", "true");
        System.setProperty("donotIncludeSMSAuthModule", "true");
    }

    /**
     * Returns the SSOToken. 
     *
     * @return Admin Token.
     */
    public static SSOToken getSSOToken() {
        String classMethod = "UpgradeUtils:getSSOToken: ";
        if (ssoToken == null) {
            ssoToken = ldapLoginInternal(bindDN, bindPasswd);
        }
        if (ssoToken != null) {
            try {
                String principal = ssoToken.getProperty(PRINCIPAL);
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "Principal in SSOToken :" + principal);
                }
            } catch (Exception e) {
                debug.error(classMethod + "Error creating SSOToken ", e);
            }
        }
        return ssoToken;
    }

    /**
     * Creates a new service schema in the configuration store.
     * The service xml file passed should follow the SMS
     * DTD.
     *
     * @param fileName Name of the service schema XML to be loaded.
     * @throws UpgradeException if there is an error creating a service.
     * @supported.api
     */
    public static void createService(String fileName) throws UpgradeException {
        String classMethod = "UpgradeUtils:createService : ";
        replaceTag(fileName, configTags);
        if (debug.messageEnabled()) {
            debug.message(classMethod + fileName);
        }
        FileInputStream fis = null;
        try {
            ServiceManager ssm = getServiceManager();
            fis = new FileInputStream(fileName);
            ssm.registerServices(fis);
        } catch (FileNotFoundException fe) {
            debug.error(classMethod + "File not found: " + fileName, fe);
            throw new UpgradeException(fe.getMessage());
        } catch (SSOException ssoe) {
            debug.error(classMethod + "SSOToken is not valid", ssoe);
            throw new UpgradeException(ssoe.getMessage());
        } catch (SMSException sme) {
            debug.error(classMethod + "Invalid service schema xml" + fileName);
            throw new UpgradeException(sme.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ie) {
                //ignore if file input stream cannot be closed.
                }
            }
        }
    }

    /**
     * Adds new attribute schema to an existing service.
     * 
     * @param serviceName the service name.
     * @param schemaType the schema type.
     * @param attributeSchemafile 
     *         XML file containing attribute schema definition.
     * @throws UpgradeException if there is an error adding the 
     *         attribute schema.
     * @supported.api
     */
    public static void addAttributeToSchema(
            String serviceName,
            String schemaType,
            String attributeSchemaFile) throws UpgradeException {
        String classMethod = "UpgradeUtils:addAttributeToSchema: ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Adding attributeschema :" 
                    + attributeSchemaFile + "for service :" + serviceName);
        }
        FileInputStream fis = null;
        ServiceSchema ss = getServiceSchema(serviceName, null, schemaType);
        try {
            fis = new FileInputStream(attributeSchemaFile);
            ss.addAttributeSchema(fis);
        } catch (IOException ioe) {
            debug.error(classMethod + "File not found " + attributeSchemaFile);
            throw new UpgradeException(ioe.getMessage());
        } catch (SMSException sme) {
            debug.error(classMethod + "Cannot add attribute schema for " 
                    + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken : ", ssoe);
            throw new UpgradeException(ssoe.getMessage());
        }
    }

    /**
     * Adds new attribute schema to a sub schema in an existing service.
     *
     * @param serviceName the service name.
     * @param subSchemaName the sub schema name.
     * @param schemaType the schema type.
     * @param attributeSchemafile 
     *         XML file containing attribute schema definition.
     * @throws UpgradeException if there is an error adding the 
     *         attribute schema.
     * @supported.api
     */
    public static void addAttributeToSubSchema(
            String serviceName,
            String subSchemaName,
            String schemaType,
            String attributeSchemaFile) throws UpgradeException {
        String classMethod = "UpgradeUtils:addAttributeToSubSchema : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Adding attribute schema : " 
                    + attributeSchemaFile);
            debug.message(" to subSchema " + subSchemaName + 
                    " to service " + serviceName);
        }
        FileInputStream fis = null;
        ServiceSchema ss =
                getServiceSchema(serviceName, subSchemaName, schemaType);
        try {
            fis = new FileInputStream(attributeSchemaFile);
            ss.addAttributeSchema(fis);
        } catch (IOException ioe) {
            debug.error(classMethod + "File not found " + attributeSchemaFile);
            throw new UpgradeException(ioe.getMessage());
        } catch (SMSException sme) {
            debug.error(classMethod + "Cannot add attribute schema to : " 
                    + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken : ", ssoe);
            throw new UpgradeException(ssoe.getMessage());
        } catch (Exception e ) {
            debug.error(classMethod + "Error setting attribute schema : ", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Sets default values of an existing attribute.
     * The existing values will be overwritten with the new values.
     *
     * @param serviceName name of the service
     * @param subSchemaName name of the subschema
     * @param schemaType the type of schema.
     * @param attributeName name of the attribute
     * @param defaultValues a set of values to be added to the attribute
     * @throws UpgradeException if there is an error.
     * @supported.api
     */
    public static void setAttributeDefaultValues(
            String serviceName,
            String subSchemaName,
            String schemaType,
            String attributeName,
            Set defaultValues) throws UpgradeException {
        String classMethod = "UpgradeUtils:setAttributeDefaultValues : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + " for attribute :" + attributeName +
                    "in service :" + serviceName);
        }
        ServiceSchema ss =
                getServiceSchema(serviceName, subSchemaName, schemaType);
        try {
            ss.setAttributeDefaults(attributeName, defaultValues);
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken", ssoe);
            throw new UpgradeException(bundle.getString("invalidSSOToken"));
        } catch (SMSException sme) {
            debug.error("Unable to set default values for attribute " +
                    attributeName + " in service :" + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        }
    }

    /*
     * Adds default values to an existing attribute.
     * The existing values in the attribute will be updated with new values.
     *
     * @param serviceName name of the service
     * @param subSchemaName name of the subschema
     * @param schemaType the schemaType
     * @param attributeName name of the attribute
     * @param defaultValues a set of values to be added to the attribute
     * @throws <code>UpgradeException</code> if there is an error.
     * @supported.api
     */
    public static void addAttributeDefaultValues(
            String serviceName,
            String subSchemaName,
            String schemaType,
            String attributeName,
            Set defaultValues) throws UpgradeException {
        String classMethod = "UpgradeUtils:addAttributeDefaultValues : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Updating attribute default values");
            debug.message("in :" + serviceName +
                    "for attribute: " + attributeName);
        }
        ServiceSchema ss =
                getServiceSchema(serviceName, subSchemaName, schemaType);
        try {
            Map attributeDefaults = ss.getAttributeDefaults();
            Set oldAttrValues = (Set) attributeDefaults.get(attributeName);
            Set newAttrValues =
                    ((oldAttrValues == null) || oldAttrValues.isEmpty())
                    ? new HashSet() : new HashSet(oldAttrValues);
            newAttrValues.addAll(defaultValues);
            ss.setAttributeDefaults(attributeName, newAttrValues);
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid SSOToken");
        } catch (SMSException sme) {
            throw new UpgradeException("Failed to add attribute default " +
                    "values");
        }
    }
    /*
     * Add attribute choice values to an existing attribute.
     * The existing attribute values will be updated with new choice values.
     *
     * @param serviceName name of the service
     * @param subSchemaName name of the subschema
     * @param schemaType the schemaType
     * @param attributeName name of the attribute
     * @param choiceValuesMap a set of choice values values to
     *        be added to the attribute, the key is the i18NKey and
     *        the values it the choice value
     * @throws <code>UpgradeException</code> if there is an error.
     */

    public static void addAttributeChoiceValues(
            String serviceName,
            String subSchemaName,
            String schemaType,
            String attributeName,
            Map choiceValuesMap)
            throws UpgradeException {
        String classMethod = "UpgradeUtils.addAttributeChoiceValues";
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchemaName, schemaType);
            AttributeSchema attrSchema = ss.getAttributeSchema(attributeName);
            addChoiceValues(attrSchema, choiceValuesMap);
        } catch (SSOException ssoe) {
            throw new UpgradeException("Error getting SSOToken ");
        } catch (SMSException sme) {
            throw new UpgradeException("Error updating choice values ");
        }
    }

    /**
     * Add choice values to an attribute .
     */
    protected static void addChoiceValues(
            AttributeSchema attrSchema,
            Map choiceValMap) throws SMSException, SSOException {
        for (Iterator i = choiceValMap.keySet().iterator(); i.hasNext();) {
            String i18nKey = (String) i.next();
            Set valueSet = (Set) choiceValMap.get(i18nKey);
            String value = (String) valueSet.iterator().next();
            attrSchema.addChoiceValue(value, i18nKey);
        }
    }

    /*
     * Remove attribute values from sub realms.
     * The existing attribute values will be updated.
     *
     * @param serviceName name of the service
     * @param attributeName name of the attribute
     * @param attrVals a set of attrubute values to be removed.
     */
    public static void removeAttributeValuesFromRealms(
            String serviceName,
            String attributeName,
            Set attrVals) {
        String classMethod = "UpgradeUtils:removeAttributeValuesFromRealms : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod +
                    "Removing attribute values from :" + attributeName);
        }
        try {
            // get a list of realms - recursively.
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realm = (String) i.next();
                ocm = new OrganizationConfigManager(ssoToken, realm);
                ocm.removeAttributeValues(serviceName, attributeName, attrVals);
            }
        } catch (SMSException sme) {
            debug.error(classMethod + "Error updating attribute values ",sme);
        } catch (Exception e) {
            debug.error(classMethod + "Error updating attribute values ",e);
        }
    }

    /*
     * Add attribute values to sub realms.
     * The existing attribute values will be updated.
     *
     * @param serviceName name of the service
     * @param attributeName name of the attribute
     * @param attrValues a set of attrubute values to be added.
     */
    public static void addAttributeValuesToRealms(
            String serviceName,
            String attributeName,
            Set attrValues) {
        String classMethod = "UpgradeUtils:addAttributeValuesToRealms";
        try {
            // get a list of realms .
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realm = (String) i.next();
                ocm = new OrganizationConfigManager(ssoToken, realm);
                ocm.addAttributeValues(serviceName, attributeName, attrValues);
            }
        } catch (SMSException sme) {
            debug.error(classMethod + "Error adding attributes " , sme);
        } catch (Exception e) {
            debug.error(classMethod + "Error adding attributes ", e);
        }
    }

    /**
     * Removes attribute schema from an existing service.
     *
     * @param serviceName Service Name.
     * @param schemaType Schema Type.
     * @param atttrubuteName name of the attributes to be deleted.
     * @param subSchemaName Name of sub schema.
     * @throws <code>UpgradeException</code> if there is an error.
     */
    public static void removeAttributeSchema(
            String serviceName,
            String schemaType,
            String attributeName,
            String subSchemaName) throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeSchema:";
        ServiceSchema ss = null;
        try {
            ss = getServiceSchema(serviceName, subSchemaName, schemaType);
            ss.removeAttributeSchema(attributeName);
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid sso Token ");
        } catch (SMSException sme) {
            throw new UpgradeException("Unable to remove attribute schema");
        }
    }

    /**
     * Removes attribute schema from an existing service.
     *
     * @param serviceName Service Name.
     * @param schemaType Schema Type.
     * @param attributeSchemaNames a list of attribute schemas to be removed. 
     * @param subSchemaName Name of sub schema.
     * @throws <code>UpgradeException</code> if there is an error.
     */
    public static void removeAttributeSchemas(
            String serviceName,
            String schemaType,
            List attributeSchemaNames,
            String subSchemaName) throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeSchemas : ";
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchemaName, schemaType);
            for (Iterator i = attributeSchemaNames.iterator(); i.hasNext();) {
                String attributeSchemaName = (String) i.next();
                ss.removeAttributeSchema(attributeSchemaName);
            }
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSO Token", ssoe);
            throw new UpgradeException("Invalid SSO Token ");
        } catch (SMSException sme) {
            debug.error(classMethod + "Error removing attribute schema" +
                    attributeSchemaNames, sme);
            throw new UpgradeException(classMethod +
                    "Unable to remove attribute schema");
        }
    }

    /*
     * Remove attribute from sub realms.
     *
     * @param serviceName name of the service
     * @param attributeName name of the attribute
     * @throws <code>UpgradeException</code> if there is an error.
     */
    public static void removeAttributeFromRealms(
            String serviceName,
            String attributeName) throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeFromRealms : ";
        if (debug.messageEnabled()) {
            UpgradeUtils.debug.message(classMethod + "Removing attribute :" +
                    attributeName + "service : " + serviceName);
        }
        try {
            // get a list of realms - recursively.
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realm = (String) i.next();
                ocm = new OrganizationConfigManager(ssoToken, realm);
                ocm.removeAttribute(serviceName, attributeName);
            }
        } catch (SMSException sme) {
            throw new UpgradeException("Error removing attribute");
        }
    }

    /**
     * Migrates services from Legacy to Realm
     */
    public static void doMigration70() {
        String classMethod = "UpgradeUtils:doMigration70";
        try {
            SMSMigration70.migrate63To70(ssoToken, rootSuffix);
        } catch (Exception e) {
            debug.error(classMethod +
                    "Error migrating from Legacy to Realm", e);
        }
    }

    /** 
     * Sets the I18N File Name .
     *
     * @param serviceName name of the service.
     * @param i18NFileName the i18NFileName attribute value.
     * @throws <code>UpgradeException</code> when there is an error.
     */
    public static void seti18NFileName(
            String serviceName,
            String value) throws UpgradeException {
        String classMethod = "UpgradeUtils:seti18NFileName : ";
        try {
            ServiceSchemaManager ssm = getServiceSchemaManager(serviceName);
            ssm.setI18NFileName(value);
            if (debug.messageEnabled()) {
                debug.message(classMethod + serviceName + 
                    " :Setting I18NFileName " + value);
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid SSOToken ");
        } catch (SMSException sme) {
            throw new UpgradeException("Error setting i18NFileName value");
        }
    }

    /** 
     * Sets the service revision number.
     *
     * @param serviceName name of the service.
     * @param revisionNumber the revisionNumber of the service.
     * @throws <code>UpgradeException</code> if there is an error.
     */
    public static void setServiceRevision(
            String serviceName,
            String revisionNumber) throws UpgradeException {
        String classMethod = "UpgradeUtils:setServiceRevision : ";
        try {
            System.out.println(bundle.getString("upg-service-name") + ":"
                + serviceName);
            System.out.println(bundle.getString("upg-revision-number")
                + ":" + revisionNumber);
            if (debug.messageEnabled()) {
                debug.message("Setting service revision for :" + serviceName
                    + "to : " + revisionNumber);
            }
            ServiceSchemaManager ssm = getServiceSchemaManager(serviceName);
            ssm.setRevisionNumber(Integer.parseInt(revisionNumber));
            if (debug.messageEnabled()) {
                debug.message(classMethod + serviceName +
                        ":Setting Service Revision Number" + revisionNumber);
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid SSOToken ");
        } catch (SMSException sme) {
            throw new UpgradeException("Error setting serviceRevision value");
        }
    }

    /**
     * Updates the values of the <code>any</code> attribute in the attribute
     * schema.
     * 
     * @param serviceName the service name where the attribute exists.
     * @param subSchema the subschema name.
     * @param schemaType the schema type
     * @param attrName the attribute name.
     * @param value the value of the <code>any</code> attribute
     * @throws UpgradeException if there is an error.
     */
    public static void modifyAnyInAttributeSchema(
            String serviceName,
            String subSchema,
            String schemaType,
            String attrName,
            String value) throws UpgradeException {
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            AttributeSchema attrSchema = ss.getAttributeSchema(attrName);
            attrSchema.setAny(value);
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid token");
        } catch (SMSException sme) {
            throw new UpgradeException("Error setting any attribute");
        }
    }

    /**
     * Updates the values of the <code>i18NKey</code> attribute in the service`
     * subschema.
     * 
     * @param serviceName the service name where the attribute exists.
     * @param subSchema the subschema name.
     * @param schemaType the schema type
     * @param value the value of the <code>i18NKey</code> attribute
     * @throws UpgradeException if there is an error.
     */
    public static void modifyI18NKeyInSubSchema(
            String serviceName,
            String subSchema,
            String schemaType,
            String i18NKeyValue) throws UpgradeException {
        String classMethod = "UpgradeUtils:modifyI18NKeyInSubSchema : ";
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            ss.setI18Nkey(i18NKeyValue);
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken");
            throw new UpgradeException("Invalid SSOToken");
        } catch (SMSException sme) {
            debug.error(classMethod + 
                    "Error setting i18N key : " + serviceName,sme);
            throw new UpgradeException("Error setting i18NKey Value");
        }
    }
    
    /**
     * Returns the current service revision number .
     *
     * @param serviceName name of the service.
     * @return revisionNumber the service revision number.
     */
    public static int getServiceRevision(String serviceName) {
        int revisionNumber = -1;
        ServiceSchemaManager ssm = getServiceSchemaManager(serviceName);
        if (ssm != null) {
            revisionNumber = ssm.getRevisionNumber();
        }
        return revisionNumber;
    }

    /**
     * Returns true if the value of realmMode attribute is true.
     * If there is an error retrieving the attribute a false will be
     * assumed.
     * 
     * @return true if realmMode attribute value is true otherwise false.
     */
    public static boolean isRealmMode() {
        String classMethod = "UpgradeUtils:isRealmMode";
        boolean isRealmMode = false;
        getSSOToken();
        try {
            ServiceSchemaManager sm = getServiceSchemaManager(IDREPO_SERVICE);
            ServiceSchema ss = sm.getSchema(SCHEMA_TYPE_GLOBAL);
            Map attributeDefaults = ss.getAttributeDefaults();
            if (attributeDefaults.containsKey(REALM_MODE)) {
                HashSet hashSet = (HashSet) attributeDefaults.get(REALM_MODE);
                String value = (String) (hashSet.iterator().next());
                if (debug.messageEnabled()) {
                    debug.message("realmMode is : " + value);
                }
                if (value != null && value.equalsIgnoreCase("true")) {
                    isRealmMode = true;
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error retreiving the attribute", e);
        }
        return isRealmMode;
    }

    /**
     * Removes choice values from attribute schema.
     *
     * @param serviceName Name of service.
     * @param schemaType Type of schema.
     * @param attributeName Name of attribute.
     * @param choiceValues Choice values e.g. Inactive
     * @param subSchema Name of sub schema.
     * @throws UpgradeException if there is an error.
     */
    public static void removeAttributeChoiceValues(
            String serviceName,
            String schemaType,
            String attributeName,
            Set choiceValues,
            String subSchema) throws UpgradeException {
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            AttributeSchema attrSchema =
                    ss.getAttributeSchema(attributeName);
            for (Iterator i = choiceValues.iterator(); i.hasNext();) {
                String choiceValue = (String) i.next();
                attrSchema.removeChoiceValue(choiceValue);
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid SSOToken");
        } catch (SMSException sme) {
            throw new UpgradeException("Error removing attribute choice vals");
        }
    }

    /**
     * Removes attributes default values.
     * 
     * @param serviceName name of the service
     * @param schemaType the schema type
     * @param attributeName name of the attribute 
     * @param defaultValues a set of values to be removed 
     * @param subSchema name of the sub schema
     * @throws UpgradeException if there is an error
     */
    public static void removeAttributeDefaultValues(
            String serviceName,
            String schemaType,
            String attributeName,
            Set defaultValues,
            String subSchema) throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeDefaultValues : ";
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            // check if service schema exists.
            if (ss != null) {
                AttributeSchema attrSchema =
                        ss.getAttributeSchema(attributeName);
                for (Iterator i = defaultValues.iterator(); i.hasNext();) {
                    String defaultValue = (String) i.next();
                    attrSchema.removeDefaultValue(defaultValue);
                }
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid SSOToken");
        } catch (SMSException sme) {
            throw new UpgradeException("Error removing attribute" +
                    " default vals");
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error removing attribute default vals", e);
            throw new UpgradeException("Error removing attribute" +
                    " default values");
        }
    }

    /**
     * Adds sub schema to a service.
     *
     * @param servicename Name of service.
     * @param subSchema the subschema name.
     * @param schemaType the schema type.
     * @param filename Name of file that contains the sub schema
     * @throws UpgradeExcpetion if there is an error
     */
    public static void addSubSchema(
            String serviceName,
            String subSchema,
            String schemaType,
            String fileName) throws UpgradeException {
        String classMethod = "UpgradeUtils:addSubSchema : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Adding subschema :" + 
                fileName + "for service : " + serviceName);
        }
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            ss.addSubSchema(new FileInputStream(fileName));
        } catch (IOException ioe) {
            throw new UpgradeException("Error reading schema file ");
        } catch (SSOException ssoe) {
            throw new UpgradeException("invalid sso token");
        } catch (SMSException ssoe) {
            throw new UpgradeException("error creating subschema");
        }
    }

    /**
     * Adds SubConfiguration to a service.
     * 
     * @param serviceName the service name
     * @param svcConfigName the service config 
     * @param subConfigName the subconfig name 
     * @param subConfigID the subconfig id
     * @param attrValues a map of attribute value pairs to be added to the
     *        subconfig.
     * @param priority the priority value 
     * @throws UpgradeException if there is an error.
     */
    public static void addSubConfiguration(
            String serviceName,
            String svcConfigName,
            String subConfigName,
            String subConfigID,
            Map attrValues, int priority) throws UpgradeException {
        String classMethod = "UpgradeUtils:addSubConfiguration";
        try {
            ServiceConfigManager scm =
                    new ServiceConfigManager(serviceName, ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            if (sc != null) {
                sc.addSubConfig(subConfigName, subConfigID,
                        priority, attrValues);
            } else {
                debug.error(classMethod + 
                        "Error adding sub cofiguration" + subConfigName);
                throw new UpgradeException("Error adding subconfig");
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException("invalid sso token");
        } catch (SMSException sm) {
            debug.error(classMethod + "Error loading subconfig", sm);
            throw new UpgradeException("error adding subconfig");
        }
    }

    /**
     * Loads the ldif changes to the directory server.
     * 
     * @param ldifFileName the name of the ldif file.
     */
    public static void loadLdif(String ldifFileName) {
        String classMethod = "UpgradeUtils:loadLdif : ";
        try {
            System.out.println(bundle.getString("upg-load-ldif-file") 
                + " :" + ldifFileName);
            LDIF ldif = new LDIF(ldifFileName);
            ld = getLDAPConnection();
            LDAPUtils.createSchemaFromLDIF(ldif, ld);
        } catch (IOException ioe) {
            debug.error(classMethod +
                    "Cannot find file . Error loading ldif"+ldifFileName,ioe);
        } catch (LDAPException le) {
            debug.error(classMethod + "Error loading ldif" +ldifFileName,le);
        }
    }

    /**
     * Helper method to return Ldap connection
     *
     * @return Ldap connection
     */
    private static LDAPConnection getLDAPConnection() {
        String classMethod = "UpgradeUtils:getLDAPConnection : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Directory Server Host: " + dsHostName);
            debug.message(classMethod + "Directory Server Port: " + dsPort);
            debug.message(classMethod + "Direcotry Server DN: " + dsManager);
        }
        if (ld == null) {
            try {
                ld = new LDAPConnection();
                ld.setConnectTimeout(300);
                ld.connect(3, dsHostName, dsPort, dsManager, dsAdminPwd);
            } catch (LDAPException e) {
                disconnectDServer();
                ld = null;
                debug.error(classMethod + " Error getting LDAP Connection");
            }
        }
        return ld;
    }

    /**
     * Helper method to disconnect from Directory Server.
     */
    private static void disconnectDServer() {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
                ld = null;
            } catch (LDAPException e) {
                debug.error("Error disconnecting ", e);
            }
        }
    }


// Legacy code to support older upgrade data based on amAdmin dtd.
// These should not be used for the new data since these will be
// deprecated along with amAdmin.
// therefore not adding public javadocs for these.
    /**
     * Imports service data.
     * @param fileName the file containing the data in xml format.
     * @throws <code>UpgradeException</code> on error
     */
    public static void importServiceData(
            String fileName)
            throws UpgradeException {
        System.out.println(bundle.getString("upg-import-service-data")
            + ": " + fileName);
        String[] args = new String[8];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-t";
        args[7] = fileName;
        invokeAdminCLI(args);
    }

    /**
     * Imports service data
     * 
     * @param fileList list of files to be imported.
     * @throws UpgradeException on error.
     */
    public static void importServiceData(
            String[] fileList) throws UpgradeException {
        System.out.println(bundle.getString("upg-import-service-data")
                           + fileList);
        int len = fileList.length;
        String[] args = new String[7 + len];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-t";
        for (int i = 0; i < len; i++) {
            args[7 + i] = fileList[i];
        }
        invokeAdminCLI(args);
    }
    
    /**
     * Imports service data
     * 
     * @param fileList list of files to be imported.
     * @throws UpgradeException
     */
    public static void importServiceData(List fileList)
            throws UpgradeException {
        String classMethod = "UpgradeUtils:importServiceData : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Import Service Data :" + fileList);
        }
        System.out.println(bundle.getString("upg-import-service-data")
                           + fileList);
        int len = fileList.size();
        String[] args = new String[7 + len];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-t";

        for (int i = 0; i < len; i++) {
            args[7 + i] = (String) fileList.get(i);
        }
        invokeAdminCLI(args);
    }

    /**
     * Imports new service schema.
     * 
     * @param fileList list of files to be imported.
     * @throws UpgradeException on error.
     */
    public static void importNewServiceSchema(
            String[] fileList) throws UpgradeException {

        int len = fileList.length;
        String[] args = new String[7 + len];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-s";
        for (int i = 0; i < len; i++) {
            args[7 + i] = fileList[i];
        }
        invokeAdminCLI(args);
    }

    /**
     * Import new service schema
     * 
     * @param fileName name of the file to be imported.
     * @throws UpgradeException on error.
     */
    public static void importNewServiceSchema(
            String fileName) throws UpgradeException {
        String[] args = new String[8];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-s";
        args[7] = fileName;
        invokeAdminCLI(args);
    }

    /**
     * Imports new service schema.
     * 
     * @param fileList list of files
     * @throws UpgradeException
     */
    public static void importNewServiceSchema(
            List fileList) throws UpgradeException {
        int len = fileList.size();
        String[] args = new String[7 + len];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-s";
        for (int i = 0; i < len; i++) {
            args[7 + i] = (String) fileList.get(i);
        }
        invokeAdminCLI(args);
    }

    // getAttributeValue - retrieve attribute value
    public void getAttributeValue(String fileName) throws UpgradeException {
        String[] args = new String[8];
        args[0] = "--runasdn";
        args[1] = bindDN;
        args[2] = "-w";
        args[3] = bindPasswd;
        args[4] = "-c";
        args[5] = "-v";
        args[6] = "-t";
        args[7] = fileName;
        invokeAdminCLI(args);
    }

    /**
     * Returns the absolute path of new service schema xml file.
     * 
     * @param fileName name of the service xml.
     * @return the absolute path of the file.
     */
    public static String getNewServiceNamePath(String fileName) {
        StringBuffer sb = new StringBuffer();
        sb.append(basedir).append(File.separator).append("upgrade").
                append(File.separator).
                append("xml").append(File.separator).
                append(fileName);
        String path = sb.toString();
        return sb.toString();
    }

    /**
     * Returns the absolute path of the <code>serverdefaults</code>
     * properties file. This file is located in the staging directory 
     * under WEB-INF/classes.
     * 
     * @return the absolute path of the file.
     */
    public static String getServerDefaultsPath() {

        StringBuffer sb = new StringBuffer();
        sb.append(stagingDir).append(File.separator).
                append("WEB-INF").append(File.separator).
                append("classes").append(File.separator).
                append(File.separator).append(SERVER_DEFAULTS_FILE);

        return sb.toString();
    }

    /**
     * Returns the absolute path of the sms template files.
     * properties file. This file is located in the staging directory 
     * under WEB-INF/template/sms.
     * 
     * @return the absolute path of the file.
     */
    public static String getServiceTemplateDir(String SCHEMA_FILE) {

        StringBuffer sb = new StringBuffer();
        sb.append(stagingDir).append(File.separator).
                append("WEB-INF").append(File.separator).
                append("template").append(File.separator).
                append("sms").append(File.separator).
                append(SCHEMA_FILE);

        return sb.toString();
    }

    /**
     * Returns the absolute path of service schema xml file.
     * The new service schema file will be located in the
     * staging directory under WEB-INF/classes.
     * 
     * @param serviceName name of the service.
     * @param fileName name of the file.
     * @return the absolute path of the file.
     */
    public static String getAbsolutePath(String serviceName, String fileName) {
        StringBuffer sb = new StringBuffer();
        sb.append(basedir).append(File.separator).append("upgrade")
                .append(File.separator).append("services")
                .append(File.separator).append(serviceName)
                .append(File.separator).append("data")
                .append(File.separator).append(fileName);

        return sb.toString();
    }

    /**
     * Returns the ssoToken used for admin operations.
     * NOTE: this might be replaced later.
     * 
     * @param bindUser the user distinguished name.
     * @param bindPwd the user password
     * @return the <code>SSOToken</code>
     */
    private static SSOToken ldapLoginInternal(
            String bindUser,
            String bindPwd) {

        String classMethod = "UpgradeUtils:ldapLoginInternal : ";
        SSOToken ssoToken = null;
        try {
            com.sun.identity.authentication.internal.AuthContext ac =
                    getLDAPAuthContext(bindUser, bindPwd);
            if (ac.getLoginStatus() == AUTH_SUCCESS) {
                ssoToken = ac.getSSOToken();
            } else {
                ssoToken = null;
            }
        } catch (LoginException le) {
            debug.error(classMethod + "Error creating SSOToken", le);

        } catch (InvalidAuthContextException iace) {
            ssoToken = null;
            debug.error(classMethod + "Error creating SSOToken", iace);
        }
        return ssoToken;
    }

    /**
     * Returns the <code>AuthContext</code>.
     * 
     * @param bindUser the user distinguished name.
     * @param bindPwd the user password.
     * @return <code>AuthContext</code> object
     * @throws javax.security.auth.login.LoginException on error.
     */
    private static com.sun.identity.authentication.internal.AuthContext 
            getLDAPAuthContext(String bindUser, String bindPwd)
            throws LoginException {
        com.sun.identity.authentication.internal.AuthPrincipal principal =
                new com.sun.identity.authentication.internal.AuthPrincipal(
                bindUser);
        com.sun.identity.authentication.internal.AuthContext authContext =
                new com.sun.identity.authentication.internal.AuthContext(
                principal, bindPwd.toCharArray());
        return authContext;
    }

    // legacy code to invoke amadmin cli
    static void invokeAdminCLI(String[] args) throws UpgradeException {
        /*
         * Set the property to inform AdminTokenAction that
         * "amadmin" CLI is executing the program
         */
        SystemProperties.initializeProperties(
                AdminTokenAction.AMADMIN_MODE, "true");

        // Initialize Crypt class
        Crypt.checkCaller();

        Main dpa = new Main();
        try {
            dpa.parseCommandLine(args);
            dpa.runCommand();
        } catch (Exception eex) {
            throw new UpgradeException(eex.getMessage());
        }
    }

    // return the properties
    public static Properties getProperties(String file) {

        String classMethod = "UpgradeUtils:getProperties : ";
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (FileNotFoundException fe) {
            debug.error(classMethod + "File Not found" + file, fe);
        } catch (IOException ie) {
            debug.error(classMethod + "Error reading file" + file, ie);
        }
        propertyFileMap.put(file,properties);
        return properties;
    }

    /**
     * Checks the service scheam for existance of an attribute.
     * 
     * @param serviceName name of the service.
     * @param attributeName the attribute name
     * @param schemaType the schema type
     * @return true if attrbute exist else false.
     * @throws UpgradeException if there is an error
     */
    public static boolean attributeExists(
            String serviceName,
            String attributeName,
            String schemaType)
            throws UpgradeException {
        boolean isExists = false;
        try {
            ServiceSchemaManager sm = getServiceSchemaManager(serviceName);
            ServiceSchema ss = sm.getSchema(schemaType);
            Map attributeDefaults = ss.getAttributeDefaults();
            if (attributeDefaults.containsKey(attributeName)) {
                HashSet hashSet =
                        (HashSet) attributeDefaults.get(attributeName);
                String value = (String) (hashSet.iterator().next());
                isExists = true;
            }
        } catch (SMSException sme) {
            throw new UpgradeException("Error getting attribute value");
        }
        return isExists;
    }

    /**
     * Returns a value of an attribute.
     * This method assumes that the attribute is single valued.
     * 
     * @param serviceName name of the service.
     * @param attributeName name of the attribute.
     * @param schemaType the schema type.
     * @return the value of the attribute 
     * @throws UpgradeException if there is an error.
     */
    public static String getAttributeValueString(
            String serviceName,
            String attributeName,
            String schemaType) throws UpgradeException {
        String value = null;
        try {
            ServiceSchemaManager sm = getServiceSchemaManager(serviceName);
            ServiceSchema ss = sm.getSchema(schemaType);
            String attributeValue = null;
            Map attributeDefaults = ss.getAttributeDefaults();
            if (attributeDefaults.containsKey(attributeName)) {
                HashSet hashSet =
                        (HashSet) attributeDefaults.get(attributeName);
                value = (String) (hashSet.iterator().next());
            }
        } catch (SMSException sme) {
            throw new UpgradeException("Error getting attr value : " 
                    + sme.getMessage());
        }
        return value;
    }

    /**
     * Returns a set of values of an attribute.
     * 
     * @param serviceName name of the service.
     * @param attributeName the attribute name.
     * @param  schemaType the schema type.
     * @return a set of values for the attribute.
     * @throws UpgradeException if there is an error.
     */
    public static Set getAttributeValue(String serviceName,
            String attributeName,
            String schemaType) throws UpgradeException {
        return getAttributeValue(serviceName, attributeName, schemaType, false);
    }
    
    /**
     * Returns a set of values of an attribute.
     * 
     * @param serviceName name of the service.
     * @param attributeName the attribute name.
     * @param  schemaType the schema type.
     * @param isOrgAttrSchema boolean value indicating whether 
     *        the attribute is to be retrieved from 
     *        <OrganizationAttributeSchema>
     * @return a set of values for the attribute.
     * @throws UpgradeException if there is an error.
     */
    public static Set getAttributeValue(String serviceName,
            String attributeName, String schemaType, boolean isOrgAttrSchema)
            throws UpgradeException {
        String classMethod = "UpgradeUtils:getAttributeValue : ";
        Set attrValues = Collections.EMPTY_SET;
        try {
            ServiceSchemaManager sm = getServiceSchemaManager(serviceName);
            ServiceSchema ss = null;
            if (isOrgAttrSchema) {
                ss = sm.getOrganizationCreationSchema();
            } else {
                ss = sm.getSchema(schemaType);
            }
            Map attributeDefaults = ss.getAttributeDefaults();
            if (attributeDefaults.containsKey(attributeName)) {
                attrValues = (Set) attributeDefaults.get(attributeName);
            }
        } catch (SMSException sme) {
            debug.error(classMethod + 
                    "Error retreiving attribute values : ",sme);
            throw new UpgradeException("Unable to get attribute values : " 
                    + sme.getMessage());
        }
        return attrValues;
    }

    /**
     * Creates a site configuration.
     * 
     * @param siteURL the site URL.
     * @param accessPoints a set of access points for the site.
     * @throws UpgradeException if there is an error.
     */
    public static void createSite(String siteURL,
            Set accessPoints) throws UpgradeException {
        try {
            SiteConfiguration.createSite(ssoToken, siteURL,
                    siteURL, accessPoints);
        } catch (ConfigurationException ce) {
            throw new UpgradeException("Unable to create Service instance");
        } catch (SMSException sme) {
            throw new UpgradeException("Unable to add to site");
        } catch (SSOException ssoe) {
            throw new UpgradeException("invalid ssotoken");
        }
    }

    /**
     * Returns the server instance name.
     * The server instance is the server name appended with the
     * deployURI.
     * 
     * @param serverName name of the server
     * @return the server instance name.
     */
    public static String getServerInstance(String serverName) {
        if (serverName == null) {
            serverName = getServerName();
        }
        String deployURI = (String) configTags.get("DEPLOY_URI");
        if (serverName !=null && !serverName.endsWith(deployURI)){
            return serverName + "/" + deployURI;
        } else {
            return serverName;
        }
    }

    /** 
     * Creates a service instance.
     * 
     * @param serverInstance the server instance value
     * @param serverId the server identifier
     * @throws UpgradeException if there is an error.
     */
    public static void createServiceInstance(
            String serverInstance, String serverId) {
        String classMethod = "UpgradeUtils:createServiceInstance : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod +  "serverInstance :" + serverInstance);
            debug.message(classMethod + "serverId :" + serverId);
        }
        try {
            ServerConfiguration.createServerInstance(
                    ssoToken, serverInstance,
                    serverId, Collections.EMPTY_SET, "");
        } catch (Exception e) {
            debug.error(classMethod + " Error creating service instance ", e);
        }
    }

    /** 
     * Creates a service instance.
     * 
     * @param serverInstance the server instance value
     * @param serverId the server identifier
     * @throws UpgradeException if there is an error.
     */
    public static void createServiceInstance(
            String serverInstance, String serverId,
            Set values,String serverConfigXML) {
            //throws UpgradeException {
        String classMethod = "UpgradeUtils:createServiceInstance : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "serverInstance :" + serverInstance);
            debug.message(classMethod + "serverId :" + serverId);
        }
        try {
            ServerConfiguration.createServerInstance(
                    ssoToken, serverInstance,
                    serverId, values,serverConfigXML);
        } catch (UnknownPropertyNameException uce) {
            //throw new UpgradeException("Unknwon property ");
        } catch (ConfigurationException ce) {
            //throw new UpgradeException("Unable to create Service instance");
        } catch (SMSException sme) {
            //throw new UpgradeException("Unable to create Service instance");
        } catch (SSOException ssoe) {
            //throw new UpgradeException("invalid ssotoken");
        }
    }
    /**
     * Adds server to a site.
     *
     * @param serverInstance Name of the server instance.
     * @param siteId Identifier of the site.
     * @throws UpgradeException if there is an error.
     */
    public static void addToSite(
            String serverInstance,
            String siteId) throws UpgradeException {
        try {
            ServerConfiguration.addToSite(ssoToken, serverInstance, siteId);
        } catch (ConfigurationException ce) {
            throw new UpgradeException("Unable to add to site");
        } catch (SMSException sme) {
            throw new UpgradeException("Unable to add to site");
        } catch (SSOException ssoe) {
            throw new UpgradeException("Unable to add to site");
        }
    }

    /**
     * Adds attributes to service sub configuration.
     * 
     * @param serviceName the service name
     * @param subConfigName the sub configuration name
     * @param attrValues Map of attributes key is the attribute name and
     *        value a set of attribute values.
     * @throws UpgradeException on error.
     */
    public static void addAttributeToSubConfiguration(
            String serviceName,
            String subConfigName,
            Map attrValues) throws UpgradeException {
        String classMethod = "UpgradeUtils:addAttributeToSubConfiguration : " ;
        try {
            ServiceConfigManager scm = getServiceConfigManager(serviceName);
            ServiceConfig sc = scm.getGlobalConfig(null);

            StringTokenizer st = new StringTokenizer(subConfigName, "/");
            int tokenCount = st.countTokens();

            for (int i = 1; i <= tokenCount; i++) {
                String scn = st.nextToken();
                sc = sc.getSubConfig(scn);
            }

            for (Iterator i = attrValues.keySet().iterator(); i.hasNext();) {
                String attrName = (String) i.next();
                sc.addAttribute(attrName, (Set) attrValues.get(attrName));
            }
        } catch (SMSException sme) {
            throw new UpgradeException("Unable to add attribute to subconfig");
        } catch (SSOException ssoe) {
            throw new UpgradeException("invalid SSOToken");
        } catch (Exception e) {
            debug.error(classMethod + "Error adding attribute to subconfig:",e);
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Added attributes " + attrValues + 
                    " to subconfig " + subConfigName 
                    + " in service " + serviceName);
        }
    }

    // the following methods might change.
    /**
     * Sets the distinguished name of the admin user.
     * 
     * @param dn the dn of the admin user.
     */
    public static void setBindDN(String dn) {
        bindDN = dn;
    }

    /**
     * Sets the deploy uri of OpenSSO instance.
     * 
     * @param dn the dn of the admin user.
     */
    public static void setDeployURI(String uri) {
        deployURI = uri;
    }

    /**
     * Gets the deploy uri of OpenSSO instance.
     */
    public static String getDeployURI() {
        if (deployURI == null) {
            deployURI = (String) configTags.get("DEPLOY_URI");
        }
        return (deployURI);
    }

    /**
     * Sets the password of the admin user.
     * 
     * @param password the password the admin user.
     */
    public static void setBindPass(String password) {
        bindPasswd = password;
    }

    /**
     * Sets the Directory Server host name.
     * 
     * @param dsHost the directory server host name.
     */
    public static void setDSHost(String dsHost) {
        dsHostName = dsHost;
    }

    /**
     * Sets the directory server port.
     * 
     * @param port the directory server port number.
     */
    public static void setDSPort(int port) {
        dsPort = port;
    }

    /**
     * Sets the distinguished name of the directory server manager.
     * 
     * @param dn the dn of the directory server manager.
     */
    public static void setDirMgrDN(String dn) {
        dsManager = dn;
    }

    /**
     * Sets the password of the directory server manager user.
     * 
     * @param password the password the directory server manager.
     */
    public static void setdirPass(String pass) {
        dsAdminPwd = pass;
    }

    /**
     * Sets the location of the upgrade base directory.
     * 
     * @param dir the name of the upgrade base directory.
     */
    public static void setBaseDir(String dir) {
        basedir = dir;
    }

    /**
     * Sets the location of the staging directory.
     * 
     * @param dir the name of the staging directory.
     */
    public static void setStagingDir(String dir) {
        stagingDir = dir;
    }

    /**
     * Sets the configuration directory location
     * 
     * @param configDir the location of the config directory
     */
    public static void setConfigDir(String dir) {
        configDir = dir;
    }

    /**
     * Gets the configuration directory location
     */
    public static String getConfigDir() {
        return (configDir);
    }

    /**
     * Returns the <code>ServiceSchemaManager</code> for a service.
     * 
     * @param serviceName the service name
     * @return the <code>ServiceSchemaManager</code> of the service.
     */
    public static ServiceSchemaManager getServiceSchemaManager(
            String serviceName) {
        return getServiceSchemaManager(serviceName, ssoToken);
    }

    /**
     * Returns the <code>ServiceSchemaManager</code> for a service.
     * 
     * @param serviceName the service name
     * @param the admin SSOToken.
     * @return the <code>ServiceSchemaManager</code> of the service.
     */
    protected static ServiceSchemaManager getServiceSchemaManager(
            String serviceName,
            SSOToken ssoToken) {
        String classMethod = "UpgradeUtils:getServiceSchemaManager : ";
        ServiceSchemaManager mgr = null;
        if (serviceName != null) {
            try {
                if (serviceName.equals(IDFF_PROVIDER_SERVICE)) {
                    mgr = new ServiceSchemaManager(ssoToken,
                         serviceName,IDFF_SERVICE_VERSION);
                } else {
                    mgr = new ServiceSchemaManager(serviceName, ssoToken);
                }
            } catch (SSOException e) {
                debug.error(classMethod +
                        "SchemaCommand.getServiceSchemaManager", e);
            } catch (SMSException e) {
                debug.error(classMethod +
                        "SchemaCommand.getServiceSchemaManager", e);
            } catch (Exception e) {
                debug.error(classMethod + "Error : ", e);
            }
        }
        return mgr;
    }

    /**
     * Returns the <code>ServiceSchema</code> of a service.
     * 
     * @param serviceName the service name
     * @param subSchemaName the sub schema.
     * @param schemaType the schema type.
     * @return the <code>ServiceSchema</code> object.
     * @throws UpgradeException if there is an error.
     */
    static ServiceSchema getServiceSchema(String serviceName,
            String subSchemaName, String schemaType)
            throws UpgradeException {
        ServiceSchema ss = null;
        try {
            SchemaType sType = getSchemaType(schemaType);
            ServiceSchemaManager ssm = getServiceSchemaManager(serviceName);
            ss = ssm.getSchema(sType);
            if (subSchemaName != null) {
                ss = ss.getSubSchema(subSchemaName);
            }
        } catch (SMSException sme) {
            throw new UpgradeException("Cannot get service schema : " 
                    + sme.getMessage());
        }
        return ss;
    }

    /**
     * Returns the <code>SchemaType</code>
     * 
     * @param schemaTypeName the schema type string value
     * @return the <code>SchemaType</code> object.
     */
    private static SchemaType getSchemaType(String schemaTypeName) {
        SchemaType schemaType = null;
        if (schemaTypeName.equalsIgnoreCase(SCHEMA_TYPE_GLOBAL)) {
            schemaType = SchemaType.GLOBAL;
        } else if (schemaTypeName.equalsIgnoreCase(SCHEMA_TYPE_ORGANIZATION)) {
            schemaType = SchemaType.ORGANIZATION;
        } else if (schemaTypeName.equalsIgnoreCase(SCHEMA_TYPE_DYNAMIC)) {
            schemaType = SchemaType.DYNAMIC;
        } else if (schemaTypeName.equalsIgnoreCase(SCHEMA_TYPE_USER)) {
            schemaType = SchemaType.USER;
        } else if (schemaTypeName.equalsIgnoreCase(SCHEMA_TYPE_POLICY)) {
            schemaType = SchemaType.POLICY;
        }
        return schemaType;
    }

    /** 
     * Returns the <code>ServiceManager</code>.
     * 
     * @return the <code>ServiceManager</code> object.
     * @throws <code>UpgradeException</cpde> if there is an error.
     */
    private static ServiceManager getServiceManager() throws UpgradeException {
        ServiceManager ssm = null;
        try {
            ssm = new ServiceManager(ssoToken);
        } catch (SMSException e) {
            throw new UpgradeException("Error creating Service manager");
        } catch (SSOException e) {
            throw new UpgradeException("Invalid SSOToken");
        }
        return ssm;
    }

    /**
     * Adds module names to the list of authenticators in core auth
     * service.
     * 
     * @param moduleName a set of authentication module names.
     * @throws UpgradeException if there is an error.
     */
    public static void updateAuthenticatorsList(Set moduleName)
            throws UpgradeException {
        addAttributeDefaultValues(AUTH_SERVICE_NAME, null, SCHEMA_TYPE_GLOBAL,
                AUTH_ATTR_NAME, moduleName);
    }

    /**
     * Returns the <code>ServiceConfigManager</code> for a service.
     *
     * @param serviceName the service name
     * @return the <code>ServiceConfigManager</code> of the service.
     */
    protected static ServiceConfigManager getServiceConfigManager(
            String serviceName) {
        return getServiceConfigManager(serviceName, ssoToken);
    }

    /**
     * Returns the <code>ServiceConfigManager</code> for a service.
     *
     * @param serviceName the service name
     * @param the admin SSOToken.
     * @return the <code>ServiceConfigManager</code> of the service.
     */
    protected static ServiceConfigManager getServiceConfigManager(
            String serviceName,
            SSOToken ssoToken) {
        String classMethod = "UpgradeUtils:getServiceConfigManager : ";
        ServiceConfigManager scm = null;
        if (serviceName != null) {
            try {
                scm = new ServiceConfigManager(serviceName, ssoToken);
            } catch (SSOException e) {
                debug.error(classMethod, e);
            } catch (SMSException e) {
                debug.error(classMethod, e);
            }
        }
        return scm;
    }

    /**
     * Returns a list of assigned services for sub organizatons
     * 
     * @param orgName name of the organization to start.
     * @param serviceName name of the service
     * @return a map of assigned services where key is the realm name and 
     * values are a Set of assigned services.
     */
    public static Map getAssignedServices(String orgName, String serviceName) {
        String classMethod = "UpgradeUtils:getAssignedServices : ";
        Map map = new HashMap();
        try {
            getSSOToken();
            // get a list of realms - recursively.
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, orgName);
            Set as = ocm.getAssignedServices();
            Set serviceSet = new HashSet();
            if (as.contains(AUTH_CONFIG_SERVICE)) {
                serviceSet.add(getServiceDN(serviceName, orgName));
                map.put(orgName, serviceSet);
            }

            Set realms = ocm.getSubOrganizationNames("*", true);
            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realm = (String) i.next();
                ocm = new OrganizationConfigManager(ssoToken, realm);
                serviceSet = new HashSet();
                as = ocm.getAssignedServices();
                if (as.contains(serviceName)) {
                    serviceSet.add(getServiceDN(serviceName, realm));
                    map.put(realm, serviceSet);
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error retrieving assigned services", e);
        }
        return map;
    }

    /**
     * Returns the service dn
     * @param serviceName name of the service
     * @param orgName the organization name 
     * @return the distinguished name of the service.
     */
    static String getServiceDN(String serviceName, String orgName) {
        if (orgName.equals(rootSuffix)) {
            return OU + EQUAL + serviceName +
                    COMMA + SERVICE_DN + COMMA + rootSuffix;
        } else {
            return OU + EQUAL + serviceName + COMMA + SERVICE_DN +
                    COMMA + ORG_NAMING_ATTR + EQUAL 
                    + orgName + COMMA + rootSuffix;
        }
    }

    /**
     * Modifies the value of the attribute iplanet-am-auth-configuration
     * 
     * The value of module name is changed from fully qualified class name to 
     * the module name. 
     */
    public static void modifyAuthConfig() {
        String classMethod = "UpgradeUtils:modifyAuthConfig : ";
        try {
            String orgName = rootSuffix;
            OrganizationConfigManager org =
                    new OrganizationConfigManager(ssoToken, orgName);
            ServiceConfig orgConfig = org.getServiceConfig(AUTH_CONFIG_SERVICE);
            ServiceConfig s = orgConfig.getSubConfig(NAMED_CONFIG);
            if (s != null) {
                Iterator aa = s.getSubConfigNames().iterator();
                while (aa.hasNext()) {
                    String ss = (String) aa.next();
                    ServiceConfig sa = s.getSubConfig(ss);
                    Map map = sa.getAttributes();
                    Set valSet = map.keySet();
                    if (valSet.contains(ATTR_AUTH_CONFIG)) {
                        String attrName = ATTR_AUTH_CONFIG;
                        Set vals = (Set) map.get(attrName);
                        Set newValSet = new HashSet();
                        if (!vals.isEmpty()) {
                            Iterator vali = vals.iterator();
                            while (vali.hasNext()) {
                                String xml = (String) vali.next();
                                int idx = xml.indexOf(START_VALUE);
                                int ydx = xml.lastIndexOf(END_VALUE);
                                String val = xml.substring(idx + 7, ydx);
                                idx = val.lastIndexOf(".");
                                if (idx != -1) {
                                    String newVal = val.substring(idx + 1);
                                    newValSet.add(ATTR_START_VALUE +
                                            START_VALUE + newVal +
                                            END_VALUE + ATTR_END_VALUE);
                                }
                            }
                        }
                        map.put(attrName, newValSet);
                    }
                    sa.setAttributes(map);
                }
            }
        } catch (Exception e) {
            debug.error(classMethod +
                    "Error modifying auth configuration attribute", e);
        }
    }

    /**
     * Modifies the i18nKey of the specified attribute in the schema.
     * 
     * @param serviceName the service name where the attribute exists.
     * @param subSchema the subschema name.
     * @param schemaType the schema type
     * @param attrName the attribute name.
     * @param value the value of the i18nKey
     * @throws UpgradeException if there is an error.
     */
    public static void modifyI18NInAttributeSchema(
            String serviceName,
            String subSchema,
            String schemaType,
            String attrName,
            String value) throws UpgradeException
    {
        try {
            ServiceSchema ss =
                getServiceSchema(serviceName, subSchema, schemaType);
            AttributeSchema attrSchema = ss.getAttributeSchema(attrName);
            attrSchema.setI18NKey(value);
        } catch (SSOException ssoe) {
            throw new UpgradeException("Invalid token");
        } catch (SMSException sme) {
            throw new UpgradeException("Error setting i18N attribute");
        }
   } 

    /**
     * Migrates auth configuration defined at role level.
     */
    public static void migrateRoleAuthConfigs() {
        String classMethod = "UpgradeUtils:migrateRoleAuthConfigs : ";
        try {
            ld = getLDAPConnection();
            String baseDN = rootSuffix;
            LDAPSearchResults res =
                    ld.search(baseDN, LDAPv2.SCOPE_SUB,
                    AUTH_CONFIG_SEARCH_FILTER, null, false);
            Set attrSet = new HashSet();
            Set cosDNSet = new HashSet();
            Map dnAttrMap = new HashMap();
            while (res.hasMoreElements()) {
                LDAPEntry ent = res.next();
                String dn = ent.getDN();
                String[] myAttrs =
                        {ATTR_AUTH_CONFIG, ATTR_AUTH_SUCCESS_URL,
                    ATTR_AUTH_FAIL_URL, ATTR_AUTH_POST_CLASS
                };
                LDAPSearchResults res1 =
                        ld.search(dn, LDAPv2.SCOPE_SUB,
                        COS_TEMPL_FILTER, myAttrs, false);
                while (res1.hasMoreElements()) {
                    LDAPEntry ent1 = res1.next();
                    String dn1 = ent1.getDN();
                    // get attributes.
                    cosDNSet.add(dn1);
                    LDAPAttributeSet attrs = ent1.getAttributeSet();
                    Enumeration attrsInSet = attrs.getAttributes();
                    Map attrMap = new HashMap();
                    while (attrsInSet.hasMoreElements()) {
                        LDAPAttribute nextAttr =
                                (LDAPAttribute) attrsInSet.nextElement();
                        String attrName = nextAttr.getName();
                        if (attrName.startsWith(AUTH_ATTR_PREFIX)) {
                            Enumeration valsInAttr = nextAttr.getStringValues();
                            attrSet = new HashSet();
                            while (valsInAttr.hasMoreElements()) {
                                String nextValue =
                                        (String) valsInAttr.nextElement();
                                attrSet.add(nextValue);
                            }
                            attrMap.put(attrName, attrSet);
                        }
                    }
                    dnAttrMap.put(dn1, attrMap);
                }
            }
            // ldapmodify
            Iterator i = cosDNSet.iterator();
            Map orgRoleMap = new HashMap();
            while (i.hasNext()) {
                String dn = (String) i.next();
                Map attrMap1 = (HashMap) dnAttrMap.get(dn);
                int rcn = dn.indexOf("=\"cn=");
                int end = dn.indexOf(",");
                String roleName = dn.substring(rcn + 5, end);
                String remStr = dn.substring(end + 1);
                String orgName = remStr.substring(0, remStr.indexOf("\""));
                String rolConfigName = roleName + "-roleauthconfig";
                Set rSet;
                if (!orgRoleMap.containsKey(orgName)) {
                    rSet = new HashSet();
                } else {
                    rSet = (HashSet) orgRoleMap.get(orgName);
                }
                Map roleMap = new HashMap();
                roleMap.put(rolConfigName, attrMap1);
                rSet.add(roleMap);
                orgRoleMap.put(orgName, rSet);
                Collection modifications = new ArrayList();
                LDAPModificationSet lds = new LDAPModificationSet();
                if (attrMap1.containsKey(ATTR_AUTH_CONFIG)) {
                    lds.add(LDAPModification.REPLACE,
                            new LDAPAttribute(ATTR_AUTH_CONFIG, rolConfigName));
                }
                if (attrMap1.containsKey(ATTR_AUTH_SUCCESS_URL)) {
                    lds.add(LDAPModification.DELETE,
                            new LDAPAttribute(ATTR_AUTH_SUCCESS_URL));
                }
                if (attrMap1.containsKey(ATTR_AUTH_FAIL_URL)) {
                    lds.add(LDAPModification.DELETE,
                            new LDAPAttribute(ATTR_AUTH_FAIL_URL));
                }
                if (attrMap1.containsKey(ATTR_AUTH_POST_CLASS)) {
                    lds.add(LDAPModification.DELETE,
                            new LDAPAttribute(ATTR_AUTH_POST_CLASS));
                }
                ld.modify(dn, lds);
            }
            // iterate thru orgs to create configs.
            Set orgSet = orgRoleMap.keySet();
            Iterator orgI = orgSet.iterator();
            while (orgI.hasNext()) {
                String oName = (String) orgI.next();
                OrganizationConfigManager org =
                        new OrganizationConfigManager(ssoToken, oName);
                ServiceConfig s = org.getServiceConfig(AUTH_CONFIG_SERVICE);
                ServiceConfig authConfig = s.getSubConfig(NAMED_CONFIG);
                if (authConfig == null) {
                    s.addSubConfig(NAMED_CONFIG, null, 0, null);
                    authConfig = s.getSubConfig(NAMED_CONFIG);
                }
                Set roleMapSet = (HashSet) orgRoleMap.get(oName);
                Iterator c = roleMapSet.iterator();
                while (c.hasNext()) {
                    Map rMap = (HashMap) c.next();
                    Set keySet = rMap.keySet();
                    String cName = (String) keySet.iterator().next();
                    Map aMap = (HashMap) rMap.get(cName);
                    authConfig.addSubConfig(cName, SUB_NAMED_CONFIG, 0, aMap);
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error migrating role auth config", e);
            disconnectDServer();
            ld = null;
        }
    }

    /**
     * Migrates Core Auth Service auth module configuration.
     */
    public static void migrateAuthServiceConfigs() {
        String classMethod = "UpgradeUtils:migrateAuthServiceConfigs : ";
        try {
            OrganizationConfigManager org =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realms = org.getSubOrganizationNames("*", true);
            realms.add(rootSuffix);

            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realmName = (String) i.next();
                createOrgAuthConfig(realmName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            debug.error(classMethod + "Error migrating role auth config", e);
            disconnectDServer();
            ld = null;
        }
    }

    /**
     * Creates auth configurations for auth modules configuration in
     * core auth service.
     */
    private static void createOrgAuthConfig(String realmName) throws Exception {

        String classMethod = "UpgradeUtils:createOrgAuthConfig: ";
        OrganizationConfigManager org =
                new OrganizationConfigManager(ssoToken, realmName);
        ServiceConfig orgConfig = org.getServiceConfig(AUTH_SERVICE_NAME);
        if (orgConfig != null) {
            Map aa = orgConfig.getAttributes();
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Org is :" + realmName);
                debug.message(classMethod + "Attribute Map is :" + aa);
            }
            String orgName = realmName;
            if (DN.isDN(realmName)) {
                orgName = LDAPDN.explodeDN(realmName, true)[0];
            }
            String authConfigName = orgName + "-authconfig";
            String adminAuthConfigName = orgName + "-admin-authconfig";
            Set authConfigAttrValue =
                    (Set) aa.get(ATTR_ORG_AUTH_MODULE);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "authConfigAttrValue : " 
                        + authConfigAttrValue);
            }
            Set newVal = new HashSet();
            if (authConfigAttrValue.size() != 1 &&
                    !authConfigAttrValue.contains(authConfigName)) {
                newVal.add(authConfigName);
                orgConfig.replaceAttributeValues(
                        ATTR_ORG_AUTH_MODULE, authConfigAttrValue, newVal);
            }
            Set adminConfigAttrValue = (Set) aa.get(ATTR_ADMIN_AUTH_MODULE);
            if (debug.messageEnabled()) {
                debug.message("adminauthConfigAttrValue : " 
                        + adminConfigAttrValue);
            }
            if (adminConfigAttrValue.size() != 1 &&
                    !adminConfigAttrValue.contains(adminAuthConfigName)) {
                newVal.clear();
                newVal.add(adminAuthConfigName);
                orgConfig.replaceAttributeValues(ATTR_ADMIN_AUTH_MODULE,
                        adminConfigAttrValue, newVal);
            }
            aa = orgConfig.getAttributes();
            ServiceConfig s = org.getServiceConfig(AUTH_CONFIG_SERVICE);
            ServiceConfig authConfig = s.getSubConfig(NAMED_CONFIG);
            if (authConfig == null) {
                s.addSubConfig(NAMED_CONFIG, null, 0, null);
                authConfig = s.getSubConfig(NAMED_CONFIG);
            }
            Map aMap = new HashMap();
            aMap.put(ATTR_AUTH_CONFIG, authConfigAttrValue);
            authConfig.addSubConfig(authConfigName, SUB_NAMED_CONFIG, 0, aMap);
            aMap.clear();
            aMap.put(ATTR_AUTH_CONFIG, adminConfigAttrValue);
            authConfig.addSubConfig(adminAuthConfigName,
                    SUB_NAMED_CONFIG, 0, aMap);
        }
    }

    /**
     * Creates Delegation Policies.
     */
    public static void loadSubRealmDelegationPolicies() {
        String classMethod = "UpgradeUtils:loadSubRealmDelegationPolicies : ";
        try {
            // get a list of realms - recursively.
            PolicyManager pm = new PolicyManager(ssoToken, HIDDEN_REALM);
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            Iterator i = realms.iterator();
            while (i.hasNext()) {
                String realm = (String) i.next();
                String orgDN = DNMapper.orgNameToDN(realm);
                String orgID = orgDN.replaceAll(",", "^");
                if (debug.messageEnabled()) {
                    System.out.println(bundle.getString("upg-org-name") 
                    + " : " + realm);
                    System.out.println(bundle.getString("upg-org-dn")
                         + orgDN);
                    System.out.println(bundle.getString("upg-org-dn") + ":" 
                         + orgID);
                }
                createRealmAdminPolicy(pm, orgDN, orgID);
                createPolicyAdminPolicy(pm, orgDN, orgID);
                createRealmReadOnlyPolicy(pm, orgDN, orgID);
                createDatastoresReadOnlyPolicy(pm, orgDN, orgID);
            }

        } catch (Exception e) {
            debug.error(classMethod +
                    "Error loading sub realm delegation polices", e);
        }
    }

    /**
     * Returns <code>AttributeType</code> object represent an 
     * <code>Attribute</code> and <code>Value</code> in the IDFF meta data
     * 
     * @param attrElement <code>AttributeType</code> object
     * @param attrName the attribute name.
     * @param attrValue the attribute value.
     * @return <code>AttributeType</code> object.
     */
    public static AttributeType getAttribute(AttributeType attrElement,
            String attrName, String attrValue) {
        attrElement.setName(attrName);
        attrElement.getValue().add(attrValue);
        return attrElement;
    }

    /**
     * Returns value of an attribute.
     * @param attrName name of the attribute.
     * @param attrs Map of attributes where key is the attribute name
     *        and values are a set of attributes.
     * @return the value of attribute if it is found else null.
     */
    public static String getAttributeString(String attrName, Map attrs) {
        return getAttributeString(attrName, attrs, null);
    }

    /**
     * Returns value of an attribute.
     * 
     * @param attrName name of the attribute.
     * @param attrs Map of attributes where key is the attribute name
     *          and values are a set of attributes.
     * @param defaultValue the default value to be returned if value 
     *          is not found.
     * @return the value of attribute if it is found else returns 
     *          the defaultValue.
     */
    public static String getAttributeString(String attrName, Map attrs,
            String defaultValue) {
        String attrValue = defaultValue;
        Set attrValSet = (Set) attrs.get(attrName);
        if (attrValSet != null && !attrValSet.isEmpty()) {
            attrValue = (String) (attrValSet.toArray())[0];
        }
        return attrValue;
    }

    /**
     * Creates Realm Admin Policy.
     * 
     * @param policyManager the policy manager object.
     * @param orgDN the organization dn.
     * @param orgID the organization identifier.
     */
    private static void createRealmAdminPolicy(PolicyManager policyManager,
            String orgDN, String orgID) {
        String classMethod = "UpgradeUtils:createRealmAdminPolicy";
        try {
            String policyName = orgID + "^^RealmAdmin";
            Policy realmPolicy = new Policy(policyName, null, false, true);
            // create Rule
            String resourceName = "sms://*" + orgDN + "/*";

            Rule rule = getRule(DELEGATION_SERVICE, resourceName);

            if (rule != null) {
                realmPolicy.addRule(rule);
            }

            String universalID = getUniversalID(orgDN, ORG_ADMIN_ROLE);
            Subject subject = getSubject(policyManager, universalID);
            if (subject != null) {
                realmPolicy.addSubject(DELEGATION_SUBJECT, subject, false);
            }
            policyManager.addPolicy(realmPolicy);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating realm admin policy", e);
        }
    }

    /**
     * Creates Policy Admin Policy.
     * 
     * @param policyManager the policy manager object.
     * @param orgDN the organization dn.
     * @param orgID the organization identifier.
     */
    private static void createPolicyAdminPolicy(PolicyManager policyManager,
            String orgDN, String orgID) {
        String classMethod = "UpgradeUtils:createRealmReadOnlyPolicy";
        try {
            String policyName = orgID + "^^PolicyAdmin";
            Policy realmPolicy = new Policy(policyName, null, false, true);
            // create Rule
            String resourceName = "sms://*" + orgDN + "/" + POLICY_SERVICE;
            Rule rule = getRule(DELEGATION_SERVICE, resourceName);
            if (rule != null) {
                realmPolicy.addRule(rule);
            }
            // add subjects
            String policyAdminRoleUniversalID =
                    getUniversalID(orgDN, ORG_POLICY_ADMIN_ROLE);
            Subject subject =
                    getSubject(policyManager, policyAdminRoleUniversalID);
            if (subject != null) {
                realmPolicy.addSubject(DELEGATION_SUBJECT, subject, false);
            }
            policyManager.addPolicy(realmPolicy);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating policy admin policy", e);
        }
    }

    /**
     * Creates Realm Read Only Policy
     * 
     * @param policyManager the policy manager object.
     * @param orgDN the organization dn.
     * @param orgID the organization identifier.
     */
    private static void createRealmReadOnlyPolicy(PolicyManager policyManager,
            String orgDN, String orgID) {
        String classMethod = "UpgradeUtils:createRealmReadOnlyPolicy";
        try {
            String policyName = orgID + "^^" + REALM_READ_ONLY;
            Policy realmPolicy = new Policy(policyName, null, false, true);
            // create Rule
            String serviceName = DELEGATION_SERVICE;
            String resourceName = "sms://*" + orgDN + "/" + REALM_SERVICE;
            Rule rule = getRule(serviceName, resourceName);
            if (rule != null) {
                realmPolicy.addRule(rule);
            }
            // add subjects
            String policyAdminRoleUniversalID =
                    getUniversalID(orgDN, ORG_POLICY_ADMIN_ROLE);
            Subject subject =
                    getSubject(policyManager, policyAdminRoleUniversalID);
            if (subject != null) {
                realmPolicy.addSubject(DELEGATION_SUBJECT, subject, false);
            }
            policyManager.addPolicy(realmPolicy);
        } catch (Exception e) {
            debug.error(classMethod +
                    "Error creating realm read only policy", e);
        }
    }

    /**
     * Creates DataStores Read Only Policy
     * 
     * @param policyManager the policy manager object.
     * @param orgDN the organization dn.
     * @param orgID the organization identifier.
     */
    private static void createDatastoresReadOnlyPolicy(
            PolicyManager policyManager, String orgDN, String orgID) {
        String classMethod = "UpgradeUtils:createDatastoresReadOnlyPolicy";
        try {
            String policyName = orgID + "^^" + DATA_STORE_READ_ONLY;
            Policy realmPolicy = new Policy(policyName, null, false, true);
            // create Rule
            String serviceName = DELEGATION_SERVICE;
            String resourceName = "sms://*" + orgDN + "/" + IDREPO_SERVICE;
            Rule rule = getRule(serviceName, resourceName);
            if (rule != null) {
                realmPolicy.addRule(rule);
            }
            // add subjects
            String policyAdminRoleUniversalID =
                    getUniversalID(orgDN, ORG_POLICY_ADMIN_ROLE);
            Subject subject =
                    getSubject(policyManager, policyAdminRoleUniversalID);
            if (subject != null) {
                realmPolicy.addSubject(DELEGATION_SUBJECT, subject, false);
            }
            policyManager.addPolicy(realmPolicy);
        } catch (Exception e) {
            debug.error(classMethod +
                    "Error creating datastores readonly policy", e);
        }
    }

    /**
     * Returns the policy <code>Rule</code> object.
     * 
     * @param serviceName name of the service.
     * @param resourceName name of the resource
     * @return <code>Rule</code> object.
     */
    private static Rule getRule(String serviceName, String resourceName) {
        String classMethod = "UpgradeUtils:getRule : ";
        Rule rule = null;
        try {
            Map actionsMap = new HashMap();
            Set values = new HashSet();
            values.add("allow");
            actionsMap.put("MODIFY", values);
            actionsMap.put("DELEGATE", values);
            actionsMap.put("READ", values);
            rule = new Rule(serviceName, resourceName, actionsMap);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating rule ", e);
        }
        return rule;
    }

    /**
     * Returns the policy <code>Rule</code> object.
     * 
     * @param serviceName name of the service.
     * @param resourceName name of the resource
     * @param actionsMap map of allowed actions on the resource.
     *        the key is the actions (MODIFY,DELEGATE,READ) 
     *        and the values is a set indicating whether 
     *        action is allowed or denied.
     * @return <code>Rule</code> object.
     */
    private static Rule getRule(String ruleName,String serviceName,
        String resourceName, Map actionsMap) {
        String classMethod = "UpgradeUtils:getRule : ";
        Rule rule = null;
        try {
            rule = new Rule(ruleName,serviceName, resourceName, actionsMap);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating rule ", e);
        }
        return rule;
    }

    /**
     * Returns the policy <code>Subject</code>
     * 
     */
    private static Subject getSubject(PolicyManager policyManager,
            String universalID) {
        String classMethod = "UpgradeUtils:getSubject : ";
        Subject subject = null;
        try {
            SubjectTypeManager stm = policyManager.getSubjectTypeManager();
            subject = stm.getSubject(AM_ID_SUBJECT);
            Set subjectValues = new HashSet(1);
            subjectValues.add(universalID);
            subject.setValues(subjectValues);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating subject", e);
        }
        return subject;
    }

    /**
     * Returns the universal identifier of an identity
     */
    private static String getUniversalID(String orgDN, String idName) {
        return new StringBuffer().append("id=").append(idName)
                .append(",ou=role,").append(orgDN).append(",amsdkdn=cn=")
                .append(idName).append(",").append(orgDN).toString();
    }

    /*
     * Return sub configurations in a service.
     * 
     * @param serviceName the service name.
     * @param serviceVersion the version of the service
     * @param realm the realm to retreive the sub configs from.
     * @return a set containing the org sub configurations.
     */
    static Set getOrgSubConfigs(String serviceName,
            String serviceVersion,String realm) {
        String classMethod = "UpgradeUtils:getOrgSubConfigs : ";
        Set subConfigs;
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                    ssoToken, serviceName, serviceVersion);
            ServiceConfig orgConfig =
                    scm.getOrganizationConfig(realm, null);
            subConfigs = orgConfig.getSubConfigNames();
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Org subConfigs : " + subConfigs);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "No organization subconfigs" , e);
            }
            subConfigs = Collections.EMPTY_SET;
        }
        return subConfigs;
    }

    /**
     * Returns a map of attributes in a subconfig. 
     * The key is subconfig name and the values a map of of attribute name
     * and values.
     */
    public static Map getSubConfigAttributes(
            String serviceName, String serviceVersion) {
        String classMethod = "UpgradeUtils:getSubConfigAttributes : ";
        Map attributesMap = new HashMap();
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                    ssoToken, serviceName, serviceVersion);
            ServiceConfig orgConfig =
                    scm.getOrganizationConfig(rootSuffix, null);
            Set subConfigs = orgConfig.getSubConfigNames();
            Iterator i = subConfigs.iterator();
            while (i.hasNext()) {
                try {
                    String subConfigName = (String) i.next();
                    ServiceConfig svcConfig =
                            orgConfig.getSubConfig(subConfigName);
                    Map attributes = svcConfig.getAttributes();
                    if (debug.messageEnabled()) {
                        debug.message("subConfigName is :" + subConfigName);
                        debug.message("attribtues are  " + attributes);
                    }
                    attributesMap.put(subConfigName, attributes);
                } catch (Exception e) {
                    debug.error(classMethod + "Error get attributes", e);
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error get attributes", e);
        }
        return attributesMap;
    }

    /**
     * Creates organization sub configuration.
     * @param serviceName name of the service
     * @param serviceVersion name of the service version
     * @param subConfigID the subconfig name
     * @param configName the config name
     * @param attributes Map of attribute , where key is attribute name and
     * a <code>Set</code> of values.
     */
    public static void createOrgSubConfig(String serviceName,
            String serviceVersion, String subConfigID,
            String configName, Map attributes) {
        String classMethod = "UpgradeUtils:createOrgSubConfig : ";
        try {
            ServiceConfigManager scm = new ServiceConfigManager(ssoToken,
                    serviceName, serviceVersion);
            ServiceConfig orgConfig =
                    scm.getOrganizationConfig(rootSuffix, null);
            orgConfig.addSubConfig(configName, subConfigID, 0, attributes);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating subconfig", e);
        }
    }

    /** 
     * Returns subconfigs in a service sub configuration.
     * 
     * @param serviceName name of the service
     * @param serviceVersion the service revision
     * @param subConfigName the name of the subconfig
     * 
     * @return a set containing sub configs 
     */
    public static Set getProviderIds(String serviceName, String serviceVersion,
            String subConfigName) {
        String classMethod = "UpgradeUtils:getProviderIds : ";
        Set providerids = new HashSet();
        try {
            ServiceConfigManager scm = new ServiceConfigManager(ssoToken,
                    serviceName, serviceVersion);
            ServiceConfig orgConfig =
                    scm.getOrganizationConfig(rootSuffix, null);
            ServiceConfig providerSC =
                    orgConfig.getSubConfig(subConfigName);
            providerids = providerSC.getSubConfigNames();
            System.out.println(bundle.getString("upg-provider-id") + ":" 
                + providerids);
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error retrieving sub configs", e);
        }
        return providerids;
    }

    /**
     * Returns the organization service config for a service
     * @param serviceName the service name
     * @param serviceVersion the service version 
     * 
     * @return <code>ServiceConfig</code> object
     */
    public static ServiceConfig getOrganizationServiceConfig(String serviceName,
            String serviceVersion) {
        ServiceConfig orgConfig = null;
        String classMethod = "UpgradeUtils:getOrganizationServiceConfig :";
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                    ssoToken, serviceName, serviceVersion);
            orgConfig =
                    scm.getOrganizationConfig(rootSuffix, null);
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error getting org config for " + serviceName +
                    "," + serviceVersion);
        }
        return orgConfig;
    }

    /**
     * Returns <code>IDFFMetaManager</code> object
     * 
     * @return  <code>IDFFMetaManager</code> object.
     */
    public static IDFFMetaManager getIDFFMetaManager() throws
            IDFFMetaException {
        IDFFMetaManager idffMetaMgr = new IDFFMetaManager(ssoToken);
        return idffMetaMgr;
    }

    /**
     * Replace tags in the upgrade services xmls
     */
    static void replaceTags(File dir, Properties p) {
        try {
            LinkedList fileList = new LinkedList();
            getFiles(dir, fileList);
            ListIterator srcIter = fileList.listIterator();
            while (srcIter.hasNext()) {
                File file = (File) srcIter.next();
                String fname = file.getAbsolutePath();
                if (fname.endsWith("xml") || fname.endsWith("ldif")) {
                    replaceTag(fname, p);
                }
            }
        } catch (Exception e) {
        // do nothing
        }
    }
    // replace tags
    static void replaceTag(String fname, Properties p) {
        String line;
        StringBuffer sb = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(fname);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                Enumeration e = p.propertyNames();
                while (e.hasMoreElements()) {
                    String oldPattern = (String) e.nextElement();
                    String newPattern = (String) p.getProperty(oldPattern);
                    String oldAtPattern = "@" + oldPattern + "@" ;
                    if (line != null && line.contains(oldAtPattern)) {
                         line = line.replaceAll(oldAtPattern, newPattern);
                    } else {
                         line = line.replaceAll(oldPattern, newPattern);
                    }
                }
                sb.append(line + "\n");
            }
            reader.close();
            BufferedWriter out = new BufferedWriter(new FileWriter(fname));
            out.write(sb.toString());
            out.close();
        } catch (Exception e) {
        // do nothing
        }
    }

    protected static void setProperties(Properties p) {
        configTags = p;
    }

    /**
     * Returns a list of files in a directory.
     *
     * @param dirName the directory name
     * @param fileList the file list to be retrieved.
     */
    public static void getFiles(File dirName,
            LinkedList fileList) {
        File[] fromFiles = dirName.listFiles();
        for (int i = 0; i < fromFiles.length; i++) {
            fileList.addLast(fromFiles[i]);
            if (fromFiles[i].isDirectory()) {
                getFiles(fromFiles[i], fileList);
            }
        }
    }

    /**
     * Returns the root suffix dn.
     * 
     * @return the root suffix string.
     */
    protected static String getRootSuffix() {
        return rootSuffix;
    }

    /**
     * Returns the Normalized Root Suffix DN.
     * 
     * @return the normalized root suffix.
     */
    public static String getNormalizedRootSuffix() {
        return (new DN(rootSuffix)).toRFCString().toLowerCase();
    }

    /**
     * Returns the People Container Root Suffix DN.
     * 
     * @return the People Containger root suffix.
     */
    public static String getPeopleOrgRootSuffix() {
        String rootOrg = rootSuffix.replaceAll(",", "_");
        return "People" + "_" + rootOrg;
    }

    /**
     * Returns the realm/organization name from the root suffix.
     * 
     * @return the name of the realm/organization.
     */
    public static String getRsDN() {
        String org = getNormalizedRootSuffix();
        String rsDN = org;
        int i = org.indexOf("=");
        if (i != -1) {
            int j = org.indexOf(",");
            if (j != -1) {
                rsDN = org.substring(i+1,j);
            } else {
                rsDN = org.substring(i+1);
            }
        }
        return rsDN;
    }

    /**
     * Creates the default server configuration .
     * The values are read from the AMConfig.properties and for each server
     * instance a subconfig is created under 
     * <code>com-sun-identity-servers</code>
     * 
     * @param serviceName the service name 
     * @param subConfigName the sub configuration name.
     * @param instanceName the instance name
     * @param instanceID the instance identifier
     * @param values a Set of values to be set.
     * @param serverConfigXML string representation of 
     *     <code>serverconfig.xml</code>
     * @throws UpgradeException if there is an error.
     */
    public static void addServerDefaults(String serviceName,
            String subConfigName, String instanceName, String instanceID,
            Set values,String serverConfigXML) throws UpgradeException {
        String classMethod = "UpgradeUtils:addServerDefaults : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "serviceName :" + serviceName);
            debug.message(classMethod + "subConfigName :" + subConfigName);
            debug.message(classMethod + "instanceName:" + instanceName);
            debug.message(classMethod + "instanceID:" + instanceID);
            debug.message(classMethod + "values:" + values);
        }

        try {
            ServiceConfigManager scm =
                    new ServiceConfigManager(serviceName, getSSOToken());
            ServiceConfig globalSvcConfig = scm.getGlobalConfig(null);
            ServiceConfig gConfig = globalSvcConfig.getSubConfig(subConfigName);

            Map serverValues = new HashMap(4);
            Set setServerId = new HashSet(2);
            setServerId.add(instanceID);
            serverValues.put(ATTR_SERVER_ID, setServerId);

            Set setServerConfigXML = new HashSet(2);
            String file = configDir + File.separator + SERVER_CONFIG_XML;
            if (serverConfigXML == null) {
                serverConfigXML = readFile(file);
            }
            setServerConfigXML.add(serverConfigXML);
            serverValues.put(ATTR_SERVER_CONFIG, values);
            serverValues.put(ATTR_SERVER_CONFIG_XML, setServerConfigXML);
            if (instanceName == null) {
                gConfig.addSubConfig(CONFIG_SERVER_DEFAULT,
                        SUB_SCHEMA_SERVER, 0, serverValues);
            } else {
                gConfig.addSubConfig(instanceName,
                    SUB_SCHEMA_SERVER, 0, serverValues);
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error adding server instance :", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Reads a file into a string.
     */
    private static String readFile(String fileName) {
        String classMethod = "UpgradeUtils:readFile : ";
        StringBuffer fileData = new StringBuffer();
        String fileString = "";
        try {
             BufferedReader reader = new BufferedReader(
                 new FileReader(fileName));
             char[] buf = new char[1024];
             int numRead=0;
             while((numRead=reader.read(buf)) != -1){
                 String readData = String.valueOf(buf, 0, numRead);
                 fileData.append(readData);
                 buf = new char[1024];
             }
             reader.close();
             fileString = fileData.toString();
        } catch (Exception e) {
             debug.error(classMethod + "Error reading file : " + fileName);
        }
        return fileString;
    }

    /**
     * Returns the properties from existing <code>AMConfig.properties</code>.
     * 
     * @return the properties from existing <code>AMConfig.properties</code>.
     */
    public static Properties getServerProperties() {
        String fileName =
                basedir +
                File.separator + DIR_UPGRADE + File.separator +
                DIR_CONFIG + File.separator + BACKUP_AMCONFIG;

        Properties properties = (Properties) propertyFileMap.get(fileName);
        if (properties == null) {
            properties = getProperties(fileName);
        }
        return properties;
    }

    /**
     * Writes the properties from existing <code>AMConfig.properties</code>.
     * 
     * @Writes the properties from existing <code>AMConfig.properties</code>.
     */
    public static void storeProperties(Properties props) {
        String classMethod = "UpgradeUtils:storeProperties : ";
        String fileName =
                basedir +
                File.separator + DIR_UPGRADE + File.separator +
                DIR_CONFIG + File.separator + BACKUP_AMCONFIG;
        // Write properties file.
        try {
            props.store(new FileOutputStream(fileName), null);
            propertyFileMap.put(fileName, props);
        } catch (IOException e) {
            debug.error(classMethod +
            "Error writing to AMConfig.properties.bak file " + fileName);
        }
    }

    /**
     * Returns the <code>serverconfig.xml</code> file contents as a string.
     * 
     * @return a string representing the <code>serverconfig.xml<code> file.
     */
    public static String getServerConfigXML() {
        String fileName =
                basedir +
                File.separator + DIR_UPGRADE + File.separator +
                DIR_CONFIG + File.separator + BACKUP_SERVER_CONFIG_XML;
        return readFile(fileName);
    }

    /** 
     * Returns the server name.
     * The server name is constructed by appending the protocol , host name
     * and port.
     * 
     * @return the server name.
     */
    public static String getServerName() {
        if (serverNameURL == null) {
            Properties amconfigProp = getServerProperties();
            String serverProto = amconfigProp.getProperty(SERVER_PROTO);
            String serverHost = amconfigProp.getProperty(SERVER_HOST);
            String serverPort = amconfigProp.getProperty(SERVER_PORT);
            serverNameURL = serverProto + "://" + serverHost + ":" + serverPort;
        }
        return serverNameURL;
    }

    /**
     * Returns the value of the server host.
     * The server host is retrieved from the <code>AMConfig.properties</code>
     * 
     * @return the server host value .
     */
    public static String getServerHost() {
        Properties amconfigProp = getServerProperties();
        return amconfigProp.getProperty(SERVER_HOST);
    }
    
    /**
     * Creates a file.
     * This method is used to create the bootstrap file 
     * 
     * @param fileName mame of the file to be created.
     * @param content value to be written to the file.
     */
    public static void writeToFile(String fileName, String content) {
        String classMethod = "UpgradeUtils:writeToFile : ";
        FileWriter fout = null;
        try {
            fout = new FileWriter(fileName);
            fout.write(content);
        } catch (IOException e) {
            debug.error(classMethod +
                    "Error writing to bootstrap file " + fileName);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                //No handling required
                }
            }
        }
    }

    /**
     * Adds attribute a sub schema.
     * 
     * @param serviceName name of the service
     * @param parentSchemaName the parent schema name.
     * @param subSchemaName the subschema name
     * @param schemaType the schema type
     * @param attributeSchemaFile the name of the file containing attributes 
     *     to be added.
     * @throws UpgradeException if there is an error adding the attributes.
     */
    public static void addAttributeToSubSchema(
            String serviceName,
            String parentSchemaName,
            String subSchemaName,
            String schemaType,
            String attributeSchemaFile) throws UpgradeException {
        String classMethod = "UpgradeUtils:addAttributeToSubSchema : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Adding attribute schema : " 
                    + attributeSchemaFile);
            debug.message(" to subSchema " + subSchemaName +
                    " to service " + serviceName);
        }
        FileInputStream fis = null;
        ServiceSchema ss =
                getServiceSchema(serviceName, parentSchemaName, schemaType);

        try {
            ServiceSchema subSchema = ss.getSubSchema(subSchemaName);
            fis = new FileInputStream(attributeSchemaFile);
            subSchema.addAttributeSchema(fis);
        } catch (IOException ioe) {
            debug.error(classMethod + "File not found " + attributeSchemaFile);
            throw new UpgradeException(ioe.getMessage());
        } catch (SMSException sme) {
            debug.error(classMethod + "Cannot add attribute schema to : " +
                    serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken : ", ssoe);
            throw new UpgradeException(ssoe.getMessage());
        } catch (Exception e) {
            debug.error(classMethod + "Error setting attribute schema : ", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Removes attribute default values from a service sub-configuration.
     * 
     * @param serviceName name of the service
     * @param sunServiceID set of service identifiers.
     * @param attributeName name of the attribute.
     * @param defaultValues set of default values to be removed.
     * @throws UpgradeException if there is an error.
     */
    public static void removeAttrDefaultValueSubConfig(
            String serviceName,
            Set sunServiceID,
            String attributeName,
            Set defaultValues) throws UpgradeException {

        String classMethod = "UpgradeUtils:removeAttrDefaultValueSubConfig: ";
        try {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realmSet = new HashSet();
            realmSet.add(rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            realms.addAll(realmSet);
            if (debug.messageEnabled()) {
                debug.message("realms is :" + realms);
            }
            Iterator oi = realms.iterator();
            while (oi.hasNext()) {
                String realm = (String) oi.next();
                Set subConfigs = getOrgSubConfigs(serviceName, "1.0", realm);
                Iterator i = subConfigs.iterator();
                while (i.hasNext()) {
                    String subConfigName = (String) i.next();
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "subconfig name is :" + subConfigName);
                        debug.message(classMethod + 
                                "Attribute Name : " + attributeName);
                    }
                    removeSubConfigAttributeDefaultValues(serviceName,
                            sunServiceID,
                            realm, subConfigName, attributeName, defaultValues);
                }
            }
        } catch (SMSException sme) {
            debug.error(classMethod + 
                    "Cannot add attribute schema to : " + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        } catch (Exception e) {
            debug.error(classMethod + "Error setting attribute schema : ", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Returns the value of <code>sunserviceid</code> attribute of a service
     * sub-configuration. 
     * 
     * @param subConfig name of the service sub-configuration
     * @return string value of <code>sunserviceid</code> attribute.
     */
    static String getSunServiceID(ServiceConfig subConfig) {
        String classMethod = "UpgradeUtils:getSunServiceID : ";
        String serviceID = "";
        try {
            String dn = subConfig.getDN();
            ld = getLDAPConnection();
            LDAPEntry ld1 = ld.read(dn);
            LDAPAttributeSet attrSet = ld1.getAttributeSet();
            if (attrSet != null) {
                for (Enumeration enums = attrSet.getAttributes();
                        enums.hasMoreElements();) {
                    LDAPAttribute attr = (LDAPAttribute) enums.nextElement();
                    String attrName = attr.getName();
                    if ((attr != null) &&
                            attrName.equalsIgnoreCase(ATTR_SUNSERVICE_ID)) {
                        String[] value = attr.getStringValueArray();
                        serviceID = value[0];
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + " sunserviceID is :" + serviceID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serviceID;
    }
    
    /**
     * Removes attributes default values from service subconfiguration.
     * 
     * @param serviceName name of the service
     * @param sunServiceID set of service identifiers
     * @param realm the realm name
     * @param subConfigName the service sub-configuration name.
     * @param attributeName name of the attribute 
     * @param defaultValues a set of values to be removed 
     */
    public static void removeSubConfigAttributeDefaultValues(
            String serviceName,
            Set sunServiceID,
            String realm,
            String subConfigName,
            String attributeName,
            Set defaultValues) {
        String classMethod =
                "UpgradeUtils:removeSubConfigAttributeDefaultValues : ";
        try {
            ServiceConfigManager scm = getServiceConfigManager(serviceName);
            ServiceConfig sc = scm.getOrganizationConfig(realm, null);
            ServiceConfig subConfig = sc.getSubConfig(subConfigName);
            String serviceID = getSunServiceID(subConfig);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "sunServiceID :" + sunServiceID);
                debug.message(classMethod + "serviceID :" + serviceID);
                debug.message(classMethod + "subConfigName :" + subConfigName);
                debug.message(classMethod + "Attribute Name :" + attributeName);
                debug.message(classMethod + "Default Values :" + defaultValues);
            }
            if (sunServiceID.contains(serviceID)) {
                Set valSet = getExistingValues(subConfig,
                        attributeName, defaultValues);
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "Values to be removed" + valSet);
                }
                subConfig.removeAttributeValues(attributeName, valSet);
            }
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken  : ", ssoe);
        } catch (SMSException sme) {
            debug.error(classMethod + "Error remove default values : ", sme);
        }
    }

    /**
     * Adds default values to an attribute in a service sub-configuration.
     * This methods iterates through all realms to update the attribute default
     * values. 
     * 
     * @param serviceName name of the service
     * @param sunServiceID set of service identifiers for the sub-configuration.
     * @param attributeName name of the attribute to be updated.
     * @param defaultValues set of default values to be added .
     */
    public static void addAttrDefaultValueSubConfig(
            String serviceName,
            Set sunServiceID,
            String attributeName,
            Set defaultValues) {

        String classMethod = "UpgradeUtils:addAttrDefaultValueSubConfig : ";
        try {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realmSet = new HashSet();
            realmSet.add(rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            realmSet.addAll(realms);
            Iterator oi = realmSet.iterator();
            while (oi.hasNext()) {
                String realm = (String) oi.next();
                Set subConfigs = getOrgSubConfigs(serviceName, "1.0", realm);
                Iterator i = subConfigs.iterator();
                while (i.hasNext()) {
                    String subConfigName = (String) i.next();
                    if (debug.messageEnabled()) {
                        debug.message(classMethod
                                + "addSubConfig : " + subConfigName);
                    }
                    addSubConfigAttributeDefaultValues(serviceName,
                            sunServiceID,
                            realm, subConfigName, attributeName, defaultValues);
                }
            }
        } catch (SMSException sme) {
            debug.error(classMethod 
                    + "Cannot add attribute schema to : " + serviceName, sme);
        } catch (Exception e) {
            debug.error(classMethod + "Error setting attribute schema : ", e);
        }
    }

    /**
     * Adds defaults values to service sub-configuration
     * 
     * @param serviceName the service name
     * @param sunServiceID set of sunservice identifiers for sub-configuration
     * @param realm the realm name
     * @param subConfigName the sub-configuration name
     * @param attributeName the attribute name
     * @param defaultValues set of default values to be updated.
     */
    public static void addSubConfigAttributeDefaultValues(
            String serviceName,
            Set sunServiceID,
            String realm,
            String subConfigName,
            String attributeName,
            Set defaultValues) {
        String classMethod =
                "UpgradeUtils:addSubConfigAttributeDefaultValues : ";
        try {
            Set oldVal = new HashSet();
            ServiceConfigManager scm = getServiceConfigManager(serviceName);
            ServiceConfig sc = scm.getOrganizationConfig(realm, null);
            ServiceConfig subConfig = sc.getSubConfig(subConfigName);
            String serviceID = getSunServiceID(subConfig);
            if (sunServiceID.contains(serviceID)) {
                Set valSet = getExistingValues(
                        subConfig, attributeName, defaultValues);
                defaultValues.removeAll(valSet);
                subConfig.replaceAttributeValues(
                        attributeName, oldVal, defaultValues);
            }
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSOToken", ssoe);
        } catch (SMSException sme) {
            debug.error(classMethod + "Error adding values ", sme);
        }
    }

    /**
     * Removes attributes from a service sub-configuration.
     * Iterates through all realms and modifies all sub configuration instances.
     * 
     * @param serviceName the service name.
     * @param sunServiceID set of service identifiers for the sub-configuration.
     * @param attributeList list of attributes to be removed.
     * @throws UpgradeException if there is an error.
     */
    public static void removeAttrFromSubConfig(
            String serviceName,
            Set sunServiceID,
            List attributeList)
            throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttrFromSubConfig : ";
        try {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realmSet = new HashSet();
            realmSet.add(rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            realmSet.addAll(realms);
            Iterator oi = realmSet.iterator();
            while (oi.hasNext()) {
                String realm = (String) oi.next();
                Set subConfigs = getOrgSubConfigs(serviceName, "1.0", realm);
                Iterator i = subConfigs.iterator();
                while (i.hasNext()) {
                    String subConfigName = (String) i.next();
                    removeSubConfigAttribute(serviceName, sunServiceID,
                            realm, subConfigName, attributeList);
                }
            }
        } catch (SMSException sme) {
            debug.error(classMethod +
                    "Cannot add attribute schema to : " + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        }
    }

    /**
     * Removes attribute from service sub-configuration instances.
     *
     * @param serviceName the service name
     * @param sunServiceID set of service identifiers
     * @param realm the realm name
     * @param subConfigName the subconfig name
     * @param attrList a list of attributes
     */
    public static void removeSubConfigAttribute(
            String serviceName,
            Set sunServiceID,
            String realm,
            String subConfigName,
            List attrList) {
        String classMethod = "UpgradeUtils:removeSubConfigAttribute : ";
        try {
            ServiceConfigManager scm = getServiceConfigManager(serviceName);
            ServiceConfig sc = scm.getOrganizationConfig(realm, null);
            ServiceConfig subConfig = sc.getSubConfig(subConfigName);
            String serviceID = getSunServiceID(subConfig);
            if (sunServiceID.contains(serviceID)) {
                Iterator i = attrList.iterator();
                while (i.hasNext()) {
                    String attributeName = (String) i.next();
                    if (debug.messageEnabled()) {
                        debug.message(classMethod 
                                + "Removing attr :" + attributeName);
                    }
                    subConfig.removeAttribute(attributeName);
                }
            }
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSO Token ", ssoe);
        } catch (SMSException sme) {
            debug.error(classMethod + "Error removing attributes : " +
                    attrList, sme);
        }
    }

    /**
     * Removes attribute default values from service schema.
     * 
     * @param serviceName the service name
     * @param schemaType the schema type
     * @param attrName name of the attribute
     * @param defaultValues a set of default values to be remove
     * @throws UpgradeException if there is an error
     */
    public static void removeAttributeDefaultValues(String serviceName,
            String schemaType, String attrName,
            Set defaultValues) throws UpgradeException {
        removeAttributeDefaultValues(serviceName, schemaType,
                attrName, defaultValues, false);
    }

    /**
     * Removes attribute default values from service schema.
     * 
     * @param serviceName the service name
     * @param schemaType the schema type
     * @param attrName name of the attribute
     * @param defaultValues a set of default values to be remove
     * @param isOrgAttrSchema boolean value true if the schema is of the type
     *     <code>OrganizationAttributeSchema</code>
     * @throws UpgradeException if there is an error
     */
    public static void removeAttributeDefaultValues(String serviceName,
            String schemaType, String attrName,
            Set defaultValues, boolean isOrgAttrSchema)
            throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeDefaultValues : ";
        ServiceSchema ss = null;
        if (debug.messageEnabled()) {
            debug.message(classMethod + "serviceName : " + serviceName);
            debug.message(classMethod + "schemaTpe :" + schemaType);
            debug.message(classMethod + "attrName : " + attrName);
            debug.message(classMethod + "defaltValues :" + defaultValues);
            debug.message(classMethod + "isOrgAttrSchema :" + isOrgAttrSchema);
        }
        try {
            if (isOrgAttrSchema) {
                ServiceSchemaManager sm =
                        getServiceSchemaManager(serviceName);
                ss = sm.getOrganizationCreationSchema();
            } else {
                ss = getServiceSchema(serviceName, null, schemaType);
            }

            if (ss != null) {
                AttributeSchema attrSchema =
                        ss.getAttributeSchema(attrName);
                for (Iterator i = defaultValues.iterator(); i.hasNext();) {
                    String defaultValue = (String) i.next();
                    attrSchema.removeDefaultValue(defaultValue);
                }
            }
        } catch (SMSException sme) {
            debug.error(classMethod + "Error removing default values ", sme);
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSO Token", ssoe);
        }
    }


    /**
     * Replaces attribute default values in a service sub-configuration.
     * Iterates through all realms to update all sub-configuration instances.
     * 
     * @param serviceName the service name
     * @param sunServiceID set of service identifiers
     * @param attributeName the attribute name.
     * @param oldValues set of values to be replaced.
     * @param newValues set of values to add.
     * @throws UpgradeException if there is an error
     */
    public static void replaceAttrDefaultValueSubConfig(
            String serviceName,
            Set sunServiceID,
            String attributeName,
            Set oldValues, 
            Set newValues) 
            throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttrDefaultValueSubConfig : ";
        try {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(ssoToken, rootSuffix);
            Set realmSet = new HashSet();
            realmSet.add(rootSuffix);
            Set realms = ocm.getSubOrganizationNames("*", true);
            realmSet.addAll(realms);
            Iterator oi = realmSet.iterator();
            while (oi.hasNext()) {
                String realm = (String) oi.next();
                Set subConfigs = getOrgSubConfigs(serviceName, "1.0", realm);
                Iterator i = subConfigs.iterator();
                while (i.hasNext()) {
                    String subConfigName = (String) i.next();
                    replaceSubConfigAttributeDefaultValues(
                            serviceName, sunServiceID,
                            realm, subConfigName, attributeName, 
                            oldValues, newValues);
                }
            }
        } catch (SMSException sme) {
            debug.error(classMethod 
                    + "Cannot add attribute schema to : " + serviceName, sme);
            throw new UpgradeException(sme.getMessage());
        } catch (Exception e) {
            debug.error(classMethod + "Error setting attribute schema : ", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Replaces attributes default values in service sub-configuration.
     *
     * @param serviceName name of the service
     * @param sunServiceID a set of subconfig service identifiers.
     * @param realm the realm name.
     * @param subConfigName the name of the service sub-configuration.
     * @param attributeName name of the attribute
     * @param oldValues set of values to be replaced.
     * @param newValues set of values to be added.
     */
    public static void replaceSubConfigAttributeDefaultValues(
            String serviceName,
            Set sunServiceID,
            String realm,
            String subConfigName,
            String attributeName,
            Set oldValues, Set newValues) {
        String classMethod =
                "UpgradeUtils:replaceSubConfigAttributeDefaultValues : ";
        try {
            ServiceConfigManager scm = getServiceConfigManager(serviceName);
            ServiceConfig sc = scm.getOrganizationConfig(realm, null);
            ServiceConfig subConfig = sc.getSubConfig(subConfigName);
            String serviceID = getSunServiceID(subConfig);
            if (debug.messageEnabled()) {
                debug.message("sunServiceID :" + sunServiceID);
                debug.message("serviceID :" + serviceID);
                debug.message("subConfigName :" + subConfigName);
            }
            if (sunServiceID.contains(serviceID)) {
                subConfig.replaceAttributeValues(attributeName,
                        oldValues, newValues);
            }
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Invalid SSO Token: ", ssoe);
        } catch (SMSException sme) {
            debug.error(classMethod 
                    + "Error replacing default values for attribute : "
                    + attributeName, sme);
        }
    }

    /**
     * Returns a set of valid attributes values for an attribute.
     * 
     * @param subConfig the <code>ServiceConfig</code> object.
     * @param attrName the attribute name.
     * @param defaultValues set of attribute values to validate with the
     *    the existing attribute values.
     */
    static Set getExistingValues(ServiceConfig subConfig,
            String attrName, Set defaultVal) {
        Set valSet = new HashSet();
        String classMethod = "UpgradeUtils:getExistingValues : ";
        String serviceID = "";
        try {
            String dn = subConfig.getDN();
            ld = getLDAPConnection();
            LDAPEntry ld1 = ld.read(dn);
            LDAPAttributeSet attrSet = ld1.getAttributeSet();
            if (attrSet != null) {
                for (Enumeration enums = attrSet.getAttributes(); 
                enums.hasMoreElements();) {
                    LDAPAttribute attr = (LDAPAttribute) enums.nextElement();
                    String attName = attr.getName();
                    if ((attName != null) &&
                            attName.equalsIgnoreCase(ATTR_SUN_KEY_VALUE)) {
                        String[] value = attr.getStringValueArray();
                        for (int i = 0; i < value.length; i++) {
                            int index = value[i].indexOf("=");
                            if (index != -1) {
                                String key = value[i].substring(0, index);
                                if (key.equalsIgnoreCase(attrName)) {
                                    String v = value[i].substring(
                                            index + 1, value[i].length());
                                    if (defaultVal.contains(v)) {
                                        valSet.add(v);
                                    }
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error retreving attribute values ", e);
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Default Values are :" + valSet);
        }
        return valSet;
    }

    /**
     * Remove all default values from an attribute.
     * 
     * @param serviceName name of the service
     * @param schemaType the schema type
     * @param attributeName name of the attribute
     * @param subSchema the sub schema name.
     * @throws UpgradeException if there is an error.
     */
    public static void removeAllAttributeDefaultValues(
            String serviceName,
            String schemaType,
            String attributeName,
            String subSchema) throws UpgradeException {
        String classMethod = "UpgradeUtils:removeAttributeDefaultValues : ";
        try {
            ServiceSchema ss =
                    getServiceSchema(serviceName, subSchema, schemaType);
            // check if service schema exists.
            if (ss != null) {
                AttributeSchema attrSchema =
                        ss.getAttributeSchema(attributeName);
                attrSchema.removeDefaultValues();
            }
        } catch (SMSException sme) {
            throw new UpgradeException(sme.getMessage());
        } catch (Exception e) {
            debug.error(classMethod + "Error removing default values", e);
            throw new UpgradeException(e.getMessage());
        }
    }

    /**
     * Returns a value of an attribute.
     * This method assumes that the attribute is single valued.
     * 
     * @param serviceName name of the service.
     * @param attributeName name of the attribute.
     * @param schemaType the schema type.
     * @param subSchemaName the sub schema name.
     * @return the value of the attribute 
     */
     public static String getSubSchemaAttributeValue(
            String serviceName,
            String schemaType,
            String attributeName,
            String subSchemaName) {
        String classMethod = "UpgradeUtils:getSubSchemaAttributeValue :";
        ServiceSchema ss = null;
        String value = null;
        try {
            ss = getServiceSchema(serviceName, subSchemaName, schemaType);
            AttributeSchema attrSchema = ss.getAttributeSchema(attributeName);
            Set defaultVal = attrSchema.getDefaultValues();
            value = (String) (defaultVal.iterator().next());
        } catch (Exception e) {
            debug.error(classMethod + "cannot retrieve attribute value for " +
                attributeName);
        }
        return value;
    }


    /**
     * Checks if the instance is FM.
     * 
     * @return true if the instance is FM.
     */
    public static boolean isFMInstance() {
        if (instanceType == null) {
            instanceType = (String) configTags.get("INSTANCE_TYPE");
        }
        return (instanceType != null && instanceType.equalsIgnoreCase("FM"));
   }
    
    /**
     * Loads new services schemas , ldifs and modifies/removes
     * certain services on a FM instance.
     * This method is invoked when the instance is
     * FM. 
     */
    public static void createServicesForFM() {
        String fileName = null;
        getSSOToken();
        String classMethod = "UpgradeUtils:createServicesForFM : ";
        try {
            if (debug.messageEnabled()) {
                debug.message(classMethod + " Deleting LDAP Entry for idrepo");
            }
            delete("ou=sunIdentityRepositoryService,ou=services," 
                    + rootSuffix, getLDAPConnection(), true);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Removing sunAMDelegation Service");
            }
            //load ldif file
            String ldifFileName = new StringBuffer(basedir)
                    .append(File.separator).append(DIR_UPGRADE)
                    .append(File.separator).append(DIR_CONFIG)
                    .append(File.separator)
                    .append(DAI_LDIF).toString();
            if (debug.messageEnabled()) {
                debug.message("ldif filename is :" + ldifFileName);
            }
            loadLdif(ldifFileName);
            ldifFileName = new StringBuffer().append(basedir)
                    .append(File.separator).append(DIR_UPGRADE)
                    .append(File.separator).append(DIR_CONFIG)
                    .append(File.separator)
                    .append(INSTALL_LDIF).toString();
            UpgradeUtils.loadLdif(ldifFileName);
            fileName = getNewServiceNamePath(UMS_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(REPO_XML);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "loading :" + fileName);
            }
            createService(fileName);
            Map m = new HashMap();
            createOrgSubConfig(IDREPO_SERVICE, "1.0", "amSDK", "amSDK1", m);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(UNIX_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(USER_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(POLICY_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(POLICY_CONFIG_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        try {
            fileName = getNewServiceNamePath(PASSWORD_RESET_XML);
            createService(fileName);
        } catch (Exception e) {
            debug.error("UpgradeUtils:createServicesFM: " + fileName, e);
        }
        removeService(DELEGATION_SERVICE);
        removeService(APPLICATION_SERVICE);
    }


    /**
     * Removes service schema from the config store.
     * 
     * @param serviceName name of the SMS service to be deleted.
     */
    public static void removeService(String serviceName,String version) {
        try {
            ServiceManager scm = getServiceManager();
            scm.removeService(serviceName,version);
        } catch (SSOException e) {
            debug.error("invalid sso token" , e);
        } catch (SMSException sme) {
            debug.error("invalid service name " , sme);
        } catch (Exception me) {
            debug.error("invalid service name " , me);
        }
     }
    /**
     * Removes service schema from the config store.
     * 
     * @param serviceName name of the SMS service to be deleted.
     */
    public static void removeService(String serviceName) {
        removeService(serviceName,"1.0");
     }

    /**
     * Validates if the Directory server host and port are valid.
     *
     * @param dsHost the directory server host name.
     * @param dsPort the directory server port name.
     * @return true if the host and port are valid else false.
     */
    public  static boolean isValidServer(String dsHost,String  dsPort) {
       boolean isValidServer = true;
       try {
            LDAPConnection ldapConn = new LDAPConnection();
            ldapConn.connect(dsHost,new Integer(dsPort).intValue());
            ldapConn.disconnect();
        } catch (LDAPException lde) {
            isValidServer =false;
        } catch (Exception e) {
            isValidServer =false;
        }
        if (!isValidServer) {
            System.out.println(bundle.getString("upg-error-ds-info") + "!! ");
        }
        return isValidServer;
    }

    /**
     * Validates the Directory Server Credentials.
     * 
     * @param dsHost the directory server host.
     * @param dsPort the directory server port.
     * @param bindDN the dn to bind with.
     * @param bindPass the password.
     * @return true if credentials are valid else false.
     */
    public static boolean isValidCredentials(String dsHost, String dsPort,
            String bindDN, String bindPass) {
        boolean isValidAuth = false;
        try {
            LDAPConnection ldapConn = new LDAPConnection();
            ldapConn.connect(dsHost, new Integer(dsPort).intValue());
            ldapConn.authenticate(bindDN, bindPass);
            ldapConn.disconnect();
            isValidAuth = true;
        } catch (Exception e) {
            // do nothing
        }
        if (!isValidAuth) {
            System.out.println(bundle.getString("upg-error-credentials")
                + " !! ");
        }
        return isValidAuth;
    }


    /**
     * Delete an entry, recursing if the entry has children
     *
     * @param dn DN of the entry to delete
     * @param ld active connection to server
     * @param doDelete true if the entries really
     * are to be deleted
     */
    public static void delete(String dn, LDAPConnection ld, boolean doDelete ) {
        String theDN = "";
        try {
            LDAPSearchConstraints cons = ld.getSearchConstraints();
            // Retrieve all results at once
            cons.setBatchSize( 0 );
             // Find all immediate child nodes; return no
             // attributes
            LDAPSearchResults res = ld.search( dn, ld.SCOPE_ONE, 
                "objectclass=*", new String[] {LDAPv3.NO_ATTRS}, false, cons );
            // Recurse on entries under this entry
            while ( res.hasMoreElements() ) {
                try {
                    // Next directory entry
                    LDAPEntry entry = res.next();
                    theDN = entry.getDN();
                    // Recurse down
                    delete( theDN, ld, doDelete );
                } catch ( LDAPException e ) {
                    continue;
                } catch ( Exception ea ) {
                    continue;
                }
            }
            // At this point, the DN represents a leaf node,
            // so stop recursing and delete the node
            try {
                if ( doDelete ) {
                    ld.delete( dn );
                    if (debug.messageEnabled()) {
                        debug.message(dn + " deleted");
                    }
                }
            } catch (LDAPException e) {
                if (debug.messageEnabled()) {
                    debug.message( e.toString() );
                }
            } catch( Exception e ) {
                if (debug.messageEnabled()) {
                    debug.message( e.toString() );
                }
            }
        } catch (Exception me) {
            // do nothing
        }
    }

    /**
     * Creates Delegation Policies.
     */
    public static void addDelegationRule(String policyName,
            Map ruleMap) {
        String classMethod = "UpgradeUtils:addDelegationRule: ";
        try {
            // get a list of realms - recursively.
            PolicyManager pm = new PolicyManager(ssoToken, HIDDEN_REALM);
            Policy policy = pm.getPolicy(policyName);
            Map actionsMap = new HashMap();
            Set values = new HashSet();
            values.add("allow");
            actionsMap.put("READ", values);

            Set rSet = ruleMap.keySet();
            Iterator i = rSet.iterator();
            while (i.hasNext()) {
                String ruleName = (String) i.next();
                String resourceNameSuffix = (String) ruleMap.get(ruleName);
                String resourceName =
                        new StringBuffer()
                        .append("sms://").append(rootSuffix)
                        .append(resourceNameSuffix).toString();
                Rule rule =
                        getRule(ruleName, DELEGATION_SERVICE,
                        resourceName, actionsMap);
                policy.addRule(rule);
            }
            pm.replacePolicy(policy);
        } catch (Exception e) {
            debug.error(classMethod +
                    "Error loading sub realm delegation polices", e);
        }
    }

    /**
     * Creates <code>OrganizationConfiguration</code> in a service.
     *
     * @param serviceName name of the service
     * @param orgName name of the organization
     * @param attrValues map of attribute names and their values. The 
     *        key is the attribute name a string and the values is a Set
     *        of values.
     */
    public static void createOrganizationConfiguration(String serviceName,
            String orgName,Map attrValues) {
        String classMethod = "UpgradeUtils:createOrganizationConfiguration: ";
        try {
            ServiceConfigManager sm = getServiceConfigManager(serviceName);
            sm.createOrganizationConfig(orgName,attrValues);
        } catch (Exception e) {
            debug.error(classMethod + "Error creating organization "
                + "configuration for " + serviceName , e);
        }
    }

    /**
     * Adds SubConfiguration to an existing subconfiguration in a service.
     * 
     * @param serviceName the service name
     * @param parentConfigName the name of parent sub configuration.
     * @param subConfigName the subconfig name 
     * @param subConfigID the subconfig id
     * @param attrValues a map of attribute value pairs to be added to the
     *        subconfig.
     * @param priority the priority value 
     * @throws UpgradeException if there is an error.
     */
    public static void addSubConfig(
            String serviceName,
            String parentConfigName,
            String subConfigName,
            String subConfigID,
            Map attrValues, int priority) throws UpgradeException {
        String classMethod = "UpgradeUtils:addSubConfig";
        try {
            ServiceConfigManager scm =
                    new ServiceConfigManager(serviceName, ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            ServiceConfig sc1 = sc.getSubConfig(parentConfigName);
            if (sc != null) {
                sc1.addSubConfig(subConfigName,subConfigID,priority,attrValues);
            } else {
                debug.error(classMethod + 
                        "Error adding sub cofiguration" + subConfigName);
                throw new UpgradeException("Error adding subconfig");
            }
        } catch (SSOException ssoe) {
            throw new UpgradeException(classMethod + "invalid sso token");
        } catch (SMSException sm) {
            debug.error(classMethod + "Error loading subconfig", sm);
            throw new UpgradeException(classMethod + "error adding subconfig");
        }
    }


     /**
      * Removes Condition Properties.
      *
      * @param policyName Name of Policy.
      * @param attributeName the name of the attribute whose default values 
      *        needs to be updated.
      * @param conditionNameMap Map of condition name to map of property name to
      *        set of attribute values to be removed.
      */
     public static void removeDelegationCondition(String policyName,
         String attributeName,Map conditionNameMap) {
         try {
             PolicyManager pm = new PolicyManager(ssoToken, HIDDEN_REALM);
             Policy policy = pm.getPolicy(policyName);

             for (Iterator i = conditionNameMap.keySet().iterator();i.hasNext();
             ) {
                 String condName = (String)i.next();
                 Condition cond = policy.getCondition(condName);
                 if (cond != null) {
                     Set removeSet = (HashSet)conditionNameMap.get(condName);
                     Map orig = cond.getProperties();

                     for (Iterator j = removeSet.iterator();
                         j.hasNext();
                     ) {
                         String defaultValue = (String)j.next();
                         Set origValues = (Set)orig.get(attributeName);
                         if (origValues != null) {
                             origValues.removeAll(removeSet);
                         }
                     }
                     cond.setProperties(orig);
                     policy.replaceCondition(condName, cond);
                 }
             }
             pm.replacePolicy(policy);
         } catch (PolicyException e) {
             debug.error("UpgradeUtils.removeDelegationCondition", e);
         } catch (SSOException e) {
             debug.error("UpgradeUtils.removeDelegationCondition", e);
         }
     }

     /**
      * Removes attribute from a condition.
      *
      * @param policyName Name of Policy.
      * @param attributeName the name of the attribute to be removed.
      * @param conditionName name of the condition
      */
    public static void removeDelegationPolicyAttribute(String policyName,
            String attributeName ,String conditionName) {
        String classMethod = "UpgradeUtils:removeDelegationPolicyAttribute";
        try {
            PolicyManager pm = new PolicyManager(ssoToken,HIDDEN_REALM);
            Policy policy = pm.getPolicy(policyName);

            Condition cond = policy.getCondition(conditionName);
            HashMap newMap=new HashMap();
            if (cond != null) {
                Map orig = cond.getProperties();
                Iterator i = (orig.keySet()).iterator();
                while (i.hasNext()) {
                    String key = (String)i.next();
                    if (!key.equals(attributeName)) {
                        HashSet values = (HashSet)orig.get(key);
                        newMap.put(key,values);
                    }
                 }

                if (debug.messageEnabled()) {
                    debug.message(classMethod + "attributes :" + newMap);
                }
                cond.setProperties(newMap);
                policy.replaceCondition(conditionName, cond);
             }
             pm.replacePolicy(policy);
         } catch (PolicyException e) {
             debug.error(classMethod,e);
         } catch (SSOException e) {
             debug.error(classMethod,e);
         }
    }
}
