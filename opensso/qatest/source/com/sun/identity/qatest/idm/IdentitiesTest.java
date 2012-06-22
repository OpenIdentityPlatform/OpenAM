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
 * $Id: IdentitiesTest.java,v 1.7 2009/01/27 00:05:42 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * <code>IdentitiesTest</code> contains the methods to create, delete, search,
 * and update identities with identity types user, role, filtered role, group,
 * and agent.  It executes the idm test cases.
 */
package com.sun.identity.qatest.idm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

public class IdentitiesTest extends IDMCommon {
    private Map cfgMap;
    private SSOToken ssoToken;
    private String prefixTestName;
    private String testAction;
    private String testIdAttr;
    private String testCount;
    private String testDescription;
    private String testRealm;
    private String testIdType;
    private String testIdName;
    private String testReturnCode;
    private String testExpectedErrCode;
    private String testExpectedErrMsg;
    private String testExpectedResult;
    private String testMemberName;
    private String testMemberType;
    
    /**
     * Empty Class constructor.
     */
    public IdentitiesTest() {
        super("IdentitiesTest");
    }
    
    /**
     * This method provides the initial test set up and retrieve test
     * input data
     * @param testNum   test case number in propperties file
     * @param testName  test case name
     */
    @BeforeTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"testNum", "testName"})
    public void setup(String testNum, String testName)
    throws Exception {
        Object[] params = {testNum, testName};
        entering("setup", params);
        prefixTestName = testName + testNum;
        cfgMap = getDataFromCfgFile(prefixTestName, "idm" + fileseparator +
                testName);
        testCount = getParams(IDMConstants.IDM_KEY_COUNT);
        log(Level.FINEST, "IdentitiesTest", "Count = " + testCount);
        testDescription = getParams(IDMConstants.IDM_KEY_DESCRIPTION);
        log(Level.FINEST, "IdentitiesTest", "Description = " + testDescription);
        testRealm = getParams(IDMConstants.IDM_KEY_REALM_NAME);
        log(Level.FINEST, "IdentitiesTest", "Realm = " + testRealm);
        Reporter.log("========== BEGIN TEST CASE ==========");
        Reporter.log("Test Name: " + prefixTestName);
        Reporter.log("Description: " + testDescription);
        Reporter.log("Realm: " + testRealm);
        Reporter.log("Number of Items: " + testCount);
        try {
            ssoToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(ssoToken)) {
                log(Level.SEVERE, "IdentitiesTest", "Sso token is invalid");
                assert false;
            }
        } catch (Exception e) {
            e.getStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * This method executes the test cases of create, update, delete, search,
     * add member, remove member for all identities
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIdentities()
    throws Exception {
        entering("testIdentities", null);
        for (int i = 0; i < Integer.parseInt(testCount); i++) {
            testAction = getParams(IDMConstants.IDM_KEY_ACTION, i);
            testIdName = getParams(IDMConstants.IDM_KEY_IDENTITY_NAME, i);
            testIdType = getParams(IDMConstants.IDM_KEY_IDENTITY_TYPE, i);
            testIdAttr = getParams(IDMConstants.IDM_KEY_IDENTITY_ATTR, i);
            testExpectedErrCode =
                    getParams(IDMConstants.IDM_KEY_EXPECTED_ERROR_CODE, i);
            testExpectedErrMsg =
                    getParams(IDMConstants.IDM_KEY_EXPECTED_ERROR_MESSAGE, i);
            testExpectedResult =
                    getParams(IDMConstants.IDM_KEY_EXPECTED_RESULT, i);
            testMemberName =
                    getParams(IDMConstants.IDM_KEY_IDENTITY_MEMBER_NAME, i);
            testMemberType =
                    getParams(IDMConstants.IDM_KEY_IDENTITY_MEMBER_TYPE, i);
            log(Level.FINEST, "testIdentities", "Action = " + testAction);
            log(Level.FINEST, "testIdentities", "ID Name = " + testIdName);
            log(Level.FINEST, "testIdentities", "ID Type = " + testIdType);
            Reporter.log("Action " + i + " : " + testAction);
            Reporter.log("Name " + i + " : " + testIdName);
            Reporter.log("Type " + i + " : " + testIdName);
            Reporter.log("Attributes " + i + " : " + testIdAttr);
            Reporter.log("Member Name " + i + " : " + testMemberName);
            Reporter.log("Expected Error Code " + i + " : " +
                    testExpectedErrCode);
            Reporter.log("Expected Error Message " + i + " : " +
                    testExpectedErrMsg);
            Reporter.log("Expected Results " + i + " : " + testExpectedResult);
            if (testIdType == null) {
                log(Level.FINE, "testIdentities", "Invalid IDType " +
                        testIdType);
                assert false;
                break;
            } else if (!isIdTypeSupported(ssoToken, testRealm, testIdType)) {
                log(Level.FINE, "testIdentities", "IDType " + testIdType +
                        " is not supported in this deployment.");
                break;
            }
            if (testAction.equals("create")) {
                try {
                    assert(createID(testIdName, testIdType, testIdAttr, 
                            ssoToken, testRealm));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::create",
                                idre.getMessage() + " " + idre.getErrorCode());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::create", e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("delete")) {
                try {
                    assert(deleteID(testIdName, testIdType, ssoToken, 
                            testRealm));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::delete",
                                idre.getMessage() + " " + idre.getErrorCode());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::delete", e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("update")) {
                try {
                    assert(update(testIdName, testIdType,
                            setIDAttributes(testIdAttr)));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::update",
                                idre.getMessage() + " " + idre.getErrorCode());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::update", e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("search")) {
                try {
                    assert(search(testIdName, testIdType));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::search",
                                idre.getMessage() + " " + idre.getErrorCode());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::search", e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("addmember")) {
                try {
                    assert(addMembers(testIdName, testIdType,
                            testMemberName, ssoToken, testRealm));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::addmember",
                                idre.getMessage());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::addmember",
                            e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("removemember")) {
                try {
                    assert(removeMembers(testIdName, testIdType,
                            testMemberName, ssoToken, testRealm));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::removemember",
                                idre.getMessage());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::removemember",
                            e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (testAction.equals("getmember")) {
                try {
                    assert(getMembers(testIdName, testIdType, testMemberType));
                } catch (IdRepoException idre) {
                    if (checkIDMExpectedErrorMessageCode(idre,
                            testExpectedErrMsg, testExpectedErrCode))
                        assert true;
                    else {
                        log(Level.SEVERE, "testIdentities::getmember",
                                idre.getMessage());
                        idre.printStackTrace();
                        throw idre;
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "testIdentities::getmember",
                            e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else {
                log(Level.SEVERE, "testIdentities", "Invalid test action, " +
                        testAction + ", for test index " + i);
                assert false;
            }
            exiting("testIdentities");
        }
    }
    
    /**
     * This method remove all identites that are created during the test
     * execution and are not cleaned up due to test failure or exception
     */
    @AfterTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        Reporter.log("=========== END TEST CASE ===========");
        AMIdentityRepository idrepo =
                new AMIdentityRepository(ssoToken, testRealm);
        Set types = idrepo.getSupportedIdTypes();
        Iterator iter = types.iterator();
        while (iter.hasNext())
            cleanupIdentities((IdType)iter.next());
        exiting("cleanup");
    }
    
    /**
     * This method remove identity of different type that is passed from
     * the argument
     */
    private void cleanupIdentities(IdType idType)
    throws Exception {
        entering("cleanupIdentities", null);
        Set result = searchIdentities(ssoToken, "*", idType,
                testRealm);
        if (!result.isEmpty()) {
            log(Level.FINEST,"cleanupIdentities",
                    "Identities found. Start the clean up " +
                    result.toString());
            Iterator iter = result.iterator();
            AMIdentity id;
            while (iter.hasNext()) {
                id = (AMIdentity)iter.next();
                log(Level.FINEST, "cleanupIdentities", "Deleting " +
                        idType.getName() + " " + id.getName());
                deleteID(id.getName(), idType.getName(),ssoToken, testRealm);
            }
        }
        exiting("cleanupIdentities");
    }
    
    /**
     * This method searches for an identity with given identity name and type
     * @param idName identity name or pattern
     * @param idType identity type
     * @return true if identity found
     */
    public boolean search(String idName, String idType)
    throws Exception {
        entering("search", null);
        boolean opSuccess = false;
        log(Level.FINE, "search", "Searching identity " + idType +
                " name or pattern " + idName + "...");
        Set idResult = searchIdentities(ssoToken, idName, getIdType(idType),
                testRealm);
        List idNameList = getAttributeList(testExpectedResult,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        log(Level.FINEST, "search", "Search result " + idResult.toString() +
                " expected result " + idNameList.toString());
        Iterator iIter = idResult.iterator();
        
        if (idResult.isEmpty() && idNameList.isEmpty())
            opSuccess = true;
        else {
            AMIdentity amid;
            while (iIter.hasNext()) {
                amid = (AMIdentity)iIter.next();
                if (!idNameList.contains(amid.getName())) {
                    opSuccess = false;
                    break;
                } else
                    opSuccess = true;
            }
        }
        if (opSuccess)
            log(Level.FINE, "search", idType + " " + idName +
                    " is searched and found successfully.");
        else
            log(Level.FINE, "create", "Failed to search " + idType +
                    " " + idName);
        exiting("search");
        return (opSuccess);
    }
    
    /**
     * This method updates an identity with identity name, type, and attributes
     * map.
     * @param idName identity name
     * @param idType identity type - user, agent, role, filtered role, group
     * @param attrMap map of identity attributes i.e. [userpassword = newone]
     * @return true if identity is updated successfully
     */
    public boolean update(String idName, String idType, Map attrMap)
    throws Exception {
        entering("update", null);
        boolean opSuccess = false;
        log(Level.FINE, "update", "Updating identity " + idType +
                " name " + idName + "...");
        log(Level.FINEST, "update", "Attributes from input file" +
                attrMap.toString());
        AMIdentity uId = getFirstAMIdentity(ssoToken, idName,
                getIdType(idType), testRealm);
        modifyIdentity(uId, attrMap);
        // Verification steps
        Set keys = attrMap.keySet();
        Iterator keyIter = keys.iterator();
        String key;
        Set expectedVal;
        Set updatedVal;
        while (keyIter.hasNext()) {
            key = (String)keyIter.next();
            expectedVal = (Set)attrMap.get(key);
            // Ignore if attribute is userpassword
            if (!key.equals("userpassword")) {
                updatedVal = uId.getAttribute(key);
                log(Level.FINEST, "update","key =" + key +
                        " updated value = " + updatedVal.toString() +
                        " expected value = " + expectedVal.toString());
                if (!updatedVal.equals(expectedVal)) {
                    opSuccess = false;
                    break;
                } else
                    opSuccess = true;
            }
        }
        if (opSuccess)
            log(Level.FINE, "update", idType + " " + idName +
                    " is updated successfully.");
        else
            log(Level.FINE, "update", "Failed to update  " + idType +
                    " " + idName);
        exiting("update");
        return (opSuccess);
    }
    
    /**
     * This method retrieve a list of member based on member type
     * @param idName identity name
     * @param idType identity type
     * @param memberType member type
     * @return true if all members are retrieved successfully
     */
    public boolean getMembers(String idName, String idType, String memberType)
    throws Exception {
        entering("getMembers", null);
        boolean opSuccess = false;
        log(Level.FINE, "getMembers", "Listing members with identity type " +
                memberType + "...");
        // Remove user member to role or group identity
        Set idNameSet = getMembers(ssoToken, idName, getIdType(idType),
                getIdType(memberType), testRealm);
        // Verification step.  Check to make sure member matches with idtype
        // and with expectedc result
        Iterator iter = idNameSet.iterator();
        AMIdentity amIdentity;
        List idNameList = getAttributeList(testExpectedResult,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        String actualType;
        log(Level.FINEST, "getMembers", "List of members : " + 
                idNameSet.toString());
        if (idNameSet.isEmpty() && idNameList.isEmpty())
            opSuccess = true;
        else {
            while (iter.hasNext()) {
                amIdentity = (AMIdentity) iter.next();
                actualType = amIdentity.getType().getName();
                log(Level.FINEST, "getMembers", "Identity name = " + 
                    amIdentity.getName() +  " .Identity type = " + actualType);
                if (!actualType.equals(memberType)) {
                log(Level.SEVERE, "getMembers", "Member " + 
                        amIdentity.getName() + " has a wrong identity type " + 
                        actualType);
                    opSuccess = false;
                    break;
                } else 
                    opSuccess = true;
                if (!idNameList.contains(amIdentity.getName())) {
                    log(Level.SEVERE, "getMembers", "Member " + 
                        amIdentity.getName() + 
                        " is not in expected result list");
                    opSuccess = false;
                    break;
                } else
                    opSuccess = true;
            }
        }
        if (opSuccess)
            log(Level.FINE, "getMembers", "All members in " + idType + " " +
                    idName + " is retrieved successfully");
        else
            log(Level.FINE, "getMembers", "Failed to retrieve members");
        exiting("listMembers");
        return opSuccess;
    }
    
    /**
     * This method checks if an identity exists.  It uses defaults value of
     * identity name and type in the properties file.
     * @return true if identity exists
     */
    public boolean doesIdentityExists()
    throws Exception {
        return doesIdentityExists(this.testIdName, this.testIdType,
                this.ssoToken, this.testRealm);
    }
    
    /**
     * This method return value of test case property based on the key
     * @param key test case property key
     * @return test case property value
     */
    protected String getParams(String key) {
        String paramKey = prefixTestName + "." + key;
        return (String)cfgMap.get(paramKey);
    }
    
    /**
     * This method return value of test case property based on the key
     * @param key test case property key
     * @return test case property value
     */
    protected String getParams(String key, int idx) {
        String paramKey = prefixTestName + "." + key + "." + idx;
        return (String)cfgMap.get(paramKey);
    }
}
