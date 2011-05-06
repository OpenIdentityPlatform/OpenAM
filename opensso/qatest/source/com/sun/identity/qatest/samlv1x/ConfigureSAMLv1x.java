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
 * $Id: ConfigureSAMLv1x.java,v 1.5 2009/01/27 00:12:48 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv1x;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
import org.testng.annotations.BeforeSuite;

/**
 * This class to test the new SAMLv1x Profiles
 */
public class ConfigureSAMLv1x extends TestCommon {

    private WebClient spWebClient;
    private WebClient idpWebClient;
    private Map<String, String> configMap;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    public WebClient webClient;
    private String spurl;
    private String idpurl;
    private String servicename = "iPlanetAMSAMLService";
    private String schematype = "Global";

    /**
     * Constructor ConfigureSAMLv1x
     */
    public ConfigureSAMLv1x() {
        super("ConfigureSAMLv1x");
    }

    /**
     * Configures SAMLv1x for the SAMLv1.x Tests
     * before they execute,
     * (1) Get the siteIds
     * (2) swap the site ids to each partners
     * (3) Configure SAMLv1x with required configuration to perform POST and
     *  artifact profiles
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
            throws Exception {
        HtmlPage page;
        ArrayList attrNames;
        ArrayList attributevalues;
        HtmlPage spattrPage;
        HtmlPage idpattrPage;
        try {
            attributevalues = new ArrayList();
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" 
                    + fileseparator + "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" 
                    + fileseparator + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv1x" 
                    + fileseparator + "SAMLv1xProfileTests", configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            getWebClient();
            // Create sp configuration
            consoleLogin(spWebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            attrNames = new ArrayList();
            attrNames.add("iplanet-am-saml-siteid-issuername-list");
            //Get siteid attribute for SP
            spattrPage = fmSP.getAttrDefs(spWebClient, servicename, schematype,
                    null, attrNames);
            if (FederationManager.getExitCode(spattrPage) != 0) {
                log(Level.SEVERE, "setup", "getAttrDefs" +
                        " command failed for SP");
                assert (false);
            }
            String spsiteID = getSiteID(spattrPage);
            // Create idp configuration
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);

            consoleLogin(idpWebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            //Get siteid attribute for idp
            idpattrPage = fmIDP.getAttrDefs(idpWebClient, servicename, 
                    schematype, null, attrNames);
            if (FederationManager.getExitCode(idpattrPage) != 0) {
                log(Level.SEVERE, "setup", "getAttrDefs" +
                        " command failed for IDP");
                assert (false);
            }
            String idpsiteID = getSiteID(idpattrPage);
            String spauthtype = "NOAUTH";
            String idpauthtype = "NOAUTH";
            if (configMap.get(TestConstants.KEY_SP_PROTOCOL).startsWith("https")){
                spauthtype = "SSL";
            }
            if (configMap.get(TestConstants.KEY_IDP_PROTOCOL).startsWith("https")){
                idpauthtype = "SSL";
            }
            // Configure SP SAMLv1x site configuration
            String strSPSiteconf = "iplanet-am-saml-partner-urls=" 
                    + "partnername=" + configMap.get(TestConstants.KEY_IDP_HOST) 
                    + "|target=" 
                    + configMap.get(TestConstants.KEY_IDP_COOKIE_DOMAIN)
                    + "|sourceid=" + idpsiteID 
                    + "|samlurl=" 
                    + idpurl + "/SAMLAwareServlet" 
                    + "|soapurl=" + idpurl + "/SAMLSOAPReceiver" 
                    + "|posturl=" + idpurl + "/SAMLPOSTProfileServlet" 
                    + "|issuer=" + configMap.get(TestConstants.KEY_IDP_HOST) 
                    + ":" + configMap.get(TestConstants.KEY_IDP_PORT) 
                    + "|hostlist=" + configMap.get(TestConstants.KEY_IDP_HOST)
                    + "|AuthType=" + idpauthtype
                    + "|siteattributemapper=" +
                    "com.sun.identity.saml.plugins.DefaultSiteAttributeMapper";           
            attributevalues.add(strSPSiteconf);
            if (FederationManager.getExitCode(fmSP.addAttrDefs(spWebClient,
                    servicename, schematype, attributevalues, null)) != 0) {
                assert (false);
            }
            attributevalues.clear();
            // Configure IDP SAMLv1x site configuration
            String strIDPSiteconf = "iplanet-am-saml-partner-urls=" 
                    + "partnername=" + configMap.get(TestConstants.KEY_SP_HOST) 
                    + "|target="
                    + configMap.get(TestConstants.KEY_SP_COOKIE_DOMAIN)
                    + "|sourceid=" + spsiteID 
                    + "|samlurl=" + spurl + "/SAMLAwareServlet" 
                    + "|soapurl=" + spurl + "/SAMLSOAPReceiver" 
                    + "|posturl=" + spurl + "/SAMLPOSTProfileServlet" 
                    + "|issuer=" + configMap.get(TestConstants.KEY_SP_HOST) 
                    + ":" + configMap.get(TestConstants.KEY_SP_PORT) 
                    + "|hostlist=" + configMap.get(TestConstants.KEY_SP_HOST) 
                    + "|AuthType=" + spauthtype
                    + "|siteattributemapper=" +
                    "com.sun.identity.saml.plugins.DefaultSiteAttributeMapper";
            attributevalues.add(strIDPSiteconf);
            if (FederationManager.getExitCode(fmIDP.addAttrDefs(idpWebClient,
                    servicename, schematype, attributevalues, null)) != 0) {
                assert (false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spWebClient, spurl + "/UI/Logout");
            consoleLogout(idpWebClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
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

    /**
     * parse the page and get the SiteID which will be used to configure the
     * SAMlv1.x partner site
     * @param page
     */
    private String getSiteID(HtmlPage aPage) {

        String siteId = null;

        String astrPage = aPage.asXml();
        String subS = "siteid=";
        String subL = "issuerName=";
        int i = astrPage.indexOf(subS) + 7;
        int j = astrPage.indexOf(subL) - 1;
        siteId = astrPage.substring(i, j);

        return siteId;
    }
}
