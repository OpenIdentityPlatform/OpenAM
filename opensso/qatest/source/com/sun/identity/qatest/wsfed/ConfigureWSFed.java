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
 * $Id: ConfigureWSFed.java,v 1.7 2009/01/27 00:18:21 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.wsfed;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.WSFedCommon;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * This class configures SP & IDP deployed war's if it hasn't done so.
 * Also it creates COT on both instances, loads WSFed meta on both side with
 * one as SP & one as IDP.
 */
public class ConfigureWSFed extends WSFedCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private Map<String, String> spConfigMap;
    private Map<String, String> idpConfigMap;
    private String spurl;
    private String idpurl;
    
    /** Creates a new instance of ConfigureWSFed */
    public ConfigureWSFed() {
        super("ConfigureWSFed");
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
     * @DocTest: WSFed|Configure SP & IDP by loading metadata on both sides.
     */
    @Parameters({"groupName"})
    @BeforeSuite(groups={"ldapv3_sec",  "s1ds_sec", "ad_sec",  "amsdk_sec"})
    public void ConfigureWSFed(String strGroupName)
    throws Exception {
        Object[] params = {strGroupName};
        entering("ConfigureWSFed", params);
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            spConfigMap = new HashMap<String, String>();
            idpConfigMap = new HashMap<String, String>();
            getWebClient();
            
            log(Level.FINEST, "ConfigureWSFed", "GroupName received from " +
                    "testng is " + strGroupName);
            configMap = getMapFromResourceBundle("wsfed" + fileseparator +
                    "WSFedTestConfigData");
            log(Level.FINEST, "ConfigureWSFed", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            spConfigMap = MultiProtocolCommon.getSPConfigurationMap(configMap);
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            //on sp side create cot, load sp metadata
            consoleLogin(webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            IDMCommon idmC = new IDMCommon();
            
            //If execution_realm is different than root realm (/) 
            //then create the realm
            idmC.createSubRealms(webClient, spfm, configMap.get(
                       TestConstants.KEY_SP_EXECUTION_REALM));
            
            HtmlPage spcotPage = spfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
               log(Level.SEVERE, "ConfigureWSFed", "listCots famadm command" +
                       " failed");
               assert false;
            }
            if (!spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.createCot(webClient,
                        configMap.get(TestConstants.KEY_SP_COT),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureWSFed", "Couldn't create " +
                            "COT at SP side");
                    log(Level.SEVERE, "ConfigureWSFed", "createCot famadm" +
                            " command failed");
                    assert false;
                }
            } else {
                log(Level.FINEST, "ConfigureWSFed", "COT exists at SP side");
            }
            
            String spMetadata[] = {"", ""};
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "wsfed");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureWSFed", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            if (!spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureWSFed", "sp entity doesnt exist. " +
                        "Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    spMetadata = MultiProtocolCommon.configureSP(webClient,
                            configMap, "wsfed", true);
                } else {
                    spMetadata = MultiProtocolCommon.configureSP(webClient,
                            configMap, "wsfed", false);
                }
                if ((spMetadata[0].equals(null)) || (spMetadata[1].
                        equals(null))) {
                    log(Level.SEVERE, "ConfigureWSFed", "Couldn't configure " +
                            "SP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage spExportEntityPage = spfm.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false, true,
                        true, "wsfed");
                if (FederationManager.getExitCode(spExportEntityPage) != 0) {
                   log(Level.SEVERE, "ConfigureWSFed", "exportEntity famadm" +
                           " command failed");
                   assert false;
                }
                spMetadata[0] = MultiProtocolCommon.getMetadataFromPage(
                        spExportEntityPage, "wsfed");
                spMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                        spExportEntityPage, "wsfed");
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_SP_COT), "");
            log(Level.FINEST, "ConfigureWSFed", "sp metadata" + spMetadata[0]);
            log(Level.FINEST, "ConfigureWSFed", "sp Ext metadata" + 
                    spMetadata[1]);
            
            //idp side create cot, load idp metadata
            consoleLogin(webClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            //If execution_realm is different than root realm (/) 
            //then create the realm
            idmC.createSubRealms(webClient, idpfm, configMap.get(
                       TestConstants.KEY_IDP_EXECUTION_REALM));
            
            HtmlPage idpcotPage = idpfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
               log(Level.SEVERE, "ConfigureWSFed", "listCots famadm" +
                       " command failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "ConfigureWSFed", "COT exists at IDP side");
            } else {
                if (FederationManager.getExitCode(idpfm.createCot(webClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureWSFed", "Couldn't create " +
                            "COT at IDP side");
                    log(Level.SEVERE, "ConfigureWSFed", "createCot famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            String[] idpMetadata = {"",""};
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "wsfed");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureWSFed", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            if (!idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureWSFed", "idp entity doesnt exist." +
                        " Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    idpMetadata = MultiProtocolCommon.configureIDP(webClient,
                            configMap, "wsfed", true);
                } else {
                    idpMetadata = MultiProtocolCommon.configureIDP(webClient,
                            configMap, "wsfed", false);
                }
                
                log(Level.FINEST, "ConfigureWSFed", "idp metadata" +
                        idpMetadata[0]);
                log(Level.FINEST, "ConfigureWSFed", "idp Ext metadata" +
                        idpMetadata[1]);
                if ((idpMetadata[0].equals(null)) || (
                        idpMetadata[1].equals(null))) {
                    log(Level.SEVERE, "ConfigureWSFed", "Couldn't configure " +
                            "IDP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage idpExportEntityPage = idpfm.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false, true,
                        true, "wsfed");
                if (FederationManager.getExitCode(idpExportEntityPage) != 0) {
                   log(Level.SEVERE, "ConfigureWSFed", "exportEntity famadm" +
                           " command failed");
                   assert false;
                }
                idpMetadata[0] = MultiProtocolCommon.getMetadataFromPage(
                        idpExportEntityPage, "wsfed");
                idpMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                        idpExportEntityPage, "wsfed");
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_IDP_COT), "");
            log(Level.FINEST, "ConfigureWSFed", "idp metadata" +
                    idpMetadata[0]);
            log(Level.FINEST, "ConfigureWSFed", "idp Ext metadata" +
                    idpMetadata[1]);
            
            //load spmetadata on idp
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureWSFed", "sp entity exists at" +
                        " idp. Delete & load the metadata ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                        "wsfed")) == 0) {
                    log(Level.FINEST, "ConfigureWSFed", "Delete sp entity on " +
                            "IDP side");
                } else {
                    log(Level.SEVERE, "ConfigureWSFed", "Couldnt delete sp " +
                            "entity on IDP side");
                    log(Level.SEVERE, "ConfigureWSFed", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), spMetadata[0],
                    spMetadata[1],
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "wsfed"))
                    != 0) {
                log(Level.SEVERE, "ConfigureWSFed", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "ConfigureWSFed", "importEntity famadm" +
                        " command failed");
                assert false;
            }
            //load idpmetadata on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureWSFed", "idp entity exists at" +
                        " sp. Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                        "wsfed")) == 0) {
                    log(Level.FINEST, "ConfigureWSFed", "Delete idp entity" +
                            " on SP side");
                } else {
                    log(Level.FINEST, "ConfigureWSFed", "Couldnt delete idp " +
                            "entity on SP side");
                log(Level.SEVERE, "ConfigureWSFed", "deleteEntity famadm" +
                        " command failed");
                    assert false;
                }
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(spfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idpMetadata[0],
                    idpMetadata[1],
                    (String)configMap.get(TestConstants.KEY_SP_COT), "wsfed"))
                    != 0) {
                log(Level.SEVERE, "ConfigureWSFed", "Couldn't import IDP " +
                        "metadata on SP side");
                log(Level.SEVERE, "ConfigureWSFed", "importEntity famadm" +
                        " command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "ConfigureWSFed", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
        }
        exiting("ConfigureWSFed");
    }
}
