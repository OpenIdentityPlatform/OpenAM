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
 * $Id: IDMDatastorePluginSupportTest.java,v 1.2 2009/01/27 00:05:40 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.DelegationCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.session.SessionConstants;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the various options for user config datastore.
 */
public class IDMDatastorePluginSupportTest extends IDMCommon {
    private SSOToken ssoToken;
    private String prefixTestName;
    private String testRealm;
    private String testOperation;
    private String testSchemaAttUpdate;
    private String testIdType;
    private String testIdName;
    private String testIdAttrs;
    private String testExpResult;
    private String testDesc;
    private String datastoreName;
    private boolean bIdSrch;
    private ResourceBundle rb;
    private SMSCommon smsc;
    private IDMCommon idmc;
    private DelegationCommon delc;
    private IdType idType;
    
    /**
     * Empty Class constructor.
     */
    public IDMDatastorePluginSupportTest() {
        super("IDMDatastorePluginSupportTest");
    }
    
    /**
     * This method provides the initial test set up and retrieve test
     * input data
     * @param testNum   test case number in propperties file
     */
    @BeforeTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"testNum"})
    public void setup(String testNum)
    throws Exception {
        Object[] params = {testNum};
        entering("setup", params);
        prefixTestName = "IDMDatastorePluginSupportTest" + testNum;
        rb = ResourceBundle.getBundle("idm" + fileseparator +
                "IDMDatastorePluginSupportTest");
        try {
            ssoToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(ssoToken)) {
                log(Level.SEVERE, "IDMDatastorePluginSupportTest", "SSO" +
                        " token is invalid");
                assert false;
            }
            smsc = new SMSCommon(ssoToken);
            idmc = new IDMCommon();
            delc = new DelegationCommon("IDMDatastorePluginSupportTest");
            datastoreName = (String)(smsc.getCreatedDatastoreNames(1,
                    "idm")).get(0);
            log(Level.FINEST, "setup", "Datastore name: " + datastoreName);
        } catch (Exception e) {
            e.getStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * This method executes the test cases of read, create, edit, delete and 
     * search operations for all identities
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIdentities()
    throws Exception {
        entering("testIdentities", null);      

        testRealm = rb.getString(prefixTestName + "." + "realm");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Realm: " +
                testRealm);
        Reporter.log("Datastore realm: " + testRealm);

        testOperation = rb.getString(prefixTestName + "." + "operation");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Operation " +
                testOperation);
        Reporter.log("Datastore operation: " + testOperation);        

        testSchemaAttUpdate = rb.getString(prefixTestName + "." +
                "schema-att-update");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Schema attribute" +
                " being update: " + testSchemaAttUpdate);
        Reporter.log("Attribute names and values being updated: " +
                testSchemaAttUpdate);

        testIdType = rb.getString(prefixTestName + "." + "identity-type");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Identity type: " +
                testIdType);
        Reporter.log("Identity type under test: " + testIdType);

        testIdName = rb.getString(prefixTestName + "." + "identity-name");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Identity name: " +
                testIdName);
        Reporter.log("Identity name under test: " + testIdName);

        testIdAttrs = rb.getString(prefixTestName + "." +
                "identity-attributes");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Identity" +
                " attributes: " + testIdAttrs);
        Reporter.log("Identity attributes: " + testIdAttrs);

        bIdSrch = new Boolean(rb.getString(prefixTestName + "." +
                "identity-search")).booleanValue();
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Search specific" +
                " identity: " + bIdSrch);
        Reporter.log("Search specific identity or not: " + bIdSrch);
        
        testExpResult = rb.getString(prefixTestName + "." + "expected-result");
        log(Level.FINEST, "IDMDatastorePluginSupportTest", "Expected result: " +
                testExpResult);
        Reporter.log("Expected result: " + testExpResult);

        testDesc = rb.getString(prefixTestName + "." + "description");
        Reporter.log("Test description: " + testDesc);

        if (testIdType == null) {
            log(Level.SEVERE, "testIdentities", "Invalid IDType " + testIdType);
            assert false;
        } else if (!isIdTypeSupported(ssoToken, testRealm, testIdType)) {
            log(Level.FINE, "testIdentities", "IDType " + testIdType +
                    " is not supported in this deployment.");
        }
        
        boolean bPlugin = smsc.isPluginConfigured(
                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMDS, testRealm);
        log(Level.FINEST, "testIdentities", "Is plugin of type Sun DS with" +
                " OpenSSO schema: " + bPlugin);
        
        if (!bPlugin && (idType.equals("role") ||
                idType.equals("filteredrole"))) {
            log(Level.FINE, "testIdentities", "Skipping tests as this" +
                    " datastore plugin does not support operations for the" +
                    " identities under test.");            
        } else {        
            idType = getIdType(testIdType);

            if (testOperation.equals("read") || testOperation.equals("search"))
            {
                if (bIdSrch) {
                    testRead(testIdName);
                } else {
                    testRead();
                }
            }

            if (testOperation.equals("create")) {
                testCreate();
            }

            if (testOperation.equals("edit")) {
                testEdit();
            }

            if (testOperation.equals("delete")) {
                testEdit();
            }

            if (testIdType.equals("user") && testOperation.equals("service")) {
                testUserService();
            }
        }
        exiting("testIdentities");
    }
    
    /**
     * This method remove all identites that are created during the test
     * execution and are not cleaned up due to test failure or exception
     */
    @AfterTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);        
        try {
            AMIdentityRepository idrepo =
                    new AMIdentityRepository(ssoToken, testRealm);
            Set types = idrepo.getSupportedIdTypes();
            Iterator iter = types.iterator();
            while (iter.hasNext())
                cleanupIdentities((IdType)iter.next());

            // This cleanup is called beacuse one of the testcase creates an
            // identity using cn as user search attribute and not cleaned up
            // by the previous cleanup.
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("cn");
            map.put("sun-idrepo-ldapv3-config-users-search-attribute", set);
            smsc.updateDataStore(testRealm, datastoreName, map, false);
            idrepo = new AMIdentityRepository(ssoToken, testRealm);
            types = idrepo.getSupportedIdTypes();
            iter = types.iterator();
            while (iter.hasNext())
                cleanupIdentities((IdType)iter.next());            
        } catch (Exception e) {
            e.getStackTrace();
            throw e;
        } finally {
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("uid");
            map.put("sun-idrepo-ldapv3-config-users-search-attribute", set);
            smsc.updateDataStore(testRealm, datastoreName, map, false);            
        }        
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
                deleteID(id.getName(), idType.getName(), ssoToken, testRealm);
            }
        }
        exiting("cleanupIdentities");
    }
    
    /**
     * This method tests read operations after chaging datastore plugin
     * attributes
     * @throws java.lang.Exception
     */
    private void testRead() throws Exception {
        testRead(null);
    }


    /**
     * This method tests read operations after chaging datastore plugin
     * attributes
     * @param strIdName identity name to search for validation
     * @throws java.lang.Exception
     */
    private void testRead(String strIdName)
            throws Exception {
        Map defVal = null;
        try {
            Map updMap = parseStringToMap(testSchemaAttUpdate, "|", ";");
            log(Level.FINEST, "testRead", "updMap: " + updMap);
            defVal = getDefaultSchemaValues(updMap);

            // Create the identity before datastore is updated. This is done
            // for two cases: first when we create identity, update datastore
            // and search for that identity (this is the null check condition)
            // and second when identity creation is not possible after datastore
            // update (This is the false check condition)
            if (strIdName == null && testExpResult.equals("false")) {
                assert(createID(testIdName, testIdType, testIdAttrs, ssoToken,
                    testRealm));
            }
            
            smsc.updateDataStore(testRealm, datastoreName, updMap, false); 
            try {
                log(Level.FINEST, "testRead", "strIdName: " + strIdName);
                if (strIdName == null) {
                    searchIdentities(ssoToken, testIdName, idType, testRealm);
                } else {
                    if (testExpResult.equals("true")) {
                        assert(createID(testIdName, testIdType, testIdAttrs,
                            ssoToken, testRealm));
                    }
                    Set setId = searchIdentities(ssoToken, testIdName, idType,
                            testRealm);
                    log(Level.FINEST, "testRead", "Search result set" +
                            " is: " + setId.toString());
                    if (testExpResult.equals("true")) {
                        if (!setValuesHasString(setId, strIdName)) {
                            log(Level.SEVERE, "testRead", "Search" +
                                    " result set does not contain id name: " +
                                    strIdName);
                            assert false;
                        }
                    } else if (testExpResult.equals("false")) {
                        if (setValuesHasString(setId, strIdName)) {
                            log(Level.SEVERE, "testRead", "Search" +
                                    " result set contain's id name: " +
                                    strIdName);
                            assert false;
                        }                        
                    }                    
                }
            } catch (IdRepoException idre) {
                if (checkIDMExpectedErrorMessageCode(idre, testExpResult))
                    assert true;
                else {
                    log(Level.SEVERE, "testRead",
                            "Error message: " + idre.getMessage() +
                            " and Error Code: " + idre.getErrorCode());
                    idre.printStackTrace();
                    throw idre;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testRead", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (defVal != null) {
                smsc.updateDataStore(testRealm, datastoreName, defVal, false);        
            }
        }
    }

    /**
     * This method tests create operations after chaging datastore plugin
     * attributes
     * @throws java.lang.Exception
     */    
    private void testCreate()
            throws Exception {
        Map defVal = null;
        try {
            Map updMap = parseStringToMap(testSchemaAttUpdate, "|", ";");
            log(Level.FINEST, "testCreate", "updMap: " + updMap);
            defVal = getDefaultSchemaValues(updMap);
            smsc.updateDataStore(testRealm, datastoreName, updMap, false); 

            try {
                createID(testIdName, testIdType, testIdAttrs, ssoToken, 
                        testRealm);
            } catch (IdRepoException idre) {
                if (checkIDMExpectedErrorMessageCode(idre, testExpResult))
                    assert true;
                else {
                    log(Level.SEVERE, "testCreate",
                            "Error message: " + idre.getMessage() +
                            " and Error Code: " + idre.getErrorCode());
                    idre.printStackTrace();
                    throw idre;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testCreate", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (defVal != null) {
                smsc.updateDataStore(testRealm, datastoreName, defVal,
                        false);        
            }
        }
    }    
    
    /**
     * This method tests edit operations after chaging datastore plugin
     * attributes
     * @throws java.lang.Exception
     */        
    private void testEdit()
            throws Exception {
        Map defVal = null;
        try {
            Map updMap = parseStringToMap(testSchemaAttUpdate, "|", ";");
            log(Level.FINEST, "testEdit", "updMap: " + updMap);
            defVal = getDefaultSchemaValues(updMap);

            assert(createID(testIdName, testIdType, testIdAttrs, ssoToken,
                    testRealm));
            smsc.updateDataStore(testRealm, datastoreName, updMap, false); 

            try {
                AMIdentity uId = getFirstAMIdentity(ssoToken, testIdName,
                        idType, testRealm);
                modifyIdentity(uId, setIDAttributes(testIdAttrs));
            } catch (IdRepoException idre) {
                if (checkIDMExpectedErrorMessageCode(idre, testExpResult))
                    assert true;
                else {
                    log(Level.SEVERE, "testEdit",
                            "Error message: " + idre.getMessage() +
                            " and Error Code: " + idre.getErrorCode());
                    idre.printStackTrace();
                    throw idre;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testEdit", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (defVal != null) {
                smsc.updateDataStore(testRealm, datastoreName, defVal,
                        false);        
            }
        }
    }    

    /**
     * This method tests delete operations after chaging datastore plugin
     * attributes
     * @throws java.lang.Exception
     */        
    private void testDelete()
            throws Exception {
        Map defVal = null;
        try {
            Map updMap = parseStringToMap(testSchemaAttUpdate, "|", ";");
            log(Level.FINEST, "testDelete", "updMap: " + updMap);
            defVal = getDefaultSchemaValues(updMap);

            assert(createID(testIdName, testIdType, testIdAttrs, ssoToken,
                    testRealm));
            smsc.updateDataStore(testRealm, datastoreName, updMap, false); 

            try {
                deleteIdentity(ssoToken, testRealm, idType, testIdName);
            } catch (IdRepoException idre) {
                if (checkIDMExpectedErrorMessageCode(idre, testExpResult))
                    assert true;
                else {
                    log(Level.SEVERE, "testDelete",
                            "Error message: " + idre.getMessage() +
                            " and Error Code: " + idre.getErrorCode());
                    idre.printStackTrace();
                    throw idre;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testDelete", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (defVal != null) {
                smsc.updateDataStore(testRealm, datastoreName, defVal,
                        false);        
            }
        }
    }

    /**
     * This method tests service assigment operations after chaging datastore
     * plugin attributes
     * @throws java.lang.Exception
     */        
    private void testUserService()
            throws Exception {
        Map defVal = null;
        try {
            Map updMap = parseStringToMap(testSchemaAttUpdate, "|", ";");
            log(Level.FINEST, "testUserServcie", "updMap: " + updMap);
            defVal = getDefaultSchemaValues(updMap);

            assert(createID(testIdName, testIdType, testIdAttrs, ssoToken,
                    testRealm));
            smsc.updateDataStore(testRealm, datastoreName, updMap, false); 

            try {
                Map quotamap = new HashMap();
                Set set = new HashSet();
                set.add("1");
                quotamap.put(SessionConstants.SESSION_QUOTA_ATTR, set);
                delc.assignServiceToUser(ssoToken, testIdName,
                            SessionConstants.SESSION_SRVC, quotamap, testRealm);
            } catch (IdRepoException idre) {
                if (checkIDMExpectedErrorMessageCode(idre, testExpResult))
                    assert true;
                else {
                    log(Level.SEVERE, "testUserService",
                            "Error message: " + idre.getMessage() +
                            " and Error Code: " + idre.getErrorCode());
                    idre.printStackTrace();
                    throw idre;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testUserService", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (defVal != null) {
                smsc.updateDataStore(testRealm, datastoreName, defVal,
                        false);        
            }
        }
    }           
        
    /**
     * This method read data attribute keys from the supplied map, gets their
     * default values from service configuration and constructs a new map
     * containing the keys and default values for those keys.
     * @param map map containing new data attribute key value pairs
     * @return map containng default values for attributes
     * @throws java.lang.Exception
     */
    private Map getDefaultSchemaValues(Map map)
            throws Exception {
        Map<String, Set<String>> retMap = new HashMap<String, Set<String>>();
        Set keys = map.keySet();
        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            String key = (String)itr.next();
            Set set =  smsc.getAttributeValueServiceConfig(testRealm,
                    "sunIdentityRepositoryService", datastoreName, key);
            retMap.put(key, set);
        }
        log(Level.FINEST, "getDefaultSchemaValues", "Default schema" +
                " attribute values are: " + retMap.toString());
        return (retMap);
    }
} 
