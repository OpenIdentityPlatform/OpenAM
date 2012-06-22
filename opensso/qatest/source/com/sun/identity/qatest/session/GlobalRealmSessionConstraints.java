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
 * $Id: GlobalRealmSessionConstraints.java,v 1.9 2009/01/27 00:16:37 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.session;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.DelegationCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class is used to test 'Active Sessions' dynamic attribute for 
 * both admin and the user at the Global and Realm levels. All the 
 * tests depend on two constraints :
 *  a)Exempt Top-level admins (can be YES/NO) 
 *  b)Resulting behavior if session quota exhausted 
 *            (can be DENY_ACESS/DESTROY_OLD_SESSION)
 */
public class GlobalRealmSessionConstraints extends TestCommon {

    private SSOToken admintoken;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private DelegationCommon dc;
    private String defActiveSessions = "5";
    private String testUser = "sessConsTest";
    private String testAdminUser = "sessConsAdminTest";    
    private String btladmin;
    private String resultBehavior;
    private boolean consTurnedOn = false;
    private boolean dynSrvcRealmAssigned = false;
    private boolean cleanedUp = false;
    private String adminGroup = "AdminGroup";
    private boolean adminGroupCreated = false;
    
    /**
    * SessionConstraints Constructor
    * Creates single admintoken and objects of common classes.
    * 
    * @throws java.lang.Exception
    */
    public GlobalRealmSessionConstraints() throws Exception {
        super("GlobalRealmSessionConstraints");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();  
        smsc = new SMSCommon(admintoken);
        dc = new DelegationCommon("GlobalRealmSessionConstraints");
    }

