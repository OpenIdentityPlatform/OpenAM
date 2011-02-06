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
 * $Id: SMNotificationTests.java,v 1.4 2009/01/27 00:08:40 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.notification;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class tests SM notifications for 
 * 1. Auth Service
 * 2. Logging Service
 * 3. Platform Service
 * 4. Session Service
 */
public class SMNotificationTests extends TestCommon implements ServiceListener {
    
    String listenerID;
    int eventID;
    private Map<String, String> configMap;
    SSOToken token;
    ServiceConfigManager scm;
    ServiceConfig sc;
    String strServiceName;
    Map attrMap;
    String attrToModify;
    String valToModify;
    boolean result = false;
    
    /** Creates a new instance of SMNotificationTests */
    public SMNotificationTests() {
        super("SMNotificationTests");
    }
    
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
            throws Exception {
        entering("setup", null);
        try {
            token = getToken(adminUser, adminPassword, realm);
            log(Level.FINEST, "setup", "Getting token");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }
    
    /**
     * It tests notification generation for identity modification event
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void PlatformServiceModificationTest()
    throws Exception {
        result = false;
        try {
            strServiceName = "iPlanetAMPlatformService";
            scm = new ServiceConfigManager(token, strServiceName, "1.0");
            listenerID = scm.addListener(this);
            log(Level.FINEST, "PlatformServiceModificationTest", "get " +
                    "GlobalConfig");
            sc = scm.getGlobalConfig(null);
            Map scAttrMap = sc.getAttributes();
            log(Level.FINEST, "PlatformServiceModificationTest", "Map " +
                    "returned from globlal config is: " + scAttrMap);
            Set oriLocaleValues = null;
            oriLocaleValues = (Set) scAttrMap.get
                    ("iplanet-am-platform-cookie-domains");
            Set newLocaleValues = oriLocaleValues;
            newLocaleValues.add("not.test.com");
            Map newLocaleValuesMap = new HashMap();
            newLocaleValuesMap.put("iplanet-am-platform-cookie-domains", 
                    newLocaleValues);
            sc.setAttributes(newLocaleValuesMap);
            log(Level.FINEST, "PlatformServiceModificationTest", "Replaced " +
                    "the attribute");
            Thread.sleep(notificationSleepTime);
            Map scAttrMapNew = sc.getAttributes();
            log(Level.FINEST, "PlatformServiceModificationTest", 
                    "Read MAP again" + scAttrMapNew);
            if (scAttrMapNew.get("iplanet-am-platform-cookie-domains").
                    equals(newLocaleValues)) {
                log(Level.FINEST, "PlatformServiceModificationTest", 
                        "Returned values are same as set");
                result = true;
            } else {
                log(Level.FINEST, "PlatformServiceModificationTest", 
                        "Returned values are NOT same as set");
                assert false;
            }
            Map oriLocaleValuesMap = new HashMap();
            oriLocaleValuesMap.put("iplanet-am-platform-cookie-domains", 
                    oriLocaleValues);
            sc.setAttributes(oriLocaleValuesMap);
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "PlatformServiceModificationTest", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * It tests notification for logging service.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void LoggingServiceModificationTest()
    throws Exception {
        result = false;
        try {
            strServiceName = "iPlanetAMLoggingService";
            scm = new ServiceConfigManager(token, strServiceName, "1.0");
            listenerID = scm.addListener(this);
            log(Level.FINEST, "LoggingServiceModificationTest", "get " +
                    "ServiceConfig");
            sc = scm.getGlobalConfig(null);
            Map scAttrMap = sc.getAttributes();
            log(Level.FINEST, "LoggingServiceModificationTest", "Map " +
                    "returned from globlal config is: " + scAttrMap);
            Set oriLoggingAttrValues = null;
            oriLoggingAttrValues = (Set) scAttrMap.get
                    ("iplanet-am-logging-num-hist-file");
            Set newLocaleValues = new HashSet();
            newLocaleValues.add("4");
            Map newLocaleValuesMap = new HashMap();
            newLocaleValuesMap.put("iplanet-am-logging-num-hist-file", 
                    newLocaleValues);
            sc.setAttributes(newLocaleValuesMap);
            log(Level.FINEST, "LoggingServiceModificationTest", "Replaced " +
                    "the attribute");
            Thread.sleep(notificationSleepTime);
            Map scAttrMapNew = sc.getAttributes();
            log(Level.FINEST, "LoggingServiceModificationTest", "Read MAP " +
                    "again" + scAttrMapNew);
            if (scAttrMapNew.get("iplanet-am-logging-num-hist-file").equals( 
                    newLocaleValues)) {
                log(Level.FINEST, "LoggingServiceModificationTest", 
                        "Returned values are same as set");
                result = true;
            } else {
                log(Level.FINEST, "LoggingServiceModificationTest", 
                        "Returned values are NOT same as set");
                assert false;
            }
            Map oriLoggingAttrValuesMap = new HashMap();
            oriLoggingAttrValuesMap.put("iplanet-am-logging-num-hist-file", 
                    oriLoggingAttrValues);
            sc.setAttributes(oriLoggingAttrValuesMap);
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "LoggingServiceModificationTest", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * It tests notification for Authentication service update at the org level.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void AuthServiceOrgModificationTest()
    throws Exception {
        result = false;
        try {
            strServiceName = "iPlanetAMAuthService";
            scm = new ServiceConfigManager(token, strServiceName, "1.0");
            listenerID = scm.addListener(this);
            log(Level.FINEST, "AuthServiceOrgModificationTest", "get " +
                    "ServiceConfig");
            sc = scm.getOrganizationConfig(realm, null);
            Map scAttrMap = sc.getAttributes();
            log(Level.FINEST, "AuthServiceOrgModificationTest", "Map " +
                    "returned from Org config is: " + scAttrMap);
            Set oriAuthAttrValues = null;
            oriAuthAttrValues = (Set) scAttrMap.get
                    ("iplanet-am-auth-persistent-cookie-mode");
            log(Level.FINEST, "AuthServiceOrgModificationTest", "Value of " +
                    "iplanet-am-auth-persistent-cookie-mode: " + 
                    oriAuthAttrValues);
            Set newLocaleValues = new HashSet();
            newLocaleValues.add("true");
            Map newLocaleValuesMap = new HashMap();
            newLocaleValuesMap.put("iplanet-am-auth-persistent-cookie-mode", 
                    newLocaleValues);
            log(Level.FINEST, "AuthServiceOrgModificationTest", "Set " +
                    "iplanet-am-auth-persistent-cookie-mode to " + 
                    newLocaleValuesMap);
            sc.setAttributes(newLocaleValuesMap);
            log(Level.FINEST, "AuthServiceOrgModificationTest", "Replaced " +
                    "the attribute");
            Thread.sleep(notificationSleepTime);
            Map scAttrMapNew = sc.getAttributes();
            log(Level.FINEST, "AuthServiceOrgModificationTest", "Read MAP " +
                    "again" + scAttrMapNew);
            if (scAttrMapNew.get("iplanet-am-auth-persistent-cookie-mode").
                    equals(newLocaleValues)) {
                log(Level.FINEST, "AuthServiceOrgModificationTest", 
                        "Returned values are same as set");
                result = true;
            } else {
                log(Level.FINEST, "AuthServiceOrgModificationTest", 
                        "Returned values are NOT same as set");
                assert false;
            }
            Map oriAuthAttrValuesMap = new HashMap();
            oriAuthAttrValuesMap.put("iplanet-am-auth-persistent-cookie-mode", 
                    oriAuthAttrValues);
            sc.setAttributes(oriAuthAttrValuesMap);
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "AuthServiceOrgModificationTest", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * It tests notification for Authentication service update at global level.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void AuthServiceGlobalModificationTest()
    throws Exception {
        result = false;
        try {
            strServiceName = "iPlanetAMAuthService";
            scm = new ServiceConfigManager(token, strServiceName, "1.0");
            listenerID = scm.addListener(this);
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "get " +
                    "ServiceConfig");
            sc = scm.getGlobalConfig(null);
            Map scAttrMap = sc.getAttributes();
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "Map r" +
                    "eturned from Global config is: " + scAttrMap);
            Set oriAuthAttrValues = null;
            oriAuthAttrValues = (Set) scAttrMap.get
                    ("sunRemoteAuthSecurityEnabled");
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "Value of " +
                    "sunRemoteAuthSecurityEnabled: " + 
                    oriAuthAttrValues);
            Set newLocaleValues = new HashSet();
            newLocaleValues.add("true");
            Map newLocaleValuesMap = new HashMap();
            newLocaleValuesMap.put("sunRemoteAuthSecurityEnabled", 
                    newLocaleValues);
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "Set " +
                    "sunRemoteAuthSecurityEnabled to " + 
                    newLocaleValuesMap);
            sc.setAttributes(newLocaleValuesMap);
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "Replaced " +
                    "the attribute");
            Thread.sleep(notificationSleepTime);
            Map scAttrMapNew = sc.getAttributes();
            log(Level.FINEST, "AuthServiceGlobalModificationTest", "Read " +
                    "MAP again" + scAttrMapNew);
            if (scAttrMapNew.get("sunRemoteAuthSecurityEnabled").equals( 
                    newLocaleValues)) {
                log(Level.FINEST, "AuthServiceGlobalModificationTest", 
                        "Returned values are same as set");
                result = true;
            } else {
                log(Level.FINEST, "AuthServiceGlobalModificationTest", 
                        "Returned values are NOT same as set");
                assert false;
            }
            Map oriAuthAttrValuesMap = new HashMap();
            oriAuthAttrValuesMap.put("sunRemoteAuthSecurityEnabled", 
                    oriAuthAttrValues);
            sc.setAttributes(oriAuthAttrValuesMap);
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "AuthServiceGlobalModificationTest", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }

    /**
     * It tests notification for Session service update at the global level.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SessionServiceGlobalModificationTest()
    throws Exception {
        result = false;
        try {
            strServiceName = "iPlanetAMSessionService";
            scm = new ServiceConfigManager(token, strServiceName, "1.0");
            listenerID = scm.addListener(this);
            log(Level.FINEST, "setup", "get ServiceConfig");
            sc = scm.getGlobalConfig(null);
            Map scAttrMap = sc.getAttributes();
            log(Level.FINEST, "SessionServiceModificationTest", "Map " +
                    "returned from globlal config is: " + scAttrMap);
            Set oriSessionAttrValues = null;
            oriSessionAttrValues = (Set) scAttrMap.get
                    ("iplanet-am-session-max-session-list-size");
            Set newLocaleValues = new HashSet();
            newLocaleValues.add("200");
            Map newLocaleValuesMap = new HashMap();
            newLocaleValuesMap.put("iplanet-am-session-max-session-list-size", 
                    newLocaleValues);
            sc.setAttributes(newLocaleValuesMap);
            log(Level.FINEST, "SessionServiceModificationTest", "Replaced " +
                    "the attribute");
            Thread.sleep(notificationSleepTime);
            Map scAttrMapNew = sc.getAttributes();
            log(Level.FINEST, "SessionServiceModificationTest", "Read MAP " +
                    "again" + scAttrMapNew);
            if (scAttrMapNew.get("iplanet-am-session-max-session-list-size").
                    equals( newLocaleValues)) {
                log(Level.FINEST, "SessionServiceModificationTest", 
                        "Returned values are same as set");
                result = true;
            } else {
                log(Level.FINEST, "SessionServiceModificationTest", 
                        "Returned values are NOT same as set");
                assert false;
            }
            Map oriSessionAttrValuesMap = new HashMap();
            oriSessionAttrValuesMap.put
                    ("iplanet-am-session-max-session-list-size", 
                    oriSessionAttrValues);
            sc.setAttributes(oriSessionAttrValuesMap);
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "SessionServiceModificationTest", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }
       
    /**
     * Removes the Event listener & destroys the token. 
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        try {
            scm.removeListener(listenerID);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(token);
        }
    }
        
    /**
     * Implementation of schemaChanged
     */
    public void schemaChanged(String serviceName, String version) {
        log(Level.FINEST, "schemaChanged", "Received Notification: Schema " +
                "changed: " + serviceName + "(" + version + ")");
        result = true;
    }
    
    /**
     * Implementation of globalConfigChanged
     */
    public void globalConfigChanged(String s, String v, String g,
            String c, int t) {
        log(Level.FINEST, "globalConfigChanged", "Received Notification: " +
                "Global Config Changed: " +
                s + "(" + v + ")" + " Group: " + g + " Component: " + c +
                " EventType: " + t);
        if (s.equals(strServiceName)) {
            log(Level.FINEST, "globalConfigChanged", "Received correct " +
                    "notification for service modified");
            result = true;
        } 
    }
    
    /**
     * Implementation of organizationConfigChanged
     */
    public void organizationConfigChanged(String s, String v,
            String o, String g, String c, int t) {
        log(Level.FINEST, "organizationConfigChanged","Org Config Changed: " + 
                s + "(" + v + ")" + " Org: " + o + " Group: " + g + 
                " Component: " + c + " EventType: " + t);
        result = true;
    }
}
