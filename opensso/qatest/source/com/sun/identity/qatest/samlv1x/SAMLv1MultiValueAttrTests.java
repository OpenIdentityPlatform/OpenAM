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
 * $Id: SAMLv1MultiValueAttrTests.java,v 1.1 2009/02/05 01:48:06 vimal_67 Exp $
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
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.SAMLv1Common;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
public class SAMLv1MultiValueAttrTests extends TestCommon {

    private ArrayList spattrmultiVal;
    private ArrayList idpattrmultiVal;
    private ArrayList spfroles;
    private ArrayList idpfroles;
    private ArrayList sproles;
    private ArrayList idproles;
    private Boolean spboolfr;
    private Boolean idpboolfr;    
    private Boolean spboolr;
    private Boolean idpboolr;
    private Boolean spexistbool;
    private Boolean idpexistbool;
    private DefaultTaskHandler task1;        
    private Map<String, String> configMap;
    private ResourceBundle rbsa;
    private Set sesQrySet;
    private String baseDir;
    private String xmlfile;            
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
    private String attrmap;
    private String description;
    private String expResult;    
    private String saProp = "SAMLv1MultiValueAttrTests";             
    SSOToken token;
    
    /**
     * Constructor SAMLv1MultiValueAttrTests
     */
    public SAMLv1MultiValueAttrTests() {
        super("SAMLv1MultiValueAttrTests");
    }