    /**
     * Initialization method. Setup:
     * (a) Creates adminuser, user Identities 
     * (b) Parameter inheritancelevel takes values Global/Realm
     * (c) Sets the session service at the inheritance level and 
     *     sets active number of sessions to "1"
     * 
     * @throws java.lang.Exception
     */
    @Parameters({"inheritancelevel"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String inheritancelevel)
    throws Exception {
        entering("setup", null);
        try {
            log(Level.FINEST, "setup", "Tests run at" 
                    + inheritancelevel + " level");
            Reporter.log("InheritanceLevel: " + inheritancelevel);
            Set set = smsc.getAttributeValueFromSchema(
                    SessionConstants.SESSION_SRVC,
                    SessionConstants.ENABLE_SESSION_CONST, 
                    SessionConstants.GLOBAL_SRVC_TYPE);
            Iterator itr = set.iterator();
            String quotaConst = (String) itr.next();
            if (quotaConst.equals("OFF")) {
                Map attrMap = new HashMap();
                set.clear();
                set.add("ON");
                attrMap.put(SessionConstants.ENABLE_SESSION_CONST, set);
                smsc.updateSvcSchemaAttribute(SessionConstants.SESSION_SRVC,
                        attrMap, SessionConstants.GLOBAL_SRVC_TYPE);                 
                consTurnedOn = true;
            }
            set = smsc.getAttributeValueFromSchema(SessionConstants.SESSION_SRVC,
                    SessionConstants.BYPASS_TOPLEVEL_ADMIN, 
                    SessionConstants.GLOBAL_SRVC_TYPE);
            itr = set.iterator();
            btladmin = (String) itr.next();
            log(Level.FINE, "setup", "Exempt Top-level admin from constraint " +
                    "checking is set to: " + btladmin);
            set = smsc.getAttributeValueFromSchema(SessionConstants.SESSION_SRVC,
                    SessionConstants.RESULTING_BEHAVIOR, 
                    SessionConstants.GLOBAL_SRVC_TYPE);
            itr = set.iterator();
            resultBehavior = (String) itr.next();     
            log(Level.FINE, "setup", "Resulting behavior if session quota " +
                    "exhausted is set to: " + resultBehavior);

            idmc.createDummyUser(admintoken, realm, "", testAdminUser);
            Map nullmap = new HashMap();
            if (!idmc.doesIdentityExists(adminGroup, IdType.GROUP, 
                    admintoken, realm)) {
                log(Level.FINE, "setup", "AdminGroup doesn't " +
                        "exist..Creating \"AdminGroup\" ");
                idmc.createIdentity(admintoken, realm, IdType.GROUP, 
                        adminGroup, nullmap);
                List privileges = new ArrayList();
                privileges.add("RealmAdmin");
                dc.addPrivileges(admintoken, adminGroup, IdType.GROUP, realm, 
                        privileges);
                adminGroupCreated = true;
            }
            idmc.addUserMember(admintoken, testAdminUser,
                    adminGroup , IdType.GROUP);
            set = idmc.getMembers(admintoken, adminGroup, IdType.GROUP, 
                    IdType.USER, realm);
            Iterator it = set.iterator();
            while (it.hasNext()) {
                AMIdentity identity = (AMIdentity)it.next();
                if (!identity.getName().equals(testAdminUser)) {
                    log(Level.SEVERE, "setup", "Test admin user not assigned " +
                            "to AdminGroup");
                    assert false;                    
                }
            }
            log(Level.FINE, "setup", "Created user " + testAdminUser +
                    " identity and added AdminGroup to this user");

            idmc.createDummyUser(admintoken, realm, "", testUser);
            log(Level.FINE, "setup", "Created user " + 
                    testUser + " identity");

            if (inheritancelevel.equals("Global")) {
                String activeSessionsBU = getGlobalSessionQuotaAttribute(
                        SessionConstants.SESSION_SRVC, 
                        SessionConstants.SESSION_QUOTA_ATTR, 
                        SessionConstants.DYNAMIC_SRVC_TYPE);
                defActiveSessions = activeSessionsBU;
                log(Level.FINE, "setup", "Number of active sessions before " +
                        "updating dynamic service attributes: "
                        + activeSessionsBU);
                if (!activeSessionsBU.equals("1")) {
                    Map quotamap = new HashMap();
                    set = new HashSet();
                    set.add("1");
                    quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
                    smsc.updateGlobalServiceDynamicAttributes(
                            SessionConstants.SESSION_SRVC, quotamap);
                    String activeSessionsAU = getGlobalSessionQuotaAttribute(
                        SessionConstants.SESSION_SRVC, 
                        SessionConstants.SESSION_QUOTA_ATTR, 
                        SessionConstants.DYNAMIC_SRVC_TYPE);
                    assert activeSessionsAU.equals("1");
                    log(Level.FINE, "setup", "Number of active sessions " +
                            "at Global level after updating dynamic service " +
                            "attributes: " + activeSessionsAU);
                }
            } else if (inheritancelevel.equals("Realm")) {
                if (smsc.isServiceAssigned(SessionConstants.SESSION_SRVC, 
                        realm)) {
                    smsc.unassignDynamicServiceRealm(
                            SessionConstants.SESSION_SRVC, realm);
                }
                String globalActiveSessions = getGlobalSessionQuotaAttribute(
                        SessionConstants.SESSION_SRVC, 
                        SessionConstants.SESSION_QUOTA_ATTR, 
                        SessionConstants.DYNAMIC_SRVC_TYPE);
                if (globalActiveSessions.equals("1")) {
                    Map quotamap = new HashMap();
                    set = new HashSet();
                    set.add("5");
                    quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
                    smsc.updateGlobalServiceDynamicAttributes(
                            SessionConstants.SESSION_SRVC, quotamap);
                    globalActiveSessions = getGlobalSessionQuotaAttribute(
                            SessionConstants.SESSION_SRVC, 
                            SessionConstants.SESSION_QUOTA_ATTR,
                            SessionConstants.DYNAMIC_SRVC_TYPE);
                }
                Map quotamap = new HashMap();
                set = new HashSet();
                set.add("1");
                quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);                
                smsc.assignDynamicServiceRealm(SessionConstants.SESSION_SRVC, 
                        realm, quotamap);
                dynSrvcRealmAssigned = true;
                smsc.updateServiceAttrsRealm(SessionConstants.SESSION_SRVC, 
                        realm, quotamap);
                
                Map map = new HashMap();
                map = smsc.getDynamicServiceAttributeRealm(
                        SessionConstants.SESSION_SRVC, realm);
                set = (Set)map.get(SessionConstants.SESSION_QUOTA_ATTR);
                Iterator iter = set.iterator();
                String realmActiveSessions = (String)iter.next();
                
                assert !globalActiveSessions.equals("1");                
                assert realmActiveSessions.equals("1");
                log(Level.FINE, "setup", "Number of active sessions at " +
                        "Realm level after updating dynamic service " +
                        "attributes: " + realmActiveSessions);
            } 
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            log(Level.FINE, "setup", "Setup failed, attempting cleanup");
            cleanup();
            throw e;
        } 
        exiting("setup");
    }
    
