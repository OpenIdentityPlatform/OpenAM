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
 * $Id: IDFFSmokeTest.java,v 1.11 2009/01/27 00:04:02 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idff;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests IDFF Federation, SLO, SSO, Name registration & Termination 
 * 1. SP Initiated Federation
 * 2. SP Initiated SLO
 * 3. SP Initiated SSO 
 * 4. SP Initiated Name Registration
 * 5. SP Initiated Termination
 * 6. IDP Initiated SLO. As IDP init federation is not supported, 
 * SP init federation is performed first to follow IDP init SLO. 
 * 7. IDP Initiated Name registration. 
 * 8. IDP Initiated Termination
 */
public class IDFFSmokeTest extends IDFFCommon {
    
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private DefaultTaskHandler task;
    private HtmlPage page;
    private Map<String, String> configMap;
    private String  baseDir;
    private URL url;
    private String xmlfile;
    private String spurl;
    private String idpurl;
    private String spmetadata;
    private String spmetadataext;

    /** Creates a new instance of IDFFSmokeTest */
    public IDFFSmokeTest() {
        super("IDFFSmokeTest");
    }
    
    /**
     * Create the webClient 
     */
    private void getWebClient()
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
     * This is setup method. It creates required users for test
     */
    @Parameters({"ssoprofile", "sloprofile", "terminationprofile", 
    "registrationprofile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String strSSOProfile, String strSLOProfile, 
            String strTermProfile, String strRegProfile)
    throws Exception {
        Object[] params = {strSSOProfile, strSLOProfile, strTermProfile, 
                strRegProfile};
        entering("setup", params);
        Reporter.log("setup parameters: " + params);
        List<String> list;
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestData"));
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "IDFFSmokeTest"));
            log(Level.FINEST, "setup", "Map is " + configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            } catch (Exception e) {
                log(Level.SEVERE, "setup", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        try {
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            
            list = new ArrayList();
            list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            log(Level.FINE, "setup", "SP user created is " + list);
            
            // Create idp users
            list.clear();
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            log(Level.FINE, "setup", "IDP user created is " + list);
            
            //If any of the profile is diff than the default profile, 
            //then only delete & import the metadata. Else leave it as it is. 
            if (strSSOProfile.equals("post") || strSLOProfile.equals("soap") ||
                    strTermProfile.equals("soap") || 
                    strRegProfile.equals("soap")) {
                HtmlPage spmetaPage = fmSP.exportEntity(webClient,
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, true, true, "idff");
                if (FederationManager.getExitCode(spmetaPage) != 0) {
                    log(Level.SEVERE, "setup", "exportEntity famadm command" +
                            " failed");
                    assert false;
                }
                spmetadataext = MultiProtocolCommon.getExtMetadataFromPage(
                        spmetaPage);            
                spmetadata = MultiProtocolCommon.getMetadataFromPage
                        (spmetaPage);            

                //if profile is set to post, change the metadata & run the tests
                log(Level.FINE, "setup", "SSO Profile is set to " + 
                        strSSOProfile);
                log(Level.FINE, "setup", "SLO Profile is set to " + 
                        strSLOProfile);
                log(Level.FINE, "setup", "Termination Profile is set to " + 
                        strTermProfile);
                log(Level.FINE, "setup", "Registration Profile is set to " + 
                        strRegProfile);
                String spmetadataextmod = spmetadataext;
                String spmetadatamod = spmetadata;
                if (strSSOProfile.equals("post")) {
                    log(Level.FINEST, "setup", "Change SSO Profile to post");
                    spmetadataextmod = setSPSSOProfile(spmetadataext, "post");
                }
                if (strSLOProfile.equals("soap")) {
                    log(Level.FINEST, "setup", "Change SLO Profile to soap");
                    spmetadatamod = setSPSLOProfile(spmetadata, "soap");
                } 
                if (strTermProfile.equals("soap")) {
                    log(Level.FINEST, "setup", "Change Termination Profile " +
                            "to soap");
                    spmetadatamod = setSPTermProfile(spmetadatamod, "soap");
                }
                if (strRegProfile.equals("soap")) {
                    log(Level.FINEST, "setup", "Change Registration Profile " +
                            "to soap");
                    spmetadatamod = setSPRegProfile(spmetadatamod, "soap");
                }
                
                log(Level.FINEST, "setup", "Modified SP Metadata is: " + 
                        spmetadatamod);
                log(Level.FINEST, "setup", "Modified SP Extended Metadata " +
                        "is: " + spmetadataextmod);
                
                //Remove & Import Entity with modified metadata. 
                log(Level.FINE, "setup", "Since SP metadata have changed, " +
                        "delete SP entity & Import it again. "); 
                if (FederationManager.getExitCode(fmSP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "idff")) == 0) {
                    log(Level.FINE, "setup", "Deleted SP entity on SP side");
                } else {
                    log(Level.SEVERE, "setup", "Couldnt delete SP entity on " +
                            "SP side");
                    log(Level.SEVERE, "setup", "deleteEntity famadm command" +
                            " failed");
                    assert false;
                }  
                if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "idff")) == 0) {
                    log(Level.FINE, "setup", "Deleted SP entity on IDP side");
                } else {
                    log(Level.FINE, "setup", "Couldnt delete SP entity on " +
                            "IDP side");
                    log(Level.SEVERE, "setup", "deleteEntity famadm command" +
                            " failed");
                    assert false;
                }  

