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
 * $Id: ConfigureSAMLv2.java,v 1.20 2009/06/08 23:28:39 mrudulahg Exp $
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
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * This class configures SP & IDP deployed war's if it hasn't done so.
 * Also it creates COT on both instances, loads samlv2 meta on both side with
 * one as SP & one as IDP.
 */
public class ConfigureSAMLv2 extends TestCommon {
    private WebClient spWebClient;
    private WebClient idpWebClient;
    private Map<String, String> configMap;
    private Map<String, String> spConfigMap;
    private Map<String, String> idpConfigMap;
    public String groupName="";
    String spurl;
    String idpurl;
    
    /** Creates a new instance of ConfigureSAMLv2 */
    public ConfigureSAMLv2() {
        super("ConfigureSAMLv2");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient() throws Exception {
        try {
            spWebClient = new WebClient(BrowserVersion.FIREFOX_3);
            idpWebClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch(Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Configure sp & idp
     * @DocTest: SAML2|Configure SP & IDP by loading metadata on both sides.
     */
    @Parameters({"groupName"})
    @BeforeSuite(groups = {"s1ds", "ldapv3", "ad", "jdbc", "amsdk", "s1ds_sec",
    "ldapv3_sec", "ad_sec", "jdbc_sec", "amsdk_sec"})
    public void configureSAMLv2(String strGroupName)
    throws Exception {
        Object[] params = {strGroupName};
        entering("configureSAMLv2", params);
        try {
            URL url;
            HtmlPage page;
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            spConfigMap = new HashMap<String, String>();
            idpConfigMap = new HashMap<String, String>();
            getWebClient();
            
            log(Level.FINEST, "configureSAMLv2", "GroupName received from " +
                    "testng is " + strGroupName);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" +
                    fileseparator + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" +
                    fileseparator + "samlv2TestData", configMap);
            log(Level.FINEST, "configureSAMLv2", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        } catch(Exception e) {
            log(Level.SEVERE, "ConfigureSAMLv2", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try {
            //Instantiate AuthenticationCommon class to 
            //create AuthenticationConfig-Generated.properties
            AuthenticationCommon authComm = new AuthenticationCommon("samlv2");
            authComm.createAuthInstancesMap();

            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            //on sp side create cot, load sp metadata
            consoleLogin(spWebClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            IDMCommon idmC = new IDMCommon();
            
            //If execution_realm is different than root realm (/)
            //then create the realm
            idmC.createSubRealms(spWebClient, spfm, configMap.get(
                    TestConstants.KEY_SP_EXECUTION_REALM));
            
            HtmlPage spcotPage = spfm.listCots(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "listCots famadm command" +
                        " failed");
                assert false;
            }
            if (!spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.createCot(spWebClient,
                        configMap.get(TestConstants.KEY_SP_COT),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "configureSAMLv2", "Couldn't create " +
                            "COT at SP side");
                    log(Level.SEVERE, "configureSAMLv2", "createCot famadm" +
                            " command failed");
                    assert false;
                }
            } else {
                log(Level.FINEST, "configureSAMLv2", "COT exists at SP side");
            }
            
            String spMetadata[] = {"", ""};
            HtmlPage spEntityPage = spfm.listEntities(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "saml2");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "listEntities famadm" +
                        " command failed");
                assert false;
            }
            if (!spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "configureSAMLv2", "sp entity doesnt exist." +
                        " Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    spMetadata = MultiProtocolCommon.importMetadata(spWebClient,
                            configMap, true, "SP");
                } else {
                    spMetadata = MultiProtocolCommon.importMetadata(spWebClient,
                            configMap, false, "SP");
                }
                if ((spMetadata[0].equals(null)) ||
                        (spMetadata[1].equals(null))) {
                    log(Level.SEVERE, "configureSAMLv2", "Couldn't configure " +
                            "SP");
                    assert false;
                }
            } 
            // Export to get the metadata.
            HtmlPage spExportEntityPage;
            if (strGroupName.contains("sec")) {
                spExportEntityPage = spfm.exportEntity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    true, true, true, "saml2");
            } else {
                spExportEntityPage = spfm.exportEntity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    false, true, true, "saml2");
            }                
            if (FederationManager.getExitCode(spExportEntityPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "exportEntity famadm" +
                        " command failed");
                assert false;
            }
            spMetadata[0] = SAMLv2Common.getMetadataFromPage(
                    spExportEntityPage);
            spMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                    spExportEntityPage);            
            spMetadata[1] = spMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_SP_COT), "");
            log(Level.FINEST, "configureSAMLv2", "sp metadata" + spMetadata[0]);
            log(Level.FINEST, "configureSAMLv2", "sp Ext metadata" +
                    spMetadata[1]);
            
            //idp side create cot, load idp metadata
            consoleLogin(idpWebClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            //If execution_realm is different than root realm (/)
            //then create the realm
            idmC.createSubRealms(idpWebClient, idpfm, configMap.get(
                    TestConstants.KEY_IDP_EXECUTION_REALM));
            
            HtmlPage idpcotPage = idpfm.listCots(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "listCots famadm command" +
                        " failed");
                assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "configureSAMLv2", "COT exists at IDP side");
            } else {
                if (FederationManager.getExitCode(idpfm.createCot(idpWebClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "configureSAMLv2", "Couldn't create " +
                            "COT at IDP side");
                    log(Level.SEVERE, "configureSAMLv2", "createCot famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            String[] idpMetadata = {"",""};
            HtmlPage idpEntityPage = idpfm.listEntities(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "listEntities famadm" +
                        " command failed");
                assert false;
            }
            if (!idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME))) {
                log(Level.FINEST, "configureSAMLv2", "idp entity doesnt" +
                        " exist. Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    idpMetadata = MultiProtocolCommon.importMetadata(
                            idpWebClient,  configMap, true, "IDP");
                } else {
                    idpMetadata = MultiProtocolCommon.importMetadata(
                            idpWebClient, configMap, false, "IDP");
                }
                
                log(Level.FINEST, "configureSAMLv2", "idp metadata" +
                        idpMetadata[0]);
                log(Level.FINEST, "configureSAMLv2", "idp Ext metadata" +
                        idpMetadata[1]);
                if ((idpMetadata[0].equals(null)) || (
                        idpMetadata[1].equals(null))) {
                    log(Level.SEVERE, "configureSAMLv2", "Couldn't configure " +
                            "IDP");
                    assert false;
                }
            } 
            // Export to get the metadata.
            HtmlPage idpExportEntityPage;
            if (strGroupName.contains("sec")) {
                idpExportEntityPage = idpfm.exportEntity(idpWebClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        true, true, true, "saml2");
            } else {
                idpExportEntityPage = idpfm.exportEntity(idpWebClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        false, true, true, "saml2");
            }               
                
            if (FederationManager.getExitCode(idpExportEntityPage) != 0) {
                log(Level.SEVERE, "configureSAMLv2", "exportEntity famadm" +
                        " command failed");
                assert false;
            }
            idpMetadata[0] = SAMLv2Common.getMetadataFromPage(
                    idpExportEntityPage);
            idpMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                    idpExportEntityPage);
            
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_IDP_COT), "");
            log(Level.FINEST, "configureSAMLv2", "idp metadata" +
                    idpMetadata[0]);
            log(Level.FINEST, "configureSAMLv2", "idp Ext metadata" +
                    idpMetadata[1]);
            
            //load spmetadata on idp
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "configureSAMLv2", "sp entity exists at" +
                        " idp. Delete & load the metadata ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(
                        idpWebClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                        "saml2")) == 0) {
                    log(Level.FINEST, "configureSAMLv2", "Delete sp entity" +
                            " on IDP side");
                } else {
                    log(Level.SEVERE, "configureSAMLv2", "Couldnt delete sp " +
                            "entity on IDP side");
                    log(Level.SEVERE, "configureSAMLv2", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(idpfm.importEntity(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), spMetadata[0],
                    spMetadata[1],
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "saml2"))
                    != 0) {
                log(Level.SEVERE, "configureSAMLv2", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "configureSAMLv2", "importEntity famadm" +
                        " command failed");
                assert false;
            }
            //load idpmetadata on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME))) {
                log(Level.FINEST, "configureSAMLv2", "idp entity exists at" +
                        " sp. Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(spWebClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                        "saml2")) == 0) {
                    log(Level.FINEST, "configureSAMLv2", "Delete idp entity" +
                            " on SP side");
                } else {
                    log(Level.SEVERE, "configureSAMLv2", "Couldnt delete idp " +
                            "entity on SP side");
                    log(Level.SEVERE, "configureSAMLv2", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(spfm.importEntity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idpMetadata[0],
                    idpMetadata[1],
                    (String)configMap.get(TestConstants.KEY_SP_COT), "saml2"))
                    != 0) {
                log(Level.SEVERE, "configureSAMLv2", "Couldn't import IDP " +
                        "metadata on SP side");
                log(Level.SEVERE, "configureSAMLv2", "importEntity famadm" +
                        " command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "configureSAMLv2", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spWebClient, spurl);
            consoleLogout(idpWebClient, idpurl);
        }
        exiting("configureSAMLv2");
    }
}