    /**
     *  Tests the following case:
     * (a) Max session quota set to "1" in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to Yes
     * (c) Set Resulting behavior if session quota exhausted to
     *     DENY_ACCESS
     * (d) User is amadmin
     * (e) Validates that atleast ten sessions can be created for amadmin.
     *     
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMaxSessionQuotaGlobalRealmYDAAmAdmin()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmYDADOSAmAdmin", null);
        Vector<SSOToken> tokenVector = new Vector<SSOToken>();
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAAmAdmin", 
                "This testcase validates session quota for amadmin when exempt " +
                "Top-level admin is Yes and resulting behavior is " +
                "DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for amadmin when exempt Top-level admin is Yes and resulting " +
                "behavior is DENY_ACCESS");
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDAAmAdmin", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDAAmAdmin", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);
        try {
            if (btladmin.equals("YES")&& 
                    resultBehavior.equals("DENY_ACCESS")) {
                for ( int count = 0; count < 10; count++ ) {
                    SSOToken token = getToken(adminUser, adminPassword, basedn);
                    tokenVector.add(token);
                }
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAAmAdmin", 
                        "Successfully created ten tokens for amadmin ");
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAAmAdmin",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch (Exception e) {
            assert false;
            cleanup();
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmYDAAmAdmin",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            while (tokenVector.size() != 0) {
                destroyToken(tokenVector.get(0));
                tokenVector.remove(0);
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmYDAAmAdmin");
    }

    
    /**
     *  Tests the following case:
     * (a) Max session quota set to "1" in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to Yes
     * (c) Set Resulting behavior if session quota exhausted to
     *     DESTROY_OLD_SESSION OR DENY_ACCESS
     * (d) User is super admin
     * (e) Validates that only one session can be created for user and old
     *     session is destroyed when a new session is created
     *     
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMaxSessionQuotaGlobalRealmYDADOSAdmin()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmYDADOSAdmin", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDADOSAdmin", 
                "This testcase validates session quota for admin when exempt " +
                "Top-level admin is Yes and resulting behavior is " +
                "DENY_ACCESS or DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for admin when exempt Top-level admin is Yes and resulting " +
                "behavior is DENY_ACCESS or DESTROY_OLD_SESSION");
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDADOSAdmin", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDADOSAdmin", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);
        try {
            if (btladmin.equals("YES")) {
                ssotokenOrig = getToken(testAdminUser, 
                                        testAdminUser, basedn);
                ssotokenNew = getToken(testAdminUser, 
                                        testAdminUser, basedn);
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDADOSAdmin", 
                        "Original token is valid and new " +
                        "token is valid here");               
                assert validateToken(ssotokenOrig);
                assert validateToken(ssotokenNew);
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDADOSAdmin",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch (Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmYDADOSAdmin",
                    e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (!cleanedUp) {
                destroyToken(ssotokenOrig);
                destroyToken(ssotokenNew);
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmYDADOSAdmin");
    }

    /**
     * Tests the following case:
     * (a) Max session quota set to 1 in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to No
     * (c) Set Resulting behavior if session quota exhausted to
     *     DESTROY_OLD_SESSION
     * (d) User is super admin
     * (e) Validates that only one session can be created for user and old
     *     session is destroyed when a new session is created
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmNDOSAdmin()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmNDOSAdmin", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSAdmin", 
                "This testcase validates session quota for admin when exempt " +
                "Top-level admin is No and resulting behavior is " +
                "DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for admin when exempt Top-level admin is No and " +
                "resulting behavior is DESTROY_OLD_SESSION");      
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDOSAdmin", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDOSAdmin", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);        
        try {
            if (btladmin.equals("NO") && 
                    resultBehavior.equals("DESTROY_OLD_SESSION")) {
                ssotokenOrig = getToken(testAdminUser, testAdminUser, basedn);
                Set set = getAllUserTokens(admintoken, testAdminUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    ssotokenNew = getToken(testAdminUser, 
                             testAdminUser, basedn);
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSAdmin", 
                             "Original token is invalid and new " +
                             "token is valid here");
                    assert validateToken(ssotokenNew);
                    }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSAdmin",
                          "Resulting behaviour and Top-level "
                          + "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level "
                          + "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDOSAdmin", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp)
                destroyToken(ssotokenNew);
        }
        exiting("testMaxSessionQuotaGlobalRealmNDOSAdmin");
    }

    /**
     * Tests the following case: 
     * (a) Max session quota set to 1 in global/realm session service 
     * (b) Set Exempt Top-level admins from constraint checking to No 
     * (c) Set Resulting behavior if session quota exhausted to DENY_ACCESS
     * (d) User is super admin
     * (e) Validates that only one session
     *     can be created for admin and new session is denied access
     * 
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}) 
    //dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmNDAAdmin()
    throws Exception {
    	entering("testMaxSessionQuotaGlobalRealmNDAAdmin", null);        
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAAdmin", 
                "This testcase validates session quota for admin when exempt " +
                "Top-level admin is No and resulting behavior is " +
                "DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for admin when exempt Top-level admin is No and " +
                "resulting behavior is DENY_ACCESS");          
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDAAdmin", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDAAdmin", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);        
        try {
            if (btladmin.equals("NO") && 
                    resultBehavior.equals("DENY_ACCESS")) {
                ssotokenOrig = getToken(testAdminUser,
                        testAdminUser, basedn);
                Set set = getAllUserTokens(admintoken, testAdminUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    try {
                        ssotokenNew = getToken(testAdminUser,
                                testAdminUser, basedn);
                        log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "ERROR: Deny access case failed for Admin");
                        assert false;
                    } catch(Exception e) {
                      log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "Cannot create new token: " + e.getMessage());
                        e.printStackTrace();
                    }
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAAdmin", 
                            "Original token is valid and new " +
                            "token is invalid here");
                    assert validateToken(ssotokenOrig);
                    assert !validateToken(ssotokenNew);
                }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAAdmin",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAAdmin", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp) {
                destroyToken(ssotokenOrig);
                destroyToken(ssotokenNew);                
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmNDAAdmin");
    }

    /**
     * Tests the following case:
     * (a) Max session quota set to 1 in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to Yes
     * (c) Set Resulting behavior if session quota exhausted to
     *     DESTROY_OLD_SESSION
     * (d) User is not a super admin
     * (e) Validates that only one session can be created for user and old
     *     session is destroyed when a new session is created
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmYDOSUser()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmYDOSUser", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDOSUser", 
                "This testcase validates session quota for user when exempt " +
                "Top-level admin is Yes and resulting behavior is " +
                "DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for user when exempt Top-level admin is Yes and " +
                "resulting behavior is DESTROY_OLD_SESSION");    
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDOSUser", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDOSUser", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);        
        try {
            if (btladmin.equals("YES") && 
                    resultBehavior.equals("DESTROY_OLD_SESSION")) {
                ssotokenOrig = getToken(testUser, 
                        testUser, basedn);
                Set set = getAllUserTokens(admintoken, testUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    ssotokenNew = getToken(testUser, 
                            testUser, basedn);
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDOSUser", 
                            "Original token is invalid and new " +
                            "token is valid here");
                    assert validateToken(ssotokenNew);
                    }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDOSUser",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmYDOSUser", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp) {
                destroyToken(ssotokenNew);
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmYDOSUser");
    }

    /**
     * Tests the following case:
     * (a) Max session quota set to 1 in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to No
     * (c) Set Resulting behavior if session quota exhausted to
     *     DESTROY_OLD_SESSION
     * (d) User is not a super admin
     * (e) Validates that only one session can be created for user and old
     *     session is destroyed when a new session is created
     *     
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmNDOSUser()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmNDOSUser", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSUser", 
                "This testcase validates session quota for user when exempt " +
                "Top-level admin is No and resulting behavior is " +
                "DESTROY_OLD_SESSION");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for user when exempt Top-level admin is No and " +
                "resulting behavior is DESTROY_OLD_SESSION");        
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDOSUser", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDOSUser", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);        
        try {
            if (btladmin.equals("NO") && 
                    resultBehavior.equals("DESTROY_OLD_SESSION")) {
                ssotokenOrig = getToken(testUser, 
                        testUser, basedn);
                Set set = getAllUserTokens(admintoken, testUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    ssotokenNew = getToken(testUser, 
                            testUser, basedn);
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSUser", 
                            "Original token is invalid and new " +
                            "token is valid here");
                    assert validateToken(ssotokenNew);
                    }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDOSUser",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDOSUser",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp)
                destroyToken(ssotokenNew);
        }
        exiting("testMaxSessionQuotaGlobalRealmNDOSUser");
    }

    /**
     * Tests the following case:
     * (a) Max session quota set to 1 in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to No
     * (c) Set Resulting behavior if session quota exhausted to
     *     DENY_ACCESS
     * (d) User is not a super admin
     * (e) Validates that only one session can be created for admin and 
     *     new session is denied access
     *     
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmNDAUser()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmNDAUser", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAUser", 
                "This testcase validates session quota for user when exempt " +
                "Top-level admin is No and resulting behavior is " +
                "DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for user when exempt Top-level admin is No and " +
                "resulting behavior is DENY_ACCESS");       
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDAUser", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmNDAUser", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);        
        try {
            if (btladmin.equals("NO") && 
                    resultBehavior.equals("DENY_ACCESS")) {
                ssotokenOrig = getToken(testUser, 
                        testUser, basedn);
                Set set = getAllUserTokens(admintoken, testUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    try {
                        ssotokenNew = getToken(testUser, 
                                testUser, basedn);
                        log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "ERROR: Deny access case failed for user");
                        assert false;
                    } catch(Exception e) {
                      log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "Cannot create new token: " + e.getMessage());
                        e.printStackTrace();
                    } 
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAUser", 
                            "Original token is valid and new " +
                            "token is invalid here");
                    assert validateToken(ssotokenOrig);
                    assert !validateToken(ssotokenNew);
                }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmNDAUser",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp) {
                destroyToken(ssotokenOrig);
                destroyToken(ssotokenNew);
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmNDAUser");
    }
    
    /**
     * Tests the following case:
     * (a) Max session quota set to 1 in global/realm session service
     * (b) Set Exempt Top-level admins from constraint checking to Yes
     * (c) Set Resulting behavior if session quota exhausted to
     *     DENY_ACCESS
     * (d) User is not a super admin
     * (e) Validates that only one session can be created for admin 
     *     and new session is denied access
     *     
     * @throws java.lang.Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods = {"testMaxSessionQuotaGlobalRealmYDADOSAdmin"})
    public void testMaxSessionQuotaGlobalRealmYDAUser()
    throws Exception {
        entering("testMaxSessionQuotaGlobalRealmYDAUser", null);
        SSOToken ssotokenOrig = null;
        SSOToken ssotokenNew = null;
        log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAUser", 
                "This testcase validates session quota for user when exempt " +
                "Top-level admin is Yes and resulting behavior is " +
                "DENY_ACCESS");
        Reporter.log("Test Description: This testcase validates session quota " +
                "for user when exempt Top-level admin is Yes and " +
                "resulting behavior is DENY_ACCESS");      
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDAUser", 
                "Top-level Admin Checked: " + btladmin);
        Reporter.log("Top-level Admin Checked: " + btladmin);
        log(Level.FINEST, "testMaxSessionQuotaGlobalRealmYDAUser", 
                "Resulting Behavior Atribute: " + resultBehavior);
        Reporter.log("Resulting Behavior Atribute: " + resultBehavior);
        try {
            if (btladmin.equals("YES") && 
                    resultBehavior.equals("DENY_ACCESS")) {
                ssotokenOrig = getToken(testUser, 
                        testUser, basedn);
                Set set = getAllUserTokens(admintoken, testUser);
                if (set.size() >= 2) {
                    assert false;
                } else {
                    try{
                        ssotokenNew = getToken(testUser, 
                                testUser, basedn);
                        log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "ERROR: Deny access case failed for user");
                        assert false;
                    } catch(Exception e) {
                      log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmNDAUser",
                                "Cannot create new token: " + e.getMessage());
                        e.printStackTrace();
                    }
                    log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAUser", 
                            "Original token is valid and new " +
                            "token is invalid here");
                    assert validateToken(ssotokenOrig);
                    assert !validateToken(ssotokenNew);
                }
            } else {
                log(Level.FINE, "testMaxSessionQuotaGlobalRealmYDAUser",
                        "Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
                Reporter.log("Note: Resulting behaviour and Top-level " +
                        "exempt attributes do not apply for this testcase");
            }
        } catch(Exception e) {
            cleanup();
            cleanedUp = true;
            log(Level.SEVERE, "testMaxSessionQuotaGlobalRealmYDAUser", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (!cleanedUp) {
                destroyToken(ssotokenOrig);
                destroyToken(ssotokenNew);
            }
        }
        exiting("testMaxSessionQuotaGlobalRealmYDAUser");
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
                smsc.unassignDynamicServiceRealm(
                        SessionConstants.SESSION_SRVC, realm);
            } 
            if (consTurnedOn) {
                Map attrMap = new HashMap();
                Set set = new HashSet();
                set.add("OFF");
                attrMap.put(SessionConstants.ENABLE_SESSION_CONST, set);
                smsc.updateSvcSchemaAttribute(SessionConstants.SESSION_SRVC,
                        attrMap, SessionConstants.GLOBAL_SRVC_TYPE);             
            }
            log(Level.FINEST, "cleanup", "Cleaning TestAdminUser: " 
                    + testAdminUser);
            idmc.removeUserMember(admintoken, testAdminUser,
                    adminGroup , IdType.GROUP, realm);
            if (adminGroupCreated) { 
                idmc.deleteIdentity(admintoken,realm,IdType.GROUP, adminGroup);
            }
            idmc.deleteIdentity(admintoken, realm, IdType.USER, testAdminUser);
            log(Level.FINEST, "cleanup", "Cleaning User: " + testUser);
            idmc.deleteIdentity(admintoken, realm, IdType.USER,
                    testUser);
            Map quotamap = new HashMap();
            Set set = new HashSet();
            set.add(defActiveSessions);
            quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
            smsc.updateGlobalServiceDynamicAttributes(
                    SessionConstants.SESSION_SRVC, quotamap);
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
     * @param serviceName contains servicename as String
     * @param attributeName name of attribute in the service
     * @param attributeType type of the attribute (User/Role etc)
     * @return String containing global active sessions
     * 
     * @throws java.lang.Exception
     */
    private String getGlobalSessionQuotaAttribute(String serviceName, 
            String attributeName, 
            String attributeType) 
    throws Exception {
        String globalActiveSessions = "0";
        Set set = smsc.getAttributeValueFromSchema(serviceName,
              attributeName, attributeType);
        Iterator itr = set.iterator();
        globalActiveSessions = (String)itr.next();
        return globalActiveSessions;
    }
}