    /**
     * Configures the SP and IDP load meta for the SAMLv1MultiValueAttrTests 
     * tests to execute. Here ptestName is the testname and ptestIndex is the 
     * test index
     */
    @Parameters({"ptestName", "ptestIndex"})    
    @BeforeClass(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void setup(String ptestName, String ptestIndex)
            throws Exception {
        ArrayList list;
        try {
            testName = ptestName;
            testIndex = ptestIndex;           
            sesQrySet = new HashSet();    
            spfroles = new ArrayList();
            idpfroles = new ArrayList();
            spboolfr = false;
            idpboolfr = false;
            sproles = new ArrayList();
            idproles = new ArrayList();
            spboolr = false;
            idpboolr = false;
            spexistbool = false;
            idpexistbool = false;
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
                    + "SAMLv1MultiValueAttrTests", configMap);
            rbsa = ResourceBundle.getBundle("samlv1x" + fileseparator +
                    "SAMLv1MultiValueAttrTests");
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);            
            testType = rbsa.getString(saProp + testIndex + "." +
                        "type");
            testInit = rbsa.getString(saProp + testIndex + "." +
                        "initiated");
            attrmap = rbsa.getString(saProp + testIndex + "." +
                        "attrmap");
            description = rbsa.getString(saProp + testIndex + "." +
                        "description");
            expResult = rbsa.getString(saProp + testIndex + "." +
                        "expectedresult");
            getWebClient();  
            // For nsrole: 
            // if the exist is Yes, then the nsrole attribute of a spuser or
            // idpuser is checked after the filteredroles or roles are created
            // if the exist is No, then the nsrole attribute of a spuser or 
            // idpuser is checked after the filteredroles or roles are deleted
          
            // Create sp user
            consoleLogin(spwebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            spattrmultiVal = new ArrayList();
            
            // retrieving properties             
            String spuserAttr = rbsa.getString(saProp + testIndex + "." +
                    "spuser" + "." + "attributes");
            String spuserMultiAttr = rbsa.getString(saProp + testIndex + "." +
                    "spuser" + "." + "multivalueattributes");  
            String spexist = "";              
            list = new ArrayList();    
            list = (ArrayList) parseStringToList(spuserAttr, ",", "&");            
         
            // adding listMulti to list and setting session Query parameters
            List listMulti = new ArrayList();
            listMulti = (ArrayList) parseStringToList(spuserMultiAttr,
                    ",", "&");            
            Iterator iterMulti = listMulti.iterator();
            while(iterMulti.hasNext()) {
                String str = (String) iterMulti.next();                              
                int index = str.indexOf("=");                
                if (index != -1) {      
                    String attrName = str.substring(0, index).trim();                                                            
                    // session query parameter set
                    sesQrySet.add(attrName);                                                  
                }                
                list.add(str);
            }  
            
            // grabbing sp attribute multivalues list
            Iterator iter = list.iterator();
            while(iter.hasNext()) {
                String str = (String) iter.next();             
                int index = str.indexOf("=");
                if (index != -1) {
                    String attrName = str.substring(0, index).trim();                    
                    String strVal = str.substring(index + 1).trim();
                    // checking nsrole attribute
                    if (attrName.equalsIgnoreCase("nsrole")) {
                        String nsroletype = rbsa.getString(saProp + testIndex +
                                "." + "spuser" + "." + "nsroletype"); 
                        spexist = rbsa.getString(saProp + testIndex + "." +
                                "spuser" + "." + "exist"); 
                        if (nsroletype.equalsIgnoreCase("filteredrole")) {
                            int id = strVal.indexOf("=");
                            if (id != -1) {
                                String frVal = strVal.substring(id + 1).trim();
                                log(Level.FINEST, "setup", "sp filtered" +
                                        "roles: " + frVal);
                                spfroles.add(frVal);                         
                            }                
                            spboolfr = true;
                        } else {
                            int id = strVal.indexOf("=");
                            if (id != -1) {
                                String rVal = strVal.substring(id + 1).trim();
                                log(Level.FINEST, "setup", "sp roles:" + rVal);
                                sproles.add(rVal);                         
                            }                
                            spboolr = true;
                        }
                    }           
                    // adding multivalues to list
                    Iterator iterSet = sesQrySet.iterator();
                    while (iterSet.hasNext()) {
                        String strSet = (String) iterSet.next();                    
                        if (strSet.equalsIgnoreCase(attrName)) {
                            spattrmultiVal.add(strVal);
                        }
                    }  
                }                
            } 
            
            // create filteredroles if nsrole attribute is found and nsroletype 
            // is filteredrole
            if (spboolfr) {
                Iterator iterfr = spfroles.iterator();
                while (iterfr.hasNext()) {
                    String strfr = (String) iterfr.next();
                    List listfr = new ArrayList(); 
                    listfr.add("cn=" + strfr);
                    listfr.add("nsrolefilter=(objectclass=inetorgperson)");
                    listfr.add("userpassword=secret12");
                    if (FederationManager.getExitCode(fmSP.createIdentity(
                            spwebClient, configMap.get(
                            TestConstants.KEY_SP_EXECUTION_REALM), 
                            strfr, "filteredrole", listfr)) != 0) {
                        log(Level.SEVERE, "setup", "createIdentity famadm " +
                                "command failed");
                        assert false;
                    }     
                }
            }            
            if (FederationManager.getExitCode(fmSP.createIdentity(spwebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                assert false;
            }
            
            // create roles if nsrole attribute is found and nsroletype is role
            // and assign the role to user
            if (spboolr) {
                Iterator iterr = sproles.iterator();
                while (iterr.hasNext()) {
                    String strr = (String) iterr.next();
                    List listr = new ArrayList(); 
                    listr.add("cn=" + strr);
                    listr.add("userpassword=" + "secret12");  
                    if (FederationManager.getExitCode(fmSP.createIdentity(
                            spwebClient, configMap.get(
                            TestConstants.KEY_SP_EXECUTION_REALM), 
                            strr, "role", listr)) != 0) {
                        log(Level.SEVERE, "setup", "createIdentity famadm " +
                                "command failed");
                        assert false;
                    }
                    fmSP.addMember(spwebClient, configMap.get(TestConstants.
                            KEY_SP_EXECUTION_REALM), configMap.get(
                            TestConstants.KEY_SP_USER), "user", strr, "role");
                }                
            }
            
            // delete filteredroles or roles if exist attribute value is No
            if (spexist.equalsIgnoreCase("no")) {
                // delete filteredroles if nsrole attribute is found and 
                // nsroletype is filteredrole
                if (spboolfr) {
                    Iterator iterfr = spfroles.iterator();
                    while (iterfr.hasNext()) {
                        String strfr = (String) iterfr.next();
                        List listfr = new ArrayList(); 
                        listfr.add(strfr);                    
                        if (FederationManager.getExitCode(fmSP.deleteIdentities(
                                spwebClient, configMap.get(TestConstants.
                                KEY_SP_EXECUTION_REALM), listfr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                // delete roles if nsrole attribute is found and 
                // nsroletype is role
                if (spboolr) {
                    Iterator iterr = sproles.iterator();
                    while (iterr.hasNext()) {
                        String strr = (String) iterr.next();
                        List listr = new ArrayList(); 
                        listr.add(strr);                    
                        if (FederationManager.getExitCode(fmSP.deleteIdentities(
                                spwebClient, configMap.get(TestConstants.
                                KEY_SP_EXECUTION_REALM), listr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                spexistbool = true;
            }
                         
            // Create idp user          
            consoleLogin(idpwebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            idpattrmultiVal = new ArrayList();  
            
            // retrieving properties                         
            String idpuserAttr = rbsa.getString(saProp + testIndex + "." +
                    "idpuser" + "." + "attributes");
            String idpuserMultiAttr = rbsa.getString(saProp + testIndex + "." +
                    "idpuser" + "." + "multivalueattributes");
            String idpexist = ""; 
            list.clear();           
            list = (ArrayList) parseStringToList(idpuserAttr, ",", "&"); 
            
            // adding listMulti to list and setting session Query parameters
            listMulti.clear();
            listMulti = (ArrayList) parseStringToList(idpuserMultiAttr,
                    ",", "&");              
            iterMulti = listMulti.iterator();
            while(iterMulti.hasNext()) {
                String str = (String) iterMulti.next();
                int index = str.indexOf("=");                
                if (index != -1) {      
                    String attrName = str.substring(0, index).trim();                                                            
                    // session query parameter set
                    sesQrySet.add(attrName);                                                  
                }
                list.add(str);
            }  
             
            // grabbing idp attribute multivalues list
            iter = list.iterator();
            while(iter.hasNext()) {
                String str = (String) iter.next();             
                int index = str.indexOf("=");
                if (index != -1) {
                    String attrName = str.substring(0, index).trim();                    
                    String strVal = str.substring(index + 1).trim();   
                    // checking nsrole attribute
                    if (attrName.equalsIgnoreCase("nsrole")) {
                        String nsroletype = rbsa.getString(saProp + testIndex +
                                "." + "idpuser" + "." + "nsroletype");
                        idpexist = rbsa.getString(saProp + testIndex + "." +
                                "idpuser" + "." + "exist");
                        if (nsroletype.equalsIgnoreCase("filteredrole")) {
                            int id = strVal.indexOf("=");
                            if (id != -1) {
                                String frVal = strVal.substring(id + 1).trim();
                                log(Level.FINEST, "setup", "idp filtered" +
                                        "roles: " + frVal);
                                idpfroles.add(frVal);                                                     
                            }                                    
                            idpboolfr = true;
                        } else {
                            int id = strVal.indexOf("=");
                            if (id != -1) {
                                String rVal = strVal.substring(id + 1).trim();
                                log(Level.FINEST, "setup", "idp roles:" + rVal);
                                idproles.add(rVal);                         
                            }                
                            idpboolr = true;
                        }
                    }           
                    // adding multivalues to list
                    Iterator iterSet = sesQrySet.iterator();
                    while (iterSet.hasNext()) {
                        String strSet = (String) iterSet.next();                    
                        if (strSet.equalsIgnoreCase(attrName)) {
                            idpattrmultiVal.add(strVal);
                        }
                    }  
                }                
            }    
            
            // create filteredroles if nsrole attribute is found and nsroletype 
            // is filteredrole
            if (idpboolfr) {
                Iterator iterfr = idpfroles.iterator();
                while (iterfr.hasNext()) {
                    String strfr = (String) iterfr.next();
                    List listfr = new ArrayList(); 
                    listfr.add("cn=" + strfr);
                    listfr.add("nsrolefilter=(objectclass=inetorgperson)");
                    listfr.add("userpassword=secret12");
                    if (FederationManager.getExitCode(fmIDP.createIdentity(
                            idpwebClient, configMap.get(
                            TestConstants.KEY_IDP_EXECUTION_REALM), 
                            strfr, "filteredrole", listfr)) != 0) {
                        log(Level.SEVERE, "setup", "createIdentity famadm " +
                                "command failed");
                        assert false;
                    }                 
                }
            }            
            if (FederationManager.getExitCode(fmIDP.createIdentity(idpwebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed for IDP");
                assert false;
            }
            
            // create roles if nsrole attribute is found and nsroletype is role
            // and assign the role to user
            if (idpboolr) {
                Iterator iterr = idproles.iterator();
                while (iterr.hasNext()) {
                    String strr = (String) iterr.next();
                    List listr = new ArrayList(); 
                    listr.add("cn=" + strr);
                    listr.add("userpassword=" + "secret12");  
                    if (FederationManager.getExitCode(fmIDP.createIdentity(
                            idpwebClient, configMap.get(
                            TestConstants.KEY_IDP_EXECUTION_REALM), 
                            strr, "role", listr)) != 0) {
                        log(Level.SEVERE, "setup", "createIdentity famadm " +
                                "command" + " failed");
                        assert false;
                    }
                    fmIDP.addMember(idpwebClient, configMap.get(TestConstants.
                            KEY_IDP_EXECUTION_REALM), configMap.get(
                            TestConstants.KEY_IDP_USER), "user", strr, "role");
                }                
            }      
            
            // delete filteredroles or roles if exist attribute value is No
            if (idpexist.equalsIgnoreCase("no")) {
                // delete filteredroles if nsrole attribute is found 
                // and nsroletype is filteredrole
                if (idpboolfr) {
                    Iterator iterfr = idpfroles.iterator();
                    while (iterfr.hasNext()) {
                        String strfr = (String) iterfr.next();
                        List listfr = new ArrayList(); 
                        listfr.add(strfr);                    
                        if (FederationManager.getExitCode(fmIDP.deleteIdentities
                                (idpwebClient, configMap.get(TestConstants.
                                KEY_IDP_EXECUTION_REALM), listfr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                // delete roles if nsrole attribute is found and 
                // nsroletype is role
                if (idpboolr) {
                    Iterator iterr = idproles.iterator();
                    while (iterr.hasNext()) {
                        String strr = (String) iterr.next();
                        List listr = new ArrayList(); 
                        listr.add(strr);                    
                        if (FederationManager.getExitCode(fmIDP.deleteIdentities
                                (idpwebClient, configMap.get(TestConstants.
                                KEY_IDP_EXECUTION_REALM), listr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                idpexistbool = true;
            }            
            
            // adding saml attribute map
            list.clear();
            list = (ArrayList) parseStringToList(attrmap, ",", "&");                  
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
     * It tests the multivalue attributes of sp and idp users in the SAMLv1 
     * assertion. It executes the tests for Artifact and Post profiles.  
     */
    @Test(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void samlv1xMultiValAttrTest()
            throws Exception {
        try {            
            String attr = ""; 
            Reporter.log("Test Description:" + testName +
                    " makes sure the multivalue attributes are returned with " +
                    "all values passed in the SAML assertion. This is " +
                    description + " test");             
            
            // setting the session.jsp query parameter values in the form
            // of a single string where attrStr is a single query parameter
            // attr1|attr2|attr3..|attrn is the value of that parameter
            // Ex: /session.jsp?attrStr=mail|telephoneNumber|cn
            Iterator iterses = sesQrySet.iterator();            
            String sesQryStr = "";
            while (iterses.hasNext()) {
                String st = (String) iterses.next();
                log(Level.FINEST, "samlv1xMultiValAttrTest", "session query " +
                    "parameter: " + st);
                sesQryStr = sesQryStr + "|" + st;
            }
            int idx = sesQryStr.indexOf("|");            
            if (idx != -1) {
                sesQryStr = sesQryStr.substring(idx+1).trim();
                log(Level.FINEST, "samlv1xMultiValAttrTest", "session query " +
                    "string: " + sesQryStr);  
            }              
            
            attr = "/session.jsp?attrStr=" + sesQryStr;                         
                        
            xmlfile = baseDir + testName + "samlv1xMultiValAttrTest" + ".xml";
            configMap.put(TestConstants.KEY_SSO_RESULT, expResult);
            SAMLv1Common.getxmlSSO(xmlfile, configMap, testType,
                    testInit, attr);                      
            Thread.sleep(5000);
            log(Level.FINEST, "samlv1xMultiValAttrTest", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            wpage = task1.execute(webClient);
            log(Level.FINEST, "samlv1xMultiValAttrTest", "WebPage: " +
                    wpage.getWebResponse().getContentAsString());            
                      
            // checking multivalues attribute
            if (testInit.equalsIgnoreCase("idp")) {
                Iterator iter = idpattrmultiVal.iterator();
                while (iter.hasNext()) {
                    String multiVal = (String) iter.next();
                    if (!idpexistbool) {
                        if (!wpage.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                            log(Level.SEVERE, "samlv1xMultiValAttrTest", 
                                "Couldn't find attribute with multivalue " +
                                multiVal);
                            assert false;
                        }             
                    } else {
                        if (wpage.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                            log(Level.SEVERE, "samlv1xMultiValAttrTest", 
                                "Found attribute with multivalue " +
                                multiVal);
                            assert false;
                        }             
                    }                    
                }
            } else {
                Iterator iter = spattrmultiVal.iterator();
                while (iter.hasNext()) {
                    String multiVal = (String) iter.next();
                    if (!spexistbool) {
                        if (!wpage.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                            log(Level.SEVERE, "samlv1xMultiValAttrTest", 
                                "Couldn't find attribute with multivalue " +
                                multiVal);
                            assert false;
                        }             
                    } else {
                        if (wpage.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                            log(Level.SEVERE, "samlv1xMultiValAttrTest", 
                                "Found attribute with multivalue " +
                                multiVal);
                            assert false;
                        }             
                    }                    
                }                
            }            
                
        } catch (Exception e) {
            log(Level.SEVERE, "samlv1xMultiValAttrTest", e.getMessage());
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
    @BeforeClass(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
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
     * Deletes the users, roles and filteredroles created for this test, 
     * removes the attribute values added to the SAML service
     */
    @AfterClass(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        ArrayList list;
        try {     
            // deleting sp user
            consoleLogin(spwebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);      
            list = new ArrayList();
            list.add(configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(spwebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    list, "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command" +
                        " failed");
                assert false;
            }
            // If exist attribute value is Yes, then filtereroles or roles 
            // are deleted here
            if (!spexistbool) {
                // delete filteredroles if nsrole attribute is found 
                // and nsroletype is filteredrole
                if (spboolfr) {
                    Iterator iterfr = spfroles.iterator();
                    while (iterfr.hasNext()) {
                        String strfr = (String) iterfr.next();
                        List listfr = new ArrayList(); 
                        listfr.add(strfr);                    
                        if (FederationManager.getExitCode(fmSP.deleteIdentities(
                                spwebClient, configMap.get(TestConstants.
                                KEY_SP_EXECUTION_REALM), listfr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                // delete roles if nsrole attribute is found and 
                // nsroletype is role
                if (spboolr) {
                    Iterator iterr = sproles.iterator();
                    while (iterr.hasNext()) {
                        String strr = (String) iterr.next();
                        List listr = new ArrayList(); 
                        listr.add(strr);                    
                        if (FederationManager.getExitCode(fmSP.deleteIdentities(
                                spwebClient, configMap.get(TestConstants.
                                KEY_SP_EXECUTION_REALM), listr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
            }
            
            // deleting idp user
            consoleLogin(idpwebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add(configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(
                    idpwebClient, configMap.get(TestConstants.
                    KEY_IDP_EXECUTION_REALM), list, "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command " +
                        "failed");
                assert false;
            }
            // If exist attribute value is Yes, then filtereroles or roles 
            // are deleted here
            if (!idpexistbool) {
                // delete filteredroles if nsrole attribute is found
                // and nsroletype is filteredrole
                if (idpboolfr) {
                    Iterator iterfr = idpfroles.iterator();
                    while (iterfr.hasNext()) {
                        String strfr = (String) iterfr.next();
                        List listfr = new ArrayList(); 
                        listfr.add(strfr);                    
                        if (FederationManager.getExitCode(fmIDP.deleteIdentities
                                (idpwebClient, configMap.get(TestConstants.
                                KEY_IDP_EXECUTION_REALM), listfr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
                // delete roles if nsrole attribute is found 
                // and nsroletype is role
                if (idpboolr) {
                    Iterator iterr = idproles.iterator();
                    while (iterr.hasNext()) {
                        String strr = (String) iterr.next();
                        List listr = new ArrayList(); 
                        listr.add(strr);                    
                        if (FederationManager.getExitCode(fmIDP.deleteIdentities
                                (idpwebClient, configMap.get(TestConstants.
                                KEY_IDP_EXECUTION_REALM), listr, 
                                "filteredrole")) != 0) {
                            log(Level.SEVERE, "setup", "deleteIdentity " +
                                    "famadm command failed");
                            assert false;
                        }                    
                    }
                }
            }
            
            // removing SAML attribute map 
            list.clear();
            list = (ArrayList) parseStringToList(attrmap, ",", "&");                  
            fmSP.removeAttrDefs(spwebClient, "iPlanetAMSAMLService", 
                    "Global", list, null);
            fmIDP.removeAttrDefs(idpwebClient, "iPlanetAMSAMLService",
                    "Global", list, null);  
            
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
