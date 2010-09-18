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
 * $Id: UserSessionConstraints.java,v 1.7 2009/01/27 00:16:38 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.DelegationCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class is used to test 'Active Sessions' dynamic attribute for 
 * two users at the User level. All the tests depend on 
 * constraint - Resulting behavior if session quota exhausted 
 *                which can be DENY_ACESS/DESTROY_OLD_SESSION 
 */
public class UserSessionConstraints extends TestCommon {

    private SSOToken admintoken;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private DelegationCommon delc;
    private String testUserWithSrvc = "sessConsTestWithSrvc";
    private String testUserWithoutSrvc = "sessConsTestWithoutSrvc";
    private String resultBehavior;
    private boolean consTurnedOn = false;
    private boolean dynSrvcRealmAssigned = false;
    private boolean cleanedUp = false;
    
    /**
     * SessionConstraints Constructor
     * Creates admintoken.
     *
     * @throws java.lang.Exception
     */
    public UserSessionConstraints() throws Exception {
        super("UserSessionConstraints");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();  
        delc = new DelegationCommon("UserSessionConstraints");
        smsc = new SMSCommon(admintoken);
    }

    /**
     * Initialization method. Setup:
     * (a) Creates adminuser, user Identities 
     * (b) Parameter inheritancelevel has values Global/Realm/User
     * (c) Validates that session service is available at each inheritance
     *     level, and if not assigns the service and sets value of active 
     *     number of sessions to "1"
     * 
     * @throws java.lang.Exception
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);
        try {
            Set set = smsc.getAttributeValueFromSchema(
                    SessionConstants.SESSION_SRVC,
                    SessionConstants.ENABLE_SESSION_CONST, 
                    SessionConstants.GLOBAL_SRVC_TYPE);
            Iterator itr = set.iterator();
            String quotaConst = (String) itr.next();            
            set = smsc.getAttributeValueFromSchema(SessionConstants.SESSION_SRVC,
                    SessionConstants.RESULTING_BEHAVIOR, 
                    SessionConstants.GLOBAL_SRVC_TYPE);
            itr = set.iterator();
            resultBehavior = (String) itr.next();     
            log(Level.FINE, "setup", "Resulting behavior if session quota " +
                    "exhausted is set to: " + resultBehavior);  
            if (quotaConst.equals("OFF")) {
                Map attrMap = new HashMap();
                set.clear();
                set.add("ON");
                attrMap.put(SessionConstants.ENABLE_SESSION_CONST, set);
                smsc.updateSvcSchemaAttribute(SessionConstants.SESSION_SRVC,
                        attrMap, SessionConstants.GLOBAL_SRVC_TYPE);                 
                consTurnedOn = true;
            }
            idmc.createDummyUser(admintoken, realm, "", testUserWithSrvc);
            log(Level.FINE,"setup", "Created user " + 
                    testUserWithSrvc + " identity");

            idmc.createDummyUser(admintoken, realm, "", testUserWithoutSrvc);
            log(Level.FINE,"setup", "Created user " + 
                    testUserWithoutSrvc + " identity");
            Map quotamap = new HashMap();
            set = new HashSet();
            set.add("1");
            quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
            delc.assignServiceToUser(admintoken, testUserWithSrvc, 
                        SessionConstants.SESSION_SRVC, quotamap, realm);
            log(Level.FINEST, "setup", "Session Service Successfully " +
            		"assigned to user " + testUserWithSrvc);
        } catch(Exception e) {
            cleanup();
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
  
    /**
     * Tests the following case: 
     * (a) Max session quota set to "1" for one user with session service
     * (b) Session service is not assigned for other user at user level
     * (c) Set resulting behavior if session quota exhausted to DENY_ACCESS
     * (d) Validates that only one session can be created for user with
     *     session service and new session is denied access
     * (e) Validates that more than one session can be created for user 
     *     without session service 
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testQuotaDAWithSessionSrvcAtGlobal()
    throws Exception {
        entering("testQuotaDAWithSessionSrvcAtGlobal", null);
        SSOToken usrtokenWithSrvcOrig = null;
        SSOToken usrtokenWithSrvcNew = null;
        SSOToken usrtokenWithoutSrvcOrig = null;
        SSOToken usrtokenWithoutSrvcNew = null;
        log(Level.FINE, "testQuotaDAWithSessionSrvcAtGlobal",
                "This testcase validates session quota for one user with session " +
                "service at user level and for other user at global level when " +
                "resulting behavior set to DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for one user with session service at user level and for other " +
                "user at global level when resulting behavior set to DENY_ACCESS");
        try {
            if (smsc.isServiceAssigned(SessionConstants.SESSION_SRVC, realm)) {
                smsc.unassignDynamicServiceRealm(SessionConstants.SESSION_SRVC,
                        realm);
            }
            Set set = smsc.getAttributeValueFromSchema(
                    SessionConstants.SESSION_SRVC,
                    SessionConstants.SESSION_QUOTA_ATTR,
                    SessionConstants.DYNAMIC_SRVC_TYPE);
            Iterator itr = set.iterator();
            String globalActiveSessions = (String)itr.next();
            if (globalActiveSessions.equals("1")) {
                set = new HashSet();
                set.add("5");
                Map quotamap = new HashMap();
                quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
                smsc.updateGlobalServiceDynamicAttributes(
                        SessionConstants.SESSION_SRVC, quotamap);
                
                set = smsc.getAttributeValueFromSchema(
                        SessionConstants.SESSION_SRVC, 
                        SessionConstants.SESSION_QUOTA_ATTR, 
                        SessionConstants.DYNAMIC_SRVC_TYPE);
                itr = set.iterator();
                globalActiveSessions = (String)itr.next();
            }
            assert !globalActiveSessions.equals("1");
            if (resultBehavior.equals("DENY_ACCESS")) {
                usrtokenWithoutSrvcOrig = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithoutSrvcNew = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithSrvcOrig = getToken(testUserWithSrvc, 
                                        testUserWithSrvc, basedn);
                try {
                    usrtokenWithSrvcNew = getToken(testUserWithSrvc,
                            testUserWithSrvc, basedn);
                    log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtGlobal", 
                            "ERROR: Deny access case for user failed ");
                    assert false;
                } 
                catch (Exception e) {
                	log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtGlobal",
                                "Cannot create new token for user: "
                    		  + testUserWithSrvc + "\n " + e.getMessage());
                        e.printStackTrace();
                }
                log(Level.FINE, "testQuotaDAWithSessionSrvcAtGlobal", 
                        "Original token and new token are valid here");
                assert validateToken(usrtokenWithoutSrvcOrig);
                assert validateToken(usrtokenWithoutSrvcNew);
                assert validateToken(usrtokenWithSrvcOrig);
                assert !validateToken(usrtokenWithSrvcNew);
            } else {
                log(Level.FINE, "testQuotaDAWithSessionSrvcAtGlobal",
                        "Resulting behaviour attribute" +
                        "do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour attribute " +
                        "do not apply for this testcase");
            }
        } catch (Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtGlobal",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (!cleanedUp) {
                destroyToken(usrtokenWithoutSrvcOrig);
                destroyToken(usrtokenWithoutSrvcNew);
                destroyToken(usrtokenWithSrvcOrig);
                destroyToken(usrtokenWithSrvcNew);     
            }
        }
        exiting("testQuotaDAWithSessionSrvcAtGlobal");
    }

    /**
     * Tests the following case: 
     * (a) Max session quota set to "1" for one user with session service
     * (b) Session service is not assigned for other user at user level
     * (c) Set resulting behavior if session quota exhausted to DENY_ACCESS
     * (d) Validates that only one session can be created for user with
     *     session service and new session is denied access
     * (e) Validates that more than one session can be created for user 
     *     without session service 
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testQuotaDAWithSessionSrvcAtGlobal"})
    public void testQuotaDOSWithSessionSrvcAtGlobal()
    throws Exception {
        entering("testQuotaDOSWithSessionSrvcAtGlobal", null);
        SSOToken usrtokenWithSrvcOrig = null;
        SSOToken usrtokenWithSrvcNew = null;
        SSOToken usrtokenWithoutSrvcOrig = null;
        SSOToken usrtokenWithoutSrvcNew = null;
        log(Level.FINE, "testQuotaDOSWithSessionSrvcAtGlobal",
                "This testcase validates session quota for one user with session" +
                "service at user level and for other user at global level when " +
                "resulting behavior set to DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for one user with session service at user level and for other " +
                "user at global level when resulting behavior set to " +
                "DESTROY_OLD_SESSION");
        try {
            if (resultBehavior.equals("DESTROY_OLD_SESSION")) {
                usrtokenWithoutSrvcOrig = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithoutSrvcNew = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithSrvcOrig = getToken(testUserWithSrvc, 
                                        testUserWithSrvc, basedn);
                usrtokenWithSrvcNew = getToken(testUserWithSrvc,
                            testUserWithSrvc, basedn);
                log(Level.FINE, "testQuotaDOSWithSessionSrvcAtGlobal", 
                        "Original token is valid and new " +
                        "token is valid here");
                assert validateToken(usrtokenWithoutSrvcOrig);
                assert validateToken(usrtokenWithoutSrvcNew);
                assert validateToken(usrtokenWithSrvcNew);
            } else {
                log(Level.FINE, "testQuotaDOSWithSessionSrvcAtGlobal",
                        "Resulting behaviour attribute" +
                        " do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour attribute " +
                        "do not apply for this testcase");
            }
        } catch (Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testQuotaDOSWithSessionSrvcAtGlobal",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (!cleanedUp) {
                destroyToken(usrtokenWithoutSrvcOrig);
                destroyToken(usrtokenWithoutSrvcNew);
                destroyToken(usrtokenWithSrvcNew);     
            }
        }
        exiting("testQuotaDOSWithSessionSrvcAtGlobal");
    }

    /**
     * Tests the following case: 
     * (a) Max session quota set to "1" for one user with session service
     * (b) Session service is not assigned for other user at user level
     * (c) Set resulting behavior if session quota exhausted to DENY_ACCESS
     * (d) Validates that only one session can be created for user with
     *     session service and new session is denied access
     * (e) Validates that more than one session can be created for user 
     *     without session service 
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testQuotaDAWithSessionSrvcAtRealm()
    throws Exception {
        entering("testQuotaDAWithSessionSrvcAtRealm", null);
        SSOToken usrtokenWithSrvcOrig = null;
        SSOToken usrtokenWithSrvcNew = null;
        SSOToken usrtokenWithoutSrvcOrig = null;
        SSOToken usrtokenWithoutSrvcNew = null;
        log(Level.FINE, "testQuotaDAWithSessionSrvcAtRealm",
                "This testcase validates session quota for one user with session" +
                "service at user level and for other user at realm level when " +
                "resulting behavior set to DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for one user with session service at user level and for other " +
                "user at realm level when resulting behavior set to DENY_ACCESS");        
        try {
            if (smsc.isServiceAssigned(SessionConstants.SESSION_SRVC, realm)) {
                smsc.unassignDynamicServiceRealm(SessionConstants.SESSION_SRVC,
                        realm);
            }
            Map quotamap = new HashMap();
            Set set = new HashSet();
            set.add("5");
            quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);  
            smsc.assignDynamicServiceRealm(SessionConstants.SESSION_SRVC, 
                    realm, quotamap);
            dynSrvcRealmAssigned = true;
            Map map = new HashMap();
            map = smsc.getDynamicServiceAttributeRealm(
                    SessionConstants.SESSION_SRVC, realm);
            set = (Set)map.get(SessionConstants.SESSION_QUOTA_ATTR);
            Iterator iter = set.iterator();
            String realmActiveSessions = (String)iter.next();
            assert !realmActiveSessions.equals("1");
            if (resultBehavior.equals("DENY_ACCESS")) {
                usrtokenWithoutSrvcOrig = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithoutSrvcNew = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithSrvcOrig = getToken(testUserWithSrvc, 
                                        testUserWithSrvc, basedn);
                try {
                    usrtokenWithSrvcNew = getToken(testUserWithSrvc,
                            testUserWithSrvc, basedn);
                    log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtRealm",
                            "ERROR: Deny access case for user failed ");
                    assert false;
                } 
                catch (Exception e) {
                      log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtRealm",
                              "Cannot create new token for user: "
                    		  + testUserWithSrvc + "\n " + e.getMessage());
                        e.printStackTrace();
                }
                log(Level.FINE, "testQuotaDAWithSessionSrvcAtRealm", 
                        "Original token is valid and new " +
                        "token is valid here");
                assert validateToken(usrtokenWithoutSrvcOrig);
                assert validateToken(usrtokenWithoutSrvcNew);
                assert validateToken(usrtokenWithSrvcOrig);
                assert !validateToken(usrtokenWithSrvcNew);
            } else {
                log(Level.FINE, "testQuotaDAWithSessionSrvcAtRealm",
                        "Resulting behaviour attribute " +
                        "do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour attribute " +
                        "do not apply for this testcase");
            }
        } catch (Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testQuotaDAWithSessionSrvcAtRealm",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (!cleanedUp) {
                destroyToken(usrtokenWithoutSrvcOrig);
                destroyToken(usrtokenWithoutSrvcNew);
                destroyToken(usrtokenWithSrvcOrig);
                destroyToken(usrtokenWithSrvcNew);
            }
        }
        exiting("testQuotaDAWithSessionSrvcAtRealm");
    }

    /**
     * Tests the following case: 
     * (a) Max session quota set to "1" for one user with session service
     * (b) Session service is not assigned for other user at user level
     * (c) Set resulting behavior if session quota exhausted to DENY_ACCESS
     * (d) Validates that only one session can be created for user with
     *     session service and new session is denied access
     * (e) Validates that more than one session can be created for user 
     *     without session service 
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testQuotaDAWithSessionSrvcAtRealm"})
    public void testQuotaDOSWithSessionSrvcAtRealm()
    throws Exception {
        entering("testQuotaDOSWithSessionSrvcAtRealm", null);
        SSOToken usrtokenWithSrvcOrig = null;
        SSOToken usrtokenWithSrvcNew = null;
        SSOToken usrtokenWithoutSrvcOrig = null;
        SSOToken usrtokenWithoutSrvcNew = null;
        log(Level.FINE, "testQuotaDOSWithSessionSrvcAtRealm",
                "This testcase validates session quota for one user with session" +
                "service at user level and for other user at realm level when " +
                "resulting behavior set to DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for one user with session service at user level and for other " +
                "user at realm level when resulting behavior set to " +
                "DESTROY_OLD_SESSION");        
        try {
            if (resultBehavior.equals("DESTROY_OLD_SESSION")) {
                usrtokenWithoutSrvcOrig = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithoutSrvcNew = getToken(testUserWithoutSrvc, 
                                        testUserWithoutSrvc, basedn);
                usrtokenWithSrvcOrig = getToken(testUserWithSrvc, 
                                        testUserWithSrvc, basedn);
                usrtokenWithSrvcNew = getToken(testUserWithSrvc,
                            testUserWithSrvc, basedn);
                log(Level.FINE, "testQuotaDOSWithSessionSrvcAtRealm", 
                        "Original token is valid and new " +
                        "token is valid here");
                assert validateToken(usrtokenWithoutSrvcOrig);
                assert validateToken(usrtokenWithoutSrvcNew);
                assert validateToken(usrtokenWithSrvcNew);
            } else {
                log(Level.FINE, "testQuotaDOSWithSessionSrvcAtRealm",
                        "Resulting behaviour attribute" +
                        "do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour attribute" +
                        " do not apply for this testcase");
            }
        } catch (Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testQuotaDOSWithSessionSrvcAtRealm",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (!cleanedUp) {
                destroyToken(usrtokenWithoutSrvcOrig);
                destroyToken(usrtokenWithoutSrvcNew);
                destroyToken(usrtokenWithSrvcNew);     
            }
        }
        exiting("testQuotaDOSWithSessionSrvcAtRealm");
    }
    
    /**
     * Cleans up the testcase attributes set in setup
     * 
     * @throws java.lang.Exception
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            if (dynSrvcRealmAssigned) {
                smsc.unassignDynamicServiceRealm(SessionConstants.SESSION_SRVC, 
                        realm);
            } 
            if (consTurnedOn) {
                Map attrMap = new HashMap();
                Set set = new HashSet();
                set.add("OFF");
                attrMap.put(SessionConstants.ENABLE_SESSION_CONST, set);
                smsc.updateSvcSchemaAttribute(SessionConstants.SESSION_SRVC,
                        attrMap, SessionConstants.GLOBAL_SRVC_TYPE);             
            }            
            log(Level.FINEST, "cleanup", "Cleaning User:" 
                    + testUserWithSrvc);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, testUserWithSrvc);
            log(Level.FINEST, "cleanup", "Cleaning User:" + testUserWithoutSrvc);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, 
            		testUserWithoutSrvc);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
        	destroyToken(admintoken);
        }
        exiting("cleanup");
    }   
}
