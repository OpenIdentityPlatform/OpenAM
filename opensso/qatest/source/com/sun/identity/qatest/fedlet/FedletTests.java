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
 * $Id: FedletTests.java,v 1.3 2009/08/20 17:07:35 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.fedlet;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.FedletCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests Http-Atrifact and HTTP-POST profiles. It tests the following
 * testcases:-
 * Fedlet(SP) initiated Single-Sign-On using HTTP-Artifact
 * IDP initiated Single-Sign-On using HTTP-Artifact
 * Fedlet(SP) initiated Single-Sign-On using HTTP-POST
 * IDP initiated Single-Sign-On using HTTP-POST
 */
public class FedletTests extends TestCommon {
    
    public WebClient webClient;
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task1;   
    private FederationManager fmfedletIDP;
    private String fedletURL;
    private HtmlPage page1;
    private HtmlPage fedletPage;
    private String testAttribute;
    private ArrayList idpattrmultiVal;
    String fedletidpurl;
    
    /**
     * This is constructor for this class.
     */
    public FedletTests() {
        super("FedletTests");
    }
    
    /**
     * This setup method creates the required users.
     */
    @Parameters({"ptestName", "pAttribute"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String ptestName, String pAttribute) 
    throws Exception {                
        ArrayList list;
        try {            
            log(Level.FINEST, "setup", "Entering");
            configMap = new HashMap<String, String>();
            testAttribute = pAttribute;
            
            //Upload global properties file in configMap
            FedletCommon.getEntriesFromResourceBundle("AMConfig", configMap);
            FedletCommon.getEntriesFromResourceBundle("fedlet" + fileseparator +
                    "FedletTests", configMap);
            log(Level.FINEST, "setup", "ConfigMap is : " + configMap);

            baseDir = getBaseDir() + fileseparator
                    + configMap.get(TestConstants.KEY_ATT_SERVER_NAME)
                    + fileseparator + "built" + fileseparator + "classes"
                    + fileseparator;                                  
            fedletURL = configMap.get(TestConstants.KEY_FEDLET_WAR_LOCATION);
            log(Level.FINEST, "setup", "Fedlet URL is : " + fedletURL);
            fedletPage = (HtmlPage) webClient.getPage(fedletURL);
            log(Level.FINEST, "setup", "Fedlet Index Page: " +
                    fedletPage.getWebResponse().getContentAsString());
            
            // Create idp user
            fedletidpurl = configMap.get(TestConstants.KEY_AMC_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_AMC_HOST) + ":"
                    + configMap.get(TestConstants.KEY_AMC_PORT)
                    + configMap.get(TestConstants.KEY_AMC_URI);
            consoleLogin(webClient, fedletidpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD));
            
            fmfedletIDP = new FederationManager(fedletidpurl);
            idpattrmultiVal = new ArrayList(); 
            list = new ArrayList();
            String idpuserMultiAttr = configMap.get(
                    TestConstants.KEY_IDP_USER_MULTIATTRIBUTES);
            list = (ArrayList) parseStringToList(idpuserMultiAttr, ",", "&");
            list.add("sn=" + configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            
            // grabbing idp attribute multivalues list
            Iterator iter = list.iterator();            
            while(iter.hasNext()) {
                String str = (String) iter.next();             
                int index = str.indexOf("=");
                if (index != -1) {
                    String attrName = str.substring(0, index).trim();                    
                    String strVal = str.substring(index + 1).trim();  
                               
                    // adding multivalues to list                   
                    if (attrName.equalsIgnoreCase(testAttribute)) {
                        idpattrmultiVal.add(strVal);
                    }                      
                }                
            }            
            if (FederationManager.getExitCode(
                    fmfedletIDP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER),
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed");
                assert false;
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Creates the webClient which will be used for rest of the tests.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getWebClient() 
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }   
    
    /**
     * Run fedlet(SP) initiated Single-Sign-On using HTTP Artifact binding      
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSPSSOHTTPArtifact()
    throws Exception {
        entering("testSPSSOHTTPArtifact", null);
        try {
            log(Level.FINEST, "testSPSSOHTTPArtifact", 
                    "Running: testSPSSOHTTPArtifact");
            String str = "Run Fedlet (SP) initiated Single Sign-On using " +
                    "HTTP Artifact binding";  
            Reporter.log("Test Description: This testcase tests the " +
                    "attribute " + testAttribute +  " in Fedlet (SP) " +
                    "initiated Single Sign-On test using " +
                    "HTTP Artifact binding");  
            log(Level.FINEST, "testSPSSOHTTPArtifact", str);
            log(Level.FINEST, "testSPSSOHTTPArtifact", "Attribute: " + 
                    testAttribute);
            getWebClient();
            xmlfile = baseDir + "testspssohttpartifact.xml";            
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);            
            log(Level.FINEST, "testSPSSOHTTPArtifact", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testSPSSOHTTPArtifact", "Page: " + 
                    page1.getWebResponse().getContentAsString());
            
            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();             
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testSPSSOHTTPArtifact",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }     
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSSOHTTPArtifact", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSSOHTTPArtifact");
    }
    
    /**
     * Run IDP initiated Single-Sign-On using HTTP Artifact
     * binding     
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact()
    throws Exception {
        entering("testIDPSSOHTTPArtifact", null);
        try {
            log(Level.FINEST, "testIDPSSOHTTPArtifact", 
                    "Running: testIDPSSOHTTPArtifact");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";            
            Reporter.log("Test Description: This testcase tests the " +
                    "attribute " + testAttribute +  " in Identity Provider" +
                    " initiated Single Sign-On using HTTP Artifact binding");  
            log(Level.FINEST, "testIDPSSOHTTPArtifact", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact", "Attribute: " + 
                    testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);                        
            log(Level.FINEST, "testIDPSSOHTTPArtifact", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact", "Page: " + 
                    page1.getWebResponse().getContentAsString());
            
            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();             
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPArtifact",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }     
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact");        
    }
   
    /**
     * Run fedlet(SP) initiated Single-Sign-On using HTTP Post binding      
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testSPSSOHTTPPost()
    throws Exception {
        entering("testSPSSOHTTPPost", null);
        try {
            log(Level.FINEST, "testSPSSOHTTPPost", 
                    "Running: testSPSSOHTTPPost");
            String str = "Run Fedlet (SP) initiated Single Sign-On using" +
                    " HTTP POST binding";            
            Reporter.log("Test Description: This testcase tests the " +
                    "attribute " + testAttribute +  " in Fedlet (SP) " +
                    "initiated Single Sign-On test using HTTP POST binding");  
            log(Level.FINEST, "testSPSSOHTTPPost", str);
            log(Level.FINEST, "testSPSSOHTTPPost", "Attribute: " + 
                    testAttribute);
            getWebClient();
            xmlfile = baseDir + "testspssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);                        
            log(Level.FINEST, "testSPSSOHTTPPost", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testSPSSOHTTPPost", "Page: " + 
                    page1.getWebResponse().getContentAsString());
            
            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();             
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testSPSSOHTTPPost",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }     
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSSOHTTPPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSSOHTTPPost");
    }
    
    /**
     * Run IDP initiated Single-Sign-On using HTTP Post binding     
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost()
    throws Exception {
        entering("testIDPSSOHTTPPost", null);
        try {
            log(Level.FINEST, "testIDPSSOHTTPPost", 
                    "Running: testIDPSSOHTTPPost");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";            
            Reporter.log("Test Description: This testcase tests the " +
                    "attribute " + testAttribute +  " in Identity Provider" +
                    " initiated Single Sign-On using HTTP POST binding");  
            log(Level.FINEST, "testIDPSSOHTTPPost", str);
            log(Level.FINEST, "testIDPSSOHTTPPost", "Attribute: " + 
                    testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);                        
            log(Level.FINEST, "testIDPSSOHTTPPost", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost", "Page: " + 
                    page1.getWebResponse().getContentAsString());
            
            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();             
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPPost",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }     
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost");        
    }    
   
    /**
     * Cleanup method deletes all the users which were created in setup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        try {
            getWebClient();
            
            // delete idp users
            fedletidpurl = configMap.get(TestConstants.KEY_AMC_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_AMC_HOST) + ":"
                    + configMap.get(TestConstants.KEY_AMC_PORT)
                    + configMap.get(TestConstants.KEY_AMC_URI);
            consoleLogin(webClient, fedletidpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD));
            
            fmfedletIDP = new FederationManager(fedletidpurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            log(Level.FINEST, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            if (FederationManager.getExitCode(fmfedletIDP.deleteIdentities(
                    webClient, configMap.get(
                    TestConstants.KEY_ATT_EXECUTION_REALM),
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities " +
                        "famadm command failed");
                assert false;
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }       

}
