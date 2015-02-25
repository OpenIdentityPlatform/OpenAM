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
 * $Id: ConfigUnconfig.java,v 1.2 2009/02/14 00:58:08 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idwsf;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.mortbay.jetty.Server;

/**
 * This class deploys and undeploys the client sampls war along with 
 * configuration of wsf sample. The use of embedded jetty web server or other
 * webcontainer with already deployed client sdk war is determined through
 * properties file.
 */
public class ConfigUnconfig extends TestCommon {
    
    private ResourceBundle rb_idwsf;
    private ResourceBundle rb_idff;
    private String clientsdkURL;

    /**
     * Creates a new instance of ConfigUnconfig
     * @throws Exception
     */
    public ConfigUnconfig()
    throws Exception {
        super("ConfigUnconfig");
        rb_idwsf = ResourceBundle.getBundle("config" + fileseparator +
                "default" + fileseparator + "ClientGlobal");
        rb_idff = ResourceBundle.getBundle("idff" + fileseparator +
                "idffTestConfigData");
    }
    
    /**
     * Deploy the client sampels war on jetty server and start the jetty
     * server.
     * @throws Exception
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void startServer()
    throws Exception {
        entering("startServer", null);
        clientsdkURL = deployClientSDKWar(rb_idwsf);
        configureWSFSample();
        exiting("startServer");
    }

    /**
     * Stop the jetty server. This basically undeploys the war.
     * @throws Exception
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void stopServer()
    throws Exception {
        entering("stopServer", null);
        undeployClientSDKWar(rb_idwsf);
        exiting("stopServer");
    }

    /**
     * This method configures the WSF samples
     */
    private void configureWSFSample()
    throws Exception {
        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage)webClient.getPage(clientsdkURL +
                "/wsc/configure.jsp");
        String configResult = TestConstants.KEY_WSC_CONFIG_SUCCESS_RESULT;
        if (page.getWebResponse().getContentAsString().contains("configured")) {
            log(Level.FINE, "configureWSFSample", "Sample is already " +
                    "configured");
        } else {
            HtmlForm form = page.getFormByName("configure");
            HtmlInput txtspProviderIDinput = form.getInputByName
                    ("spProviderIDinput");
            txtspProviderIDinput.setValueAttribute(rb_idff.
                    getString(TestConstants.KEY_SP_ENTITY_NAME));
            HtmlInput txtidpProt = form.getInputByName("idpProt");
            txtidpProt.setValueAttribute(rb_idff.getString(TestConstants.
                    KEY_IDP_PROTOCOL));
            HtmlInput txtidpHost = form.getInputByName("idpHost");
            txtidpHost.setValueAttribute(rb_idff.getString(TestConstants.
                    KEY_IDP_HOST));
            HtmlInput txtidpPort = form.getInputByName("idpPort");
            txtidpPort.setValueAttribute(rb_idff.getString(TestConstants.
                    KEY_IDP_PORT));
            HtmlInput txtidpDeploymenturi = form.getInputByName(
                    "idpDeploymenturi");
            txtidpDeploymenturi.setValueAttribute(rb_idff.getString(
                    TestConstants.KEY_IDP_DEPLOYMENT_URI));
            HtmlPage returnedPage = (HtmlPage)form.getInputByName("submit").click();
            if (returnedPage.getWebResponse().getContentAsString().contains(
                    configResult)) {
                log(Level.FINE, "configureWSFSample", "WSC sample is " +
                        "configured successfully");
            } else {
                log(Level.SEVERE, "configureWSFSample", "WSC sample is not" +
                        "configured " + returnedPage.getWebResponse().
                        getContentAsString());
            }
        }
   }
}
