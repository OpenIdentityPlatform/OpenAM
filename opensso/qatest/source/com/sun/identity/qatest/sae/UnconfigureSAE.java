
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
 * $Id: UnconfigureSAE.java,v 1.6 2009/03/17 19:27:05 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.sae;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;

/**
 * This class removes the configuration on SP & IDP. 
 * It removes SP & IDP entities on both sides, and also removes the COT. 
 */
public class UnconfigureSAE extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private String spurl;
    private String idpurl;
    
    /** Creates a new instance of UnconfigureSAE */
    public UnconfigureSAE() {
        super ("UnconfigureSAE");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient() 
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log (Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Configure sp & idp
     * @DocTest: SAE|Unconfigure SP & IDP by deleting entities & COT's 
     */
    @AfterTest (groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureSAE()
    throws Exception {
        entering ("UnconfigureSAE", null);
        String spurl;
        String idpurl;
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("sae" + fileseparator +
                    "saeTestConfigData");
            log (Level.FINEST, "UnconfigureSAE", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            consoleLogin (webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin (webClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
        } catch (Exception e) {
            log (Level.SEVERE, "UnconfigureSAE", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try{
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get (TestConstants.KEY_IDP_REALM), "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAE", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            //Delete IDP & SP entities on IDP
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log (Level.FINEST, "UnconfigureSAE", 
                        "idp entity exists at sp.");
                if (FederationManager.getExitCode(idpfm.deleteEntity (webClient,
                        configMap.get (TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get (TestConstants.KEY_IDP_REALM), false,
                        "saml2")) == 0) {
                    log (Level.FINEST, "UnconfigureSAE", "Deleted IDP entity " +
                            "on IDP side");
                } else {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldnt delete sp " +
                            "entity on IDP side");
                    log (Level.SEVERE, "UnconfigureSAE", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log (Level.FINEST, "UnconfigureSAE",
                        "sp entity exists at idp. ");
                if (FederationManager.getExitCode(idpfm.deleteEntity (webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_REALM), false,
                        "saml2")) == 0) {
                    log (Level.FINEST, "UnconfigureSAE", "Deleted sp entity " +
                            "on IDP side");
                } else {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldnt delete sp " +
                            "entity on IDP side");
                    log (Level.SEVERE, "UnconfigureSAE", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            //Delete COT on IDP side.
            HtmlPage idpcotPage = idpfm.listCots(webClient,
                    configMap.get (TestConstants.KEY_IDP_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAE", "listCots famadm command" +
                       " failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains (configMap.get (TestConstants.KEY_IDP_COT))) {
                log (Level.FINEST, "UnconfigureSAE", "COT exists at IDP side");
                if (FederationManager.getExitCode(idpfm.deleteCot(webClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_REALM))) != 0) {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldn't delete " +
                            "COT at IDP side");
                    log(Level.SEVERE, "UnconfigureSAE", "deleteCot famadm" +
                            " command failed");
                    assert false;
                } else {
                    log (Level.FINEST, "UnconfigureSAE", "Deleted COT " +
                            "at IDP side");                    
                }
            }
            
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get (TestConstants.KEY_SP_REALM), "saml2");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAE", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            //Delete SP & IDP entities on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains (configMap.get (TestConstants.KEY_SP_ENTITY_NAME)))
            {
                log (Level.FINEST, "UnconfigureSAE", "sp entity exists at sp.");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get (TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get (TestConstants.KEY_SP_REALM), false,
                        "saml2")) == 0) {
                    log (Level.FINEST, "UnconfigureSAE",
                            "Deleted sp entity on " + "SP side");
                } else {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldnt delete idp " +
                            "entity on SP side");
                    log (Level.SEVERE, "UnconfigureSAE", "deleteEntity" +
                            " famadm command failed");
                    assert false;
                }
            }
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains (configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log (Level.FINEST, "UnconfigureSAE",
                        "idp entity exists at sp. ");
                if (FederationManager.getExitCode(spfm.deleteEntity (webClient,
                        configMap.get (TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get (TestConstants.KEY_SP_REALM), false,
                        "saml2")) == 0) {
                    log (Level.FINEST, "UnconfigureSAE",
                            "Deleted idp entity on " + "SP side");
                } else {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldnt delete idp " +
                            "entity on SP side");
                    log (Level.SEVERE, "UnconfigureSAE", "deleteEntity" +
                            " famadm command failed");
                    assert false;
                }
            }
            
            //Delete COT on sp side.
            HtmlPage spcotPage = spfm.listCots (webClient,
                    configMap.get (TestConstants.KEY_SP_REALM));
            if (spcotPage.getWebResponse().getContentAsString().
                    contains (configMap.get (TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.deleteCot(webClient,
                        configMap.get (TestConstants.KEY_SP_COT),
                        configMap.get (TestConstants.KEY_SP_REALM))) != 0) {
                    log (Level.SEVERE, "UnconfigureSAE", "Couldn't delete " +
                            "COT at SP side");
                    log (Level.SEVERE, "UnconfigureSAE", "deleteCot famadm" +
                            " command failed");
                    assert false;
                } else {
                    log (Level.FINEST, "UnconfigureSAE", "Deleted COT " +
                            "at SP side");                    
                }
            }
        } catch (Exception e) {
            log (Level.SEVERE, "UnconfigureSAE", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout (webClient, spurl);
            consoleLogout (webClient, idpurl);
        }
        exiting ("UnconfigureSAE");
    }
}
