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
 * $Id: DelegationTest.java,v 1.8 2009/05/27 23:07:57 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.delegation;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.DelegationCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class contains methods to perform tests on delegation module.
 */
public class DelegationTest extends DelegationCommon {
    
    /**
     * No of actions in a given scenario.
     */
    private int testCount;
    
    /**
     * Name of the test action name.
     */
    private String testAction;
    
    /**
     * Identity name.
     */
    private String testIdName;
    
    /**
     * Identity password.
     */
    private String testIdPassword;
    
    /**
     * Identity type.
     */
    private String testIdType;
    
    /**
     * Attributes for given Identity.
     */
    private String testIdAttr;
    
    /**
     * Name of the member to be placed in a group or role.
     */
    private String testMemberName;
    
    /**
     * Privilege name.
     */
    private String testPrivileges;
    
    /**
     * Test prefix name.
     */
    private String prefixTestName;
    
    /**
     * Map to hold the configuration of the test.
     */
    private Map cfgMap;
    
    /**
     * Description of the currently executing scenario.
     */
    private String testDescription;
    
    /**
     * Realm name on which testcases are being executed.
     */
    private String delegationRealm;
    
    /**
     * SSOToken for currently logged in user.
     */
    private SSOToken ssoToken;
    
    /**
     * Service Management Service object used to create service related actions.
     */
    private SMSCommon smsObj;
    
    /**
     * Map to hold datastore configuration information
     */
    private Map dsCfgMap;
    
    /**
     * PolicyCommon object
     */
    private PolicyCommon mpc;
    
    /**
     * Default Constructor for DelegationTest
     */
    public DelegationTest()
    throws Exception {
        super("DelegationTests");
        mpc = new PolicyCommon();
    }
    
