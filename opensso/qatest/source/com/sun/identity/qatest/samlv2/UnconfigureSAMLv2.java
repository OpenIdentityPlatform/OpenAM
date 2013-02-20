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
 * $Id: UnconfigureSAMLv2.java,v 1.14 2009/01/27 00:14:10 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;

/**
 * This class removes the configuration on SP & IDP
 * It removes SP & IDP entities on both sides, and also removes the COT.
 */
public class UnconfigureSAMLv2 extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private String spurl;
    private String idpurl;
    
    /** Creates a new instance of UnconfigureSAMLv2 */
    public UnconfigureSAMLv2() {
        super("UnconfigureSAMLv2");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient()
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch(Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Configure sp & idp
     * @DocTest: SAML2|Unconfigure SP & IDP by deleting entities & COT's
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureSAMLv2()
    throws Exception {
        entering("UnconfigureSAMLv2", null);
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            log(Level.FINEST, "UnconfigureSAMLv2", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                    configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + "://" +
                    configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            consoleLogin(webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureSAMLv2", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try{
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
                log(Level.SEVERE, "UnconfigureSAMLv2", "listEntities famadm" +
                        " command failed");
            }
            //Delete IDP & SP entities on IDP
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2", "idp entity exists at" +
                        " sp.");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "Deleted IDP" +
                            " entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2", "Couldnt delete" +
                            " IDP entity on IDP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2", "deleteEntity" +
                            " famadm command failed");
                }
            }
            
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2", "sp entity exists at" +
                        " idp. ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "Deleted SP" +
                            " entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2", "Couldnt delete" +
                            " SP entity on IDP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2", "deleteEntity" +
                            " famadm command failed");
                }
            }
            
            //Delete COT on IDP side.
            HtmlPage idpcotPage = idpfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
                log(Level.SEVERE, "UnconfigureSAMLv2", "listCots famadm" +
                        " command failed");
                assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "UnconfigureSAMLv2", "COT exists at IDP" +
                        " side");
                if(MultiProtocolCommon.COTcontainsEntities(idpfm, webClient, 
                        configMap.get(TestConstants.KEY_IDP_COT), configMap.get(
                        TestConstants.KEY_IDP_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "COT has entities" +
                            "at IDP side. COT won't be deleted");
                } else {
                    if (FederationManager.getExitCode(idpfm.deleteCot(webClient,
                            configMap.get(TestConstants.KEY_IDP_COT),
                            configMap.get(TestConstants.
                            KEY_IDP_EXECUTION_REALM))) != 0) {
                        log(Level.SEVERE, "UnconfigureSAMLv2", "Couldn't " +
                                "delete COT at IDP side");
                        log(Level.SEVERE, "UnconfigureSAMLv2", "deleteCot " +
                                "famadm command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureSAMLv2", "Deleted COT " +
                                "at IDP side");
                    }
                }
            }
            
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    "saml2");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
                log(Level.SEVERE, "UnconfigureSAMLv2", "listEntities famadm" +
                        " command failed");
            }
            //Delete SP & IDP entities on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2", "sp entity exists at" +
                        " sp. ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "Deleted sp entity" +
                            " on SP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2", "Couldnt delete" +
                            " idp entity on SP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2", "deleteEntity" +
                            " famadm command failed");
                }
            }
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2", "idp entity exists at" +
                        " sp.");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "Deleted idp" +
                            " entity on SP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2", "Couldnt delete" +
                            " idp entity on SP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2", "deleteEntity" +
                            " famadm command failed");
                }
            }
            
            //Delete COT on sp side.
            HtmlPage spcotPage = spfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
                log(Level.SEVERE, "UnconfigureSAMLv2", "listCots famadm" +
                        " command failed");
            }
            if (spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if(MultiProtocolCommon.COTcontainsEntities(spfm, webClient, 
                        configMap.get(TestConstants.KEY_SP_COT), configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureSAMLv2", "COT has entities" +
                            "at SP side. COT wont be deleted");
                } else {
                    if (FederationManager.getExitCode(spfm.deleteCot(webClient,
                            configMap.get(TestConstants.KEY_SP_COT),
                            configMap.get(TestConstants.KEY_SP_EXECUTION_REALM)
                            )) != 0) {
                        log(Level.SEVERE, "UnconfigureSAMLv2", "Couldn't " +
                                "delete COT at SP side");
                        log(Level.SEVERE, "UnconfigureSAMLv2", "deleteCot " +
                                "famadm command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureSAMLv2", "Deleted COT " +
                                "at SP side");
                    }
                } 
            }
            
            IDMCommon idmC = new IDMCommon();
            //If execution_realm is different than root realm (/)
            //then delete the realm at SP side
            idmC.deleteSubRealms(webClient, spfm, configMap.get(TestConstants.
                    KEY_SP_EXECUTION_REALM), configMap.get(TestConstants.
                    KEY_SP_SUBREALM_RECURSIVE_DELETE));
            
            //If execution_realm is different than root realm (/)
            //then create the realm at IDP side
            idmC.deleteSubRealms(webClient, idpfm, configMap.get(TestConstants.
                    KEY_IDP_EXECUTION_REALM), configMap.get(TestConstants.
                    KEY_IDP_SUBREALM_RECURSIVE_DELETE));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureSAMLv2", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
        }
        exiting("UnconfigureSAMLv2");
    }
}
