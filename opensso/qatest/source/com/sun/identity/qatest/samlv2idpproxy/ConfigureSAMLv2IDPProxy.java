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
 * $Id: ConfigureSAMLv2IDPProxy.java,v 1.3 2009/01/27 00:15:32 nithyas Exp $
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
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
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
public class ConfigureSAMLv2IDPProxy extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    public String groupName = "";
    String spurl;
    String idpurl;
    String idpproxyurl;
    
    private String ENABLE_IDP_PROXY_DEFAULT = "       <Attribute name=\"enableIDPProxy\">\n" +
            "           <Value>false</Value>\n" +
            "       </Attribute>";
    private String ENABLE_IDP_PROXY_VALUE = "       <Attribute name=\"enableIDPProxy\">\n" +
            "           <Value>true</Value>\n" +
            "       </Attribute>";
    private String IDP_PROXY_LIST_DEFAULT = "       <Attribute name=\"idpProxyList\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>";
    private String IDP_PROXY_COUNT_DEFAULT = "       <Attribute name=\"idpProxyCount\">\n" +
            "           <Value>0</Value>\n" +
            "       </Attribute>";
    private String IDP_PROXY_COUNT_VALUE = "       <Attribute name=\"idpProxyCount\">\n" +
            "           <Value>1</Value>\n" +
            "       </Attribute>";
    
    /** Creates a new instance of ConfigureSAMLv2IDPProxy */
    public ConfigureSAMLv2IDPProxy() {
        super("ConfigureSAMLv2IDPProxy");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient() throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch(Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Configure sp, idp & idp proxy
     * @DocTest: SAML2|Configure SP, IDP & IDP Proxy by loading metadata, 
     * creating COTs.
     */
    @Parameters({"groupName"})
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void ConfigureSAMLv2IDPProxy(String strGroupName)
    throws Exception {
        Object[] params = {strGroupName};
        entering("ConfigureSAMLv2IDPProxy", params);
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();
            
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "GroupName received " +
                    "from testng is " + strGroupName);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData",
                    configMap);
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL)
                    + "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL)
                    + "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + configMap.get(TestConstants.KEY_IDP_PORT)
                    + configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            idpproxyurl = configMap.get(TestConstants.KEY_IDP_PROXY_PROTOCOL)
                    + "://" + configMap.get(TestConstants.KEY_IDP_PROXY_HOST) + 
                    ":" + configMap.get(TestConstants.KEY_IDP_PROXY_PORT)
                    + configMap.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
        } catch(Exception e) {
            log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try {
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            FederationManager idpproxyfm = new FederationManager(idpproxyurl);
            
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
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listCots " +
                       "famadm command failed");
               assert false;
            }
            if (!spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.createCot(webClient,
                        configMap.get(TestConstants.KEY_SP_COT),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "create COT at SP side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "createCot " +
                            "famadm command failed");
                    assert false;
                }
            } else {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "COT exists at " +
                        "SP side");
            }
            
            String spMetadata[] = {"", ""};
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listEntities " +
                       "famadm command failed");
               assert false;
            }
            if (!spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "sp entity " +
                        "doesnt exist. Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    spMetadata = configureSP(webClient, spfm, configMap, true);
                } else {
                    spMetadata = configureSP(webClient, spfm, configMap, false);
                }
                if ((spMetadata[0].equals(null)) ||
                        (spMetadata[1].equals(null))) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "configure SP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage spExportEntityPage = spfm.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, true, true, "saml2");
                if (FederationManager.getExitCode(spExportEntityPage) != 0) {
                   log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                           "exportEntity famadm command failed");
                   assert false;
                }
                spMetadata[0] = SAMLv2Common.getMetadataFromPage(
                        spExportEntityPage);
                spMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                        spExportEntityPage);
            }
            spMetadata[1] = spMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_SP_COT), "");
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "sp metadata" + 
                    spMetadata[0]);
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "sp Ext metadata" +
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
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listCots famadm " +
                       "command failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "COT exists at " +
                        "IDP side");
            } else {
                if (FederationManager.getExitCode(idpfm.createCot(webClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "create COT at IDP side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "createCot " +
                            "famadm command failed");
                    assert false;
                }
            }
            
            String[] idpMetadata = {"",""};
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listEntities " +
                       "famadm command failed");
               assert false;
            }
            if (!idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp entity " +
                        "doesnt exist. Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    idpMetadata = MultiProtocolCommon.importMetadata(webClient,
                            configMap, true, "IDP");
                } else {
                    idpMetadata = MultiProtocolCommon.importMetadata(webClient,
                            configMap, false, "IDP");
                }
                
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp metadata" +
                        idpMetadata[0]);
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp Ext metadata" +
                        idpMetadata[1]);
                if ((idpMetadata[0].equals(null)) || (
                        idpMetadata[1].equals(null))) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "configure IDP");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage idpExportEntityPage = idpfm.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, true, true, "saml2");
                if (FederationManager.getExitCode(idpExportEntityPage) != 0) {
                   log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                           "exportEntity famadm command failed");
                   assert false;
                }
                idpMetadata[0] = SAMLv2Common.getMetadataFromPage(
                        idpExportEntityPage);
                idpMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                        idpExportEntityPage);
            }
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_IDP_COT), "");
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp metadata" +
                    idpMetadata[0]);
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp Ext metadata" +
                    idpMetadata[1]);
            
            
            //idp proxy side create cot, load proxy metadata
            consoleLogin(webClient, idpproxyurl + "/UI/Login",
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));

            //If execution_realm is different than root realm (/) 
            //then create the realm
            idmC.createSubRealms(webClient, idpproxyfm, configMap.get(
                       TestConstants.KEY_IDP_PROXY_EXECUTION_REALM));
            
            HtmlPage idpproxycotPage = idpproxyfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpproxycotPage) != 0) {
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listCots " +
                       "famadm command failed");
               assert false;
            }
            if (idpproxycotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_PROXY_COT))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "COT exists at " +
                        "IDP side");
            } else {
                if (FederationManager.getExitCode(idpproxyfm.createCot(webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_COT),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM),
                        null, null)) != 0) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "create COT at IDP side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "createCot " +
                            "famadm command failed");
                    assert false;
                }
            }
            
            String[] idpproxyMetadata = {"", ""};
            HtmlPage idpproxyEntityPage = idpproxyfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "saml2");
            if (FederationManager.getExitCode(idpproxyEntityPage) != 0) {
               log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "listEntities " +
                       "famadm command failed");
               assert false;
            }
            if (!idpproxyEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME)))
            {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "IDP Proxy " +
                        "entity doesnt exist. Get template & create the entity");
                if (strGroupName.contains("sec")) {
                    idpproxyMetadata = MultiProtocolCommon.importMetadata(
                            webClient, configMap, true, "IDPPROXY");
                } else {
                    idpproxyMetadata = MultiProtocolCommon.importMetadata(
                            webClient,
                            configMap, false, "IDPPROXY");
                }
                
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy " +
                        "proxymetadata" + idpproxyMetadata[0]);
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy Ext " +
                        "metadata" + idpproxyMetadata[1]);
                if ((idpproxyMetadata[0].equals(null)) || (
                        idpproxyMetadata[1].equals(null))) {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                            "configure IDP proxy ");
                    assert false;
                }
            } else {
                //If entity exists, export to get the metadata.
                HtmlPage idpproxyExportEntityPage = idpproxyfm.exportEntity(
                        webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), false, true, true, 
                        "saml2");
                if (FederationManager.getExitCode(idpproxyExportEntityPage) 
                        != 0) {
                   log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                           "exportEntity famadm command failed at IDP Proxy");
                   assert false;
                }
                idpproxyMetadata[0] = SAMLv2Common.getMetadataFromPage(
                        idpproxyExportEntityPage);
                idpproxyMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                        idpproxyExportEntityPage);
            }
            idpproxyMetadata[1] = idpproxyMetadata[1].replaceAll(
                    configMap.get(TestConstants.KEY_IDP_COT), "");
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy metadata" +
                    idpproxyMetadata[0]);
            log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy Ext " +
                    "metadata" + idpproxyMetadata[1]);

            //load spmetadata on idp proxy
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "sp entity " +
                        "exists at idp proxy. Delete & load the metadata ");
                if (FederationManager.getExitCode(idpfm.deleteEntity(
                        webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "Deleted " +
                            "sp entity on IDP Proxy side");
                } else {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete sp entity on IDP Proxy side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                    assert false;
                }
            }
            
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            spMetadata[1] = spMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(idpproxyfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    spMetadata[0], spMetadata[1], (String)configMap.get(
                    TestConstants.KEY_IDP_PROXY_COT), "saml2")) != 0) {
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                        "import SP metadata on IDP Proxy side");
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "importEntity " +
                        "famadmcommand failed");
                assert false;
            }

            //load idp proxy metadata on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy " +
                        "entity exists at sp. Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "Deleted " +
                            "idp proxy entity on SP side");
                } else {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete idp proxy entity on SP side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                    assert false;
                }
            }
            idpproxyMetadata[1] = idpproxyMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            idpproxyMetadata[1] = idpproxyMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (FederationManager.getExitCode(spfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    idpproxyMetadata[0], idpproxyMetadata[1], (String)configMap.
                    get(TestConstants.KEY_SP_COT), "saml2")) != 0) {
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                        "import IDP metadata on SP side");
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "importEntity " +
                        "famadm command failed");
                assert false;
            }

            //load idp proxy metadata on idp
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp proxy " +
                        "entity exists at idp. Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "saml2")) == 0) {
                    log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "Deleted " +
                            "idp proxy entity on IDP side");
                } else {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete idp proxy entity on IDP side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                    assert false;
                }
            }

            if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpproxyMetadata[0], idpproxyMetadata[1],
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "saml2"))
                    != 0) {
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                        "import IDP Proxy metadata on IDP side");
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "importEntity " +
                        "famadm command failed");
                assert false;
            }

            //load idpmetadata on idp proxy
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            idpMetadata[1] = idpMetadata[1].replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            if (idpproxyEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.
                    KEY_IDP_PROXY_ENTITY_NAME))) {
                log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "idp entity " +
                        "exists at IDP Proxy. Delete & load the metadata ");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), false, "saml2")) == 0) {
                    log(Level.FINEST, "ConfigureSAMLv2IDPProxy", "Deleted " +
                            "idp entity on IDP Proxy side");
                } else {
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldnt " +
                            "delete idpm entity on IDP Proxy side");
                    log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", 
                            "deleteEntity famadm command failed");
                    assert false;
                }
            }
    
            if (FederationManager.getExitCode(idpproxyfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    idpMetadata[0], idpMetadata[1], (String)configMap.get(
                    TestConstants.KEY_IDP_PROXY_COT), "saml2")) != 0) {
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "Couldn't " +
                        "import IDP metadata on IDP Proxy side");
                log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", "importEntity " +
                        "famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "ConfigureSAMLv2IDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
        }
        exiting("ConfigureSAMLv2IDPProxy");
    }
    
    private String[] configureSP(WebClient webClient, FederationManager spfm, 
            Map m, boolean signed) {
        String[] arrMetadata= {"", ""};
        try {
            //get sp  metadata
            HtmlPage spmetaPage;
            if (signed) {
                spmetaPage = spfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_SP_ENTITY_NAME), true,
                        true, (String)m.get(TestConstants.KEY_SP_METAALIAS),
                        null, null, null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_SP_CERTALIAS), null,
                        null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_SP_CERTALIAS), null,
                        null, null, null, null, null, null, "saml2");
            } else {
                spmetaPage = spfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_SP_ENTITY_NAME), true,
                        true, (String)m.get(TestConstants.KEY_SP_METAALIAS),
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "saml2");
            }
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               assert false;
            }
            
            arrMetadata[0] = MultiProtocolCommon.getMetadataFromPage(spmetaPage, 
                    "saml2");
            arrMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                    spmetaPage, "saml2");
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null)))
            {
                assert(false);
            } else {
                arrMetadata[0] = arrMetadata[0].replaceAll("&lt;", "<");
                arrMetadata[0] = arrMetadata[0].replaceAll("&gt;", ">");
                arrMetadata[1] = arrMetadata[1].replaceAll("&lt;", "<");
                arrMetadata[1] = arrMetadata[1].replaceAll("&gt;", ">");
                String IDP_PROXY_LIST_VALUE = "       <Attribute name=\"" +
                        "idpProxyList\">\n" +
                        "           <Value>" + m.get(TestConstants.
                        KEY_IDP_ENTITY_NAME) +"</Value>\n" +
                        "       </Attribute>";

                arrMetadata[1] = arrMetadata[1].replace(ENABLE_IDP_PROXY_DEFAULT, 
                        ENABLE_IDP_PROXY_VALUE);
                arrMetadata[1] = arrMetadata[1].replace(IDP_PROXY_COUNT_DEFAULT, 
                        IDP_PROXY_COUNT_VALUE);
                arrMetadata[1] = arrMetadata[1].replaceAll(IDP_PROXY_LIST_DEFAULT,
                        IDP_PROXY_LIST_VALUE);
                if (FederationManager.getExitCode(spfm.importEntity(webClient,
                        (String)m.get(TestConstants.KEY_SP_REALM), 
                        arrMetadata[0], arrMetadata[1], 
                        (String)m.get(TestConstants.KEY_SP_COT), "saml2")) != 0) {
                    arrMetadata[0] = null;
                    arrMetadata[1] = null;
                    assert(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrMetadata;
    }
}