    /**
     * Reads the necessary test configuration and prepares the system
     * for Delegation testing.  Creates a delegation realm in which all the
     * identites are created and deleted.
     * @param testNum Test Number to be executed.
     * @param testName Name of the test.
     */
    @BeforeTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"testNum", "testName"})
    public void setup(String testNum, String testName)
    throws Exception {
        Object[] params = {testNum, testName};
        entering("setup", params);
        try {
            prefixTestName = testName + testNum;
            cfgMap = getDataFromCfgFile(prefixTestName, "delegation" +
                    fileseparator + testName);
            testCount = Integer.parseInt(getParams(
                    DelegationConstants.IDM_KEY_COUNT));
            log(Level.FINEST, "setup", "Count = " + testCount);
            testDescription = getParams(
                    DelegationConstants.IDM_KEY_DESCRIPTION);
            log(Level.FINEST, "setup", "Description = " +
                    testDescription);
            delegationRealm = getParams(DelegationConstants.IDM_KEY_REALM_NAME);
            int dsConfIdx = Integer.parseInt(
                    getParams(DelegationConstants.DS_CONF_IDX));
            log(Level.FINEST, "setup", "Realm = " + delegationRealm);
            ssoToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(ssoToken)) {
                log(Level.SEVERE, "setup", "Sso token is invalid");
                assert false;
            } else {
                assert(createRealm(ssoToken, delegationRealm, dsConfIdx));
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", "Setup Failed.");
            e.getStackTrace();
            destroyToken(ssoToken);
            throw e;
        }
        exiting("setup");
    }

    /**
     * This method executes the test cases of create, update, delete,
     * add member, remove member, create policy, login and logout
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDelegation()
    throws Exception {
        entering("testDelegation", null);
        try {
            boolean success = false;
            Reporter.log("Test Name: " + prefixTestName);
            Reporter.log("Description: " + testDescription);
            Reporter.log("Realm: " + delegationRealm);
            Reporter.log("Number of Items: " + testCount);
            for (int i = 0; i < testCount; i++) {
                boolean status = false;
                testAction = getParams(DelegationConstants.IDM_KEY_ACTION, i);
                testIdName =
                        getParams(DelegationConstants.IDM_KEY_IDENTITY_NAME, i);
                testIdType =
                        getParams(DelegationConstants.IDM_KEY_IDENTITY_TYPE, i);
                testIdAttr =
                        getParams(DelegationConstants.IDM_KEY_IDENTITY_ATTR, i);
                testPrivileges =
                        getParams(
                        DelegationConstants.IDM_KEY_IDENTITY_PRIVILEGES, i);
                testIdPassword =
                        getParams(DelegationConstants.IDM_KEY_IDENTITY_PASSWORD,
                        i);
                testMemberName =
                        getParams(
                        DelegationConstants.IDM_KEY_IDENTITY_MEMBER_NAME, i);
                String failCase = getParams(DelegationConstants.SHOULD_FAIL, i);
                boolean shouldFail;
                if (failCase != null && failCase.equals("true")) {
                    shouldFail = true;
                } else {
                    shouldFail = false;
                }
                String strLocRB = getParams(
                        DelegationConstants.POLICY_FILE_NAME, i);
                String strGblRB = getParams(
                        DelegationConstants.GLOBAL_POLICY_FILE_NAME, i);
                String strRefRB = getParams(
                        DelegationConstants.REFERRAL_POLICY_FILE_NAME, i);
                String serviceName =
                        getParams(DelegationConstants.SERVICE_NAME, i);
                String attrValPair =
                        getParams(DelegationConstants.ATTR_VALUE_PAIR, i);
                String schema_type = getParams(DelegationConstants.SCHEMA_TYPE,
                        i);
                testIdName = ((testIdName!= null) && 
                        (testIdName.equalsIgnoreCase("@amadmin@")))?
                        adminUser:testIdName;
                testIdPassword = ((testIdPassword != null) && 
                        (testIdPassword.equalsIgnoreCase(
                        "@amadminPassword@")))?
                        adminPassword:testIdPassword;
                log(Level.FINEST, "testDelegation", "Action = " + testAction);
                log(Level.FINEST, "testDelegation", "ID Name = " + testIdName);
                log(Level.FINEST, "testDelegation", "ID Type = " + testIdType);
                Reporter.log("Executing Index :" + i);
                Reporter.log("Action : " + testAction);
                Reporter.log("Name : " + testIdName);
                Reporter.log("Type : " + testIdType);
                Reporter.log("Attributes  : " + testIdAttr);
                Reporter.log("TestMemberName : " + testMemberName);
                if (testAction.equals("create")) {
                    try {
                      status = createID(testIdName, testIdType, testIdAttr,
                                ssoToken, delegationRealm);
                    } catch (IdRepoException idre) {
                        log(Level.SEVERE, "testDelegation", "Create error " +
                                idre.getMessage() + " " + idre.getErrorCode());
                        if (!shouldFail) {
                            idre.printStackTrace();
                            throw idre;
                        }
                    } catch (Exception e) {
                        log(Level.SEVERE, "testDelegation", e.getMessage());
                        if (!shouldFail) {
                            throw e;
                        }
                    }
                    if ((shouldFail && !status) || (!shouldFail && status)) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else if (testAction.equals("delete")) {
                    try {
                        status = deleteID(testIdName, testIdType, ssoToken,
                                delegationRealm);
                    } catch (Exception e) {
                        log(Level.SEVERE, "testDelegation", "Delete error " +
                                e.getMessage());
                        if (!shouldFail) {
                            throw e;
                        }
                    }
                    if ((shouldFail && !status) || (!shouldFail && status)) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else if (testAction.equals("addmember")) {
                    try {
                        assert(addMembers(testIdName, testIdType,
                                testMemberName, ssoToken, delegationRealm));
                    } catch (Exception e) {
                        log(Level.SEVERE, "testDelegation", "AddMember error " +
                                e.getMessage());
                        throw e;
                    }
                } else if (testAction.equals("removemember")) {
                    try {
                        assert(removeMembers(testIdName, testIdType,
                                testMemberName, ssoToken, delegationRealm));
                    } catch (Exception e) {
                        log(Level.SEVERE, "testDelegation",
                                "Removemember error " + e.getMessage());
                        throw e;
                    }
                } else if (testAction.equals("addprivileges")) {
                    try {
                        log(Level.FINE, "testDelegation",
                                "Adding privileges " +
                                testIdType + " " + testIdName + "...");
                        Reporter.log("TestPrivileges : " + testPrivileges);
                        success = addPrivilegesToId(testIdName, testIdType,
                                testPrivileges, ssoToken, delegationRealm);
                        if (success) {
                            log(Level.FINE, "testDelegation",
                                    "Successfully Added " +
                                    "privileges to " + testIdType + " " +
                                    testIdName + "...");
                            assert(showPrivileges(ssoToken, testIdName,
                                    getIdType(testIdType), delegationRealm,
                                    testPrivileges));
                        } else {
                            assert false;
                        }
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation", "Error adding " +
                                "privileges to " + testIdName + " " +
                                ex.getMessage());
                        throw ex;
                    }
                } else if (testAction.equals("login")) {
                    destroyToken(ssoToken);
                    String orgName = "";
                    if (delegationRealm.equals(realm)) {
                        orgName = basedn;
                    } else {
                        StringTokenizer orgs =
                                new StringTokenizer(delegationRealm, "/");
                        while (orgs.hasMoreTokens()) {
                            orgName = "o=" + orgs.nextToken() + "," + orgName;
                        }
                        orgName = orgName + "ou=services," + basedn;
                    }
                    log(Level.FINEST, "testDelegation", "org name = " +
                            orgName);
                    try {
                        ssoToken = getToken(testIdName, testIdPassword,
                                orgName);
                        log(Level.FINEST, "testDelegation",
                                ssoToken.toString());
                        if (ssoToken == null) {
                            assert false;
                        } else {
                            log(Level.FINEST, "testDelegation",
                                    "Present Login " + "user " + testIdName);
                            assert true;
                        }
                    } catch (Exception e) {
                        log(Level.SEVERE, "testDelegation", "Login error " +
                                e.getMessage());
                        throw e;
                    }
                } else if (testAction.equals("logout")) {
                    destroyToken(ssoToken);
                    ssoToken = getToken(adminUser, adminPassword, basedn);
                    assert true;
                } else if (testAction.equals("removeprivileges")) {
                    try {
                        log(Level.FINE, "testDelegation",
                                "Removing privileges from " +
                                testIdType + " " + testIdName + "...");
                        assert(removePrivilegesFromId(testIdName, testIdType,
                                testPrivileges, ssoToken, delegationRealm));
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation", "Error removing " +
                                "privileges from " + testIdName +
                                ex.getMessage());
                        throw ex;
                    }
                } else if (testAction.equals("createpolicy")) {
                    try {
                        String realmName = delegationRealm.substring(
                                delegationRealm.lastIndexOf("/") + 1);
                        Reporter.log("Policy File Name :" + strLocRB);
                        Reporter.log("Global Policy File Name :" + strGblRB);
                        String parentRealm = getParentRealm(delegationRealm);
                        int policyIdx =
                                Integer.parseInt(
                                getParams(DelegationConstants.POLICY_CONFIG_NO,
                                i));
                        Reporter.log("Policy index :" + policyIdx);
                        log(Level.FINE, "testDelegation",
                                "Creating policy file" + strLocRB + ".xml");
                        mpc.createPolicyXML("delegation" + fileseparator +
                                strGblRB, "delegation" + fileseparator +
                                strLocRB, policyIdx,
                                strLocRB + ".xml", realmName);
                        log(Level.FINE, "testDelegation", "Creating policy in "+
                                "realm " + realmName + " using file " +
                                strLocRB + ".xml");
                        String subOrg = delegationRealm.substring(
                                delegationRealm.lastIndexOf("/") + 1);
                        String subOrgLoginUrl = loginURL + "?org=" + subOrg;
                        //testIdName
                        testIdName = (testIdName.equalsIgnoreCase("@amadmin@"))?
                                adminUser:testIdName;
                        testIdPassword = (testIdPassword.equalsIgnoreCase(
                                "@amadminPassword@"))?
                                adminPassword:testIdPassword;
                        status = mpc.createPolicy(strLocRB + ".xml", realmName,
                                subOrgLoginUrl, testIdName, testIdPassword);
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error creating Policy " + ex.getMessage());
                        assert false;
                        throw ex;
                    }
                    if ((shouldFail && !status) || (!shouldFail && status)) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else if (testAction.equals("createreferralpolicy")) {
                    try {
                        Reporter.log("Policy File Name :" + strLocRB);
                        Reporter.log("Global Policy File Name :" + strGblRB);
                        Reporter.log("Referral policy File Name :" + strRefRB);
                        String parentRealm = getParentRealm(delegationRealm);
                        String realmName = delegationRealm.substring(
                                delegationRealm.lastIndexOf("/") + 1);
                        int policyIdx =
                                Integer.parseInt(
                                getParams(
                                DelegationConstants.REF_POLICY_CONFIG_NO, i));
                        Reporter.log("Referral policy index :" + policyIdx);
                        log(Level.FINE, "testDelegation",
                                "Creating ref policy "+
                                "xml " + strRefRB + ".xml");
                        mpc.createReferralPolicyXML("delegation" +
                                fileseparator + strGblRB, "delegation" +
                                fileseparator + strRefRB,
                                "delegation" + fileseparator + strLocRB,
                                policyIdx, strRefRB + ".xml");
                        log(Level.FINE, "testDelegation",
                                "Creating ref policy "+
                                "in " + parentRealm + " using file " +
                                strRefRB + ".xml");
                        String subOrgLoginUrl = loginURL + "?org=" +
                                parentRealm;
                        status = mpc.createPolicy(strRefRB + ".xml",
                                parentRealm, subOrgLoginUrl, testIdName,
                                testIdPassword);
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error creating referral Policy " +
                                ex.getMessage());
                        throw ex;
                    }
                    if ((shouldFail && !status) || (!shouldFail && status)) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else if (testAction.equals("deletepolicy")) {
                    try {
                        String realmName = delegationRealm.substring(
                                delegationRealm.lastIndexOf("/") + 1);
                        int policyIdx = 0;
                        if (strRefRB != null) {
                            log(Level.FINE, "testDelegation",
                                    "Deleting ref policy");
                            policyIdx =
                                    Integer.parseInt(
                                    getParams(
                                    DelegationConstants.REF_POLICY_CONFIG_NO,
                                    i));
                            mpc.deleteReferralPolicies("delegation" +
                                    fileseparator + strLocRB, "delegation" +
                                    fileseparator + strRefRB,
                                    policyIdx);
                        }
                        log(Level.FINE, "testDelegation",
                                "Deleting policy in realm " +
                                "using file " + strLocRB + ".xml");
                        String subOrg = delegationRealm.substring(
                                delegationRealm.lastIndexOf("/") + 1);
                        String subOrgLoginUrl = loginURL + "?org=" + subOrg;
                        policyIdx =
                                Integer.parseInt(
                                getParams(DelegationConstants.POLICY_CONFIG_NO,
                                i));
                        status = mpc.deletePolicies("delegation" +
                                fileseparator + strLocRB, policyIdx,
                                realmName, subOrgLoginUrl, testIdName,
                                testIdPassword);
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error Deleting Policy " + ex.getMessage());
                        assert false;
                        throw ex;
                    }
                    if ((shouldFail && !status) || (!shouldFail && status)) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else if (testAction.equals("createsubrealm")) {
                    try {
                        int dsConfIdx = Integer.parseInt(
                                getParams(DelegationConstants.DS_CONF_IDX, i));
                        assert(createRealm(ssoToken, testIdName, dsConfIdx));
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error creating realm " + ex.getMessage());
                        throw ex;
                    }
                } else if (testAction.equals("addservice")) {
                    try {
                        Reporter.log("Service Name : " + serviceName);
                        Reporter.log("Attribute value pair : " + attrValPair);
                        Map attrMap = attributesToMap(attrValPair);
                        if (testIdType.equals("realm")) {
                            smsObj = new SMSCommon(ssoToken);
                            assert(smsObj.assignDynamicServiceRealm(serviceName,
                                    delegationRealm, attrMap));
                        } else if (testIdType.equals("user")) {
                            assert(assignServiceToUser(ssoToken, testIdName,
                                    serviceName, attrMap, delegationRealm));
                        } else {
                            log(Level.SEVERE, "testDelegation",
                                    "Invalid test id for addservice action, " +
                                    testIdType);
                            assert false;
                        }
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error assigning service " + ex.getMessage());
                        throw ex;
                    }
                } else if (testAction.equals("unassignservice")) {
                    try {
                        Reporter.log("Service Name : " + serviceName);
                        if (testIdType.equals("realm")) {
                            smsObj = new SMSCommon(ssoToken);
                            assert(smsObj.unassignDynamicServiceRealm(
                                    serviceName, delegationRealm));
                        } else if (testIdType.equals("user")) {
                            assert(unAssignServiceFromUser(ssoToken, testIdName,
                                    serviceName, delegationRealm));
                        } else {
                            log(Level.SEVERE, "testDelegation",
                                    "Invalid test id for unassignservice " +
                                    "action, " + testIdType);
                            assert false;
                        }
                    } catch (Exception ex) {
                        log(Level.SEVERE, "testDelegation",
                                "Error unassigning service " + ex.getMessage());
                        throw ex;
                    }
                } else if (testAction.equals("modifyservice")) {
                    Reporter.log("Service Name : " + serviceName);
                    Reporter.log("Attribute value pair : " + attrValPair);
                    Reporter.log("Schema Type : "+ schema_type);
                    Map attrMap = attributesToMap(attrValPair);
                    if (testIdType.equals("realm")) {
                        smsObj = new SMSCommon(ssoToken);
                        if (schema_type != null) {
                            boolean bStat = false;
                            try {
                                smsObj.updateSvcSchemaAttribute(serviceName,
                                    attrMap, schema_type);
                            } catch (Exception e) {
                                bStat = true;
                            }
                            assert (bStat);
                        } else {
                            boolean bStat = false;
                            try {
                                smsObj.updateGlobalServiceDynamicAttributes(
                                    serviceName, attrMap);
                            } catch (Exception e) {
                                bStat = true;
                            }
                            assert (bStat);
                        }
                    } else {
                        assert(modifyUsersAssignedService(ssoToken, testIdName,
                                serviceName, attrMap, delegationRealm));
                    }
                } else {
                    log(Level.SEVERE, "testDelegation",
                            "Invalid test action, " +
                            testAction + ", for test index " + i);
                    assert false;
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "testDelegation",
                    "Error executing scenario " + prefixTestName);
            ex.printStackTrace();
            destroyToken(ssoToken);
            throw ex;
        }
        exiting("testDelegation");
    }
    
    /**
     * This method remove all identites that are created during the test
     * execution and are not cleaned up due to test failure or exception
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void deleteRealms()
    throws Exception {
        entering("deleteRealms", null);
        try {
            //Because of exception token gets destroyed recreate it
            //so that it will not create issue for deleting the realms.
            if (!validateToken(ssoToken)) {
                ssoToken =  getToken(adminUser, adminPassword, basedn);
            }
            assert(deleteRealmsRecursively(ssoToken));
        } catch (Exception ex) {
            log(Level.SEVERE, "deleteRealms",
                    "Error in deleting realms " + ex.getMessage());
            throw ex;
        } finally {
            destroyToken(ssoToken);
        }
        exiting("deleteRealms");
    }
    
    /**
     * Creates a realm with datastore. Since both sub realm and sub sub realm
     * utilize the same datastore, while creating sub sub realm no datastore is
     * created, but the one created at sub realm is utilized, as that is copied
     * by default when creating sub sub realm. The implementation logic needs
     * to change if sub sub realm wants to create its own datastore.
     * @param ssoToken ssotoken to be used for creating realm
     * @param realm Name of the realm ex:/red/red1/red2
     * @param dsConfIdx Index of the data store configuration information that
     *        need to be used from the UMGlobalDatastoreConfig file.
     * @return Return true if realm creation is successfull
     */
    private boolean createRealm(SSOToken ssoToken, String realm, int dsConfIdx)
            throws Exception {
        try {
            if ((realm != null) && !realm.equals("/")) {
                String childRealm =
                        realm.substring(realm.lastIndexOf("/") + 1);
                if (searchRealms(ssoToken, childRealm,
                        getParentRealm(realm)).isEmpty()) {
                    SMSCommon smsc = new SMSCommon(ssoToken, "config" +
                            fileseparator + "default" + fileseparator +
                            "UMGlobalConfig");
                    Map<String, String> umDatastoreTypes =
                            new HashMap<String, String>();
                    umDatastoreTypes.put("1", serverName);
                    log(Level.FINE, "createRealm",
                            "Creating delegation realm " + realm + "...");
                    int secIdx = realm.substring(1,
                            realm.length()).indexOf("/");
                    log(Level.FINEST, "createRealm", "secIdx: " + secIdx);

                    // If there is sub sub realm, do not try to create a 
                    // datastore in it. Just use the one inherited from the
                    // sub realm
                    if (secIdx != -1) {
                        createIdentity(ssoToken, getParentRealm(realm),
                                IdType.REALM, childRealm, new HashMap());
                        //This sleep required to let the SMS to
                        //self notify the realm creation and its
                        //associated components such as services
                        //and datastores
                        Thread.sleep(notificationSleepTime);
                    } else {
                        createIdentity(ssoToken, getParentRealm(realm),
                                IdType.REALM, childRealm, new HashMap());

                        ResourceBundle gblCfgData =
                                ResourceBundle.getBundle("config" +
                                fileseparator + "default" + fileseparator +
                                "UMGlobalConfig");

                        ResourceBundle cfgData =
                                ResourceBundle.getBundle("Configurator-" +
                                serverName + "-Generated");
                        String umdatastore = cfgData.getString("umdatastore");

                        if ((gblCfgData.getString("UMGlobalConfig." +
                                "createNewDatastores")).equals("true") &&
                                (!umdatastore.equals("embedded"))) {

                            smsc.deleteAllDataStores(realm, -1);
                            smsc.createUMDatastoreGlobalMap(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX,
                                    umDatastoreTypes,
                                    SMSConstants.QATEST_EXEC_MODE_SINGLE,
                                    "delegation");
                            smsc.createDataStore(1, "delegation" +
                                    fileseparator +
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "-Generated");
                            //This sleep required to let the SMS to
                            //self notify the realm creation and its
                            //associated components such as services
                            //and datastores
                            Thread.sleep(notificationSleepTime);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "createRealm", "Creating realm failed...");
            ex.printStackTrace();
            throw ex;
        }
        return true;
    }
    
    /**
     * This method return value of test case property based on the key
     * @param key test case property key
     * @return test case property value
     */
    private String getParams(String key) {
        String paramKey = prefixTestName + "." + key;
        return (String)cfgMap.get(paramKey);
    }
    
    /**
     * This method return value of test case property based on the key
     * @param key test case property key
     * @return test case property value
     */
    private String getParams(String key, int idx) {
        String paramKey = prefixTestName + "." + key + "." + idx;
        return (String)cfgMap.get(paramKey);
    }
}