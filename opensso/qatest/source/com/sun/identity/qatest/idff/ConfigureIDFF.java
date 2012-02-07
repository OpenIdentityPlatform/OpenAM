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
 * $Id: ConfigureIDFF.java,v 1.10 2009/01/27 00:04:00 nithyas Exp $
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
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * This class configures SP & IDP deployed war's if it hasn't done so.
 * Also it creates COT on both instances, loads IDFF meta on both side with
 * one as SP & one as IDP.
 */
public class ConfigureIDFF extends TestCommon {
    private WebClient spWebClient;
    private WebClient idpWebClient;
    private Map<String, String> configMap;
    private Map<String, String> spConfigMap;
    private Map<String, String> idpConfigMap;
    
    /** Creates a new instance of ConfigureIDFF */
    public ConfigureIDFF() {
        super("ConfigureIDFF");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient()
    throws Exception {
        try {
            spWebClient = new WebClient(BrowserVersion.FIREFOX_3);
            idpWebClient = new WebClient(BrowserVersion.FIREFOX_3);
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
    @Parameters({"groupName"})
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void ConfigureIDFF(String strGroupName)
    throws Exception {
        Object[] params = {strGroupName};
        entering("ConfigureIDFF", params);
        String spurl = null;
        String idpurl = null;
        try {
            URL url;
            HtmlPage page;
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            spConfigMap = new HashMap<String, String>();
            idpConfigMap = new HashMap<String, String>();
            getWebClient();
            
            log(Level.FINEST, "ConfigureIDFF", "GroupName received from " +
                    "testng is " + strGroupName);
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            log(Level.FINEST, "ConfigureIDFF", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
                        
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            consoleLogin(spWebClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            IDMCommon idmC = new IDMCommon();
            
            //If execution_realm is different than root realm (/) 
            //then create the realm
            idmC.createSubRealms(spWebClient, spfm, configMap.get(
                       TestConstants.KEY_SP_EXECUTION_REALM));
            
            //on sp side create cot, load sp metadata
            HtmlPage spcotPage = spfm.listCots(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
                log(Level.SEVERE, "ConfigureIDFF", "listsCot famadm command" +
                        " failed");
                assert false;
            }
            if (!spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.createCot(spWebClient,
                        configMap.get(TestConstants.KEY_SP_COT),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureIDFF", "Couldn't create " +
                            "COT at SP side ");
                    log(Level.SEVERE, "ConfigureIDFF", "createCot famadm" +
                            " command failed");
                    assert false;
                }
            } else {
                log(Level.FINEST, "ConfigureIDFF", "COT exists at SP side");
            }
            
            String spMetadata[]= {"",""};
            HtmlPage spEntityPage = spfm.listEntities(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "idff");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
                log(Level.SEVERE, "ConfigureIDFF", "listsEntities famadm" +
                        " command failed");
                assert false;
            }
            if (!spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureIDFF", "sp entity doesnt exist. " +
                        "Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    log(Level.FINEST, "ConfigureIDFF", "Enable XML signing.");
                    List<String> arrList = new ArrayList();
                    arrList.add("XMLSigningOn=true");
                    if (FederationManager.getExitCode(spfm.setAttrDefs(
                            spWebClient, "sunFAMIDFFConfiguration", 
                            "Global", "", arrList)) != 0) {
                        log(Level.SEVERE, "ConfigureIDFF", "Couldn't set " +
                                "XMLSigningOn=true on SP side ");
                    } else {
                        log(Level.FINEST, "ConfigureIDFF", "Successfully set " +
                                "XMLSigningOn=true on SP side ");
                    }
                    spMetadata = MultiProtocolCommon.configureSP(spWebClient,
                            configMap, "idff", true);
                } else {
                    spMetadata = MultiProtocolCommon.configureSP(spWebClient,
                            configMap, "idff", false);
                }
                if ((spMetadata[0].equals(null)) || (spMetadata[1].
                        equals(null))) {
                    log(Level.SEVERE, "ConfigureIDFF", "Couldn't configure " +
                            "SP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage spExportEntityPage = spfm.exportEntity(spWebClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, true, true, "idff");
                if (FederationManager.getExitCode(spExportEntityPage) != 0) {
                    log(Level.SEVERE, "ConfigureIDFF", "exportEntity famadm" +
                            " command failed");
                    assert false;
                }
                spMetadata[0] = MultiProtocolCommon.getMetadataFromPage(
                        spExportEntityPage);
                spMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                        spExportEntityPage);
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_SP_COT), "");
            log(Level.FINEST, "ConfigureIDFF", "sp metadata" + spMetadata[0]);
            log(Level.FINEST, "ConfigureIDFF", "sp Ext metadata" +
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
               log(Level.SEVERE, "ConfigureIDFF", "listCots famadm command" +
                       " failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "ConfigureIDFF", "COT exists at IDP side");
            } else {
                if (FederationManager.getExitCode(idpfm.createCot(idpWebClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureIDFF", "Couldn't create " +
                            "COT at IDP side");
                    log(Level.SEVERE, "ConfigureIDFF", "createCot famadm" +
                            " commad failed");
                    assert false;
                }
            }
            
            String[] idpMetadata = {"",""};
            HtmlPage idpEntityPage = idpfm.listEntities(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    "idff");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureIDFF", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            if (!idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureIDFF", "idp entity doesnt exist. " +
                        "Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    log(Level.FINEST, "ConfigureIDFF", "Enable XML signing.");
                    List<String> arrList = new ArrayList();
                    arrList.add("XMLSigningOn=true");
                    if (FederationManager.getExitCode(idpfm.setAttrDefs(
                            idpWebClient, "sunFAMIDFFConfiguration", 
                            "Global", "", arrList)) !=0) {
                        log(Level.SEVERE, "ConfigureIDFF", "Couldn't set " +
                                "XMLSigningOn=true on IDP side ");
                    } else {
                        log(Level.FINEST, "ConfigureIDFF", "Successfully set " +
                                "XMLSigningOn=true on IDP side ");
                    }
                    idpMetadata = MultiProtocolCommon.configureIDP(idpWebClient,
                            configMap, "idff", true);
                } else {
                    idpMetadata = MultiProtocolCommon.configureIDP(idpWebClient,
                            configMap, "idff", false);
                }
                
                log(Level.FINEST, "ConfigureIDFF", "idp metadata" +
                        idpMetadata[0]);
                log(Level.FINEST, "ConfigureIDFF", "idp Ext metadata" +
                        idpMetadata[1]);
                if ((idpMetadata[0].equals(null)) || (
                        idpMetadata[1].equals(null))) {
                    log(Level.SEVERE, "ConfigureIDFF", "Couldn't configure " +
                            "IDP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage idpExportEntityPage = idpfm.exportEntity(idpWebClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        false, true, true, "idff");
                if (FederationManager.getExitCode(idpExportEntityPage) != 0) {
                   log(Level.SEVERE, "ConfigureIDFF", "exportEntities famadm" +
                           " command failed");
                   assert false;
                }
                idpMetadata[0] = MultiProtocolCommon.getMetadataFromPage(
                        idpExportEntityPage);
                idpMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                        idpExportEntityPage);
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_IDP_COT), "");
            log(Level.FINEST, "ConfigureIDFF", "idp metadata" +
                    idpMetadata[0]);
            log(Level.FINEST, "ConfigureIDFF", "idp Ext metadata" +
                    idpMetadata[1]);
            
            //load spmetadata on idp
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureIDFF", "sp entity exists at idp. " +
                        "Delete & load the metadata ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(
                        idpWebClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        false, "idff")) == 0) {
                    log(Level.FINEST, "ConfigureIDFF", "Delete sp entity on " +
                            "IDP side");
                } else {
                    log(Level.FINEST, "ConfigureIDFF", "Couldnt delete sp " +
                            "entity on IDP side");
                    log(Level.SEVERE, "ConfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(idpfm.importEntity(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    spMetadata[0], spMetadata[1],
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "idff"))
                    != 0) {
                log(Level.SEVERE, "ConfigureIDFF", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "ConfigureIDFF", "importEntity famadm" +
                        " command failed");
                assert false;
            }
            //load idpmetadata on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureIDFF", "idp entity exists at sp. " +
                        "Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(spWebClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, "idff")) == 0) {
                    log(Level.FINEST, "ConfigureIDFF", "Delete idp entity on " +
                            "SP side");
                } else {
                    log(Level.FINEST, "ConfigureIDFF", "Couldnt delete idp " +
                            "entity on SP side");
                    log(Level.FINEST, "ConfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(spfm.importEntity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    idpMetadata[0], idpMetadata[1],
                    (String)configMap.get(TestConstants.KEY_SP_COT), "idff"))
                    != 0) {
                log(Level.SEVERE, "ConfigureIDFF", "Couldn't import IDP");
                log(Level.SEVERE, "ConfigureIDFF", "importEntity famadm" +
                        " commad failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "ConfigureIDFF", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spWebClient, spurl);
            consoleLogout(idpWebClient, idpurl);
        }
        exiting("ConfigureIDFF");
    }
}
