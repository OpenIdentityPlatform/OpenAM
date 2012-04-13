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
 * $Id: SAMLv1xProfileTests.java,v 1.8 2009/03/13 17:41:12 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv1x;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SAMLv1Common;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * This class tests the SAMLv1x Artifact and Post Profiles with and with not
 * attributes
 */
public class SAMLv1xProfileTests extends TestCommon {

    AMIdentity amid;
    private ArrayList spattrmultiVal;
    private ArrayList idpattrmultiVal;
    private Set sesQrySet;    
    private Map<String, String> configMap;
    private String baseDir;
    private String xmlfile;
    SSOToken token;    
    private DefaultTaskHandler task1;
    private HtmlPage wpage;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    public WebClient spwebClient;
    public WebClient idpwebClient;
    public WebClient webClient;
    private String spurl;
    private String idpurl;
    private String testName;
    private String testType;
    private String testInit;
    private String testIndex;
    private String spmetaAliasname;
    private String idpmetaAliasname;       
    private String strUniversalid = "";     
    private IDMCommon idmc;
    
    /**
     * Constructor SAMLv1xProfileTests
     */
    public SAMLv1xProfileTests() {
        super("SAMLv1xProfileTests");
    }

    /**
     * Configures the SP and IDP load meta for the SAMLv1xProfileTests 
     * tests to execute. Here ptestName is the test name, ptestType is the 
     * test type, ptestInit is the one which says sp initiated or idp initiated
     * and nameidformatindex is the nameidformat index
     */
    @Parameters({"ptestName", "ptestType", "ptestInit", "nameidformatIndex"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String ptestName, String ptestType, String ptestInit, 
            String nameidformatIndex)
            throws Exception {
        ArrayList list;
        try {
            testName = ptestName;
            testType = ptestType;
            testInit = ptestInit;
            testIndex = nameidformatIndex; 
            idmc = new IDMCommon();
            sesQrySet = new HashSet();            
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator 
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) 
                    + SAMLv2Common.fileseparator + "built" 
                    + SAMLv2Common.fileseparator + "classes" 
                    + SAMLv2Common.fileseparator;
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator 
                    + "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator 
                    + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv1x" + fileseparator 
                    + "SAMLv1xProfileTests", configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);    
            spmetaAliasname = configMap.get(TestConstants.KEY_SP_METAALIAS);
            idpmetaAliasname = configMap.get(TestConstants.KEY_IDP_METAALIAS);
            getWebClient();
          
            // Create sp users
            consoleLogin(spwebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            list = new ArrayList();     
                                    
            // grabbing multivalues attribute of a sp user            
            spattrmultiVal = new ArrayList();   
            if (!testIndex.equals("0")) {
                list = (ArrayList) parseStringToList(configMap.get(
                        TestConstants.KEY_SP_USER_MULTIVALUE_ATTRIBUTE), 
                        ",", "&");
            }
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                String str = (String) iter.next();
                int index = str.indexOf("=");                
                if (index != -1) {      
                    String attrName = str.substring(0, index).trim();
                    if (!attrName.equalsIgnoreCase(configMap.get(TestConstants.
                            KEY_NAMEIDFORMAT_KEYVALUE + testIndex))) {                    
                        String strVal = str.substring(index+1).trim(); 
                        // session query parameter set
                        sesQrySet.add(attrName);
                        // sp attribute multivalues list
                        spattrmultiVal.add(strVal);                
                    }  
                }
            }       
            
            list.add("mail=" + configMap.get(TestConstants.KEY_SP_USER) +
                    "@" + spmetaAliasname);             
            list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");            
            if (FederationManager.getExitCode(fmSP.createIdentity(spwebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                assert false;
             }      
                        
            // retrieving the univesalid of a sp user and 
            // modifying the attribute  
            if (!testIndex.equals("0")) {
                token = getToken(configMap.get(TestConstants.KEY_SP_USER), 
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD), realm);
                strUniversalid = token.getProperty("sun.am." +
                        "UniversalIdentifier");     
                log(Level.FINEST, "setup", "Universalid: " + strUniversalid);
                if (validateToken(token)) 
                    destroyToken(token);  
                token = getToken(adminUser, adminPassword, realm);
                amid = idmc.getFirstAMIdentity(token, 
                        configMap.get(TestConstants.KEY_SP_USER), IdType.USER,
                        realm);
                Map mapAttr = new HashMap();       
                Set setAttrVal = new HashSet();
                setAttrVal.add(strUniversalid);                
                mapAttr.put(configMap.get(
                        TestConstants.KEY_NAMEIDFORMAT_KEYVALUE + testIndex),
                        setAttrVal);    
                // session query parameter set
                sesQrySet.add(configMap.get(
                        TestConstants.KEY_NAMEIDFORMAT_KEYVALUE + testIndex));
                idmc.modifyIdentity(amid, mapAttr);   
                if (validateToken(token)) 
                    destroyToken(token);
            }             
                         
            // Create idp users          
            consoleLogin(idpwebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            list.clear();
            
            // grabbing multivalues attribute of a idp user
            // and removes the namedidformat attribute from the list if present
            idpattrmultiVal = new ArrayList();
            if (!testIndex.equals("0")) {
                list = (ArrayList) parseStringToList(configMap.get(
                        TestConstants.KEY_IDP_USER_MULTIVALUE_ATTRIBUTE), 
                        ",", "&");
            }
            Iterator idpiter = list.iterator();
            while (idpiter.hasNext()) {
                String strg = (String) idpiter.next();
                int inx = strg.indexOf("=");                
                if (inx != -1) {
                    String attrName = strg.substring(0, inx).trim();
                    String strVal = strg.substring(inx+1).trim(); 
                    if (attrName.equalsIgnoreCase(configMap.get(TestConstants.
                            KEY_NAMEIDFORMAT_KEYVALUE + testIndex))) {
                        idpiter.remove();
                    } else {                           
                        // session query parameter set
                        sesQrySet.add(attrName);
                        // idp attribute multivalues list
                        idpattrmultiVal.add(strVal);  
                    }
                }                
            }      
            
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");              
            if (!testIndex.equals("0")) {
                list.add(configMap.get(TestConstants.KEY_NAMEIDFORMAT_KEYVALUE +
                        testIndex) + "=" + strUniversalid); 
                // session query parameter set
                sesQrySet.add(configMap.get(TestConstants.
                        KEY_NAMEIDFORMAT_KEYVALUE + testIndex));
            } else {
                list.add("mail=" + configMap.get(TestConstants.KEY_IDP_USER) +
                        "@" + idpmetaAliasname);
            }                   
            if (FederationManager.getExitCode(fmIDP.createIdentity(idpwebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed for IDP");
                assert false;
            }
            
            // adding nameidformat attribute to the SAML service
            if (!testIndex.equals("0")) {
                list.clear();
                list.add("iplanet-am-saml-name-id-format-attr-map=" +                           
                    configMap.get(TestConstants.KEY_NAMEIDFORMAT_KEY +
                    testIndex) + "=" + configMap.get(TestConstants.
                    KEY_NAMEIDFORMAT_KEYVALUE + testIndex));          
                fmSP.addAttrDefs(spwebClient, "iPlanetAMSAMLService", 
                    "Global", list, null);
                fmIDP.addAttrDefs(idpwebClient, "iPlanetAMSAMLService",
                    "Global", list, null);           
            }
            
            // adding saml attribute map
            list.clear();
            list = (ArrayList) parseStringToList(configMap.get(TestConstants.
                    KEY_ATTRMAP), ",", "");                  
            fmSP.addAttrDefs(spwebClient, "iPlanetAMSAMLService", 
                    "Global", list, null);
            fmIDP.addAttrDefs(idpwebClient, "iPlanetAMSAMLService",
                    "Global", list, null);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {          
            consoleLogout(spwebClient, spurl + "/UI/Logout");
            consoleLogout(idpwebClient, idpurl + "/UI/Logout");            
        }
        exiting("setup");
    }

    /**
     * Executes the tests of Artifact and Post Profiles. 
     * It also tests the mapping of a nameidformat to the 
     * user profile attributes.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv1xTest()
            throws Exception {
        try {
            String result = null;
            String attr = "";
            Boolean bool = false;
            getWebClient();
            if (!testIndex.equals("0")) {
                Reporter.log("Test Description: This test  " + testName +
                    " makes sure the nameidformat is mapped to user profile" + 
                    " attribute. This is " + testInit + " Initiated " +
                    testType + " Profile");                    
            } else {
                Reporter.log("Test Description: This test  " + testName +
                    " will run to make sure " +
                    " with test Type : " + testType +
                    " for  : " + testInit + " will work fine");
            }
            
            if (!testIndex.equals("0")) {
                result = strUniversalid;       
                attr = "&amp;NameIDFormat=" + configMap.get(
                        TestConstants.KEY_NAMEIDFORMAT_KEY + testIndex); 
                log(Level.FINEST, "samlv1xTest", "Result: " + result);      
                log(Level.FINEST, "samlv1xTest", "Attr: " + attr);           
            } else {
                if (testInit.equalsIgnoreCase("sp")) {                    
                result = configMap.get(TestConstants.KEY_IDP_USER) + "@"
                        + idpmetaAliasname;
                } else {
                result = configMap.get(TestConstants.KEY_SP_USER) + "@"
                        + spmetaAliasname;
                }              
            }    
            Thread.sleep(5000);
            xmlfile = baseDir + testName + "samlv1xTest" + ".xml";
            configMap.put(TestConstants.KEY_SSO_RESULT, result);
            SAMLv1Common.getxmlSSO(xmlfile, configMap, testType,
                    testInit, attr);                      
            Thread.sleep(5000);
            log(Level.FINEST, "samlv1xTest", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            wpage = task1.execute(webClient);
            Thread.sleep(5000);            
            if (wpage.getWebResponse().getContentAsString().contains(result)) {
                log(Level.FINEST, "samlv1xTest", "Found the right " +
                        "value of a attribute");                
                bool = true;
            } else {
                if (testInit.equalsIgnoreCase("sp")) {
                    Iterator iter = idpattrmultiVal.iterator();
                    while (iter.hasNext()) {
                        String multiVal = (String) iter.next();
                        if (wpage.getWebResponse().getContentAsString().
                                contains(multiVal)) {
                            log(Level.FINEST, "samlv1xAttrMapTest", 
                                " Found the value of a attribute " +
                                multiVal); 
                            bool = true;
                        }             
                    }
                } else {
                    Iterator iter = spattrmultiVal.iterator();
                    while (iter.hasNext()) {
                        String multiVal = (String) iter.next();
                        if (wpage.getWebResponse().getContentAsString().
                                contains(multiVal)) {
                            log(Level.FINEST, "samlv1xAttrMapTest", 
                                " Found the value of a attribute " +
                                multiVal);
                            bool = true;
                        }             
                    }                
                }
            }
            if (!bool) {
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv1xTest", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spwebClient, spurl + "/UI/Logout");
            consoleLogout(idpwebClient, idpurl + "/UI/Logout");
        }
    }  
        
    /**
     * Create the webClient which will be used for the rest of the tests.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getWebClient()
            throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
            spwebClient = new WebClient(BrowserVersion.FIREFOX_3);
            idpwebClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Deletes the users created for this test, removes the attributes values
     * added to the SAML service
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        ArrayList list;
        try {
            list = new ArrayList();
            consoleLogin(spwebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            consoleLogin(idpwebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            
            // removing nameid attribute from the SAML service
            if (!testIndex.equals("0")) {
                list.clear();
                list.add("iplanet-am-saml-name-id-format-attr-map=" +                           
                    configMap.get(TestConstants.KEY_NAMEIDFORMAT_KEY +
                    testIndex) + "=" + configMap.get(TestConstants.
                    KEY_NAMEIDFORMAT_KEYVALUE + testIndex));          
                fmSP.removeAttrDefs(spwebClient, "iPlanetAMSAMLService", 
                        "Global", list, null);                    
                fmIDP.removeAttrDefs(idpwebClient, "iPlanetAMSAMLService",
                        "Global", list, null);           
            }          
            
            // removing SAML attribute map 
            list.clear();
            list = (ArrayList) parseStringToList(configMap.get(TestConstants.
                    KEY_ATTRMAP), ",", "");                  
            fmSP.removeAttrDefs(spwebClient, "iPlanetAMSAMLService", 
                    "Global", list, null);
            fmIDP.removeAttrDefs(idpwebClient, "iPlanetAMSAMLService",
                    "Global", list, null);
            
            list.clear();
            list.add(configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(spwebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    list, "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command" +
                        " failed");
                assert false;
            }
           
            list.clear();
            list.add(configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(
                    idpwebClient, configMap.get(TestConstants.
                    KEY_IDP_EXECUTION_REALM), list, "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command " +
                        "failed");
                assert false;
            }            
            
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spwebClient, spurl + "/UI/Logout");
            consoleLogout(idpwebClient, idpurl + "/UI/Logout");
        }
    }   
}
