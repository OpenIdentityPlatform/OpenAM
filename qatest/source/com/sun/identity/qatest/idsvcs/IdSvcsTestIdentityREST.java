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
 * $Id: IdSvcsTestIdentityREST.java,v 1.10 2009/02/24 06:58:52 vimal_67 Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idsvcs;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.IdSvcsCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class contains generic methods to test Identity REST service interfaces. 
 * It retrieves the parameters and attributes from the 
 * properties file and generates two maps. These maps are passed on to the 
 * common methods to call the REST operations like create, search, read, update
 * delete, isTokenValid, authenticate and attributes
 */
public class IdSvcsTestIdentityREST extends TestCommon {

    private ResourceBundle rb_amconfig;
    private ResourceBundle rbid;
    private TextPage page;
    private IdSvcsCommon idsvcsc;
    private IDMCommon idmcommon;
    private WebClient webClient;
    private String idsProp = "IdSvcsTestIdentityREST";
    private int index;
    private String strTestRealm;
    private String strSetup;
    private String strCleanup;
    private String admToken = "";
    private String userToken = "";
    private String serverURI = "";
    private Boolean idTypeSupported = false;
    private SSOToken idTypeSupportedToken;
        
    /**
     * Class constructor Definition
     */
    public IdSvcsTestIdentityREST()
            throws Exception {
        super("IdSvcsTestIdentityREST");
        rb_amconfig = ResourceBundle.getBundle("AMConfig");
        strTestRealm = rb_amconfig.getString("execution_realm");
        rbid = ResourceBundle.getBundle("idsvcs" + fileseparator + idsProp);
        idmcommon = new IDMCommon();
        idsvcsc = new IdSvcsCommon();
    }
    
