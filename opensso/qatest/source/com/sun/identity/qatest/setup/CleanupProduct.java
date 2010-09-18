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
 * $Id: CleanupProduct.java,v 1.1 2008/06/26 20:27:12 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.setup;

import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This class deletes datastores and resets Policy Service at root realm
 * depending upon the parameters set in resources/config/default/UMGlobalConfig
 * and single server tests server configuration resource bundles.
 * 
 * This class is called from xml/opensso-common.xml.
 */
public class CleanupProduct extends TestCommon {
    
    private SSOToken admintoken;

    /**
     * This method make the actual calls to delete datastores in various servers
     * and modify policy service.
     */
    public CleanupProduct(String serverName0, String serverName1)
    throws Exception {
        super("CleanupProduct");
        
        try {
            String namingProtocol = "";
            String namingHost = "";
            String namingPort = "";
            String namingURI = "";

            ResourceBundle cfg0 = ResourceBundle.getBundle("Configurator-" +
                    serverName0);

            ResourceBundle gblCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "default" + fileseparator +
                    "UMGlobalConfig");
            SMSCommon smsGbl = new SMSCommon("config" + fileseparator +
                    "default" + fileseparator + "UMGlobalConfig");

            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. This loop is executed only if
            // both entries are sepcified.
            if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                    (serverName1.indexOf("SERVER_NAME2") == -1)) {

                // Delete datastores only if this flag (deleteCreatedDatastores)
                // in resources/config/UMGlobalConfig resource bundle is set to
                // true.
                if ((gblCfgData.getString("UMGlobalConfig." +
                        "deleteCreatedDatastores")).equals("true")) {

                    WebClient webClient = new WebClient();

                    // Initiating cleanup for server index 0. This is with
                    // refrence to definitions in 
                    // resources/config/UMGlobalDatstoreConfig resource bundle.
                    //This is done using famadm.jsp.
                    log(Level.FINE, "CleanupProduct", "Initiating cleanup" +
                            " for " +  serverName0);

                    String logoutURL = null;
                    try {
                        String strURL = cfg0.getString(
                                TestConstants.KEY_AMC_NAMING_URL);
                        log(Level.FINE, "CleanupProduct", "Server URL: " +
                                strURL);
                        Map map = getURLComponents(strURL);
                        log(Level.FINE, "CleanupProduct", "Server URL" +
                                " Components: " +  map);

                        namingProtocol = (String)map.get("protocol");
                        namingHost = (String)map.get("host");
                        namingPort = (String)map.get("port");
                        namingURI = (String)map.get("uri");

                        String loginURL = namingProtocol + ":" + "//" +
                                namingHost + ":" + namingPort + namingURI +
                                "/UI/Login";
                        logoutURL = namingProtocol + ":" + "//" +
                                namingHost + ":" + namingPort + namingURI +
                                "/UI/Logout";
                        String famadmURL = namingProtocol + ":" + "//" +
                                namingHost + ":" + namingPort + namingURI;

                         FederationManager famadm = 
                                 new FederationManager(famadmURL);
                            consoleLogin(webClient, loginURL, adminUser,
                                    adminPassword);

                         if (FederationManager.getExitCode(
                             famadm.deleteDatastores(webClient, realm,
                             smsGbl.getCreatedDatastoreNames(0))) != 0) {
                                 log(Level.SEVERE, "ClenaupProduct", 
                                         "deleteDatastores famadm command" +
                                         " failed");
                                 assert false;
                             }
                        } catch (Exception e) {
                            log(Level.SEVERE, "CleanupProduct", e.getMessage());
                            e.printStackTrace();
                        } finally {
                            consoleLogout(webClient, logoutURL);
                        }

                    // Initiating cleanup for server index 1. This is with
                    // refrence to definitions in resources/config/
                    // UMGlobalDatstoreConfig resource bundle. This is for
                    // single server tests and uses client api's to do all the
                    // configuration.                    
                    log(Level.FINE, "CleanupProduct", "Initiating cleanup" +
                            " for " + serverName1);
                    admintoken = getToken(adminUser, adminPassword, basedn);
                    SMSCommon smsc = new SMSCommon(admintoken, "config" +
                            fileseparator + "default" + fileseparator +
                            "UMGlobalConfig");
                    smsc.deleteCreatedDataStores(realm, 1);
                    modifyPolicyService(smsc, serverName1);

                    // Unconfigure other two servers (index 2 and 3 in
                    // resources/config/UMGlobalDatstoreConfig only if
                    // multiprotocol is enabled. This flag is set in the server
                    // configuration file for server index 0.                
                    if (cfg0.getString(TestConstants.
                            KEY_ATT_MULTIPROTOCOL_ENABLED).
                            equalsIgnoreCase("true")) {
                        String serverName2 = cfg0.getString(TestConstants.
                                KEY_ATT_IDFF_SP);
                        String serverName3 = cfg0.getString(TestConstants.
                                KEY_ATT_WSFED_SP);
                        ResourceBundle cfg2 = ResourceBundle.getBundle(
                                "Configurator-" + serverName2);
                        ResourceBundle cfg3 = ResourceBundle.getBundle(
                                "Configurator-" + serverName3);

                        // Initiating cleanup for server index 2. This is with
                        // refrence to definitions in resources/config/
                        // UMGlobalDatstoreConfig resource bundle. This is done
                        // using famadm.jsp.                    
                        log(Level.FINE, "CleanupProduct", "Initiating cleanup" +
                                " for " +  serverName2);
                        try {
                            String strURL = cfg2.
                                    getString(TestConstants.KEY_AMC_NAMING_URL);
                            log(Level.FINE, "CleanupProduct", "Server URL: " +
                                    strURL);
                            Map map = getURLComponents(strURL);
                            log(Level.FINE, "CleanupProduct", "Server URL" +
                                    " Components: " +  map);

                            namingProtocol = (String)map.get("protocol");
                            namingHost = (String)map.get("host");
                            namingPort = (String)map.get("port");
                            namingURI = (String)map.get("uri");

                            String loginURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI +
                                    "/UI/Login";
                            logoutURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI +
                                    "/UI/Logout";

                            String famadmURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI;

                             FederationManager famadm =
                                     new FederationManager(famadmURL);
                            consoleLogin(webClient, loginURL, adminUser,
                                    adminPassword);

                            if (FederationManager.getExitCode(
                                    famadm.deleteDatastores(
                                    webClient, realm, 
                                    smsGbl.getCreatedDatastoreNames(2))) != 0) {
                                log(Level.SEVERE, "ClenaupProduct",
                                        "deleteDatastores famadm command" +
                                        " failed");
                                assert false;
                            }
                        } catch (Exception e) {
                            log(Level.SEVERE, "CleanupProduct", e.getMessage());
                            e.printStackTrace();
                        } finally {
                            consoleLogout(webClient, logoutURL);
                        }

                        // Initiating cleanup for server index 3. This is with
                        // refrenceto definitions in resources/config/
                        // UMGlobalDatstoreConfig resource bundle. This is done
                        // using famadm.jsp.                
                        log(Level.FINE, "CleanupProduct", "Initiating cleanup" +
                                " for " + serverName3);
                        try {
                            String strURL = cfg3.
                                    getString(TestConstants.KEY_AMC_NAMING_URL);
                            log(Level.FINE, "CleanupProduct", "Server URL: " +
                                    strURL);
                            Map map = getURLComponents(strURL);
                            log(Level.FINE, "CleanupProduct", "Server URL" +
                                    " Components: " +  map);

                            namingProtocol = (String)map.get("protocol");
                            namingHost = (String)map.get("host");
                            namingPort = (String)map.get("port");
                            namingURI = (String)map.get("uri");

                            String loginURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI +
                                    "/UI/Login";
                            logoutURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI +
                                    "/UI/Logout";

                            String famadmURL = namingProtocol + ":" + "//" +
                                    namingHost + ":" + namingPort + namingURI;

                            FederationManager famadm =
                                    new FederationManager(famadmURL);
                            consoleLogin(webClient, loginURL, adminUser,
                                    adminPassword);

                             if (FederationManager.getExitCode(
                             famadm.deleteDatastores(webClient, realm,
                             smsGbl.getCreatedDatastoreNames(3)))
                             != 0) {
                                 log(Level.SEVERE, "ClenaupProduct", 
                                         "deleteDatastores famadm command" +
                                         " failed");
                                 assert false;
                             }
                        } catch (Exception e) {
                            log(Level.SEVERE, "CleanupProduct", e.getMessage());
                            e.printStackTrace();
                        } finally {
                            consoleLogout(webClient, logoutURL);
                        }
                    }
                } else
                    log(Level.INFO, "CleanupProduct", "Nothing to delete");

            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. This loop is executed only if
            // SERVER_NAME1 is sepcified. This setup refers to single server 
            // tests only.                
            } else if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                    (serverName1.indexOf("SERVER_NAME2") != -1)) {
                log(Level.FINE, "CleanupProduct", "Initiating cleanup" +
                            " for " + serverName0);
                admintoken = getToken(adminUser, adminPassword, basedn);
                SMSCommon smsc = new SMSCommon(admintoken, "config" +
                        fileseparator + "default" + fileseparator +
                        "UMGlobalConfig");
                if ((gblCfgData.getString(
                        "UMGlobalConfig.deleteCreatedDatastores")).
                        equals("true")) {
                    smsc.deleteCreatedDataStores(realm, 1);
                    modifyPolicyService(smsc, serverName0);
                } else
                    log(Level.INFO, "CleanupProduct", "Nothing to delete");
            }
        } catch(Exception e) {
            log(Level.SEVERE, "ClenanupProduct", e.getMessage());
            e.printStackTrace();
        } finally {          
            destroyToken(admintoken);
        }
    }

    /**
     * This method modifies Policy Service using SMS API. LDAP server name,
     * port, bind bn, bind password, users base dn, roles base dn, ssl enabled
     * will be modified if User Management datastore is set to a remote
     * directory. The data values are picked from
     * resources/config/UMGlobalDatastoreConfig resource bundle.
     */
    private void modifyPolicyService(SMSCommon smsc, String serverName)
    throws Exception {
        entering("modifyPolicyService", null);

        String dsRealm = null;
        String ldapServer;
        String ldapPort;
        String dsAdminPassword;
        String orgName;
        String sslEnabled;
        String authId;
        String umDSType;

        ResourceBundle cfgData = ResourceBundle.getBundle("Configurator-" +
                serverName);

        umDSType = cfgData.getString("umdatastore");

        try {
            if ((umDSType).equals("dirServer")) {
                dsAdminPassword = cfgData.getString("ds_dirmgrpasswd");
            } else
                dsAdminPassword = cfgData.getString("amadmin_password");
                dsRealm = TestCommon.realm;
                ldapServer = cfgData.getString("directory_server");
                ldapPort = cfgData.getString("directory_port");
                orgName = cfgData.getString("config_root_suffix");
                sslEnabled = "false";
                authId = cfgData.getString("ds_dirmgrdn");

                Map scAttrMap =
                        smsc.getAttributes("iPlanetAMPolicyConfigService",
                        dsRealm, "Organization");
                log(Level.FINEST, "modifyPolicyService",
                        "Policy service attributes before modification: " +
                        scAttrMap.toString());

                Map newPolicyAttrValuesMap = new HashMap();
                Set newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(ldapServer + ":" + ldapPort);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-server",
                        newPolicyAttrValues);
                newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(dsAdminPassword);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-bind-password",
                        newPolicyAttrValues);
                newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(authId);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-bind-dn",
                        newPolicyAttrValues);
                newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(sslEnabled);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-ssl-enabled",
                        newPolicyAttrValues);
                newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(orgName);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-base-dn",
                        newPolicyAttrValues);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-is-roles-base-dn",
                        newPolicyAttrValues);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-users-base-dn",
                        newPolicyAttrValues);

                log(Level.FINEST, "modifyPolicyService",
                        "Policy attributes being modified: " +
                        newPolicyAttrValuesMap);
                smsc.updateSvcAttribute(dsRealm,
                        "iPlanetAMPolicyConfigService", newPolicyAttrValuesMap,
                        "Organization");

                Map scAttrMapNew =
                        smsc.getAttributes("iPlanetAMPolicyConfigService",
                        dsRealm, "Organization");
                log(Level.FINEST, "modifyPolicyService",
                        "Policy service attributes after modification: "
                        + scAttrMapNew.toString());
        } catch (Exception e) {
            log(Level.SEVERE, "modifyPolicyService", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("modifyPolicyService");
    }

    public static void main(String args[]) {
        try {
            CleanupProduct cp = new CleanupProduct(args[0], args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
