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
 * $Id: ClientConfigCreator.java,v 1.27 2009/05/27 23:07:19 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.setup;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.TestConstants;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.PropertyResourceBundle;

/**
 * This class does the following:
 * (a) This class creates the tag swapped AMConfig.properties file. It takes 
 *     AMClient.properties and Configurator-<server name> files under resources 
 *     directory and create a new file called AMConfig.properties file by tag
 *     swapping attribute annotated with values as @COPY_FROM_CONFIG@ with
 *     values read from Configurator-<server name> file specified under the
 *     resources directory. This  AMConfig.properties is  the client side
 *     AMConfig.properties file.
 * (b) This class creates the tag swapped multi server config data properties
 *     file. It takes the two Configurator-<server name> files under resources 
 *     directory and create a new config file (the name of this file is 
 *     specific to the multi server mode under execution. For eg for samlv2 its
 *     called  samlv2TestConfigData) by tag swapping attribute annotated with
 *     values as @COPY_FROM_CONFIG@ with values read from
 *     Configurator-<server name> file specified under the resources directory.
 *     This file is consumed by the respective module to do its configuration.
 */
public class ClientConfigCreator { 

    private String fileseparator = System.getProperty("file.separator");
    private String uriseparator = "/";
    private String hostname;
    private Map properties_ss = new HashMap();
    private Map properties_saml = new HashMap();
    private Map properties_idff = new HashMap();
    private Map properties_wsfed = new HashMap();
    private Map properties_sae = new HashMap();
    private String ALL_FILE_CLIENT_PROPERTIES = "resources" + fileseparator +
            "AMConfig.properties";
    private String SAML_FILE_CLIENT_PROPERTIES =
            "resources" + fileseparator + "samlv2" + fileseparator +
            "samlv2TestConfigData.properties";
    private String IDFF_FILE_CLIENT_PROPERTIES =
            "resources" + fileseparator + "idff" + fileseparator +
            "idffTestConfigData.properties";
    private String WSFED_FILE_CLIENT_PROPERTIES =
            "resources" + fileseparator + "wsfed" + fileseparator +
            "WSFedTestConfigData.properties";
    private String SAE_FILE_CLIENT_PROPERTIES =
            "resources" + fileseparator + "sae" + fileseparator +
            "saeTestConfigData.properties";

    /**
     * Default constructor. Calls method to transfer properties from:
     * (a) default AMClient.properties to client AMConfig.properties file. 
     * (b) multiple configuration files to a single multi server execution mode
     *     file.
     */
    public ClientConfigCreator(String testDir, String serverName1,
            String serverName2, String executionMode)
        throws Exception {

        InetAddress addr = InetAddress.getLocalHost();
        hostname = addr.getCanonicalHostName();

        if ((serverName2.indexOf("SERVER_NAME2")) != -1) {
            getDefaultValues(testDir, serverName1);
        } else {
            Map<String, String> configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle(testDir + fileseparator + 
                    "resources" + fileseparator + "config" + fileseparator +
                    "default" + fileseparator +
                    "ConfiguratorCommon.properties");
            configMap.putAll(getMapFromResourceBundle(testDir + fileseparator + 
                    "resources" + fileseparator + "Configurator-" + serverName1 
                    + ".properties"));
            if (configMap.get(TestConstants.
                    KEY_ATT_MULTIPROTOCOL_ENABLED).equalsIgnoreCase("true")) {
                getDefaultValues(testDir, serverName1, 
                        configMap.get(TestConstants.KEY_ATT_IDFF_SP), 
                        serverName2, properties_idff, "multiprotocol");
                getDefaultValues(testDir, serverName1, 
                        configMap.get(TestConstants.KEY_ATT_WSFED_SP), 
                        properties_wsfed, "multiprotocol");
                getDefaultValues(testDir, serverName1, serverName2, 
                        configMap.get(TestConstants.KEY_ATT_IDFF_SP),
                        properties_saml, "multiprotocol");
            } else {
                getDefaultValues(testDir, serverName1, serverName2, 
                        properties_saml, "samlv2");
                getDefaultValues(testDir, serverName1, serverName2, 
                        properties_idff, "idff");
                getDefaultValues(testDir, serverName1, serverName2, 
                        properties_wsfed, "wsfed");
            }
                getDefaultValues(testDir, serverName1, serverName2, 
                        properties_sae, "sae");
            createFileFromMap(properties_saml, SAML_FILE_CLIENT_PROPERTIES);
            createFileFromMap(properties_idff, IDFF_FILE_CLIENT_PROPERTIES);
            createFileFromMap(properties_wsfed, WSFED_FILE_CLIENT_PROPERTIES);
            createFileFromMap(properties_sae, SAE_FILE_CLIENT_PROPERTIES);
        }
        createFileFromMap(properties_ss, ALL_FILE_CLIENT_PROPERTIES);
    }

