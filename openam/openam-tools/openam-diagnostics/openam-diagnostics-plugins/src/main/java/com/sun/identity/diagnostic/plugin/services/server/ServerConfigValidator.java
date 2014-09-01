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
 * $Id: ServerConfigValidator.java,v 1.3 2009/11/13 21:55:12 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;


import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;


/**
 * This is a supporting class to validate the server configuration
 * properties.
 */
public class ServerConfigValidator extends ServerConfigBase {
    
    private static Map validProp = new HashMap();
    private static Map invalidProp = new HashMap();
    private Map<String, Map> testResult = new HashMap();
    private static boolean status = true;
    private static boolean isSvrRunning = false;
    private IToolOutput toolOutWriter;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        validProp = loadPropertiesToMap(VALID_SERVER_PROP);
    }
    
    // instantiation of this class.
    public ServerConfigValidator() {
    }
    
    /**
     * Validate the configuration.
     */
    public void validate(String path) {
        toolOutWriter = ServerConfigService.getToolWriter();
        Map <String, String> dsInstance = validateDSServers(path);
        if (!dsInstance.isEmpty()) {
            Properties configProp = null;
            SSOToken ssoToken = null;
            try {
                configProp = loadConfigFromBootfile(path);
                if (!configProp.isEmpty()) {
                    ssoToken = getAdminSSOToken();
                    if (ssoToken != null) {
                        processServers(ssoToken,
                            getServerInstanceName(dsInstance));
                     } else {
                        toolOutWriter.printError("svr-auth-msg");
                     }
                } else {
                    toolOutWriter.printError("svr-ins-prop-load-fail",
                        new String[] {getServerInstanceName(dsInstance)});
                }
            } catch (Exception e) {
                Debug.getInstance(DEBUG_NAME).error(
                    "ServerConfigValidator.validate: " +
                    "Exception in validating configuration information", e);
                toolOutWriter.printError("svr-prop-load-fail");
                toolOutWriter.printStatusMsg(false, "svr-get-boot-corrupt");
            }
        } else {
            //All boot servers are down
            toolOutWriter.printError("svr-all-boot-svr-dwn",
                new String[] {path});
            toolOutWriter.printStatusMsg(false, "svr-get-boot-corrupt");
        }
    }
    
    /**
     * Compare the configured properties with the default properties
     * and create a Map of properties that have changed.
     */
    private void compareProperties(
        SSOToken ssoToken,
        Map defProp,
        String svrInstance
    ) {
        Map<String, String> changedProp = new HashMap<String,String>();
        try {
            changedProp = detectChangedDefaultProperties(defProp);
            Set serverSet = ServerConfiguration.getServers(ssoToken);
            for (Iterator items = serverSet.iterator(); items.hasNext();) {
                String server = (String) items.next();
                toolOutWriter.printMessage("\n");
                toolOutWriter.printMessage("svr-process-svr-ins",
                    new String[] {server});
                isSvrRunning = isServerRunning(server);
                toolOutWriter.printStatusMsg(isSvrRunning, "svr-ins-running");
                if (!isSvrRunning) {
                    toolOutWriter.printWarning("svr-ins-not-running",
                        new String[] {server});
                }
                if (!existInServerList(ssoToken, server)) {
                    toolOutWriter.printError("svr-no-svr-entry",
                        new String[] {server});
                }
                Properties sProps = ServerConfiguration.getServerInstance(
                    ssoToken, server);
                SystemProperties.initializeProperties(sProps, false);
                validateProperties(sProps, changedProp, defProp, server);
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.compareProperties: " +
                "Exception", e);
        }
    }
    
    private Map detectChangedDefaultProperties(Map defProp) {
        toolOutWriter.printMessage("\n");
        toolOutWriter.printMessage("svr-glog-prop-chg");
        toolOutWriter.printMessage(SMALL_LINE_SEP_1);
        Map <String, String> changeMap = new HashMap();
        //Identify changed default properties
        Map defFileProp = loadPropertiesToMap(DEFAULT_SERVER_PROP);
        Set keys = defFileProp.keySet();
        Iterator keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            String key = (String)keyIter.next();
            String value = (String)defFileProp.get(key);
            value = (value != null && value.length() > 0) ?
                value.trim() : "";
            String defValue = (String)defProp.get(key);
            defValue = (defValue != null && defValue.length() > 0) ?
                defValue.trim() : "";
            String[] params = {key, defValue};
            if (!(defValue.equals(value)) && defProp.containsKey(key)) {
                changeMap.put(key, defValue);
                toolOutWriter.printMessage("svr-print-prop", params);
            }
        }
        //Check for added properties
        Set propKeys = defProp.keySet();
        Iterator propKeyIter = propKeys.iterator();
        while (propKeyIter.hasNext()) {
            String key = (String)propKeyIter.next();
            if (!keys.contains(key)) {
                changeMap.put(key, (String)defProp.get(key));
            }
        }
        return changeMap;
    }
    
    private static boolean getServerStatus() {
        return isSvrRunning;
    }
    
    /**
     * Validate all the server entries
     */
    private void processServers(
        SSOToken ssoToken,
        String svrInstance
    ) throws SMSException, SSOException , IOException {
        Map<String, String> defaultProp = loadPropertiesToMap(
            ServerConfiguration.getServerInstance(ssoToken,
            ServerConfiguration.DEFAULT_SERVER_CONFIG));
        compareProperties(ssoToken, defaultProp, svrInstance);
        validateEncryptionKey(ssoToken);
    }
    
    /**
     * Validate the configuration properties against the valid properties
     * list.
     */
    private void validateProperties(
        Properties cfgProp,
        Map changedProp,
        Map defProp,
        String svrInstance
    ) {
        toolOutWriter.printMessage("svr-validate-ins-prop",
            new String[] {svrInstance});
        for (Enumeration e = cfgProp.propertyNames();
            e.hasMoreElements();) {
            String key = (String)e.nextElement();
            if (!validProp.containsKey(key)) {
                toolOutWriter.printWarning("svr-prop-not-in-valid-list" ,
                    new String[] {key});
                if (!defProp.containsKey(key)){
                    invalidProp.put(key, (String)cfgProp.getProperty(key));
                    toolOutWriter.printError("svr-invalid-prop",
                        new String[] {key});
                    cfgProp.remove(key);
                }
            }
        }
        verifyProperties(svrInstance, defProp, detectChangedProperties(
            cfgProp, defProp));
    }
    
    private Map detectChangedProperties(
        Properties cfgProp,
        Map defProp
    ) {
        Map chgProp = new HashMap();
        toolOutWriter.printMessage("svr-ins-prop-chg");
        toolOutWriter.printMessage(SMALL_LINE_SEP_1);
        for (Enumeration e = cfgProp.propertyNames();
            e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String value = cfgProp.getProperty(key);
            String defValue = (String)defProp.get(key);
            if (key.equals(ENC_PWD_PROPERTY) || 
                key.equals(AM_SERVICES_SECRET)) {
                value = "xxxxxxxxxxxxxxxx";
            }
            String[] params = {key, value};
            if ((defValue != null) && (defValue.length() > 0)) {
                if (!((defValue.trim()).equals(value.trim()))) {
                    chgProp.put(key, value);
                    toolOutWriter.printMessage("svr-print-prop", params);
                }
            } else {
                if ((value != null) && (value.length() > 0)) {
                    chgProp.put(key, value);
                    toolOutWriter.printMessage("svr-print-prop", params);
                }
            }
        }
        toolOutWriter.printMessage(SMALL_LINE_SEP_1);
        return chgProp;
    }
    
    private boolean verifyNotification(String svrInstance, String svrURL) {
        String notURL = null;
        notURL = SystemProperties.get(CLIENT_NOTIFICATION_URL);
        boolean valid = false;
        if (notURL != null && notURL.indexOf("%") == -1) {
            if (!notURL.contains(svrURL)) {
                valid = urlExistsInSite(svrInstance,
                    notURL, getAdminSSOToken());
            } else {
                valid = true;
            }
        }
        toolOutWriter.printStatusMsg(valid, "svr-notify-url-validation");
        return valid;
    }
    
    /**
     * Verify the changed configuration properties.
     */
    private void verifyProperties(
        String svrInstance,
        Map defProp,
        Map cProp
    ) {
        SSOToken ssoToken = null;
        String[] ins = {svrInstance};
        if (cProp.isEmpty()) {
            toolOutWriter.printError("svr-ins-prop-unswapped" , ins);
        } else {
            ssoToken = getAdminSSOToken();
            if (getServerStatus()){
                if (!verifyNotification(svrInstance,
                    getURLStrFromProperties(cProp))) {
                    String[] params = {CLIENT_NOTIFICATION_URL,
                    SystemProperties.get(CLIENT_NOTIFICATION_URL)};
                    toolOutWriter.printError("svr-invalid-value", params);
                }
            }
            if (!validateServerSiteEntry(svrInstance, ssoToken)) {
                toolOutWriter.printError("svr-belongs-to-invalid-site", ins);
            }
            if (!isPropertyMatchEntry(cProp, svrInstance)) {
                toolOutWriter.printStatusMsg(false, "svr-validating-php-uri");
                toolOutWriter.printError("svr-url-prop-mismatch",
                    new String[] {getURLStrFromProperties(cProp)});
            } else {
                toolOutWriter.printStatusMsg(true, "svr-validating-php-uri");
            }
            boolean found = false;
            try {
                if (!existsInOrganizationAlias(ssoToken,
                    (String)cProp.get(AM_SERVER_HOST))) {
                    toolOutWriter.printError("svr-entry-not-in-org-alias", ins);
                } else {
                    found = true;
                }
            } catch (SMSException sms) {
                Debug.getInstance(DEBUG_NAME).error(
                    "ServerConfigValidator.verifyProperties: " +
                    "Exception", sms);
            }
            toolOutWriter.printStatusMsg(found, "svr-org-alias-entry");
            validatePersistentSearchList(defProp, cProp, svrInstance);
        }
    }
    
    /**
     * Get the server list entry of the configured server
     */
    private boolean existInServerList(
        SSOToken ssoToken,
        String serverURL
    ) {
        boolean match = false;
        try {
            match = ServerConfiguration.isServerInstanceExist(
                ssoToken, serverURL);
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.existInServerList: " +
                "Exception", e);
        }
        return match;
    }
    
    /**
     * Connection disable property
     * Name: com.sun.am.event.connection.disable.list
     */
    private void validatePersistentSearchList(
        Map dProp,
        Map cProp,
        String svr
    ) {
        String value = null;
        boolean valid = true;
        value = (String)cProp.get(DISABLE_PERSISTENT_SEARCH);
        if (value != null && value.length() > 0 ) {
            String defValue = (String)dProp.get(DISABLE_PERSISTENT_SEARCH);
            StringTokenizer st = new StringTokenizer(defValue, ",");
            Set validValues = new HashSet();
            while (st.hasMoreTokens()) {
                validValues.add(st.nextToken());
            }
            st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                valid &= (validValues.contains(st.nextToken()));
            }
        }
        if (!valid) {
            String[] params = {DISABLE_PERSISTENT_SEARCH, value};
            toolOutWriter.printError("svr-invalid-value", params);
        }
        toolOutWriter.printStatusMsg(valid, "svr-persist-prop");
    }
    
    private boolean validateServerSiteEntry(
        String instance,
        SSOToken ssoToken
    ) {
        boolean valid = false;
        try {
            String site =
                ServerConfiguration.getServerSite(ssoToken, instance);
            if (site != null) {
                valid = SiteConfiguration.isSiteExist(ssoToken, site);
            } else {
                valid = true;
                toolOutWriter.printWarning("svr-no-site-cfg",
                    new String[] {instance});
            }
        } catch (Exception sms) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.validateServerSiteEntry: " +
                "SMS Exception", sms);
        }
        toolOutWriter.printStatusMsg(valid, "svr-validate-site-cfg");
        return valid;
    }
    
    private boolean urlExistsInSite(
        String svrInstance,
        String url,
        SSOToken ssoToken
    ) {
        boolean valid = true;
        Set<String> allSites = new HashSet<String>();
        try {
            String site = ServerConfiguration.getServerSite(
                ssoToken, svrInstance);
            allSites.add(SiteConfiguration.getSitePrimaryURL(
                ssoToken, site));
            Set failoverURLs =
                SiteConfiguration.getSiteSecondaryURLs(
                ssoToken, site);
            if ((failoverURLs != null) && !failoverURLs.isEmpty()) {
                for (Iterator<String> i = failoverURLs.iterator();
                    i.hasNext(); ) {
                    allSites.add(i.next());
                }
            }
            valid = allSites.contains(getURIFromComponents(url));
        } catch (Exception sms) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.urlExistsInSite: " +
                "SMS Exception", sms);
            valid = false;
        }
        return valid;
    }
    
    /**
     * Helper method to construct the URI from host, port, protocol
     * from the configuration properties for the server.
     */
    private String getURIFromComponents(String urlStr) {
        String uri = null;
        try {
            URL url = new URL(urlStr);
            String hostname = url.getHost();
            String port = Integer.toString(url.getPort());
            String protocol = url.getProtocol();        
            uri = protocol + "://" + hostname + ":" + port;
        } catch (MalformedURLException mfe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.getURIFromComponents: " +
                "Invalid  URL exception", mfe);
        }
        return uri;
    }
    
    private void validateEncryptionKey(SSOToken ssoToken) {
        toolOutWriter.printMessage("svr-process-enc-key");
        Map keyMap = getEncryptionKeyMap(ssoToken);
        boolean valid = true;
        if (keyMap.size() > 1) {
            valid = false;
            //Server entries have diff keys
            toolOutWriter.printError(
                "svr-entries-with-multiple-enc-key");
            for (Iterator i = keyMap.entrySet().iterator();
                i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                Set sList = (Set)entry.getValue();
                StringBuilder sBuf = new StringBuilder();
                Iterator iter = sList.iterator();
                while (iter.hasNext()) {
                    String var = (String)iter.next();
                    sBuf.append(var).append(" ");
                }
                String[] params = {(String)entry.getKey(),
                sBuf.toString()};
                toolOutWriter.printError("svr-enc-key-list", params);
            }
        }
        toolOutWriter.printStatusMsg(valid, "svr-validating-enc-key");
    }
    
    /**
     * Get the all the server lists of the configured server
     */
    private Map getEncryptionKeyMap(SSOToken ssoToken) {
        Map <String, Set<String>> keySvrMap =
            new HashMap<String, Set<String>>();
        Set<String> svrList = null;
        try {
            Set serverSet = ServerConfiguration.getServers(ssoToken);
            for (Iterator<String> items = serverSet.iterator();
                items.hasNext();) {
                String server = items.next();
                Properties sProps = ServerConfiguration.getServerInstance(
                    ssoToken, server);
                String key = sProps.getProperty(ENC_PWD_PROPERTY);
                if (keySvrMap.containsKey(key)) {
                    svrList = keySvrMap.get(key);
                    svrList.add(server);
                    keySvrMap.put(key, svrList);
                } else {
                    Set<String> invSvrList = new HashSet<String>();
                    invSvrList.add(server);
                    if (key != null) {
                        keySvrMap.put(key, invSvrList);
                    } else {
                        keySvrMap.put("missing-enc-key", invSvrList);
                    }
                }
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.getEncryptionKeyMap: " +
                "Exception", e);
        }
        return keySvrMap;
    }
    
    private Map validateDSServers(String path) {
        boolean valid = false;
        String[] params = {path};
        Map<String, String> dsInstance = null;
        try {
            toolOutWriter.printMessage("svr-get-boot-info");
            dsInstance = getBootServer((Map)getBootServers(path));
            if (dsInstance.isEmpty()) {
                //ERROR - DS is not running
                toolOutWriter.printError("ds-no-ins-to-connect");
            } else {
                valid = true;
            }
        } catch (UnsupportedEncodingException uee) {
            toolOutWriter.printError("svr-boot-parse-err", params);
        } catch (IOException ioe) {
            toolOutWriter.printError("svr-boot-not-found", params);
        } catch (Exception e) {
            toolOutWriter.printError("svr-boot-read-err", params);
            //ERROR - Cannot read BOOT FILE
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigValidator.validateDSServers : " +
                "Cannot parse the bootstrap file at :" +
                path, e);
            
        }
        return dsInstance;
    }
    
    private Map getBootServer(Map<String, Map> bServers) {
        boolean match = false;
        Map <String, String> dsInstance = new HashMap<String, String>();
        for (Iterator<String> it = bServers.keySet().iterator();
            it.hasNext() && !match; ) {
            String dsInstanceKey = it.next();
            dsInstance = (Map)bServers.get(dsInstanceKey);
            String[] params = {dsInstance.get(DS_HOST),
                dsInstance.get(DS_PORT)};
            toolOutWriter.printMessage("ds-ins-validation",
                new String[] {dsInstanceKey});
            if (isDSRunning(dsInstance)) {
                toolOutWriter.printMessage("ds-connect-at-suffix",
                    new String[] {dsInstance.get(DS_BASE_DN)});
                if (isValidSuffix(dsInstance)) {
                    match = true;
                } else {
                    toolOutWriter.printStatusMsg(false, "ds-sfx-connect");
                    //Invalid DS Suffix
                    toolOutWriter.printError("ds-failed-connect-at-suffix",
                        new String[] {dsInstance.get(DS_BASE_DN)});
                }
            } else {
                toolOutWriter.printStatusMsg(false, "ds-ins-connect");
                toolOutWriter.printError("ds-not-running" , params);
                dsInstance.clear();
            }
        }
        return dsInstance;
    }
    
    private String getServerInstanceName(Map dsInstance) {
        return (String)dsInstance.get(SERVER_INSTANCE);
    }
    
    private boolean isPropertyMatchEntry(Map prop, String svr) {
        return getURLStrFromProperties(prop).equalsIgnoreCase(svr);
    }   
}
