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
 * $Id: UnconfigureSAMLv2IDPProxy.java,v 1.4 2009/01/27 00:15:33 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2idpproxy;

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
public class UnconfigureSAMLv2IDPProxy extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    
    /** Creates a new instance of UnconfigureSAMLv2IDPProxy */
    public UnconfigureSAMLv2IDPProxy() {
        super("UnconfigureSAMLv2IDPProxy");
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
     * Unconfigure sp & idp
     * @DocTest: SAML2|Unconfigure SP, IDP & IDP Proxy by deleting entities 
     * & COT's 
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureSAMLv2IDPProxy()
    throws Exception {
        entering("UnconfigureSAMLv2IDPProxy", null);
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            idpproxyurl = configMap.get(TestConstants.KEY_IDP_PROXY_PROTOCOL)
                    + "://" + configMap.get(TestConstants.KEY_IDP_PROXY_HOST) + 
                    ":" + configMap.get(TestConstants.KEY_IDP_PROXY_PORT)
                    + configMap.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
            
            consoleLogin(webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login",
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try{
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            FederationManager idpproxyfm = new FederationManager(idpproxyurl);
            
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "listEntities " +
                       "famadm command failed");
            }
            //Delete IDP & SP entities on IDP
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "idp entity " +
                        "exists at sp.");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted " +
                            "IDP  entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete IDP entity on IDP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }
            
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "IDP Proxy " +
                        "entity exists at idp. ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted " +
                            "IDP Proxy entity on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete IDP Proxy entity on IDP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }
            
            //Delete COT on IDP side.
            HtmlPage idpcotPage = idpfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "listCots " +
                       "famadm command failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "COT exists " +
                        "at IDP side");
                if(MultiProtocolCommon.COTcontainsEntities(idpfm, webClient, 
                        configMap.get(TestConstants.KEY_IDP_COT), configMap.get(
                        TestConstants.KEY_IDP_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "COT has " +
                            "entities at IDP side. COT won't be deleted");
                } else {
                    if (FederationManager.getExitCode(idpfm.deleteCot(webClient,
                            configMap.get(TestConstants.KEY_IDP_COT),
                            configMap.get(TestConstants.
                            KEY_IDP_EXECUTION_REALM))) 
                            != 0) {
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "Couldn't delete COT at IDP side");
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "deleteCot famadm  command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", 
                                "Deleted COT at IDP side");                    
                    }
                }
            }
            
            HtmlPage idpproxyEntityPage = idpproxyfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(idpproxyEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                       "listEntities famadm" +
                       " command failed for IDP Proxy");
            }
            //Delete IDP & SP entities on IDP Proxy
            if (idpproxyEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "idp entity " +
                        "exists at sp.");
                if (FederationManager.getExitCode(idpproxyfm.deleteEntity(
                        webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted " +
                            "IDP PROXY entity on IDP PROXY side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete IDP PROXY entity on IDP PROXY side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }
            
            if (idpproxyEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "sp entity " +
                        "exists at IDP PROXY. ");
                if (FederationManager.getExitCode(idpproxyfm.deleteEntity(
                        webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted SP" +
                            " entity on IDP PROXY side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete SP entity on IDP PROXY side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }

            if (idpproxyEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "sp entity " +
                        "exists at IDP PROXY. ");
                if (FederationManager.getExitCode(idpproxyfm.deleteEntity(
                        webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted IDP" +
                            " entity on IDP PROXY side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete IDP entity on IDP PROXY side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }

            //Delete COT on IDP PROXY side.
            HtmlPage idpproxycotPage = idpproxyfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpproxycotPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "listCots famadm" +
                       " command failed");
               assert false;
            }
            if (idpproxycotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_PROXY_COT))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "COT exists " +
                        "at IDP Proxy side");
                if(MultiProtocolCommon.COTcontainsEntities(idpproxyfm, 
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_PROXY_COT), configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "COT has " +
                            "entities at IDP Proxy side. COT won't be deleted");
                } else {
                    if (FederationManager.getExitCode(idpproxyfm.deleteCot(
                            webClient, configMap.get(TestConstants.
                            KEY_IDP_PROXY_COT), configMap.get(TestConstants.
                            KEY_IDP_EXECUTION_REALM))) != 0) {
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "Couldn't delete COT at IDP Proxy side");
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "deleteCot famadm command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", 
                                "Deleted COT at IDP Proxy side");                    
                    }
                }
            }
            
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "listEntities " +
                       "famadm command failed");
            }
            //Delete SP & IDP Proxy entities on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "sp entity " +
                        "exists at" +
                        " sp. ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted " +
                            "sp entity on SP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete idp entity on SP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME)))
            {
                log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "idp proxy " +
                        "entity exists at sp.");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "Deleted " +
                            "idp proxy entity on SP side");
                } else {
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete idp proxy entity on SP side");
                    log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                }
            }
            
            //Delete COT on sp side.
            HtmlPage spcotPage = spfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
               log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", "listCots " +
                       "famadm command failed");
            }
            if (spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if(MultiProtocolCommon.COTcontainsEntities(spfm, webClient, 
                        configMap.get(TestConstants.KEY_SP_COT), configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM))) {
                    log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", "COT has " +
                            "entities at SP side. COT wont be deleted");
                } else {
                    if (FederationManager.getExitCode(spfm.deleteCot(webClient,
                            configMap.get(TestConstants.KEY_SP_COT),
                            configMap.get(TestConstants.
                            KEY_SP_EXECUTION_REALM))) != 0) {
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "Couldn't delete COT at SP side");
                        log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", 
                                "deleteCot famadm command failed");
                    } else {
                        log(Level.FINEST, "UnconfigureSAMLv2IDPProxy", 
                                "Deleted COT at SP side");                    
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

            //If execution_realm is different than root realm (/) 
            //then create the realm at IDP side
            idmC.deleteSubRealms(webClient, idpproxyfm, configMap.get(
                    TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), configMap.get(
                    TestConstants.KEY_IDP_PROXY_SUBREALM_RECURSIVE_DELETE));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureSAMLv2IDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
            consoleLogout(webClient, idpproxyurl);
        }
        exiting("UnconfigureSAMLv2IDPProxy");
    }
}