                Thread.sleep(9000);
                if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        spmetadatamod, spmetadataextmod, null, "idff")) != 0) {
                    log(Level.SEVERE, "setup", "Couldn't import SP " +
                            "metadata on SP side");
                    log(Level.SEVERE, "setup", "importEntoty famadm command" +
                            " failed");
                    assert false;
                } else {
                     log(Level.FINE, "setup", "Successfully imported " +
                             "modified SP entity on SP side");
                }
                spmetadataextmod = spmetadataextmod.replaceAll(
                        "hosted=\"true\"", "hosted=\"false\"");
                spmetadataextmod = spmetadataextmod.replaceAll(
                        "hosted=\"1\"", "hosted=\"0\"");
                spmetadataextmod = spmetadataextmod.replaceAll(
                        (String)configMap.get(TestConstants.KEY_SP_COT),
                        (String)configMap.get(TestConstants.KEY_IDP_COT));

                if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        spmetadatamod, spmetadataextmod, null, "idff")) != 0) {
                    log(Level.SEVERE, "setup", "Couldn't import SP " +
                            "metadata on IDP side");
                    assert false;
                } else {
                     log(Level.FINE, "setup", "Successfully imported " +
                             "modified SP entity on IDP side");
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * @DocTest: IDFF|Perform SP initiated federation.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSPInitFederation()
    throws Exception {
        entering("testSPInitFederation", null);
        try {
            log(Level.FINE, "testSPInitFederation", 
                    "Running: testSPInitFederation");
            getWebClient();
            log(Level.FINE, "testSPInitFederation", "Login to SP with " + 
                    configMap.get(TestConstants.KEY_SP_USER));
            consoleLogin(webClient, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_SP_USER),
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            xmlfile = baseDir + "testspinitfederation.xml";
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            log(Level.FINE, "testSPInitFederation", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPInitFederation", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPInitFederation");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated SLO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPInitFederation"})
    public void testSPInitSLO()
    throws Exception {
        entering("testSPInitSLO", null);
        try {
            log(Level.FINE, "testSPInitSLO", "Running: testSPInitSLO");
            xmlfile = baseDir + "testspinitslo.xml";
            getxmlSPIDFFLogout(xmlfile, configMap);
            log(Level.FINE, "testSPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPInitSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPInitSLO");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated SSO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPInitSLO"})
    public void testSPInitSSO()
    throws Exception {
        entering("testSPInitSSO", null);
        try {
            log(Level.FINE, "testSPInitSSO", "Running: testSPInitSSO");
            log(Level.FINE, "testSPInitSSO", "Login to IDP with " + 
                    TestConstants.KEY_IDP_USER);
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_USER),
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            xmlfile = baseDir + "testspinitsso.xml";
            getxmlSPIDFFSSO(xmlfile, configMap);
            log(Level.FINE, "testSPInitSSO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPInitSSO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPInitSSO");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated Name registration.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPInitSSO"})
    public void testSPInitNameReg()
    throws Exception {
        entering("testSPInitNameReg", null);
        try {
            log(Level.FINE, "testSPInitNameReg", "Running: testSPInitNameReg");
            xmlfile = baseDir + "testspinitnamereg.xml";
            getxmlSPIDFFNameReg(xmlfile, configMap);
            log(Level.FINE, "testSPInitNameReg", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPInitNameReg", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPInitNameReg");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated Termination.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPInitNameReg"})
    public void testSPInitTerminate()
    throws Exception {
        entering("testSPInitTerminate", null);
        try {
            log(Level.FINE, "testSPInitTerminate", 
                    "Running: testSPInitTerminate");
            xmlfile = baseDir + "testspinitterminate.xml";
            getxmlSPIDFFTerminate(xmlfile, configMap);
            log(Level.FINE, "testSPInitTerminate", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPInitTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("testSPInitTerminate");
    }

    /**
     * @DocTest: IDFF|Perform IDP initiated SLO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPInitTerminate"})
    public void testIDPInitSLO()
    throws Exception {
        entering("testIDPInitSLO", null);
        try {
            log(Level.FINE, "testIDPInitSLO", "Running: testIDPInitSLO");
            xmlfile = baseDir + "testspinitfederation.xml";
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_SP_USER),
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            log(Level.FINE, "testIDPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            xmlfile = baseDir + "testidpinitslo.xml";
            getxmlIDPIDFFLogout(xmlfile, configMap);
            log(Level.FINE, "testIDPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPInitSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPInitSLO");
    }
   
    /**
     * @DocTest: IDFF|Perform IDP initiated Name registration.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testIDPInitSLO"})
    public void testIDPInitNameReg()
    throws Exception {
        entering("testIDPInitNameReg", null);
        try {
            log(Level.FINE, "testIDPInitNameReg", "Running: " +
                    "testIDPInitNameReg");
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_USER),
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            xmlfile = baseDir + "testidpinitnamereg.xml";
            getxmlIDPIDFFNameReg(xmlfile, configMap);
            log(Level.FINE, "testIDPInitNameReg", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPInitNameReg", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPInitNameReg");
    }
   
    /**
     * @DocTest: IDFF|Perform IDP initiated Termination.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testIDPInitNameReg"})
    public void testIDPInitTerminate()
    throws Exception {
        entering("testIDPInitTerminate", null);
        try {
            log(Level.FINE, "testIDPInitTerminate", "Running: " +
                    "testIDPInitTerminate");
            xmlfile = baseDir + "testspinitterminate.xml";
            getxmlIDPIDFFTerminate(xmlfile, configMap);
            log(Level.FINE, "testIDPInitTerminate", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPInitTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("testIDPInitTerminate");
    }

    /**
     * This methods deletes all the users as part of cleanup
     */
    @Parameters({"ssoprofile", "sloprofile", "terminationprofile", 
    "registrationprofile"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String strSSOProfile, String strSLOProfile, 
            String strTermProfile, String strRegProfile)
    throws Exception {
        Object[] params = {strSSOProfile, strSLOProfile, strTermProfile, 
                strRegProfile};
        entering("cleanup", params);
        Reporter.log("Cleanup parameters: " + params);
        ArrayList idList;
        try {
            log(Level.FINE, "cleanup", "Entering Cleanup");
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_SP_USER));
            log(Level.FINE, "cleanup", "sp users to delete :" +
                    configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            
            // Create idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_IDP_USER));
            log(Level.FINE, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            
            //If any of the profile is diff than the default profile, 
            //then only delete & import the metadata. Else leave it as it is. 
            if (strSSOProfile.equals("post") || strSLOProfile.equals("soap") ||
                    strTermProfile.equals("soap") || 
                    strRegProfile.equals("soap")) {
                //Remove & Import Entity with modified metadata. 
                log(Level.FINE, "setup", "Since SP metadata have changed, " +
                        "delete SP entity & Import it again. "); 
                if (FederationManager.getExitCode(fmSP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "idff")) == 0) {
                    log(Level.FINE, "setup", "Deleted SP entity on SP side");
                } else {
                    log(Level.SEVERE, "setup", "Couldnt delete SP entity on " +
                            "SP side");
                    log(Level.SEVERE, "setup", "deleteEntity famadm command" +
                            " failed");
                    assert false;
                }  
                if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "idff")) == 0) {
                    log(Level.FINE, "setup", "Deleted SP entity on IDP side");
                } else {
                    log(Level.SEVERE, "setup", "Couldnt delete SP entity on " +
                            "IDP side");
                    log(Level.SEVERE, "setup", "deleteEntity famadm command" +
                            " failed");
                    assert false;
                }  

                Thread.sleep(9000);
                if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        spmetadata, spmetadataext, null, "idff")) != 0) {
                    log(Level.SEVERE, "setup", "Couldn't import SP " +
                            "metadata on SP side");
                    log(Level.SEVERE, "setup", "importEntity famadm command" +
                            " failed");
                    assert false;
                } else {
                     log(Level.FINE, "setup", "Successfully imported " +
                             "modified SP entity on SP side");
                }
                
                spmetadataext = spmetadataext.replaceAll(
                        "hosted=\"true\"", "hosted=\"false\"");
                spmetadataext = spmetadataext.replaceAll(
                        "hosted=\"1\"", "hosted=\"0\"");
                spmetadataext = spmetadataext.replaceAll(
                        (String)configMap.get(TestConstants.KEY_SP_COT),
                        (String)configMap.get(TestConstants.KEY_IDP_COT));
                if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        spmetadata, spmetadataext, null, "idff")) != 0) {
                    log(Level.SEVERE, "setup", "Couldn't import SP " +
                            "metadata on IDP side");
                    log(Level.SEVERE, "setup", "importEntity famadm command" +
                            " failed");
                    assert false;
                } else {
                     log(Level.FINE, "setup", "Successfully imported " +
                             "modified SP entity on IDP side");
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("cleanup");
    }
}
