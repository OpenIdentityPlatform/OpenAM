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
 * $Id: UnconfigureSAMLv1x.java,v 1.2 2009/01/27 00:12:48 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv1x;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.SAMLv1Common;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Map;
import java.util.ResourceBundle;
import org.testng.annotations.AfterSuite;


/**
 * This class to Unconfigure SAMLv1x
 */
public class  UnconfigureSAMLv1x extends TestCommon {
    
    
    private WebClient spWebClient;
    private WebClient idpWebClient;
    private Map<String, String> configMap;
    private String baseDir ;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    public  WebClient webClient;
    private String spurl;
    private String idpurl;
    private String servicename = "iPlanetAMSAMLService";
    private String schematype = "Global";
    
    /**
     * Constructor UnconfigureSAMLv1x
     */
    public UnconfigureSAMLv1x() {
        super("UnconfigureSAMLv1x");
    }
    
    /**
     * Unonfigures SAMLv1x 
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void UnconfigureSAMLv1x()
    throws Exception {
        ArrayList list;
        try {
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + SAMLv2Common.fileseparator + "built"
                    + SAMLv2Common.fileseparator + "classes"
                    + SAMLv2Common.fileseparator;
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv1x" + fileseparator
                    + "SAMLv1xProfileTests", configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            getWebClient();
            // Create sp users
            consoleLogin(spWebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            list = new ArrayList();
            list.clear();
            list.add("iplanet-am-saml-partner-urls");
            if (FederationManager.getExitCode(fmSP.removeAttrDefs(spWebClient,
                    servicename, schematype, list, null)) != 0 ) {
                assert(false);
            }
            // Create idp users
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            consoleLogin(idpWebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            if (FederationManager.getExitCode(fmIDP.removeAttrDefs(idpWebClient,
                    servicename, schematype, list, null)) != 0 ) {
                assert(false);
            }
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureSAMLv1x", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spWebClient, spurl + "/UI/Logout");
            consoleLogout(idpWebClient, idpurl + "/UI/Logout");
        }
        exiting("UnconfigureSAMLv1x");
    }
    
    /**
     * Create the webClient which will be used for 
     * the configuration.
     */
    public void getWebClient()
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
            spWebClient = new WebClient(BrowserVersion.FIREFOX_3);
            idpWebClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