    /**
     * Creates required setup
     */
    @Parameters({"testNumber", "setup", "cleanup"})
    @BeforeClass(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void setup(String testNumber, String setup, 
            String cleanup) throws Exception {
        Object[] params = {testNumber, setup, cleanup};
        entering("setup", params);
        try {
            index = new Integer(testNumber).intValue();
            strSetup = setup;
            strCleanup = cleanup;
            serverURI = protocol + ":" + "//" + host + ":" + port + uri;
            
            // admin user token for idTypeSupported function
            idTypeSupportedToken = getToken(adminUser, adminPassword, basedn);
                                     
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }

    /**
     * Calling Identity REST Operations through common URL method
     */
    @Test(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec", 
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void testIdSvcsREST()
            throws Exception {
        entering("testIdSvcsREST", null);
        try {
            int i = 0;
            int operations = 0;
            webClient = new WebClient();
            admToken = idsvcsc.authenticateREST(adminUser, adminPassword);
                operations = new Integer(rbid.getString(idsProp + index + 
                        "." + "operations")).intValue();
                String description = rbid.getString(idsProp + index + "." +
                        "description");
                String expResult = rbid.getString(idsProp + index + "." +
                        "expectedresult");
                Reporter.log("TestCase ID: " + idsProp + index);
                Reporter.log("Test description: " + description);
                Reporter.log("Expected Result: " + expResult);
                
                while (i < operations) {
                    String operationName = rbid.getString(idsProp + index +
                            "." + "operation" + i + "." + "name"); 
                    Reporter.log("Operation: " + operationName);
                    if (operationName.equals("create")) {
                        Map<String, Set<String>> anmap = new HashMap(); 
                        Map pmap = new HashMap();  
                        String identity_name = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "identity_name");
                        String identity_type = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "identity_type");
                        String attributes = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        pmap.put("identity_name", identity_name);
                        pmap.put("identity_type", identity_type);
                        if (identity_type.equalsIgnoreCase("AgentOnly")) {
                            Set set = new HashSet();
                            if (attributes.contains("AgentType=WebAgent")) {
                                set.add(protocol + ":" + "//" + host +
                                        ":" + port);                            
                            } else {
                                set.add(serverURI);                            
                            }                            
                            anmap.put("AGENTURL", set);
                        }
                        pmap.put("identity_realm", 
                                URLEncoder.encode(strTestRealm));                        
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            Reporter.log("Create Identity: " + identity_name); 
                            Reporter.log("Type: " + identity_type);
                            page = idsvcsc.commonURLREST(operationName, pmap, 
                                    anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("search")) {
                        Map<String, Set<String>> anmap = new HashMap();    
                        Map pmap = new HashMap();     
                        String filter = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "filter");
                        String attributes = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "attributes");
                        String exist = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "exist");
                        String objecttype = rbid.getString(idsProp + 
                                    index + "." + "operation" + i + 
                                    "." + "objecttype");
                        pmap.put("filter", filter);
                        anmap = parseStringToMap(attributes, ",", "&");
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                objecttype);
                        if (idTypeSupported) {
                            Reporter.log("Search filter: " + filter);
                            page = idsvcsc.commonURLREST(operationName, pmap, 
                                anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        
                            // search filter contains "*"
                            if (filter.contains("*")) {
                                String identities = rbid.getString(idsProp + 
                                        index + "." + "operation" + i +
                                        "." + "identities");
                                String[] iden = getArrayOfString(identities);
                                if (exist.equals("yes")) {
                                    idsvcsc.commonSearchREST(objecttype,
                                            admToken, page, filter, 
                                            iden, Boolean.TRUE);
                                } else {
                                    idsvcsc.commonSearchREST(objecttype, 
                                            admToken, page, filter, 
                                            iden, Boolean.FALSE);
                                }                                        
                            } 
                        
                            // search filter does not contain "*"
                            else {
                                String str = page.getContent();
                                if (exist.equals("yes")) {
                                    if (!str.contains(filter)) {
                                        log(Level.SEVERE, "testIdSvcsREST", 
                                                operationName + " Identity " +
                                                "does not exists: " + filter);
                                        assert false; 
                                    }
                                } else {
                                    if (str.contains(filter)) {
                                        log(Level.SEVERE, "testIdSvcsREST",
                                                operationName + 
                                                " Identity exists: " + filter);
                                        assert false; 
                                    }
                                } 
                            }
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("read")) {
                        Map<String, Set<String>> anmap = new HashMap();   
                        Map pmap = new HashMap();    
                        String identity_name = rbid.getString(idsProp + 
                                index + "." + "operation" + i + 
                                "." + "identity_name");
                        String objecttype = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "objecttype");
                        String attributes = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        pmap.put("name", identity_name);
                        pmap.put("attributes_names", "objecttype");
                        pmap.put("attributes_values_objecttype", objecttype);
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                objecttype);
                        if (idTypeSupported) {
                            Reporter.log("Read Attributes: " + identity_name);
                            page = idsvcsc.commonURLREST(operationName, pmap, 
                                anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("update")) {
                        Map<String, Set<String>> anmap = new HashMap();    
                        Map pmap = new HashMap();     
                        String identity_name = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "identity_name");
                        String identity_type = rbid.getString(idsProp + 
                                index + "." + "operation" + i + 
                                "." + "identity_type");
                        String attributes = rbid.getString(idsProp + 
                                index + "." + "operation" + i + 
                                "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        pmap.put("identity_name", identity_name);
                        pmap.put("identity_type", identity_type);
                        pmap.put("attributes_names", "objecttype");
                        pmap.put("attributes_values_objecttype", identity_type);
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            Reporter.log("Update Attributes: " + identity_name);
                            page = idsvcsc.commonURLREST(operationName, pmap, 
                                anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("delete") &&
                            strCleanup.equals("false")) {
                        Map<String, Set<String>> anmap = new HashMap(); 
                        Map pmap = new HashMap();  
                        String identity_name = rbid.getString(idsProp + 
                                index + "." + "operation" + i + 
                                "." + "identity_name");
                        String identity_type = rbid.getString(idsProp +
                                index + "." + "operation" + i + 
                                "." + "identity_type");
                        String attributes = rbid.getString(idsProp + 
                                index + "." + "operation" + i + 
                                "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        pmap.put("identity_name", identity_name);
                        pmap.put("identity_type", identity_type);
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            Reporter.log("Delete Identity: " + identity_name); 
                            Reporter.log("Type: " + identity_type);
                            page = idsvcsc.commonURLREST(operationName, pmap, 
                                anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("isTokenValid")) {
                        Map<String, Set<String>> anmap = new HashMap();
                        Map pmap = new HashMap(); 
                        String identity_type = rbid.getString(idsProp +
                                index + "." + "operation" + i + 
                                "." + "identity_type");
                        String attributes = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        String paramName = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "parameter_name");
                        String userType = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "user_type");
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            Reporter.log("User Type: " + userType); 
                            Reporter.log("isTokenvalid: " + paramName);
                            if (userType.equals("normaluser")) {
                                if (paramName.equals("tokenid")) { 
                                    if (!userToken.contains("%")) { 
                                        pmap.put("tokenid", 
                                            URLEncoder.encode(userToken, 
                                            "UTF-8"));
                                    } else {
                                        pmap.put("tokenid", userToken);
                                    }                                  
                                } else {
                                    if (!userToken.contains("%")) { 
                                        pmap.put("iPlanetDirectoryPro", 
                                            URLEncoder.encode(userToken, 
                                            "UTF-8"));
                                    } else {
                                        pmap.put("iPlanetDirectoryPro", 
                                            userToken);
                                    }                                    
                                }
                                page = idsvcsc.commonURLREST(operationName, 
                                        pmap, anmap, userToken);
                            
                                // releasing user token
                                idsvcsc.commonLogOutREST(userToken);
                            } else {
                                if (paramName.equals("tokenid")) { 
                                    if (!admToken.contains("%")) { 
                                        pmap.put("tokenid", 
                                            URLEncoder.encode(admToken, 
                                            "UTF-8"));
                                    } else {
                                        pmap.put("tokenid", admToken);
                                    }                                    
                                } else {
                                    if (!admToken.contains("%")) { 
                                        pmap.put("iPlanetDirectoryPro", 
                                            URLEncoder.encode(admToken, 
                                            "UTF-8"));
                                    } else {
                                        pmap.put("iPlanetDirectoryPro", 
                                            admToken);
                                    }         
                                }
                                page = idsvcsc.commonURLREST(operationName, 
                                        pmap, anmap, admToken);
                            }
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                            String str = "boolean=true";
                            if (!page.getContent().contains(str)) 
                                assert false;
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("authenticate")) {
                        Map<String, Set<String>> anmap = new HashMap();
                        Map pmap = new HashMap(); 
                        String username = rbid.getString(idsProp + index + 
                                "." + "operation" + i + "." + "username");
                        String password = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "password");
                        String identity_type = rbid.getString(idsProp + index +
                                "." + "operation" + i + "." + "identity_type");
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            Reporter.log("Username: " + username);
                            Reporter.log("Password: " + password);
                            userToken = idsvcsc.authenticateREST(username, 
                                    password);
                            pmap.put("tokenid", URLEncoder.encode(userToken, 
                                            "UTF-8"));
                            page = idsvcsc.commonURLREST("isTokenValid", 
                                        pmap, anmap, userToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                            String str = "boolean=true";
                            if (!page.getContent().contains(str)) 
                                assert false;
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else if (operationName.equals("attributes")) {
                        Map<String, Set<String>> anmap = new HashMap(); 
                        Map pmap = new HashMap(); 
                        String identity_type = rbid.getString(idsProp +
                                index + "." + "operation" + i + 
                                "." + "identity_type");
                        String attributes = rbid.getString(idsProp + 
                                index + "." + "operation" + i +
                                "." + "attributes");
                        anmap = parseStringToMap(attributes, ",", "&");
                        idTypeSupported = idmcommon.isIdTypeSupported(
                                idTypeSupportedToken, strTestRealm, 
                                identity_type);
                        if (idTypeSupported) {
                            page = idsvcsc.commonURLREST(operationName, pmap,
                                    anmap, admToken);
                            log(Level.FINEST, "testIdSvcsREST",
                                    "Page for " + operationName  + " : " +
                                    page.getContent());
                        } else {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                        }
                    } else {
                        if (strCleanup.equals("false")) {
                            log(Level.FINEST, "testIdSvcsREST", 
                                    "Not a Valid REST Operation " + 
                                    operationName);
                        }
                    }
                    i++;
                }
        } catch (Exception e) {
            log(Level.SEVERE, "testIdSvcsREST", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } finally {
            
            // releasing admin user token
            idsvcsc.commonLogOutREST(admToken);
        }
        exiting("testIdSvcsREST");
    }
        
    /**
     * Cleanup method. This method:
     * (a) Delete users
     * (b) Deletes policies
     */
    @AfterClass(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec", 
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        try {
            admToken = idsvcsc.authenticateREST(adminUser, adminPassword);
            if (strCleanup.equals("true")) {
                int i = 0;
                int operations = 0;
                operations = new Integer(rbid.getString(idsProp + index +
                        "." + "operations")).intValue();
                while (i < operations) {
                    String operationName = rbid.getString(idsProp + index + 
                            "." + "operation" + i + "." + "name"); 
                    Map<String, Set<String>> anmap = new HashMap();
                    Map pmap = new HashMap(); 
                    String identity_name = rbid.getString(idsProp + index +
                            "." + "operation" + i + "." + "identity_name");
                    String identity_type = rbid.getString(idsProp + index + 
                            "." + "operation" + i + "." + "identity_type");
                    String attributes = rbid.getString(idsProp + index +
                            "." + "operation" + i + "." + "attributes");
                    anmap = parseStringToMap(attributes, ",", "&");
                    pmap.put("identity_name", identity_name);
                    pmap.put("identity_type", identity_type);
                    idTypeSupported = idmcommon.isIdTypeSupported(
                            idTypeSupportedToken, strTestRealm, identity_type);
                    if (idTypeSupported) {
                        page = idsvcsc.commonURLREST(operationName, pmap, 
                                anmap, admToken); 
                    } else {
                        log(Level.FINEST, "testIdSvcsREST", 
                                    operationName + " IdType Not Supported");
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
                        
            // releasing admin user token
            idsvcsc.commonLogOutREST(admToken);
            
            // releasing admin user token of idTypeSupported function
            if (validateToken(idTypeSupportedToken)) 
                destroyToken(idTypeSupportedToken);
        }
        exiting("cleanup");
    }
        
}
