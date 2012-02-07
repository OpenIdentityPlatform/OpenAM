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
 * $Id: SetupProduct.java,v 1.23 2009/07/02 18:48:12 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.setup;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.LDAPCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This class configures the product. This means calling the configurator to
 * configure the deployed war and configure the datastores. If product is 
 * is already configured, only datastores are configured, depending upon flags
 * set in resource file.
 * 
 * This class is called from xml/opensso-common.xml
 */
public class SetupProduct extends TestCommon {
    
    private String FILE_CLIENT_PROPERTIES = "AMConfig.properties";
    private Map properties = new HashMap();
    private SSOToken admintoken;
    List list;
    private static final Map EMPTY_MAP = Collections
            .unmodifiableMap(new HashMap());
    
    /**
     * This method configures the deployed war and datastores for different
     * servers.     
     */
    public SetupProduct(String serverName0, String serverName1)
    throws Exception {
        super("SetupProduct");
        
        try {
            boolean bserver0 = false;
            boolean bserver1 = false;
            boolean bserver2 = false;
            boolean bserver3 = false;

            String namingProtocol0 = "";
            String namingHost0 = "";
            String namingPort0 = "";
            String namingURI0 = "";

            String namingProtocol1 = "";
            String namingHost1 = "";
            String namingPort1 = "";
            String namingURI1 = "";

            String namingProtocol2 = "";
            String namingHost2 = "";
            String namingPort2 = "";
            String namingURI2 = "";

            String namingProtocol3 = "";
            String namingHost3 = "";
            String namingPort3 = "";
            String namingURI3 = "";

            String serverName2 = null;
            String serverName3 = null;

            ResourceBundle cfg0 = null;
            ResourceBundle cfg1 = null;
            ResourceBundle cfg2 = null;
            ResourceBundle cfg3 = null;
            ResourceBundle cfgData = null;
           
            SMSCommon smscSS = null;

            Map<String, String> umDatastoreTypes = new HashMap<String,
                    String>();
            boolean bMulti = false;

            ResourceBundle gblCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "default" + fileseparator +
                    "UMGlobalConfig");
            SMSCommon smscGbl = new SMSCommon("config" + fileseparator +
                    "default" + fileseparator + "UMGlobalConfig");

            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. This loop is executed only if
            // both entries are sepcified.
            if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                    (serverName1.indexOf("SERVER_NAME2") == -1)) {


                if (serverName0.indexOf(".") != -1) {
                    log(Level.SEVERE, "SetupProduct", "Server configuration " +
                            "file Configurator-" + serverName0 + ".properties" +
                            " cannot have \".\" in its name");
                    assert false;
                }
                if (serverName1.indexOf(".") != -1) {
                    log(Level.SEVERE, "SetupProduct", "Server configuration " +
                            "file Configurator-" + serverName1 + ".properties" +
                            " cannot have \".\" in its name");
                    assert false;
                }

                Map<String, String> configMapServer0 = new HashMap<String,
                        String>();
                configMapServer0 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                configMapServer0.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName0));
                createFileFromMap(configMapServer0, serverName + fileseparator +
                        "built" + fileseparator + "classes" + fileseparator +
                        "Configurator-" + serverName0 +
                        "-Generated.properties");

                cfg0 = ResourceBundle.getBundle("Configurator-" + serverName0 +
                        "-Generated");

                Map<String, String> configMapServer1 = new HashMap<String,
                        String>();
                configMapServer1 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                configMapServer1.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName1));
                createFileFromMap(configMapServer1, serverName + fileseparator +
                        "built" + fileseparator + "classes" + fileseparator +
                        "Configurator-" + serverName1 +
                        "-Generated.properties");

                cfg1 = ResourceBundle.getBundle("Configurator-" + serverName1 +
                        "-Generated");
                
                log(Level.FINE, "SetupProduct", "Multiprotocol " +
                        "flag is set to: " +
                        cfg0.getString(
                        TestConstants.KEY_ATT_MULTIPROTOCOL_ENABLED));

                if (cfg0.getString(TestConstants.KEY_ATT_MULTIPROTOCOL_ENABLED).
                        equalsIgnoreCase("true")) {
                    bMulti = true;

                    serverName2 = cfg0.getString(TestConstants.
                            KEY_ATT_IDFF_SP);
                    serverName3 = cfg0.getString(TestConstants.
                            KEY_ATT_WSFED_SP);

                    if (serverName2.indexOf(".") != -1) {
                        log(Level.SEVERE, "SetupProduct", "Server" +
                                " configuration file Configurator-" +
                                serverName2 + ".properties cannot have" +
                                " \".\" in its name");
                        assert false;
                    }
                    if (serverName3.indexOf(".") != -1) {
                        log(Level.SEVERE, "SetupProduct", "Server" +
                                " configuration file Configurator-" +
                                serverName3 + ".properties cannot have" +
                                " \".\" in its name");
                        assert false;
                    }

                    Map<String, String> configMapServer2 = new HashMap<String,
                            String>();
                    configMapServer2 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                    configMapServer2.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName2));
                    createFileFromMap(configMapServer2, serverName +
                            fileseparator + "built" + fileseparator +
                            "classes" + fileseparator + "Configurator-" +
                            serverName2 + "-Generated.properties");

                    cfg2 = ResourceBundle.getBundle("Configurator-" +
                            serverName2 + "-Generated");

                    Map<String, String> configMapServer3 = new HashMap<String,
                            String>();
                    configMapServer3 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                    configMapServer3.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName3));
                    createFileFromMap(configMapServer3, serverName +
                            fileseparator + "built" + fileseparator +
                            "classes" + fileseparator + "Configurator-" +
                            serverName3 + "-Generated.properties");

                    cfg3 = ResourceBundle.getBundle("Configurator-" +
                            serverName3 + "-Generated");
                }
                
                // Initiating setp for server index 0. This is with refrence to
                // definitions in resources/config/UMGlobalDatstoreConfig 
                // resource bundle. This is done using famadm.jsp.
                log(Level.FINE, "SetupProduct", "Initiating setup for " +
                        serverName0);                
                String strURL = cfg0.
                        getString(TestConstants.KEY_AMC_NAMING_URL);
                log(Level.FINE, "SetupProduct", "Server URL: " + strURL);
                Map map = getURLComponents(strURL);
                log(Level.FINE, "SetupProduct", "Server URL Components: " +
                        map);
                namingProtocol0 = (String)map.get("protocol");
                namingHost0 = (String)map.get("host");
                namingPort0 = (String)map.get("port");
                namingURI0 = (String)map.get("uri");
                
                list = new ArrayList();
                
                bserver0 = configureProduct(getConfigurationMap("Configurator-"
                        + serverName0 + "-Generated", namingProtocol0,
                        namingHost0, namingPort0, namingURI0), "0");
                if (!bserver0) {
                    log(Level.FINE, "SetupProduct",
                            "Configuration failed for " + serverName0);
                    assert false;
                }

                // Initiating setp for server index 1. This is with refrence to
                // definitions in resources/config/UMGlobalDatstoreConfig 
                // resource bundle. This is for single server tests and uses
                // client api's to do all the configuration.
                log(Level.FINE, "SetupProduct", "Initiating setup for " +
                        serverName1);
                bserver1 = configureProduct(
                        getConfigurationMap("Configurator-" + serverName1 +
                        "-Generated"), "1");
                if (!bserver1) {
                    log(Level.FINE, "SetupProduct", "Configuration failed" +
                            " for " + serverName1);
                    assert false;
                }

                if (bMulti) {
                    //configure multiple sp's
                    log(Level.FINE, "SetupProduct", "Initiating setup for " +
                            "multiple SP's");

                    // Initiating setp for server index 2. This is with refrence
                    // to definitions in resources/config/UMGlobalDatstoreConfig
                    // resource bundle. This is done using famadm.jsp.
                    log(Level.FINE, "SetupProduct", "Initiating setup for " +
                            serverName2);
                    cfg2 = ResourceBundle.
                            getBundle("Configurator-" + serverName2 +
                            "-Generated");
                    strURL = cfg2.
                            getString(TestConstants.KEY_AMC_NAMING_URL);
                    log(Level.FINE, "SetupProduct", "Server URL: " + strURL);
                    map = getURLComponents(strURL);
                    log(Level.FINE, "SetupProduct", "Server URL Components: " +
                            map);
                    namingProtocol2 = (String)map.get("protocol");
                    namingHost2 = (String)map.get("host");
                    namingPort2 = (String)map.get("port");
                    namingURI2 = (String)map.get("uri");

                    bserver2 = configureProduct(
                            getConfigurationMap("Configurator-" + serverName2 +
                            "-Generated", namingProtocol2, namingHost2,
                            namingPort2, namingURI2), "2");
                    if (!bserver2) {
                        log(Level.FINE, "SetupProduct", "Configuration failed" +
                                " for " + serverName2);
                        assert false;
                    }

                    // Initiating setp for server index 3. This is with refrence
                    // to definitions in resources/config/UMGlobalDatstoreConfig
                    // resource bundle. This is done using famadm.jsp.
                    log(Level.FINE, "SetupProduct", "Initiating setup for " +
                            serverName3);
                    cfg3 = ResourceBundle.getBundle("Configurator-" +
                            serverName3 + "-Generated");
                    strURL = cfg3.
                            getString(TestConstants.KEY_AMC_NAMING_URL);
                    log(Level.FINE, "SetupProduct", "Server URL: " + strURL);
                    map = getURLComponents(strURL);
                    log(Level.FINE, "SetupProduct", "Server URL Components:" +
                            " " +  map);
                    namingProtocol3 = (String)map.get("protocol");
                    namingHost3 = (String)map.get("host");
                    namingPort3 = (String)map.get("port");
                    namingURI3 = (String)map.get("uri");

                    bserver3 = configureProduct(
                            getConfigurationMap("Configurator-" + serverName3 +
                            "-Generated",
                            namingProtocol3, namingHost3, namingPort3,
                            namingURI3), "3");
                    if (!bserver3) {
                        log(Level.FINE, "SetupProduct", "Configuration failed" +
                                " for " + serverName3);
                        assert false;
                    }
                }

                WebClient webClient0 = null;
                WebClient webClient2 = null;
                WebClient webClient3 = null;
                
                FederationManager famadm0 = null;
                FederationManager famadm2 = null;
                FederationManager famadm3 = null;
                
                String logoutURL0 = null;
                String logoutURL2 = null;
                String logoutURL3 = null;
                
                if (bserver0) {
                    if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true")) {
                        String adminUser0 = cfg0.getString(
                                TestConstants.KEY_ATT_AMADMIN_USER);
                        String adminPassword0 = cfg0.getString(
                                TestConstants.KEY_ATT_AMADMIN_PASSWORD);
                        String loginURL0 = namingProtocol0 + ":" + "//" + 
                                namingHost0 + ":" + namingPort0 + namingURI0 + 
                                "/UI/Login";
                        logoutURL0 = namingProtocol0 + ":" + "//" + 
                                namingHost0 + ":" + namingPort0 + namingURI0 + 
                                "/UI/Logout";
                        String famadmURL0 = namingProtocol0 + ":" + "//" + 
                                namingHost0 + ":" + namingPort0 + namingURI0;
                        famadm0 =  new FederationManager(famadmURL0);
                        webClient0 = new WebClient();
                        consoleLogin(webClient0, loginURL0, adminUser0,
                                adminPassword0);
                        Map mCfgData0 =  getSvrcfgDetails(famadm0, webClient0,
                                namingProtocol0 + ":" + "//" + namingHost0 +
                                ":" + namingPort0 + namingURI0);
                        umDatastoreTypes.put("0", serverName0);
                        umDatastoreTypes.put("0." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                (String)mCfgData0.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER));
                        umDatastoreTypes.put("0." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT,
                                (String)mCfgData0.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT));
                        umDatastoreTypes.put("0." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                (String)mCfgData0.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                        umDatastoreTypes.put("0." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID,
                                (String)mCfgData0.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID));
                    }
                }

                if (bserver1) {
                    if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true")) {
                        String namingURL = cfg1.getString(KEY_AMC_NAMING_URL);
                        Map namingURLMap = getURLComponents(namingURL);

                        namingProtocol1 = (String) namingURLMap.get("protocol");
                        namingHost1 = (String) namingURLMap.get("host");
                        namingPort1 = (String) namingURLMap.get("port");
                        namingURI1 = (String) namingURLMap.get("uri");

                            log(Level.FINE, "SetupProduct", "UM Datastore for " +
                                    serverName1 + " is " +
                                    cfg1.getString("umdatastore"));

                            admintoken = getToken(adminUser, adminPassword,
                                    basedn);
                            smscSS = new SMSCommon(admintoken, "config" +
                                    fileseparator + "default" + fileseparator +
                                    "UMGlobalConfig");
                            String url = namingProtocol1 + "://" + namingHost1
                                    + ":" + namingPort1 + namingURI1 ;
                            log(Level.FINEST, "SetuProduct",
                                    "serverconfig.xml details for " +
                                    namingURL + ":  " +
                                    (smscSS.getServerConfigData(url)).
                                    toString());
                            Map mCfgData1 = smscSS.getServerConfigData(url);
                            umDatastoreTypes.put("1", serverName0);
                            umDatastoreTypes.put("1." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                    (String)mCfgData1.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER));
                            umDatastoreTypes.put("1." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT,
                                    (String)mCfgData1.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT));
                            umDatastoreTypes.put("1." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + 
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                    (String)mCfgData1.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                            umDatastoreTypes.put("1." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID,
                                    (String)mCfgData1.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID));
                    }
                }

                if (bMulti) {
                    if (bserver2) {
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "createNewDatastores")).equals("true")) {
                            String adminUser2 = cfg2.getString(
                                    TestConstants.KEY_ATT_AMADMIN_USER);
                            String adminPassword2 = cfg2.getString(
                                    TestConstants.KEY_ATT_AMADMIN_PASSWORD);

                            String loginURL2 = namingProtocol2 + ":" + "//" +
                                    namingHost2 + ":" + namingPort2 +
                                    namingURI2 + "/UI/Login";
                            logoutURL2 = namingProtocol2 + ":" + "//" +
                                    namingHost2 + ":" + namingPort2 +
                                    namingURI2 + "/UI/Logout";
                            String famadmURL2 = namingProtocol2 + ":" + "//" +
                                    namingHost2 + ":" + namingPort2 +
                                    namingURI2;
                            famadm2 = new FederationManager(famadmURL2);
                            webClient2 = new WebClient();
                            consoleLogin(webClient2, loginURL2, adminUser2,
                                    adminPassword2);
                            Map mCfgData2 =  getSvrcfgDetails(famadm2,
                                    webClient2, namingProtocol2 + ":" + "//" +
                                    namingHost2 + ":" + namingPort2 +
                                    namingURI2);
                            umDatastoreTypes.put("2", serverName2);
                            umDatastoreTypes.put("2." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                    (String)mCfgData2.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER));
                            umDatastoreTypes.put("2." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT,
                                    (String)mCfgData2.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT));
                            umDatastoreTypes.put("2." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                    (String)mCfgData2.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                            umDatastoreTypes.put("2." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID,
                                    (String)mCfgData2.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID));
                        }
                    }

                    if (bserver3) {
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "createNewDatastores")).equals("true")) {
                            String adminUser3 = cfg3.getString(
                                    TestConstants.KEY_ATT_AMADMIN_USER);
                            String adminPassword3 = cfg3.getString(
                                    TestConstants.KEY_ATT_AMADMIN_PASSWORD);

                            String loginURL3 = namingProtocol3 + ":" + "//" +
                                    namingHost3 + ":" + namingPort3 +
                                    namingURI3 + "/UI/Login";
                            logoutURL3 = namingProtocol3 + ":" + "//" +
                                    namingHost3 + ":" + namingPort3 +
                                    namingURI3 + "/UI/Logout";
                            String famadmURL3 = namingProtocol3 + ":" + "//" +
                                    namingHost3 + ":" + namingPort3 +
                                    namingURI3;

                            WebClient webClient = null;
                            famadm3 = new FederationManager(famadmURL3);
                            webClient3 = new WebClient();
                            consoleLogin(webClient3, loginURL3, adminUser3,
                                    adminPassword3);
                            Map mCfgData3 =  getSvrcfgDetails(famadm3,
                                    webClient3, namingProtocol3 + ":" + "//" +
                                    namingHost3 + ":" + namingPort3 +
                                    namingURI3);
                            umDatastoreTypes.put("3", serverName3);
                            umDatastoreTypes.put("3." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                    (String)mCfgData3.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER));
                            umDatastoreTypes.put("3." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT,
                                    (String)mCfgData3.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT));
                            umDatastoreTypes.put("3." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                    (String)mCfgData3.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                            umDatastoreTypes.put("3." +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID,
                                    (String)mCfgData3.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID));
                        }
                    }
                }

                if (bMulti)
                    createGlobalDatastoreFile(umDatastoreTypes,
                            SMSConstants.QATEST_EXEC_MODE_ALL);
                else
                    createGlobalDatastoreFile(umDatastoreTypes,
                            SMSConstants.QATEST_EXEC_MODE_DUAL);

                if (bserver0) {
                    if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true")) {
                        try {
                            boolean bDSCreate = false;
                            if (cfg0.getString(
                                    TestConstants.KEY_ATT_CONFIG_UMDATASTORE).
                                    equals("dirServer")) {
                            cfgData = ResourceBundle.getBundle("config" +
                                    fileseparator +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "-Generated");
                            int dCount = new Integer(cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "0." + SMSConstants.UM_DATASTORE_COUNT)).
                                    intValue();

                                    bDSCreate = createDataStoreUsingfamadm(
                                            webClient0, famadm0, cfgData, 0,
                                            dCount);
                             } else {
                                    bDSCreate = createDataStoreUsingfamadm(
                                            webClient0, famadm0, cfg0,
                                            umDatastoreTypes, 0);
                             }
                            if (bDSCreate) {
                                HtmlPage pageDStore =
                                        famadm0.listDatastores(webClient0,
                                        realm);
                                if (FederationManager.
                                        getExitCode(pageDStore) != 0) {
                                    log(Level.SEVERE, "SetupProduct",
                                            "listDatastores famadm" +
                                            " command failed");
                                    assert false;
                                }
                                if ((gblCfgData.getString("UMGlobalConfig." +
                                        "deleteExistingDatastores")).
                                        equals("true")) {
                                    List datastoreList = smscGbl.
                                            getDatastoreDeleteList(
                                            getListFromHtmlPage(pageDStore), 0);
                                    if (datastoreList.size() != 0) {
                                        if (FederationManager.getExitCode(
                                                famadm0.deleteDatastores(
                                                webClient0, realm,
                                                datastoreList)) != 0) {
                                            log(Level.SEVERE, "SetupProduct",
                                                    "deleteDatastores famadm" +
                                                    " command failed");
                                            assert false;
                                        }
                                    }
                                }
                            } else {
                                log(Level.SEVERE, "SetupProduct", "DataStore" +
                                    " configuration didn't succeed for " +
                                    namingHost0);
                            }
                        } catch (Exception e) {
                            log(Level.SEVERE, "SetupProduct", e.getMessage());
                            e.printStackTrace();
                        } finally {
                            consoleLogout(webClient0, logoutURL0);
                        }
                    }
                }
                
                if (bserver1) {
                    String namingURL = cfg1.getString(KEY_AMC_NAMING_URL);
                    Map namingURLMap = getURLComponents(namingURL);

                    namingProtocol1 = (String) namingURLMap.get("protocol");
                    namingHost1 = (String) namingURLMap.get("host");
                    namingPort1 = (String) namingURLMap.get("port");
                    namingURI1 = (String) namingURLMap.get("uri");

                    if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true")) {
                        smscSS.createDataStore(1, "config" + fileseparator +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "-Generated");
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "deleteExistingDatastores")).equals("true"))
                            smscSS.deleteAllDataStores(realm, 1);
                        if ((cfg1.getString("umdatastore")).equals("embedded"))
                        {
                            String url = namingProtocol1 + "://" + namingHost1 +
                                    ":" + namingPort1 + namingURI1 ;
                            Map dmap = smscSS.getServerConfigData(url);
                            cfgData = ResourceBundle.getBundle("config" +
                                    fileseparator +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "-Generated");
                            dmap.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID,
                                    cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "1." + SMSConstants.UM_LDAPv3_AUTHID +
                                    ".0"));
                            dmap.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHPW,
                                    cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "1." + SMSConstants.UM_LDAPv3_AUTHPW +
                                    ".0"));
                            modifyAuthConfigproperties(dmap);
                        }
                        modifyPolicyService(smscSS, serverName0, 1, 0);
                    }
                }
                
                if (bMulti) {
                    if (bserver2) {
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "createNewDatastores")).equals("true")) {
                         try {
                             boolean bDSCreate = false;
                             if (cfg2.getString(
                                    TestConstants.KEY_ATT_CONFIG_UMDATASTORE).
                                    equals("dirServer")) {
                                 int dCount = new Integer(cfgData.getString(
                                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                                        + "2." +
                                        SMSConstants.UM_DATASTORE_COUNT)).
                                        intValue();
                                 bDSCreate = createDataStoreUsingfamadm(
                                            webClient2, famadm2, cfgData, 2,
                                            dCount);
                             } else {
                                    bDSCreate = createDataStoreUsingfamadm(
                                            webClient2, famadm2, cfg2,
                                            umDatastoreTypes, 2);
                             }
                             if (bDSCreate) {
                                 HtmlPage pageDStore =
                                        famadm2.listDatastores(webClient2,
                                        realm);
                                 if (FederationManager.
                                    getExitCode(pageDStore) != 0) {
                                        log(Level.SEVERE, "SetupProduct",
                                                "listDatastores famadm" +
                                                " command failed");
                                        assert false;
                                 }
                                 if ((gblCfgData.getString("UMGlobalConfig." +
                                        "deleteExistingDatastores")).
                                        equals("true")) {
                                    List datastoreList = 
                                            smscGbl.getDatastoreDeleteList(
                                            getListFromHtmlPage(pageDStore), 2);
                                    if (datastoreList.size() != 0) {
                                        if (FederationManager.getExitCode(
                                                famadm2.deleteDatastores(
                                                webClient2, realm,
                                                datastoreList)) != 0) {
                                            log(Level.SEVERE, "SetupProduct",
                                                    "deleteDatastores famadm" +
                                                    " command failed");
                                            assert false;
                                        }
                                    }
                                }
                            } else
                                log(Level.SEVERE, "SetupProduct", "DataStore" +
                                    " configuration didn't succeed for " + 
                                    namingHost2);
                            } catch (Exception e) {
                                log(Level.SEVERE, "SetupProduct",
                                        e.getMessage());
                                e.printStackTrace();
                            } finally {
                                consoleLogout(webClient2, logoutURL2);
                            }
                        }
                    }
                    
                    if (bserver3) {
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "createNewDatastores")).equals("true")) {
                            try {
                                boolean bDSCreate = false;
                                if (cfg3.getString(TestConstants.
                                        KEY_ATT_CONFIG_UMDATASTORE).
                                        equals("dirServer")) {
                                    int dCount = new Integer(cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "3." +
                                    SMSConstants.UM_DATASTORE_COUNT)).
                                    intValue();
                                    bDSCreate = createDataStoreUsingfamadm(
                                            webClient3, famadm3, cfgData, 3,
                                            dCount);
                                } else {
                                    bDSCreate = createDataStoreUsingfamadm(
                                            webClient3, famadm3, cfg3,
                                            umDatastoreTypes, 3);
                                }
                                if (bDSCreate) {
                                    HtmlPage pageDStore =
                                            famadm3.listDatastores(webClient3,
                                            realm);
                                    if (FederationManager.
                                        getExitCode(pageDStore) != 0) {
                                        log(Level.SEVERE, "SetupProduct",
                                            "listDatastores famadm command" +
                                            " failed");
                                        assert false;
                                    }
                                    if ((gblCfgData.getString(
                                            "UMGlobalConfig." +
                                        "deleteExistingDatastores")).
                                        equals("true")) {
                                        List datastoreList = 
                                            smscGbl.getDatastoreDeleteList(
                                            getListFromHtmlPage(pageDStore), 3);
                                        if (datastoreList.size() != 0) {
                                            if (FederationManager.getExitCode(
                                                famadm3.deleteDatastores(
                                                webClient3, realm,
                                                datastoreList))
                                                != 0) {
                                                log(Level.SEVERE,
                                                        "SetupProduct",
                                                        "deleteDatastores" +
                                                        " famadm command" +
                                                        " failed");
                                                assert false;
                                            }
                                        }
                                    }
                                } else
                                    log(Level.SEVERE, "SetupProduct",
                                            "DataStore configuration didn't" +
                                            " succeed for " +  namingHost3);
                            } catch (Exception e) {
                                log(Level.SEVERE, "SetupProduct",
                                        e.getMessage());
                                e.printStackTrace();
                            } finally {
                                consoleLogout(webClient3, logoutURL3);
                            }
                        }
                    }
                }
            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. This loop is executed only if
            // SERVER_NAME1 is sepcified. This setup refers to single server 
            // tests only.
            } else if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                    (serverName1.indexOf("SERVER_NAME2") != -1)) {

                if (serverName0.indexOf(".") != -1) {
                    log(Level.SEVERE, "SetupProduct", "Server configuration " +
                        "file Configurator-" + serverName0 + ".properties" +
                        " cannot have \".\" in its name");
                    assert false;
                }

                Map<String, String> configMapServer1 = new HashMap<String,
                        String>();
                configMapServer1 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                configMapServer1.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName0));
                createFileFromMap(configMapServer1, serverName + fileseparator +
                        "built" + fileseparator + "classes" + fileseparator +
                        "Configurator-" + serverName0 +
                        "-Generated.properties");
                
                cfg1 = ResourceBundle.getBundle("Configurator-" +
                        serverName0 + "-Generated");
                
                String namingURL = cfg1.getString(KEY_AMC_NAMING_URL);
                Map namingURLMap = getURLComponents(namingURL);
                
                namingProtocol1 = (String) namingURLMap.get("protocol");
                namingHost1 = (String) namingURLMap.get("host");
                namingPort1 = (String) namingURLMap.get("port");
                namingURI1 = (String) namingURLMap.get("uri");
                
                // Initiating setp for server index 1. This is with refrence
                // to definitions in resources/config/UMGlobalDatstoreConfig 
                // resource bundle. This is done using client api's.
                log(Level.FINE, "SetupProduct", "Initiating setup for " +
                        serverName0);   
                
                bserver1 = configureProduct(getConfigurationMap("Configurator-"
                        + serverName0 + "-Generated", namingProtocol1,
                        namingHost1, namingPort1, namingURI1), "1");
                if (!bserver1) {
                    log(Level.FINE, "SetupProduct", "Configuration failed for" +
                            " " + serverName0);
                    setSingleServerSetupFailedFlag();
                    assert false;
                } else {
                    if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true")) {
                        admintoken = getToken(adminUser, adminPassword, basedn);
                        smscSS = new SMSCommon(admintoken, "config" +
                                fileseparator + "default" + fileseparator +
                                "UMGlobalConfig");
                        String url = namingProtocol1 + "://" + namingHost1 +
                                ":" +  namingPort1 + namingURI1 ;
                        log(Level.FINEST, "SetuProduct",
                                "serverconfig.xml details for " + url + ":  " +
                                (smscSS.getServerConfigData(url)).toString());
                        Map mCfgData = smscSS.getServerConfigData(url);
                        umDatastoreTypes.put("1", serverName0);
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID));
                        createGlobalDatastoreFile(umDatastoreTypes,
                                SMSConstants.QATEST_EXEC_MODE_SINGLE);
                        smscSS.createDataStore(1, "config" + fileseparator +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "-Generated");
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "deleteExistingDatastores")).equals("true"))
                            smscSS.deleteAllDataStores(realm, 1);
                        if ((cfg1.getString("umdatastore")).equals("embedded"))
                        {
                            url = namingProtocol1 + "://" + namingHost1 + ":" +
                                    namingPort1 + namingURI1 ;
                            Map dmap = smscSS.getServerConfigData(url);
                            cfgData = ResourceBundle.getBundle("config" +
                                    fileseparator +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "-Generated");
                            dmap.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID,
                                    cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "1." + SMSConstants.UM_LDAPv3_AUTHID +
                                    ".0"));
                            dmap.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHPW,
                                    cfgData.getString(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "1." + SMSConstants.UM_LDAPv3_AUTHPW +
                                    ".0"));
                            modifyAuthConfigproperties(dmap);
                        }
                        modifyPolicyService(smscSS, serverName0, 1, 0);
                    }
                }
            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. If only SERVER_NAME2 is 
            // specified, its an error conditions.
            } else if ((serverName0.indexOf("SERVER_NAME1") != -1) &&
                    (serverName1.indexOf("SERVER_NAME2") == -1)) {
                log(Level.FINE, "SetupProduct", "Unsupported configuration." +
                        " Cannot have SERVER_NAME2 specified without" +
                        " SERVER_NAME1.");
                assert false;
            }

            if (distAuthEnabled) {
                String strServiceName = "iPlanetAMAuthService";
                ServiceConfigManager scm = new ServiceConfigManager(admintoken, 
                        strServiceName, "1.0");
                log(Level.FINEST, "SetupProduct", "get ServiceConfig");
                ServiceConfig sc = scm.getOrganizationConfig(realm, null);
                Map scAttrMap = sc.getAttributes();
                log(Level.FINEST, "SetupProduct", "Map " +
                        "returned from Org config is: " + scAttrMap);
                Set oriAuthAttrValues = null;
                oriAuthAttrValues = (Set) scAttrMap.get
                        ("iplanet-am-auth-login-success-url");
                log(Level.FINEST, "SetupProduct", "Value of " +
                        "iplanet-am-auth-login-success-url: " + 
                        oriAuthAttrValues);
                Set newAuthValues = new HashSet();
                newAuthValues.add(namingProtocol1 + "://" + namingHost1 + ":" + 
                        namingPort1 + namingURI1 + "/console");
                Map newAuthValuesMap = new HashMap();
                newAuthValuesMap.put("iplanet-am-auth-login-success-url", 
                        newAuthValues);
                log(Level.FINEST, "AuthServiceOrgModificationTest", "Set " +
                        "iplanet-am-auth-login-success-url to " + 
                        newAuthValuesMap);
                sc.setAttributes(newAuthValuesMap);
            }                        
        } catch(Exception e) {
            log(Level.SEVERE, "SetupProduct", e.getMessage());
            e.printStackTrace();
        } finally {          
            destroyToken(admintoken);
        }
    }

    /**
     * In case product configuration fails, this method rewrites the client
     * side AMConfig.properties file and sets product setup flag to false.
     */
    public void setSingleServerSetupFailedFlag() {
        entering("setSingleServerSetupFailedFlag", null);
        try {
            log(Level.FINE, "setSingleServerSetupFailedFlag", "Single server" +
                    " product configuration failed.");
            Set set = new HashSet();
            set.add((String)TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT);
            properties = getMapFromResourceBundle("AMConfig", null, set);
            properties.put(TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT, "fail");
            createFileFromMap(properties, "resources" + fileseparator +
                    FILE_CLIENT_PROPERTIES);
        } catch(Exception e) {
            log(Level.SEVERE, "setSingleServerSetupFailedFlag", e.getMessage());
            e.printStackTrace();
        }
        exiting("setSingleServerSetupFailedFlag");
    }

    /**
     * This method creates the generated UMGlobalDatastoreConfig resource bundle
     * under resources/config
     * @param serverName
     * @param map
     * @param sIdx
     * @throws java.lang.Exception
     */
    private void createGlobalDatastoreFile(Map map, String sIdx)
    throws Exception {
        SMSCommon smsc = new SMSCommon("config" + fileseparator + "default" +
                fileseparator + "UMGlobalConfig");
        smsc.createUMDatastoreGlobalMap(SMSConstants.UM_DATASTORE_PARAMS_PREFIX,
                map, sIdx);
    }
        
    /**
     * This method creates the datastore using famadm.jsp
     */
    private boolean createDataStoreUsingfamadm(WebClient webClient,
            FederationManager famadm, ResourceBundle cfgData, int
            index, int dCount)
    throws Exception {
        String dsRealm = null;
        String dsType;
        String dsName;
        String adminId;
        String ldapServer;
        String ldapPort;
        String dsAdminPassword;
        String orgName;
        boolean dsCreated = false;
        
        Map stdLDAPv3Data = getMapFromResourceBundle("config" +
                        fileseparator +
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "-Generated",
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + index);

        for (int i = 0; i < dCount; i++) {
            dsCreated = false;
            dsType = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_DATASTORE_TYPE + "." +
                    i);

            dsName = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_DATASTORE_NAME + "." +
                    i);
            adminId = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_DATASTORE_ADMINID +
                    "." + i);
            dsRealm = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_DATASTORE_REALM +
                    "." + i);
            ldapServer = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_LDAPv3_LDAP_SERVER +
                    "." + i);
            ldapPort = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_LDAPv3_LDAP_PORT +
                    "." + i);
            dsAdminPassword = cfgData.getString(
                    SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.UM_DATASTORE_ADMINPW +
                    "." + i);
            orgName = cfgData.getString(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + index + "." +
                    SMSConstants.
                    UM_LDAPv3_ORGANIZATION_NAME + "." + i);
            
            Set s = stdLDAPv3Data.keySet();
            Iterator it = s.iterator();
            String key;
            String newkey = null;
            String value = null;
            while (it.hasNext()) {
                key = (String)it.next();
                if ((key.contains(".sun")) && (key.contains("." + i))) {
                    newkey = key.substring(key.indexOf(index + ".sun") + 2,
                            key.length() - 2);
                value = (String)stdLDAPv3Data.get(key);
                if (value.indexOf("|") != 0) {
                    List locList = getAttributeList(value, "|");
                    for (int j = 0; j < locList.size(); j++) {
                        list.add(newkey + "=" + (String)locList.get(j));
                    }
                } else
                    list.add(newkey + "=" + value);
                }
            }

            list.remove(SMSConstants.UM_LDAPv3_LDAP_SERVER + "=" + ldapServer);
            list.remove(SMSConstants.UM_LDAPv3_LDAP_PORT + "=" + ldapPort);
            list.add(SMSConstants.UM_LDAPv3_LDAP_SERVER + "=" + ldapServer +
                    ":" + ldapPort);


            log(Level.FINEST, "createDataStoreUsingfamadm", "Datastore" +
                    " attributes list:" + list);
            HtmlPage page = famadm.listDatastores(webClient, dsRealm);
            if (FederationManager.getExitCode(page) != 0) {
                log(Level.SEVERE, "createDataStoreUsingfamadm",
                        "listDatastores famadm command failed");
                assert false;
            }

            // Do nothing if a database by the chosen name already exists
	    if (getHtmlPageStringIndex(
                    page, dsName) == -1) {
                LDAPCommon ldc = null;
                ldc = new LDAPCommon(ldapServer, ldapPort, adminId,
                        dsAdminPassword, orgName);
                ResourceBundle smsGblCfg = ResourceBundle.
                        getBundle("config" + fileseparator + "default" +
                        fileseparator + "UMGlobalConfig");
                String schemaString = (String)smsGblCfg.
                        getString(SMSConstants.UM_SCHEMNA_LIST
                        + "." +
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMDS);
                String schemaAttributes = (String)smsGblCfg.
                        getString(SMSConstants.UM_SCHEMNA_ATTR
                        + "." +
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMDS);
                ldc.loadAMUserSchema(schemaString, schemaAttributes);
                ldc.disconnectDServer();
                Thread.sleep(5000);
                if (FederationManager.getExitCode(famadm.createDatastore(
                webClient, dsRealm, dsName, dsType, list)) != 0) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "createDatastore famadm command failed");
                    assert false;
                }
                page = famadm.listDatastores(webClient, dsRealm);
                if (FederationManager.getExitCode(page) != 0) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "listDatastores famadm command failed");
                    assert false;
                }
                if (getHtmlPageStringIndex(page, dsName) == -1) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "Datastore creation failed: " + list);
                     assert false;
                }
            }
            dsCreated = true;
	    list.clear();
        }
        return (dsCreated);
    }

    /**
     * This method creates the datastore using famadm.jsp
     */
    private boolean createDataStoreUsingfamadm(WebClient webClient,
            FederationManager famadm, ResourceBundle cfgData, Map umDMap, int
            index)
    throws Exception {
        String dsRealm = null;
        String dsType;
        String dsName;
        String adminId;
        String ldapServer;
        String ldapPort;
        String dsAdminPassword;
        String dsAuthPassword;
        String orgName;
        boolean dsCreated = false;

        Map stdLDAPv3Data = getMapFromResourceBundle("config" + fileseparator +
                "default" + fileseparator +
                SMSConstants.UM_DATASTORE_PARAMS_PREFIX,
                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_LDAP);
        
            dsCreated = false;
            dsType = SMSConstants.UM_DATASTORE_SCHEMA_TYPE_LDAP;
            dsName = (String)stdLDAPv3Data.get(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + "." + dsType + "." +
                    SMSConstants.UM_DATASTORE_NAME);
            adminId = (String)umDMap.get(index + "." + SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + "." +
                    SMSConstants.UM_LDAPv3_AUTHID);
            dsRealm = (String)stdLDAPv3Data.get(SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + "." + dsType + "." +
                    SMSConstants.UM_DATASTORE_REALM);
            ldapServer = (String)umDMap.get(index + "." + SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX +  "." +
                    SMSConstants.UM_LDAPv3_LDAP_SERVER);
            ldapPort = (String)umDMap.get(index + "." + SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + "." +
                    SMSConstants.UM_LDAPv3_LDAP_PORT);
            dsAdminPassword =
                    cfgData.getString(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            dsAuthPassword =
                    cfgData.getString(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            orgName = (String)umDMap.get(index + "." + SMSConstants.
                    UM_DATASTORE_PARAMS_PREFIX + "." + SMSConstants.
                    UM_LDAPv3_ORGANIZATION_NAME);

            Set s = stdLDAPv3Data.keySet();
            Iterator it = s.iterator();
            String key;
            String newkey = null;
            String value = null;
            while (it.hasNext()) {
                key = (String)it.next();
                if (key.contains(".sun")) {
                    newkey = key.substring(key.indexOf(".sun") + 1,
                            key.length());
/*
                if (newkey.equals(SMSConstants.UM_LDAPv3_AUTHPW))
                    value = dsAuthPassword;
                else 
*/
                    value = (String)stdLDAPv3Data.get(key);
                if (value.indexOf("ROOT_SUFFIX") != -1)
                    value = value.replace("ROOT_SUFFIX", orgName);
                if (value.indexOf("|") != 0) {
                    List locList = getAttributeList(value, "|");
                    for (int j = 0; j < locList.size(); j++) {
                        list.add(newkey + "=" + (String)locList.get(j));
                    }
                } else
                    list.add(newkey + "=" + value);
                }
            }

            list.add(SMSConstants.UM_LDAPv3_LDAP_SERVER + "=" + ldapServer +
                    ":" + ldapPort);


            log(Level.FINEST, "createDataStoreUsingfamadm", "Datastore" +
                    " attributes list:" + list);
            HtmlPage page = famadm.listDatastores(webClient, dsRealm);
            if (FederationManager.getExitCode(page) != 0) {
                log(Level.SEVERE, "createDataStoreUsingfamadm",
                        "listDatastores famadm command failed");
                assert false;
            }

            // Do nothing if a database by the chosen name already exists
	    if (getHtmlPageStringIndex(
                    page, dsName) == -1) {
                LDAPCommon ldc = new LDAPCommon(ldapServer, ldapPort, adminId,
                        dsAdminPassword, orgName);
                ResourceBundle smsGblCfg = ResourceBundle.
                        getBundle("config" + fileseparator + "default" +
                        fileseparator + "UMGlobalConfig");
                String schemaString = (String)smsGblCfg.
                        getString(SMSConstants.UM_SCHEMNA_LIST + "." + dsType);
                String schemaAttributes = (String)smsGblCfg.
                        getString(SMSConstants.UM_SCHEMNA_ATTR + "." + dsType);
                ldc.loadAMUserSchema(schemaString, schemaAttributes);
                ldc.disconnectDServer();
                Thread.sleep(5000);
                if (FederationManager.getExitCode(famadm.createDatastore(
                webClient, dsRealm, dsName, dsType, list)) != 0) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "createDatastore famadm command failed");
                    assert false;
                }
                page = famadm.listDatastores(webClient, dsRealm);
                if (FederationManager.getExitCode(page) != 0) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "listDatastores famadm command failed");
                    assert false;
                }
                if (getHtmlPageStringIndex(
                        page, dsName) == -1) {
                    log(Level.SEVERE, "createDataStoreUsingfamadm",
                            "Datastore creation failed: " + list);
                     assert false;
                }
            }
            dsCreated = true;
	    list.clear();
            
        return (dsCreated);
    }

    /**
     * This method modifies Policy Service using SMS API. LDAP server name,
     * port, bind bn, bind password, users base dn, roles base dn, ssl enabled
     * will be modified if User Management datastore is set to a remote
     * directory. The data values are picked from
     * resources/config/UMGlobalDatastoreConfig resource bundle.
     */
    private void modifyPolicyService(SMSCommon smsc, String serverName,
            int mIdx, int dIdx)
    throws Exception {
        entering("modifyPolicyService", null);
        String dsRealm = null;
        String ldapServer;
        String ldapPort;
        String dsAdminPassword;
        String dsAuthPassword;
        String orgName;
        String sslEnabled;
        String authId;
        String umDSType;

        ResourceBundle amCfgData = ResourceBundle.getBundle("Configurator-" +
                serverName + "-Generated");
        ResourceBundle cfgData = ResourceBundle.getBundle("config" +
                fileseparator + SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                "-Generated");

        umDSType = amCfgData.getString("umdatastore");

        try {
            if ((umDSType).equals("dirServer")) {
                dsRealm = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_DATASTORE_REALM + "." + dIdx);
                ldapServer = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_LDAPv3_LDAP_SERVER +
                        "." + dIdx);
                ldapPort = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_LDAPv3_LDAP_PORT +
                        "." + dIdx);
                dsAdminPassword = cfgData.getString(
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mIdx + "." + SMSConstants.UM_DATASTORE_ADMINPW +
                        "." + dIdx);
                dsAuthPassword = cfgData.getString(
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mIdx + "." + SMSConstants.UM_LDAPv3_AUTHPW +
                        "." + dIdx);
                orgName = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_LDAPv3_ORGANIZATION_NAME + "." + dIdx);
                sslEnabled = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_LDAPv3_LDAP_SSL_ENABLED + "." + dIdx);
                authId = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + mIdx + "." +
                        SMSConstants.UM_LDAPv3_AUTHID + "." + dIdx);

                Map scAttrMap =
                        smsc.getAttributes("iPlanetAMPolicyConfigService",
                        dsRealm, "Organization");
                log(Level.FINEST, "modifyPolicyService",
                        "Policy service attributes before modification: " +
                        scAttrMap);

                Map newPolicyAttrValuesMap = new HashMap();
                Set newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(ldapServer + ":" + ldapPort);
                newPolicyAttrValuesMap.
                        put("iplanet-am-policy-config-ldap-server",
                        newPolicyAttrValues);
                newPolicyAttrValues = new HashSet();
                newPolicyAttrValues.add(dsAuthPassword);
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
                        + scAttrMapNew);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "modifyPolicyService", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("modifyPolicyService");
    }

    /**
     * This method is only called when running sanity module.
     * Configures the deployed war and datastore for the server as the Config
     * Directory
     */
    public SetupProduct(String serverName0, String serverName1, 
            String strModuleName)
    throws Exception {
        super("SetupProduct");
        
        try {
            boolean bserver1 = false;

            String namingProtocol = "";
            String namingHost = "";
            String namingPort = "";
            String namingURI = "";

            ResourceBundle gblCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "default" + fileseparator +
                    "UMGlobalConfig");

            Map<String, String> umDatastoreTypes = new HashMap<String,
                    String>();
            
            // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified while
            // executing ant command for qatest. This loop is executed only if
            // both entries are sepcified.
            if (strModuleName.equalsIgnoreCase("sanity")) {
                // Checks if both SERVER_NAME1 and SERVER_NAME2 are specified
                // while executing ant command for qatest. This loop is executed
                // only if SERVER_NAME1 is sepcified. This setup refers to
                // single server tests only.                
                if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                        (serverName1.indexOf("SERVER_NAME2") == -1)) {
                    log(Level.SEVERE, "SetupProduct", "Sanity module supports" +
                            "only single server execution.");
                    assert false;
                } else if ((serverName0.indexOf("SERVER_NAME1") == -1) &&
                        (serverName1.indexOf("SERVER_NAME2") != -1)) {
                    log(Level.FINEST, "SetupProduct", "Sanity module: " +
                            "User config datastore will be default to config" +
                            "datastore.");
                    Map<String, String> configMapServer1 = new HashMap<String,
                        String>();
                    configMapServer1 = getMapFromResourceBundle(
                        "config" + fileseparator + "default" + fileseparator +
                        "ConfiguratorCommon");
                    configMapServer1.putAll(getMapFromResourceBundle(
                        "Configurator-" + serverName0));
                    configMapServer1.put(
                            TestConstants.KEY_ATT_CONFIG_DATASTORE, "embedded");
                    configMapServer1.put(
                            TestConstants.KEY_ATT_CONFIG_UMDATASTORE,
                            "embedded");
                    createFileFromMap(configMapServer1, serverName + 
                        fileseparator + "built" + fileseparator + "classes" + 
                        fileseparator + "Configurator-" + serverName0 +
                        "-Generated.properties");
                    ResourceBundle cfg1 = ResourceBundle.getBundle(
                            "Configurator-" + serverName0 + "-Generated");
                    String namingURL = cfg1.getString(KEY_AMC_NAMING_URL);
                    Map namingURLMap = getURLComponents(namingURL);

                    namingProtocol = (String) namingURLMap.get("protocol");
                    namingHost = (String) namingURLMap.get("host");
                    namingPort = (String) namingURLMap.get("port");
                    namingURI = (String) namingURLMap.get("uri");
                    String url = namingProtocol + "://" + namingHost + ":" + 
                            namingPort + namingURI ;
                    String loginURL = url + "/UI/Login";
                    log(Level.FINE, "SetupProduct", "Initiating setup for " +
                            serverName0);
                    bserver1 = configureProduct(getConfigurationMap(
                            "Configurator-" + serverName0 + "-Generated"), "1");
                    if (!bserver1) {
                        log(Level.FINE, "SetupProduct", "Configuration failed" +
                                " for " + serverName0);
                        assert false;
                    } else {
                        admintoken = getToken(adminUser, adminPassword, basedn);
                        SMSCommon smscSS = new SMSCommon(admintoken, "config" +
                                fileseparator + "default" + fileseparator +
                                "UMGlobalConfig");
                        url = namingProtocol + "://" + namingHost + ":" +
                                namingPort + namingURI ;
                        log(Level.FINEST, "SetuProduct", "serverconfig.xml" +
                                " details for " + namingURL + ":  " +
                                (smscSS.getServerConfigData(url)).toString());
                        Map mCfgData = smscSS.getServerConfigData(url);
                        umDatastoreTypes.put("1", serverName0);
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_SERVER));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_LDAP_PORT));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME));
                        umDatastoreTypes.put("1." +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID,
                                (String)mCfgData.get(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                SMSConstants.UM_LDAPv3_AUTHID));
                        createGlobalDatastoreFile(umDatastoreTypes,
                                SMSConstants.QATEST_EXEC_MODE_SINGLE);
                        smscSS.createDataStore(1, "config" + fileseparator +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "-Generated");
                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "deleteExistingDatastores")).equals("true"))
                            smscSS.deleteAllDataStores(realm, 1);
                        modifyPolicyService(smscSS, serverName0, 1, 0);
                        ResourceBundle cfgData = ResourceBundle.getBundle(
                                "config" + fileseparator +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "-Generated");
                        mCfgData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "." + SMSConstants.UM_LDAPv3_AUTHID,
                                cfgData.getString(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "1." + SMSConstants.UM_LDAPv3_AUTHID + ".0"));
                        mCfgData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "." + SMSConstants.UM_LDAPv3_AUTHPW,
                                cfgData.getString(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                "1." + SMSConstants.UM_LDAPv3_AUTHPW + ".0"));
                        modifyAuthConfigproperties(mCfgData);
                    }
                } else if ((serverName0.indexOf("SERVER_NAME1") != -1) &&
                    (serverName1.indexOf("SERVER_NAME2") == -1)) {
                    log(Level.FINE, "SetupProduct", "Unsupported " +
                            "configuration." + " Cannot have SERVER_NAME2 " 
                            + "specified without SERVER_NAME1.");
                    assert false;
                }
            } else {
                log(Level.SEVERE, "SetupProduct", "This part of the code " +
                        "should never be reached.Contact QA administrator");
            }
         } catch (Exception e) {
            log(Level.SEVERE, "SetupProduct", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("SetupProduct");
}

    /**
     * This method modifies the generated 
     * AuthenticationConfig.properties file with the details of the embedded
     * directory server
     * @param serverConfigMap - a <code>Map</code> containing the configuration
     * details for the OpenSSO configuration.
     */
    private void modifyAuthConfigproperties(Map<String, String> serverconfigMap) 
            throws Exception { 
        Map<String, String> authConfigMap;
        StringBuffer buff;
        StringBuffer buffer;
        
        try {
            String strAuthConfigFileName = getBaseDir() + fileseparator +
                serverName + fileseparator + "built" + 
                fileseparator + "classes" + fileseparator + 
                "config" + fileseparator +
                "AuthenticationConfig.properties";
            authConfigMap = getMapFromProperties(strAuthConfigFileName);
            buff = new StringBuffer();
            buffer = new StringBuffer();
            
            buff.append(SMSConstants.UM_DATASTORE_PARAMS_PREFIX)
                    .append(".")
                    .append(SMSConstants.UM_LDAPv3_LDAP_SERVER);
            buffer.append(serverconfigMap.get(buff.toString()));
            buff.setLength(0);
            buff.append(SMSConstants.UM_DATASTORE_PARAMS_PREFIX)
                    .append(".")
                    .append(SMSConstants.UM_LDAPv3_LDAP_PORT);
            buffer.append(":")
                    .append(serverconfigMap.get(buff.toString()));
            authConfigMap.put("ldap.iplanet-am-auth-ldap-server", 
                    buffer.toString());
            
            buffer.setLength(0);
            buff.setLength(0);
            buff.append(SMSConstants.UM_DATASTORE_PARAMS_PREFIX)
                    .append(".")
                    .append(SMSConstants.UM_LDAPv3_ORGANIZATION_NAME);
            authConfigMap.put("ldap.iplanet-am-auth-ldap-base-dn", 
                    serverconfigMap.get(buff.toString()));
            
            buffer.append(SMSConstants.UM_DATASTORE_PARAMS_PREFIX)
                    .append(".")
                    .append(SMSConstants.UM_LDAPv3_AUTHID);
            authConfigMap.put("ldap.iplanet-am-auth-ldap-bind-dn", 
                    serverconfigMap.get(buffer.toString()));
            
            buff.setLength(0);
            buff.append(SMSConstants.UM_DATASTORE_PARAMS_PREFIX)
                    .append(".")
                    .append(SMSConstants.UM_LDAPv3_AUTHPW);
            authConfigMap.put("ldap.iplanet-am-auth-ldap-bind-passwd", 
                    serverconfigMap.get(buff.toString()));
            
            log(Level.FINEST, "modifyAuthConfigproperties", "authConfigMap :" +
                    " \n" + authConfigMap);
            createFileFromMap(authConfigMap, serverName + fileseparator +
                        "built" + fileseparator + "classes" + fileseparator +
                        "config" + fileseparator +
                        "AuthenticationConfig.properties");
        } catch (Exception e) {
            log(Level.SEVERE, "modifyAuthConfigproperties", "Exception when " +
                    "changing AuthenticationConfig.properties");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        try {
            if (args.length == 3) {
                SetupProduct cp = new SetupProduct(args[0], args[1], args[2]);
            } else {
                SetupProduct cp = new SetupProduct(args[0], args[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}