    /**
     * Method to do the actual transfer of properties from  default
     * AMClient.properties to client AMConfig.properties file.
     */
    private void getDefaultValues(String testDir, String serverName)
        throws Exception {
        Map<String, String> configMap = new HashMap<String, String>();
        configMap = getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "config" + fileseparator +
                "default" + fileseparator + 
                "ConfiguratorCommon.properties");
        configMap.putAll(getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "Configurator-" + serverName
                + ".properties"));
        CheckValues(configMap, serverName);
        
        String strNamingURL = configMap.get(
                TestConstants.KEY_AMC_NAMING_URL);
        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);  

        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);

        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);

        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);

        for (Iterator iter = configMap.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (value.equals("@COPY_FROM_CONFIG@")) {
                if (key.equals(TestConstants.KEY_AMC_PROTOCOL))
                    value = strProtocol;
                else if (key.equals(TestConstants.KEY_AMC_HOST))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_AMC_PORT))
                    value = strPort;
                else if (key.equals(TestConstants.KEY_AMC_URI))
                    value = strURI;
                else if (key.equals(TestConstants.KEY_AMC_NAMING_URL))
                    value = strNamingURL;
                else if (key.equals(TestConstants.KEY_AMC_BASEDN))
                    value = configMap.get(
                            TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX);
                else if (key.equals(TestConstants.KEY_AMC_SERVICE_PASSWORD))
                    value = configMap.get(
                            TestConstants.KEY_AMC_SERVICE_PASSWORD);
                else if (key.equals(TestConstants.KEY_ATT_AM_ENC_PWD))
                    value = configMap.get(
                            TestConstants.KEY_ATT_AM_ENC_PWD);
                else if (key.equals(TestConstants.KEY_AMC_WSC_CERTALIAS))
                    value = configMap.get(
                            TestConstants.KEY_AMC_WSC_CERTALIAS);
                else if (key.equals(TestConstants.KEY_AMC_KEYSTORE))
                    value = configMap.get(
                            TestConstants.KEY_AMC_KEYSTORE);
                else if (key.equals(TestConstants.KEY_AMC_KEYPASS))
                    value = configMap.get(
                            TestConstants.KEY_AMC_KEYPASS);
                else if (key.equals(TestConstants.KEY_AMC_STOREPASS))
                    value = configMap.get(
                            TestConstants.KEY_AMC_STOREPASS);
                else if (key.equals(TestConstants.KEY_AMC_XMLSIG_CERTALIAS))
                    value = configMap.get(
                            TestConstants.KEY_AMC_XMLSIG_CERTALIAS);
                else if (key.equals(TestConstants.KEY_AMC_IDM_CACHE_ENABLED))
                    value = configMap.get(
                            TestConstants.KEY_AMC_IDM_CACHE_ENABLED);
                else if (key.equals(TestConstants.KEY_AMC_AUTHNSVC_URL))
                    value = strProtocol + "://" + strHost + ":" + strPort +
                            strURI + "/" + "Liberty/authnsvc";
            }
            value = value.replace("@BASE_DIR@", testDir + fileseparator +
                    serverName);
            properties_ss.put(key, value);
        }
  
        properties_ss.put(TestConstants.KEY_ATT_SERVER_NAME, serverName);
        int unusedPort = getUnusedPort();
        String notificationURL = "http://" + hostname + ":" + unusedPort 
                + (String)configMap.get(TestConstants.KEY_INTERNAL_WEBAPP_URI) + 
                uriseparator + "notificationservice";
        properties_ss.put(TestConstants.KEY_AMC_NOTIFICATION_URL, 
                notificationURL);
    }

    /**
     * Method to do the actual transfer of properties from  default
     * configuration files to single multi server execution mode config data
     * file.
     */
    private void getDefaultValues(String testDir, String serverName1,
            String serverName2, Map properties_protocol, String strModule)
        throws Exception {

        Map<String, String> configMap1 = new HashMap<String, String>();
        configMap1 = getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "config" + fileseparator +
                "default" + fileseparator + 
                "ConfiguratorCommon.properties");
        configMap1.putAll(getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "Configurator-" + serverName1 
                + ".properties"));
        CheckValues(configMap1, serverName1);

        String strNamingURL = configMap1.get(TestConstants.KEY_AMC_NAMING_URL);

        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);  

        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);

        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);

        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);

        int iDot = strHost.indexOf(".");
        String cookieDomain;
        if (iDot == -1)
            cookieDomain = strHost;
        else
            cookieDomain = strHost.substring(iDot,
                    strHost.length());

        for (Iterator iter = configMap1.entrySet().iterator(); 
                iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (value.equals("@COPY_FROM_CONFIG@")) {
                if (key.equals(TestConstants.KEY_AMC_PROTOCOL))
                    value = strProtocol;
                else if (key.equals(TestConstants.KEY_AMC_HOST))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_AMC_PORT))
                    value = strPort;
                else if (key.equals(TestConstants.KEY_AMC_URI))
                    value = strURI;
                else if (key.equals(TestConstants.KEY_ATT_METAALIAS))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_ATT_ENTITY_NAME))
                    value = strModule + ".idp." + strHost;
                else if (key.equals(TestConstants.KEY_ATT_COT))
                    value = "idpcot";
            }
            value = value.replace("@BASE_DIR@", testDir + fileseparator +
                    serverName1 + "_" + serverName2);
            if (key.equals(TestConstants.KEY_ATT_METAALIAS)) {
                if (!configMap1.get(TestConstants.
                        KEY_ATT_EXECUTION_REALM).endsWith("/")) {
                    value = configMap1.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) + "/" + 
                            value;
                } else {
                    value = configMap1.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) + value;
                }
            }
            if (!key.equals(TestConstants.KEY_AMC_NAMING_URL) &&
                    !key.equals(TestConstants.KEY_ATT_DEFAULTORG) &&
                    !key.equals(TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT) &&
                    !key.equals(TestConstants.KEY_ATT_LOG_LEVEL))
            properties_protocol.put("idp_" + key, value);
        }
        properties_protocol.put(TestConstants.KEY_IDP_COOKIE_DOMAIN,
                cookieDomain);
        properties_protocol.put(TestConstants.KEY_IDP_SERVER_ALIAS,
                serverName1);

        Map<String, String> configMap2 = new HashMap<String, String>();
        configMap2 = getMapFromResourceBundle(testDir + fileseparator 
                + "resources" + fileseparator + "config" + fileseparator +
                "default" + fileseparator + 
                "ConfiguratorCommon.properties");
        configMap2.putAll(getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "Configurator-" + serverName2 
                + ".properties"));
        PropertyResourceBundle configDef2 = new PropertyResourceBundle(
            new FileInputStream(testDir + fileseparator + "resources" +
                fileseparator + "Configurator-" +
                serverName2 + ".properties"));
        CheckValues(configMap2, serverName2);

        strNamingURL = configMap2.get(TestConstants.KEY_AMC_NAMING_URL);
 
        iFirstSep = strNamingURL.indexOf(":");
        strProtocol = strNamingURL.substring(0, iFirstSep);

        iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);

        iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);

        iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);

        iDot = strHost.indexOf(".");
        if (iDot == -1)
            cookieDomain = strHost;
        else
            cookieDomain = strHost.substring(iDot,
                    strHost.length());

        for (Iterator iter = configMap2.entrySet().iterator(); 
                iter.hasNext();) { 
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (value.equals("@COPY_FROM_CONFIG@")) {
                if (key.equals(TestConstants.KEY_AMC_PROTOCOL))
                    value = strProtocol;
                else if (key.equals(TestConstants.KEY_AMC_HOST))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_AMC_PORT))
                    value = strPort;
                else if (key.equals(TestConstants.KEY_AMC_URI))
                    value = strURI;
                else if (key.equals(TestConstants.KEY_AMC_NAMING_URL))
                    value = strNamingURL;
                else if (key.equals(TestConstants.KEY_AMC_BASEDN))
                    value = configMap2.get(
                            TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX);
                else if (key.equals(TestConstants.KEY_AMC_SERVICE_PASSWORD))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_SERVICE_PASSWORD);
                else if (key.equals(TestConstants.KEY_ATT_AM_ENC_PWD))
                    value = configMap2.get(
                            TestConstants.KEY_ATT_AM_ENC_PWD);
                else if (key.equals(TestConstants.KEY_AMC_WSC_CERTALIAS))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_WSC_CERTALIAS);
                else if (key.equals(TestConstants.KEY_AMC_KEYSTORE))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_KEYSTORE);
                else if (key.equals(TestConstants.KEY_AMC_KEYPASS))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_KEYPASS);
                else if (key.equals(TestConstants.KEY_AMC_STOREPASS))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_STOREPASS);
                else if (key.equals(TestConstants.KEY_AMC_XMLSIG_CERTALIAS))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_XMLSIG_CERTALIAS);
                else if (key.equals(TestConstants.KEY_AMC_IDM_CACHE_ENABLED))
                    value = configMap2.get(
                            TestConstants.KEY_AMC_IDM_CACHE_ENABLED);
                else if (key.equals(TestConstants.KEY_AMC_AUTHNSVC_URL))
                    value = strProtocol + "://" + strHost + ":" + strPort +
                            strURI + "/" + "Liberty/authnsvc";
            }
            if (value.equals("@COPY_FROM_CONFIG@")) {
                if (key.equals(TestConstants.KEY_AMC_PROTOCOL))
                    value = strProtocol;
                else if (key.equals(TestConstants.KEY_AMC_HOST))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_AMC_PORT))
                    value = strPort;
                else if (key.equals(TestConstants.KEY_AMC_URI))
                    value = strURI;
                else if (key.equals(TestConstants.KEY_ATT_METAALIAS))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_ATT_ENTITY_NAME))
                    value = strModule + ".sp." + strHost;
                else if (key.equals(TestConstants.KEY_ATT_COT))
                    value = "spcot";
            }
            if (key.equals(TestConstants.KEY_ATT_METAALIAS)) {
                if (!configMap2.get(TestConstants.
                        KEY_ATT_EXECUTION_REALM).endsWith("/")) {
                    value = configMap2.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) + "/" + 
                            value;
                } else {
                    value = configMap2.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) +  value;
                }
            }
            value = value.replace("@BASE_DIR@", testDir + fileseparator +
                    serverName1 + "_" + serverName2);
            if (!key.equals(TestConstants.KEY_AMC_NAMING_URL) &&
                    !key.equals(TestConstants.KEY_ATT_DEFAULTORG) &&
                    !key.equals(TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT) &&
                    !key.equals(TestConstants.KEY_ATT_LOG_LEVEL))
            properties_protocol.put("sp_" + key, value);

            properties_ss.put(key, value);
        }
        properties_protocol.put(TestConstants.KEY_SP_COOKIE_DOMAIN,
                cookieDomain);

        properties_ss.put(TestConstants.KEY_ATT_SERVER_NAME, serverName1 + "_" +
                serverName2);
                int unusedPort = getUnusedPort();
        String notificationURL = "http://" + hostname + ":" + unusedPort +
                (String)configMap2.get(TestConstants.KEY_INTERNAL_WEBAPP_URI) + 
                uriseparator + "notificationservice";
        properties_ss.put(TestConstants.KEY_AMC_NOTIFICATION_URL, 
                notificationURL);

    }
    
    /**
     * Method to do the actual transfer of properties from  default
     * configuration files to single multi server execution mode config data
     * file.
     * serverName1 will be used for IDP
     * serverName2 will be used for SP
     * serverName3 will be used as IDP proxy
     */
    public void getDefaultValues(String testDir, String serverName1, String 
            serverName2, String serverName3, Map properties_protocol,
            String strModule) throws Exception
    {
        getDefaultValues(testDir, serverName1, serverName2, 
                properties_protocol, strModule);
        Map<String, String> configMap3 = new HashMap<String, String>();
        configMap3 = getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "config" + fileseparator +
                "default" + fileseparator + 
                "ConfiguratorCommon.properties");
        configMap3.putAll(getMapFromResourceBundle(testDir + fileseparator + 
                "resources" + fileseparator + "Configurator-" + serverName3 
                + ".properties"));
        CheckValues(configMap3, serverName3);

        String strNamingURL = configMap3.get(TestConstants.KEY_AMC_NAMING_URL);

        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);  

        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);

        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);

        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);

        for (Iterator iter = configMap3.entrySet().iterator(); 
                iter.hasNext();) { 
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (value.equals("@COPY_FROM_CONFIG@")) {
                if (key.equals(TestConstants.KEY_AMC_PROTOCOL))
                    value = strProtocol;
                else if (key.equals(TestConstants.KEY_AMC_HOST))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_AMC_PORT))
                    value = strPort;
                else if (key.equals(TestConstants.KEY_AMC_URI))
                    value = strURI;
                else if (key.equals(TestConstants.KEY_ATT_METAALIAS))
                    value = strHost;
                else if (key.equals(TestConstants.KEY_ATT_ENTITY_NAME))
                    value = strModule + ".proxy." + strHost;
                else if (key.equals(TestConstants.KEY_ATT_COT))
                    value = "proxycot";
            }
            value = value.replace("@BASE_DIR@", testDir + fileseparator +
                    serverName1 + "_" + serverName2);

            if (key.equals(TestConstants.KEY_ATT_METAALIAS)) {
                if (!configMap3.get(TestConstants.
                        KEY_ATT_EXECUTION_REALM).endsWith("/")) {
                    value = configMap3.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) + "/" + 
                            value;
                } else {
                    value = configMap3.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM) + 
                            value;
                }
            properties_protocol.put("idpProxy_sp_" + key, value + "proxysp");
            properties_protocol.put("idpProxy_idp_" + key, value + "proxyidp");
            }
            if (!key.equals(TestConstants.KEY_AMC_NAMING_URL) &&
                    !key.equals(TestConstants.KEY_ATT_DEFAULTORG) &&
                    !key.equals(TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT) &&
                    !key.equals(TestConstants.KEY_ATT_LOG_LEVEL))
            properties_protocol.put("idpProxy_" + key, value);
        }       
    }

    /**
     * Reads data from a Map object, creates a new file and writes data to that
     * file
     */
    private void CheckValues(Map properties, String serverName)
        throws Exception
    {
        if ((properties.get(TestConstants.KEY_AMC_NAMING_URL).equals(null)) ||
               (properties.get(TestConstants.KEY_AMC_NAMING_URL).equals(""))) {
            System.out.println(TestConstants.KEY_AMC_NAMING_URL + 
                    " should have some value\n");
            assert false;
        }
               
        if ((properties.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD).
                equals(null)) || (properties.get(TestConstants.
                KEY_ATT_AMADMIN_PASSWORD).equals(""))) {
            System.out.println(TestConstants.KEY_ATT_AMADMIN_PASSWORD + 
                    " value should not be empty\n");
            assert false;
        }
               
        if ((properties.get(TestConstants.KEY_AMC_SERVICE_PASSWORD).
                equals(null)) || (properties.get(TestConstants.
                KEY_AMC_SERVICE_PASSWORD).equals(""))) {
            System.out.println(TestConstants.KEY_AMC_SERVICE_PASSWORD + 
                    " value should not be empty\n");
            assert false;
        }

        String strNamingURL =
                (String)properties.get(TestConstants.KEY_AMC_NAMING_URL);

        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);

        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);

        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);

        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);

        WebClient webclient = new WebClient();
        String strURL = strProtocol + "://" + strHost + ":" + strPort +
                strURI +  "/config/options.htm";
        URL url = new URL(strURL);
        HtmlPage page = null;
        try {
            page = (HtmlPage)webclient.getPage(url);
        } catch (java.net.UnknownHostException e) {
            System.out.println("Product configuration was not" +
                    " successfull." + strURL + " was not found." +
                    " Please check if war is deployed properly" +
                    " or url name parameters are correct.");
            assert false;
        }

        String strDSPassword = (String)properties.get(
                TestConstants.KEY_ATT_DS_DIRMGRPASSWD);
        String strDSHost = (String)properties.get(
                TestConstants.KEY_ATT_DIRECTORY_SERVER);
        
        // The logic below checks if the FQDN specified for config directory
        // server name is same as server host name or not.
        boolean hMatch = true;
        if (!strDSHost.equals(null) || !strDSHost.equals("")) {
            String strHostAdd = InetAddress.getByName(strHost).toString();
            String strHostIPAdd = strHostAdd.substring(strHostAdd.indexOf("/") +
                    1, strHostAdd.length());
            String strDSAdd = InetAddress.getByName(strDSHost).toString();
            String strDSIPAdd = strDSAdd.substring(strDSAdd.indexOf("/") + 1,
                    strDSAdd.length());
            if (strDSIPAdd.indexOf("127") == -1) {
                if (!strHostIPAdd.equals(strDSIPAdd))
                    hMatch = false;
            }
        }
        
        // The logic below checks that directory server password has to be 
        // specified if we are using external directory when embedded mode is
        // set for config directory server
        if (((properties.get(TestConstants.KEY_ATT_CONFIG_DATASTORE)).
                    equals("embedded")) && ((strDSPassword.equals(null)) ||
                    (strDSPassword.equals(""))) && (!hMatch)) {
                System.out.println(TestConstants.KEY_ATT_DS_DIRMGRPASSWD +
                        " value should not be empty for server " + serverName +
                        "\n");
                assert false;
        }
        if ((page != null) && ((page.asXml()).indexOf("configuration") != -1)) {

            if ((properties.get(TestConstants.KEY_ATT_CONFIG_DATASTORE)).
                    equals("dirServer")) {

                if ((properties.get(TestConstants.KEY_ATT_DIRECTORY_SERVER).
                        equals(null)) || (properties.get(TestConstants.
                        KEY_ATT_DIRECTORY_SERVER).equals(""))) {
                    System.out.println(TestConstants.KEY_ATT_DIRECTORY_SERVER +
                            " value should not be empty for server " +
                            serverName + "\n");
                    assert false;
                }

                if ((properties.get(TestConstants.KEY_ATT_DIRECTORY_PORT).
                        equals(null)) || (properties.get(TestConstants.
                        KEY_ATT_DIRECTORY_PORT).equals(""))) {
                    System.out.println(TestConstants.KEY_ATT_DIRECTORY_PORT +
                            " value should not be empty for server " +
                            serverName + "\n");
                    assert false;
                }

                 if ((properties.get(TestConstants.KEY_ATT_DS_DIRMGRPASSWD).
                        equals(null)) || (properties.get(TestConstants.
                        KEY_ATT_DS_DIRMGRPASSWD).equals(""))) {
                    System.out.println(TestConstants.KEY_ATT_DS_DIRMGRPASSWD +
                            " value should not be empty for server " +
                            serverName + "\n");
                    assert false;
                }
            }

            if ((properties.get(TestConstants.KEY_ATT_CONFIG_DIR).
                    equals(null)) || (properties.get(TestConstants.
                    KEY_ATT_CONFIG_DIR).equals(""))) {
                System.out.println(TestConstants.KEY_ATT_CONFIG_DIR +
                        " value should not be empty for server " +
                        serverName + "\n");
                assert false;
            }

            if ((properties.get(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX).
                    equals(null)) || (properties.get(TestConstants.
                    KEY_ATT_CONFIG_ROOT_SUFFIX).equals(""))) {
                System.out.println(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX +
                        " value should not be empty for server " +
                        serverName + "\n");
                assert false;
            }
        }
   }

    /**
     * Reads data from a Map object, creates a new file and writes data to that
     * file
     */
    private void createFileFromMap(Map properties, String fileName)
        throws Exception
    {
        StringBuffer buff = new StringBuffer();
        for (Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            buff.append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append("\n");
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(
            fileName));
        out.write(buff.toString());
        out.close();
    }

    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values.
     * @param resourcebundle name
     */
    protected Map getMapFromResourceBundle(String rbName)
    throws Exception {
        Map map = new HashMap();
        PropertyResourceBundle rb = new PropertyResourceBundle(
            new FileInputStream(rbName));
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);
                map.put(key, value);
         }
        return (map);
    }

    /**
      * Returns a unused port on a given host.
      *    @param hostname (eg localhost)
      *    @param start: starting port number to check (eg 389).
      *    @param incr : port number increments to check (eg 1000).
      *    @return available port num if found. -1 of not found.
      */
    private int getUnusedPort()
        throws Exception
    {
        int defaultPort = -1;
        int start = 44444;
        int incr = 100;
        InetAddress inetAdd = InetAddress.getLocalHost();

        for (int i = start; i < 65500 && (defaultPort == -1); i += incr) {
            Random rnd = new Random();
            int rNum = rnd.nextInt(1000);
            if (canUseAsPort(inetAdd.getHostAddress(), i + rNum)) {
                defaultPort = i + rNum;
            }
        }

        return defaultPort;
    }

    /**
      * Checks whether the given host:port is currenly under use.
      *    @param hostname (eg localhost)
      *    @param incr : port number.
      *    @return  true if not in use, false if in use.
      */
    public static boolean canUseAsPort(String hostname, int port)
    {
        boolean canUseAsPort = false;
        ServerSocket serverSocket = null;
        try {
            InetSocketAddress socketAddress =
                new InetSocketAddress(hostname, port);
            serverSocket = new ServerSocket();
            serverSocket.bind(socketAddress);
            canUseAsPort = true;
     
            serverSocket.close();
       
            Socket s = null;
            try {
              s = new Socket();
              s.connect(socketAddress, 1000);
              canUseAsPort = false;
            } catch (Throwable t) {
            }
            finally {
              if (s != null) {
                try {
                  s.close();
                } catch (Throwable t) { }
              }
            }
        } catch (IOException ex) {
          canUseAsPort = false;
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception ex) { }
        }
     
        return canUseAsPort;
    }

    public static void main(String args[]) {
        try {
            ClientConfigCreator creator = new ClientConfigCreator(args[0],
                    args[1], args[2], args[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}