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
 * $Id: IDMTestConfig.java,v 1.6 2009/05/27 23:07:57 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * <code>IDMTestConfig</code> runs before each test suite to set up and clean 
 * up the test environment for idm tests.  It creates sub realm and datastore 
 * that is specified in SMSGlobalDatastoreConfig.properties before executing the 
 * tests.  It removes datastore and sub realm after running the tests.
 */
public class IDMTestConfig extends IDMCommon { 
    private SMSCommon smsObj;
    private Map dsCfgMap;
    private SSOToken ssotoken;
    private String subRealm;
    private String datastoreNum = "0";
    
    /** 
     * Creates a new instance of IDMDataStoreSetup 
     */
    public IDMTestConfig() 
    throws Exception {
        super("IDMTestConfig");
        try {
            ssotoken = getToken(adminUser, adminPassword, basedn);
            smsObj = new SMSCommon(ssotoken, "config" + fileseparator +
                    "default" + fileseparator + "UMGlobalConfig");
            dsCfgMap = new HashMap();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }    
    
    /**
     * This method creates a datastore and sub realm that are specified in 
     * properties file by index number.
     * @param dsindex index in the properties file to be created
     * @param setupds true to create a datastore
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"dsindex","setupidm"})
    public void setupIdmTest(String dsindex, boolean setupidm)
    throws Exception {
        Object[] params = {dsindex, setupidm};
        if (!setupidm) return;
        entering("setupIdmTest", params);
        try {
 
            dsCfgMap = smsObj.getDataStoreConfigByIndex(
                    Integer.parseInt(dsindex), "idm" + fileseparator +
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX);
            subRealm = (String)dsCfgMap.get(
                    SMSConstants.UM_DATASTORE_REALM + "." + datastoreNum);

             if ((subRealm != null) && !subRealm.equals("/")) {
                String childRealm = 
                        subRealm.substring(subRealm.lastIndexOf("/") + 1);
                if (searchRealms(ssotoken, childRealm).isEmpty()) {
                    log(Level.FINE,"setupIdmTest", "Creating sub realm " + 
                    subRealm + "...");
                    createIdentity(ssotoken, getParentRealm(subRealm), 
                            IdType.REALM, childRealm, new HashMap());
                }
            }

            ResourceBundle gblCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "default" + fileseparator +
                    "UMGlobalConfig");

            ResourceBundle cfgData =
                ResourceBundle.getBundle("Configurator-" + serverName +
                "-Generated");
            String umdatastore = cfgData.getString("umdatastore");

            if ((gblCfgData.getString("UMGlobalConfig." +
                            "createNewDatastores")).equals("true") &&
                            (!umdatastore.equals("embedded"))) {
                Set listDataStore = smsObj.listDataStore(subRealm);
                Iterator iterSet = listDataStore.iterator();
                String eds;
                while (iterSet.hasNext()) {
                    eds = (String)iterSet.next();
                    log(Level.FINE, "setupIdmTest", "Deleting existing" +
                            " datastore " +  eds);
                    smsObj.deleteDataStore(subRealm, eds);
                }
                
                // Create a datastore
                log(Level.FINE, "setupIdmTest", "Creating datastore..."); 
                Map<String, String> umDatastoreTypes =
                        new HashMap<String, String>();
                umDatastoreTypes.put("1", serverName);
                smsObj.createUMDatastoreGlobalMap(
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX, umDatastoreTypes,
                    SMSConstants.QATEST_EXEC_MODE_SINGLE, "idm");
                smsObj.createDataStore(1, "idm" + fileseparator +
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "-Generated");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setupIdmTest", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setupIdmTest");
    }
    
    /**
     * This method removes a datastore and sub realm that are specified in 
     * properties file by index number.
     * @param setupds true to create a datastore
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"cleanupidm"})
    public void cleanupIdmTest(boolean cleanupidm)
    throws Exception {
        Object[] params = {cleanupidm};
        entering("cleanupIdmTest", null);
        if (!cleanupidm) return;
        try {
            log(Level.FINE, "cleanupIdmTest", "Deleting realm: " + subRealm);
            deleteRealm(ssotoken, subRealm);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanupIdmTest", e.getMessage());
            e.printStackTrace();
        }
        exiting("cleanupIdmTest");
    }  
}