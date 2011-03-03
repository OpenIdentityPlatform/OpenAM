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
 * $Id: AuthenticationCommon.java,v 1.15 2009/08/13 12:32:31 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.authentication;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.logging.Level;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

/**
 * This class contains helper method related to Authentication.
 */
public class AuthenticationCommon extends TestCommon 
        implements AuthConstants, SMSConstants {

    private static Map<String, String> globalAuthInstancesMap = new HashMap();
    private SSOToken ssoToken;
    private SMSCommon smsCommon;
    private IDMCommon idmCommon;
    private static final String AUTH_INSTANCE_SUBCONFIGID = "serverconfig";
    protected static final String AUTH_CONFIGURATION_SERVICE_NAME =
            "iPlanetAMAuthConfiguration";
    private static final String AUTH_CONFIGURATION_SUBCONFIGID = 
            "NamedConfiguration";
    private static final String AUTH_CONFIGURATION_GENERATED_PROPS =
            serverName + fileseparator + "built" + fileseparator + "classes" +
            fileseparator + "config" + fileseparator +
            "AuthenticationConfig-Generated.properties";
    private static final String DEFAULT_LDAP_SERVER =
            "UM_CONFIG_LDAPSERVER_COLON_PORT";
    private static final String DEFAULT_LDAP_BASEDN = "UM_CONFIG_SUFFIX";
    private static final String DEFAULT_LDAP_BINDDN = "UM_CONFIG_BIND_DN";
    private static final String DEFAULT_LDAP_BINDPW = "UM_CONFIG_BIND_PW";
    private String moduleName;
    private String[] instancePrefixes =
        {LDAP_INSTANCE_PREFIX, MEMBERSHIP_INSTANCE_PREFIX,
         DATASTORE_INSTANCE_PREFIX, AD_INSTANCE_PREFIX,
         ANONYMOUS_INSTANCE_PREFIX, NT_INSTANCE_PREFIX,
         JDBC_INSTANCE_PREFIX, RADIUS_INSTANCE_PREFIX, UNIX_INSTANCE_PREFIX};
    private String testLogoutURL;
    private WebClient webClient;

    public AuthenticationCommon() {
        this("authentication");
    }

    public AuthenticationCommon(String module) {
        super(module);
        moduleName = module;
        try {
            idmCommon =  new IDMCommon(moduleName);
            testLogoutURL = protocol + ":" + "//" + host + ":" + port +
                        uri + "/UI/Logout";
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    /**
     * Read the AuthenticationConfig.properties file into a <code>Map</code>
     */
    public void createAuthInstancesMap()
    throws Exception {
        entering("createAuthInstancesMap", null);
        Map globalDefaultMap = new HashMap();
        Map globalMap = new HashMap();
        Map globalDatastoreMap = new HashMap();
        Map moduleSpecificMap = new HashMap();
        String umPrefix = "UM_CONFIG1";
        String keyPrefix = "UMGlobalDatastoreConfig1";

        try {
            StringBuffer defAuthBuffer = new StringBuffer(getBaseDir()).
                    append(fileseparator).append(serverName).
                    append(fileseparator).append("built").append(fileseparator).
                    append("classes").append(fileseparator).append("config").
                    append(fileseparator).append("default").
                    append(fileseparator).
                    append("AuthenticationConfig.properties");
            globalDefaultMap =
                    getMapFromProperties(defAuthBuffer.toString());
            globalAuthInstancesMap.putAll(globalDefaultMap);
            log(Level.FINEST, "createAuthInstancesMap",
                    "globalDefaultMap = " + globalDefaultMap);

            StringBuffer globalAuthBuffer = new StringBuffer(getBaseDir()).
                    append(fileseparator).append(serverName).
                    append(fileseparator).append("built").append(fileseparator).
                    append("classes").append(fileseparator).append("config").
                    append(fileseparator).
                    append("AuthenticationConfig.properties");
            globalMap = getMapFromProperties(globalAuthBuffer.toString());
            log(Level.FINEST, "createAuthInstancesMap", "globalMap = " +
                    globalMap);
            if (globalMap.containsValue("UM_CONFIG_LDAPSERVER_COLON_PORT") ||
                    globalMap.containsValue("UM_CONFIG_SUFFIX") ||
                    globalMap.containsValue("UM_CONFIG_BIND_DN") ||
                    globalMap.containsValue("UM_CONFIG_BIND_PW") ||
                    globalMap.containsValue("")) {
                StringBuffer globalUMBuffer = new StringBuffer(getBaseDir()).
                        append(fileseparator).append(serverName).
                        append(fileseparator).append("built").
                        append(fileseparator).append("classes").
                        append(fileseparator).append("config").
                        append(fileseparator).
                        append("UMGlobalDatastoreConfig-Generated.properties");
                globalDatastoreMap =
                        getMapFromProperties(globalUMBuffer.toString());

                log(Level.FINE, "createAuthInstancesMap",
                        "Retrieving datastore " +
                        "directory server host, port, and root suffix.");
                if (moduleName.equals("samlv2")) {
                    keyPrefix = "UMGlobalDatastoreConfig0";
                    umPrefix = "UM_CONFIG0";
                }

                String globalLDAPServer =
                        ((String) globalMap.get("ldap." +
                        LDAP_AUTH_SERVER)).trim();
                if (globalLDAPServer.equals(DEFAULT_LDAP_SERVER) ||
                        globalLDAPServer.equals("")) {
                    String umDirectoryHost =
                            ((String) globalDatastoreMap.get(keyPrefix + "." +
                            UM_LDAPv3_LDAP_SERVER + ".0")).trim();
                    String umDirectoryPort =
                            ((String) globalDatastoreMap.get(keyPrefix + "." +
                            UM_LDAPv3_LDAP_PORT + ".0")).trim();
                    log(Level.FINEST, "createAuthInstancesMap",
                            "Replacing the value of ldap." + LDAP_AUTH_SERVER +
                            " with " + umDirectoryHost + ":" + umDirectoryPort);
                    globalMap.put("ldap." + LDAP_AUTH_SERVER,
                            umDirectoryHost + ":" + umDirectoryPort);
                }

                String globalLDAPBaseDN = ((String) globalMap.get("ldap." +
                        LDAP_AUTH_BASEDN)).trim();
                if (globalLDAPBaseDN.equals(DEFAULT_LDAP_BASEDN) ||
                        globalLDAPBaseDN.equals("")) {
                    String umDirectoryBaseDN =
                            ((String) globalDatastoreMap.get(keyPrefix + "." +
                            "datastore-root-suffix.0")).trim();
                    log(Level.FINEST, "createAuthInstancesMap",
                            "Replacing the value of ldap." + LDAP_AUTH_BASEDN +
                            " with " + umDirectoryBaseDN);
                    globalMap.put("ldap." + LDAP_AUTH_BASEDN,
                            umDirectoryBaseDN);
                }

                String globalLDAPBindDN =
                        ((String) globalMap.get("ldap." +
                        LDAP_AUTH_BINDDN)).trim();
                if (globalLDAPBindDN.equals(DEFAULT_LDAP_BINDDN) ||
                        globalLDAPBindDN.equals("")) {
                    String umDirectoryBindDN =
                            ((String) globalDatastoreMap.get(keyPrefix + "." +
                            UM_LDAPv3_AUTHID + ".0")).trim();
                    log(Level.FINEST, "createAuthInstancesMap",
                            "Replacing the value of ldap." + LDAP_AUTH_BINDDN +
                            " with " + umDirectoryBindDN);
                    globalMap.put("ldap." + LDAP_AUTH_BINDDN,
                            umDirectoryBindDN);
                }

                String globalLDAPBindPw =
                        ((String) globalMap.get("ldap." +
                        LDAP_AUTH_BIND_PASSWD)).trim();
                if (globalLDAPBindPw.equals(DEFAULT_LDAP_BINDPW) ||
                        globalLDAPBindPw.equals("")) {
                    String umDirectoryBindPw =
                            ((String) globalDatastoreMap.get(keyPrefix + "." +
                            UM_LDAPv3_AUTHPW + ".0")).trim();
                    log(Level.FINEST, "createAuthInstancesMap",
                            "Replacing the value of ldap." +
                            LDAP_AUTH_BIND_PASSWD +
                            " with " + umDirectoryBindPw);
                    globalMap.put("ldap." + LDAP_AUTH_BIND_PASSWD,
                            umDirectoryBindPw);
                }
            }
            globalAuthInstancesMap.putAll(globalMap);
            log(Level.FINEST, "createAuthInstancesMap",
                    "Updated globalAuthInstancesMap = " +
                    globalAuthInstancesMap);

            StringBuffer moduleFileName = new StringBuffer(getBaseDir()).
                    append(fileseparator).append(serverName).
                    append(fileseparator).append("built").append(fileseparator).
                    append("classes").append(fileseparator).append(moduleName).
                    append(fileseparator).
                    append("AuthenticationConfig.properties");
            log(Level.FINE, "createAuthInstancesMap",
                    "Checking if the file " +
                    moduleFileName.toString() + " exists ...");
            File moduleAuthFile = new File(moduleFileName.toString());
            if (moduleAuthFile.exists()) {
                log(Level.FINE, "createAuthInstancesMap",
                        "Creating a Map from " + moduleAuthFile);
                moduleSpecificMap =
                        getMapFromProperties(moduleFileName.toString());
                log(Level.FINEST, "createAuthInstancesMap",
                        "moduleSpecificMap = " + moduleSpecificMap);
                log(Level.FINE, "createAuthInstancesMap",
                        "Updating the globalAuthInstancesMap with the " +
                        "module specific values");
                globalAuthInstancesMap.putAll(moduleSpecificMap);
                log(Level.FINEST, "createAuthInstancesMap",
                        "Updated globalAuthInstancesMap = " +
                        globalAuthInstancesMap);
            }

            if (globalAuthInstancesMap.containsValue(umPrefix + "_SUFFIX0")
                        || globalAuthInstancesMap.containsValue(umPrefix + 
                        "_LDAPSERVER_NAME0:" + umPrefix + "_LDAPSERVER_PORT0")
                        || globalAuthInstancesMap.containsValue(umPrefix +
                        "_BIND_DN0") ||
                        globalAuthInstancesMap.containsValue(
                        umPrefix + "_BIND_PW0")) {
                log(Level.SEVERE, "createAuthInstancesMap",
                        globalAuthBuffer.toString() + " and/or " +
                        moduleFileName + " contains one or more of the " +
                        "following unswapped tags " + umPrefix + "_SUFFIX0, " +
                        umPrefix + "_LDAPSERVER_NAME0:" + umPrefix +
                        "_LDAPSERVER_PORT0, " + umPrefix +
                        "_BIND_DN0, and " + umPrefix + "_BIND_PW0!");
                log(Level.SEVERE, "createAuthInstancesMap",
                        "Please update the " + keyPrefix +
                        " UM directory server values in " + getBaseDir() +
                        "resources" + fileseparator + "config" + fileseparator +
                        "UMGlobalDatastoreConfig.properties");
                assert false;
            }

            createFileFromMap(globalAuthInstancesMap,
                    AUTH_CONFIGURATION_GENERATED_PROPS);

        } catch (Exception e) {
            log(Level.SEVERE, "createAuthInstancesMap", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Create the authentication module instances.
     */
    public void createAuthInstances()
    throws Exception {
        try {
            String instancesToCreate =
                    ((String)globalAuthInstancesMap.get("instances-to-create"));
            String[] instanceTokens = instancesToCreate.split("\\|");

            for (String instanceToken: instanceTokens) {
                String[] tokenFields = instanceToken.split(",");
                String instancePrefix = tokenFields[0];
                int numberOfInstances = new Integer(tokenFields[1]).intValue();

                for (int iIndex = 1; iIndex <= numberOfInstances; iIndex++) {
                    String indexString = Integer.toString(iIndex);
                    Map<String, String> instanceMap =
                            getModuleInstanceMap(instancePrefix, indexString);
                    log(Level.FINEST, "createAuthInstances",
                            "instanceMap = " + instanceMap);
                    if (!instanceMap.containsKey(INSTANCE_REALM)) {
                        log(Level.SEVERE, "createAuthInstances",
                                "The authentication module instance " +
                                "configuration map does not contain the key " +
                                INSTANCE_REALM);
                        assert false;
                    }
                    String instanceRealm = instanceMap.get(INSTANCE_REALM);
                    log(Level.FINE, "createAuthInstances", "Removing key " +
                            INSTANCE_REALM + " from the instance map");
                    instanceMap.remove(INSTANCE_REALM);
                    log(Level.FINEST, "createAuthInstances",
                            "instanceMap = " + instanceMap);
                    if (!instanceMap.containsKey(INSTANCE_NAME)) {
                        log(Level.SEVERE, "createAuthInstances",
                                "The authentication module instance " +
                                "configuration map does not contain the key " +
                                INSTANCE_NAME);
                        assert false;
                    }
                    String instanceName = instanceMap.get(INSTANCE_NAME);
                    log(Level.FINE, "createAuthInstances", "Removing key " +
                            INSTANCE_NAME + " from the instance map");
                    instanceMap.remove(INSTANCE_NAME);
                    log(Level.FINEST, "createAuthInstances",
                            "instanceMap = " + instanceMap);
                    if (!instanceMap.containsKey(INSTANCE_SERVICE)) {
                        log(Level.SEVERE, "createAuthInstances",
                                "The authentication module instance " +
                                "configuration map does not contain the key " +
                                INSTANCE_SERVICE);
                        assert false;
                    }
                    String instanceService = instanceMap.get(INSTANCE_SERVICE);
                    log(Level.FINE, "createAuthInstances", "Removing key " +
                            INSTANCE_SERVICE + " from the instance map");
                    instanceMap.remove(INSTANCE_SERVICE);
                    log(Level.FINEST, "createAuthInstances",
                            "instanceMap = " + instanceMap);
                    createAuthInstance(instanceRealm, instanceService,
                            instanceName, instanceMap);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "createAuthInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Delete authentication module instances.
     */
    public void deleteAuthInstances()
    throws Exception {

        try {
            String instancesToCreate =
                    ((String)globalAuthInstancesMap.get("instances-to-create"));
            String[] instanceTokens = instancesToCreate.split("\\|");

            for (String instanceToken: instanceTokens) {
                String[] tokenFields = instanceToken.split(",");
                String instancePrefix = tokenFields[0];
                int numberOfInstances = new Integer(tokenFields[1]).intValue();

                for (int iIndex = 1; iIndex <= numberOfInstances; iIndex++) {
                    String indexString = Integer.toString(iIndex);
                    Map<String, String> instanceMap =
                            getModuleInstanceMap(instancePrefix, indexString);
                    log(Level.FINEST, "deleteAuthInstances",
                            "instanceMap = " + instanceMap);
                    String instanceRealm = instanceMap.get(INSTANCE_REALM);
                    String instanceName = instanceMap.get(INSTANCE_NAME);
                    String instanceService = instanceMap.get(INSTANCE_SERVICE);
                    log(Level.FINE, "deleteAuthInstances",
                            "Deleting authentication instance " +
                            instanceName + " of service " + instanceService +
                            " from realm " + instanceRealm);
                    deleteAuthInstance(instanceRealm, instanceService,
                            instanceName);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "createAuthInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Get authentication module instance <code>Map</code>
     * @param instanceType - the type of module instance to be retrieved
     * (e.g. "ldap", "ad", "membership", etc).  This will be the prefix of the
     * properties gathered from globalAuthInstancesMap.
     * @param instanceIndex - the numeric index related to the specific module
     * instance of instanceType.
     * @returns a <code>Map</code> containing the configuration properties for
     * an authentication mode instance.
     */
     public Map getModuleInstanceMap(String instanceType, String instanceIndex) 
     throws Exception {
         Object[] params = {instanceType, instanceIndex};
         entering("getModuleInstanceMap", params);
         Map instanceMap = new HashMap();

         try {
             if (!instanceType.equals(LDAP_INSTANCE_PREFIX) &&
                     !instanceType.equals(MEMBERSHIP_INSTANCE_PREFIX) &&
                     !instanceType.equals(DATASTORE_INSTANCE_PREFIX) &&
                     !instanceType.equals(AD_INSTANCE_PREFIX) &&
                     !instanceType.equals(ANONYMOUS_INSTANCE_PREFIX) &&
                     !instanceType.equals(NT_INSTANCE_PREFIX) &&
                     !instanceType.equals(JDBC_INSTANCE_PREFIX) &&
                     !instanceType.equals(RADIUS_INSTANCE_PREFIX) &&
                     !instanceType.equals(UNIX_INSTANCE_PREFIX)) {
                 log(Level.SEVERE, "getModuleInstanceMap", "Instance type " +
                         instanceType + " is not recognized.");
                 assert false;
             }
             Set<String> propertyKeys = globalAuthInstancesMap.keySet();
             for (Iterator iter=propertyKeys.iterator(); iter.hasNext(); ) {
                 String propertyKey = (String)iter.next();
                 if (propertyKey.startsWith(instanceType)) {
                     String[] propertyTokens = propertyKey.split("\\.");
                     if (propertyTokens.length < 2) {
                         log(Level.SEVERE, "getModuleInstanceMap",
                                 "The property key " + propertyKey +
                                 " contained less than 2 tokens");
                         assert false;
                     }
                     if (propertyTokens.length == 2 ||
                             (propertyTokens[2].equals(instanceIndex))) {
                         String propertyValue =
                                 (String) globalAuthInstancesMap.get(
                                 propertyKey);
                         String propertyName = propertyTokens[1];
                         if (!propertyName.equals(INSTANCE_REALM) &&
                                 !propertyName.equals(INSTANCE_NAME) &&
                                 !propertyName.equals(INSTANCE_SERVICE)) {
                             Set propValueSet = new HashSet();
                             propValueSet.add(propertyValue);
                             instanceMap.put(propertyName, propValueSet);
                         } else {
                             instanceMap.put(propertyName, propertyValue);
                         }
                     }
                 }
             }
             if (instanceMap.size() == 0) {
                 log(Level.SEVERE, "getModuleInstanceMap",
                         "The authentication module instance map for instance "
                         + instanceType + instanceIndex + " is empty!");
                 assert false;
             }
             log(Level.FINEST, "getModuleInstanceMap", 
                     "The authentication module instance map for instance " + 
                     instanceType + instanceIndex + " = " + instanceMap);
             return instanceMap;
         } catch (Exception e) {
            log(Level.SEVERE, "testZeroPageLogin", e.getMessage(), params);
            e.printStackTrace();
            throw e;             
         }
     }

     /**
      * Tests zero page login for anonymous authentication for given mode.
      * @param wc - the web client simulating the user's browser.
      * @param user - the user ID that will be authenticated.
      * @param mode - the type of authentication being used (e.g. module, user,
      * authlevel, role, service, etc.)
      * @param modeValue - the value for the authentication type being used.
      * @param passMsg - the message which should appear in the resulting browser
      * page when successful.
      */
     public void testZeroPageLogin(WebClient wc, String user, String mode,
            String modeValue, String passMsg)
     throws Exception {
         testZeroPageLogin(wc, user, null, mode, modeValue, passMsg);
     }

    /**
     * Tests zero page login for given mode. 
     * @param wc - the web client simulating the user's browser.
     * @param user - the user ID that will be authenticated.
     * @param password - the password for "user".
     * @param mode - the type of authentication being used (e.g. module, user,
     * authlevel, role, service, etc.)
     * @param modeValue - the value for the authentication type being used.
     * @param passMsg - the message which should appear in the resulting browser
     * page when successful.
     */
    public void testZeroPageLogin(WebClient wc, String user, String password, 
            String mode, String modeValue, String passMsg)
    throws Exception {
        Object[] params = {user, password, mode, modeValue, passMsg};
        entering("testZeroPageLogin", params);
        String baseLoginString = null;        
        String loginString = null;
        
        try {
            baseLoginString = protocol + ":" + "//" + host + ":" + port + uri +
                    "/UI/Login?";
            StringBuffer loginBuffer = new StringBuffer(baseLoginString);
            loginBuffer.append(mode).append("=").append(modeValue).
                    append("&IDToken1=").append(user);
            
            if (password != null) {
                loginBuffer.append("&IDToken2=").append(password);
            }

            loginString = loginBuffer.toString();             
            log(Level.FINEST, "testZeroPageLogin", loginString);
            URL url = new URL(loginString);
            HtmlPage page = (HtmlPage)wc.getPage(url);
            
            log(Level.FINEST, "testZeroPageLogin", "Title of resulting page = "
                    + page.getTitleText());
            // Tests for everything if mode is not set to "role" or the 
            // configured plugin is of type amsdk.
            ssoToken = getToken(adminUser, adminPassword, basedn);
            smsCommon = new SMSCommon(ssoToken);
            if (!mode.equalsIgnoreCase("role") || 
                    smsCommon.isPluginConfigured(ssoToken,
                    SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm )) {             
                assert this.getHtmlPageStringIndex(page, passMsg) != -1;
            } else {
                log(Level.FINEST, "testZeroPageLogin", 
                        "Role based test is skipped for non amsdk plugin ...");
            }   
        } catch (Exception e) {
            log(Level.SEVERE, "testZeroPageLogin", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        } finally {
            if (ssoToken != null) {
                destroyToken(ssoToken);
            }
        }
        exiting("testZeroPageLogin");
    }

    /**
     * Retrieve the SMSCommon instance.
     * @return <code>SMSCommon</code> instance
     */
    public SMSCommon getSMSCommon() { return smsCommon; }

    /**
     * Retrieve the IDMCommon instance.
     * @return <code>IDMCommon</code> instance
     */
    public IDMCommon getIDMCommon() { return idmCommon; }
    
    /**
     * Returns the profile attribute based on the profile test performed
     * @param profile - the value which indicates how the profile creation
     * should be set.
     * @return a String containing the profile creation attribute name/value
     * pair to update in the authentication service.
     */
    public String getProfileAttribute(String profile){
        String profileAttribute = null;
        if (profile.equals("dynamic")) {
            profileAttribute = "true";
        } else if(profile.equals("required")) {
            profileAttribute = "false";
        } else {
            profileAttribute = "ignore";
        }
        return profileAttribute;
    }
    
    /**
     * Create an authentication module instance in a realm.
     * @param instanceRealm - the realm in which the authentication module should
     * be created.
     * @param authService - the service corresponding to the authentication
     * module to be created (e.g. "iPlanetAMAuthLDAPService").
     * @param instanceName - the name of the authentication module to be created
     * @param subConfigId - the ID of the parent configuration
     * @param attrValues - a Map containing the attributes values to be set in
     * the authentication module to be created
     */
    public void createAuthInstance(String instanceRealm,
            String authService,
            String instanceName,
            String subConfigId,
            Map attrValues)
    throws Exception {
        Object[] params = {instanceRealm, authService, instanceName,
                subConfigId, attrValues};
        entering("createAuthInstance", params);
        SSOToken idToken = null;
        try {
            if (instanceRealm == null) {
                log(Level.SEVERE, "createAuthInstance",
                        "instanceRealm was null");
                assert false;
            }
            if (authService == null) {
                log(Level.SEVERE, "createAuthInstance", "authService was null");
                assert false;
            }
            if (instanceName == null) {
                log(Level.SEVERE, "createAuthInstance",
                        "instanceName was null");
                assert false;
            }
            if (subConfigId == null) {
                log(Level.SEVERE, "createAuthInstance", "subConfigId was null");
                assert false;
            }
            
            idToken = getToken(adminUser, adminPassword, basedn);

            smsCommon = new SMSCommon(idToken);

            if ((instanceRealm != null) && !instanceRealm.equals("/")) {
                String absoluteRealm = instanceRealm;
                if (instanceRealm.indexOf("/") != 0) {
                    absoluteRealm = "/" + instanceRealm;
                }
                String parentRealm = idmCommon.getParentRealm(absoluteRealm);
                String childRealm =
                        absoluteRealm.substring(absoluteRealm.lastIndexOf("/")
                        + 1);
                if (idmCommon.searchRealms(idToken, childRealm,
                        parentRealm).isEmpty()) {
                    Map realmMap = new HashMap();
                    Set realmSet = new HashSet();
                    realmSet.add("Active");
                    realmMap.put("sunOrganizationStatus", realmSet);
                    idmCommon.createIdentity(idToken, parentRealm,
                            IdType.REALM, childRealm, realmMap);
                    Thread.sleep(notificationSleepTime);
                }
            }
            log(Level.FINE, "createAuthInstance",
                    "Creating the authentication instance " + instanceName +
                    " in realm " + instanceRealm);
            smsCommon.addSubConfig(instanceRealm, authService, instanceName,
                    subConfigId, attrValues);
            exiting("createAuthInstance");
        } catch (Exception e) {
            log(Level.SEVERE, "createAuthInstance", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (idToken != null) {
                destroyToken(idToken);
            }
        }
    }

    /**
     * Create an authentication module in the root realm.
     * @param authService - the service corresponding to the authentication
     * module to be created (e.g. "iPlanetAMAuthLDAPService").
     * @param instanceName - the name of the authentication module to be created
     * @param subConfigId - the ID of the parent configuration
     * @param attrValues - a Map containing the attributes values to be set in
     * the authentication module to be created
     */
    public void createAuthInstance(String instanceRealm,
            String authService,
            String instanceName,
            Map attrValues)
    throws Exception {
        createAuthInstance(instanceRealm, authService, instanceName,
                AUTH_INSTANCE_SUBCONFIGID, attrValues);
    }

    /**
     * Create an authentication module in the root realm.
     * @param authService - the service corresponding to the authentication
     * module to be created (e.g. "iPlanetAMAuthLDAPService").
     * @param instanceName - the name of the authentication module to be created
     * @param attrValues - a Map containing the attributes values to be set in
     * the authentication module to be created
     */
    public void createAuthInstance(String authService,
            String instanceName,
            Map attrValues)
    throws Exception {
        createAuthInstance("/", authService,
                instanceName, AUTH_INSTANCE_SUBCONFIGID, attrValues);
    }

    /**
     * Create an authentication configuration (chain) in a realm.
     * @param configRealm - the realm in which the authentication configuration
     * should be created.
     * @param configName - the name of the authentication configuration to be
     * created.
     * @param attributeValues - a Map containing the attribute values to be set
     * in the authentication configuration.  attrValues should contain the
     * following keys:
     * iplanet-am-auth-configuration - an XML string with the authentication
     * module(s), criteria, and any JAAS options
     * iplanet-am-auth-login-success-url - the URL to which the user is
     * redirected upon a successful authentication.
     * iplanet-am-auth-login-failure-url - the URL to which the user is
     * redirected upon a failed authentication.
     */
     public void createAuthConfig(String configRealm,
             String configName,
             String[] configInstances,
             Map attrValues)
     throws Exception {
         try {
             if (configName == null) {
                 log(Level.SEVERE, "createAuthConfig",
                         "Authentication configuration name was null");
                 assert false;
             }
             String subConfigName = "Configurations/" + configName;
             String configXML = createAuthConfigXML(configInstances);

             Map confAttrs = new HashMap();
             if (attrValues != null) {
                 Set keySet = attrValues.keySet();
                 for (Iterator keyIter = keySet.iterator(); keyIter.hasNext();)
                 {
                    String key = (String)keyIter.next();
                    String value = (String)attrValues.get(key);
                    Set valueSet = new HashSet();
                    valueSet.add(value);
                    confAttrs.put(key, valueSet);
                 }
             }

             Set xmlSet = new HashSet();
             xmlSet.add(configXML);
             confAttrs.put("iplanet-am-auth-configuration", xmlSet);

             ssoToken = getToken(adminUser, adminPassword, basedn);
             smsCommon = new SMSCommon(ssoToken);
             smsCommon.addSubConfig(configRealm,
                     AUTH_CONFIGURATION_SERVICE_NAME,
                     subConfigName, AUTH_CONFIGURATION_SUBCONFIGID, confAttrs);
         } catch (Exception e) {
            log(Level.SEVERE, "createAuthConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             if (ssoToken != null) {
                 destroyToken(ssoToken);
             }
         }
     }
     
     private String createAuthConfigXML(String[] instanceEntries)
     throws Exception {
         StringBuffer xmlBuff = new StringBuffer("<AttributeValuePair>");
         for (String instanceEntry: instanceEntries) {
             String updatedEntry = instanceEntry.replace(",", " ");
             log(Level.FINEST, "createAuthConfigXML", "updatedEntry = " +
                     updatedEntry);
             xmlBuff.append("<Value>").append(updatedEntry).append("</Value>");
             log(Level.FINEST, "createAuthConfigXML", "configXML = " +
                     xmlBuff);
         }
         xmlBuff.append("</AttributeValuePair>");
         return(xmlBuff.toString());
     }

     /**
      * Create an authentication configuration in a realm.
      * @param configRealm - the realm in which the authentication configuration
      * should be created.
      * @param configName - the name of the authentication configuration to be
      * created.
      * @param configInstances - the authentication instances in the
      * authentication configuration (e.g.
      * <instamce-name1>,criteria1,[JAAS-flags1]|...|
      * <instamce-name(n)>,criteria(n),[JAAS-flags(n)]
      */

     /**
      * Create an authentication configuration (chain) in the root realm.
      * @param configName - the name of the authentication configuration to be
      * created.
      * @param attrValues - a Map containing the attribute values to be set
      * in the authentication configuration.
      */
     public void createAuthConfig(String configName, 
             String[] configInstances,
             Map attrValues)
     throws Exception {
         createAuthConfig("/", configName, configInstances, attrValues);
     }

     /**
      * Delete an authentication module instance in a realm.
      * @realmName - the name of the realm in which the authentication module
      * instance should be deleted.
      * @authService - the authentication service corresponding to the
      * authentication module instance to be deleted.
      * @instanceName - the name of authentication module instance to be
      * deleted.
      */
     public void deleteAuthInstance(String realmName,
             String authService,
             String instanceName)
     throws Exception {
         try {
             ssoToken = getToken(adminUser, adminPassword, basedn);
             smsCommon = new SMSCommon(ssoToken);
             smsCommon.deleteSubConfig(realmName, authService, instanceName);
         } catch (Exception e) {
            log(Level.SEVERE, "deleteAuthInstance", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             if (ssoToken != null) {
                 destroyToken(ssoToken);
             }
         }
     }

     /**
      * Delete an authentication module instance in a realm.
      * @realmName - the name of the realm in which the authentication module
      * instance should be deleted.
      * @authService - the authentication service corresponding to the
      * authentication module instance to be deleted.
      * @instanceName - the name of authentication module instance to be
      * deleted.
      */
     public void deleteAuthInstance(String authService, String instanceName)
     throws Exception {
         deleteAuthInstance("/", authService, instanceName);
     }

     /**
      * Delete an authentication configuration in a realm.
      * @realmName - the name of the realm in which the authentication
      * configuration should be deleted.
      * @configName - the name of the authentication configuration to be
      * deleted.
      */
     public void deleteAuthConfig(String realmName, String configName)
     throws Exception {
        Object[] params = {realmName, configName};
        entering("deleteAuthConfig", params);
        try {
            if (configName == null) {
                log(Level.SEVERE, "deleteAuthConfig",
                        "Authentication configuration name was null");
            }
            String subConfigName = "Configurations/" + configName;
            ssoToken = getToken(adminUser, adminPassword, basedn);
            smsCommon = new SMSCommon(ssoToken);
            smsCommon.deleteSubConfig(realmName,
                    AUTH_CONFIGURATION_SERVICE_NAME,
                    subConfigName);
            exiting("deleteAuthConfig");
        } catch (Exception e) {
            log(Level.SEVERE, "deleteAuthConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (ssoToken != null) {
                destroyToken(ssoToken);
            }
        }
     }

     /**
      * Delete an authentication configuration in a realm.
      * @configName - the name of the authentication configuration to be
      * deleted.
      */
     public void deleteAuthConfig(String configName)
     throws Exception {
        deleteAuthConfig("/", configName);
     }

     /**
      * Get the name of an authentication module instance
      * @prefix - the authentication config property prefix (e.g. "ldap",
      * "membership", "datastore", "ad", etc.
      * @index - the authentication instance index
      * @return the name of the authentication instance.
      */
     public String getAuthInstanceName(String prefix, String index) 
     throws Exception {
         String instanceName = null;
         String nameKey = prefix + "." + INSTANCE_NAME + "." + index;

         if (globalAuthInstancesMap == null) {
             log(Level.SEVERE, "getAuthInstanceName",
                     "The global authentication instances map is null!");
             assert false;
         }

         try {
             if (globalAuthInstancesMap.containsKey(nameKey)) {
                 instanceName = globalAuthInstancesMap.get(prefix + "." +
                         INSTANCE_NAME + "." + index);
             } else {
                 log(Level.SEVERE, "getAuthInstanceName",
                         "The global authentication instances map does not " +
                         "contain key " + nameKey);
                 assert false;
             }
         } catch (Exception e) {
            log(Level.SEVERE, "deleteAuthConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             return instanceName;
         }
     }

     /**
      * Get the name of the primary authentication module instance
      * @prefix - the authentication config property prefix (e.g. "ldap",
      * "membership", "datastore", "ad", etc.
      * @return the name of the authentication instance.
      */
     public String getAuthInstanceName(String prefix)
     throws Exception {
         return getAuthInstanceName(prefix, "1");
     }

     /**
      * Get the authentication level of an authentication module instance'
      * @param prefix - the authentication config property prefix (e.g. "ldap",
      * "membership", "datastore", "ad", etc.
      * @param index - the authentication instance index
      * @return the authlevel of the authentication instance.
      */
     public String getAuthInstanceLevel(String prefix, String index)
     throws Exception {
         String levelValue = null;
         String levelAttrName = null;

         try {
             levelAttrName = getAuthLevelAttrName(prefix);
             log(Level.FINEST, "getAuthInstanceLevel", "authLevelAttrName = " +
                     levelAttrName);

            String serviceKey = prefix + "." + INSTANCE_SERVICE;
             if (!globalAuthInstancesMap.containsKey(serviceKey)) {
                 log(Level.SEVERE, "getAuthInstanceLevel",
                         "The global authentication instances map " +
                         "does not contain the key " + serviceKey);
                 assert false;
             }
             String serviceName = globalAuthInstancesMap.get(serviceKey);
             log(Level.FINEST, "getAuthInstanceLevel", "serviceName = " +
                     serviceName);

             String realmKey = prefix + "." + INSTANCE_REALM + "." + index;
             if (!globalAuthInstancesMap.containsKey(realmKey)) {
                 realmKey = prefix + "." + INSTANCE_REALM;
                 if (!globalAuthInstancesMap.containsKey(realmKey)) {
                     log(Level.SEVERE, "getAuthInstanceLevel",
                             "The global authentication instances map " +
                             "does not contain the key " + realmKey);
                     assert false;
                 }
             }
             String instanceRealm = globalAuthInstancesMap.get(realmKey);
             log(Level.FINEST, "getAuthInstanceLevel", "instanceRealm = " +
                     instanceRealm);

             String instanceName = getAuthInstanceName(prefix, index);
             log(Level.FINEST, "getAuthInstanceLevel", "instanceName = " +
                     instanceName);

             ssoToken = getToken(adminUser, adminPassword, basedn);
             smsCommon = new SMSCommon(ssoToken);
             Set valueSet =
                     smsCommon.getAttributeValueServiceConfig(instanceRealm,
                     serviceName, instanceName, levelAttrName);
             if (valueSet.isEmpty()) {
                 log(Level.SEVERE, "getAuthInstanceLevel",
                         "Unable to obtain auth level from instance " +
                         instanceName + " of service " + serviceName);
                 assert false;
             }
             log(Level.FINEST, "getAuthInstanceLevel",
                     "Auth instance level set = " + valueSet);
             Iterator setIter = valueSet.iterator();
             while (setIter.hasNext()) {
                 levelValue = (String) setIter.next();
                 log(Level.FINEST, "getAuthInstanceLevel", "levelValue = " +
                         levelValue);
             }
         } catch (Exception e) {
            log(Level.SEVERE, "getAuthInstanceLevel", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             if (ssoToken != null) {
                 destroyToken(ssoToken);
             }
             return levelValue;
         }
     }

     /**
      * Get the authentication level of an authentication module instance'
      * @param prefix - the authentication config property prefix (e.g. "ldap",
      * "membership", "datastore", "ad", etc.
      * @return the authlevel of the authentication instance.
      */
     public String getAuthInstanceLevel(String prefix)
     throws Exception {
         return getAuthInstanceLevel(prefix, "1");
     }

     /**
      * Verify if a Unix or NT authentication module will be used on Windows.
      * moduleType - a String containing the module type
      * (e.g. LDAP, AD, NT, etc.) that will be used in the test
      * @return false if the module type is unix or nt and the operating system
      * of the FAM deployment is not Windows based or true otherwise.
      */
     public boolean isValidModuleTest(String moduleType)
     throws Exception {
        boolean validTest = true;
        if (moduleType.equalsIgnoreCase("unix") ||
                moduleType.equalsIgnoreCase("nt")) {
            webClient = new WebClient();
            String osType = getServerConfigValue(webClient, "Operating System");
            if (moduleType.equalsIgnoreCase("unix")) {
                String productVersion = getServerConfigValue(webClient,
                        "OpenSSO Version");
                validTest = !osType.toLowerCase().contains("windows") &&
                        !osType.toLowerCase().contains("aix") &&
                        (productVersion.toLowerCase().contains("enterprise") ||
                        productVersion.toLowerCase().contains("express"));
            }
            if (moduleType.equalsIgnoreCase("nt")) {
                validTest = !osType.toLowerCase().contains("windows");
            }
        }
        return validTest;

     }
     
     /**
      * Retrieve the auth level attribute name for a particluar module type.
      * type - a String containing the type of module (e.g. "ldap", "ad", etc.)
      * @return - a String with the name of the authentication module's 
      * auth level attribute name.
      */
     private String getAuthLevelAttrName(String type)
     throws Exception {
         String levelProp = "";
         try {
           if (type.equals(LDAP_INSTANCE_PREFIX)) {
               levelProp = LDAP_AUTH_LEVEL;
           } else if (type.equals(DATASTORE_INSTANCE_PREFIX)) {
               levelProp = DATASTORE_AUTH_LEVEL;
           } else if (type.equals(MEMBERSHIP_INSTANCE_PREFIX)) {
               levelProp = MEMBERSHIP_AUTH_LEVEL;
           } else if (type.equals(ANONYMOUS_INSTANCE_PREFIX)) {
               levelProp = ANONYMOUS_AUTH_LEVEL;
           } else if (type.equals(JDBC_INSTANCE_PREFIX)) {
               levelProp = JDBC_AUTH_LEVEL;
           } else if (type.equals(AD_INSTANCE_PREFIX)) {
               levelProp = AD_AUTH_LEVEL;
           } else if (type.equals(NT_INSTANCE_PREFIX)) {
               levelProp = NT_AUTH_LEVEL;
           } else if (type.equals(RADIUS_INSTANCE_PREFIX)) {
               levelProp = RADIUS_AUTH_LEVEL;
           } else if (type.equals(UNIX_INSTANCE_PREFIX)) {
               levelProp = UNIX_AUTH_LEVEL;
           } else {
                 log(Level.SEVERE, "getAuthInstanceLevel", "The type value " +
                         type + " is not recognized.");
                 assert false;
           }
         } catch (Exception e) {
            log(Level.SEVERE, "getAuthInstanceLevel", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             return levelProp;
         }
     }

    /**
     * Perform remote (API based) authentication.
     * @param authRealm - a String containing the name of the realm in which
     * the user should be authenticated.
     * @param authType - a String containing the type of authentication to be
     * performed.  Recognized values are "module", "authlevel", "user",
     * "service", and "role".
     * @param indexName - a String containing the value supplied to
     * <code>AuthContext.login()</code>.  For a module based authentication,
     * indexName would be the name of the authentication module instance which
     * should be used for authentication.
     * @param userName - the user ID to be authentication
     * @param password - the password for the user specified in userName
     * @param instanceName - the name of the authentication module instance.
     * Used to handle a <code>ChoiceCallback</code> to select a particular
     * authentication module instance.
     * @return an <code>SSOToken</code> if the authentication is successful
     * and null if the authentication
     */
    public SSOToken performRemoteLogin(String authRealm, String authType,
            String indexName, String userName,
            String password, String instanceName)
    throws Exception {
        Object[] params = {authRealm, authType, indexName, userName, password,
                instanceName};

        entering("performRemoteLogin", params);
        AuthContext lc = null;
        Callback[] callbacks = null;
        SSOToken obtainedToken = null;
        AuthContext.IndexType indexType = null;

        if (authType != null) {
            if (authType.equals("module")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
            } else if (authType.equals("authlevel") ||
                    authType.equals("level")) {
                indexType = AuthContext.IndexType.LEVEL;
            } else if (authType.equals("user")) {
                indexType = AuthContext.IndexType.USER;
            } else if (authType.equals("service")) {
                indexType = AuthContext.IndexType.SERVICE;
            } else if (authType.equals("role")) {
                indexType = AuthContext.IndexType.ROLE;
            } else {
                log(Level.SEVERE, "performRemoteLogin",
                        "Unsupported authType " + authType + " provided.  " +
                        "Expecting either \"module\", \"level\", \"user\", " +
                        "\"service\", or \"role\".");
                assert false;
            }
        } else {
            log(Level.SEVERE, "performRemoteLogin", "authType was set to null");
            assert false;
        }

        try {
            try {
                lc = new AuthContext(authRealm);
                log(Level.FINE, "performRemoteLogin",
                        "Invoking AuthContext.login with indexName " +
                        indexName + " ...");
                lc.login(indexType, indexName);
            } catch (AuthLoginException le) {
                throw le;
            }

            while (lc.hasMoreRequirements()) {
                callbacks = lc.getRequirements();
                if (callbacks != null) {
                    try {
                        for (int i = 0; i < callbacks.length; i++) {
                            if (callbacks[i] instanceof NameCallback) {
                                NameCallback namecallback =
                                        (NameCallback)callbacks[i];
                                namecallback.setName(userName);
                            }
                            if (callbacks[i] instanceof PasswordCallback) {
                                PasswordCallback passwordcallback =
                                        (PasswordCallback)callbacks[i];
                                passwordcallback.setPassword(
                                        password.toCharArray());
                            }
                            if (callbacks[i] instanceof ChoiceCallback) {
                                ChoiceCallback choiceCallback =
                                        (ChoiceCallback)callbacks[i];
                                String[] strChoices =
                                        choiceCallback.getChoices();
                                int choiceIndex = -1;
                                for (int j=0; j < strChoices.length; j++) {
                                     if (strChoices[j].equals(instanceName)) {
                                         choiceIndex = j;
                                         break;
                                     }
                                }
                                choiceCallback.setSelectedIndex(choiceIndex);
                            }
                        }
                        log(Level.FINE, "performRemoteLogin",
                                "Submitting callbacks ...");
                        lc.submitRequirements(callbacks);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }

            log(Level.FINE, "performRemoteLogin",
                    "Retrieving authentication status ...");
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                log(Level.FINEST, "performRemoteLogin",
                        "Authentication was successful");
                try {
                    obtainedToken = lc.getSSOToken();
                } catch (Exception e) {
                    throw e;
                }
            }
        } catch (Exception e) {
                log(Level.SEVERE, "performRemoteLogin", e.getMessage());
                e.printStackTrace();
                throw e;
        } finally {
            return obtainedToken;
        }
    }

    public SSOToken performRemoteLogin(String authRealm, String authType,
            String indexName, String userName,
            String password)
    throws Exception {
        return performRemoteLogin(authRealm, authType, indexName,
                userName, password, null);
    }

    /**
     * Performs generic form based login
     */
    public void testFormBasedAuth(Map mapValidate)
    throws Exception {
        try {
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createAuthXML(mapValidate);
            log(Level.FINEST, "testFormBasedAuth",
                    "Login XML file: " + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            webClient = new WebClient();
            Page page = task.execute(webClient);
            log(Level.FINEST, "testFormBasedAuth",
                    "testXMLBasedAuth page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testFormBasedAuth", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Positive Service based service Login
     */
    public void testServicebasedPositive(Map mapValidate)
    throws Exception {
        try {
            boolean isNegative = false;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createServiceXML(mapValidate, isNegative);
            log(Level.FINEST, "testServicebasedPositive",
                    "testServicebasedPositive XML file:" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            webClient = new WebClient();
            Page page = task.execute(webClient);
            log(Level.FINEST, "testServicebasedPositive",
                    "testServicebasedPositive page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testServicebasedPositive", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Negative Service based service Login
     */
    public void testServicebasedNegative(Map mapValidate)
    throws Exception {
        try {
            boolean isNegative = true;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createServiceXML(mapValidate, isNegative);
            log(Level.FINEST, "testServicebasedNegative",
                    "testServicebasedNegative XML file:" + xmlFile);
            webClient = new WebClient();
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testServicebasedNegative",
                    "testServicebasedNegative page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testServicebasedNegative", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Account lockout tests
     */
    public void testAccountLockout(Map mapValidate)
    throws Exception {
        webClient = new WebClient();
        try {
            boolean isWarn = true;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createLockoutXML(mapValidate, isWarn);
            log(Level.FINEST, "testAccountLockout",
                    "testAccountLockout XML file:" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testAccountLockout",
                    "testAccountLockout page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testAccountLockout", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Accountlockout warning tests
     */
    public void testAccountLockWarning(Map mapValidate)
    throws Exception {
        webClient = new WebClient();
        try {
            boolean isWarn = false;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createLockoutXML(mapValidate, isWarn);
            log(Level.FINEST, "testAccountLockWarning",
                    "testAccountLockWarning XML file:" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testAccountLockWarning",
                    "testAccountLockWarning page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testAccountLockWarning", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Account lockout and verifies the inetuser status after the
     * lockout
     */
    public void testAccountLockoutUserStatus(Map mapValidate, String username)
    throws Exception {
        webClient = new WebClient();
        Map attrMap = new HashMap();
        try {
            boolean isWarn = true;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createLockoutXML(mapValidate, isWarn);
            log(Level.FINEST, "testAccountLockoutUserStatus",
                    "testAccountLockoutUserStatus XML file:" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testAccountLockoutUserStatus",
                    "testAccountLockoutUserStatus page after Login" +
                    page.getWebResponse().getContentAsString());
            //now verify the user attibutes
            attrMap = idmCommon.getIdentityAttributes(username, realm);
            if (attrMap.containsKey("inetuserstatus")) {
                Set userSet = (Set) attrMap.get("inetuserstatus");
                for (Iterator itr = userSet.iterator(); itr.hasNext();) {
                    String userStatus = (String) itr.next();
                    if (userStatus.equals("Inactive")) {
                        log(Level.FINE, "testAccountLockoutUserStatus",
                                "ValidationPass" + userStatus);
                        assert true;
                    } else {
                        log(Level.FINE, "testAccountLockoutUserStatus",
                                "ValidationFail" + userStatus);
                        assert false;
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testAccountLockoutUserStatus",
                    e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs Account lockout user and verifies the custom attributes
     * after lockout
     */
    public void testAccountLockoutUserAttr(Map mapValidate, String username,
            String attrName,
            String attrValue)
    throws Exception {
        webClient = new WebClient();
        Map attrMap = new HashMap();
        try {
            boolean isWarn = true;
            CreateTestXML testXML = new CreateTestXML();
            String xmlFile = testXML.createLockoutXML(mapValidate, isWarn);
            log(Level.FINEST, "testAccountLockoutUserAttribute",
                    "testAccountLockoutUserAttribute XML file" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testAccountLockoutUserAttr page after login",
                    "testAccountLockoutUserAttr After login" +
                    page.getWebResponse().getContentAsString());
            //now verify the user attibutes
            attrMap = idmCommon.getIdentityAttributes(username, realm);
            if (attrMap.containsKey(attrName)) {
                Set attrSet = (Set) attrMap.get(attrName);
                for (Iterator itr = attrSet.iterator(); itr.hasNext();) {
                    String attrVal = (String) itr.next();
                    if (attrVal.equals(attrValue)) {
                        log(Level.FINEST, "testAccountLockoutUserAttr",
                                "ValidationPass" + attrVal);
                        assert true;
                    } else {
                        log(Level.FINEST, "testAccountLockoutUserAttr",
                                "ValidationFail" + attrVal);
                        assert false;
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testAccountLockoutUserAttr", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Performs login with Authentication attributes and verifies the user
     * attibutes. This is performed for the module based authenticaation
     */
    public void testUserLoginAuthAttribute(Map executeMap, Map userAttrMap)
    throws Exception {
        webClient = new WebClient();
        Map idmAttrMap = new HashMap();
        String userattrName;
        Iterator attIterator;
        String userattrVal;
        Set idmattrSet;
        String idmattrVal;
        try {
            CreateTestXML testXML = new CreateTestXML();
            String userCreds = (String) executeMap.get("users");
            String testRealm = (String) executeMap.get("realm");
            String[] userTokens = userCreds.split(":");
            String userName = userTokens[0];

            //now verify the user attibutes
            idmAttrMap = idmCommon.getIdentityAttributes(userName, testRealm);
            log(Level.FINEST, "testUserLoginAuthAttribute", "idmAttrMap" +
                    idmAttrMap);
            log(Level.FINEST, "testUserLoginAuthAttribute", "userAttrMap" +
                    userAttrMap);
            for (attIterator = userAttrMap.keySet().iterator();
                    attIterator.hasNext();) {
                userattrName = (String) attIterator.next();
                if (userattrName.equals("userpassword")) {
                    continue;
                }
                Set userattrSet = (Set) userAttrMap.get(userattrName);
                for (Iterator iter = userattrSet.iterator(); iter.hasNext();) {
                    userattrVal = (String) iter.next();
                    if (idmAttrMap.containsKey(userattrName)) {
                        idmattrSet = (Set) idmAttrMap.get(userattrName);
                        for (Iterator itr = idmattrSet.iterator();
                                itr.hasNext();) {
                            idmattrVal = (String) itr.next();
                            if (idmattrVal.equals(userattrVal)) {
                                log(Level.FINE,
                                        "testUserLoginAuthAttribute",
                                        "ValidationPass" + userattrVal);
                                assert true;
                            } else {
                                log(Level.FINE,
                                        "testUserLoginAuthAttribute",
                                        "ValidationFail" + userattrVal);
                                assert false;
                            }
                        }
                    }
                }
            }

            String xmlFile = testXML.createAuthXML(executeMap);
            log(Level.FINEST, "testUserLoginAuthAttribute",
                    "testUserLoginAuthAttribute XML file" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            Page page = task.execute(webClient);
            log(Level.FINEST, "testUserLoginAuthAttribute",
                    "testUserLoginAuthAttribute page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "testUserLoginAuthAttribute", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
    }

    /**
     * Retrieve the attribute values for a particular authentication module
     * instance.
     * @param instanceType - a <code>String</code> containing the type of
     * authentication module (e.g. "ldap", "datastore", "ad", "jdbc", etc.)
     * @param instanceIndex - a <code>String</code> containing the index for
     * the particular authentication module instance.
     * @return a <code>Map</code> containing the attribute values for the
     * authentication module instance.  The keys are <code>String</code> objects
     * with the attribute names and the values are <code>Set</code> objects
     * containing the attribute values.
     * @throws Exception
     */
    public Map getAuthInstanceAttrs(String instanceType, String instanceIndex)
    throws Exception {
         Map attrValues = null;

         try {
            String serviceKey = instanceType + "." + INSTANCE_SERVICE;
             if (!globalAuthInstancesMap.containsKey(serviceKey)) {
                 log(Level.SEVERE, "getAuthInstanceAttrs",
                         "The global authentication instances map " +
                         "does not contain the key " + serviceKey);
                 assert false;
             }
             String serviceName = globalAuthInstancesMap.get(serviceKey);
             log(Level.FINEST, "getAuthInstanceAttrs", "serviceName =" +
                     serviceName);

             String realmKey = instanceType + "." + INSTANCE_REALM + "." +
                     instanceIndex;
             if (!globalAuthInstancesMap.containsKey(realmKey)) {
                 realmKey = instanceIndex + "." + INSTANCE_REALM;
                 if (!globalAuthInstancesMap.containsKey(realmKey)) {
                     log(Level.SEVERE, "getAuthInstanceAttrs",
                             "The global authentication instances map " +
                             "does not contain the key " + realmKey);
                     assert false;
                 }
             }
             String instanceRealm = globalAuthInstancesMap.get(realmKey);
             log(Level.FINEST, "getAuthInstanceAttrs", "instanceRealm = " +
                     instanceRealm);

             String instanceName = getAuthInstanceName(instanceType,
                     instanceIndex);
             log(Level.FINEST, "getAuthInstanceAttrs", "instanceName = " +
                     instanceName);

             ssoToken = getToken(adminUser, adminPassword, basedn);
             smsCommon = new SMSCommon(ssoToken);
             attrValues = smsCommon.getSubConfigAttrs(instanceRealm,
                     serviceName,
                     instanceName);
             if (attrValues == null) {
                 log(Level.SEVERE, "getAuthInstanceAttrs",
                         "Unable to obtain attribute values from instance " +
                         instanceName + " of service " + serviceName);
                 assert false;
             }
             log(Level.FINEST, "getAuthInstanceAttrs",
                     "Auth instance attribute map = " + attrValues);
         } catch (Exception e) {
            log(Level.SEVERE, "getAuthInstanceAttrs", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             if (ssoToken != null) {
                 destroyToken(ssoToken);
             }
             return attrValues;
         }
    }

    /**
     * Set attribute values in an authentication module instance
     * @param instanceType - a <code>String</code> containing the type of
     * authentication module (e.g. "ldap", "datastore", "ad", "jdbc", etc.)
     * @param instanceIndex - a <code>String</code> containing the index for
     * the particular authentication module instance.
     * @param attrValues - a <code>Map</code> containing attribute values to
     * be set in the authentication module instance.
     * @throws Exception
     */
    public void setAuthInstanceAttrs(String instanceType,
            String instanceIndex,
            Map attrValues)
    throws Exception {
         try {
            String serviceKey = instanceType + "." + INSTANCE_SERVICE;
             if (!globalAuthInstancesMap.containsKey(serviceKey)) {
                 log(Level.SEVERE, "setAuthInstanceAttrs",
                         "The global authentication instances map " +
                         "does not contain the key " + serviceKey);
                 assert false;
             }
             String serviceName = globalAuthInstancesMap.get(serviceKey);
             log(Level.FINEST, "setAuthInstanceAttrs", "serviceName =" +
                     serviceName);

             String realmKey = instanceType + "." + INSTANCE_REALM + "." +
                     instanceIndex;
             if (!globalAuthInstancesMap.containsKey(realmKey)) {
                 realmKey = instanceIndex + "." + INSTANCE_REALM;
                 if (!globalAuthInstancesMap.containsKey(realmKey)) {
                     log(Level.SEVERE, "setAuthInstanceAttrs",
                             "The global authentication instances map " +
                             "does not contain the key " + realmKey);
                     assert false;
                 }
             }
             String instanceRealm = globalAuthInstancesMap.get(realmKey);
             log(Level.FINEST, "setAuthInstanceAttrs", "instanceRealm = " +
                     instanceRealm);

             String instanceName = getAuthInstanceName(instanceType,
                     instanceIndex);
             log(Level.FINEST, "setAuthInstanceAttrs", "instanceName = " +
                     instanceName);

             ssoToken = getToken(adminUser, adminPassword, basedn);
             smsCommon = new SMSCommon(ssoToken);
             smsCommon.modifySubConfig(instanceRealm, serviceName,
                     instanceName, attrValues);
         } catch (Exception e) {
            log(Level.SEVERE, "setAuthInstanceAttrs", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
            throw e;
         } finally {
             if (ssoToken != null) {
                 destroyToken(ssoToken);
             }
         }
    }

    /**
     * Get the global authentication instances map.
     * @return the static <code>Map</code> containing all the authentication
     * instances properties.
     */
    public Map getGlobalAuthInstancesMap() {
        return globalAuthInstancesMap;
    }

    /**
     * Set property values in the globalAuthInstancesMap.
     * @param attrValues - a <code>Map</code> containing the property values to
     * be set in the globalAuthInstancesMap.
     * @throws Exception
     */
    public void setPropsInGlobalAuthInstancesMap(Map<String,String> attrValues)
    throws Exception {
        try {
           if (attrValues != null)  {
               log(Level.FINE, "setPropsInGlobalAuthInstancesMap",
                       "Putting attribues values from input map " +
                       "into globalAuthInstancesMap");
               globalAuthInstancesMap.putAll(attrValues);
               log(Level.FINE, "setPropsInGlobalAuthInstancesMap",
                       "Writing updated globalAuthInstancesMap to file ");
               createFileFromMap(globalAuthInstancesMap, 
                       AUTH_CONFIGURATION_GENERATED_PROPS);
           } else {
               log(Level.SEVERE, "setPropsInGlobalAuthInstancesMap",
                       "The Map containing the attributes values to be set" +
                       "is null");
               assert false;
           }
        } catch (Exception e) {
            log(Level.SEVERE, "setPropsInGlobalAuthInstancesMap",
                    "Exception message = " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
