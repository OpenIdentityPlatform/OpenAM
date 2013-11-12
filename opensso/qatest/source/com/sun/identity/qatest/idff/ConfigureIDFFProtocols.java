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
 * $Id: ConfigureIDFFProtocols.java,v 1.7 2009/01/27 00:04:01 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idff;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;

/**
 * This class configures SP & IDP deployed war's if it hasn't done so.
 * Also it creates COT on both instances, loads IDFF meta on both side with
 * one as SP & one as IDP.
 */
public class ConfigureIDFFProtocols extends IDFFCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private String  baseDir;
    private String spurl;
    private String idpurl;
    private String spmetadata;
    private String spmetadataext;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    
    /** Creates a new instance of ConfigureIDFF */
    public ConfigureIDFFProtocols() {
        super("ConfigureIDFFProtocols");
    }
    
    /**
     * Create the webClient which should be run before each test.
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
     * Configure sp & idp
     * @DocTest: IDFF|Configure SP & IDP by loading metadata on both sides.
     */
    @Parameters({"ssoprofile", "sloprofile", "terminationprofile", 
    "registrationprofile"})
    @BeforeTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void ConfigureIDFFProtocol(String strSSOProfile,
            String strSLOProfile, String strTermProfile, String strRegProfile)
    throws Exception {
        Object[] params = {strSSOProfile, strSLOProfile, 
            strTermProfile, strRegProfile};
        entering("ConfigureIDFF", params);
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestData"));
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
            
            //If any of the profile is diff than the default profile, 
            //then only delete & import the metadata. Else leave it as it is. 
            if (strSSOProfile.equals("post") || strSLOProfile.equals("soap") ||
                    strTermProfile.equals("soap") || 
                    strRegProfile.equals("soap")) {
                consoleLogin(webClient, spurl + "/UI/Login",
                        configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                        configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));

                fmSP = new FederationManager(spurl);
                consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                        TestConstants.KEY_IDP_AMADMIN_USER),
                        configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
                fmIDP = new FederationManager(idpurl);
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
                spmetadata =
                        MultiProtocolCommon.getMetadataFromPage(spmetaPage);

                //if profile is set to post, change the metadata & run the 
                //tests.
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
                    log(Level.FINEST, "setup", "Change Termination Profile" +
                            " to soap");
                    spmetadatamod = setSPTermProfile(spmetadatamod, "soap");
                }
                if (strRegProfile.equals("soap")) {
                    log(Level.FINEST, "setup", "Change Registration Profile" +
                            " to soap");
                    spmetadatamod = setSPRegProfile(spmetadatamod, "soap");
                }
                
                log(Level.FINEST, "setup", "Modified SP Metadata is: " + 
                        spmetadatamod);
                log(Level.FINEST, "setup", "Modified SP Extended Metadata" +
                        " is: " + spmetadataextmod);
                
                //Remove & Import Entity with modified metadata. 
                log(Level.FINE, "setup", "Since SP metadata have changed, " +
                        "delete SP entity & Import it again. "); 
                assert (loadSPMetadata(spmetadatamod, spmetadataextmod, fmSP,
                        fmIDP, configMap, webClient, false));
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            log(Level.FINEST, "cleanup", "Logging out of SP & IDP");
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }        
        exiting("ConfigureIDFF");
    }
    
    /**
     * Configure sp & idp by loading default SP metadata on both sides
     * @DocTest: IDFF|Unconfigure SP & IDP by loading default SP metadata on 
     * both sides.
     */
    @Parameters({"ssoprofile", "sloprofile", "terminationprofile", 
    "registrationprofile"})
    @AfterTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureIDFFProtocol(String strSSOProfile,
            String strSLOProfile, String strTermProfile, String strRegProfile)
    throws Exception {
        Object[] params = {strSSOProfile, strSLOProfile, 
            strTermProfile, strRegProfile};
        entering("UnconfigureIDFF", params);
        try {
            getWebClient();
            
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));

            //If any of the profile is diff than the default profile, 
            //then only delete & import the metadata. Else leave it as it is. 
            if (strSSOProfile.equals("post") || strSLOProfile.equals("soap") ||
                    strTermProfile.equals("soap") ||
                    strRegProfile.equals("soap")) {
                //Remove & Import Entity with modified metadata. 
                log(Level.FINE, "setup", "Since SP metadata have changed, " +
                        "delete SP entity & Import it again. "); 
                assert (loadSPMetadata(spmetadata, spmetadataext, fmSP, fmIDP, 
                        configMap, webClient, false));
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            log(Level.FINEST, "cleanup", "Logging out of SP & IDP");
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }        
        exiting("ConfigureIDFF");
    }    
}
