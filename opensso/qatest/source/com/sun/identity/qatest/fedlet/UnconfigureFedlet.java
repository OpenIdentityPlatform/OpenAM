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
 * $Id: UnconfigureFedlet.java,v 1.1 2009/03/20 17:38:46 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.fedlet;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.FedletCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.mortbay.jetty.Server;

/**
 * This class removes the configuration on IDP side.
 * It removes IDP and Fedlet entities, and the COT's. 
 */
public class UnconfigureFedlet extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private Server server;    
    private String fedletidpurl;
            
    /** Creates a new instance of UnconfigureFedlet */
    public UnconfigureFedlet() {
        super("UnconfigureFedlet");
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
     * Unconfigure Fedlet
     * @DocTest: Fedlet|Unconfigure IDP by deleting entities & COT's
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureFedlet()
    throws Exception {
        entering("UnconfigureFedlet", null);
        try {
            server = new Server();
            log(Level.FINE, "stopServer", "Stopping jetty server");
            server.stop();
            log(Level.FINE, "stopServer", "Stopped jetty server");

            // Time delay required by the jetty server process to die
            Thread.sleep(30000);
            
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();            
            
            FedletCommon.getEntriesFromResourceBundle("fedlet" +
                    fileseparator + "FedletTests", configMap);
            FedletCommon.getEntriesFromResourceBundle("AMConfig", configMap);
            log(Level.FINEST, "UnconfigureFedlet", "Map:" + configMap);            
            
            fedletidpurl = configMap.get(TestConstants.KEY_AMC_PROTOCOL)
                    + "://" + configMap.get(TestConstants.KEY_AMC_HOST) + ":"
                    + configMap.get(TestConstants.KEY_AMC_PORT)
                    + configMap.get(TestConstants.KEY_AMC_URI);
            
            consoleLogin(webClient, fedletidpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureFedlet", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try{            
            FederationManager fedletidpfm = new FederationManager(fedletidpurl);
            
            HtmlPage idpEntityPage = fedletidpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
                log(Level.SEVERE, "UnconfigureFedlet", "listEntities famadm" +
                        " command failed");
                assert false;
            }
            //Delete IDP & Fedlet entities on IDP
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_FEDLETIDP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureFedlet", "idp entity exists at" +
                        " idp.");
                if (FederationManager.getExitCode(fedletidpfm.deleteEntity(
                        webClient, configMap.get(
                        TestConstants.KEY_FEDLETIDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureFedlet", "Deleted IDP" +
                            " entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureFedlet", "Couldnt delete" +
                            " IDP entity on IDP side");
                    log(Level.SEVERE, "UnconfigureFedlet", "deleteEntity" +
                            " famadm command failed");
                }
            }
            
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_FEDLET_NAME))) {
                log(Level.FINEST, "UnconfigureFedlet", "fedlet entity " +
                        "exists at idp. ");
                if (FederationManager.getExitCode(fedletidpfm.deleteEntity(
                        webClient, configMap.get(TestConstants.KEY_FEDLET_NAME),
                        configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureFedlet", "Deleted Fedlet" +
                            " entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureFedlet", "Couldnt delete" +
                            " Fedlet entity on IDP side");
                    log(Level.SEVERE, "UnconfigureFedlet", "deleteEntity" +
                            " famadm command failed");
                }
            }
            
            //Delete COT on IDP side.
            HtmlPage idpcotPage = fedletidpfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
                log(Level.SEVERE, "UnconfigureFedlet", "listCots famadm" +
                        " command failed");
                assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_FEDLET_COT))) {
                log(Level.FINEST, "UnconfigureFedlet", "COT exists at IDP" +
                        " side");
                if(MultiProtocolCommon.COTcontainsEntities(fedletidpfm,
                        webClient, configMap.get(TestConstants.KEY_FEDLET_COT),
                        configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureFedlet", "COT has entities" +
                            "at IDP side. COT won't be deleted");
                } else {
                    if (FederationManager.getExitCode(fedletidpfm.deleteCot(
                            webClient, configMap.get(
                            TestConstants.KEY_FEDLET_COT), configMap.get(
                            TestConstants.KEY_ATT_EXECUTION_REALM))) != 0) {
                        log(Level.SEVERE, "UnconfigureFedlet", "Couldn't " +
                                "delete COT at IDP side");
                        log(Level.SEVERE, "UnconfigureFedlet", "deleteCot " +
                                "famadm command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureFedlet", "Deleted COT " +
                                "at IDP side");
                    }
                }
            }                        
            
            // If execution_realm is different than root realm (/)
            // then delete the realm at IDP side
            IDMCommon idmC = new IDMCommon();
            idmC.deleteSubRealms(webClient, fedletidpfm,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    configMap.get(
                    TestConstants.KEY_ATT_SUBREALM_RECURSIVE_DELETE));
            
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureFedlet", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {           
            consoleLogout(webClient, fedletidpurl);
        }
        exiting("UnconfigureFedlet");
    }
}
