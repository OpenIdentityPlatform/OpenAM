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
 * $Id: SessionServerTimeConstraintsTest.java.v 1.1 
 * 2007/03/06 12:40:00 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests session timeout conditions for various levels and conflict
 * resolution between global/realm/role/filtered role/user level for those
 * conditions.
 */
public class SessionServerTimeConstraintsTest extends TestCommon {

    private SSOToken admintoken;
    private SSOToken usertoken;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private String loginURL;
    private String logoutURL;
    private String amadmURL;
    private String maxSessionTime;
    private String maxIdleTime;
    private String maxCachingTime;
    private String userName = "sstctuser";
    private List list;
    private boolean bVal = false;

    /**
     * Initialize global attributes in the construtor
     */
    public SessionServerTimeConstraintsTest() 
    throws Exception {
        super("SessionServerTimeConstraintsTest");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        smsc = new SMSCommon(admintoken);
    }

    /**
     * Initialization method. This method:
     * (a) Creates a user
     * (b) Sets default session attributes as global variables
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);
        try {
            logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                    "/UI/Logout";
            log(Level.FINEST, "setup", "logoutURL:" + logoutURL);

            Map map = new HashMap();
            Set set = new HashSet();
            set.add(userName);
            map.put("sn", set);
            set = new HashSet();
            set.add(userName);
            map.put("cn", set);
            set = new HashSet();
            set.add(userName);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("inetuserstatus", set);
            idmc.createIdentity(admintoken, realm, IdType.USER, userName, map);

            set = smsc.getAttributeValueFromSchema(
                    "iPlanetAMSessionService",
                    "iplanet-am-session-max-session-time", "Dynamic");
            for (Iterator itr = set.iterator(); itr.hasNext();)
                maxSessionTime = (String)itr.next();
            log(Level.FINEST, "setup", "maxSessionTime:" + maxSessionTime);

            set = smsc.getAttributeValueFromSchema(
                    "iPlanetAMSessionService",
                    "iplanet-am-session-max-idle-time", "Dynamic");
            for (Iterator itr = set.iterator(); itr.hasNext();)
                maxIdleTime = (String)itr.next();
            log(Level.FINEST, "setup", "maxIdleTime:" + maxIdleTime);

            set = smsc.getAttributeValueFromSchema(
                    "iPlanetAMSessionService",
                    "iplanet-am-session-max-caching-time", "Dynamic");
            for (Iterator itr = set.iterator(); itr.hasNext();)
                maxCachingTime = (String)itr.next();
            log(Level.FINEST, "setup", "maxCachingTime:" + maxCachingTime);
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }

    /**
     * Validates max session time set in global session service through a user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMaxSessionTimeGlobal()
    throws Exception {
        entering("testMaxSessionTimeGlobal", null);
        try {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("2");
            map.put("iplanet-am-session-max-session-time", set);
            set = new HashSet();
            set.add("1");
            map.put("iplanet-am-session-max-idle-time", set);
            set = new HashSet();
            set.add("1");
            map.put("iplanet-am-session-max-caching-time", set);
            smsc.updateGlobalServiceDynamicAttributes(
                    "iPlanetAMSessionService", map);

            Set setVal = smsc.getAttributeValueFromSchema(
                    "iPlanetAMSessionService",
                    "iplanet-am-session-max-session-time", "Dynamic");
            log(Level.FINEST, "evaluateNewSessionAttribute", "setVal: " +
                    setVal);

            if (!setVal.contains("2"))
                assert false;

            usertoken = getToken(userName, userName, basedn);
            Thread.sleep(55000);
            usertoken.setProperty("MyProperty", "val1");
            String strProperty = usertoken.getProperty("MyProperty");
            log(Level.FINEST, "evaluateNewSessionAttribute",
                    "Session property value: " + strProperty);
            assert (strProperty.equals("val1"));

            Thread.sleep(100000);
            bVal = false;
            try {
                strProperty = usertoken.getProperty("MyProperty");
            } catch(SSOException e) {
                log(Level.FINEST, "testMaxSessionTimeGlobal",
                        "Set property failed. Expected behaviour");
                bVal = true;
            }
            log(Level.FINEST, "testMaxSessionTimeGlobal", "bVal: " + bVal);
            assert (bVal);
        } catch(Exception e) {
            log(Level.SEVERE, "testMaxSessionTimeGlobal", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testMaxSessionTimeGlobal");
    }

    /**
     * Validates max idle time set in global session service through a user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testMaxSessionTimeGlobal"})
    public void testMaxIdleTimeGlobal()
    throws Exception {
        entering("testMaxIdleTimeGlobal", null);
        try {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("5");
            map.put("iplanet-am-session-max-session-time", set);
            smsc.updateGlobalServiceDynamicAttributes(
                    "iPlanetAMSessionService", map);
            Set setVal = smsc.getAttributeValueFromSchema(
                    "iPlanetAMSessionService",
                    "iplanet-am-session-max-session-time", "Dynamic");
            log(Level.FINEST, "evaluateNewSessionAttribute", "setVal: " +
                    setVal);
            if (!setVal.contains("5"))
                assert false;
            usertoken = getToken(userName, userName, basedn);
            Thread.sleep(155000);
            assert (!validateToken(usertoken)); 
        } catch(Exception e) {
            log(Level.SEVERE, "testMaxIdleTimeGlobal", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            resetGlobalSessionAttributes();
        }
        exiting("testMaxIdleTimeGlobal");
    }

    /**
     * Validates max idle time set in global session service through a user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testMaxIdleTimeGlobal"})
    public void testInhertianceRealm()
    throws Exception {
        entering("testInhertianceRealm", null);
        try {
            smsc.assignDynamicServiceRealm("iPlanetAMSessionService", realm,
                    null);
            Map map = smsc.getDynamicServiceAttributeRealm(
                    "iPlanetAMSessionService", realm);
            log(Level.FINEST, "testInhertianceRealm", "Map: " + map);
            Set set = (Set)map.get("iplanet-am-session-max-session-time");
            if (!set.contains(maxSessionTime))
                assert false;
            set = (Set)map.get("iplanet-am-session-max-idle-time");
            if (!set.contains(maxIdleTime))
                assert false;
            set = (Set)map.get("iplanet-am-session-max-caching-time");
            if (!set.contains(maxCachingTime))
                assert false;
        } catch(Exception e) {
            log(Level.SEVERE, "testInhertianceRealm", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            smsc.unassignDynamicServiceRealm("iPlanetAMSessionService", realm);
        }
        exiting("testInhertianceRealm");
    }

    /**
     * Validates max session time set in session service in a realm through a
     * user. Also validates conflict resolution between global/realm level.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testInhertianceRealm"})
    public void testMaxSessionTimeRealm()
    throws Exception {
        entering("testMaxSessionTimeRealm", null);
        try {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("2");
            map.put("iplanet-am-session-max-session-time", set);
            set = new HashSet();
            set.add("1");
            map.put("iplanet-am-session-max-idle-time", set);
            set = new HashSet();
            set.add("1");
            map.put("iplanet-am-session-max-caching-time", set);
            smsc.assignDynamicServiceRealm("iPlanetAMSessionService", realm,
                    map);

            usertoken = getToken(userName, userName, basedn);
            Thread.sleep(55000);
            usertoken.setProperty("MyProperty", "val1");
            String strProperty = usertoken.getProperty("MyProperty");
            log(Level.FINEST, "evaluateNewSessionAttribute",
                    "Session property value: " + strProperty);
            assert (strProperty.equals("val1"));

            Thread.sleep(100000);
            bVal = false;
            try {
                strProperty = usertoken.getProperty("MyProperty");
            } catch(SSOException e) {
                log(Level.FINEST, "testMaxSessionTimeGlobal",
                        "Set property failed. Expected behaviour");
                bVal = true;
            }
            log(Level.FINEST, "testMaxSessionTimeGlobal", "bVal: " + bVal);
            assert (bVal);
            
            smsc.unassignDynamicServiceRealm("iPlanetAMSessionService", realm);
        } catch(Exception e) {
            log(Level.SEVERE, "testMaxSessionTimeRealm", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("testMaxSessionTimeRealm");
    }

    /**
     * Validates max idle time set in session service in a realm through a 
     * user. Also validates conflict resolution between global/realm level.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testMaxSessionTimeRealm"})
    public void testMaxIdleTimeRealm()
    throws Exception {
        entering("testMaxIdleTimeRealm", null);
        try {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("5");
            map.put("iplanet-am-session-max-session-time", set);
            set = new HashSet();            
            set.add("1");
            map.put("iplanet-am-session-max-idle-time", set);
            set = new HashSet();
            set.add("1");
            map.put("iplanet-am-session-max-caching-time", set);
            smsc.assignDynamicServiceRealm("iPlanetAMSessionService", realm,
                    map);
            usertoken = getToken(userName, userName, basedn);
            Thread.sleep(155000);
            assert (!validateToken(usertoken));
        } catch(Exception e) {
            log(Level.SEVERE, "testMaxIdleTimeRealm", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testMaxIdleTimeRealm");
    }

    /**
     * Cleanup method. This method:
     * (a) Delete user
     * (b) Delete sessions service from realm.
     * (c) Reset global session service attributes to default values
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINEST, "cleanup", "UserName:" + userName);
            Reporter.log("UserName:" + userName);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, userName);
            smsc.unassignDynamicServiceRealm("iPlanetAMSessionService", realm);
            resetGlobalSessionAttributes();
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
        	destroyToken(admintoken);
        }
        exiting("cleanup");
    }

    /**
     * Sets global session (max session, idle and caching)  attributes to
     * default values.
     */
    private void resetGlobalSessionAttributes()
    throws Exception {
        entering("resetGlobalSessionAttributes", null);
        try {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add(maxSessionTime);
            map.put("iplanet-am-session-max-session-time", set);
            set = new HashSet();
            set.add(maxIdleTime);
            map.put("iplanet-am-session-max-idle-time", set);
            set = new HashSet();
            set.add(maxCachingTime);
            map.put("iplanet-am-session-max-caching-time", set);
            smsc.updateGlobalServiceDynamicAttributes(
                    "iPlanetAMSessionService", map);
        } catch(Exception e) {
            log(Level.SEVERE, "resetGlobalSessionAttributes", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("resetGlobalSessionAttributes");
    }
}
